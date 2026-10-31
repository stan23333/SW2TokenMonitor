package com.tokenmonitor.sw2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TokenAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), TokenAppWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences sp = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE);

        String model = sp.getString("model_name", "gemini-3.8-flash");
        long totalTokens = sp.getLong("used_tokens", 3338800);
        long inTokens = sp.getLong("input_tokens", 3297293);
        long outTokens = sp.getLong("output_tokens", 41532);
        long cacheTokens = sp.getLong("cache_tokens", 33472572);
        double cost = Double.longBitsToDouble(sp.getLong("cost_usd", Double.doubleToLongBits(0.00)));
        long updateTime = sp.getLong("last_update_time", System.currentTimeMillis());

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_token_layout);

        views.setTextViewText(R.id.widget_tv_model, model);
        views.setTextViewText(R.id.widget_tv_tokens, formatTokens(totalTokens));
        views.setTextViewText(R.id.widget_tv_input, formatCompact(inTokens));
        views.setTextViewText(R.id.widget_tv_output, formatCompact(outTokens));
        views.setTextViewText(R.id.widget_tv_cache, formatCompact(cacheTokens));
        views.setTextViewText(R.id.widget_tv_cost, String.format(Locale.US, "$%.2f", cost));

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        views.setTextViewText(R.id.widget_tv_sync_time, sdf.format(new Date(updateTime)));

        // 点击小组件直接启动手机 App
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static String formatTokens(long tokens) {
        if (tokens >= 1_000_000) {
            return String.format(Locale.US, "%,.1f M", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format(Locale.US, "%,.1f K", tokens / 1_000.0);
        } else {
            return String.format(Locale.US, "%d", tokens);
        }
    }

    private static String formatCompact(long tokens) {
        if (tokens >= 1_000_000) {
            return String.format(Locale.US, "%.2fM", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format(Locale.US, "%.1fK", tokens / 1_000.0);
        } else {
            return String.valueOf(tokens);
        }
    }
}
