package com.gome.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gome.launcher.util.WallpaperUtils;

public class ShutDownReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WallpaperUtils.setBooleanValue(WallpaperUtils.SAVED_FLAG,true);
    }
}
