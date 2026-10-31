package com.tokenmonitor.sw2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.sonyericsson.extras.liveware.aef.control.Control;

import java.io.ByteArrayOutputStream;

public class WatchPusher {

    public static final String SW2_HOST_PKG = "com.sonymobile.smartconnect.smartwatch2";
    public static final String EXTENSION_PERMISSION = "com.sonyericsson.extras.liveware.aef.HOSTAPP_PERMISSION";

    public static void pushDirectly(Context context) {
        SharedPreferences sp = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE);

        WatchRenderer.WatchData data = new WatchRenderer.WatchData();
        data.modelName = sp.getString("model_name", "gemini-3.8-flash");
        data.totalTokens = sp.getLong("used_tokens", 3338800);
        data.inputTokens = sp.getLong("input_tokens", 3297293);
        data.outputTokens = sp.getLong("output_tokens", 41532);
        data.cacheReadTokens = sp.getLong("cache_tokens", 33472572);
        data.costUsd = Double.longBitsToDouble(sp.getLong("cost_usd", Double.doubleToLongBits(0.00)));
        data.lastUpdateTime = sp.getLong("last_update_time", System.currentTimeMillis());
        data.localIp = TokenExtensionService.getLocalIpAddress(context);

        int activeTheme = sp.getInt("active_theme", WatchRenderer.THEME_M3);

        // 渲染最新帧
        Bitmap bitmap = WatchRenderer.render(activeTheme, false, data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

        // 核心：直接向索尼主机插件发射底层 DISPLAY_DATA 广播！
        Intent intent = new Intent(Control.Intents.CONTROL_DISPLAY_DATA_INTENT);
        intent.setPackage(SW2_HOST_PKG);
        intent.putExtra(Control.Intents.EXTRA_AEA_PACKAGE_NAME, context.getPackageName());
        intent.putExtra(Control.Intents.EXTRA_DATA, outputStream.toByteArray());

        context.sendBroadcast(intent, EXTENSION_PERMISSION);
        AppLog.d("WatchPusher", "Direct CONTROL_DISPLAY_DATA_INTENT broadcast pushed to SW2 host app!");
    }
}
