package com.gome.launcher;

import android.app.ActivityManager;
import android.app.IconPackHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ThemeConfig;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.gome.launcher.util.DisplayMetricsUtils;
import com.mediatek.launcher3.LauncherLog;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by huanghaihao on 2017/9/11.
 * 主題助手
 */

public class ThemeHelper {
    // 系统默认主题
    public static final String THEME_DEFAULT = "com.gometheme.defaulttheme";
    //乐章主题
    public static final String THEME_RITMO = "com.gometheme.ritmo";
    //视野主题
    public static final String THEME_VISION = "com.gometheme.vision";
    //憧憬主题
    public static final String THEME_HOPE = "com.gometheme.hope";

    private static final String TAG = "ThemeHelper";
    //计算出的左右边距
    private static final float RELATIVE_BG_LEFT = 7f;
    //计算出的上下边距
    private static final float RELATIVE_BG_TOP = 2f;
    //整个图片的大小56dp
    public static final float WHOLE_SIZE = 56f;
    //default
    private static final float DEFAULT_DISPLAY_SIZE = 50f;
    private static final float DEFAULT_CORNER_SIZE = 12f;
    private static final float DEFAULT_ENLARGE_SIZE = 51f;
    private static final float DEFAULT_SHRINK_SIZE = 38f;
    //ritmo
    private static final float RITMO_DISPLAY_SIZE = 52f;
    private static final float RITMO_CORNER_SIZE = 12f;
    private static final float RITMO_ENLARGE_SIZE = 53f;
    private static final float RITMO_SHRINK_SIZE = 38f;
    //vision
    private static final float VISION_DISPLAY_SIZE = 52f;
    private static final float VISION_CORNER_SIZE = 12f;
    private static final float VISION_ENLARGE_SIZE = 53f;
    private static final float VISION_SHRINK_SIZE = 38f;
    //hope
    private static final float HOPE_DISPLAY_SIZE = 52f;
    private static final float HOPE_CORNER_SIZE = 12f;
    private static final float HOPE_ENLARGE_SIZE = 53f;
    private static final float HOPE_SHRINK_SIZE = 38f;
    private static ThemeHelper sThemeHelper;
    private Context mContext;
    private IconPackHelper mIconPackHelper;
    private ThemeInfo mThemeInfo;

    private SharedPreferences mSharedPrefs;



    public static ThemeHelper getInstance(Context context) {
        if (null == sThemeHelper) {
            sThemeHelper = new ThemeHelper(context);
        }
        return sThemeHelper;
    }

    private ThemeHelper(Context context) {
        mContext = context;
        mSharedPrefs = Utilities.getPrefs(mContext);
        mIconPackHelper = new IconPackHelper(context);
        getThemeInfo();
    }

    public  boolean getThemeStateChanged() {
        String themeStateSp = mSharedPrefs.getString(Utilities.THEME_NAME,THEME_DEFAULT );
        String curTheme = getCurTheme();

            if (themeStateSp.equals(curTheme))
            {
                return false;
            }else
            {
                SharedPreferences.Editor editor = mSharedPrefs.edit();
                editor.putString(Utilities.THEME_NAME,curTheme);
                editor.apply();
                return true;
            }
    }

