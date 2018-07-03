package com.hc.sh.appcenter;
import com.hc.sh.appcenter.IForExListener;
import com.hc.sh.appcenter.AppDataForEx;
import com.hc.sh.appcenter.IForExStartListener;

interface IForExService{
    void startDownload(String pkgName);
    void continueDownload(String pkgName);
    void pauseDownload(String pkgName);
    void cancelDownload(String pkgName);
    void addListener(IForExListener listener);

    AppDataForEx[] getAllDownloadInfo();
    AppDataForEx getSpecDownloadInfo(String pkgName);

    boolean checkServiceStart(IForExStartListener listener);

    void deleteApkFiles(String pkgName);
}
