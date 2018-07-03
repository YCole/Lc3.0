package com.gome.launcher.unread;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.View;

import com.gome.launcher.AppInfo;
import com.gome.launcher.FolderInfo;
import com.gome.launcher.ItemInfo;
import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherApplication;
import com.gome.launcher.LauncherSettings;
import com.gome.launcher.R;
import com.gome.launcher.ShortcutInfo;
import com.gome.launcher.util.DLog;
import com.gome.launcher.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by gaoquan 2017.6.1
 */

class UnreadSupportShortcut {
    public UnreadSupportShortcut(String pkgName, String clsName, String keyString, int type) {
        mComponent = new ComponentName(pkgName, clsName);
        mKey = keyString;
        mShortcutType = type;
        mUnreadNum = 0;
    }

    ComponentName mComponent;
    String mKey;
    int mShortcutType;
    int mUnreadNum;

    @Override
    public String toString() {
        return "{UnreadSupportShortcut[" + mComponent + "], key = " + mKey + ",type = "
                + mShortcutType + ",unreadNum = " + mUnreadNum + "}";
    }
}

/**
 * M: This class is a util class, implemented to do the following two things,:
 *
 * 1.Read config xml to get the shortcuts which support displaying unread number,
 * then get the initial value of the unread number of each component and update
 * shortcuts and folders through callbacks implemented in Launcher.
 *
 * 2. Receive unread broadcast sent by application, update shortcuts and folders in
 * workspace, hot seat and update application icons in app customize paged view.
 */
public class BadgeUnreadLoader {
    private static final String TAG = "BadgeUnreadLoader";

    private static final ArrayList<UnreadSupportShortcut> UNREAD_SUPPORT_SHORTCUTS =
          new ArrayList<UnreadSupportShortcut>();

    private ArrayList<ComponentName> mComponentNameArrayList = new ArrayList<ComponentName>();
    private ArrayList<ComponentName> mFilterAppList = new ArrayList<ComponentName>();
    private static final String TAG_FILTERAPPS = "filterapps";
    private static final Object LOG_LOCK = new Object();

    private Context mContext;

    private WeakReference<UnreadCallbacks> mCallbacks;

    private BadgeSQL mBadgeSQL;

    private BadgeContentObserver mBadgeContentObserver;

    /**
     * Added by gaoquan 2017.8.14
     */
    //-------------------------------start--------------///
    private static final String WEIXIN_PACKAGE_NAME = "com.tencent.mm";
    private static final String WEIXIN_CLASS_NAME = "com.tencent.mm.ui.LauncherUI";
    private static final String WEIXIN_BADGE_URI = "content://com.android.badge/badge";
    //-------------------------------end--------------///
    public static final String BADGE_URI = "content://com.gome.launcher.unread.badgecontentprovider/badges";
    public static final String PERMISSION_UNREAD_CHANGED = "com.gome.launcher.permission.SHOW_BADGE";
	
	public static final String BADGE_ACTION = "com.gome.launcher.update_badge";

    public static final int SINGLE_UNREAD_COUNT = 9;

    public BadgeUnreadLoader(Context context) {
        mContext = context;
        mBadgeSQL = new BadgeSQL(this, context);
        mBadgeContentObserver = new BadgeContentObserver(context, new Handler(), this);
        registerContentObservers();
        initFilterApps();
    }

    /**
     * Added by gaoquan 2017.11.23
     * fix GM12B_量产PRODUCTION-7765【桌面】长按桌面，选择更多设置-图标角标，不支持角标的应用也有开关设置，请修改
     */
    //-------------------------------start--------------///
    public void initFilterApps(){
        try {
            XmlResourceParser parser = mContext.getResources().getXml(R.xml.launcher_apps_badge);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            XmlUtils.beginDocument(parser, TAG_FILTERAPPS);
            final int depth = parser.getDepth();
            int type = -1;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                    && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.FilterApps);
                mFilterAppList.add(new ComponentName(a
                        .getString(R.styleable.FilterApps_PackageName), a
                        .getString(R.styleable.FilterApps_ClassName)));
                a.recycle();

            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //-------------------------------end--------------///

    /**
     * get SQLite
     */
    public BadgeSQL getBadgeSQL() {
        return mBadgeSQL;

    }

    public void registerContentObservers() {
        Uri uri = Uri.parse(BADGE_URI);
        mContext.getContentResolver().registerContentObserver(uri, false, mBadgeContentObserver);
        /**
         * Added by gaoquan 2017.8.14
         */
        //-------------------------------start--------------///
        Uri WeiXinUri = Uri.parse(WEIXIN_BADGE_URI);
        mContext.getContentResolver().registerContentObserver(WeiXinUri, false, mBadgeContentObserver);
        //-------------------------------end--------------///
    }

    public void unRegisterContentObservers() {
        if (mBadgeContentObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mBadgeContentObserver);
            mBadgeContentObserver = null;
        }
    }