    public ThemeInfo getThemeInfo() {
        if (null == mThemeInfo) {
            mThemeInfo = new ThemeInfo();

            String themePkg = getCurTheme();
            mThemeInfo.themePkg = themePkg;
            mThemeInfo.flag = ThemeInfo.FLAG_ONLY;

            String rootPath = getThemeRootPath(themePkg);
            mThemeInfo.toolIcon = rootPath + "/gome_bg_launcher_app.png";
            mThemeInfo.folderIcon = rootPath + "/gome_icon_launcher_folder.png";
            mThemeInfo.addIcon = rootPath + "/gome_icon_launcher_folder_addnew.png";
            mThemeInfo.downloadEmptyIcon = rootPath + "/gome_icon_appdownload_empty.png";
            mThemeInfo.clockBackground = rootPath + "/gome_icon_launcher_clock_bg.png";
            mThemeInfo.clock_pointer_hour = rootPath + "/launcher_clock_hour.png";
            mThemeInfo.clock_pointer_minute = rootPath + "/launcher_clock_minute.png";
            mThemeInfo.clock_pointer_second = rootPath + "/launcher_clock_second.png";
            mThemeInfo.calendarBgIcon = rootPath + "/gome_icon_launcher_calendar_bg.png";
            mThemeInfo.calendarNum_0 = rootPath + "/calendar_num_0.png";
            mThemeInfo.calendarNum_1 = rootPath + "/calendar_num_1.png";
            mThemeInfo.calendarNum_2 = rootPath + "/calendar_num_2.png";
            mThemeInfo.calendarNum_3 = rootPath + "/calendar_num_3.png";
            mThemeInfo.calendarNum_4 = rootPath + "/calendar_num_4.png";
            mThemeInfo.calendarNum_5 = rootPath + "/calendar_num_5.png";
            mThemeInfo.calendarNum_6 = rootPath + "/calendar_num_6.png";
            mThemeInfo.calendarNum_7 = rootPath + "/calendar_num_7.png";
            mThemeInfo.calendarNum_8 = rootPath + "/calendar_num_8.png";
            mThemeInfo.calendarNum_9 = rootPath + "/calendar_num_9.png";

            switch (themePkg) {
                case THEME_RITMO:
                    mThemeInfo.displaySize = DisplayMetricsUtils.dip2px(RITMO_DISPLAY_SIZE);
                    mThemeInfo.cornerSize = DisplayMetricsUtils.dip2px(RITMO_CORNER_SIZE);
                    mThemeInfo.shrinkSize = DisplayMetricsUtils.dip2px(RITMO_SHRINK_SIZE);
                    mThemeInfo.enlargeSize = DisplayMetricsUtils.dip2px(RITMO_ENLARGE_SIZE);
                    break;
                case THEME_VISION:
                    mThemeInfo.displaySize = DisplayMetricsUtils.dip2px(VISION_DISPLAY_SIZE);
                    mThemeInfo.cornerSize = DisplayMetricsUtils.dip2px(VISION_CORNER_SIZE);
                    mThemeInfo.enlargeSize = DisplayMetricsUtils.dip2px(VISION_ENLARGE_SIZE);
                    mThemeInfo.shrinkSize = DisplayMetricsUtils.dip2px(VISION_SHRINK_SIZE);
                    //mThemeInfo.flag = ThemeInfo.FLAG_ONLY_MASK;
                    break;
                case THEME_HOPE:
                    mThemeInfo.displaySize = DisplayMetricsUtils.dip2px(HOPE_DISPLAY_SIZE);
                    mThemeInfo.cornerSize = DisplayMetricsUtils.dip2px(HOPE_CORNER_SIZE);
                    mThemeInfo.shrinkSize = DisplayMetricsUtils.dip2px(HOPE_SHRINK_SIZE);
                    mThemeInfo.enlargeSize = DisplayMetricsUtils.dip2px(HOPE_ENLARGE_SIZE);
                    break;
                case THEME_DEFAULT:
                default:
                    mThemeInfo.displaySize = DisplayMetricsUtils.dip2px(DEFAULT_DISPLAY_SIZE);
                    mThemeInfo.cornerSize = DisplayMetricsUtils.dip2px(DEFAULT_CORNER_SIZE);
                    mThemeInfo.shrinkSize = DisplayMetricsUtils.dip2px(DEFAULT_SHRINK_SIZE);
                    mThemeInfo.enlargeSize = DisplayMetricsUtils.dip2px(DEFAULT_ENLARGE_SIZE);
                    break;
            }
            LauncherLog.d(TAG, "ThemeInfo>" + mThemeInfo.toString());
        }
        return mThemeInfo;
    }

