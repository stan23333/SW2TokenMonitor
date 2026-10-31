package com.tokenmonitor.sw2;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

import java.util.Locale;

public class TokenExtensionService extends ExtensionService {

    public static final String EXTENSION_KEY = "com.tokenmonitor.sw2.key";
    private static TokenControlExtension sCurrentControl;

    public TokenExtensionService() {
        super(EXTENSION_KEY);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocalHttpServer.getInstance(this).start();
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new TokenRegistrationInformation(this);
    }

    @Override
    protected boolean keepRunningWhenConnected() {
        return true;
    }

    @Override
    public ControlExtension createControlExtension(String hostAppPackageName) {
        LocalHttpServer.getInstance(this).start();
        sCurrentControl = new TokenControlExtension(this, hostAppPackageName);
        return sCurrentControl;
    }

    public static TokenControlExtension getCurrentControl() {
        return sCurrentControl;
    }

    public static String getLocalIpAddress(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo winfo = wm.getConnectionInfo();
                int ip = winfo.getIpAddress();
                if (ip != 0) {
                    return String.format(Locale.US, "%d.%d.%d.%d",
                            (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
                }
            }
        } catch (Exception ignored) {}
        return "192.168.0.11";
    }
}
