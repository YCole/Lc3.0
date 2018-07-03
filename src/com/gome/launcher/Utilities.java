/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gome.launcher;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.gome.launcher.compat.UserHandleCompat;
import com.gome.launcher.config.FeatureFlags;
import com.gome.launcher.util.DisplayMetricsUtils;
import com.gome.launcher.util.IconNormalizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various utilities shared amongst the Launcher's classes.
 */
public final class Utilities {

    private static final String TAG = "Launcher.Utilities";
    //add for dynamic calender icon by louhan 20170714 start
    public static final String PACKAGE_NAME_CALENDAR = "com.android.calendar";
    public static final String PACKAGE_NAME_CLOCK = "hct.com.cn.alarmclock";
    //add for dynamic calender icon by louhan 20170714 end

    public static final boolean FR_SYSTEMUI_CLOCK = false;

    public static final int FLAG_EX_OPERATOR = 1 << 0;

    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();

    private static final Pattern sTrimPattern =
            Pattern.compile("^[\\s|\\p{javaSpaceChar}]*(.*)[\\s|\\p{javaSpaceChar}]*$");

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }
    static int sColors[] = { 0xffff0000, 0xff00ff00, 0xff0000ff };
    static int sColorIndex = 0;

    private static final int[] sLoc0 = new int[2];
    private static final int[] sLoc1 = new int[2];

    // TODO: use the full N name (e.g. ATLEAST_N*****) when available
    public static final boolean ATLEAST_N = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    public static final boolean ATLEAST_MARSHMALLOW =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    public static final boolean ATLEAST_LOLLIPOP_MR1 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;

    public static final boolean ATLEAST_LOLLIPOP =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    public static final boolean ATLEAST_KITKAT =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    public static final boolean ATLEAST_JB_MR1 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;

    public static final boolean ATLEAST_JB_MR2 =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;

    // These values are same as that in {@link AsyncTask}.
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    /**
     * An {@link Executor} to be used with async task with no limit on the queue size.
     */
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public static final String ALLOW_ROTATION_PREFERENCE_KEY = "pref_allowRotation";

    public static final String THEME_NAME = "thme";

    // Added by wjq in 2017-7-4 for hideOrShowSystemUIClock start
    public static final String CLOCK_WIDGET_NAME = "clock";
    public static final String SHORTCUT_WIDGET_CONTAINER = "ShortcutAndWidgetContainer";
    //Added by wjq in 2017-7-20 for packageNameUpdate start
    public static final String DESK_CLOCK_ACTION_NAME = "com.gome.launcher.clockwidget.changed";
    //Added by wjq in 2017-7-20 for packageNameUpdate end
    public static final String DESK_CLOCK_STATUS= "widget_status";
    // Added by wjq in 2017-7-4 for hideOrShowSystemUIClock end
    public static boolean isPropertyEnabled(String propertyName) {
        ///M ALPS02577089 return Log.isLoggable(propertyName, Log.VERBOSE);
        return "1".equals(android.os.SystemProperties.get(propertyName));
    }

    public static boolean isAllowRotationPrefEnabled(Context context) {
        boolean allowRotationPref = false;
        if (ATLEAST_N) {
            // If the device was scaled, used the original dimensions to determine if rotation
            // is allowed of not.
            int originalDensity = DisplayMetrics.DENSITY_DEVICE_STABLE;
            Resources res = context.getResources();
            int originalSmallestWidth = res.getConfiguration().smallestScreenWidthDp
                    * res.getDisplayMetrics().densityDpi / originalDensity;
            allowRotationPref = originalSmallestWidth >= 600;
        }
        return getPrefs(context).getBoolean(ALLOW_ROTATION_PREFERENCE_KEY, allowRotationPref);
    }

    public static Bitmap createIconBitmap(Cursor c, int iconIndex, Context context) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return createIconBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), context);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * linhai Get screen related information 2017/6/16
     * @param context
     * @return
     */
    public static DisplayMetrics getDisplayMetrics(Context context){
        WindowManager mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    /**
     * linhai 获得屏幕高度  add 2017/6/16
     */
    public static  int getScreenHeight(Context context)
    {
        return  Utilities.getDisplayMetrics(context).heightPixels;
    }


    /**
     * Returns a bitmap suitable for the all apps view. If the package or the resource do not
     * exist, it returns null.
     */
    public static Bitmap createIconBitmap(String packageName, String resourceName,
            Context context) {
        PackageManager packageManager = context.getPackageManager();
        // the resource
        try {
            Resources resources = packageManager.getResourcesForApplication(packageName);
            if (resources != null) {
                final int id = resources.getIdentifier(resourceName, null, null);
                //update by huanghaihao in 2017-9-7 for update icon start
//                return Utilities.createGomeIconBitmap(context, createIconBitmap(
//                        resources.getDrawableForDensity(id, LauncherAppState.getInstance()
//                                .getInvariantDeviceProfile().fillResIconDpi), context));
                return createIconBitmap(
                        resources.getDrawableForDensity(id, LauncherAppState.getInstance()
                                .getInvariantDeviceProfile().fillResIconDpi), context);
                //update by huanghaihao in 2017-9-7 for update icon start
            }
        } catch (Exception e) {
            // Icon not found.
        }
        return null;
    }

    /**
     * 定制以gome_bg为底板的icon图标
     * Added by huanghaihao in 2017-6-29 for cut iconbitmap
     *
     * @param context
     * @param srcBitmap 源图
     * @return
     */
    public static Bitmap createGomeIconBitmap(Context context, Bitmap srcBitmap) {
        if (null == srcBitmap) {
            return null;
        }
        ThemeHelper helper = ThemeHelper.getInstance(context);
        Bitmap icon = helper.getToolIcon();
        if (null == icon) {
            return srcBitmap;
        }
        ThemeHelper.ThemeInfo themeInfo = helper.getThemeInfo();
        Drawable toolDrawable = new BitmapDrawable(context.getResources(), icon.copy(Bitmap.Config.ARGB_8888, true));
        Drawable srcDrawable = new BitmapDrawable(context.getResources(), srcBitmap);
        icon.recycle();

        Bitmap toolIcon = createIconBitmap(toolDrawable, context, (int) themeInfo.toolSize);
        Bitmap checkBitmap = createIconBitmap(srcDrawable, context, DisplayMetricsUtils.dip2px(ThemeHelper.WHOLE_SIZE));
        Bitmap fixBitmap = createIconBitmap(srcDrawable, context, (int) themeInfo.enlargeSize);
        if (themeInfo.flag == ThemeHelper.ThemeInfo.FLAG_ONLY_MASK) {
            return combineOrClipBitmap(toolIcon, fixBitmap, PorterDuff.Mode.SRC_IN);
        }
        if (themeInfo.flag == ThemeHelper.ThemeInfo.FLAG_ONLY) {
            if (helper.checkImageHasOwnBg(checkBitmap)) {
                Bitmap cutBitmap = combineOrClipBitmap(toolIcon, fixBitmap, PorterDuff.Mode.SRC_IN);
                //Bitmap newToolIcon = createIconBitmap(toolDrawable, context, (int) themeInfo.toolSize);
                //return combineOrClipBitmap(newToolIcon, cutBitmap, PorterDuff.Mode.SRC_OVER);
                return cutBitmap;
            } else {
                srcBitmap = createIconBitmap(srcDrawable, context, (int) themeInfo.shrinkSize);
                return combineOrClipBitmap(toolIcon, srcBitmap, PorterDuff.Mode.SRC_OVER);
            }
        }
        //异常场景返回原图
        toolIcon.recycle();
        return srcBitmap;
    }

    /**
     * 加载背板/底板
     * @param toolIcon 背板
     * @param srcBitmap
     * @return
     */
    public static Bitmap combineOrClipBitmap(Bitmap toolIcon, Bitmap srcBitmap,
                                             PorterDuff.Mode mode) {
        Paint bitPaint = new Paint();
        bitPaint.setAntiAlias(true);
        Canvas canvas = new Canvas(toolIcon);
        canvas.drawBitmap(toolIcon, 0, 0, bitPaint);
        float translateX = (toolIcon.getWidth() - srcBitmap.getWidth()) / 2.0f;
        float translateY = (toolIcon.getHeight() - srcBitmap.getHeight()) / 2.0f;
        bitPaint.setXfermode(new PorterDuffXfermode(mode));
        canvas.drawBitmap(srcBitmap, translateX, translateY, bitPaint);
        bitPaint.setXfermode(null);
        canvas.setBitmap(null);
        if (!srcBitmap.isRecycled()) {
            srcBitmap.recycle();
            srcBitmap = null;
        }
        return toolIcon;
    }

    private static int getIconBitmapSize() {
        return LauncherAppState.getInstance().getInvariantDeviceProfile().iconBitmapSize;
    }

    /**
     * Returns a bitmap which is of the appropriate size to be displayed as an icon
     */
    public static Bitmap createIconBitmap(Bitmap icon, Context context) {
        final int iconBitmapSize = getIconBitmapSize();
        if (iconBitmapSize == icon.getWidth() && iconBitmapSize == icon.getHeight()) {
            return icon;
        }
        return createIconBitmap(new BitmapDrawable(context.getResources(), icon), context);
    }

    /**
     * Added by gaoquan 2017.11.11
     * 从系统中取图标时加保护
     */
    public static Bitmap getBadgedIconBitmap(
            Drawable icon, UserHandleCompat user, Context context) {
        try{
            return createBadgedIconBitmap(icon, user, context);
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return createBadgedIconBitmap(icon, user, context);
        }
    }
    
    /**
     * Returns a bitmap suitable for the all apps view. The icon is badged for {@param user}.
     * The bitmap is also visually normalized with other icons.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Bitmap createBadgedIconBitmap(
            Drawable icon, UserHandleCompat user, Context context) {
        float scale = FeatureFlags.LAUNCHER3_ICON_NORMALIZATION ?
                IconNormalizer.getInstance().getScale(icon) : 1;
        Bitmap bitmap = createIconBitmap(icon, context, scale);
        if (Utilities.ATLEAST_LOLLIPOP && user != null
                && !UserHandleCompat.myUserHandle().equals(user)) {
            BitmapDrawable drawable = new FixedSizeBitmapDrawable(bitmap);
            Drawable badged = context.getPackageManager().getUserBadgedIcon(
                    drawable, user.getUser());
            if (badged instanceof BitmapDrawable) {
                return ((BitmapDrawable) badged).getBitmap();
            } else {
                return createIconBitmap(badged, context);
            }
        } else {
            return bitmap;
        }
    }

    /**
     * Returns a bitmap suitable for the all apps view.
     */
    public static Bitmap createIconBitmap(Drawable icon, Context context) {
        return createIconBitmap(icon, context, 1.0f /* scale */);
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context, int iconSize) {
        synchronized (sCanvas) {
            final int iconBitmapSize = iconSize;

            int width = iconBitmapSize;
            int height = iconBitmapSize;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null && bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // Scale the icon proportionally to the icon dimensions
                final float ratio = (float) sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (width / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (height * ratio);
                }
            }

            // no intrinsic size --> use default size
            int textureWidth = iconBitmapSize;
            int textureHeight = iconBitmapSize;

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (textureWidth-width) / 2;
            final int top = (textureHeight-height) / 2;

            @SuppressWarnings("all") // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left+width, top+height, debugPaint);
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left+width, top+height);
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            icon.draw(canvas);
            canvas.restore();
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }

    /**
     * @param scale the scale to apply before drawing {@param icon} on the canvas
     */
    public static Bitmap createIconBitmap(Drawable icon, Context context, float scale) {
        synchronized (sCanvas) {
            final int iconBitmapSize = getIconBitmapSize();

            int width = iconBitmapSize;
            int height = iconBitmapSize;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null && bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // Scale the icon proportionally to the icon dimensions
                final float ratio = (float) sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (width / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (height * ratio);
                }
            }

            // no intrinsic size --> use default size
            int textureWidth = iconBitmapSize;
            int textureHeight = iconBitmapSize;

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (textureWidth-width) / 2;
            final int top = (textureHeight-height) / 2;

            @SuppressWarnings("all") // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left+width, top+height, debugPaint);
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left+width, top+height);
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.scale(scale, scale, textureWidth / 2, textureHeight / 2);
            icon.draw(canvas);
            canvas.restore();
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }
    // added by jubingcheng for test start on 2017/6/26

    /**
     * save bitmap for test.
     *
     * @param bitmap
     * @param name
     */
    public static void saveBmp(Bitmap bitmap, String name) {
        if (true)//change to [false] when you test. Do not commit
        {
            return;
        }
        File f = new File("/mnt/sdcard/" + name + ".png");
        try {
            f.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (fOut == null) {
            return;
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
            fOut.flush();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e22) {
            e22.printStackTrace();
        }
    }
    // added by jubingcheng for test end on 2017/6/26

    /**
     * Inverse of {@link #getDescendantCoordRelativeToParent(View, View, int[], boolean)}.
     */
    public static float mapCoordInSelfToDescendent(View descendant, View root,
                                                   int[] coord) {
        ArrayList<View> ancestorChain = new ArrayList<View>();

        float[] pt = {coord[0], coord[1]};

        View v = descendant;
        while(v != root) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);

        float scale = 1.0f;
        Matrix inverse = new Matrix();
        int count = ancestorChain.size();
        for (int i = count - 1; i >= 0; i--) {
            View ancestor = ancestorChain.get(i);
            View next = i > 0 ? ancestorChain.get(i-1) : null;

            pt[0] += ancestor.getScrollX();
            pt[1] += ancestor.getScrollY();

            if (next != null) {
                pt[0] -= next.getLeft();
                pt[1] -= next.getTop();
                next.getMatrix().invert(inverse);
                inverse.mapPoints(pt);
                scale *= next.getScaleX();
            }
        }

        coord[0] = (int) Math.round(pt[0]);
        coord[1] = (int) Math.round(pt[1]);
        return scale;
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in a parent view's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param root The root view to make the coordinates relative to.
     * @param coord The coordinate that we want mapped.
     * @param includeRootScroll Whether or not to account for the scroll of the descendant:
     *          sometimes this is relevant as in a child's coordinates within the descendant.
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     *         this scale factor is assumed to be equal in X and Y, and so if at any point this
     *         assumption fails, we will need to return a pair of scale factors.
     */
    public static float getDescendantCoordRelativeToParent(View descendant, View root,
                                                           int[] coord, boolean includeRootScroll) {
        ArrayList<View> ancestorChain = new ArrayList<View>();

        float[] pt = {coord[0], coord[1]};

        View v = descendant;
        while(v != root && v != null) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);

        float scale = 1.0f;
        int count = ancestorChain.size();
        for (int i = 0; i < count; i++) {
            View v0 = ancestorChain.get(i);
            // For TextViews, scroll has a meaning which relates to the text position
            // which is very strange... ignore the scroll.
            if (v0 != descendant || includeRootScroll) {
                pt[0] -= v0.getScrollX();
                pt[1] -= v0.getScrollY();
            }

            v0.getMatrix().mapPoints(pt);
            pt[0] += v0.getLeft();
            pt[1] += v0.getTop();
            scale *= v0.getScaleX();
        }

        coord[0] = (int) Math.round(pt[0]);
        coord[1] = (int) Math.round(pt[1]);
        return scale;
    }

    public static float getDescendantCoordRelativeToParentIngoreScale(View descendant, View root,
                                                           int[] coord) {
        ArrayList<View> ancestorChain = new ArrayList<View>();

        float[] pt = {coord[0], coord[1]};

        View v = descendant;
        while(v != root && v != null) {
            ancestorChain.add(v);
            v = (View) v.getParent();
        }
        ancestorChain.add(root);

        float scale = 1.0f;
        int count = ancestorChain.size();
        for (int i = 0; i < count; i++) {
            View v0 = ancestorChain.get(i);
            // For TextViews, scroll has a meaning which relates to the text position
            // which is very strange... ignore the scroll.
            if (v0 != descendant) {
                pt[0] -= v0.getScrollX();
                pt[1] -= v0.getScrollY();
            }
//            v0.getMatrix().mapPoints(pt);
            pt[0] += v0.getLeft();
            pt[1] += v0.getTop();
            scale *= v0.getScaleX();
        }

        coord[0] = (int) Math.round(pt[0]);
        coord[1] = (int) Math.round(pt[1]);
        return scale;
    }
    /**
     * Utility method to determine whether the given point, in local coordinates,
     * is inside the view, where the area of the view is expanded by the slop factor.
     * This method is called while processing touch-move events to determine if the event
     * is still within the view.
     */
    public static boolean pointInView(View v, float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < (v.getWidth() + slop) &&
                localY < (v.getHeight() + slop);
    }

    public static void scaleRect(Rect r, float scale) {
        if (scale != 1.0f) {
            r.left = (int) (r.left * scale + 0.5f);
            r.top = (int) (r.top * scale + 0.5f);
            r.right = (int) (r.right * scale + 0.5f);
            r.bottom = (int) (r.bottom * scale + 0.5f);
        }
    }

    public static int[] getCenterDeltaInScreenSpace(View v0, View v1, int[] delta) {
        v0.getLocationInWindow(sLoc0);
        v1.getLocationInWindow(sLoc1);

        sLoc0[0] += (v0.getMeasuredWidth() * v0.getScaleX()) / 2;
        sLoc0[1] += (v0.getMeasuredHeight() * v0.getScaleY()) / 2;
        sLoc1[0] += (v1.getMeasuredWidth() * v1.getScaleX()) / 2;
        sLoc1[1] += (v1.getMeasuredHeight() * v1.getScaleY()) / 2;

        if (delta == null) {
            delta = new int[2];
        }

        delta[0] = sLoc1[0] - sLoc0[0];
        delta[1] = sLoc1[1] - sLoc0[1];

        return delta;
    }

    public static void scaleRectAboutCenter(Rect r, float scale) {
        int cx = r.centerX();
        int cy = r.centerY();
        r.offset(-cx, -cy);
        Utilities.scaleRect(r, scale);
        r.offset(cx, cy);
    }

    public static void startActivityForResultSafely(
            Activity activity, Intent intent, int requestCode) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity.", e);
        }
    }

    static boolean isSystemApp(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        ComponentName cn = intent.getComponent();
        String packageName = null;
        if (cn == null) {
            ResolveInfo info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if ((info != null) && (info.activityInfo != null)) {
                packageName = info.activityInfo.packageName;
            }
        } else {
            packageName = cn.getPackageName();
        }
        if (packageName != null) {
            try {
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                return (info != null) && (info.applicationInfo != null) &&
                        ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            } catch (NameNotFoundException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isSystemApp(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        if (packageName != null ) {
            try {
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                return (info != null) && (info.applicationInfo != null) &&
                        ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            } catch (NameNotFoundException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * This picks a dominant color, looking for high-saturation, high-value, repeated hues.
     * @param bitmap The bitmap to scan
     * @param samples The approximate max number of samples to use.
     */
    static int findDominantColorByHue(Bitmap bitmap, int samples) {
        final int height = bitmap.getHeight();
        final int width = bitmap.getWidth();
        int sampleStride = (int) Math.sqrt((height * width) / samples);
        if (sampleStride < 1) {
            sampleStride = 1;
        }

        // This is an out-param, for getting the hsv values for an rgb
        float[] hsv = new float[3];

        // First get the best hue, by creating a histogram over 360 hue buckets,
        // where each pixel contributes a score weighted by saturation, value, and alpha.
        float[] hueScoreHistogram = new float[360];
        float highScore = -1;
        int bestHue = -1;

        for (int y = 0; y < height; y += sampleStride) {
            for (int x = 0; x < width; x += sampleStride) {
                int argb = bitmap.getPixel(x, y);
                int alpha = 0xFF & (argb >> 24);
                if (alpha < 0x80) {
                    // Drop mostly-transparent pixels.
                    continue;
                }
                // Remove the alpha channel.
                int rgb = argb | 0xFF000000;
                Color.colorToHSV(rgb, hsv);
                // Bucket colors by the 360 integer hues.
                int hue = (int) hsv[0];
                if (hue < 0 || hue >= hueScoreHistogram.length) {
                    // Defensively avoid array bounds violations.
                    continue;
                }
                float score = hsv[1] * hsv[2];
                hueScoreHistogram[hue] += score;
                if (hueScoreHistogram[hue] > highScore) {
                    highScore = hueScoreHistogram[hue];
                    bestHue = hue;
                }
            }
        }

        SparseArray<Float> rgbScores = new SparseArray<Float>();
        int bestColor = 0xff000000;
        highScore = -1;
        // Go back over the RGB colors that match the winning hue,
        // creating a histogram of weighted s*v scores, for up to 100*100 [s,v] buckets.
        // The highest-scoring RGB color wins.
        for (int y = 0; y < height; y += sampleStride) {
            for (int x = 0; x < width; x += sampleStride) {
                int rgb = bitmap.getPixel(x, y) | 0xff000000;
                Color.colorToHSV(rgb, hsv);
                int hue = (int) hsv[0];
                if (hue == bestHue) {
                    float s = hsv[1];
                    float v = hsv[2];
                    int bucket = (int) (s * 100) + (int) (v * 10000);
                    // Score by cumulative saturation * value.
                    float score = s * v;
                    Float oldTotal = rgbScores.get(bucket);
                    float newTotal = oldTotal == null ? score : oldTotal + score;
                    rgbScores.put(bucket, newTotal);
                    if (newTotal > highScore) {
                        highScore = newTotal;
                        // All the colors in the winning bucket are very similar. Last in wins.
                        bestColor = rgb;
                    }
                }
            }
        }
        return bestColor;
    }

    /*
     * Finds a system apk which had a broadcast receiver listening to a particular action.
     * @param action intent action used to find the apk
     * @return a pair of apk package name and the resources.
     */
    static Pair<String, Resources> findSystemApk(String action, PackageManager pm) {
        final Intent intent = new Intent(action);
        for (ResolveInfo info : pm.queryBroadcastReceivers(intent, 0)) {
            if (info.activityInfo != null &&
                    (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                final String packageName = info.activityInfo.packageName;
                try {
                    final Resources res = pm.getResourcesForApplication(packageName);
                    return Pair.create(packageName, res);
                } catch (NameNotFoundException e) {
                    Log.w(TAG, "Failed to find resources for " + packageName);
                }
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isViewAttachedToWindow(View v) {
        if (ATLEAST_KITKAT) {
            return v.isAttachedToWindow();
        } else {
            // A proxy call which returns null, if the view is not attached to the window.
            return v.getKeyDispatcherState() != null;
        }
    }

    /**
     * Returns a widget with category {@link AppWidgetProviderInfo#WIDGET_CATEGORY_SEARCHBOX}
     * provided by the same package which is set to be global search activity.
     * If widgetCategory is not supported, or no such widget is found, returns the first widget
     * provided by the package.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static AppWidgetProviderInfo getSearchWidgetProvider(Context context) {
        SearchManager searchManager =
                (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        ComponentName searchComponent = searchManager.getGlobalSearchActivity();
        if (searchComponent == null) return null;
        String providerPkg = searchComponent.getPackageName();

        AppWidgetProviderInfo defaultWidgetForSearchPackage = null;

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (AppWidgetProviderInfo info : appWidgetManager.getInstalledProviders()) {
            if (info.provider.getPackageName().equals(providerPkg)) {
                if (ATLEAST_JB_MR1) {
                    if ((info.widgetCategory & AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX) != 0) {
                        return info;
                    } else if (defaultWidgetForSearchPackage == null) {
                        defaultWidgetForSearchPackage = info;
                    }
                } else {
                    return info;
                }
            }
        }
        return defaultWidgetForSearchPackage;
    }

    /**
     * Compresses the bitmap to a byte array for serialization.
     */
    public static byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Could not write bitmap");
            return null;
        }
    }

    /**
     * Find the first vacant cell, if there is one.
     *
     * @param vacant Holds the x and y coordinate of the vacant cell
     * @param spanX Horizontal cell span.
     * @param spanY Vertical cell span.
     *
     * @return true if a vacant cell was found
     */
    public static boolean findVacantCell(int[] vacant, int spanX, int spanY,
            int xCount, int yCount, boolean[][] occupied) {

        for (int y = 0; (y + spanY) <= yCount; y++) {
            for (int x = 0; (x + spanX) <= xCount; x++) {
                boolean available = !occupied[x][y];
                out:            for (int i = x; i < x + spanX; i++) {
                    for (int j = y; j < y + spanY; j++) {
                        available = available && !occupied[i][j];
                        if (!available) break out;
                    }
                }

                if (available) {
                    vacant[0] = x;
                    vacant[1] = y;
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Trims the string, removing all whitespace at the beginning and end of the string.
     * Non-breaking whitespaces are also removed.
     */
    public static String trim(CharSequence s) {
        if (s == null) {
            return null;
        }

        // Just strip any sequence of whitespace or java space characters from the beginning and end
        Matcher m = sTrimPattern.matcher(s);
        return m.replaceAll("$1");
    }

    /**
     * Calculates the height of a given string at a specific text size.
     */
    public static float calculateTextHeight(float textSizePx) {
        Paint p = new Paint();
        p.setTextSize(textSizePx);
        Paint.FontMetrics fm = p.getFontMetrics();
        return -fm.top + fm.bottom;
    }

    /**
     * Convenience println with multiple args.
     */
    public static void println(String key, Object... args) {
        StringBuilder b = new StringBuilder();
        b.append(key);
        b.append(": ");
        boolean isFirstArgument = true;
        for (Object arg : args) {
            if (isFirstArgument) {
                isFirstArgument = false;
            } else {
                b.append(", ");
            }
            b.append(arg);
        }
        System.out.println(b.toString());
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRtl(Resources res) {
        return ATLEAST_JB_MR1 &&
                (res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
    }

    public static void assertWorkerThread() {
        if (LauncherAppState.isDogfoodBuild() &&
                (LauncherModel.sWorkerThread.getThreadId() != Process.myTid())) {
            throw new IllegalStateException();
        }
    }

    /**
     * Returns true if the intent is a valid launch intent for a launcher activity of an app.
     * This is used to identify shortcuts which are different from the ones exposed by the
     * applications' manifest file.
     *
     * @param launchIntent The intent that will be launched when the shortcut is clicked.
     */
    public static boolean isLauncherAppTarget(Intent launchIntent) {
        if (launchIntent != null
                && Intent.ACTION_MAIN.equals(launchIntent.getAction())
                && launchIntent.getComponent() != null
                && launchIntent.getCategories() != null
                && launchIntent.getCategories().size() == 1
                && launchIntent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && TextUtils.isEmpty(launchIntent.getDataString())) {
            // An app target can either have no extra or have ItemInfo.EXTRA_PROFILE.
            Bundle extras = launchIntent.getExtras();
            if (extras == null) {
                return true;
            } else {
                Set<String> keys = extras.keySet();
                return keys.size() == 1 && keys.contains(ItemInfo.EXTRA_PROFILE);
            }
        };
        return false;
    }

    public static float dpiFromPx(int size, DisplayMetrics metrics){
        float densityRatio = (float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        return (size / densityRatio);
    }
    public static int pxFromDp(float size, DisplayMetrics metrics) {
        return (int) Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                size, metrics));
    }
    public static int pxFromSp(float size, DisplayMetrics metrics) {
        return (int) Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                size, metrics));
    }

    public static String createDbSelectionQuery(String columnName, Iterable<?> values) {
        return String.format(Locale.ENGLISH, "%s IN (%s)", columnName, TextUtils.join(", ", values));
    }

    /**
     * Wraps a message with a TTS span, so that a different message is spoken than
     * what is getting displayed.
     * @param msg original message
     * @param ttsMsg message to be spoken
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static CharSequence wrapForTts(CharSequence msg, String ttsMsg) {
        if (Utilities.ATLEAST_LOLLIPOP) {
            SpannableString spanned = new SpannableString(msg);
            spanned.setSpan(new TtsSpan.TextBuilder(ttsMsg).build(),
                    0, spanned.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return spanned;
        } else {
            return msg;
        }
    }

    /**
     * Replacement for Long.compare() which was added in API level 19.
     */
    public static int longCompare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(
                LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean isPowerSaverOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return ATLEAST_LOLLIPOP && powerManager.isPowerSaveMode();
    }

    public static boolean isWallapaperAllowed(Context context) {
        if (ATLEAST_N) {
            return context.getSystemService(WallpaperManager.class).isSetWallpaperAllowed();
        }
        return true;
    }

    /**
     * An extension of {@link BitmapDrawable} which returns the bitmap pixel size as intrinsic size.
     * This allows the badging to be done based on the action bitmap size rather than
     * the scaled bitmap size.
     */
    private static class FixedSizeBitmapDrawable extends BitmapDrawable {

        public FixedSizeBitmapDrawable(Bitmap bitmap) {
            super(null, bitmap);
        }

        @Override
        public int getIntrinsicHeight() {
            return getBitmap().getWidth();
        }

        @Override
        public int getIntrinsicWidth() {
            return getBitmap().getWidth();
        }
    }

    ///M:
    public static boolean isSystemApp(AppInfo info) {
        if (info == null) {
            return false;
        }
        return (info.flags & AppInfo.DOWNLOADED_FLAG) == 0;
    }

    /**
     * get custom toast
     * add by rongwenzhao 2017-7-13
     */
    public static Toast getCustomWidget(Context context, int textId){
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.widget_toast_view, null);
        TextView text = (TextView)view.findViewById(R.id.toast_text);
        text.setText(context.getString(textId));

        Toast toast = new Toast(context);
        toast.setGravity(Gravity.TOP,0,0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        return toast;
    }

    /**
     * add by gaoquan 2017.7.7
     * screen shot for background blur
     */
    public static Bitmap takeScreenShot(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();
        b1.setConfig(Bitmap.Config.ARGB_4444);
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        int width = activity.getWindowManager().getDefaultDisplay().getWidth();
        int height = activity.getWindowManager().getDefaultDisplay().getHeight();
        Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height - statusBarHeight);
        view.destroyDrawingCache();
        if (b != b1 && !b1.isRecycled()) {
            b1.recycle();
        }
        return b;
    }

    /**
     * add by gaoquan 2017.7.7
     * set background blur
     */
    public static void blurSetAsViewBackground(Context context, View view, Bitmap bkg, float radius) {
        Bitmap bitmap = blurToBitmap(context, bkg, radius, true);
        view.setBackground(new BitmapDrawable(context.getResources(), bitmap));
    }

    /**
     * add by gaoquan 2017.7.7
     * set blur
     */
    public static Bitmap blurToBitmap(Context context, Bitmap bkg, float radius, boolean recycle) {
        bkg = toSmallofBitmap(bkg, recycle);
        Bitmap bitmap = bkg.copy(bkg.getConfig(), true);
        if (recycle && !bkg.isRecycled()) {
            bkg.recycle();
        }
        final RenderScript rs = RenderScript.create(context);
        final Allocation input = Allocation.createFromBitmap(rs, bkg, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(radius);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(bitmap);

        bitmap = toBigofBitmap(bitmap);

        rs.destroy();
        return bitmap;
    }

    /**
     * add by gaoquan 2017.7.7
     * scale to big bitmap
     */
    private static Bitmap toBigofBitmap(Bitmap bitmap) {
        if(bitmap == null){
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(15f, 15f); //Ratio of length and width magnification
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    /**
     * add by gaoquan 2017.7.7
     * scale to small bitmap
     */
    public static Bitmap toSmallofBitmap(Bitmap bitmap, boolean recycle) {
        if(bitmap == null){
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(0.0625f, 0.0625f); //Ratio of length and width magnification
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (recycle && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return resizeBmp;
    }

    /**
     * add by gaoquan 2017.7.7
     * use blur
     */
    public static void applyBlur(Context context, View view, Bitmap bkg, float radius) {
        blurSetAsViewBackground(context, view, bkg, radius);
    }

    /**
     * add by gaoquan 2017.7.7
     * merge bitmap
     */
    public static Bitmap mergeBitmap(Bitmap bgBitmap, Bitmap topBitmap, boolean recycle) {
        Bitmap bitmap = Bitmap.createBitmap(bgBitmap.getWidth(), bgBitmap.getHeight(),
                bgBitmap.getConfig());
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        canvas.drawBitmap(bgBitmap, new Matrix(), paint);
        canvas.drawBitmap(topBitmap, 0, 0, paint);
        if (recycle) {
           if (bgBitmap != null && !bgBitmap.isRecycled()) {
               bgBitmap.recycle();
           }
           if(topBitmap != null && !topBitmap.isRecycled()){
               topBitmap.recycle();
           }
        }
        return bitmap;
    }

    /**
     * add by gaoquan 2017.7.7
     * scale bitmap
     */
    public static Bitmap zoomImage(Bitmap bgimage, float newWidth, float newHeight) {
        if(bgimage == null){
            return null;
        }
        float width = bgimage.getWidth();
        float height = bgimage.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = 1.0f * newWidth / width;
        float scaleHeight =  1.0f * newHeight / height;
        if(Float.compare(scaleWidth, scaleHeight) >= 0){
            matrix.postScale(scaleHeight, scaleHeight);
        } else {
            matrix.postScale(scaleWidth, scaleWidth);
        }

        Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width,
                (int) height, matrix, true);
        if (bgimage != bitmap && !bgimage.isRecycled()) {
            bgimage.recycle();
        }
        return bitmap;
    }

    /**
     * add by gaoquan 2017.7.13
     * Get a screenshot
     *
     * @return screenshot
     */
    public static Bitmap getScreenImage(Context context) {
        Bitmap bitmap = null;
        Matrix mMatrix = new Matrix();
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();

        WindowManager mWindowManager = (WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE);
        Display mDisplay = mWindowManager.getDefaultDisplay();

        mDisplay.getRealMetrics(mDisplayMetrics);

        float[] dims = { mDisplayMetrics.widthPixels
                , mDisplayMetrics.heightPixels};

        float degrees = getDegreesForRotation(mDisplay.getRotation());
        if (degrees > 0) {
            mMatrix.reset();
            mMatrix.preRotate(-degrees);
            mMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }

        int screenWidth = (int)dims[0];
        int screenHeight = (int)dims[1];

        try {
            bitmap = SurfaceControl.screenshot(screenWidth, screenHeight);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.toString());
            bitmap = null;
        }
        if (bitmap != null) {
            try {
                if (degrees > 0) {
                    Bitmap ss = Bitmap.createBitmap(
                            mDisplayMetrics.widthPixels,
                            mDisplayMetrics.heightPixels,
                            Bitmap.Config.ARGB_4444);
                    Canvas c = new Canvas(ss);
                    c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
                    c.rotate(degrees);
                    c.translate(-dims[0] / 2, -dims[1] / 2);
                    c.drawBitmap(bitmap, 0, 0, null);
                    c.setBitmap(null);
                    bitmap.recycle();
                    bitmap = ss;
                }
            } catch (Exception e) {
                bitmap = null;
            }
            if(bitmap != null){
                bitmap.setHasAlpha(false);
                bitmap.prepareToDraw();
            }
        }
        return bitmap;
    }

    /**
     * add by gaoquan 2017.7.13
     * Get a screenshot degree
     *
     */
    public static float getDegreesForRotation(int value) {
        switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f;
        }
        return 0f;
    }

    /**
     * add by gaoquan 2017.7.13
     * java blur
     *
     *     start
     */
    private static final short[] stackblur_mul = {
            512, 512, 456, 512, 328, 456, 335, 512, 405, 328, 271, 456, 388, 335, 292, 512,
            454, 405, 364, 328, 298, 271, 496, 456, 420, 388, 360, 335, 312, 292, 273, 512,
            482, 454, 428, 405, 383, 364, 345, 328, 312, 298, 284, 271, 259, 496, 475, 456,
            437, 420, 404, 388, 374, 360, 347, 335, 323, 312, 302, 292, 282, 273, 265, 512,
            497, 482, 468, 454, 441, 428, 417, 405, 394, 383, 373, 364, 354, 345, 337, 328,
            320, 312, 305, 298, 291, 284, 278, 271, 265, 259, 507, 496, 485, 475, 465, 456,
            446, 437, 428, 420, 412, 404, 396, 388, 381, 374, 367, 360, 354, 347, 341, 335,
            329, 323, 318, 312, 307, 302, 297, 292, 287, 282, 278, 273, 269, 265, 261, 512,
            505, 497, 489, 482, 475, 468, 461, 454, 447, 441, 435, 428, 422, 417, 411, 405,
            399, 394, 389, 383, 378, 373, 368, 364, 359, 354, 350, 345, 341, 337, 332, 328,
            324, 320, 316, 312, 309, 305, 301, 298, 294, 291, 287, 284, 281, 278, 274, 271,
            268, 265, 262, 259, 257, 507, 501, 496, 491, 485, 480, 475, 470, 465, 460, 456,
            451, 446, 442, 437, 433, 428, 424, 420, 416, 412, 408, 404, 400, 396, 392, 388,
            385, 381, 377, 374, 370, 367, 363, 360, 357, 354, 350, 347, 344, 341, 338, 335,
            332, 329, 326, 323, 320, 318, 315, 312, 310, 307, 304, 302, 299, 297, 294, 292,
            289, 287, 285, 282, 280, 278, 275, 273, 271, 269, 267, 265, 263, 261, 259
    };

    private static final byte[] stackblur_shr = {
            9, 11, 12, 13, 13, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16, 17,
            17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 18, 19,
            19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 20, 20, 20,
            20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 21,
            21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
            21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22,
            22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
            22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 23,
            23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
            23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
            23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
            23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24
    };

    static final int EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors();
    static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(EXECUTOR_THREADS);

    public static Bitmap javaBlur(Bitmap original, float radius, boolean recycle) {
        int w = original.getWidth();
        int h = original.getHeight();
        int[] currentPixels = new int[w * h];
        original.getPixels(currentPixels, 0, w, 0, 0, w, h);
        int cores = EXECUTOR_THREADS;

        ArrayList<BlurTask> horizontal = new ArrayList<BlurTask>(cores);
        ArrayList<BlurTask> vertical = new ArrayList<BlurTask>(cores);
        for (int i = 0; i < cores; i++) {
            horizontal.add(new BlurTask(currentPixels, w, h, (int) radius, cores, i, 1));
            vertical.add(new BlurTask(currentPixels, w, h, (int) radius, cores, i, 2));
        }

        try {
            EXECUTOR.invokeAll(horizontal);
        } catch (InterruptedException e) {
            return null;
        }

        try {
            EXECUTOR.invokeAll(vertical);
        } catch (InterruptedException e) {
            return null;
        }
        if (recycle && !original.isRecycled()) {
            original.recycle();
        }
        return Bitmap.createBitmap(currentPixels, w, h, Bitmap.Config.ARGB_4444);
    }

    private static void blurIteration(int[] src, int w, int h, int radius, int cores, int core, int step) {
        int x, y, xp, yp, i;
        int sp;
        int stack_start;
        int stack_i;

        int src_i;
        int dst_i;

        long sum_r, sum_g, sum_b,
                sum_in_r, sum_in_g, sum_in_b,
                sum_out_r, sum_out_g, sum_out_b;

        int wm = w - 1;
        int hm = h - 1;
        int div = (radius * 2) + 1;
        int mul_sum = stackblur_mul[radius];
        byte shr_sum = stackblur_shr[radius];
        int[] stack = new int[div];

        if (step == 1)
        {
            int minY = core * h / cores;
            int maxY = (core + 1) * h / cores;

            for(y = minY; y < maxY; y++)
            {
                sum_r = sum_g = sum_b =
                        sum_in_r = sum_in_g = sum_in_b =
                                sum_out_r = sum_out_g = sum_out_b = 0;

                src_i = w * y; // start of line (0,y)

                for(i = 0; i <= radius; i++)
                {
                    stack_i    = i;
                    stack[stack_i] = src[src_i];
                    sum_r += ((src[src_i] >>> 16) & 0xff) * (i + 1);
                    sum_g += ((src[src_i] >>> 8) & 0xff) * (i + 1);
                    sum_b += (src[src_i] & 0xff) * (i + 1);
                    sum_out_r += ((src[src_i] >>> 16) & 0xff);
                    sum_out_g += ((src[src_i] >>> 8) & 0xff);
                    sum_out_b += (src[src_i] & 0xff);
                }


                for(i = 1; i <= radius; i++)
                {
                    if (i <= wm) src_i += 1;
                    stack_i = i + radius;
                    stack[stack_i] = src[src_i];
                    sum_r += ((src[src_i] >>> 16) & 0xff) * (radius + 1 - i);
                    sum_g += ((src[src_i] >>> 8) & 0xff) * (radius + 1 - i);
                    sum_b += (src[src_i] & 0xff) * (radius + 1 - i);
                    sum_in_r += ((src[src_i] >>> 16) & 0xff);
                    sum_in_g += ((src[src_i] >>> 8) & 0xff);
                    sum_in_b += (src[src_i] & 0xff);
                }


                sp = radius;
                xp = radius;
                if (xp > wm) xp = wm;
                src_i = xp + y * w; //   img.pix_ptr(xp, y);
                dst_i = y * w; // img.pix_ptr(0, y);
                for(x = 0; x < w; x++)
                {
                    src[dst_i] = (int)
                            ((src[dst_i] & 0xff000000) |
                                    ((((sum_r * mul_sum) >>> shr_sum) & 0xff) << 16) |
                                    ((((sum_g * mul_sum) >>> shr_sum) & 0xff) << 8) |
                                    ((((sum_b * mul_sum) >>> shr_sum) & 0xff)));
                    dst_i += 1;

                    sum_r -= sum_out_r;
                    sum_g -= sum_out_g;
                    sum_b -= sum_out_b;

                    stack_start = sp + div - radius;
                    if (stack_start >= div) stack_start -= div;
                    stack_i = stack_start;

                    sum_out_r -= ((stack[stack_i] >>> 16) & 0xff);
                    sum_out_g -= ((stack[stack_i] >>> 8) & 0xff);
                    sum_out_b -= (stack[stack_i] & 0xff);

                    if(xp < wm)
                    {
                        src_i += 1;
                        ++xp;
                    }

                    stack[stack_i] = src[src_i];

                    sum_in_r += ((src[src_i] >>> 16) & 0xff);
                    sum_in_g += ((src[src_i] >>> 8) & 0xff);
                    sum_in_b += (src[src_i] & 0xff);
                    sum_r    += sum_in_r;
                    sum_g    += sum_in_g;
                    sum_b    += sum_in_b;

                    ++sp;
                    if (sp >= div) sp = 0;
                    stack_i = sp;

                    sum_out_r += ((stack[stack_i] >>> 16) & 0xff);
                    sum_out_g += ((stack[stack_i] >>> 8) & 0xff);
                    sum_out_b += (stack[stack_i] & 0xff);
                    sum_in_r  -= ((stack[stack_i] >>> 16) & 0xff);
                    sum_in_g  -= ((stack[stack_i] >>> 8) & 0xff);
                    sum_in_b  -= (stack[stack_i] & 0xff);
                }

            }
        }

        // step 2
        else if (step == 2)
        {
            int minX = core * w / cores;
            int maxX = (core + 1) * w / cores;

            for(x = minX; x < maxX; x++)
            {
                sum_r =    sum_g =    sum_b =
                        sum_in_r = sum_in_g = sum_in_b =
                                sum_out_r = sum_out_g = sum_out_b = 0;

                src_i = x; // x,0
                for(i = 0; i <= radius; i++)
                {
                    stack_i    = i;
                    stack[stack_i] = src[src_i];
                    sum_r           += ((src[src_i] >>> 16) & 0xff) * (i + 1);
                    sum_g           += ((src[src_i] >>> 8) & 0xff) * (i + 1);
                    sum_b           += (src[src_i] & 0xff) * (i + 1);
                    sum_out_r       += ((src[src_i] >>> 16) & 0xff);
                    sum_out_g       += ((src[src_i] >>> 8) & 0xff);
                    sum_out_b       += (src[src_i] & 0xff);
                }
                for(i = 1; i <= radius; i++)
                {
                    if(i <= hm) src_i += w; // +stride

                    stack_i = i + radius;
                    stack[stack_i] = src[src_i];
                    sum_r += ((src[src_i] >>> 16) & 0xff) * (radius + 1 - i);
                    sum_g += ((src[src_i] >>> 8) & 0xff) * (radius + 1 - i);
                    sum_b += (src[src_i] & 0xff) * (radius + 1 - i);
                    sum_in_r += ((src[src_i] >>> 16) & 0xff);
                    sum_in_g += ((src[src_i] >>> 8) & 0xff);
                    sum_in_b += (src[src_i] & 0xff);
                }

                sp = radius;
                yp = radius;
                if (yp > hm) yp = hm;
                src_i = x + yp * w; // img.pix_ptr(x, yp);
                dst_i = x;               // img.pix_ptr(x, 0);
                for(y = 0; y < h; y++)
                {
                    src[dst_i] = (int)
                            ((src[dst_i] & 0xff000000) |
                                    ((((sum_r * mul_sum) >>> shr_sum) & 0xff) << 16) |
                                    ((((sum_g * mul_sum) >>> shr_sum) & 0xff) << 8) |
                                    ((((sum_b * mul_sum) >>> shr_sum) & 0xff)));
                    dst_i += w;

                    sum_r -= sum_out_r;
                    sum_g -= sum_out_g;
                    sum_b -= sum_out_b;

                    stack_start = sp + div - radius;
                    if(stack_start >= div) stack_start -= div;
                    stack_i = stack_start;

                    sum_out_r -= ((stack[stack_i] >>> 16) & 0xff);
                    sum_out_g -= ((stack[stack_i] >>> 8) & 0xff);
                    sum_out_b -= (stack[stack_i] & 0xff);

                    if(yp < hm)
                    {
                        src_i += w; // stride
                        ++yp;
                    }

                    stack[stack_i] = src[src_i];

                    sum_in_r += ((src[src_i] >>> 16) & 0xff);
                    sum_in_g += ((src[src_i] >>> 8) & 0xff);
                    sum_in_b += (src[src_i] & 0xff);
                    sum_r    += sum_in_r;
                    sum_g    += sum_in_g;
                    sum_b    += sum_in_b;

                    ++sp;
                    if (sp >= div) sp = 0;
                    stack_i = sp;

                    sum_out_r += ((stack[stack_i] >>> 16) & 0xff);
                    sum_out_g += ((stack[stack_i] >>> 8) & 0xff);
                    sum_out_b += (stack[stack_i] & 0xff);
                    sum_in_r  -= ((stack[stack_i] >>> 16) & 0xff);
                    sum_in_g  -= ((stack[stack_i] >>> 8) & 0xff);
                    sum_in_b  -= (stack[stack_i] & 0xff);
                }
            }
        }

    }

    private static class BlurTask implements Callable<Void> {
        private final int[] _src;
        private final int _w;
        private final int _h;
        private final int _radius;
        private final int _totalCores;
        private final int _coreIndex;
        private final int _round;

        public BlurTask(int[] src, int w, int h, int radius, int totalCores, int coreIndex, int round) {
            _src = src;
            _w = w;
            _h = h;
            _radius = radius;
            _totalCores = totalCores;
            _coreIndex = coreIndex;
            _round = round;
        }

        @Override
        public Void call() throws Exception {
            blurIteration(_src, _w, _h, _radius, _totalCores, _coreIndex, _round);
            return null;
        }
    }
    /**
     * add by gaoquan 2017.7.13
     * java blur
     *
     *     end
     */
    //add for dynamic calender icon by louhan 20170714 start
    public static Bitmap createCalendarIconBitmap(Context context, Drawable icon) {
        return createCalendarIconBitmap(context, icon, true,
                Color.parseColor("#ffff4a4a"),
                30f
                , Color.parseColor("#ff151515")
                , 7f);
    }

    public static Bitmap createCalendarIconBitmap(Context context, Drawable icon, boolean isWeekAbove, int dayColor, float dayTextSize, int weekColor, float weekTextSize) {
        final String[] WEEK = context.getResources().getStringArray(R.array.weeks);
        Bitmap calendarIcon = createIconBitmap(icon, context);
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String dayString = String.valueOf(day);
//        if (day < 10) {
//            dayString = String.valueOf(0) + String.valueOf(day);
//        }
        String weekString = WEEK[calendar.get(Calendar.DAY_OF_WEEK) - 1];
//        if (dayString.length() != 2) {
//            throw new RuntimeException("The date character length of the" +
//                    " calendar icon on the desktop is not equal to two");
//        }
        synchronized (sCanvas) {
            final Canvas canvas = sCanvas;
            canvas.setBitmap(calendarIcon);

            Paint mDatePaint = new Paint();
            mDatePaint.setAntiAlias(true);
            mDatePaint.setFilterBitmap(true);
            mDatePaint.setDither(true);
            Typeface font = Typeface.createFromAsset(context.getAssets(), "fonts/FZLTHK.TTF");
            mDatePaint.setTypeface(font);
            //mDatePaint.setFakeBoldText(true);
            //draw icon
            canvas.drawBitmap(calendarIcon, 0, 0, mDatePaint);
            mDatePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

            float weekStringSize = 16f;
            float dayStringSize = 8f;
            float dayStringLeftPadding = 12f;
            float dayStringSingleLeftPadding = 20f;
            float dayStringTopPadding = 19f;
            Rect rect = new Rect();
            int top = 0;
            int gap = 0;
            int width1 = 0;
            int height1 = 0;
            int width2 = 0;
            int height2 = 0;

            if (isWeekAbove) {
                // Added by louhan in 2017-8-17 for fix bug GMOS-2772
                mDatePaint.setTextSize(DisplayMetricsUtils.sp2px(weekTextSize));
                Log.d(TAG, "DisplayMetricsUtils.dip2px(weekTextSize) + " + DisplayMetricsUtils.dip2px(weekTextSize));

                if(ThemeHelper.getInstance(context).getCurTheme().equals(ThemeHelper.THEME_DEFAULT)){
                    mDatePaint.setColor(Color.WHITE);
                }else if(ThemeHelper.getInstance(context).getCurTheme().equals(ThemeHelper.THEME_HOPE)){
                    mDatePaint.setColor(Color.parseColor("#FFE95750"));
                }else if(ThemeHelper.getInstance(context).getCurTheme().equals(ThemeHelper.THEME_RITMO)) {
                    mDatePaint.setColor(Color.parseColor("#FFDF0000"));
                }else {
                    mDatePaint.setColor(weekColor);
                }
                mDatePaint.getTextBounds(weekString, 0, weekString.length(), rect);
                // Added by louhan in 2017-8-17 for fix bug GMOS-2772
                top = DisplayMetricsUtils.dip2px(weekStringSize); //for weekString: The smaller the upward movement.
                gap = DisplayMetricsUtils.dip2px(dayStringSize);  //for dayString: The smaller the value, the lower the movement
                width1 = rect.right - rect.left;
                height1 = rect.bottom - rect.top;
                width2 = calendarIcon.getWidth();
                height2 = calendarIcon.getHeight() + top - gap;
                canvas.drawText(weekString, (width2 - width1) / 2 - rect.left, top, mDatePaint);
                // Added by louhan in 2017-8-17 for fix bug GMOS-2772
//                mDatePaint.setTextSize(DisplayMetricsUtils.dip2px(dayTextSize));
//                Log.d(TAG, "DisplayMetricsUtils.dip2px(dayTextSize) + " + DisplayMetricsUtils.dip2px(dayTextSize));
//                mDatePaint.setColor(dayColor);
//                //mDatePaint.setShadowLayer(5, 5, -5, Color.DKGRAY);
//                //mDatePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
//                mDatePaint.getTextBounds(dayString, 0, dayString.length(), rect);
//                width1 = rect.right - rect.left;
//                height1 = rect.bottom - rect.top;
//                width2 = calendarIcon.getWidth();
//                height2 = calendarIcon.getHeight() + top + gap;
//                canvas.drawText(dayString, (width2 - width1) / 2 - rect.left, (height2 - height1) / 2 - rect.top - gap, mDatePaint);
                if (day < 10) {
                    Drawable first = getNumBitmapDrawable(Integer.parseInt(String.valueOf(dayString.charAt(0))),context);
                    Rect oneR = new Rect(0, 0, first.getIntrinsicWidth(), first.getIntrinsicHeight());
                    first.setBounds(oneR);

                    canvas.save();
                    canvas.translate(DisplayMetricsUtils.dip2px(dayStringSingleLeftPadding), DisplayMetricsUtils.dip2px(dayStringTopPadding));
                    first.draw(canvas);
                    canvas.restore();
                } else {
                    Drawable first = getNumBitmapDrawable(Integer.parseInt(String.valueOf(dayString.charAt(0))),context);
                    Drawable second = getNumBitmapDrawable(Integer.parseInt(String.valueOf(dayString.charAt(1))),context);
//                Drawable first = context.getDrawable(ICON_DAYS[Integer.parseInt(String.valueOf(dayString.charAt(0)))]);
//                Drawable second = context.getDrawable(ICON_DAYS[Integer.parseInt(String.valueOf(dayString.charAt(1)))]);
                    Rect oneR = new Rect(0, 0, first.getIntrinsicWidth(), first.getIntrinsicHeight());
                    first.setBounds(oneR);

                    Rect sixR = new Rect(0, 0, second.getIntrinsicWidth(), second.getIntrinsicHeight());
                    second.setBounds(sixR);

                    canvas.save();
                    Log.d(TAG,"DisplayMetricsUtils.dip2px(dayStringLeftPadding) + " + DisplayMetricsUtils.dip2px(dayStringLeftPadding)
                    + "  DisplayMetricsUtils.dip2px(dayStringTopPadding) + " + DisplayMetricsUtils.dip2px(dayStringTopPadding));
                    canvas.translate(DisplayMetricsUtils.dip2px(dayStringLeftPadding), DisplayMetricsUtils.dip2px(dayStringTopPadding));
                    first.draw(canvas);

                    canvas.translate(oneR.width(), 0);
                    second.draw(canvas);

                    canvas.restore();
                }
            }
            return calendarIcon;
        }
    }
    //add for dynamic calender icon by louhan 20170714 end

    public static BitmapDrawable getNumBitmapDrawable(int i, Context context) {
        BitmapDrawable b = null;
        switch (i) {
            case 0:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_0(), context));
                break;
            case 1:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_1(), context));
                break;
            case 2:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_2(), context));
                break;
            case 3:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_3(), context));
                break;
            case 4:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_4(), context));
                break;
            case 5:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_5(), context));
                break;
            case 6:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_6(), context));
                break;
            case 7:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_7(), context));
                break;
            case 8:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_8(), context));
                break;
            case 9:
                b = new BitmapDrawable(context.getResources(), toZoomofBitmap(ThemeHelper.getInstance(context).getCalendarNum_9(), context));
                break;
            default:
                break;
        }
        return b;
    }

    public static Bitmap toZoomofBitmap(Bitmap bitmap, Context context) {
        float densityDpi = context.getResources().getDisplayMetrics().densityDpi;
//        float defaultDpi = densityDpi >= DisplayMetrics.DENSITY_560 ? 640 :
//                (densityDpi <= DisplayMetrics.DENSITY_360 ? 320 : 480);
        float defaultDpi = DisplayMetrics.DENSITY_XXXHIGH == densityDpi ? 640 :
                (DisplayMetrics.DENSITY_XHIGH == densityDpi ? 320 : 480);
        if (bitmap == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        Log.d(TAG, "(float) (densityDpi / defaultDpi) :" + (float) (densityDpi / defaultDpi));
        matrix.postScale((float) (densityDpi / defaultDpi), (float) (densityDpi / defaultDpi)); //Ratio of length and width magnification
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    //added by liuning for 判断是否是内置可卸载应用 on 2017/11/3 start
    public static boolean isVendorApp(String packageName) {
        return (getApplicationInfoFlagsEx(packageName) & FLAG_EX_OPERATOR) != 0;
    }

    public static int getApplicationInfoFlagsEx(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return 0;
        }
        int flagsEx = 0;
        try {
            ApplicationInfo info = LauncherApplication.getAppContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);

            Field field = ApplicationInfo.class.getDeclaredField("flagsEx");
            field.setAccessible(true);
            flagsEx = field.getInt(info);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return flagsEx;
    }
    //added by liuning for 判断是否是内置可卸载应用 on 2017/11/3 end
}