    /**
     * 获取系统中当前配置主题 所对应的Icons 所对应的package
     *
     * @return
     */
    public String getCurTheme() {
        ThemeConfig currentTheme = mContext.getResources().getConfiguration().themeConfig;
        if (currentTheme != null) {
            currentTheme = (ThemeConfig) currentTheme.clone();
        } else {
            currentTheme = ThemeConfig.getBootTheme(mContext.getContentResolver());
        }
        Log.i("ThemeHelper", "currentTheme="+currentTheme.getIconPackPkgName());
        return currentTheme.getIconPackPkgName();
    }

    /**
     * 获取应用包名 是否有在当前主题中是否有对应的theme icon
     *
     * @param componentName
     * @return
     */
    public boolean hasThemeIcon(ComponentName componentName) {
        ActivityInfo info = new ActivityInfo();
        try {
            info.packageName = componentName.getPackageName();
            info.name = componentName.getClassName();
            mIconPackHelper.loadIconPack(mThemeInfo.themePkg);
        } catch (PackageManager.NameNotFoundException e) {
            LauncherLog.e(TAG, e.getMessage(), e);
        }
        //如果返回的resourceId == 0 ，则表示当前的iconpack中没有该Activity对应的theme icon
        boolean hasTheme = 0 != mIconPackHelper.getThemedActivityIconOnly(info);
        LauncherLog.d(TAG, "componentName:" + componentName + ",hasThemeIcon:" + hasTheme);
        return hasTheme;
    }

    public Bitmap getThemeIcon(ComponentName componentName) {
        ActivityInfo info = new ActivityInfo();
        try {
            info.packageName = componentName.getPackageName();
            info.name = componentName.getClassName();
            mIconPackHelper.loadIconPack(mThemeInfo.themePkg);
        } catch (PackageManager.NameNotFoundException e) {
            LauncherLog.e(TAG, e.getMessage(), e);
        }
        int resId = mIconPackHelper.getThemedActivityIconOnly(info);
        if(resId > 0){
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            int densi = am.getLauncherLargeIconDensity();
            BitmapDrawable bd = (BitmapDrawable)mContext.getResources().getDrawableForDensity(resId, densi);
            return bd.getBitmap();
        }
        return null;
    }

    public boolean isDefaultTheme() {
        return THEME_DEFAULT.equals(mThemeInfo.themePkg);
    }

    public Bitmap getThemeRes(String path) {
        Bitmap bitmap = null;
        InputStream inputStream = null;
        try {
            inputStream = mContext.getAssets().open(path);
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (Throwable e) {
            LauncherLog.e(TAG, e.getMessage(), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LauncherLog.e(TAG, e.getMessage(), e);
                }
            }
        }
        return bitmap;
    }

    /**
     * 获取蒙板
     *
     * @return
     */
    public Bitmap getToolIcon() {
        return getThemeRes(mThemeInfo.toolIcon);
    }

    /**
     * 获取文件夹背景
     *
     * @return
     */
    public Bitmap getFolderIcon() {
        return getThemeRes(mThemeInfo.folderIcon);
    }

    /**
     * 下载的蒙板
     *
     * @return
     */
    public Bitmap getDownloadEmptyIcon() {
        return getThemeRes(mThemeInfo.downloadEmptyIcon);
    }

    /**
     * 获取文件夹+
     *
     * @return
     */
    public Bitmap getAddIcon() {
        return getThemeRes(mThemeInfo.addIcon);
    }

    public Bitmap getCalendarBgIcon() {
        return getThemeRes(mThemeInfo.calendarBgIcon);
    }

    public Bitmap getCalendarNum_0() {
        return getThemeRes(mThemeInfo.calendarNum_0);
    }

    public Bitmap getCalendarNum_1() {
        return getThemeRes(mThemeInfo.calendarNum_1);
    }

    public Bitmap getCalendarNum_2() {
        return getThemeRes(mThemeInfo.calendarNum_2);
    }

    public Bitmap getCalendarNum_3() {
        return getThemeRes(mThemeInfo.calendarNum_3);
    }

    public Bitmap getCalendarNum_4() {
        return getThemeRes(mThemeInfo.calendarNum_4);
    }

