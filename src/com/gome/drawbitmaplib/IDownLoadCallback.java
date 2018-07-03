package com.gome.drawbitmaplib;

import android.graphics.Bitmap;

/**
 * Created by admin on 2017/7/31.
 */

public interface IDownLoadCallback {
    void onReady();

    void onIconReady(Bitmap bitmap);

    void onStart(String appName, String pkgName, String iconUrl);

    void onPause(String appName, String pkgName);

    void onResume(String appName, String pkgName);

    void onProcess(String appName, String pkgName, long process);

    void onInstalling(String pkgName, float process);

    void onInstalled(String pkgName);

    void onCanceled(String appName, String pkgName);

    void onComplete(String appName, String pkgName);

    void onDownloadError(String appName, String pkgName, String err);

    void onInstallError(String appName, String pkgName, String err);

    void onSucceed(String appName);
}
