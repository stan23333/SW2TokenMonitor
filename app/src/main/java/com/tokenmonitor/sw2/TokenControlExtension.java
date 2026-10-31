package com.tokenmonitor.sw2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;

public class TokenControlExtension extends ControlExtension {

    private boolean mIsLowPowerMode = false;
    private int mActiveTheme = WatchRenderer.THEME_M3;
    private final WatchRenderer.WatchData mData = new WatchRenderer.WatchData();
    private boolean mIsReceiverRegistered = false;

    // 核心：监听 HTTP 接收器发出的即时数据到达广播，0秒同步手表屏幕
    private final BroadcastReceiver mDataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppLog.d("TokenControlExtension", "Instant broadcast received! Pushing frame to watch...");
            renderDashboard();
        }
    };

    public TokenControlExtension(Context context, String hostAppPackageName) {
        super(context, hostAppPackageName);
        loadPreferences();
    }

    private void loadPreferences() {
        SharedPreferences sp = mContext.getSharedPreferences("token_prefs", Context.MODE_PRIVATE);
        mActiveTheme = sp.getInt("active_theme", WatchRenderer.THEME_M3);
        mData.modelName = sp.getString("model_name", "gemini-3.8-flash");
        mData.totalTokens = sp.getLong("used_tokens", 3338800);
        mData.inputTokens = sp.getLong("input_tokens", 3297293);
        mData.outputTokens = sp.getLong("output_tokens", 41532);
        mData.cacheReadTokens = sp.getLong("cache_tokens", 33472572);
        mData.costUsd = Double.longBitsToDouble(sp.getLong("cost_usd", Double.doubleToLongBits(0.00)));
        mData.lastUpdateTime = sp.getLong("last_update_time", System.currentTimeMillis());
        mData.localIp = TokenExtensionService.getLocalIpAddress(mContext);
    }

    @Override
    public void onActiveLowPowerModeChange(boolean lowPowerModeOn) {
        mIsLowPowerMode = lowPowerModeOn;
        renderDashboard();
    }

    @Override
    public void onResume() {
        loadPreferences();
        setScreenState(Control.Intents.SCREEN_STATE_AUTO);

        // 注册即时监听
        if (!mIsReceiverRegistered) {
            IntentFilter filter = new IntentFilter(LocalHttpServer.ACTION_TOKEN_DATA_UPDATED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mContext.registerReceiver(mDataUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                mContext.registerReceiver(mDataUpdateReceiver, filter);
            }
            mIsReceiverRegistered = true;
        }

        renderDashboard();
        PeriodicUpdateReceiver.scheduleNext(mContext);
    }

    @Override
    public void onPause() {
        // 注销动态监听，节约资源
        if (mIsReceiverRegistered) {
            try {
                mContext.unregisterReceiver(mDataUpdateReceiver);
            } catch (Exception ignored) {}
            mIsReceiverRegistered = false;
        }
    }

    public void updateTheme(int theme) {
        mActiveTheme = theme;
        renderDashboard();
    }

    public void renderDashboard() {
        loadPreferences();
        mData.localIp = TokenExtensionService.getLocalIpAddress(mContext);
        Bitmap bitmap = WatchRenderer.render(mActiveTheme, mIsLowPowerMode, mData);
        showBitmap(bitmap);
    }

    @Override
    public void onTouch(ControlTouchEvent event) {
        if (event.getAction() == Control.TapActions.SINGLE_TAP) {
            mData.lastUpdateTime = System.currentTimeMillis();
            SharedPreferences sp = mContext.getSharedPreferences("token_prefs", Context.MODE_PRIVATE);
            sp.edit().putLong("last_update_time", mData.lastUpdateTime).commit();

            TokenAppWidgetProvider.updateAllWidgets(mContext);
            renderDashboard();

            PeriodicUpdateReceiver.scheduleNext(mContext);
        }
    }
}