    public Bitmap getCalendarNum_5() {
        return getThemeRes(mThemeInfo.calendarNum_5);
    }

    public Bitmap getCalendarNum_6() {
        return getThemeRes(mThemeInfo.calendarNum_6);
    }

    public Bitmap getCalendarNum_7() {
        return getThemeRes(mThemeInfo.calendarNum_7);
    }

    public Bitmap getCalendarNum_8() {
        return getThemeRes(mThemeInfo.calendarNum_8);
    }

    public Bitmap getCalendarNum_9() {
        return getThemeRes(mThemeInfo.calendarNum_9);
    }

    public Bitmap getClockBackground() {
        return getThemeRes(mThemeInfo.clockBackground);
    }

    public BitmapDrawable getClockPointerHour() {
        Bitmap bitmap = getThemeRes(mThemeInfo.clock_pointer_hour);
        BitmapDrawable bitmapDrawable = bitmapChangeToBitmapDrawable(bitmap);
        return bitmapDrawable;
    }
    public BitmapDrawable getClockPointerMinute() {
        Bitmap bitmap = getThemeRes(mThemeInfo.clock_pointer_minute);
        BitmapDrawable bitmapDrawable = bitmapChangeToBitmapDrawable(bitmap);
        return bitmapDrawable;
    }
    public BitmapDrawable getClockPointerSecond() {
        Bitmap bitmap = getThemeRes(mThemeInfo.clock_pointer_second);
        BitmapDrawable bitmapDrawable = bitmapChangeToBitmapDrawable(bitmap);
        return bitmapDrawable;
    }

    public static BitmapDrawable bitmapChangeToBitmapDrawable(Bitmap bitmap) {
        BitmapDrawable bitmapDrawable = new BitmapDrawable(LauncherApplication.getAppContext().getResources(),bitmap);
        bitmapDrawable.setMipMap(true);
        bitmapDrawable.setAntiAlias(true);
        return bitmapDrawable;
    }

    /**
     * 获取主题根路径
     *
     * @return
     */
    public String getThemeRootPath(String themePkg) {
        int densityDpi = mContext.getResources().getDisplayMetrics().densityDpi;
        String path = DisplayMetrics.DENSITY_XXXHIGH == densityDpi ? "xxxhdpi" :
                (DisplayMetrics.DENSITY_XHIGH == densityDpi ? "xhdpi" : "xxhdpi");
        switch (themePkg) {
            case THEME_RITMO:
                path += "/themeimgs/ritmo";
                break;
            case THEME_VISION:
                path += "/themeimgs/vision";
                break;
            case THEME_HOPE:
                path += "/themeimgs/hope";
                break;
            case THEME_DEFAULT:
            default:
                path += "/themeimgs/default";
                break;
        }
        return path;
    }

