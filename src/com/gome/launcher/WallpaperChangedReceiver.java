/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gome.launcher;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.android.gallery3d.common.BitmapUtils;
import com.gome.launcher.util.WallpaperUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import static com.gome.launcher.util.WallpaperUtils.SAVED_FLAG;
import static com.gome.launcher.util.WallpaperUtils.SAVED_RES_ID;


public class WallpaperChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "WallpaperChangedReceiver";
    private static Timer sPrefsTimer = null;
    private static TimerTask sPrefsTimerTask = null;
    public void onReceive(final Context context, Intent data) {
        Intent intent = new Intent(context,SavePNGService.class);
        context.startService(intent);
        LauncherAppState.getInstance().onWallpaperChanged();
        int lastResId =  WallpaperUtils.getIntValue("systemWallpaperId");
        int resId = WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_SYSTEM);
        WallpaperUtils.setIntValue("systemWallpaperId",resId);
        cancelSPrefsTimer();
        sPrefsTimer = new Timer();
        sPrefsTimerTask = new TimerTask() {
            @Override
            public void run() {
                WallpaperUtils.setBooleanValue(SAVED_FLAG,false);
            }
        };
        sPrefsTimer.schedule(sPrefsTimerTask,500);
        if (lastResId != resId && !WallpaperUtils.getBooleanValue(SAVED_FLAG)) {
            WallpaperUtils.setIntValue(SAVED_RES_ID, -1);
        }

//        // Added by wjq in 2017-7-5 for wallpaper set start
//        boolean flag = WallpaperUtils.getBooleanValue(SAVED_FLAG);
//        Log.i(TAG," WallpaperChangedReceiver onReceive = " + flag);
//        // Added by wjq in 2017-7-5 for fix bug GMOS-1493 start
//        cancelSPrefsTimer();
//        if (flag) {
//
//            sPrefsTimer = new Timer();
//            sPrefsTimerTask = new TimerTask() {
//                @Override
//                public void run() {
//                    WallpaperUtils.setBooleanValue(SAVED_FLAG,false);
//                }
//            };
//            sPrefsTimer.schedule(sPrefsTimerTask,500);
//            // Added by wjq in 2017-7-5 for fix bug GMOS-1493 end
//        } else {
//
//            WallpaperUtils.setIntValue(SAVED_RES_ID,-1);
//        }
//        // Added by wjq in 2017-7-5 for wallpaper set end
    }

    private void cancelSPrefsTimer() {
        if (sPrefsTimer != null) {
            sPrefsTimer.cancel();
            sPrefsTimer = null;
        }

        if(sPrefsTimerTask != null) {
            sPrefsTimerTask.cancel();
            sPrefsTimerTask = null;
        }
    }

}
