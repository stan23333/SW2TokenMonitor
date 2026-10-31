package com.tokenmonitor.sw2;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WatchRenderer {

    public static final int WIDTH = 220;
    public static final int HEIGHT = 176;

    // 主题 0: Material You 现代莫兰迪 (新增 M3 风格)
    public static final int THEME_M3 = 0;
    // 主题 1: 赛博霓虹蓝绿 (Cyber Neon)
    public static final int THEME_CYBER = 1;
    // 主题 2: 幻境霓裳 (Synthwave Purple)
    public static final int THEME_SYNTHWAVE = 2;
    // 主题 3: 极光翡翠 (Matrix Aurora)
    public static final int THEME_AURORA = 3;
    // 主题 4: 经典极简白金 (Classic Monochrome)
    public static final int THEME_MONO = 4;

    public static class WatchData {
        public String modelName = "gemini-3.8-flash";
        public long totalTokens = 3338800;
        public long inputTokens = 3297293;
        public long outputTokens = 41532;
        public long cacheReadTokens = 33472572;
        public double costUsd = 0.00;
        public long lastUpdateTime = System.currentTimeMillis();
        public String localIp = "192.168.0.11";
    }

    public static Bitmap render(int theme, boolean isLowPower, WatchData data) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);

        if (isLowPower) {
            renderAodState(canvas, data);
        } else {
            renderActiveState(canvas, theme, data);
        }

        return bitmap;
    }

    private static void renderAodState(Canvas canvas, WatchData data) {
        canvas.drawColor(Color.BLACK);

        Paint paint = new Paint();
        paint.setAntiAlias(false); // 关键：半透半反待机屏关掉抗锯齿，文字轮廓极致锐利
        paint.setColor(Color.WHITE);

        // 1. 顶部 Header
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("OPENCHAMBER", 8, 17, paint);
        canvas.drawText("WIFI ●", WIDTH - 46, 17, paint);

        paint.setStrokeWidth(1);
        canvas.drawLine(8, 22, WIDTH - 8, 22, paint);

        // 2. 模型名称与今日标签
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String cleanModel = cleanModelName(data.modelName);
        canvas.drawText(cleanModel, 8, 41, paint);

        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("TODAY", WIDTH - 46, 41, paint);

        // 3. 核心 Token 消耗大字
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        String tokenStr = formatTokens(data.totalTokens);
        canvas.drawText(tokenStr, 8, 76, paint);

        // 4. 四宫格小数据卡片
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRect(8, 85, WIDTH - 8, 145, paint);
        canvas.drawLine(WIDTH / 2, 85, WIDTH / 2, 145, paint);
        canvas.drawLine(8, 115, WIDTH - 8, 115, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("IN: " + formatTokensCompact(data.inputTokens), 13, 106, paint);
        canvas.drawText("OUT: " + formatTokensCompact(data.outputTokens), WIDTH / 2 + 7, 106, paint);
        canvas.drawText("CACH: " + formatTokensCompact(data.cacheReadTokens), 13, 137, paint);
        canvas.drawText(String.format(Locale.US, "$%.2f", data.costUsd), WIDTH / 2 + 7, 137, paint);

        // 5. 底部页脚
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date(data.lastUpdateTime));
        canvas.drawText(data.localIp, 8, 163, paint);
        canvas.drawText(timeStr, WIDTH - 56, 163, paint);
    }

    private static void renderActiveState(Canvas canvas, int theme, WatchData data) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int bgColor, primaryColor, accentColor, numColor, subTextColor, boxBgColor, boxBorderColor;

        if (theme == THEME_M3) {
            // Material You (Google M3 现代莫兰迪：灰黑底 + 薰衣草淡紫 + 柔白 + 柔粉)
            bgColor = Color.rgb(28, 27, 31);          // M3 Surface
            primaryColor = Color.rgb(208, 188, 255);    // M3 Primary (薰衣草紫)
            accentColor = Color.rgb(168, 235, 178);     // M3 Success (柔青绿)
            numColor = Color.rgb(230, 225, 229);        // M3 OnSurface (高质感纯白)
            subTextColor = Color.rgb(202, 196, 208);    // M3 Outline Variant
            boxBgColor = Color.rgb(43, 41, 48);         // M3 Surface Container Highest
            boxBorderColor = Color.rgb(74, 68, 88);
        } else if (theme == THEME_SYNTHWAVE) {
            bgColor = Color.rgb(13, 9, 26);
            primaryColor = Color.rgb(255, 0, 128);    // 荧光粉红
            accentColor = Color.rgb(179, 136, 255);   // 浅柔紫
            numColor = Color.rgb(0, 240, 255);        // 电光青
            subTextColor = Color.rgb(180, 150, 230);
            boxBgColor = Color.rgb(25, 18, 44);
            boxBorderColor = Color.rgb(58, 39, 84);
        } else if (theme == THEME_AURORA) {
            bgColor = Color.rgb(8, 18, 13);
            primaryColor = Color.rgb(0, 255, 136);    // 薄荷翠绿
            accentColor = Color.rgb(52, 211, 153);    // 翡翠浅绿
            numColor = Color.rgb(167, 243, 208);      // 冰透白绿
            subTextColor = Color.rgb(110, 231, 183);
            boxBgColor = Color.rgb(15, 36, 26);
            boxBorderColor = Color.rgb(28, 70, 48);
        } else if (theme == THEME_MONO) {
            bgColor = Color.rgb(10, 10, 10);
            primaryColor = Color.rgb(255, 255, 255);
            accentColor = Color.rgb(200, 200, 200);
            numColor = Color.rgb(255, 255, 255);
            subTextColor = Color.rgb(160, 160, 160);
            boxBgColor = Color.rgb(24, 24, 24);
            boxBorderColor = Color.rgb(50, 50, 50);
        } else {
            // 赛博霓虹
            bgColor = Color.rgb(10, 14, 20);
            primaryColor = Color.rgb(0, 210, 255);    // 霓虹天蓝
            accentColor = Color.rgb(88, 255, 177);    // 荧光绿
            numColor = Color.rgb(88, 255, 177);
            subTextColor = Color.rgb(140, 175, 210);
            boxBgColor = Color.rgb(20, 30, 42);
            boxBorderColor = Color.rgb(35, 55, 75);
        }

        canvas.drawColor(bgColor);

        // 1. 顶部 Header
        paint.setColor(primaryColor);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("OPENCHAMBER", 8, 17, paint);

        paint.setColor(accentColor);
        canvas.drawText("WIFI ●", WIDTH - 46, 17, paint);

        paint.setColor(primaryColor);
        paint.setStrokeWidth(2);
        canvas.drawLine(8, 22, WIDTH - 8, 22, paint);

        // 2. 模型名称
        paint.setColor(subTextColor);
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String cleanModel = cleanModelName(data.modelName);
        canvas.drawText(cleanModel, 8, 41, paint);

        paint.setColor(primaryColor);
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("TODAY", WIDTH - 46, 41, paint);

        // 3. 核心 Token 消耗大字
        paint.setColor(numColor);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        String tokenStr = formatTokens(data.totalTokens);
        canvas.drawText(tokenStr, 8, 76, paint);

        // 4. 四宫格小数据卡片 (M3 胶囊圆角卡片)
        paint.setColor(boxBgColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(8, 85, WIDTH - 8, 145, 6, 6, paint);

        paint.setColor(boxBorderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(8, 85, WIDTH - 8, 145, 6, 6, paint);
        canvas.drawLine(WIDTH / 2, 85, WIDTH / 2, 145, paint);
        canvas.drawLine(8, 115, WIDTH - 8, 115, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        if (theme == THEME_M3) {
            paint.setColor(Color.rgb(208, 188, 255)); // M3 浅紫
            canvas.drawText("IN: " + formatTokensCompact(data.inputTokens), 13, 106, paint);

            paint.setColor(Color.rgb(255, 180, 171)); // M3 柔粉
            canvas.drawText("OUT: " + formatTokensCompact(data.outputTokens), WIDTH / 2 + 7, 106, paint);

            paint.setColor(Color.rgb(168, 235, 178)); // M3 薄荷绿
            canvas.drawText("CACH: " + formatTokensCompact(data.cacheReadTokens), 13, 137, paint);

            paint.setColor(Color.rgb(255, 220, 193)); // M3 柔杏色
            canvas.drawText(String.format(Locale.US, "$%.2f", data.costUsd), WIDTH / 2 + 7, 137, paint);
        } else {
            paint.setColor(Color.rgb(0, 210, 255));
            canvas.drawText("IN: " + formatTokensCompact(data.inputTokens), 13, 106, paint);

            paint.setColor(Color.rgb(255, 0, 128));
            canvas.drawText("OUT: " + formatTokensCompact(data.outputTokens), WIDTH / 2 + 7, 106, paint);

            paint.setColor(Color.rgb(88, 255, 177));
            canvas.drawText("CACH: " + formatTokensCompact(data.cacheReadTokens), 13, 137, paint);

            paint.setColor(Color.rgb(255, 214, 102));
            canvas.drawText(String.format(Locale.US, "$%.2f", data.costUsd), WIDTH / 2 + 7, 137, paint);
        }

        // 5. 底部页脚
        paint.setColor(subTextColor);
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date(data.lastUpdateTime));
        canvas.drawText(data.localIp, 8, 163, paint);
        canvas.drawText(timeStr, WIDTH - 56, 163, paint);
    }

    private static String cleanModelName(String raw) {
        if (raw == null || raw.isEmpty()) return "AI Model";
        String lower = raw.toLowerCase(Locale.US);
        if (lower.contains("gemini-3.8-flash") || lower.contains("flash-high")) {
            return "Gemini 3.8 Flash";
        } else if (lower.contains("gpt-5.6-sol") || lower.contains("gpt-5.6")) {
            return "GPT-5.6 Sol";
        } else if (lower.contains("claude-3-5-sonnet") || lower.contains("claude-3.5")) {
            return "Claude 3.5 Sonnet";
        } else if (lower.contains("gpt-4o")) {
            return "GPT-4o";
        }
        if (raw.contains("/")) {
            raw = raw.substring(raw.lastIndexOf('/') + 1);
        }
        if (raw.length() > 16) {
            return raw.substring(0, 15) + "…";
        }
        return raw;
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

    private static String formatTokensCompact(long tokens) {
        if (tokens >= 1_000_000) {
            return String.format(Locale.US, "%.2fM", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format(Locale.US, "%.1fK", tokens / 1_000.0);
        } else {
            return String.valueOf(tokens);
        }
    }
}
