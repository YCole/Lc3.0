package com.hc.sh.appcenter;
import com.hc.sh.appcenter.AppDataForEx;

interface IForExListener{
  void onStart(String appName,String pkgName,String iconUrl,boolean isUpdate);
  void onPause(String appName,String pkgName,String iconUrl);
  void onResume(String appName,String pkgName,String iconUrl);
  void onProcess(String appName,String pkgName,long process,String iconUrl,boolean isUpdate);
  void onInstalling(String appName,String pkgName);
  void onInstalled(String appName,String pkgName);
  void onCanceled(String appName,String pkgName);
  void onComplete(String appName,String pkgName);
  void onDownloadError(String appName,String pkgName,String err);
  void onInstallError(String appName,String pkgName,String err);

  void onProcessAll(in AppDataForEx[] data);
}
