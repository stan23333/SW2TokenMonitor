package com.tokenmonitor.sw2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TokenExtensionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        intent.setClass(context, TokenExtensionService.class);
        context.startService(intent);
    }
}
