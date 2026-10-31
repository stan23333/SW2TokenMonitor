package com.tokenmonitor.sw2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class PeriodicUpdateReceiver extends BroadcastReceiver {

    public static final String ACTION_PERIODIC_TICK = "com.tokenmonitor.sw2.ACTION_PERIODIC_TICK";
    public static final long INTERVAL_MILLIS = 5 * 60 * 1000; // 5分钟

    @Override
    public void onReceive(Context context, Intent intent) {
        AppLog.d("PeriodicUpdateReceiver", "5-minute Alarm triggered! Refreshing watch & widgets...");

        // 1. 更新最新时间戳
        long now = System.currentTimeMillis();
        SharedPreferences sp = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE);
        sp.edit().putLong("last_update_time", now).commit();

        // 2. 刷新手机桌面小组件
        TokenAppWidgetProvider.updateAllWidgets(context);

        // 3. 【双重发射】：常规 ControlExtension 发射 + 底层 Intent 直射
        TokenControlExtension control = TokenExtensionService.getCurrentControl();
        if (control != null) {
            control.renderDashboard();
        }
        WatchPusher.pushDirectly(context);

        // 4. 预约下一次 5 分钟
        scheduleNext(context);
    }

    public static void scheduleNext(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, PeriodicUpdateReceiver.class);
        intent.setAction(ACTION_PERIODIC_TICK);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = System.currentTimeMillis() + INTERVAL_MILLIS;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }
}
