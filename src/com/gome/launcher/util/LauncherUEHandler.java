/**
 * 应用程序异常类：用于捕获异常和提示错误信息
 *
 * @created by jubingcheng 2017-6-21
 */

package com.gome.launcher.util;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class LauncherUEHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "LauncherUEHandler";
    private static final String LOG_DIR = "/gome_launcher/";
    private static final String LOG_NAME = "error.log";
    private Application app;

    public LauncherUEHandler(Application app) {
        this.app = app;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String info = null;
        ByteArrayOutputStream baos = null;
        PrintStream ps = null;
        baos = new ByteArrayOutputStream();
        ps = new PrintStream(baos);
        ex.printStackTrace(ps);
        info = new String(baos.toByteArray());
        ps.close();
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("LAUNCHER_ERR", "Thread.getName()=" + thread.getName() + " id=" + thread.getId() + " state=" + thread.getState());
        Log.d("LAUNCHER_ERR", "Error[" + info + "]");
        saveErrorLog(info);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 保存异常错误日志 added by jubingcheng 2017-6-21
     * @param content
     */
    private void saveErrorLog(String content) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;//如果没有挂载SD卡，则无法写LOG
        }

        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + LOG_DIR + LOG_NAME;
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        } else if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        FileOutputStream fos = null;
        try {
            file.createNewFile();
            try {
                fos = new FileOutputStream(file);
                if (fos != null)
                {
                    try {
                        fos.write(content.getBytes());
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}