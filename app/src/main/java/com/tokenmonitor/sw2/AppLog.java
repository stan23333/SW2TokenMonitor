package com.tokenmonitor.sw2;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLog {

    private static final String TAG = "SW2TokenMonitor";
    private static File sLogFile;

    public static void init(Context context) {
        if (sLogFile == null) {
            sLogFile = new File(context.getExternalFilesDir(null), "sync.log");
        }
    }

    public static synchronized void d(String tag, String message) {
        Log.d(tag, message);
        appendToFile(tag, message);
    }

    public static synchronized void e(String tag, String message, Throwable tr) {
        Log.e(tag, message, tr);
        appendToFile(tag, message + (tr != null ? " | " + tr.getMessage() : ""));
    }

    private static void appendToFile(String tag, String message) {
        if (sLogFile == null) return;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            String line = "[" + sdf.format(new Date()) + "][" + tag + "] " + message + "\n";
            try (FileOutputStream fos = new FileOutputStream(sLogFile, true)) {
                fos.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }
}
