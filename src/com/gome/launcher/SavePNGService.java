package com.gome.launcher;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.gallery3d.common.BitmapUtils;

/**
 * Created by admin on 2018/1/13.
 */

public class SavePNGService extends IntentService{

    public SavePNGService() {
        /**
         * Creates an IntentService.  Invoked by your subclass's constructor.
         *
         * @param name Used to name the worker thread, important only for debugging.
         */
        super("SavePNGService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        BitmapUtils.savePNGAsFile(LauncherApplication.getAppContext());
    }


}
