package com.gome.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

    static final String SYSTEM_READY = "com.gome.launcher.SYSTEM_READY";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("StartupReceiver", "SDCARD_TEST StartupReceiver onReceive BOOT_COMPLETE intent:"+intent);
        context.sendStickyBroadcast(new Intent(SYSTEM_READY));
    }
}