    /**
     * Modified by gaoquan 2017.10.16
     * fix:GMOS2.0GMOS-10023【Launcher】下载登录微信且有角标，进入微信的应用信息界面清除数据，桌面微信的角标还是存在
     */
    //-------------------------------start--------------///
    public void clearUnreadNumsByPackageName(String unreadPackageName){
        if(mBadgeSQL.updateByPackageName(unreadPackageName, 0)){
            mContext.getContentResolver().notifyChange(Uri.parse(BADGE_URI), null);
        }
    }
    //-------------------------------end--------------///

    /**
     * get all open switch app
     */
    public void initUnreadNums() {
        mBadgeSQL.queryOpen();
    }

    /**
     * update and show unread badge
     */
    public void updateUnreadNums(ComponentName component, int unreadNum) {
        int index = supportUnreadFeature(component);
        setUnreadNumberAt(index, unreadNum);
        if (mCallbacks != null) {
            if(component != null) {
                if(unreadNum >= 0){
                    final UnreadCallbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.bindComponentUnreadChanged(component, unreadNum);
                    }
                }
            }
        }
    }

    public void update(){
        initialize();
    }

    private void initialize(){
        if(mCallbacks == null || mCallbacks.get() == null){
            initialize(LauncherApplication.getLauncher());
        }
        if (mCallbacks != null) {
            UnreadCallbacks callbacks = mCallbacks.get();
            if (callbacks != null) {
                callbacks.bindUnreadInfoIfNeeded();
            }
        }
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(UnreadCallbacks callbacks) {
        mCallbacks = new WeakReference<UnreadCallbacks>(callbacks);
        DLog.d(TAG, "initialize: callbacks = " + callbacks
                + ", mCallbacks = " + mCallbacks);
    }

    /**
     * Load and initialize unread shortcuts.
     *
     */
    public void loadAndInitUnreadShortcuts() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                loadUnreadSupportShortcutsFromPackageName();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (mCallbacks != null) {
                    UnreadCallbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.bindUnreadInfoIfNeeded();
                    }
                }
            }
        }.execute();
    }

    /**
     * delete one unread app
     */
    public void deleteUnreadSupportByPackageName(String packageName) {
        for (Iterator iter = UNREAD_SUPPORT_SHORTCUTS.iterator(); iter.hasNext(); ) {
            if(packageName.equals(
                    ((UnreadSupportShortcut)iter.next()).mComponent.getPackageName())) {
                iter.remove();
            }
        }
        for (Iterator iter = mComponentNameArrayList.iterator(); iter.hasNext(); ) {
            if(packageName.equals(
                    ((ComponentName)iter.next()).getPackageName())) {
                iter.remove();
            }
        }
        mBadgeSQL.deleteOne(packageName);
		Intent intent = new Intent();
        intent.setAction(BADGE_ACTION);
        mContext.sendBroadcast(intent);
    }

    /**
     * add one unread app
     */
    public void addUnreadSupportByPackageName(String packageName) {
        ComponentName componentName = null;
        PackageManager pm = mContext.getPackageManager();
        PackageInfo packageInfo = null;
        String pName = "";
        String cName = "";
        try {
            packageInfo = pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        if (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission(PERMISSION_UNREAD_CHANGED, packageInfo.packageName)) {
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            resolveIntent.setPackage(packageInfo.packageName);
            List<ResolveInfo> resolveinfoList = mContext.getPackageManager()
                    .queryIntentActivities(resolveIntent, 0);

            ResolveInfo resolveinfo = resolveinfoList.iterator().next();
            if (resolveinfo != null) {
                pName = resolveinfo.activityInfo.packageName;
                cName = resolveinfo.activityInfo.name;
            }
        }
        UNREAD_SUPPORT_SHORTCUTS.add(new UnreadSupportShortcut(pName,
                cName, pName + ".unread", 0));
        componentName = new ComponentName(pName, cName);
        mComponentNameArrayList.add(componentName);
        mBadgeSQL.addOne(pName, cName);
		Intent intent = new Intent();
        intent.setAction(BADGE_ACTION);
        mContext.sendBroadcast(intent);
    }

    /**
     * load app support unread badge app
     */
    private void loadUnreadSupportShortcutsFromPackageName() {
        Log.v(TAG, "loadUnreadSupportShortcutsFromPackageName size==");
        ComponentName componentName = null;
        UNREAD_SUPPORT_SHORTCUTS.clear();
        mComponentNameArrayList.clear();

        PackageManager pm = mContext.getPackageManager();

        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveinfoList = mContext.getPackageManager()
                .queryIntentActivities(resolveIntent, 0);
        Log.v(TAG, "loadUnreadSupportShortcutsFromPackageName resolveinfoList size==" + resolveinfoList.size());
        LooperApp:
        for (ResolveInfo resolveinfo : resolveinfoList) {
            String packageName = resolveinfo.activityInfo.packageName;
            String className = resolveinfo.activityInfo.name;
            for (ComponentName component : mFilterAppList) {
                if(packageName.equals(component.getPackageName()) && className.equals(component.getClassName())) {
                    continue LooperApp;
                }
            }
            if(packageName.equals(WEIXIN_PACKAGE_NAME) && className.equals(WEIXIN_CLASS_NAME)){
                synchronized (LOG_LOCK) {
                    UNREAD_SUPPORT_SHORTCUTS.add(new UnreadSupportShortcut(packageName,
                            className, packageName + ".unread", 0));
                    componentName = new ComponentName(packageName, className);
                    mComponentNameArrayList.add(componentName);
                    mBadgeSQL.initData(packageName, className);
                }
                continue;
            }
            Log.v(TAG, "resolveinfo packageName==" + packageName);
            Log.v(TAG, "resolveinfo className==" + className);
            if (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission(PERMISSION_UNREAD_CHANGED, packageName)) {
                Log.v(TAG, "loadUnreadSupportShortcutsFromPackageName packageName==" + packageName);
                Log.v(TAG, "loadUnreadSupportShortcutsFromPackageName className==" + className);
                synchronized (LOG_LOCK) {
                    UNREAD_SUPPORT_SHORTCUTS.add(new UnreadSupportShortcut(packageName,
                            className, packageName + ".unread", 0));
                    componentName = new ComponentName(packageName, className);
                    mComponentNameArrayList.add(componentName);
                    mBadgeSQL.initData(packageName, className);
                }
            }
        }
    }

    public void addWeiXinUnreadSupport(String packageName, String className){
        if(packageName.equals(WEIXIN_PACKAGE_NAME) && className.equals(WEIXIN_CLASS_NAME)){
            boolean hasItem = false;
            for(UnreadSupportShortcut uss : UNREAD_SUPPORT_SHORTCUTS){
                if(uss.mComponent.getPackageName().equals(packageName) && uss.mComponent.getClassName().equals(className)){
                    hasItem = true;
                    break;
                }
            }
            if(!hasItem){
                synchronized (LOG_LOCK) {
                    UNREAD_SUPPORT_SHORTCUTS.add(new UnreadSupportShortcut(packageName,
                            className, packageName + ".unread", 0));
                    mComponentNameArrayList.add(new ComponentName(packageName, className));
                }
            }

        }
    }
    
    /**
     * Get unread support shortcut information, since the information are stored
     * in an array list, we may query it and modify it at the same time, a lock
     * is needed.
     *
     * @return
     */
    private static String getUnreadSupportShortcutInfo() {
        String info = " Unread support shortcuts are ";
        synchronized (LOG_LOCK) {
            info += UNREAD_SUPPORT_SHORTCUTS.toString();
        }
        return info;
    }

    /**
     * Whether the given component support unread feature.
     *
     * @param component
     * @return
     */
    static int supportUnreadFeature(ComponentName component) {
        DLog.d(TAG, "supportUnreadFeature: component = " + component);
        if (component == null) {
            return -1;
        }

        final int size = UNREAD_SUPPORT_SHORTCUTS.size();
        for (int i = 0; i < size; i++) {
            if (UNREAD_SUPPORT_SHORTCUTS.get(i).mComponent.equals(component)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Set the unread number of the item in the list with the given unread number.
     *
     * @param index
     * @param unreadNum
     * @return
     */
    static boolean setUnreadNumberAt(int index, int unreadNum) {
        if (index >= 0 && index < UNREAD_SUPPORT_SHORTCUTS.size()) {
            if (UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum != unreadNum) {
                UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum = unreadNum;
                return true;
            }
        }
        return false;
    }

    /**
     * Get unread number of application at the given position in the supported
     * shortcut list.
     *
     * @param index
     * @return
     */
    static synchronized int getUnreadNumberAt(int index) {
        if (index < 0 || index >= UNREAD_SUPPORT_SHORTCUTS.size()) {
            return 0;
        }
        DLog.d(TAG, "getUnreadNumberAt: index = " + index
                    + getUnreadSupportShortcutInfo());
        return UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum;
    }

    /**
     * Get unread number for the given component.
     *
     * @param component
     * @return
     */
    public static int getUnreadNumberOfComponent(ComponentName component) {
        final int index = supportUnreadFeature(component);
        return getUnreadNumberAt(index);
    }

    /**
     * Draw unread number for the given icon.
     *
     * @param canvas
     * @param icon
     * @return
     */
    public static void drawUnreadEventIfNeed(Canvas canvas, View icon) {
        ItemInfo info = (ItemInfo) icon.getTag();
        if(info instanceof ShortcutInfo) {
            if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                return;
            }
        }
        if (info != null && info.unreadNum > 0) {
            Resources res = icon.getContext().getResources();

            /// M: Meature sufficent width for unread text and background image
            Paint unreadTextNumberPaint = new Paint();
            unreadTextNumberPaint.setTextSize(res.getDimension(R.dimen.unread_text_number_size));
            unreadTextNumberPaint.setAntiAlias(true);
            unreadTextNumberPaint.setTypeface(Typeface.SANS_SERIF);
            unreadTextNumberPaint.setColor(0xffffffff);
            unreadTextNumberPaint.setTextAlign(Paint.Align.CENTER);

            Paint unreadTextPlusPaint = new Paint(unreadTextNumberPaint);
            unreadTextPlusPaint.setTextSize(res.getDimension(R.dimen.unread_text_plus_size));

            String unreadTextNumber;
            String unreadTextPlus = "+";
            Rect unreadTextNumberBounds = new Rect(0, 0, 0, 0);
            Rect unreadTextPlusBounds = new Rect(0, 0, 0, 0);
            if (info.unreadNum > Launcher.MAX_UNREAD_COUNT_TWO) {
                unreadTextNumber = String.valueOf(Launcher.MAX_UNREAD_COUNT_TWO) + unreadTextPlus;
//                unreadTextPlusPaint.getTextBounds(unreadTextPlus, 0,
//                        unreadTextPlus.length(), unreadTextPlusBounds);
            } else {
                unreadTextNumber = String.valueOf(info.unreadNum);
            }
            unreadTextNumberPaint.getTextBounds(unreadTextNumber, 0,
                    unreadTextNumber.length(), unreadTextNumberBounds);
            int textHeight = unreadTextNumberBounds.height();
            int textWidth = unreadTextNumberBounds.width();// + unreadTextPlusBounds.width();

            Drawable unreadBgNinePatchDrawable;
            float unreadBgWidth = res.getDimension(R.dimen.unread_minWidth);
            float unreadBgHeight = res.getDimension(R.dimen.unread_minHeight);
            /// M: Draw unread background image.
            if (info.unreadNum > Launcher.MAX_UNREAD_COUNT_TWO) {
                unreadBgNinePatchDrawable =
                        res.getDrawable(R.drawable.gome_badge_plus, null);
                unreadBgWidth = res.getDimension(R.dimen.unread_plusWidth);
            }else if(info.unreadNum > SINGLE_UNREAD_COUNT){
                unreadBgNinePatchDrawable =
                        res.getDrawable(R.drawable.gome_badge_double, null);
                unreadBgWidth = res.getDimension(R.dimen.unread_doubleWidth);
            }else {
                unreadBgNinePatchDrawable =
                        res.getDrawable(R.drawable.gome_badge, null);
                unreadBgWidth = res.getDimension(R.dimen.unread_minWidth);
            }
//            int unreadBgWidth = unreadBgNinePatchDrawable.getIntrinsicWidth();
//            int unreadBgHeight = unreadBgNinePatchDrawable.getIntrinsicHeight();
//
//            int unreadMinWidth = (int) res.getDimension(R.dimen.unread_minWidth);
//            if (unreadBgWidth < unreadMinWidth) {
//                unreadBgWidth = unreadMinWidth;
//            }
//            int unreadTextMargin = (int) res.getDimension(R.dimen.unread_text_margin);
//            if (unreadBgWidth < textWidth + unreadTextMargin) {
//                unreadBgWidth = textWidth + unreadTextMargin;
//            }
//            if (unreadBgHeight < textHeight) {
//                unreadBgHeight = textHeight;
//            }
            Rect unreadBgBounds = new Rect(0, 0, (int)unreadBgWidth, (int)unreadBgHeight);
            unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

            float unreadMarginTop = 0f;
            float unreadMarginRight = 0f;
            if (info instanceof ShortcutInfo) {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                        unreadMarginTop = res.getDimension(R.dimen.hotseat_unread_margin_top);
                        unreadMarginRight = res.getDimension(R.dimen.hotseat_unread_margin_right);
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                        unreadMarginTop = res.getDimension(R.dimen.workspace_unread_margin_top);
                        unreadMarginRight = res.getDimension(R.dimen.workspace_unread_margin_right);
                } else {
                    unreadMarginTop = res.getDimension(R.dimen.folder_unread_margin_top);
                    unreadMarginRight = res.getDimension(R.dimen.folder_unread_margin_right);
                }
            } else if (info instanceof FolderInfo) {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    unreadMarginTop = res.getDimension(R.dimen.hotseat_unread_margin_top);
                    unreadMarginRight = res.getDimension(R.dimen.hotseat_unread_margin_right);
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    unreadMarginTop = res.getDimension(R.dimen.workspace_unread_margin_top);
                    unreadMarginRight = res.getDimension(
                            R.dimen.workspace_unread_margin_right);
                }
            } else if (info instanceof AppInfo) {
                unreadMarginTop = res.getDimension(R.dimen.app_list_unread_margin_top);
                unreadMarginRight = res.getDimension(R.dimen.app_list_unread_margin_right);
            }
            if (info.unreadNum > Launcher.MAX_UNREAD_COUNT_TWO) {
                unreadMarginRight -= res.getDimension(R.dimen.unread_text_plus_width);
            }
            if (info.unreadNum > Launcher.MAX_UNREAD_COUNT) {
                unreadMarginRight -= res.getDimension(R.dimen.unread_text_plus_width);
            }
            if (info.unreadNum <= Launcher.MAX_UNREAD_COUNT_TWO && info.unreadNum > SINGLE_UNREAD_COUNT) {
                unreadMarginRight -= res.getDimension(R.dimen.unread_text_margin_right);
            }

            //float unreadBgPosX = icon.getScrollX() + icon.getWidth() - unreadBgWidth - unreadMarginRight;
            //float unreadBgPosY = icon.getScrollY() + unreadMarginTop;
            float unreadBgPosX = icon.getScrollX() + icon.getWidth() / 2.0f + unreadMarginRight;
            float unreadBgPosY = icon.getScrollY() + icon.getPaddingTop() - unreadMarginTop;

            canvas.save();
            canvas.translate(unreadBgPosX, unreadBgPosY);

            if (unreadBgNinePatchDrawable != null) {
                unreadBgNinePatchDrawable.draw(canvas);
            } else {
                return;
            }

            /// M: Draw unread text.
            Paint.FontMetrics fontMetrics = unreadTextNumberPaint.getFontMetrics();
            if (info.unreadNum > Launcher.MAX_UNREAD_COUNT_TWO) {
                canvas.drawText(unreadTextNumber,
                        unreadBgWidth / 2,
                        //(unreadBgWidth - unreadTextPlusBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2 - res.getDimension(R.dimen.unread_margin_fix),
                        unreadTextNumberPaint);
//                canvas.drawText(unreadTextPlus,
//                        (unreadBgWidth + unreadTextNumberBounds.width()) / 2,
//                        (unreadBgHeight + textHeight) / 2 + fontMetrics.ascent / 2,
//                        unreadTextPlusPaint);
            } else {
                canvas.drawText(unreadTextNumber,
                        unreadBgWidth / 2,
                        (unreadBgHeight + textHeight) / 2 - res.getDimension(R.dimen.unread_margin_fix),
                        unreadTextNumberPaint);
            }
            canvas.restore();
        }
    }

    public interface UnreadCallbacks {
        /**
         * Bind shortcuts and application icons with the given component, and
         * update folders unread which contains the given component.
         *
         * @param component
         * @param unreadNum
         */
        void bindComponentUnreadChanged(ComponentName component, int unreadNum);

        /**
         * Bind unread shortcut information if needed, this call back is used to
         * update shortcuts and folders when launcher first created.
         */
        void bindUnreadInfoIfNeeded();
    }
}