    /**
     * 是否是icon是否有底板
     * Added by huanghaihao in 2017-6-29 for cut iconbitmap
     *
     * @param srcBitmap icon
     * @return
     */
    public boolean checkImageHasOwnBg(Bitmap srcBitmap) {
        //获取原图的大小相对放大后尺寸的比例
        float srcHeightEnlarge = srcBitmap.getHeight() / DisplayMetricsUtils.dip2px(WHOLE_SIZE);
        float srcWidthEnlarge = srcBitmap.getWidth() / DisplayMetricsUtils.dip2px(WHOLE_SIZE);
        // 根据比例取计算出和放大后图片一样大小的四个顶角的像素点
        float top = DisplayMetricsUtils.dip2px(RELATIVE_BG_LEFT) * srcHeightEnlarge - 1 /*+ (mThemeInfo.enlargeSize - mThemeInfo.displaySize) * srcHeightEnlarge*/;
        float left = DisplayMetricsUtils.dip2px(RELATIVE_BG_LEFT) * srcWidthEnlarge - 1 /*+ (mThemeInfo.enlargeSize - mThemeInfo.displaySize) * srcWidthEnlarge*/;
        float right = srcBitmap.getWidth() - left;
        float bottom = srcBitmap.getHeight() - top;
        int leftTopPixel = srcBitmap.getPixel((int) left, (int) top) >> 24 & 0XFF;
        int rightTopPixel = srcBitmap.getPixel((int) right, (int) top) >> 24 & 0XFF;
        int leftBottomPixel = srcBitmap.getPixel((int) left, (int) bottom) >> 24 & 0XFF;
        int rightBottomPixel = srcBitmap.getPixel((int) right, (int) bottom) >> 24 & 0XFF;
        // 根据比例取计算出和放大后图片一样大小的四个中心点的像素点
        float topCenter = DisplayMetricsUtils.dip2px(RELATIVE_BG_TOP) * srcHeightEnlarge;
        float leftCenter = DisplayMetricsUtils.dip2px(RELATIVE_BG_TOP) * srcHeightEnlarge;
        float rightCenter = srcBitmap.getWidth() - leftCenter - 1;
        float bottomCenter = srcBitmap.getHeight() - topCenter - 1;
        int leftCenterPixel = srcBitmap.getPixel((int) leftCenter, (int) (srcBitmap.getHeight() / 2f)) >> 24 & 0XFF;
        int topCenterPixel = srcBitmap.getPixel((int) topCenter, (int) (srcBitmap.getWidth() / 2f)) >> 24 & 0XFF;
        int rightCenterPixel = srcBitmap.getPixel((int) rightCenter, (int) (srcBitmap.getHeight() / 2f)) >> 24 & 0XFF;
        int bottomCenterPixel = srcBitmap.getPixel((int) bottomCenter, (int) (srcBitmap.getWidth() / 2f)) >> 24 & 0XFF;
        //判断8个点全部不是透明则认为源图有背板
        if (leftTopPixel > 0 && rightTopPixel > 0 && leftBottomPixel > 0 && rightBottomPixel > 0
                && bottomCenterPixel > 0 && rightCenterPixel > 0 && topCenterPixel > 0 && leftCenterPixel > 0) {
            return true;
        }
        return false;
    }

    public static class ThemeInfo {
        public static final int FLAG_ONLY = 0;
        public static final int FLAG_ONLY_MASK = 1;
        public static final int FLAG_ONLY_BASE = 2;
        public static final int FLAG_BASE_MASK = 3;
        public static final int FLAG_MASK_BASE = 4;
        int flag;
        //主题包
        String themePkg;
        //底板/蒙板
        String toolIcon;
        //文件夹
        String folderIcon;
        //文件夹中的+
        String addIcon;
        //图片尺寸
        float toolSize = DisplayMetricsUtils.dip2px(WHOLE_SIZE);
        //显示的尺寸
        float displaySize;
        //显示的圆角尺寸
        float cornerSize;
        //缩小的尺寸
        float shrinkSize;
        //放大尺寸
        float enlargeSize;
       
        //下载的空icon
        String downloadEmptyIcon;

        String clockBackground;
        String clock_pointer_hour;
        String clock_pointer_minute;
        String clock_pointer_second;

        String calendarBgIcon;
        String calendarNum_0;
        String calendarNum_1;
        String calendarNum_2;
        String calendarNum_3;
        String calendarNum_4;
        String calendarNum_5;
        String calendarNum_6;
        String calendarNum_7;
        String calendarNum_8;
        String calendarNum_9;

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ThemeInfo{");
            sb.append("flag=").append(flag);
            sb.append(", themePkg='").append(themePkg).append('\'');
            sb.append(", toolIcon='").append(toolIcon).append('\'');
            sb.append(", folderIcon='").append(folderIcon).append('\'');
            sb.append(", addIcon='").append(addIcon).append('\'');
            sb.append(", toolSize=").append(toolSize);
            sb.append(", displaySize=").append(displaySize);
            sb.append(", cornerSize=").append(cornerSize);
            sb.append(", shrinkSize=").append(shrinkSize);
            sb.append(", enlargeSize=").append(enlargeSize);
            sb.append(", downloadEmptyIcon='").append(downloadEmptyIcon).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
