package com.gome.launcher.util;

import android.util.Log;

/**
 * Created by linhai on 2017/6/19.
 */
public class DLog {
    private static final int VERBOSE = 1;
    private static final int DEBUG = 2;
    private static final int INFO = 3;
    private static final int WARN = 4;
    private static final int ERROR = 5;

    public static final boolean DEBUGABLE = false;
    private static final int LOG_LEVEL = VERBOSE;


    public static void v(String tag, String msg) {
        if (DEBUGABLE && VERBOSE >= LOG_LEVEL) Log.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (DEBUGABLE && DEBUG >= LOG_LEVEL) Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (DEBUGABLE && INFO >= LOG_LEVEL) Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (DEBUGABLE && WARN >= LOG_LEVEL) Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (DEBUGABLE && ERROR >= LOG_LEVEL) Log.e(tag, msg);
    }

}
