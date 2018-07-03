package com.gome.launcher;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.gome.launcher.util.LauncherUEHandler;

/**
 * Created by linhai on 2017/6/19.
 */
public class LauncherApplication extends Application {

    private static final String TAG = "LauncherApplication";

    /**
     * Added by gaoquan 2017.6.1
     */
    //-------------------------------start--------------///
    private static LauncherModel mLauncherModel;
    //-------------------------------end--------------///

    private static Launcher mLauncher;
    private static LauncherApplication instance;
    private static LauncherUEHandler launcherUEHandler;// added by jubingcheng for catch-Exception on 2017/6/21
    public static boolean needRestart = false;// added by jubingcheng for fix multi-launcher on 2017/9/7

    @Override
    public void onCreate() {
        Log.v(TAG, "PROCEDURE Application onCreate()");
        super.onCreate();
        // added by jubingcheng for diff-process protection start on 2017/9/7
        String process = getCurProcessName(this);
        Log.v(TAG, "PROCEDURE Application onCreate() process:"+process);
//        if (process!= null && !process.equals("com.gome.launcher")) {
//            return;
//        }
        // added by jubingcheng for diff-process protection end on 2017/9/7
        instance = this;
        // added by jubingcheng for catch-Exception start on 2017/6/21
        launcherUEHandler = new LauncherUEHandler(this);
        Thread.setDefaultUncaughtExceptionHandler(launcherUEHandler);
        // added by jubingcheng for catch-Exception end on 2017/6/21

    }

    String getCurProcessName(Context context) {
        int pid = Process.myPid();
        for (ActivityManager.RunningAppProcessInfo appProcess : ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningAppProcesses()) {
            if (appProcess != null) {
                if (appProcess.pid == pid && !TextUtils.isEmpty(appProcess.processName)) {
                    return appProcess.processName;
                }
            }
        }
        return null;
    }



    public static LauncherApplication getAppContext() {
        return instance;
    }

    public static Launcher getLauncher() {
        return mLauncher;
    }

    public static void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    /**
     * Added by gaoquan 2017.6.1
     */
    //-------------------------------start--------------///
    public static void setLauncherModel(LauncherModel launcherModel){
        mLauncherModel = launcherModel;
    }

    public static LauncherModel getLauncherModel(){
        return mLauncherModel;
    }
    //-------------------------------end--------------///

}
