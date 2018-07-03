package com.gome.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.gome.launcher.util.WallpaperUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gome.app.GomeAlertDialog;

/**
 * Utility class used to help set lockscreen wallpapers on N+.
 */
public class NycWallpaperUtils {

    /**
     * Calls cropTask.execute(), once the user has selected which wallpaper to set. On pre-N
     * devices, the prompt is not displayed since there is no API to set the lockscreen wallpaper.
     */
    public static void executeCropTaskAfterPrompt(
            final Activity activity, final AsyncTask<Integer, ?, ?> cropTask,
            DialogInterface.OnCancelListener onCancelListener, final ItemSelectListener itemSelectListener) {
        if (Utilities.ATLEAST_N) {
            if (!WallpaperUtils.GM_LAUNCHER) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.wallpaper_instructions)
                        .setItems(R.array.which_wallpaper_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedItemIndex) {
                                int whichWallpaper;
                                if (selectedItemIndex == 0) {
                                    whichWallpaper = WallpaperManager.FLAG_SYSTEM;
                                } else if (selectedItemIndex == 1) {
                                    whichWallpaper = WallpaperManager.FLAG_LOCK;
                                } else {
                                    whichWallpaper = WallpaperManager.FLAG_SYSTEM
                                            | WallpaperManager.FLAG_LOCK;
                                }
                                cropTask.execute(whichWallpaper);
                            }
                        })
                        .setOnCancelListener(onCancelListener)
                        .show();
            } else {
                // Added by wjq in 2017-09-07 for fox bug GMOS-4279 start
//                new GomeAlertDialog.Builder(activity)
//                        .setItems(R.array.gome_which_wallpaper_options, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int selectedItemIndex) {
//                                int whichWallpaper;
//                                if (selectedItemIndex == 0) {
//                                    whichWallpaper = WallpaperManager.FLAG_LOCK;
//                                } else if (selectedItemIndex == 1) {
//                                    whichWallpaper = WallpaperManager.FLAG_SYSTEM;
//                                } else {
//                                    whichWallpaper = WallpaperManager.FLAG_SYSTEM
//                                            | WallpaperManager.FLAG_LOCK;
//                                }
//                                cropTask.execute(whichWallpaper);
//                            }
//                        })
//                        .setOnCancelListener(onCancelListener)
//                        .show();
                // Added by wjq in 2017-09-07 for fox bug GMOS-4279 end
                WallpaperDialog wallpaperDialog = new WallpaperDialog(new ItemSelectListener() {
                    @Override
                    public void itemSelected(int selectedItemIndex) {
                        if (null != itemSelectListener) {
                            itemSelectListener.itemSelected(selectedItemIndex);
                        }
                        int whichWallpaper;
                        if (selectedItemIndex == 0) {
                            whichWallpaper = WallpaperManager.FLAG_LOCK;
                        } else if (selectedItemIndex == 1) {
                            whichWallpaper = WallpaperManager.FLAG_SYSTEM;
                        } else {
                            whichWallpaper = WallpaperManager.FLAG_SYSTEM
                                    | WallpaperManager.FLAG_LOCK;
                        }
                        cropTask.execute(whichWallpaper);
                        if (activity instanceof WallpaperPickerActivity) {
                            ( (WallpaperPickerActivity)activity).showLoadingContent();
                        }
                    }
                });
                wallpaperDialog.show(activity.getFragmentManager(), "wallpaperDialog");
                wallpaperDialog.setOnCancelListener(onCancelListener);
            }
        } else {
            cropTask.execute(WallpaperManager.FLAG_SYSTEM);
        }
    }

    public static void setStream(Context context, final InputStream data, Rect visibleCropHint,
            boolean allowBackup, int whichWallpaper) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        if (Utilities.ATLEAST_N) {
            wallpaperManager.setStream(data, visibleCropHint, allowBackup, whichWallpaper);
        } else {
            // Fall back to previous implementation (set system)
            wallpaperManager.setStream(data);
        }
    }

    public static void clear(Context context, int whichWallpaper) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        if (Utilities.ATLEAST_N) {
            wallpaperManager.clear(whichWallpaper);
        } else {
            // Fall back to previous implementation (clear system)
            wallpaperManager.clear();
        }
    }
}