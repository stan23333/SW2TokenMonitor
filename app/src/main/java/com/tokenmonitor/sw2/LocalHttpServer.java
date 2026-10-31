package com.tokenmonitor.sw2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class LocalHttpServer {

    private static final String TAG = "LocalHttpServer";
    public static final int PORT = 8888;
    public static final String ACTION_TOKEN_DATA_UPDATED = "com.tokenmonitor.sw2.ACTION_TOKEN_DATA_UPDATED";

    private static LocalHttpServer sInstance;
    private final Context mContext;
    private ServerSocket mServerSocket;
    private volatile boolean mIsRunning = false;

    public static synchronized LocalHttpServer getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LocalHttpServer(context.getApplicationContext());
        }
        return sInstance;
    }

    private LocalHttpServer(Context context) {
        mContext = context;
        AppLog.init(context);
    }

    public synchronized void start() {
        if (mIsRunning && mServerSocket != null && !mServerSocket.isClosed()) return;
        mIsRunning = true;
        new Thread(() -> {
            try {
                if (mServerSocket != null && !mServerSocket.isClosed()) {
                    try { mServerSocket.close(); } catch (Exception ignored) {}
                }
                mServerSocket = new ServerSocket();
                mServerSocket.setReuseAddress(true);
                mServerSocket.bind(new InetSocketAddress(PORT));
                AppLog.d(TAG, "HTTP Server listening on port " + PORT);

                while (mIsRunning) {
                    Socket socket = mServerSocket.accept();
                    handleClient(socket);
                }
            } catch (Exception e) {
                if (mIsRunning) {
                    AppLog.e(TAG, "HTTP Server socket bind/accept failed", e);
                }
            }
        }).start();
    }

    public synchronized void stop() {
        mIsRunning = false;
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (Exception ignored) {}
            mServerSocket = null;
        }
    }

    private void handleClient(Socket socket) {
        new Thread(() -> {
            String clientIp = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "unknown";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 OutputStream os = socket.getOutputStream()) {

                String line = reader.readLine();
                if (line == null) return;

                int contentLength = 0;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }

                StringBuilder body = new StringBuilder();
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    int read = 0;
                    while (read < contentLength) {
                        int r = reader.read(buffer, read, contentLength - read);
                        if (r == -1) break;
                        read += r;
                    }
                    body.append(buffer, 0, read);
                }

                String jsonStr = body.toString();
                if (!jsonStr.isEmpty()) {
                    JSONObject json = new JSONObject(jsonStr);

                    WatchRenderer.WatchData data = new WatchRenderer.WatchData();
                    data.modelName = json.optString("model", "gemini-3.8-flash");
                    data.totalTokens = json.optLong("total_tokens", json.optLong("tokens", 0));
                    data.inputTokens = json.optLong("input_tokens", 0);
                    data.outputTokens = json.optLong("output_tokens", 0);
                    data.cacheReadTokens = json.optLong("cache_tokens", 0);
                    data.costUsd = json.optDouble("cost", 0.0);
                    data.lastUpdateTime = System.currentTimeMillis();

                    AppLog.d(TAG, "Received payload from " + clientIp + ": model=" + data.modelName + ", total=" + data.totalTokens + ", in=" + data.inputTokens + ", out=" + data.outputTokens);

                    // 1. 持久化存储
                    SharedPreferences sp = mContext.getSharedPreferences("token_prefs", Context.MODE_PRIVATE);
                    sp.edit()
                            .putString("model_name", data.modelName)
                            .putLong("used_tokens", data.totalTokens)
                            .putLong("input_tokens", data.inputTokens)
                            .putLong("output_tokens", data.outputTokens)
                            .putLong("cache_tokens", data.cacheReadTokens)
                            .putLong("cost_usd", Double.doubleToLongBits(data.costUsd))
                            .putLong("last_update_time", data.lastUpdateTime)
                            .commit();

                    // 2. 立即刷新手机桌面小组件
                    TokenAppWidgetProvider.updateAllWidgets(mContext);

                    // 3. 【核心双重直推手表】：无论手表处于什么生命周期，直接发射最新帧！
                    TokenControlExtension control = TokenExtensionService.getCurrentControl();
                    if (control != null) {
                        control.renderDashboard();
                    }
                    WatchPusher.pushDirectly(mContext);

                    // 4. 发送广播通知 Activity
                    Intent updateIntent = new Intent(ACTION_TOKEN_DATA_UPDATED);
                    updateIntent.setPackage(mContext.getPackageName());
                    mContext.sendBroadcast(updateIntent);

                    // 5. 重置下一次 5 分钟心跳定时
                    PeriodicUpdateReceiver.scheduleNext(mContext);
                }

                // HTTP 200 回包
                String responseBody = "{\"status\":\"ok\",\"received_at\":" + System.currentTimeMillis() + "}";
                byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + responseBytes.length + "\r\n" +
                        "Connection: close\r\n\r\n" + responseBody;
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.flush();

            } catch (Exception e) {
                AppLog.e(TAG, "Client handle error from " + clientIp, e);
            }
        }).start();
    }
}
