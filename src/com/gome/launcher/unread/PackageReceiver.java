package com.gome.launcher.unread;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherApplication;
import com.gome.launcher.packagecircle.PackageCircle;

/**
 * Created by gaoquan
 */

/**
 * update unread badge app if app install or uninstall
 */
public class PackageReceiver extends BroadcastReceiver {

    private static final String TAG = "PackageReceiver";
    private BadgeUnreadLoader mBadgeUnreadLoader;
    /**
     * Added by gaoquan 2017.7.20
     */
    //-------------------------------start--------------///
    private PackageCircle mPackageCircle;
    //-------------------------------end--------------///

    @Override
    public void onReceive(Context context, Intent intent) {
        Launcher launcher = ((LauncherApplication)context.getApplicationContext())
                .getLauncher();
        if (launcher != null && intent != null) {
            mBadgeUnreadLoader = launcher.getUnreadLoader();
            /**
             * Added by gaoquan 2017.7.20
             */
            //-------------------------------start--------------///
            mPackageCircle = launcher.getPackageCircle();
//            if (Intent.ACTION_PACKAGE_CHANGED.equals(intent.getAction())) {
//                if (intent.getData() != null) {
//                    String packageName = intent.getData().getSchemeSpecificPart();
//                    if(mPackageCircle != null){
//                        mPackageCircle.updateCircleChanged(packageName);
//                    }
//                }
//            }
            if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
                if (intent.getData() != null) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    Log.i(TAG, "ACTION_PACKAGE_REPLACED packageName== " + packageName);
                    if(mPackageCircle != null){
                        mPackageCircle.updateCircleChanged(packageName);
                    }
                }
            }
            //-------------------------------end--------------///
            if (mBadgeUnreadLoader != null) {
                if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
                    if (intent.getData() != null) {
                        String packageName = intent.getData().getSchemeSpecificPart();
                        Log.i(TAG, "ACTION_PACKAGE_ADDED packageName== " + packageName);
                        mBadgeUnreadLoader.addUnreadSupportByPackageName(packageName);
                        /**
                         * Added by gaoquan 2017.7.20
                         */
                        //-------------------------------start--------------///
                        if(mPackageCircle != null){
                            mPackageCircle.mNewApp = packageName;
                        }
                        //-------------------------------end--------------///
                    }
                }
                if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
                    if (intent.getData() != null) {
                        String packageName = intent.getData().getSchemeSpecificPart();
                        Log.i(TAG, "ACTION_PACKAGE_REMOVED packageName== " + packageName);
                        mBadgeUnreadLoader.deleteUnreadSupportByPackageName(packageName);
                    }
                }
            }
        }
    }
}
