package com.gome.launcher.util;

/**
 * Created by jubingcheng on 2017/7/12.
 */

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DisplayMetricsUtils {
    private static final String TAG = "DisplayMetricsUtils";
    private static Activity activity;
    private static int deviceWidth = 0;
    private static int deviceHeight = 0;
    private static int displayWidth = 0;
    private static int displayHeight = 0;
    private static int statusBarHeight = 0;
	private static int navigationBarHeight = 0;    

    public static void init(Activity _activity) {
        activity = _activity;
    }

    /**
     * @return 状态栏固定高度（无论是否隐藏）
     */
    public static int getStatusBarStableHeight() {
        if (statusBarHeight != 0) {
            return statusBarHeight;
        }
        Resources resources = activity.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId == 0) {
            return 0;
        }
        try {
            statusBarHeight = resources.getDimensionPixelSize(resourceId);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    /**
     * @return 状态栏动态高度（隐藏时为0）
     */
    public static int getStatusBarDynamicHeight() {
        Rect rect = new Rect();
        activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getWindowVisibleDisplayFrame(rect);
        return rect.top;
    }

    /**
     * @return 导航栏固定高度（无论是否隐藏）
     */
    public static int getNavigationBarStableHeight() {
        if (navigationBarHeight != 0) {
            return navigationBarHeight;
        }
        Resources resources = activity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId == 0) {
            return 0;
        }
        try {
            navigationBarHeight = resources.getDimensionPixelSize(resourceId);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return navigationBarHeight;
    }

    /**
     * @return 设备实际宽度
     */
    public static int getDeviceWidth() {
        if (deviceWidth != 0) {
            return deviceWidth;
        }
        calibration();
        return deviceWidth;
    }

    /**
     * @return 设备实际高度
     */
    public static int getDeviceHeight() {
        if (deviceHeight != 0) {
            return deviceHeight;
        }
        calibration();
        return deviceHeight;
    }

    /**
     * @return 屏幕显示宽度
     */
    public static int getDisplayWidth() {
        if (displayWidth != 0) {
            return displayWidth;
        }
        calibration();
        return displayWidth;
    }

    /**
     * @return 屏幕显示高度（有导航栏的设备为设备高度-导航栏固定高度，无论导航栏是否隐藏）
     */
    public static int getDisplayHeight() {
        if (displayHeight != 0) {
            return displayHeight;
        }
        calibration();
        return displayHeight;
    }

    /**
     * 度量设备宽高
     */
    public static void calibration() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        try {
            Class.forName("android.view.Display").getMethod("getRealMetrics", new Class[]{DisplayMetrics.class}).invoke(display, new Object[]{dm});
            deviceWidth = Math.min(dm.widthPixels, dm.heightPixels);
            deviceHeight = Math.max(dm.widthPixels, dm.heightPixels);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Resources res = activity.getResources();
        displayWidth = Math.min(res.getDisplayMetrics().widthPixels, res.getDisplayMetrics().heightPixels);
        displayHeight = Math.max(res.getDisplayMetrics().widthPixels, res.getDisplayMetrics().heightPixels);

        Log.v(TAG, "calibration() device(" + deviceWidth + "," + deviceHeight + ") display(" + displayWidth + "," + displayHeight + ")" +
                " getStatusBarStableHeight:" + getStatusBarStableHeight() + " getNavigationBarStableHeight:" + getNavigationBarStableHeight()+ " hasNavigationBarByCompareHeight:" + hasNavigationBarByCompareHeight() + " hasNavigationBarByPermanentKey:" + hasNavigationBarByPermanentKey());
    }

    /**
     * @return 通过对比高度判断设备是否有导航栏（无论导航栏是否隐藏）
     */
    public static boolean hasNavigationBarByCompareHeight() {
        return getDeviceHeight() > getDisplayHeight();
    }

    /**
     * @return 通过物理按键判断设备是否有导航栏（无论导航栏是否隐藏）
     */
    public static boolean hasNavigationBarByPermanentKey() {
        boolean hasMenuKey = false;
        boolean hasBackKey = false;
        try {
            hasMenuKey = ViewConfiguration.get(activity).hasPermanentMenuKey();
        } catch (java.lang.NoSuchMethodError e) {
            // TODO: handle exception
        }
        try {
            hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        } catch (java.lang.NoSuchMethodError e) {
            // TODO: handle exception
        }
        Log.v(TAG, "checkDeviceHasNavigationBarByPermanentKey hasMenuKey:" + hasMenuKey + " hasBackKey:" + hasBackKey);
        if (!hasMenuKey && !hasBackKey) {
            return true;
        }
        return false;
    }

    /**
     * 透明状态栏和导航栏
     */
    public static void translucentStatusAndNavigationBar() {
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                activity.getWindow().getAttributes().systemUiVisibility |=
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                Field flagDrawsSystemBarBackgroundsField = WindowManager.LayoutParams.class.getField("FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS");
                activity.getWindow().addFlags(flagDrawsSystemBarBackgroundsField.getInt(null));
                Method setStatusBarColorMethod = Window.class.getDeclaredMethod("setStatusBarColor", int.class);
                setStatusBarColorMethod.invoke(activity.getWindow(), Color.TRANSPARENT);
                Method setNavigationBarColorMethod = Window.class.getDeclaredMethod("setNavigationBarColor", int.class);
                setNavigationBarColorMethod.invoke(activity.getWindow(), Color.TRANSPARENT);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "NoSuchFieldException while setting up transparent navigation");
            } catch (NoSuchMethodException e2) {
                Log.e(TAG, "NoSuchMethodException while setting up transparent navigation");
            } catch (IllegalAccessException e3) {
                Log.e(TAG, "IllegalAccessException while setting up transparent navigation");
            } catch (IllegalArgumentException e4) {
                Log.e(TAG, "IllegalArgumentException while setting up transparent navigation");
            } catch (InvocationTargetException e5) {
                Log.e(TAG, "InvocationTargetException while setting up transparent navigation");
            }
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 透明状态栏
     */
     public static void translucentStatusBar() {
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                activity.getWindow().getAttributes().systemUiVisibility |=
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                Field flagDrawsSystemBarBackgroundsField = WindowManager.LayoutParams.class.getField("FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS");
                activity.getWindow().addFlags(flagDrawsSystemBarBackgroundsField.getInt(null));
                Method setStatusBarColorMethod = Window.class.getDeclaredMethod("setStatusBarColor", int.class);
                setStatusBarColorMethod.invoke(activity.getWindow(), Color.TRANSPARENT);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "NoSuchFieldException while setting up transparent bars");
            } catch (NoSuchMethodException e2) {
                Log.e(TAG, "NoSuchMethodException while setting up transparent bars");
            } catch (IllegalAccessException e3) {
                Log.e(TAG, "IllegalAccessException while setting up transparent bars");
            } catch (IllegalArgumentException e4) {
                Log.e(TAG, "IllegalArgumentException while setting up transparent bars");
            } catch (InvocationTargetException e5) {
                Log.e(TAG, "InvocationTargetException while setting up transparent bars");
            }
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 透明导航栏
     */
    public static void translucentNavigationBar() {
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                activity.getWindow().getAttributes().systemUiVisibility |=
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                Field flagDrawsSystemBarBackgroundsField = WindowManager.LayoutParams.class.getField("FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS");
                activity.getWindow().addFlags(flagDrawsSystemBarBackgroundsField.getInt(null));
                Method setNavigationBarColorMethod = Window.class.getDeclaredMethod("setNavigationBarColor", int.class);
                setNavigationBarColorMethod.invoke(activity.getWindow(), Color.TRANSPARENT);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "NoSuchFieldException while setting up transparent navigation");
            } catch (NoSuchMethodException e2) {
                Log.e(TAG, "NoSuchMethodException while setting up transparent navigation");
            } catch (IllegalAccessException e3) {
                Log.e(TAG, "IllegalAccessException while setting up transparent navigation");
            } catch (IllegalArgumentException e4) {
                Log.e(TAG, "IllegalArgumentException while setting up transparent navigation");
            } catch (InvocationTargetException e5) {
                Log.e(TAG, "InvocationTargetException while setting up transparent navigation");
            }
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 隐藏状态栏
     */
    public static void hideStatusBar()
    {
//        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(hasNavigationBarByCompareHeight()){
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    /**
     * 显示状态栏
     */
    public static void showStatusBar()
    {
//        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (hasNavigationBarByCompareHeight()) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    /**
     * @return densityDpi
     * ldpi:    120     240x320 240x400 240x432
     * mdpi:    160     320x480
     * hdpi:    240     480x800 480x854 540x960
     * xhdpi:   320     720x1280
     * xxhdpi:  480     1080x1920
     * xxxhdpi: 640     2160x3840
     */
    public static int getDensityDpi() {
        return activity.getResources().getDisplayMetrics().densityDpi;
    }

    /**
     * @return density
     * ldpi:    0.75    240x320 240x400 240x432
     * mdpi:    1.0     320x480
     * hdpi:    1.5     480x800 480x854 540x960
     * xhdpi:   2.0     720x1280
     * xxhdpi:  3.0     1080x1920
     * xxxhdpi: 4.0     2160x3840
     */
    public static float getDensity() {
        return activity.getResources().getDisplayMetrics().density;
    }

    /**
     * @return scaledDensity for font
     */
    public static float getScaledDensity() {
        return activity.getResources().getDisplayMetrics().scaledDensity;
    }

    public static int dip2px(float dipValue) {
        return (int) (dipValue * getDensity() + 0.5f);
    }

    public static int px2dip(float pxValue) {
        return (int) (pxValue / getDensity() + 0.5f);
    }

    public static final int sp2px(float spValue) {
        return (int) (spValue * getScaledDensity() + 0.5f);
    }

    public static final int px2sp(float pxValue) {
        return (int) (pxValue / getScaledDensity() + 0.5f);
    }
}