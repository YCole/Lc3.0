/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gome.drawbitmaplib.BitmapInfo;
import com.gome.drawbitmaplib.IDownLoadCallback;
import com.gome.launcher.PagedView.PageSwitchListener;
import com.gome.launcher.allapps.AllAppsContainerView;
import com.gome.launcher.classify.IconClassifyManager;
import com.gome.launcher.compat.AppWidgetManagerCompat;
import com.gome.launcher.compat.LauncherActivityInfoCompat;
import com.gome.launcher.compat.LauncherAppsCompat;
import com.gome.launcher.compat.PackageInstallerCompat;
import com.gome.launcher.compat.UserHandleCompat;
import com.gome.launcher.compat.UserManagerCompat;
import com.gome.launcher.model.WidgetsModel;
import com.gome.launcher.move.AdapterItemAddListener;
import com.gome.launcher.move.MoveViewsRestContainer;
import com.gome.launcher.packagecircle.PackageCircle;
import com.gome.launcher.settings.SettingsProvider;
import com.gome.launcher.unread.BadgeUnreadLoader;
import com.gome.launcher.unread.PackageReceiver;
import com.gome.launcher.util.BitmapUtils;
import com.gome.launcher.util.ComponentKey;
import com.gome.launcher.util.DLog;
import com.gome.launcher.util.DisplayMetricsUtils;
import com.gome.launcher.util.LongArrayMap;
import com.gome.launcher.util.PackageManagerHelper;
import com.gome.launcher.util.PrivateUtil;
import com.gome.launcher.util.TestingUtils;
import com.gome.launcher.util.Thunk;
import com.gome.launcher.util.WallpaperUtils;
import com.gome.launcher.view.WidgetHorizontalScrollView;
import com.gome.launcher.widget.PendingAddWidgetInfo;
import com.gome.launcher.widget.WidgetHostViewLoader;
import com.gome.launcher.widget.WidgetsContainerViewGM;
import com.gome.launcher.widget.WidgetsRecyclerView;
import com.hc.sh.appcenter.AppDataForEx;
import com.hc.sh.appcenter.IForExListener;
import com.hc.sh.appcenter.IForExService;
import com.mediatek.launcher3.LauncherHelper;
import com.mediatek.launcher3.LauncherLog;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static com.gome.launcher.WallpaperPickerActivity.IMAGE_PICK;

/**
 * Default launcher application.
 */
public class Launcher extends Activity
        implements View.OnClickListener, OnLongClickListener, LauncherModel.Callbacks,
        View.OnTouchListener, PageSwitchListener, LauncherProviderChangeListener,
        BadgeUnreadLoader.UnreadCallbacks, AppSearchWindow.FolderAppListener, PackageCircle.CircleCallbacks
        ,ItemUpdateListener,AdapterItemAddListener,DragController.ShortCutDeleteListener, DragController.ShortCutDeletableListener
{
    static final String TAG = "Launcher";
    static final boolean LOGD = false;

    static final boolean PROFILE_STARTUP = false;
    static final boolean DEBUG_WIDGETS = false;
    static final boolean DEBUG_STRICT_MODE = false;
    static final boolean DEBUG_RESUME_TIME = false;
    static final boolean DEBUG_DUMP_LOG = false;

    static final boolean ENABLE_DEBUG_INTENTS = false; // allow DebugIntents to run

    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_PICK_WALLPAPER = 10;

    private static final int REQUEST_BIND_APPWIDGET = 11;
    private static final int REQUEST_RECONFIGURE_APPWIDGET = 12;

    private static final int REQUEST_PERMISSION_CALL_PHONE = 13;

    private static final int WORKSPACE_BACKGROUND_GRADIENT = 0;
    private static final int WORKSPACE_BACKGROUND_TRANSPARENT = 1;
    private static final int WORKSPACE_BACKGROUND_BLACK = 2;

    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;

    /**
     * IntentStarter uses request codes starting with this. This must be greater than all activity
     * request codes used internally.
     */
    protected static final int REQUEST_LAST = 100;

    static final int SCREEN_COUNT = 5;

    // To turn on these properties, type
    // adb shell setprop log.tag.PROPERTY_NAME [VERBOSE | SUPPRESS]
    static final String DUMP_STATE_PROPERTY = "launcher_dump_state";

    // The Intent extra that defines whether to ignore the launch animation
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "com.gome.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CONTAINER = "launcher.add_container";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cell_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cell_y";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_span_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_span_y";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_COMPONENT = "launcher.add_component";
    // Type: parcelable
    private static final String RUNTIME_STATE_PENDING_ADD_WIDGET_INFO = "launcher.add_widget_info";
    // Type: parcelable
    private static final String RUNTIME_STATE_PENDING_ADD_WIDGET_ID = "launcher.add_widget_id";

    static final String INTRO_SCREEN_DISMISSED = "launcher.intro_screen_dismissed";
    static final String FIRST_RUN_ACTIVITY_DISPLAYED = "launcher.first_run_activity_displayed";

    static final String FIRST_LOAD_COMPLETE = "launcher.first_load_complete";
    static final String ACTION_FIRST_LOAD_COMPLETE =
            "com.gome.launcher.action.FIRST_LOAD_COMPLETE";

    static final String HOME_PAGE = "home_page_screen_id";
    private static final String QSB_WIDGET_ID = "qsb_widget_id";
    private static final String QSB_WIDGET_PROVIDER = "qsb_widget_provider";

    public static final String USER_HAS_MIGRATED = "launcher.user_migrated_from_old_data";

    @Thunk static final HandlerThread sWorkerThread = new HandlerThread("download");
    static {
        sWorkerThread.start();
    }
    @Thunk static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    private static Launcher mInstance = null;

    //add for dynamic download icon by louhan & weijiaqi 20170804
    public ConcurrentHashMap<ShortcutInfo, IDownLoadCallback> mDownLoadAppMap = new ConcurrentHashMap<>();
    private static DownloadBind mDownloadBind;
    private static DLServiceConnection mDLServiceConnection = new DLServiceConnection();
    private ArrayList<AppData> mAppDataList;
    private static IForExService mIForExService;
    // Added by louhan for fix bug GMOS-7669,7938

    /**
     * Added by gaoquan 2017.9.28
     */
    //-------------------------------start--------------///
    public boolean mIsFolderAnimate = false;
    //-------------------------------end--------------///

    //add by chenchao 2017.10.25 start
    protected static final int INSTALLAPPLIST = 0;
    protected static final int SETAllAPPLIST = 1;
    public static final int UPDATEAPPLIST = 2;
    protected static final int UNINSTALLAPPLIST = 3;
    private boolean hasInitCustomPageManager = false;
    //add by chenchao 2017.10.25 end
    // Added by louhan in 2017-12-08 for fix bug PRODUCTION-10682
    private void bindAppService() {
        Log.e(TAG, "bindAppService");
        if (mAppDataList != null) {
            mAppDataList.clear();
            mAppDataList = null;
        }
        if (mDownloadBind != null) {
            mDownloadBind.onDestroy();
            mDownloadBind = null;
        }

        if (mDownLoadAppMap != null) {
            mDownLoadAppMap.clear();
            mDownLoadAppMap = null;
        }

        mDownLoadAppMap = new ConcurrentHashMap<>();
        mAppDataList = new ArrayList<>();
        mDownloadBind = new DownloadBind(mInstance,mDownLoadAppMap);

        Intent intent = new Intent();
        intent.setAction("com.hc.cnappstore.ex.down");
        intent.setPackage("com.hc.cnappstore");
        getApplicationContext().bindService(intent, mDLServiceConnection, BIND_AUTO_CREATE);
    }

    private void unbindAppService() {
        if (mIForExService != null) {
            try {
                mIForExService.addListener(null);
                mIForExService = null;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mDownloadBind.onDestroy();
        mDownloadBind = null;
        getApplicationContext().unbindService(mDLServiceConnection);
    }

    //2017/11/28 静态类防止内存溢出  wjq
    private  static class  DLServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected>>>>>>>>" + name);
            mIForExService = IForExService.Stub.asInterface(service);
            try {
                mIForExService.addListener(mDownloadBind);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected>>>>>>>>" + name);
            try {
                if (mIForExService != null) {
                    mIForExService.addListener(null);
                }
                mIForExService = null;
                mDownloadBind.onDestroy();
                mDownloadBind = null;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    //add for dynamic download icon invalid status by louhan 20170912
    @Override
    public void onAppIconReady(String pkgName, Bitmap bitmap) {
        IDownLoadCallback downLoadCallback = findIDownLoadCallbackByPkgName(pkgName);
        if (downLoadCallback != null) {
            downLoadCallback.onIconReady(bitmap);
        }
    }
    //add for dynamic download icon invalid status by louhan 20170912

    public IDownLoadCallback findIDownLoadCallbackByPkgName(String pkgName) {
        if (mDownLoadAppMap == null) {
            return null;
        }
        Iterator iterator = mDownLoadAppMap.entrySet().iterator();
        while (iterator.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) iterator.next();
            ShortcutInfo shortcutInfo = (ShortcutInfo) entry.getKey();
            IDownLoadCallback iDownLoadCallback = (IDownLoadCallback) entry.getValue();
            if (shortcutInfo.getPkgName() != null && shortcutInfo.getPkgName().equals(pkgName)) {
                return iDownLoadCallback;
            }
        }
        return null;
    }

    public DownloadAppInfo findDownloadAppInfoByPkgName(String pkgName) {
        if (mDownLoadAppMap == null) {
            return null;
        }
        DownloadAppInfo downloadAppInfo;
        Iterator iterator = mDownLoadAppMap.entrySet().iterator();
        while (iterator.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) iterator.next();
            ShortcutInfo shortcutInfo = (ShortcutInfo) entry.getKey();
            IDownLoadCallback iDownLoadCallback = (IDownLoadCallback) entry.getValue();
            if (shortcutInfo.getPkgName() != null && shortcutInfo.getPkgName().equals(pkgName)) {
                downloadAppInfo = new DownloadAppInfo(shortcutInfo, iDownLoadCallback);
                return downloadAppInfo;
            }
        }
        return null;
    }


    public void replaceBubbleTextView(final BubbleTextView view) {
        final ShortcutInfo info = (ShortcutInfo) view.getTag();
        if (info != null) {
            DownloadAppInfo temp = findDownloadAppInfoByPkgName(info.getPkgName());
            if (temp != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        view.onStart(info.title.toString(),info.getPkgName(),info.getIconUrl());
                    }
                });
                mDownLoadAppMap.put(temp.getShortcutInfo(), view);
            }
        }

    }
    public FolderIcon findFolderIconInAnyWhere(FolderInfo folderInfo) {
        FolderIcon folderIcon = null;
        if (folderInfo == null) {
            return folderIcon;
        }
        int container = (int) folderInfo.container;
        switch (container) {
            case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                folderIcon = (FolderIcon) mWorkspace.
                        getScreenWithId(folderInfo.screenId).getShortcutsAndWidgets().
                        getChildAt(folderInfo.cellX, folderInfo.cellY);
                break;
            case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                folderIcon = (FolderIcon)mHotseat.getLayout().
                        getShortcutsAndWidgets().getChildAt(folderInfo.cellX, folderInfo.cellY);
                break;
            default:
                break;
        }
        return folderIcon;
    }

    public View findBubbleTextViewInAnyWhere(ShortcutInfo shortcutInfo){
        View view = null;
        if (shortcutInfo ==null) {
            return view;
        }
        int container = (int) shortcutInfo.container;
        switch (container) {
            case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                if (mWorkspace.getScreenWithId(shortcutInfo.screenId) != null &&
                        mWorkspace.getScreenWithId(shortcutInfo.screenId).getShortcutsAndWidgets() != null) {
                    view = mWorkspace.getScreenWithId(shortcutInfo.screenId).
                            getShortcutsAndWidgets().getChildAt(shortcutInfo.cellX, shortcutInfo.cellY);
                }
                break;
            case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                if (mHotseat.getLayout() != null &&
                        mHotseat.getLayout().getShortcutsAndWidgets() != null) {
                    view = mHotseat.getLayout().getShortcutsAndWidgets().
                            getChildAt(shortcutInfo.cellX, shortcutInfo.cellY);
                }
                break;
            default:
                FolderInfo folderInfo = mModel.findFolderById(shortcutInfo.container);
                FolderIcon folderIcon = findFolderIconInAnyWhere(folderInfo);
                if (folderIcon != null) {
                    view = folderIcon.getFolder().mContent.
                            getItemByXY(shortcutInfo.rank, shortcutInfo.cellX, shortcutInfo.cellY);
                }
                break;
        }
        return view;
    }

    @Override
    public void notifyDatabaseChanged(ItemInfo info) {
        if (info != null && info instanceof ShortcutInfo) {
            ShortcutInfo itemInfo = (ShortcutInfo) info;
            DownloadAppInfo downloadAppInfo = findDownloadAppInfoByPkgName(itemInfo.getPkgName());
            boolean shouldUpdateBubbleTextView = false;
            if (downloadAppInfo != null) {
                ShortcutInfo shortcutInfo = downloadAppInfo.getShortcutInfo();
                shouldUpdateBubbleTextView = shortcutInfo.container == itemInfo.container;
                shortcutInfo.container = itemInfo.container;
                shortcutInfo.cellX = itemInfo.cellX;
                shortcutInfo.cellY = itemInfo.cellY;
                shortcutInfo.rank = itemInfo.rank;
                shortcutInfo.spanX = itemInfo.spanX;
                shortcutInfo.spanY = itemInfo.spanY;
                shortcutInfo.screenId = itemInfo.screenId;

                if (!shouldUpdateBubbleTextView) {
                    return;
                }

                View view = findBubbleTextViewInAnyWhere(shortcutInfo);
                if (view != null && view instanceof BubbleTextView) {
                    replaceBubbleTextView((BubbleTextView) view);
                }
            }
        }
    }

    @Override
    public void onAppAdded(final String packageName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDragController.cancelDragByPkgName(packageName);
            }
        });
        DownloadAppInfo downloadAppInfo = findDownloadAppInfoByPkgName(packageName);
        if (downloadAppInfo != null && downloadAppInfo.getDownLoadCallback() != null && downloadAppInfo.getShortcutInfo() != null) {
            updateDownloadItemByPkgName(packageName, BitmapInfo.INSTALLED, -1, null, null);
            Log.i(TAG, "broadcast error come in packageName = " + packageName);
            ShortcutInfo shortcutInfo = downloadAppInfo.getShortcutInfo();
//            shortcutInfo.isNewApp = true;
//            shortcutInfo.appDownLoadStatus=BitmapInfo.INSTALLED;
//            shortcutInfo.flags = AppInfo.DOWNLOADED_FLAG;
//            getPackageCircle().updateCircleChanged(packageName);
//            Log.d(TAG,"remove before = " + mDownLoadAppMap.size());
            downloadAppInfo.getDownLoadCallback().onSucceed(shortcutInfo.title + "");
            mDownLoadAppMap.remove(shortcutInfo);
            Log.d(TAG, "remove end = " + mDownLoadAppMap.size());
        }
    }

    @Override
    public void onItemsQueryBySystem(final AppInfo appInfo) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //Added by wjq in 2017-7-5 for PRODUCTION-7482 start
                if (mDownLoadAppMap == null) {
                    Log.e("hcdownload","mDownLoadAppMap is null");
                    return;
                }
                //Added by wjq in 2017-7-5 for PRODUCTION-7482 end
                Iterator iterator = mDownLoadAppMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    HashMap.Entry entry = (HashMap.Entry) iterator.next();
                    final ShortcutInfo shortcutInfo = (ShortcutInfo) entry.getKey();
                    final IDownLoadCallback iDownLoadCallback = (IDownLoadCallback) entry.getValue();
                    String packageName = appInfo.componentName.getPackageName();
                    if (shortcutInfo.getPkgName() != null && shortcutInfo.getPkgName().equals(packageName)) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (iDownLoadCallback != null) {
                                    iDownLoadCallback.onInstalled(shortcutInfo.getPkgName());
                                    ((BubbleTextView)iDownLoadCallback).setText(appInfo.title);
                                }
                            }
                        });
                        shortcutInfo.isNewApp = true;
                        shortcutInfo.appDownLoadStatus = BitmapInfo.INSTALLED;
                        shortcutInfo.itemType = appInfo.itemType;
                        shortcutInfo.flags = AppInfo.DOWNLOADED_FLAG;
                        shortcutInfo.setIntent(appInfo.getIntent());
                        getPackageCircle().updateCircleChanged(shortcutInfo.getPkgName());
                        final ContentValues values = new ContentValues();
                        values.put(LauncherSettings.Favorites.STATUS,  BitmapInfo.INSTALLED);
                        values.put(LauncherSettings.Favorites.INTENT, shortcutInfo.getIntent().toUri(0));
                        values.put(LauncherSettings.Favorites.ITEM_TYPE, appInfo.itemType);
                        Runnable updateRunnable = new Runnable() {
                            @Override
                            public void run() {
                                Launcher.this.getContentResolver().update(
                                        LauncherSettings.Favorites.CONTENT_URI,
                                        values,
                                        LauncherSettings.BaseLauncherColumns.INTENT + " LIKE ? ",
                                        new String[]{"%" + shortcutInfo.getPkgName() + "%"});
                            }
                        };

                        runOnDownLoadThread(updateRunnable);
                        Log.d(TAG, "remove before = " + mDownLoadAppMap.size());
                        mDownLoadAppMap.remove(shortcutInfo);
                        Log.d(TAG, "remove end = " + mDownLoadAppMap.size());
                    }

                }
            }
        };
        runOnDownLoadThread(r);
    }

    @Override
    public void onAdded(BubbleTextView bubbleTextView) {
        replaceBubbleTextView(bubbleTextView);
    }

    /**
     * Callback itemInfo when ShortCut  is delete
     * @param itemInfo
     */
    @Override
    public void completeDrop(ItemInfo itemInfo) {
        if (itemInfo instanceof  ShortcutInfo) {
            ShortcutInfo shortcutInfo = (ShortcutInfo)itemInfo;
            DownloadAppInfo downloadAppInfo = findDownloadAppInfoByPkgName(shortcutInfo.getPkgName());
            if (downloadAppInfo != null && downloadAppInfo.getShortcutInfo() != null) {
                mDownLoadAppMap.remove(downloadAppInfo.getShortcutInfo());
                //delete install package
                if (shortcutInfo.getDownLoadStatus() == BitmapInfo.PRE_INSTALL) {
                    if (mIForExService != null) {
                        String packageName = "";
                        if (shortcutInfo.getIntent() != null && shortcutInfo.getIntent().getPackage() != null) {
                            packageName = shortcutInfo.getIntent().getPackage();
                        }
                        if (packageName == null){
                            packageName = shortcutInfo.getPkgName();
                        }
                        try {
                            Log.d(TAG,"packageName = " + packageName);
                            mIForExService.deleteApkFiles(packageName);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (mIForExService != null) {
                try {
                    mIForExService.cancelDownload(shortcutInfo.getPkgName());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean canDrop(ItemInfo itemInfo) {
        if (itemInfo instanceof  ShortcutInfo) {
            ShortcutInfo shortcutInfo = (ShortcutInfo)itemInfo;
            BubbleTextView bubbleTextView =
                    (BubbleTextView) findIDownLoadCallbackByPkgName(shortcutInfo.getPkgName());
            if (bubbleTextView != null && bubbleTextView.getBitmapInfo().getStatus() == BitmapInfo.INSTALLING) {
                return false;
            }
        }
        return true;
    }

    //2017/11/28 静态类防止内存溢出  wjq
    public static class DownloadBind extends IForExListener.Stub {
        Handler mHandler;
        ConcurrentHashMap mDownLoadAppMap;
        Launcher mLauncher;

        public DownloadBind(Launcher launcher, ConcurrentHashMap downloadAppMap) {
            mHandler = new Handler(launcher.getMainLooper());
            mLauncher = launcher;
            mDownLoadAppMap = downloadAppMap;
        }

        public void onDestroy() {
            mHandler.removeCallbacksAndMessages(null);
            mLauncher = null;
            mDownLoadAppMap.clear();
        }

        @Override
        public void onStart(final String appName,final String pkgName, final String iconUrl,final boolean isUpdate) throws RemoteException {
            Log.e("hcdownload","onStart appName = " + appName);
            // Modify by louhan in 2017-10-20 for fix bug PRODUCTION-3490
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //add for dynamic download icon invalid status by louhan & weijiaqi 20170804
                    if (mLauncher == null) {
                        return;
                    }
                    // Added by louhan for fix bug PRODUCTION-8765
                    if (mLauncher.getHideAppList() != null && mLauncher.getHideAppList().toString().contains(pkgName)) {
                        return;
                    }
                    if (mLauncher.findIDownLoadCallbackByPkgName(pkgName) != null) {
                        return;
                    }
                    //add for dynamic download icon invalid status by louhan & weijiaqi 20170804
                    ShortcutInfo shortcutInfo = null;
                    if (isUpdate) {
//                final ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
//                        mWorkspace.getAllShortcutAndWidgetContainers();
//                int childCount = 0;
//                View view = null;
//                Object tag = null;
//                for (ShortcutAndWidgetContainer layout : childrenLayouts) {
//                    childCount = layout.getChildCount();
//                    for (int j = 0; j < childCount; j++) {
//                        view = layout.getChildAt(j);
//                        tag = view.getTag();
//                        if (tag instanceof ShortcutInfo) {
//                            final ShortcutInfo info = (ShortcutInfo) tag;
//                            final Intent intent = info.intent;
//                            if (intent != null) {
//                                final ComponentName componentName = intent.getComponent();
//                                if (componentName != null && componentName.getPackageName().equals(pkgName) && view instanceof IDownLoadCallback) {
//                                    info.setPkgName(pkgName);
//                                    info.setIconUrl(iconUrl);
//                                    mDownLoadAppMap.put(info, (IDownLoadCallback) view);
//                                    break;
//                                }
//
//                            }
//                        }
//                    }
//                }
                    } else {
                        setInfo(appName + "[" + pkgName + "]" + ">>onStart >>");
                        ArrayList<ItemInfo> tempList = new ArrayList<>();
                        Bitmap bitmap = ThemeHelper.getInstance(LauncherApplication.getAppContext()).getDownloadEmptyIcon();
                        Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
                        intent.setPackage(pkgName);
                        CharSequence title = appName;
                        shortcutInfo = new ShortcutInfo(intent, title, title, bitmap, UserHandleCompat.myUserHandle());
                        shortcutInfo.flags = AppInfo.DOWNLOADED_FLAG;
                        shortcutInfo.setIconUrl(iconUrl);
                        shortcutInfo.setPkgName(pkgName);
                        shortcutInfo.appDownLoadStatus = BitmapInfo.DOWNLOADING;
                        tempList.add(shortcutInfo);
                        mLauncher.mModel.addAndBindAddedWorkspaceItems(mLauncher.getApplicationContext(), tempList);
                        BubbleTextView bubbleTextView = new BubbleTextView(mLauncher);
                        mDownLoadAppMap.putIfAbsent(shortcutInfo, bubbleTextView);
                    }
//            Log.i("mDownLoadAppMap", "appName:"+appName + "|" + shortcutInfo.getPkgName() + " size = " + mDownLoadAppMap.size());
                }
            });
        }

        @Override
        public void onPause(final String appName, final String pkgName, final String iconUrl) throws RemoteException {
            Log.e("hcdownload","onPause appName = " + appName);
            Log.e(TAG, pkgName + " = onPause");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher == null) {
                        return;
                    }
                    mLauncher.updateDownloadItemByPkgName(pkgName, BitmapInfo.DOWNLOAD_PAUSE, -1, null, null);
                    IDownLoadCallback iDownLoadCallback = mLauncher.findIDownLoadCallbackByPkgName(pkgName);
                    if (iDownLoadCallback != null) {
                        iDownLoadCallback.onPause(appName, pkgName);
                    }
                }
            });

            setInfo(appName + "[" + pkgName + "]" + ">>onPause >>");
        }

        @Override
        public void onResume(final String appName, final String pkgName, final String iconUrl) throws RemoteException {
            Log.e("hcdownload","onResume appName = " + appName);
            Log.e(TAG, pkgName + " = onResume");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher == null) {
                        return;
                    }
                    mLauncher.updateDownloadItemByPkgName(pkgName, BitmapInfo.DOWNLOADING, -1, null, null);
                    IDownLoadCallback iDownLoadCallback = mLauncher.findIDownLoadCallbackByPkgName(pkgName);
                    if (iDownLoadCallback != null) {
                        iDownLoadCallback.onResume(appName, pkgName);
                    }
                }
            });


            setInfo(appName + "[" + pkgName + "]" + ">>onResume >>");
        }

        @Override
        public void onProcess(final String appName, String pkgName, final long process, final String iconUrl, boolean isUpdate) throws RemoteException {
            setInfo(appName + "[" + pkgName + "]" + "onProcess >>" + process);
        }

        @Override
        public void onInstalling(final String appName, final String pkgName) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher == null) {
                        return;
                    }
                    IDownLoadCallback iDownLoadCallback = mLauncher.findIDownLoadCallbackByPkgName(pkgName);
                    Log.e(TAG, "iDownLoadCallback + " + iDownLoadCallback);
                    if (iDownLoadCallback != null) {
                        iDownLoadCallback.onInstalling(pkgName, 0f);
                    }
                    mLauncher.updateDownloadItemByPkgName(pkgName, BitmapInfo.INSTALLING, 100, null, null);
                }
            });
            setInfo(appName + "[" + pkgName + "]" + "onInstalling");
        }

        @Override
        public void onInstalled(String appName, final String pkgName) throws RemoteException {
            setInfo(appName + "[" + pkgName + "]" + "onInstalled");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher == null) {
                        return;
                    }
                    mLauncher.deleteApp(pkgName);
                }
            });

        }

        @Override
        public void onCanceled(final String appName, final String pkgName) throws RemoteException {
            setInfo(appName + "[" + pkgName + "]" + "onCanceled");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher == null) {
                        return;
                    }
                    mLauncher.deleteApp(pkgName);
                    DownloadAppInfo downloadAppInfo = mLauncher.findDownloadAppInfoByPkgName(pkgName);
                    if (downloadAppInfo != null && downloadAppInfo.getShortcutInfo() != null) {
                        mDownLoadAppMap.remove(downloadAppInfo.getShortcutInfo());
                    }
                    if (downloadAppInfo != null && downloadAppInfo.getDownLoadCallback() != null) {
                        downloadAppInfo.getDownLoadCallback().onCanceled(appName, pkgName);
                        mLauncher.removeItem((View) downloadAppInfo.getDownLoadCallback(), downloadAppInfo.getShortcutInfo(), true);
                    }
                    if (!mLauncher.mWorkspace.isInAppManageMode()) {
                        mLauncher.mWorkspace.removeExtraEmptyScreen(false, false);
                    }

//                    final ContentResolver cr = getApplicationContext().getContentResolver();
//                    Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
//                            new String[]{LauncherSettings.Favorites._ID, LauncherSettings.Favorites.INTENT},
//                            LauncherSettings.Favorites.TITLE + "=?", new String[]{appName}, null);
//
//                    while (c.moveToNext()) {
//                        final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
//                        final long id = c.getLong(idIndex);
//                        final Uri uri = LauncherSettings.Favorites.getContentUri(id);
//                        cr.delete(uri, null, null);
//                    }
//                    c.close();
                }
            });
        }

        @Override
        public void onComplete(final String appName, final String pkgName) throws RemoteException {
            setInfo(appName + "[" + pkgName + "]" + "onComplete");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher == null) {
                        return;
                    }
                    mLauncher.deleteApp(pkgName);
                    IDownLoadCallback iDownLoadCallback = mLauncher.findIDownLoadCallbackByPkgName(pkgName);
                    if (iDownLoadCallback != null) {
                        iDownLoadCallback.onComplete(appName, pkgName);
                    }
                }
            });
        }

        @Override
        public void onDownloadError(final String appName, final String pkgName, final String err) throws RemoteException {
            Log.e("hcdownload","onDownloadError appName = " + appName);
            setInfo(appName + "[" + pkgName + "]" + "onDownloadError>>" + err);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher == null) {
                        return;
                    }
                    mLauncher.deleteApp(pkgName);
                    IDownLoadCallback iDownLoadCallback = mLauncher.findIDownLoadCallbackByPkgName(pkgName);
                    if (iDownLoadCallback != null) {
                        iDownLoadCallback.onDownloadError(appName, pkgName, err);
                    }
                    mLauncher.updateDownloadItemByPkgName(pkgName, BitmapInfo.DOWNLOAD_ERROR, 100, null, null);
                }
            });
        }

        @Override
        public void onInstallError(final String appName, final String pkgName, final String err) throws RemoteException {
            setInfo(appName + "[" + pkgName + "]" + "onInstallError>>" + err);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher == null) {
                        return;
                    }
                    mLauncher.deleteApp(pkgName);
                    IDownLoadCallback iDownLoadCallback = mLauncher.findIDownLoadCallbackByPkgName(pkgName);
                    if (iDownLoadCallback != null) {
                        iDownLoadCallback.onInstallError(appName, pkgName, err);
                    }
                    mLauncher.updateDownloadItemByPkgName(pkgName, BitmapInfo.INSTALL_ERROR, 100, null, null);
                }
            });
        }

        @Override
        public void onProcessAll(final AppDataForEx[] data) throws RemoteException {
            Log.d(TAG, "TTTT onProcessAll>>>>>" + data.length);
            if (mLauncher == null) {
                return;
            }
            mLauncher.setAllDatas(data);
        }
    }

    private static void setInfo(final String Info) {
        Log.d(TAG, "setInfo>>>>>" + Info);
    }

    private AppData getOrCreateAppData(final String appName, String pkgName, final String iconUrl) {

        for (AppData appData : mAppDataList) {
            if (appData.mPkgName != null && appData.mPkgName.equals(pkgName)) {
                return appData;
            }
        }
        AppData appData = new AppData();
        appData.mAppName = appName;
        appData.mPkgName = pkgName;
        appData.mIconUrl = iconUrl;

        mAppDataList.add(appData);
        return appData;
    }

    private void deleteApp(String pkgName) {
        ArrayList<AppData> tmpList = new ArrayList<>();
        tmpList.addAll(mAppDataList);
        for (AppData appData : tmpList) {
            if (appData.mPkgName != null && appData.mPkgName.equals(pkgName)) {
                mAppDataList.remove(appData);
                break;
            }
        }
    }

    private void setAllDatas(final AppDataForEx[] data) {

        if (data == null) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (AppDataForEx appDataForEx : data) {
                    if (appDataForEx != null) {
                        Log.d(TAG, "setAllDatas>>>>>" + appDataForEx.mAppName + ">>"
                                + appDataForEx.mState + ">>" + appDataForEx.mProcess);
                        Log.e("hcdownload","setAllDatas appName = " + appDataForEx.mAppName);
                        AppData appData = getOrCreateAppData(appDataForEx.mAppName, appDataForEx.mPkgName, appDataForEx.mIconUrl);
                        appData.mProcess = appDataForEx.mProcess;
                        appData.mState = appDataForEx.mState;

                        Iterator iterator = mDownLoadAppMap.entrySet().iterator();
                        Log.e(TAG, "size = " + mDownLoadAppMap.size());
                        while (iterator.hasNext()) {
                            HashMap.Entry entry = (HashMap.Entry) iterator.next();
                            ShortcutInfo shortcutInfo = (ShortcutInfo) entry.getKey();
                            IDownLoadCallback iDownLoadCallback = (IDownLoadCallback) entry.getValue();
                            if (shortcutInfo != null && shortcutInfo.getPkgName() != null && shortcutInfo.getPkgName().equals(appData.mPkgName)) {
                                updateDownloadItemByPkgName(appData.mPkgName, BitmapInfo.DOWNLOADING, appData.mProcess, null, null);
                                if (iDownLoadCallback != null) {
                                    iDownLoadCallback.onProcess(appData.mAppName, appData.mPkgName, appData.mProcess);
                                }
                            }
                        }

                    }
                }
            }
//                mAdapter.notifyDataSetChanged();

        });
    }

    // Modify by louhan in 2017-10-12 for fix bug GMOS-9449
    public void updateDownloadItemByPkgName(final String pkgName, int state, long process, Intent intent, Bitmap bitmap) {
        Log.d(TAG, "pkgName + " + pkgName);
        final ContentValues values = new ContentValues();
        if (-1 != state)
            values.put(LauncherSettings.Favorites.STATUS, state);
        if (-1 != process)
            values.put(LauncherSettings.Favorites.PROCESS, process);
        if (null != intent)
            values.put(LauncherSettings.Favorites.INTENT, intent.toUri(0));
        if (null != bitmap)
            values.put(LauncherSettings.Favorites.ICON, Utilities.flattenBitmap(bitmap));

        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                Launcher.this.getContentResolver().update(
                        LauncherSettings.Favorites.CONTENT_URI,
                        values,
                        LauncherSettings.BaseLauncherColumns.INTENT + " LIKE ? ",
                        new String[]{"%" + pkgName + "%"});
            }
        };

        runOnDownLoadThread(updateRunnable);

    }

    public void updateDownloadItem(final String titleName, int state, long process, Intent intent, Bitmap bitmap) {
        Log.e(TAG, "titleName + " + titleName);
        final ContentValues values = new ContentValues();
        if (-1 != state)
            values.put(LauncherSettings.Favorites.STATUS, state);
        if (-1 != process)
            values.put(LauncherSettings.Favorites.PROCESS, process);
        if (null != intent)
            values.put(LauncherSettings.Favorites.INTENT, intent.toUri(0));
        if (null != bitmap)
            values.put(LauncherSettings.Favorites.ICON, Utilities.flattenBitmap(bitmap));

        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                Launcher.this.getContentResolver().update(
                        LauncherSettings.Favorites.CONTENT_URI,
                        values,
                        LauncherSettings.BaseLauncherColumns.TITLE + "= ?",
                        new String[]{titleName});
            }
        };

        runOnDownLoadThread(updateRunnable);

    }
    // Modify by louhan in 2017-10-20 for fix bug PRODUCTION-3490
    public boolean QueryDownloadItemByPkgName(final String pkgName) {
        Log.d(TAG, "pkgName --> " + pkgName);
        final ContentResolver cr = getApplicationContext().getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
                new String[]{LauncherSettings.Favorites._ID, LauncherSettings.Favorites.INTENT},
                LauncherSettings.BaseLauncherColumns.INTENT + " LIKE ? ", new String[]{"%" + pkgName + "%"}, null);
        if (c != null) {
            while ( c.moveToNext()) {
                try {
                    final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
                    Intent intent = Intent.parseUri(c.getString(intentIndex), 0);
                    Log.d(TAG, "intent --> " + intent.toString());
                    if ((intent.getComponent() != null && intent.getComponent().getPackageName() != null && intent.getComponent().getPackageName().equals(pkgName)) || intent.getPackage() != null) {
                        c.close();
                        return true;
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            c.close();
        }
        return false;
    }
    //add for dynamic download icon by louhan & weijiaqi 20170804

    public void addApps(final Folder folder, final List<? extends ItemInfo> apps) {
        getModel().addWorkspaceItemsFromFolder(this, apps);
    }

    @Override
    public void removeApps(final Folder folder, final List<? extends ItemInfo> apps) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                removeApps(folder, apps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        getModel().removeWorkspaceItemsToFolder(this, folder, apps);
    }

    /** The different states that Launcher can be in. */
    public enum State { NONE, WORKSPACE, TRANSITION_EFFECT, APPS, APPS_SPRING_LOADED, WIDGETS,
        WIDGETS_SPRING_LOADED, WALLPAPER_PICKER, GALLERY_LOADING}

    @Thunk State mState = State.WORKSPACE;
    @Thunk LauncherStateTransitionAnimation mStateTransitionAnimation;

    //move by rongwenzhao from 6.0 to mtk7.0 2017-7-3 begin
    private ShakeListener mShakeListener;
    //move by rongwenzhao from 6.0 to mtk7.0 2017-7-3 end

    private boolean mIsSafeModeEnabled;

    static final int APPWIDGET_HOST_ID = 1024;
    public static final int EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT = 300;
    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;
    private static final int ACTIVITY_START_DELAY = 1000;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    @Thunk static int NEW_APPS_ANIMATION_DELAY = 500;

    private final BroadcastReceiver mCloseSystemDialogsReceiver
            = new CloseSystemDialogsIntentReceiver();

    private LayoutInflater mInflater;

    /**
     * Added by gaoquan 2017.6.1
     */
    //-------------------------------start--------------///

    /// M: Add for launcher unread shortcut feature. @{
    public static final int MAX_UNREAD_COUNT_TWO = 99;
    public static final int MAX_UNREAD_COUNT = 999;

    /// M: Add for unread message feature.
    private BadgeUnreadLoader mUnreadLoader = null;
    public boolean mUnreadLoadCompleted = false;
    private boolean mBindingWorkspaceFinished = false;
    public boolean mBindingAppsFinished = false;
    /// @}

    //-------------------------------end--------------///

    /**
     * Added by gaoquan 2017.7.12
     */
    //-------------------------------start--------------///
    @Thunk ImageView mFolderBlur;
    private boolean isShowFolderBlur = false;
    //-------------------------------end--------------///
    /**
     * Added by gaoquan 2017.7.20
     */
    //-------------------------------start--------------///
    private PackageCircle mPackageCircle = null;
    //-------------------------------end--------------///
    /**
     * Added by gaoquan 2017.7.25
     */
    //-------------------------------start--------------///
    public boolean isShowAppSearchWindow = false;
    //-------------------------------end--------------///
    /**
     * Added by gaoquan 2017.8.9
     */
    //-------------------------------start--------------///
    private boolean defaultNeedCustomContent = false;
    private static final String CUSTOM_PAGE_OPEN = "custom_page_open";
    //-------------------------------end--------------///

    public PackageReceiver mPackageReceiver = null;

    @Thunk Workspace mWorkspace;
    private View mLauncherView;
    private View mPageIndicators;
    @Thunk DragLayer mDragLayer;
    private DragController mDragController;
    private FrameLayout frameLayout;
    public View mWeightWatcher;

    private View mOverViewButtoContainer;
    public View mTransitionEffectsContainer;
    public WallpaperContainerViewGM mWallpaperPickerContainer;

    private AppWidgetManagerCompat mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    @Thunk PendingAddItemInfo mPendingAddInfo = new PendingAddItemInfo();
    private LauncherAppWidgetProviderInfo mPendingAddWidgetInfo;
    private int mPendingAddWidgetId = -1;

    private int[] mTmpAddItemCellCoordinates = new int[2];

    @Thunk Hotseat mHotseat;
    private ViewGroup mOverviewPanel;

    private View mAllAppsButton;
    private View mWidgetsButton;

    private SearchDropTargetBar mSearchDropTargetBar;

    // Main container view for the all apps screen.
//    @Thunk AllAppsContainerView mAppsView;

    // Main container view and the model for the widget tray screen.
    //@Thunk WidgetsContainerView mWidgetsView; //delete by rongwenzhao widget list show horizontal 2017-6-27
    //add by rongwenzhao widget list show horizontal 2017-6-27 begin
    @Thunk
    WidgetsContainerViewGM mWidgetsView;

    //container of widgets list in first level
    WidgetsRecyclerView mWidgetsRecyclerView;

    //horizontal scrollerView container of widgets in second level(shown by click widgeticon with muth widgets )
    WidgetHorizontalScrollView mWidgetHorizontalScrollView;

    //added by liuning for multi apps move on 2017/7/18 start
    MoveViewsRestContainer mMoveViewsRestContainer;
    //added by liuning for multi apps move on 2017/7/18 end

    //add for first click widget add button
    private boolean isFirstClickWidgetButton = true;
    //add by rongwenzhao widget list show horizontal 2017-6-27 end

    @Thunk
    WidgetsModel mWidgetsModel;

    private boolean mAutoAdvanceRunning = false;
    private AppWidgetHostView mQsb;

    private Bundle mSavedState;
    // We set the state in both onCreate and then onNewIntent in some cases, which causes both
    // scroll issues (because the workspace may not have been measured yet) and extra work.
    // Instead, just save the state that we need to restore Launcher to, and commit it in onResume.
    private State mOnResumeState = State.NONE;

    private SpannableStringBuilder mDefaultKeySsb = null;

    @Thunk boolean mWorkspaceLoading = true;

    private boolean mPaused = true;
    private boolean mRestoring;
    private boolean mWaitingForResult;
    private boolean mOnResumeNeedsLoad;

    private ArrayList<Runnable> mBindOnResumeCallbacks = new ArrayList<Runnable>();
    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<Runnable>();

    private Bundle mSavedInstanceState;

    private LauncherModel mModel;
    private IconCache mIconCache;
    @Thunk boolean mUserPresent = true;
    private boolean mVisible = false;
    private boolean mHasFocus = false;
    private boolean mHasFocusOnPause = false;//add by jubingcheng for fix: enter app and press homekey immediately, if launcher is onPause, it will go to homepage[GMOS-6893] and if app is landscape, it will stuck screen[GMOS-8276] on 2017/09/22
    private boolean mAttached = false;

    private LauncherClings mClings;

    private static LongArrayMap<FolderInfo> sFolders = new LongArrayMap<>();

    private View.OnTouchListener mHapticFeedbackTouchListener;

    // Related to the auto-advancing of widgets
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = 250;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    @Thunk HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance =
        new HashMap<View, AppWidgetProviderInfo>();

    // Determines how long to wait after a rotation before restoring the screen orientation to
    // match the sensor state.
    private final int mRestoreScreenOrientationDelay = 500;

    @Thunk Drawable mWorkspaceBackgroundDrawable;

    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<Integer>();
    private static final boolean DISABLE_SYNCHRONOUS_BINDING_CURRENT_PAGE = false;

    static final ArrayList<String> sDumpLogs = new ArrayList<String>();
    static Date sDateStamp = new Date();
    static DateFormat sDateFormat =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    static long sRunStart = System.currentTimeMillis();
    static final String CORRUPTION_EMAIL_SENT_KEY = "corruptionEmailSent";

    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    // Holds the page that we need to animate to, and the icon views that we need to animate up
    // when we scroll to that page on resume.
    @Thunk ImageView mFolderIconImageView;
    private Bitmap mFolderIconBitmap;
    private Canvas mFolderIconCanvas;
    private Rect mRectForFolderAnimation = new Rect();

    private DeviceProfile mDeviceProfile;

    private boolean mMoveToDefaultScreenFromNewIntent;

    // This is set to the view that launched the activity that navigated the user away from
    // launcher. Since there is no callback for when the activity has finished launching, enable
    // the press state and keep this reference to reset the press state when we return to launcher.
    private BubbleTextView mWaitingForResume;
    //add by huanghaihao in 2017-7-14 for adding more app in folder start
    private LauncherProviderChangeListener mLauncherProviderChangeListener;

    protected static HashMap<String, CustomAppWidget> sCustomAppWidgets =
            new HashMap<String, CustomAppWidget>();

    static {
        if (TestingUtils.ENABLE_CUSTOM_WIDGET_TEST) {
            TestingUtils.addDummyWidget(sCustomAppWidgets);
        }
    }

    @Thunk Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            if (mWorkspace != null) {
                mWorkspace.buildPageHardwareLayers();
            }
        }
    };

    private static PendingAddArguments sPendingAddItem;

    @Thunk static class PendingAddArguments {
        int requestCode;
        Intent intent;
        long container;
        long screenId;
        int cellX;
        int cellY;
        int appWidgetId;
    }

    private Stats mStats;
    FocusIndicatorView mFocusHandler;
    private boolean mRotationEnabled = false;

    @Thunk void setOrientation() {
        if (mRotationEnabled) {
            unlockScreenOrientation(true);
        } else {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
    }

    /**
     * Added by gaoquan 2017.6.1
     */
    //-------------------------------start--------------///

    public BadgeUnreadLoader getUnreadLoader() {
        return mUnreadLoader;
    }

    //-------------------------------end--------------///
    /**
     * Added by gaoquan 2017.7.20
     */
    //-------------------------------start--------------///
    public PackageCircle getPackageCircle(){
        return mPackageCircle;
    }
    //-------------------------------end--------------///

    private Runnable mUpdateOrientationRunnable = new Runnable() {
        public void run() {
            setOrientation();
        }
    };

    /// M: If workspcae no initialized, save last restore workspace screen.
    private int mCurrentWorkSpaceScreen = PagedView.INVALID_RESTORE_PAGE;
    private Bundle savedInstanceState;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "PROCEDURE onCreate() start");

        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.preOnCreate();
        }

        super.onCreate(savedInstanceState);
        if (mInstance != null) {
            LauncherApplication.needRestart = true;
            Log.e(TAG, "PROCEDURE onCreate() multi-instance needRestart! return!");
            return;
        }
        mInstance = this;
        // added by jubingcheng for DisplayMetricsUtils init start on 2017/7/12
        DisplayMetricsUtils.init(this);
        DisplayMetricsUtils.calibration();
        // added by jubingcheng for DisplayMetricsUtils init end on 2017/7/12
        LauncherAppState app = LauncherAppState.getInstance();

        // Load configuration-specific DeviceProfile
        mDeviceProfile = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE ?
                app.getInvariantDeviceProfile().landscapeProfile
                : app.getInvariantDeviceProfile().portraitProfile;

        mSharedPrefs = Utilities.getPrefs(this);
        mIsSafeModeEnabled = getPackageManager().isSafeMode();
        mModel = app.setLauncher(this);
        mModel.setItemUpdateListener(this);
        mIconCache = app.getIconCache();

        /**
         * Added by gaoquan 2017.6.1
         */
        //-------------------------------start--------------///
        /**M: added for unread feature, load and bind unread info.@{**/
        if (getResources().getBoolean(R.bool.config_unreadSupport)) {
            mUnreadLoader = new BadgeUnreadLoader(getApplicationContext());
            // initialize unread loader
            mUnreadLoader.initialize(this);
            mUnreadLoader.loadAndInitUnreadShortcuts();
        }
        /**@}**/
        //-------------------------------end--------------///
        /**
         * Added by gaoquan 2017.7.20
         */
        //-------------------------------start--------------///
        mPackageCircle = new PackageCircle(this);
        mPackageCircle.initialize(this);
        //-------------------------------end--------------///

        if(Build.VERSION.SDK_INT >= 26) {
            mPackageReceiver = new PackageReceiver();
            IntentFilter mPackageIntentFilter = new IntentFilter();
            mPackageIntentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            mPackageIntentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            mPackageIntentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            mPackageIntentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            mPackageIntentFilter.addDataScheme("package");
            registerReceiver(mPackageReceiver, mPackageIntentFilter);
        }

        mDragController = new DragController(this);
        mInflater = getLayoutInflater();
        mStateTransitionAnimation = new LauncherStateTransitionAnimation(this);

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "(Launcher)onCreate: savedInstanceState = " + savedInstanceState
                    + ", mModel = " + mModel + ", mIconCache = " + mIconCache + ", this = " + this);
        }
        mStats = new Stats(this);

        mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);

        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();

        // If we are getting an onCreate, we can actually preempt onResume and unset mPaused here,
        // this also ensures that any synchronous binding below doesn't re-trigger another
        // LauncherModel load.
        mPaused = false;

        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing(
                    Environment.getExternalStorageDirectory() + "/launcher");
        }

        setContentView(R.layout.launcher);
        bindAppService();//add for dynamic download icon by louhan & weijiaqi 20170804
        app.getInvariantDeviceProfile().landscapeProfile.setSearchBarHeight(getSearchBarHeight());
        app.getInvariantDeviceProfile().portraitProfile.setSearchBarHeight(getSearchBarHeight());
        setupViews();
        mDeviceProfile.layout(this);

        lockAllApps();

        mSavedState = savedInstanceState;
        restoreState(mSavedState);

        if (PROFILE_STARTUP) {
            android.os.Debug.stopMethodTracing();
        }

        //Add by chenchao start 2017.10.30
        initIconClassifyManager();
        //Add by chenchao end 2017.10.30
        this.savedInstanceState=savedInstanceState;

        if (!mRestoring) {
            if (DISABLE_SYNCHRONOUS_BINDING_CURRENT_PAGE) {
                // If the user leaves launcher, then we should just load items asynchronously when
                // they return.
                mModel.startLoader(PagedView.INVALID_RESTORE_PAGE);
            } else {
                // We only load the page synchronously if the user rotates (or triggers a
                // configuration change) while launcher is in the foreground
                Log.d(TAG,"mModel.startLoader");
                mModel.startLoader(mWorkspace.getRestorePage());
            }
        }

        // For handling default keys
        mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(mDefaultKeySsb, 0);

        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mCloseSystemDialogsReceiver, filter);

        mRotationEnabled = getResources().getBoolean(R.bool.allow_rotation);
        // In case we are on a device with locked rotation, we should look at preferences to check
        // if the user has specifically allowed rotation.
        if (!mRotationEnabled) {
            mRotationEnabled = Utilities.isAllowRotationPrefEnabled(getApplicationContext());
        }

        // On large interfaces, or on devices that a user has specifically enabled screen rotation,
        // we want the screen to auto-rotate based on the current orientation
        setOrientation();

        //add by rongwenzhao from 6.0 to mtk7.0 2017-7-3 begin
        mShakeListener = new ShakeListener(this);
        mShakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {
                if(!mPaused){
                    DLog.d(TAG,"Launcher Phone is Shaking =============");
                    stopShakeListening();
                    mWorkspace.neatIcon();
                }
            }
        });
        //add by rongwenzhao from 6.0 to mtk7.0 2017-7-3 end

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onCreate(savedInstanceState);
        }

        if (shouldShowIntroScreen()) {
            showIntroScreen();
        } else {
            showFirstRunActivity();
//            showFirstRunClings();
        }
        Log.v(TAG, "PROCEDURE onCreate() end");
    }

    public static Launcher getInstance() {
        return mInstance;
    }

    private void initIconClassifyManager() {
        new Thread() {
            @Override
            public void run() {
                IconClassifyManager.loadClassifyInfos();
            }
        }.start();
    }
    @Override
    public void onSettingsChanged(String settings, boolean value) {
        if (Utilities.ALLOW_ROTATION_PREFERENCE_KEY.equals(settings)) {
            mRotationEnabled = value;
            if (!waitUntilResume(mUpdateOrientationRunnable, true)) {
                mUpdateOrientationRunnable.run();
            }
        }
    }

    private LauncherCallbacks mLauncherCallbacks;

    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPostCreate(savedInstanceState);
        }
    }

    /**
     * Call this after onCreate to set or clear overlay.
     */
    public void setLauncherOverlay(LauncherOverlay overlay) {
        if (overlay != null) {
            overlay.setOverlayCallbacks(new LauncherOverlayCallbacksImpl());
        }
        mWorkspace.setLauncherOverlay(overlay);
    }

    public boolean setLauncherCallbacks(LauncherCallbacks callbacks) {
        mLauncherCallbacks = callbacks;
        mLauncherCallbacks.setLauncherSearchCallback(new Launcher.LauncherSearchCallbacks() {
            private boolean mWorkspaceImportanceStored = false;
            private boolean mHotseatImportanceStored = false;
            private int mWorkspaceImportanceForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
            private int mHotseatImportanceForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

            @Override
            public void onSearchOverlayOpened() {
                if (mWorkspaceImportanceStored || mHotseatImportanceStored) {
                    return;
                }
                // The underlying workspace and hotseat are temporarily suppressed by the search
                // overlay. So they sholudn't be accessible.
                if (mWorkspace != null) {
                    mWorkspaceImportanceForAccessibility =
                            mWorkspace.getImportantForAccessibility();
                    mWorkspace.setImportantForAccessibility(
                            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                    mWorkspaceImportanceStored = true;
                }
                if (mHotseat != null) {
                    mHotseatImportanceForAccessibility = mHotseat.getImportantForAccessibility();
                    mHotseat.setImportantForAccessibility(
                            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                    mHotseatImportanceStored = true;
                }
            }

            @Override
            public void onSearchOverlayClosed() {
                if (mWorkspaceImportanceStored && mWorkspace != null) {
                    mWorkspace.setImportantForAccessibility(mWorkspaceImportanceForAccessibility);
                }
                if (mHotseatImportanceStored && mHotseat != null) {
                    mHotseat.setImportantForAccessibility(mHotseatImportanceForAccessibility);
                }
                mWorkspaceImportanceStored = false;
                mHotseatImportanceStored = false;
            }
        });
        return true;
    }

    @Override
    public void onLauncherProviderChange(boolean isDownloadingUpdate) {
        Log.d(TAG, "onLauncherProviderChange");
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onLauncherProviderChange();
        }
        //add by huanghaihao in 2017-7-14 for adding more app in folder start
        if (mLauncherProviderChangeListener != null) {
            mLauncherProviderChangeListener.onLauncherProviderChange(isDownloadingUpdate);
        }
        //add by huanghaihao in 2017-7-14 for adding more app in folder end
    }

    /**
     * Updates the bounds of all the overlays to match the new fixed bounds.
     */
    public void updateOverlayBounds(Rect newBounds) {
//        mAppsView.setSearchBarBounds(newBounds);
        mWidgetsView.setSearchBarBounds(newBounds);
    }

    /**
     * Added by gaoquan 2017.8.9
     */
    //-------------------------------start--------------///

    public void closeCustomContent() {
        Log.i(TAG, "closeCustomContent");
        if (mWorkspace != null && mWorkspace.hasCustomContent()) {
            mWorkspace.removeCustomContentPage();
        }
        hasInitCustomPageManager = false;
    }

    public void openCustomContent() {
        if (mWorkspace != null && !mWorkspace.hasCustomContent() && !hasInitCustomPageManager) {
            Log.i(TAG, "openCustomContent");
        }
    }
    //-------------------------------end--------------///

    /** To be overridden by subclasses to hint to Launcher that we have custom content */
    public boolean needCustomContentToLeft() {
//        if (mLauncherCallbacks != null) {
//            return mLauncherCallbacks.needCustomContentToLeft();
//        }
        /**
         * Added by gaoquan 2017.8.9
         */
        //-------------------------------start--------------///
        return mSharedPrefs.getBoolean(CUSTOM_PAGE_OPEN, defaultNeedCustomContent);
        //-------------------------------end--------------///
    }

    /**
     * To be overridden by subclasses to populate the custom content container and call
     * {@link #addToCustomContentPage}. This will only be invoked if
     * {@link #needCustomContentToLeft()} is {@code true}.
     */
    protected void populateCustomContentContainer() {
        //if (mLauncherCallbacks != null) {
        //    mLauncherCallbacks.populateCustomContentContainer();
        //}
    }

    /**
     * Invoked by subclasses to signal a change to the {@link #addCustomContentToLeft} value to
     * ensure the custom content page is added or removed if necessary.
     */
    protected void invalidateHasCustomContentToLeft() {
        if (mWorkspace == null || mWorkspace.getScreenOrder().isEmpty()) {
            // Not bound yet, wait for bindScreens to be called.
            return;
        }

        if (!mWorkspace.hasCustomContent() && needCustomContentToLeft()) {
            // Create the custom content page and call the subclass to populate it.
            mWorkspace.createCustomContentContainer();
            populateCustomContentContainer();
        } else if (mWorkspace.hasCustomContent() && !needCustomContentToLeft()) {
            mWorkspace.removeCustomContentPage();
        }
    }

    public Stats getStats() {
        return mStats;
    }

    public LayoutInflater getInflater() {
        return mInflater;
    }

    public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !isWorkspaceLoading();
    }

    public int getViewIdForItem(ItemInfo info) {
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        // This cast is safe as long as the id < 0x00FFFFFF
        // Since we jail all the dynamically generated views, there should be no clashes
        // with any other views.
        return (int) info.id;
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    private long completeAdd(PendingAddArguments args) {
        long screenId = args.screenId;
        if (args.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            // When the screen id represents an actual screen (as opposed to a rank) we make sure
            // that the drop page actually exists.
            screenId = ensurePendingDropLayoutExists(args.screenId);
        }

        switch (args.requestCode) {
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(args.intent, args.container, screenId, args.cellX,
                        args.cellY);
                break;
            case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(args.appWidgetId, args.container, screenId, null, null);
                break;
            case REQUEST_RECONFIGURE_APPWIDGET:
                completeRestoreAppWidget(args.appWidgetId);
                break;
        }
        // Before adding this resetAddInfo(), after a shortcut was added to a workspace screen,
        // if you turned the screen off and then back while in All Apps, Launcher would not
        // return to the workspace. Clearing mAddInfo.container here fixes this issue
        resetAddInfo();
        return screenId;
    }

    private void handleActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        // Reset the startActivity waiting flag
        setWaitingForResult(false);
        final int pendingAddWidgetId = mPendingAddWidgetId;
        mPendingAddWidgetId = -1;

        Runnable exitSpringLoaded = new Runnable() {
            @Override
            public void run() {
                exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED),
                        EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
            }
        };

        if (requestCode == REQUEST_BIND_APPWIDGET) {
            // This is called only if the user did not previously have permissions to bind widgets
            final int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(appWidgetId, mPendingAddInfo, null,
                        mPendingAddWidgetInfo, ON_ACTIVITY_RESULT_ANIMATION_DELAY);

                // When the user has granted permission to bind widgets, we should check to see if
                // we can inflate the default search bar widget.
                getOrCreateQsbBar();
            }
            return;
        } else if (requestCode == REQUEST_PICK_WALLPAPER) {
            if (resultCode == RESULT_OK && mWorkspace.isInOverviewMode()) {
                // User could have free-scrolled between pages before picking a wallpaper; make sure
                // we move to the closest one now to avoid visual jump.
                mWorkspace.setCurrentPage(mWorkspace.getPageNearestToCenterOfScreen());
                showWorkspace(false);
            }
            return;
        }else if (requestCode == IMAGE_PICK) {
            // Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 start
            // Added by wjq in 2017-8-11 for fix bug GMOS-1494 start
//            if (null != mWallpaperPickerContainer)
//                mWallpaperPickerContainer.setWallpaperFromGallery(data);
            // Added by wjq in 2017-8-11 for fix bug GMOS-1494 end
            Log.e(TAG,mState.name() + " | " + mWorkspace.getState().name());
            if (mWorkspace.getState() == Workspace.State.NORMAL) {
                mState = State.WORKSPACE;
                mWallpaperPickerContainer.setVisibility(View.GONE);
            }
//            else
//            {
//                mState = State.WALLPAPER_PICKER;
//            }
            // Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 end
        }

        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET ||
                requestCode == REQUEST_CREATE_APPWIDGET);

        final boolean workspaceLocked = isWorkspaceLocked();

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onActivityResult: requestCode = " + requestCode
                    + ", resultCode = " + resultCode + ", data = " + data
                    + ", mPendingAddInfo = " + mPendingAddInfo);
        }

        // We have special handling for widgets
        if (isWidgetDrop) {
            final int appWidgetId;
            int widgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    : -1;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }

            final int result;
            if (appWidgetId < 0 || resultCode == RESULT_CANCELED) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not " +
                        "returned from the widget configuration activity.");
                result = RESULT_CANCELED;
                completeTwoStageWidgetDrop(result, appWidgetId);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        exitSpringLoadedDragModeDelayed(false, 0, null);
                    }
                };
                if (workspaceLocked) {
                    // No need to remove the empty screen if we're mid-binding, as the
                    // the bind will not add the empty screen.
                    mWorkspace.postDelayed(onComplete, ON_ACTIVITY_RESULT_ANIMATION_DELAY);
                } else {
                    mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                            ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
                }
            } else {
                if (!workspaceLocked) {
                    if (mPendingAddInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                        // When the screen id represents an actual screen (as opposed to a rank)
                        // we make sure that the drop page actually exists.
                        mPendingAddInfo.screenId =
                                ensurePendingDropLayoutExists(mPendingAddInfo.screenId);
                    }
                    final CellLayout dropLayout = mWorkspace.getScreenWithId(mPendingAddInfo.screenId);

                    dropLayout.setDropPending(true);
                    final Runnable onComplete = new Runnable() {
                        @Override
                        public void run() {
                            completeTwoStageWidgetDrop(resultCode, appWidgetId);
                            dropLayout.setDropPending(false);
                        }
                    };
                    mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                            ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
                } else {
                    PendingAddArguments args = preparePendingAddArgs(requestCode, data, appWidgetId,
                            mPendingAddInfo);
                    sPendingAddItem = args;
                }
            }
            return;
        }

        if (requestCode == REQUEST_RECONFIGURE_APPWIDGET) {
            if (resultCode == RESULT_OK) {
                // Update the widget view.
                PendingAddArguments args = preparePendingAddArgs(requestCode, data,
                        pendingAddWidgetId, mPendingAddInfo);
                if (workspaceLocked) {
                    sPendingAddItem = args;
                } else {
                    completeAdd(args);
                }
            }
            // Leave the widget in the pending state if the user canceled the configure.
            return;
        }

        // The pattern used here is that a user PICKs a specific application,
        // which, depending on the target, might need to CREATE the actual target.

        // For example, the user would PICK_SHORTCUT for "Music playlist", and we
        // launch over to the Music app to actually CREATE_SHORTCUT.
        if (resultCode == RESULT_OK && mPendingAddInfo.container != ItemInfo.NO_ID) {
            /// M.ALPS01808563, Modify -1 to pendingAddWidgetId;
            final PendingAddArguments args = preparePendingAddArgs(requestCode,
                    data, pendingAddWidgetId, mPendingAddInfo);
            if (isWorkspaceLocked()) {
                sPendingAddItem = args;
            } else {
                completeAdd(args);
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            }
        } else if (resultCode == RESULT_CANCELED) {
            mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                    ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
        }
        mDragLayer.clearAnimatedView();

    }

    @Override
    protected void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        handleActivityResult(requestCode, resultCode, data);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onActivityResult(requestCode, resultCode, data);
        }
    }

    /** @Override for MNC */
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CALL_PHONE && sPendingAddItem != null
                && sPendingAddItem.requestCode == REQUEST_PERMISSION_CALL_PHONE) {
            View v = null;
            CellLayout layout = getCellLayout(sPendingAddItem.container, sPendingAddItem.screenId);
            if (layout != null) {
                v = layout.getChildAt(sPendingAddItem.cellX, sPendingAddItem.cellY);
            }
            Intent intent = sPendingAddItem.intent;
            sPendingAddItem = null;
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivitySafely(v, intent, null);
            } else {
                // TODO: Show a snack bar with link to settings
                Toast.makeText(this, getString(R.string.msg_no_phone_permission,
                        getString(R.string.app_name)), Toast.LENGTH_SHORT).show();
            }
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onRequestPermissionsResult(requestCode, permissions,
                    grantResults);
        }
    }

    private PendingAddArguments preparePendingAddArgs(int requestCode, Intent data, int
            appWidgetId, ItemInfo info) {
        PendingAddArguments args = new PendingAddArguments();
        args.requestCode = requestCode;
        args.intent = data;
        args.container = info.container;
        args.screenId = info.screenId;
        args.cellX = info.cellX;
        args.cellY = info.cellY;
        args.appWidgetId = appWidgetId;
        return args;
    }

    /**
     * Check to see if a given screen id exists. If not, create it at the end, return the new id.
     *
     * @param screenId the screen id to check
     * @return the new screen, or screenId if it exists
     */
    private long ensurePendingDropLayoutExists(long screenId) {
        CellLayout dropLayout = mWorkspace.getScreenWithId(screenId);
        if (dropLayout == null) {
            // it's possible that the add screen was removed because it was
            // empty and a re-bind occurred
            mWorkspace.addExtraEmptyScreen();
            return mWorkspace.commitExtraEmptyScreen();
        } else {
            return screenId;
        }
    }

    @Thunk void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {

        ///M. ALPS02202279.
        if (mWorkspace == null) {
            LauncherLog.d(TAG, "completeTwoStageWidgetDrop: mWorkspace = " + mWorkspace
                + ",mPendingAddInfo:" + mPendingAddInfo);
            return;
        }
        CellLayout cellLayout = mWorkspace.getScreenWithId(mPendingAddInfo.screenId);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, mPendingAddInfo.container,
                            mPendingAddInfo.screenId, layout, null);
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED),
                            EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(mPendingAddInfo, cellLayout,
                    (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else if (onCompleteRunnable != null) {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onRestart() {
        Log.v(TAG, "PROCEDURE onRestart() start");
        super.onRestart();
        Log.v(TAG, "PROCEDURE onRestart() end");
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "PROCEDURE onStop() start");

        super.onStop();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "(Launcher)onStop: this = " + this);
        }

        FirstFrameAnimatorHelper.setIsVisible(false);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStop();
        }

        Log.v(TAG, "PROCEDURE onStop() end");
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "PROCEDURE onStart() start");

        super.onStart();
        /**
         * Added by gaoquan 2017.6.1
         */
        //-------------------------------start--------------///
        LauncherApplication.setLauncher(this);
        LauncherApplication.setLauncherModel(mModel);
        //-------------------------------end--------------///

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "(Launcher)onStart: this = " + this);
        }
        FirstFrameAnimatorHelper.setIsVisible(true);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStart();
        }
        // Added by wjq in 2017-7-4 for wallpaper set start
        Point outSize = WallpaperUtils.getDefaultWallpaperSize(getResources(),
                getWindowManager());
        WallpaperManager.getInstance(this).suggestDesiredDimensions(outSize.x,outSize.y);
        // Added by wjq in 2017-7-4 for wallpaper set end



        //when theme changed, launcher cannot receive broadcast WallpaperChanged
        //so launcher onCreated get wallpaper from wallpaperManager
        sWorker.post(new Runnable() {
            @Override
            public void run() {
                com.android.gallery3d.common.BitmapUtils.savePNGAsFile(Launcher.this);
            }
        });
        Log.v(TAG, "PROCEDURE onStart() end");
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "PROCEDURE onResume() start");
        // added by jubingcheng for fix:multi-instance start on 2017/9/7
        if (LauncherApplication.needRestart) {
            super.onResume();
            Log.e(TAG, "PROCEDURE onResume() needRestart killProcess!!");
            Process.killProcess(Process.myPid());
            return;
        }
        // added by jubingcheng for fix:multi-instance end on 2017/9/7

        //add by chenchao when open or close custompage in setting start
        if (needCustomContentToLeft()) {
            openCustomContent();
        }else{
            closeCustomContent();
        }
        //add by chenchao when open or close custompage in setting end
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.preOnResume();
        }

        super.onResume();
        if (LauncherLog.DEBUG) {
            LauncherLog.e(TAG, "(Launcher)onResume: mRestoring = " + mRestoring
                    + ", mOnResumeNeedsLoad = " + mOnResumeNeedsLoad
                    + ",mPagesAreRecreated = " + ", this = " + this);
        }

        //add by rongwenzhao Shake automatically align the desktop icon begin 2017-7-3
        if(mState == State.WORKSPACE && mWorkspace.getState() == Workspace.State.OVERVIEW &&
                SettingsProvider.getBooleanCustomDefault(this,SettingsProvider.SETTINGS_UI_SHAKE_ARRANGE_ICONS,false)
                && !ShakeListener.isRegister){
            startShakeListening();
        }
        //add by rongwenzhao Shake automatically align the desktop icon end  2017-7-3

        // Restore the previous launcher state
        if (mOnResumeState == State.WORKSPACE) {
            showWorkspace(false);
        } else if (mOnResumeState == State.APPS) {
            boolean launchedFromApp = (mWaitingForResume != null);
            // Don't update the predicted apps if the user is returning to launcher in the apps
            // view after launching an app, as they may be depending on the UI to be static to
            // switch to another app, otherwise, if it was
            showAppsView(false /* animated */, false /* resetListToTop */,
                    !launchedFromApp /* updatePredictedApps */, false /* focusSearchBar */);
        } else if (mOnResumeState == State.WIDGETS) {
            showWidgetsView(false, false);
        }
        //modify by chenchao for PRODUCTION-7042 on 2017/11/17 start
        //added by liuning for PRODUCTION-1287 on 2017/10/17 start
        if(mWorkspace.isInAppManageMode() || mWorkspace.isInOverviewMode()) {
            //进入图库选择壁纸后，会直接回到桌面，此时立刻hideStatusBar，按home键会出现负一屏消失的情况
            if (mOnResumeState == State.NONE) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DisplayMetricsUtils.hideStatusBar();
                    }
                },100);
            } else {
                DisplayMetricsUtils.hideStatusBar();
            }
        }
        //added by liuning for PRODUCTION-1287 on 2017/10/17 end
        //modify by chenchao for PRODUCTION-7042 on 2017/11/17 end
        mOnResumeState = State.NONE;

        mPaused = false;
        if (mRestoring || mOnResumeNeedsLoad) {
            setWorkspaceLoading(true);

            // If we're starting binding all over again, clear any bind calls we'd postponed in
            // the past (see waitUntilResume) -- we don't need them since we're starting binding
            // from scratch again
            mBindOnResumeCallbacks.clear();

            mModel.startLoader(PagedView.INVALID_RESTORE_PAGE);
            mRestoring = false;
            mOnResumeNeedsLoad = false;
        }
        if (mBindOnResumeCallbacks.size() > 0) {
            // We might have postponed some bind calls until onResume (see waitUntilResume) --
            // execute them here
            long startTimeCallbacks = 0;
            if (DEBUG_RESUME_TIME) {
                startTimeCallbacks = System.currentTimeMillis();
            }

            for (int i = 0; i < mBindOnResumeCallbacks.size(); i++) {
                mBindOnResumeCallbacks.get(i).run();
            }
            mBindOnResumeCallbacks.clear();
            if (DEBUG_RESUME_TIME) {
                Log.d(TAG, "Time spent processing callbacks in onResume: " +
                    (System.currentTimeMillis() - startTimeCallbacks));
            }
        }
        if (mOnResumeCallbacks.size() > 0) {
            for (int i = 0; i < mOnResumeCallbacks.size(); i++) {
                mOnResumeCallbacks.get(i).run();
            }
            mOnResumeCallbacks.clear();
        }

        // Reset the pressed state of icons that were locked in the press state while activities
        // were launching
        if (mWaitingForResume != null) {
            // Resets the previous workspace icon press state
            mWaitingForResume.setStayPressed(false);
        }

        // It is possible that widgets can receive updates while launcher is not in the foreground.
        // Consequently, the widgets will be inflated in the orientation of the foreground activity
        // (framework issue). On resuming, we ensure that any widgets are inflated for the current
        // orientation.
        if (!isWorkspaceLoading()) {
            getWorkspace().reinflateWidgetsIfNecessary();
        }
        reinflateQSBIfNecessary();

        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onResume: " + (System.currentTimeMillis() - startTime));
        }

        // We want to suppress callbacks about CustomContent being shown if we have just received
        // onNewIntent while the user was present within launcher. In that case, we post a call
        // to move the user to the main screen (which will occur after onResume). We don't want to
        // have onHide (from onPause), then onShow, then onHide again, which we get if we don't
        // suppress here.
        if (mWorkspace.getCustomContentCallbacks() != null
                && !mMoveToDefaultScreenFromNewIntent) {
            // If we are resuming and the custom content is the current page, we call onShow().
            // It is also possible that onShow will instead be called slightly after first layout
            // if PagedView#setRestorePage was set to the custom content page in onCreate().
            if (mWorkspace.isOnOrMovingToCustomContent()) {
                mWorkspace.getCustomContentCallbacks().onShow(true);
            }
        }
        mMoveToDefaultScreenFromNewIntent = false;
        updateInteraction(Workspace.State.NORMAL, mWorkspace.getState());
        Log.v(TAG, "PROCEDURE onResume() mWorkspace start");
        mWorkspace.onResume(mHasFocus);
        Log.v(TAG, "PROCEDURE onResume() mWorkspace end");

        if (!isWorkspaceLoading()) {
            // Process any items that were added while Launcher was away.
            InstallShortcutReceiver.disableAndFlushInstallQueue(this);
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onResume();
        }
        if (mWallpaperPickerContainer != null) {
            mWallpaperPickerContainer.notifyDataChanged();
        }
        Log.v(TAG, "PROCEDURE onResume() end");
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "PROCEDURE onPause() start");

        mHasFocusOnPause = mHasFocus;
        // Added by wjq in 2017-7-4 for hideOrShowSystemUIClock start
        // updated by jubingcheng for fix[incorrect showOrHideSystemUIClock effect when launch apps] start on 2017/7/13
//        if (!mHasFocus)
//            showOrHideSystemUIClock(true);
        if (Utilities.FR_SYSTEMUI_CLOCK) {
            startShowOrHideSystemUIClockTimer(true, SHOW_SYSTEMUI_CLOCK_DELAY);
        }
        // updated by jubingcheng for fix[incorrect showOrHideSystemUIClock effect when launch apps] end on 2017/7/13
        // Added by wjq in 2017-7-4 for hideOrShowSystemUIClock end
        // Ensure that items added to Launcher are queued until Launcher returns
        InstallShortcutReceiver.enableInstallQueue();

        super.onPause();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "(Launcher)onPause: this = " + this);
        }

        mPaused = true;
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();
        //add linhai for pagedview is reordering spark home key start on 2017/10/10
        mWorkspace.cancelReorder();
        //add linhai for pagedview is reordering spark home key end on 2017/10/10

        // We call onHide() aggressively. The custom content callbacks should be able to
        // debounce excess onHide calls.
        if (mWorkspace.getCustomContentCallbacks() != null) {
            mWorkspace.getCustomContentCallbacks().onHide();
        }

        // deleted by jubingcheng for unnecessary function[exit overview-mode when onpause if transition effect view visible] start on 2017/7/3
//        backToWorkSpaceNormal();
        // deleted by jubingcheng for unnecessary function[exit overview-mode when onpause if transition effect view visible] end on 2017/7/3

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPause();
        }

        stopShakeListening();//add by rongwenzhao stopShake Listener 2017-7-3
        Log.v(TAG, "PROCEDURE onPause() end");
    }

    /**
     * linhai
     * 返回首页  2017/6/16
     */
    public void backToWorkSpaceNormal() {
//        if (mState == mState.TRANSITION_EFFECT) {
            if (isTransitionEffectViewVisible()) {
                exitTransitionEffect(false);
                showWorkspace(true);
            }
//        }else if (mState == mState.WALLPAPER_PICKER) {
//            //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 start
//            if (isWallpaperViewVisible()) {
//                exitWallpaperPicker();
//                showWorkspace(true);
//            }
//            //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 end
//        }
    }

    public interface CustomContentCallbacks {
        // Custom content is completely shown. {@code fromResume} indicates whether this was caused
        // by a onResume or by scrolling otherwise.
        public void onShow(boolean fromResume);

        // Custom content is completely hidden
        public void onHide();

        // Custom content scroll progress changed. From 0 (not showing) to 1 (fully showing).
        public void onScrollProgressChanged(float progress);

        // Indicates whether the user is allowed to scroll away from the custom content.
        boolean isScrollingAllowed();
    }

    public interface LauncherOverlay {

        /**
         * Touch interaction leading to overscroll has begun
         */
        public void onScrollInteractionBegin();

        /**
         * Touch interaction related to overscroll has ended
         */
        public void onScrollInteractionEnd();

        /**
         * Scroll progress, between 0 and 100, when the user scrolls beyond the leftmost
         * screen (or in the case of RTL, the rightmost screen).
         */
        public void onScrollChange(float progress, boolean rtl);

        /**
         * Called when the launcher is ready to use the overlay
         * @param callbacks A set of callbacks provided by Launcher in relation to the overlay
         */
        public void setOverlayCallbacks(LauncherOverlayCallbacks callbacks);
    }

    public interface LauncherSearchCallbacks {
        /**
         * Called when the search overlay is shown.
         */
        public void onSearchOverlayOpened();

        /**
         * Called when the search overlay is dismissed.
         */
        public void onSearchOverlayClosed();
    }

    public interface LauncherOverlayCallbacks {
        public void onScrollChanged(float progress);
    }

    class LauncherOverlayCallbacksImpl implements LauncherOverlayCallbacks {

        public void onScrollChanged(float progress) {
            if (mWorkspace != null) {
                mWorkspace.onOverlayScrollChanged(progress);
            }
        }
    }

    protected boolean hasSettings() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasSettings();
        } else {
            // On devices with a locked orientation, we will at least have the allow rotation
            // setting.
            return !getResources().getBoolean(R.bool.allow_rotation);
        }
    }

    public void addToCustomContentPage(View customContent,
            CustomContentCallbacks callbacks, String description) {
        mWorkspace.addToCustomContentPage(customContent, callbacks, description);
    }

    // The custom content needs to offset its content to account for the QSB
    public int getTopOffsetForCustomContent() {
        return mWorkspace.getPaddingTop();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onRetainNonConfigurationInstance: mSavedState = "
                    + mSavedState + ", mSavedInstanceState = " + mSavedInstanceState);
        }
        // Flag the loader to stop early before switching
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
        }
        //TODO(hyunyoungs): stop the widgets loader when there is a rotation.

        return Boolean.TRUE;
    }

    // We can't hide the IME if it was forced open.  So don't bother
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.v(TAG, "PROCEDURE onWindowFocusChanged() hasFocus:" + hasFocus);

        super.onWindowFocusChanged(hasFocus);
        // added by jubingcheng for fix:multi-instance start on 2017/9/7
        if (LauncherApplication.needRestart) {
            Log.e(TAG, "PROCEDURE onWindowFocusChanged() needRestart return");
            return;
        }
        // added by jubingcheng for fix:multi-instance end on 2017/9/7
        mHasFocus = hasFocus;

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onWindowFocusChanged(hasFocus);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v(TAG, "PROCEDURE onConfigurationChanged() newConfig:" + newConfig);

        super.onConfigurationChanged(newConfig);
    }

    private boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final int uniChar = event.getUnicodeChar();
        final boolean handled = super.onKeyDown(keyCode, event);
        final boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        if (LauncherLog.DEBUG_KEY) {
            LauncherLog.d(TAG, " onKeyDown: KeyCode = " + keyCode + ", KeyEvent = " + event
                    + ", uniChar = " + uniChar + ", handled = " + handled
                    + ", isKeyNotWhitespace = " + isKeyNotWhitespace);
        }

        if (!handled && acceptFilter() && isKeyNotWhitespace) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown(mWorkspace, mDefaultKeySsb,
                    keyCode, event);
            if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
                // something usable has been typed - start a search
                // the typed text will be retrieved and cleared by
                // showSearchDialog()
                // If there are multiple keystrokes before the search dialog takes focus,
                // onSearchRequested() will be called for every keystroke,
                // but it is idempotent, so it's fine.
                return onSearchRequested();
            }
        }

        // Eat the long press event so the keyboard doesn't come up.
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        }

        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Ignore the menu key if we are currently dragging or are on the custom content screen
            if (!isOnCustomContent() && !mDragController.isDragging()) {
                // Close any open folders
                closeFolder();

                // Stop resizing any widgets
                mWorkspace.exitWidgetResizeMode();

                // Show the overview mode if we are on the workspace
                if (mState == State.WORKSPACE && !mWorkspace.isInOverviewMode() &&
                        !mWorkspace.isSwitchingState()) {
                    mOverviewPanel.requestFocus();
                    showOverviewMode(true, true /* requestButtonFocus */);
                }
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private String getTypedText() {
        return mDefaultKeySsb.toString();
    }

    private void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }

    /**
     * Given the integer (ordinal) value of a State enum instance, convert it to a variable of type
     * State
     */
    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        final State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                state = stateValues[i];
                break;
            }
        }
        return state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "restoreState: savedState = " + savedState);
        }

        if (savedState == null) {
            return;
        }

        State state = intToState(savedState.getInt(RUNTIME_STATE, State.WORKSPACE.ordinal()));
        if (state == State.APPS) { //mod by rongwenzhao 不保留widget返回状态，因为已从全屏换位底部横屏展示，需要在overview状态。orign code : state == State.APPS || state == State.WIDGETS 2017-6-26
            mOnResumeState = state;
        }

        int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN,
                PagedView.INVALID_RESTORE_PAGE);
        if (currentScreen != PagedView.INVALID_RESTORE_PAGE) {
            mWorkspace.setRestorePage(currentScreen);
        }

        /// M: Save last restore workspace screen.
        mCurrentWorkSpaceScreen = currentScreen;

        final long pendingAddContainer = savedState.getLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, -1);
        final long pendingAddScreen = savedState.getLong(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);

        if (pendingAddContainer != ItemInfo.NO_ID && pendingAddScreen > -1) {
            mPendingAddInfo.container = pendingAddContainer;
            mPendingAddInfo.screenId = pendingAddScreen;
            mPendingAddInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
            mPendingAddInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
            mPendingAddInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
            mPendingAddInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
            mPendingAddInfo.componentName =
                    savedState.getParcelable(RUNTIME_STATE_PENDING_ADD_COMPONENT);
            AppWidgetProviderInfo info = savedState.getParcelable(
                    RUNTIME_STATE_PENDING_ADD_WIDGET_INFO);
            mPendingAddWidgetInfo = info == null ?
                    null : LauncherAppWidgetProviderInfo.fromProviderInfo(this, info);

            mPendingAddWidgetId = savedState.getInt(RUNTIME_STATE_PENDING_ADD_WIDGET_ID);
            setWaitingForResult(true);
            mRestoring = true;
        }
    }

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        final DragController dragController = mDragController;

        mLauncherView = findViewById(R.id.launcher);
        mFocusHandler = (FocusIndicatorView) findViewById(R.id.focus_indicator);
        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        mWorkspace = (Workspace) mDragLayer.findViewById(R.id.workspace);
        mWorkspace.setPageSwitchListener(this);
        mPageIndicators = mDragLayer.findViewById(R.id.page_indicator);

        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setLightNavigationBar(true);
        mWorkspaceBackgroundDrawable = getResources().getDrawable(R.drawable.workspace_bg);
        setWorkspaceBackground(WORKSPACE_BACKGROUND_GRADIENT);
        // Setup the drag layer
        mDragLayer.setShortCutDeleteListener(this);
        mDragLayer.setShortCutDeletableListener(this);
        mDragLayer.setup(this, dragController);

        // Setup the hotseat
        mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (mHotseat != null) {
            mHotseat.setOnLongClickListener(this);
        }

        // Setup the overview panel
        setupOverviewPanel();

        // Setup the workspace
        mWorkspace.setHapticFeedbackEnabled(false);
        mWorkspace.setOnLongClickListener(this);
        mWorkspace.setup(dragController);
        dragController.addDragListener(mWorkspace);

        // Get the search/delete bar
        mSearchDropTargetBar = (SearchDropTargetBar)
                mDragLayer.findViewById(R.id.search_drop_target_bar);

        // Setup Apps and Widgets
//        mAppsView = (AllAppsContainerView) findViewById(R.id.apps_view);
        //mod and add by rongwenzhao 2017-6-27 to show widgets horizontal begin
        mWidgetsView = (WidgetsContainerViewGM) findViewById(R.id.widgets_view_horizontal);
        mWidgetsRecyclerView = (WidgetsRecyclerView) findViewById(R.id.widgets_list_view);
        mWidgetHorizontalScrollView = (WidgetHorizontalScrollView) findViewById(R.id.widgets_scroll_container);
        //mod and add by rongwenzhao 2017-6-27 to show widgets horizontal end
//        if (mLauncherCallbacks != null && mLauncherCallbacks.getAllAppsSearchBarController() != null) {
//            mAppsView.setSearchBarController(mLauncherCallbacks.getAllAppsSearchBarController());
//        } else {
//            mAppsView.setSearchBarController(new DefaultAppSearchController());
//        }

        /**
         * Added by gaoquan 2017.7.12
         */
        //-------------------------------start--------------///
        mFolderBlur = (ImageView) findViewById(R.id.folder_blur);
        //-------------------------------end--------------///

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dragController.setDragScoller(mWorkspace);
        dragController.setScrollView(mDragLayer);
        dragController.setMoveTarget(mWorkspace);
        dragController.addDropTarget(mWorkspace);
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.setup(this, dragController);
            mSearchDropTargetBar.setQsbSearchBar(getOrCreateQsbBar());
        }

        //added by liuning for multi apps move on 2017/7/18 start
        mMoveViewsRestContainer = (MoveViewsRestContainer) mDragLayer.findViewById(R.id.move_views_rest_container);
        mMoveViewsRestContainer.setAdapterItemAddListener(this);
        mMoveViewsRestContainer.setVisibility(View.INVISIBLE);
        dragController.addDragListener(mMoveViewsRestContainer);
        dragController.addDropTarget(mMoveViewsRestContainer);
        //added by liuning for multi apps move on 2017/7/18 end

        if (TestingUtils.MEMORY_DUMP_ENABLED) {
            TestingUtils.addWeightWatcher(this);
        }
    }
	
	//added by liuning for multi apps move on 2017/7/18 start
    public void enterAppManageMode() {
        mWorkspace.setIsAppManageMode(true);
        showMoveViewsRestContainer();
    }

    public void exitAppManageMode() {
        mWorkspace.setIsAppManageMode(false);
        hideMoveViewsRestContainer();
        mWorkspace.removeExtraEmptyScreen(false, true);
        mWorkspace.showPageScaleAinimation(true, true);
        Folder currentFolder = getWorkspace().getOpenFolder();
        if (currentFolder != null) {
            currentFolder.setFolderNameEditAble();
        }
    }

    public void showMoveViewsRestContainer(){
        mMoveViewsRestContainer.show();
    }

    public MoveViewsRestContainer getMoveViewsRestContainer(){
        return mMoveViewsRestContainer;
    }

    public void hideMoveViewsRestContainer(){
        mMoveViewsRestContainer.hide();
        mMoveViewsRestContainer.placeAllItemBack();
    }
    //added by liuning for multi apps move on 2017/7/18 end

    private void setupOverviewPanel() {
        mOverviewPanel = (ViewGroup) findViewById(R.id.overview_panel);
		//linhai add 增加底边切页效果选择栏 start 2017/6/16
        mOverViewButtoContainer = mOverviewPanel.findViewById(R.id.overview_button_container);
        mTransitionEffectsContainer = mOverviewPanel.findViewById(R.id.transition_effect_view);
        // Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 start
        mWallpaperPickerContainer = (WallpaperContainerViewGM) mOverviewPanel.findViewById(R.id.wallpaper_picker_view);
        // Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 end
//        mWallpaperPickerContainer.setGalleryLoadListener(new WallpaperContainerViewGM.GalleryLoadListener() {
//            @Override
//            public void onClickListener() {
//                mState = State.GALLERY_LOADING;
//            }
//        });
        // Long-clicking buttons in the overview panel does the same thing as clicking them.
        OnLongClickListener performClickOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return v.performClick();
            }
        };

        // Bind wallpaper button actions
        View wallpaperButton = findViewById(R.id.wallpaper_button);
        wallpaperButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mWorkspace.isSwitchingState()) {
                    onClickWallpaperPicker(view);
                }
            }
        });
        wallpaperButton.setOnLongClickListener(performClickOnLongClick);
        wallpaperButton.setOnTouchListener(getHapticFeedbackTouchListener());

        // Bind widget button actions
        mWidgetsButton = findViewById(R.id.widget_button);
        mWidgetsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mWorkspace.isSwitchingState()) {
                    onClickAddWidgetButton(view);
                }
            }
        });
        mWidgetsButton.setOnLongClickListener(performClickOnLongClick);
        mWidgetsButton.setOnTouchListener(getHapticFeedbackTouchListener());

        // Bind settings actions
        View settingsButton = findViewById(R.id.settings_button);
        boolean hasSettings = hasSettings();
        if (hasSettings) {
            settingsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mWorkspace.isSwitchingState()) {
                        onClickSettingsButton(view);
                    }
                }
            });
            settingsButton.setOnLongClickListener(performClickOnLongClick);
            settingsButton.setOnTouchListener(getHapticFeedbackTouchListener());
        } else {
            settingsButton.setVisibility(View.GONE);
        }

        View effectsButton = findViewById(R.id.effects_button);
        effectsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickTransitionEffectButton(v);
            }
        });
        View themeButton = findViewById(R.id.theme_button);
        themeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickThemeButton();
            }
        });
        themeButton.setOnTouchListener(getHapticFeedbackTouchListener());
        effectsButton.setOnTouchListener(getHapticFeedbackTouchListener());
        //linhai end 2017/6/16
        mOverviewPanel.setAlpha(0f);
    }


    public  void  onClickThemeButton()
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.gome.theme.chooser2","com.gome.theme.chooser2.ThemeChooserActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

    }


    /**
     * 编辑界面是否正在编辑
     * add by 3h in 2017-08-10 for page settings start
     * @return
     */
    public boolean isEditing() {
        return mTransitionEffectsContainer.getVisibility() == View.VISIBLE
                || mWallpaperPickerContainer.getVisibility() == View.VISIBLE
                || mWidgetsView.getVisibility() == View.VISIBLE;
    }

    /*
     *linhai 增加底边切页效果选择栏 start 2017/6/16
    */
    public void onClickTransitionEffectButton(View v) {
        if (mState == State.WORKSPACE && mWorkspace.getState() == Workspace.State.OVERVIEW) {
            mState = State.TRANSITION_EFFECT;

//            mOverViewButtoContainer.setAlpha(0);
//            mTransitionEffectsContainer.setAlpha(1);
            mOverViewButtoContainer.setVisibility(View.GONE);
            mTransitionEffectsContainer.setVisibility(View.VISIBLE);
            //add by huanghaihao in 2017-7-27 for page settings start
            mWorkspace.setSettingsVisibility(View.GONE);
            //add by huanghaihao in 2017-7-27 for page settings end
//            LauncherAnimUtils.fadeAlphaInOrOut(mOverViewButtoContainer, false, 300);
//            LauncherAnimUtils.fadeAlphaInOrOut(mTransitionEffectsContainer, true, 300, 300);
        }
    }

    //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 start
    private void showWallpaperPickerView(float offset) {
        if (mState == State.WORKSPACE && mWorkspace.getState() == Workspace.State.OVERVIEW) {
            mState = State.WALLPAPER_PICKER;
            mOverViewButtoContainer.setVisibility(View.GONE);
            mWallpaperPickerContainer.setVisibility(View.VISIBLE);
        }
    }
    //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 end

    /**
     * Sets the all apps button. This method is called from {@link Hotseat}.
     */
    public void setAllAppsButton(View allAppsButton) {
        mAllAppsButton = allAppsButton;
    }

    public View getAllAppsButton() {
        return mAllAppsButton;
    }

    public View getWidgetsButton() {
        return mWidgetsButton;
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     */
    public View createShortcut(ShortcutInfo info) {
        return createShortcut((ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param parent The group the shortcut belongs to.
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from layoutResId.
     */
    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) mInflater.inflate(R.layout.app_icon,
                parent, false);
        favorite.applyFromShortcutInfo(info, mIconCache);
        favorite.setOnClickListener(this);
        //delete by linhai remove focusChange start 29/8/2017
         //        favorite.setOnFocusChangeListener(mFocusHandler);
        //delete by linhai remove focusChange end 29/8/2017
        return favorite;
    }

    /**
     * Add a shortcut to the workspace.
     *
     * @param data The intent describing the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, long screenId, int cellX,
            int cellY) {
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screenId);

        ShortcutInfo info = InstallShortcutReceiver.fromShortcutIntent(this, data);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "completeAddShortcut: info = " + info + ", data = " + data
                    + ", container = " + container + ", screenId = " + screenId + ", cellX = "
                    + cellX + ", cellY = " + cellY + ", layout = " + layout);
        }

        if (info == null || mPendingAddInfo.componentName == null) {
            return;
        }
        if (!PackageManagerHelper.hasPermissionForActivity(
                this, info.intent, mPendingAddInfo.componentName.getPackageName())) {
            // The app is trying to add a shortcut without sufficient permissions
            Log.e(TAG, "Ignoring malicious intent " + info.intent.toUri(0));
            return;
        }
        //add by huanghaihao in 2017-8-2  for removing the same icon start
        LauncherAppState app = LauncherAppState.getInstance();
        if (app.isDisableAllApps() && app.getModel().shortcutExists(this, info)) {
            Toast.makeText(this, String.format(getString(R.string.shortcut_exists), info.title),
                    Toast.LENGTH_SHORT).show();
            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "completeAddShortcut " + info);
            }
            return;
        }
        //add by huanghaihao in 2017-8-2  for removing the same icon end
        final View view = createShortcut(info);

        boolean foundCellSpan = false;
        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;

            // If appropriate, either create a folder or add to an existing folder
            if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                    true, null,null)) {
                return;
            }
            DropTarget.DragObject dragObject = new DropTarget.DragObject();
            dragObject.dragInfo = info;
            if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                    true)) {
                return;
            }
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY);
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        LauncherModel.addItemToDatabase(this, info, container, screenId, cellXY[0], cellXY[1]);

        if (!mRestoring) {
            mWorkspace.addInScreen(view, container, screenId, cellXY[0], cellXY[1], 1, 1,
                    isWorkspaceLocked());
        }
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     */
    @Thunk void completeAddAppWidget(int appWidgetId, long container, long screenId,
            AppWidgetHostView hostView, LauncherAppWidgetProviderInfo appWidgetInfo) {

        ItemInfo info = mPendingAddInfo;
        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(appWidgetId);
        }
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "completeAddAppWidget: appWidgetId = " + appWidgetId
                    + ", container = " + container + ", screenId = " + screenId);
        }

        if (appWidgetInfo.isCustomWidget) {
            appWidgetId = LauncherAppWidgetInfo.CUSTOM_WIDGET_ID;
        }

        LauncherAppWidgetInfo launcherInfo;
        launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = info.spanX;
        launcherInfo.spanY = info.spanY;
        launcherInfo.minSpanX = info.minSpanX;
        launcherInfo.minSpanY = info.minSpanY;
        launcherInfo.user = mAppWidgetManager.getUser(appWidgetInfo);

        LauncherModel.addItemToDatabase(this, launcherInfo,
                container, screenId, info.cellX, info.cellY);

        if (!mRestoring) {
            if (hostView == null) {
                // Perform actual inflation because we're live
                launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId,
                        appWidgetInfo);
            } else {
                // The AppWidgetHostView has already been inflated and instantiated
                launcherInfo.hostView = hostView;
            }
            launcherInfo.hostView.setVisibility(View.VISIBLE);
            addAppWidgetToWorkspace(launcherInfo, appWidgetInfo, isWorkspaceLocked());
        }
        resetAddInfo();
    }

    private void addAppWidgetToWorkspace(LauncherAppWidgetInfo item,
            LauncherAppWidgetProviderInfo appWidgetInfo, boolean insert) {
        if (Utilities.FR_SYSTEMUI_CLOCK) {
            // Added by wjq in 2017-7-4 for hideOrShowSystemUIClock start
            if (item.screenId == mWorkspace.getScreenIdForPageIndex(mWorkspace.getCurrentPage())) {
                if (item.providerName.toString().toLowerCase().contains(Utilities.CLOCK_WIDGET_NAME)) {
                    // updated by jubingcheng for fix[incorrect showOrHideSystemUIClock effect when launch apps] start on 2017/7/13
//                showOrHideSystemUIClock(false);
                    startShowOrHideSystemUIClockTimer(false, 0);
                    // updated by jubingcheng for fix[incorrect showOrHideSystemUIClock effect when launch apps] end on 2017/7/13
                }

            }
        }
        // Added by wjq in 2017-7-4 for hideOrShowSystemUIClock end
        item.hostView.setTag(item);
        item.onBindAppWidget(this);

        item.hostView.setFocusable(true);
        //delete by linhai remove focusChange start 29/8/2017
//        item.hostView.setOnFocusChangeListener(mFocusHandler);
        //delete by linhai remove focusChange end 29/8/2017

        /// M:ALPS02393519
        if (mWorkspace != null) {
        mWorkspace.addInScreen(item.hostView, item.container, item.screenId,
                item.cellX, item.cellY, item.spanX, item.spanY, insert);
        } else {
           LauncherLog.d(TAG, "error , mWorkspace is null");
        }

        if (!item.isCustomWidget()) {
            addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                LauncherLog.d(TAG, "ACTION_SCREEN_OFF: mPendingAddInfo = " + mPendingAddInfo
                         + ", this = " + this);
                mUserPresent = false;
                mDragLayer.clearAllResizeFrames();
                updateAutoAdvanceState();
                // deleted by jubingcheng for unnecessary function[exit overview-mode when screen-off] start on 2017/7/3
//                // Reset AllApps to its initial state only if we are not in the middle of
//                // processing a multi-step drop
//                if (mAppsView != null && mWidgetsView != null &&
//                        mPendingAddInfo.container == ItemInfo.NO_ID) {
//                    if (!showWorkspace(false)) {
//                        // If we are already on the workspace, then manually reset all apps
//                        mAppsView.reset();
//                    }
//                }
                // deleted by jubingcheng for unnecessary function[exit overview-mode when screen-off] end on 2017/7/3
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUserPresent = true;
                updateAutoAdvanceState();
            } else if (ENABLE_DEBUG_INTENTS && DebugIntents.DELETE_DATABASE.equals(action)) {
                mModel.resetLoadedState(false, true);
                mModel.startLoader(PagedView.INVALID_RESTORE_PAGE,
                        LauncherModel.LOADER_FLAG_CLEAR_WORKSPACE);
            } else if (ENABLE_DEBUG_INTENTS && DebugIntents.MIGRATE_DATABASE.equals(action)) {
                mModel.resetLoadedState(false, true);
                mModel.startLoader(PagedView.INVALID_RESTORE_PAGE,
                        LauncherModel.LOADER_FLAG_CLEAR_WORKSPACE
                                | LauncherModel.LOADER_FLAG_MIGRATE_SHORTCUTS);
            }

            IDownLoadCallback iDownLoadCallback = null;
            if(action.equals("installProgress1")) {
                String msg = intent.getExtras().getString("packageName");
                Log.d(TAG, "msg + installProgress1 +" + msg);
                iDownLoadCallback = findIDownLoadCallbackByPkgName(msg);
                if (iDownLoadCallback != null) {
                    iDownLoadCallback.onInstalling(msg,1f);
                }
            }
            if(action.equals("installProgress2")) {
                String msg = intent.getExtras().getString("packageName");
                Log.d(TAG, "msg + installProgress2 +" + msg);
                iDownLoadCallback = findIDownLoadCallbackByPkgName(msg);
                if (iDownLoadCallback != null) {
                    iDownLoadCallback.onInstalling(msg,2f);
                }
            }
            if(action.equals("installProgress3")) {
                String msg = intent.getExtras().getString("packageName");
                Log.d(TAG, "msg + installProgress3 +" + msg);
                iDownLoadCallback = findIDownLoadCallbackByPkgName(msg);
                if (iDownLoadCallback != null) {
                    iDownLoadCallback.onInstalling(msg,3f);
                }
            }
            if(action.equals("installProgress4")) {
                String msg = intent.getExtras().getString("packageName");
                Log.d(TAG, "msg + installProgress4 +" + msg);
                iDownLoadCallback = findIDownLoadCallbackByPkgName(msg);
                if (iDownLoadCallback != null) {
                    iDownLoadCallback.onInstalling(msg,4f);
                }
            }

            if(action.equals("installProgress5")) {
                String msg = intent.getExtras().getString("packageName");
                Log.e(TAG, "msg + installProgress5 +" + msg);
                DownloadAppInfo downloadAppInfo = findDownloadAppInfoByPkgName(msg);
                // Modify by louhan in 2017-12-18 for fix bug GMOS2X1-3394
                final UserHandleCompat user = UserHandleCompat.myUserHandle();
                final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
                final List<LauncherActivityInfoCompat> matches = launcherApps.getActivityList(msg,
                        user);
                Log.e(TAG, "matches.size() + " + matches.size());
                if (matches.size() == 0) {
                    ShortcutInfo shortcutInfo = downloadAppInfo.getShortcutInfo();
                    removeItem((View) downloadAppInfo.getDownLoadCallback(), downloadAppInfo.getShortcutInfo(), true);
                    mDownLoadAppMap.remove(shortcutInfo);
                } else {
                    if (downloadAppInfo != null && downloadAppInfo.getDownLoadCallback() != null) {
                        downloadAppInfo.getDownLoadCallback().onInstalled(msg);
                    }
                    // Modify by louhan in 2017-10-12 for fix bug GMOS-9449
                    updateDownloadItemByPkgName(msg, BitmapInfo.INSTALLED, -1, null, null);
                    if (downloadAppInfo != null && downloadAppInfo.getShortcutInfo() != null) {
                        ShortcutInfo shortcutInfo = downloadAppInfo.getShortcutInfo();
                        shortcutInfo.isNewApp = true;
                        shortcutInfo.appDownLoadStatus = BitmapInfo.INSTALLED;
                        shortcutInfo.flags = AppInfo.DOWNLOADED_FLAG;
                        getPackageCircle().updateCircleChanged(msg);
                        Log.d(TAG, "remove before = " + mDownLoadAppMap.size());
//                        mDownLoadAppMap.remove(shortcutInfo);
                        Log.d(TAG, "remove end = " + mDownLoadAppMap.size());
                    }
                }
            }
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onAttachedToWindow.");
        }

        // Listen for broadcasts related to user-presence
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // For handling managed profiles
        if (ENABLE_DEBUG_INTENTS) {
            filter.addAction(DebugIntents.DELETE_DATABASE);
            filter.addAction(DebugIntents.MIGRATE_DATABASE);
        }
        filter.addAction("installProgress1");
        filter.addAction("installProgress2");
        filter.addAction("installProgress3");
        filter.addAction("installProgress4");
        filter.addAction("installProgress5");
        registerReceiver(mReceiver, filter);
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        mAttached = true;
        mVisible = true;

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onAttachedToWindow();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDetachedFromWindow.");
        }

        mVisible = false;

        if (mAttached) {
            unregisterReceiver(mReceiver);
            mAttached = false;
        }
        updateAutoAdvanceState();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDetachedFromWindow();
        }
    }

    public void onWindowVisibilityChanged(int visibility) {
        mVisible = visibility == View.VISIBLE;
        updateAutoAdvanceState();
        // The following code used to be in onResume, but it turns out onResume is called when
        // you're in All Apps and click home to go to the workspace. onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (mVisible) {
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we force it to build
                // layers on all the workspace pages, so that transitioning to Launcher from other
                // apps is nice and speedy.
                observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                    private boolean mStarted = false;
                    public void onDraw() {
                        if (mStarted) return;
                        mStarted = true;
                        // We delay the layer building a bit in order to give
                        // other message processing a time to run.  In particular
                        // this avoids a delay in hiding the IME if it was
                        // currently shown, because doing that may involve
                        // some communication back with the app.
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                        final ViewTreeObserver.OnDrawListener listener = this;
                        mWorkspace.post(new Runnable() {
                            public void run() {
                                if (mWorkspace != null &&
                                        mWorkspace.getViewTreeObserver() != null) {
                                    mWorkspace.getViewTreeObserver().
                                            removeOnDrawListener(listener);
                                }
                            }
                        });
                        return;
                    }
                });
            }
            clearTypedText();
        }
    }

    @Thunk void sendAdvanceMessage(long delay) {
        mHandler.removeMessages(ADVANCE_MSG);
        Message msg = mHandler.obtainMessage(ADVANCE_MSG);
        mHandler.sendMessageDelayed(msg, delay);
        mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    @Thunk void updateAutoAdvanceState() {
        boolean autoAdvanceRunning = mVisible && mUserPresent && !mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != mAutoAdvanceRunning) {
            mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                long delay = mAutoAdvanceTimeLeft == -1 ? mAdvanceInterval : mAutoAdvanceTimeLeft;
                sendAdvanceMessage(delay);
            } else {
                if (!mWidgetsToAdvance.isEmpty()) {
                    mAutoAdvanceTimeLeft = Math.max(0, mAdvanceInterval -
                            (System.currentTimeMillis() - mAutoAdvanceSentTime));
                }
                mHandler.removeMessages(ADVANCE_MSG);
                mHandler.removeMessages(0); // Remove messages sent using postDelayed()
            }
        }
    }

    @Thunk final Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == ADVANCE_MSG) {
                int i = 0;
                for (View key: mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    final int delay = mAdvanceStagger * i;
                    if (v instanceof Advanceable) {
                        mHandler.postDelayed(new Runnable() {
                           public void run() {
                               ((Advanceable) v).advance();
                           }
                       }, delay);
                    }
                    i++;
                }
                sendAdvanceMessage(mAdvanceInterval);
            }
            return true;
        }
    });

    private void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addWidgetToAutoAdvanceIfNeeded hostView = " + hostView
                + ", appWidgetInfo = " + appWidgetInfo);
        }

        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1) return;
        View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        if (v instanceof Advanceable) {
            mWidgetsToAdvance.put(hostView, appWidgetInfo);
            ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
            updateAutoAdvanceState();
        }
    }

    private void removeWidgetToAutoAdvance(View hostView) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "removeWidgetToAutoAdvance hostView = " + hostView);
        }
        if (mWidgetsToAdvance.containsKey(hostView)) {
            mWidgetsToAdvance.remove(hostView);
            updateAutoAdvanceState();
        }
    }

    public void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = (isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space);
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    /**
     * added by liuning for show no space when add widget on 2017/7/29
     */
    public void showWidgetOutOfSpaceMessage() {
        //Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();//delete by rongwenzhao 2017-7-13
//        Utilities.getCustomWidget(this, R.string.out_of_space).show();//add by rongwenzhao mod show style of toast 2017-7-13
         //add by linhai for show no space show view on 2017/10/17
		 if (mSearchDropTargetBar != null) mSearchDropTargetBar.toastShow();
		  //end linhai for show no space show view on 2017/10/17
    }


    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    public AllAppsContainerView getAppsView() {
        return null; /*mAppsView;*/
    }

    public WidgetsContainerViewGM getWidgetsView() {//mod by rongwenzhao widget list show horizontal 2017-6-27
        return mWidgetsView;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public Hotseat getHotseat() {
        return mHotseat;
    }

    public ViewGroup getOverviewPanel() {
        return mOverviewPanel;
    }

    public SearchDropTargetBar getSearchDropTargetBar() {
        return mSearchDropTargetBar;
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    protected SharedPreferences getSharedPrefs() {
        return mSharedPrefs;
    }

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    public void closeSystemDialogs() {
        getWindow().closeAllPanels();

        // Whatever we were doing is hereby canceled.
        setWaitingForResult(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }
        super.onNewIntent(intent);
        // added by jubingcheng for fix:multi-instance start on 2017/9/7
        if (LauncherApplication.needRestart) {
            Log.e(TAG, "PROCEDURE onNewIntent() needRestart return");
            return;
        }
        // added by jubingcheng for fix:multi-instance end on 2017/9/7
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onNewIntent: intent = " + intent);
        }
        // Close the menu
        Folder openFolder = mWorkspace.getOpenFolder();
        // updated by jubingcheng for fix: enter app and press homekey immediately, if launcher is onPause, it will go to homepage[GMOS-6893] and if app is landscape, it will stuck screen[GMOS-8276] on 2017/9/22 start
//        boolean alreadyOnHome = mHasFocus && ((intent.getFlags() &
//                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
//                != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        boolean alreadyOnHome = mHasFocusOnPause && mHasFocus && ((intent.getFlags() &
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        // updated by jubingcheng for fix: enter app and press homekey immediately, if launcher is onPause, it will go to homepage[GMOS-6893] and if app is landscape, it will stuck screen[GMOS-8276] on 2017/9/22 end
        Log.v(TAG, "PROCEDURE onNewIntent() alreadyOnHome:" + alreadyOnHome + " intent:" + intent);
        boolean isActionMain = Intent.ACTION_MAIN.equals(intent.getAction());
        //added by liuning for back to homepage only when the workspace is in the normal mode on 2017/8/18 start
        boolean isWorkspaceNormalMode = true;
        if (mState == State.WORKSPACE && (mWorkspace.isInOverviewMode() || mWorkspace.isInAppManageMode())) {
            isWorkspaceNormalMode = false;
        }
        //added by liuning for back to homepage only when the workspace is in the normal mode on 2017/8/18 end

        if (isActionMain) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();

            if (mWorkspace == null) {
                // Can be cases where mWorkspace is null, this prevents a NPE
                return;
            }
            // In all these cases, only animate if we're already on home
            mWorkspace.exitWidgetResizeMode();

            closeFolder(alreadyOnHome);
            DisplayMetricsUtils.showStatusBar();
            exitSpringLoadedDragMode();

            // If we are already on home, then just animate back to the workspace,
            // otherwise, just wait until onResume to set the state back to Workspace
            if (alreadyOnHome) {
                showWorkspace(true);
            } else {
                mOnResumeState = State.WORKSPACE;
            }

            //added by liuning for exit appManageMode when click the home key on 2017/8/18 start
            if (mState == State.WORKSPACE && mWorkspace.isInAppManageMode()) {
                exitAppManageMode();
                /**
                 * Added by gaoquan 2018.03.15
                 * fix 	OS2X-13872【Launcher】带有角标的文件夹拖动至移动区域后，有的显示角标，有的不显示
                 */
                //-------------------------------start--------------///
                bindWorkspaceUnreadInfo();
                //-------------------------------end--------------///
            }
            //added by liuning for exit appManageMode when click the home key on 2017/8/18 end

            final View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

            // Reset the apps view
//            if (!alreadyOnHome && mAppsView != null) {
//                mAppsView.scrollToTop();
//            }

            // Reset the widgets view
            if (!alreadyOnHome && mWidgetsView != null) {
                mWidgetsView.scrollToTop();
            }

            if (mLauncherCallbacks != null) {
                mLauncherCallbacks.onHomeIntent();
            }
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onNewIntent(intent);
        }

        // Defer moving to the default screen until after we callback to the LauncherCallbacks
        // as slow logic in the callbacks eat into the time the scroller expects for the snapToPage
        // animation.
        if (isActionMain) {
            boolean moveToDefaultScreen = mLauncherCallbacks != null ?
                    mLauncherCallbacks.shouldMoveToDefaultScreenOnHomeIntent() : true;
            if (alreadyOnHome && mState == State.WORKSPACE && isWorkspaceNormalMode && !mWorkspace.isTouchActive() &&
                    openFolder == null && moveToDefaultScreen && !mWorkspace.isSwitchingState()) {//modified by liuning for back to homepage only when the workspace is in the normal mode

                // We use this flag to suppress noisy callbacks above custom content state
                // from onResume.
                mMoveToDefaultScreenFromNewIntent = true;
                mWorkspace.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mWorkspace != null) {
                            mWorkspace.moveToDefaultScreen(true);
                        }
                    }
                });
            }
        }

        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onNewIntent: " + (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        Log.v(TAG, "PROCEDURE onRestoreInstanceState() state:" + state);
        super.onRestoreInstanceState(state);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onRestoreInstanceState: state = " + state
                    + ", mSavedInstanceState = " + mSavedInstanceState);
        }
        for (int page: mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "PROCEDURE onSaveInstanceState() outState:" + outState);

        // Catches the case where our activity is created and immediately destroyed and our views
        // are not yet fully bound. In this case, we can't trust the state of our activity and
        // instead save our previous state (which hasn't yet been consumed / applied, a fact we
        // know as it's not null)
        if (isWorkspaceLoading() && mSavedState != null) {
            outState.putAll(mSavedState);
            return;
        }

        if (mWorkspace.getChildCount() > 0) {
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN,
                    mWorkspace.getCurrentPageOffsetFromCustomContent());
        } else { /// M: If workspcae no initialized, use saved last restore workspace screen.
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mCurrentWorkSpaceScreen);
        }
        super.onSaveInstanceState(outState);

        outState.putInt(RUNTIME_STATE, mState.ordinal());
        // We close any open folder since it will not be re-opened, and we need to make sure
        // this state is reflected.
        closeFolder(false);

        if (mPendingAddInfo.container != ItemInfo.NO_ID && mPendingAddInfo.screenId > -1 &&
                mWaitingForResult) {
            outState.putLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, mPendingAddInfo.container);
            outState.putLong(RUNTIME_STATE_PENDING_ADD_SCREEN, mPendingAddInfo.screenId);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, mPendingAddInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, mPendingAddInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, mPendingAddInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, mPendingAddInfo.spanY);
            outState.putParcelable(RUNTIME_STATE_PENDING_ADD_COMPONENT,
                    mPendingAddInfo.componentName);
            outState.putParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO, mPendingAddWidgetInfo);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_WIDGET_ID, mPendingAddWidgetId);
        }

        // Save the current widgets tray?
        // TODO(hyunyoungs)
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "PROCEDURE onDestroy() start");

        super.onDestroy();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "(Launcher)onDestroy: this = " + this);
        }

        // Remove all pending runnables
        mHandler.removeMessages(ADVANCE_MSG);
        mHandler.removeMessages(0);
        mWorkspace.removeCallbacks(mBuildLayersRunnable);

        // Stop callbacks from LauncherModel
        LauncherAppState app = (LauncherAppState.getInstance());

        // It's possible to receive onDestroy after a new Launcher activity has
        // been created. In this case, don't interfere with the new Launcher.
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
            app.setLauncher(null);
        }

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        mAppWidgetHost = null;

        mWidgetsToAdvance.clear();
        if (needCustomContentToLeft()){
            closeCustomContent();
        }
        TextKeyListener.getInstance().release();

        unregisterReceiver(mCloseSystemDialogsReceiver);

        mDragLayer.clearAllResizeFrames();
        ((ViewGroup) mWorkspace.getParent()).removeAllViews();
        mWorkspace.removeAllWorkspaceScreens();
        mWorkspace = null;
        mDragController = null;
        //add for dynamic download icon by louhan & weijiaqi 20170804
        unbindAppService();
        mDownLoadAppMap.clear();
        mDownLoadAppMap = null;
        FolderIcon.removeBubbleTextViewCreateListener();


        /**
         * Added by gaoquan 2017.6.1
         */
        //-------------------------------start--------------///
        /**M: added for unread feature, load and bind unread info.@{**/
        if (getResources().getBoolean(R.bool.config_unreadSupport)) {
            if (mUnreadLoader != null) {
                mUnreadLoader.unRegisterContentObservers();
                mUnreadLoader.initialize(null);
            }
        }
        /**@}**/
        //-------------------------------end--------------///
        /**
         * Added by gaoquan 2017.7.20
         */
        //-------------------------------start--------------///
        if(mPackageCircle != null) {
            mPackageCircle.initialize(null);
        }
        //-------------------------------end--------------///
        if(Build.VERSION.SDK_INT >= 26) {
            unregisterReceiver(mPackageReceiver);
            mPackageReceiver = null;
        }

        PackageInstallerCompat.getInstance(this).onStop();
        stopShakeListening();//add by rongwenzhao stopShake Listener 2017-7-3
        LauncherAnimUtils.onDestroyActivity();
        if (mIconCache != null)
        {
            mIconCache.clearIconCache();
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDestroy();
        }
        mInstance = null;
        Log.v(TAG, "PROCEDURE onDestroy() end");
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        onStartForResult(requestCode);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startIntentSenderForResult (IntentSender intent, int requestCode,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
        onStartForResult(requestCode);
        try {
            super.startIntentSenderForResult(intent, requestCode,
                fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    private void onStartForResult(int requestCode) {
        if (requestCode >= 0) {
            setWaitingForResult(true);
        }
    }

    /**
     * Indicates that we want global search for this activity by setting the globalSearch
     * argument for {@link #startSearch} to true.
     */
    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "startSearch.");
        }

        if (initialQuery == null) {
            // Use any text typed in the launcher as the initial query
            initialQuery = getTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }
        Rect sourceBounds = new Rect();
        if (mSearchDropTargetBar != null) {
            sourceBounds = mSearchDropTargetBar.getSearchBarBounds();
        }

        boolean clearTextImmediately = startSearch(initialQuery, selectInitialQuery,
                appSearchData, sourceBounds);
        if (clearTextImmediately) {
            clearTypedText();
        }

        // We need to show the workspace after starting the search
        showWorkspace(true);
    }

    /**
     * Start a text search.
     *
     * @return {@code true} if the search will start immediately, so any further keypresses
     * will be handled directly by the search UI. {@code false} if {@link Launcher} should continue
     * to buffer keypresses.
     */
    public boolean startSearch(String initialQuery,
            boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        if (mLauncherCallbacks != null && mLauncherCallbacks.providesSearch()) {
            return mLauncherCallbacks.startSearch(initialQuery, selectInitialQuery, appSearchData,
                    sourceBounds);
        }

        startGlobalSearch(initialQuery, selectInitialQuery,
                appSearchData, sourceBounds);
        return false;
    }

    /**
     * Starts the global search activity. This code is a copied from SearchManager
     */
    private void startGlobalSearch(String initialQuery,
            boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        final SearchManager searchManager =
            (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();
        if (globalSearchActivity == null) {
            Log.w(TAG, "No global search activity found.");
            return;
        }
        Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(globalSearchActivity);
        // Make sure that we have a Bundle to put source in
        if (appSearchData == null) {
            appSearchData = new Bundle();
        } else {
            appSearchData = new Bundle(appSearchData);
        }
        // Set source to package name of app that starts global search if not set already.
        if (!appSearchData.containsKey("source")) {
            appSearchData.putString("source", getPackageName());
        }
        intent.putExtra(SearchManager.APP_DATA, appSearchData);
        if (!TextUtils.isEmpty(initialQuery)) {
            intent.putExtra(SearchManager.QUERY, initialQuery);
        }
        if (selectInitialQuery) {
            intent.putExtra(SearchManager.EXTRA_SELECT_QUERY, selectInitialQuery);
        }
        intent.setSourceBounds(sourceBounds);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Global search activity not found: " + globalSearchActivity);
        }
    }

    public boolean isOnCustomContent() {
        return mWorkspace.isOnOrMovingToCustomContent();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.onPrepareOptionsMenu(menu);
        }
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        // Use a custom animation for launching search
        return true;
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mWaitingForResult;
    }

    public boolean isWorkspaceLoading() {
        return mWorkspaceLoading;
    }

    private void setWorkspaceLoading(boolean value) {
        boolean isLocked = isWorkspaceLocked();
        mWorkspaceLoading = value;
        if (isLocked != isWorkspaceLocked()) {
            onWorkspaceLockedChanged();
        }
    }

    private void setWaitingForResult(boolean value) {
        boolean isLocked = isWorkspaceLocked();
        mWaitingForResult = value;
        if (isLocked != isWorkspaceLocked()) {
            onWorkspaceLockedChanged();
        }
    }

    protected void onWorkspaceLockedChanged() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onWorkspaceLockedChanged();
        }
    }

    private void resetAddInfo() {
        mPendingAddInfo.container = ItemInfo.NO_ID;
        mPendingAddInfo.screenId = -1;
        mPendingAddInfo.cellX = mPendingAddInfo.cellY = -1;
        mPendingAddInfo.spanX = mPendingAddInfo.spanY = -1;
        mPendingAddInfo.minSpanX = mPendingAddInfo.minSpanY = 1;
        mPendingAddInfo.dropPos = null;
        mPendingAddInfo.componentName = null;
    }

    void addAppWidgetFromDropImpl(final int appWidgetId, final ItemInfo info, final
            AppWidgetHostView boundWidget, final LauncherAppWidgetProviderInfo appWidgetInfo) {
        if (LOGD) {
            Log.d(TAG, "Adding widget from drop");
        }
        addAppWidgetImpl(appWidgetId, info, boundWidget, appWidgetInfo, 0);
    }

    void addAppWidgetImpl(final int appWidgetId, final ItemInfo info,
            final AppWidgetHostView boundWidget, final LauncherAppWidgetProviderInfo appWidgetInfo,
            int delay) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addAppWidgetImpl: appWidgetId = " + appWidgetId
                    + ", info = " + info + ", boundWidget = " + boundWidget
                    + ", appWidgetInfo = " + appWidgetInfo + ", delay = " + delay);
        }
        if (appWidgetInfo.configure != null) {
            mPendingAddWidgetInfo = appWidgetInfo;
            mPendingAddWidgetId = appWidgetId;

            /// M.ALPS01808563. use the setWaitingForResult API
            setWaitingForResult(true);

            // Launch over to configure widget, if needed
            mAppWidgetManager.startConfigActivity(appWidgetInfo, appWidgetId, this,
                    mAppWidgetHost, REQUEST_CREATE_APPWIDGET);

        } else {
            // Otherwise just add it
            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    // Exit spring loaded mode if necessary after adding the widget
                    exitSpringLoadedDragModeDelayed(true, EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT,
                            null);
                }
            };
            completeAddAppWidget(appWidgetId, info.container, info.screenId, boundWidget,
                    appWidgetInfo);
            mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete, delay, false);
        }
    }

    protected void moveToCustomContentScreen(boolean animate) {
        // Close any folders that may be open.
        closeFolder();
        mWorkspace.moveToCustomContentScreen(animate);
    }

    public void addPendingItem(PendingAddItemInfo info, long container, long screenId,
            int[] cell, int spanX, int spanY) {
        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET:
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                int span[] = new int[2];
                span[0] = spanX;
                span[1] = spanY;
                addAppWidgetFromDrop((PendingAddWidgetInfo) info,
                        container, screenId, cell, span);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                processShortcutFromDrop(info.componentName, container, screenId, cell);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            }
    }

    /**
     * Process a shortcut drop.
     *
     * @param componentName The name of the component
     * @param screenId The ID of the screen where it should be added
     * @param cell The cell it should be added to, optional
     */
    private void processShortcutFromDrop(ComponentName componentName, long container, long screenId,
            int[] cell) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "processShortcutFromDrop componentName = " + componentName
                + ", container = " + container + ", screenId = " + screenId);
        }
        resetAddInfo();
        mPendingAddInfo.container = container;
        mPendingAddInfo.screenId = screenId;
        mPendingAddInfo.dropPos = null;
        mPendingAddInfo.componentName = componentName;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }

        Intent createShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        createShortcutIntent.setComponent(componentName);
        Utilities.startActivityForResultSafely(this, createShortcutIntent, REQUEST_CREATE_SHORTCUT);
    }

    /**
     * Process a widget drop.
     *
     * @param info The PendingAppWidgetInfo of the widget being added.
     * @param screenId The ID of the screen where it should be added
     * @param cell The cell it should be added to, optional
     */
    private void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, long screenId,
            int[] cell, int[] span) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addAppWidgetFromDrop: info = " + info
                + ", container = " + container + ", screenId = " + screenId);
        }
        resetAddInfo();
        mPendingAddInfo.container = info.container = container;
        mPendingAddInfo.screenId = info.screenId = screenId;
        mPendingAddInfo.dropPos = null;
        mPendingAddInfo.minSpanX = info.minSpanX;
        mPendingAddInfo.minSpanY = info.minSpanY;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            // In the case where we've prebound the widget, we remove it from the DragLayer
            if (LOGD) {
                Log.d(TAG, "Removing widget view from drag layer and setting boundWidget to null");
            }
            getDragLayer().removeView(hostView);

            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetFromDropImpl(appWidgetId, info, hostView, info.info);

            // Clear the boundWidget so that it doesn't get destroyed.
            info.boundWidget = null;
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            Bundle options = info.bindOptions;

            boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                    appWidgetId, info.info, options);
            if (success) {
                addAppWidgetFromDropImpl(appWidgetId, info, null, info.info);
            } else {
                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                mAppWidgetManager.getUser(mPendingAddWidgetInfo)
                    .addToIntent(intent, AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
                // TODO: we need to make sure that this accounts for the options bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    FolderIcon addFolder(CellLayout layout, long container, final long screenId, int cellX,
            int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        // Update the model
        LauncherModel.addItemToDatabase(Launcher.this, folderInfo, container, screenId,
                cellX, cellY);
        sFolders.put(folderInfo.id, folderInfo);

        // Create the view
        FolderIcon newFolder =
            FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo, mIconCache);
        mWorkspace.addInScreen(newFolder, container, screenId, cellX, cellY, 1, 1,
                isWorkspaceLocked());
        // Force measure the new folder icon
        CellLayout parent = mWorkspace.getParentCellLayoutForView(newFolder);
        parent.getShortcutsAndWidgets().measureChild(newFolder);
        return newFolder;
    }

    /**
     * Unbinds the view for the specified item, and removes the item and all its children.
     *
     * @param v the view being removed.
     * @param itemInfo the {@link ItemInfo} for this view.
     * @param deleteFromDb whether or not to delete this item from the db.
     */
    public boolean removeItem(View v, ItemInfo itemInfo, boolean deleteFromDb) {
        if (itemInfo instanceof ShortcutInfo) {
            // Remove the shortcut from the folder before removing it from launcher
            FolderInfo folderInfo = sFolders.get(itemInfo.container);
            if (folderInfo != null) {
                folderInfo.remove((ShortcutInfo) itemInfo);
            } else {
                mWorkspace.removeWorkspaceItem(v);
            }
            if (deleteFromDb) {
                LauncherModel.deleteItemFromDatabase(this, itemInfo);
            }
        } else if (itemInfo instanceof FolderInfo) {
            final FolderInfo folderInfo = (FolderInfo) itemInfo;
            unbindFolder(folderInfo);
            mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                LauncherModel.deleteFolderAndContentsFromDatabase(this, folderInfo);
            }
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) itemInfo;
            mWorkspace.removeWorkspaceItem(v);
            removeWidgetToAutoAdvance(widgetInfo.hostView);
            widgetInfo.hostView = null;
            if (deleteFromDb) {
                deleteWidgetInfo(widgetInfo);
            }

        } else {
            return false;
        }
        return true;
    }

    /**
     * Unbinds any launcher references to the folder.
     */
    public void unbindFolder(FolderInfo folder) {
        sFolders.remove(folder.id);
    }

    public static LongArrayMap<FolderInfo> getsFolders() {
        return sFolders;
    }

    /**
     * Deletes the widget info and the widget id.
     */
    private void deleteWidgetInfo(final LauncherAppWidgetInfo widgetInfo) {
        final LauncherAppWidgetHost appWidgetHost = getAppWidgetHost();
        if (appWidgetHost != null && !widgetInfo.isCustomWidget() && widgetInfo.isWidgetIdValid()) {
            // Deleting an app widget ID is a void call but writes to disk before returning
            // to the caller...
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void ... args) {
                    appWidgetHost.deleteAppWidgetId(widgetInfo.appWidgetId);
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
        }
        LauncherModel.deleteItemFromDatabase(this, widgetInfo);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.i(TAG, "KEY_EVENT dispatchKeyEvent event:"+event);
        if (LauncherLog.DEBUG_KEY) {
            LauncherLog.d(TAG, "dispatchKeyEvent: keyEvent = " + event);
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (Utilities.isPropertyEnabled(DUMP_STATE_PROPERTY)) {
                        dumpState();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "KEY_EVENT onBackPressed");
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "Back key pressed, mState = " + mState
                + ", mOnResumeState = " + mOnResumeState);
        }

        if (mLauncherCallbacks != null && mLauncherCallbacks.handleBackPressed()) {
            return;
        }

        if (mDragController.isDragging()) {
            mDragController.cancelDrag();
            return;
        }
        //add linhai for pagedview is reordering spark back key start on 2017/10/10
        if(mWorkspace.getIsReordering())
        {
            mWorkspace.cancelReorder();
            return;
        }
        //add linhai for pagedview is reordering spark back key start on 2017/10/10
        /// M: don't allow respond back key if workspace is in switching state
        if (!mWorkspace.isFinishedSwitchingState()) {
            LauncherLog.d(TAG, "The workspace is in switching state"
                + " when back key pressed, directly return.");
            return;
        }
        /// M.

        if (isAppsViewVisible()) {
            showWorkspace(true);
        } else if (isWidgetsViewVisible())  {
            //showOverviewMode(true);//delete by rongwenzhao to show widget list horizontal 2017-6-27
            //add by rongwenzhao to show widget list horizontal 2017-6-27 begin
            exitHorizontalWidgetsView(true);
            //add by huanghaihao in 2017-7-27 for page settings start
            mWorkspace.setSettingsVisibility(View.VISIBLE);
            //add by huanghaihao in 2017-7-27 for page settings end
			//add by linhai no space show view invisible 2017/10/17
            if (mSearchDropTargetBar != null) mSearchDropTargetBar.animateToStateForToastLy(
                    SearchDropTargetBar.State.INVISIBLE, 0);
		    //end linhai no space show view invisible 2017/10/17
        }else if(isSecondLevelWidgetVisible()){
            showHorizontalWidgetsView();
            //add by rongwenzhao to show widget list horizontal 2017-6-27 end
        } else if (isTransitionEffectViewVisible()) {
            exitTransitionEffect(true);
            //add by huanghaihao in 2017-7-27 for page settings start
            mWorkspace.setSettingsVisibility(View.VISIBLE);
            //add by huanghaihao in 2017-7-27 for page settings end

        } else if (isWallpaperViewVisible()) {
            exitWallpaperPicker();
            //add by huanghaihao in 2017-7-27 for page settings start
            mWorkspace.setSettingsVisibility(View.VISIBLE);
            //add by huanghaihao in 2017-7-27 for page settings end

        }
        else if (mWorkspace.isInOverviewMode()) {
            showWorkspace(true);
        } else if (mWorkspace.getOpenFolder() != null) {
            Folder openFolder = mWorkspace.getOpenFolder();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
            } else {
                closeFolder();
            }
		//modify by liuning for multi apps move on 2017/7/18 start
        } else if (mWorkspace.isInAppManageMode() && !mDragLayer.hasResizeFrames()) {
            exitAppManageMode();
            /**
             * Added by gaoquan 2018.03.15
             * fix 	OS2X-13872【Launcher】带有角标的文件夹拖动至移动区域后，有的显示角标，有的不显示
             */
            //-------------------------------start--------------///
            bindWorkspaceUnreadInfo();
            //-------------------------------end--------------///
		//modify by liuning for multi apps move on 2017/7/18 end
        } else {
            mWorkspace.exitWidgetResizeMode();

            // Back button is a no-op here, but give at least some feedback for the button press
            mWorkspace.showOutlinesTemporarily();
        }
    }

    /**
     * linhai 判断切页动画选择view是否显示 start 2017/6/16
     * @return true
     */

    public boolean isTransitionEffectViewVisible() {
        return (mState == State.TRANSITION_EFFECT) || (mOnResumeState == State.TRANSITION_EFFECT);
    }

    //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 start
    public boolean isWallpaperViewVisible() {
        return (mState == State.WALLPAPER_PICKER) || (mOnResumeState == State.WALLPAPER_PICKER);
    }
    //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 end

    /**
     * linhai 设置切页动画view不显示 start 2017/6/16
     * @param animated
     */
    public void exitTransitionEffect(boolean animated) {
            mOverViewButtoContainer.setVisibility(View.VISIBLE);
            mTransitionEffectsContainer.setVisibility(View.GONE);
            mState = State.WORKSPACE;
    }
    //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 start
    public void exitWallpaperPicker() {
        if (LauncherAppState.getInstance().hasWallpaperChangedSinceLastCheck()
                && mWorkspace != null) {
            mWorkspace.setWallpaperDimension();
        }
        mOverViewButtoContainer.setVisibility(View.VISIBLE);
        mWallpaperPickerContainer.setVisibility(View.GONE);
        mState = State.WORKSPACE;
    }
    //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 end

    /**
     * Re-listen when widget host is reset.
     */
    @Override
    public void onAppWidgetHostReset() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onAppWidgetReset.");
        }

        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }

        // Recreate the QSB, as the widget has been reset.
        bindSearchProviderChanged();
    }

    /**
     * Launches the intent referred by the clicked shortcut.
     *
     * @param v The view representing the clicked shortcut.
     */
    public void onClick(View v) {
        Log.i(TAG, "TOUCH_EVENT onClick");
        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).

        /// M: add systrace to analyze application launche time.
        LauncherHelper.beginSection("Launcher.onClick");

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "Click on view " + v);
        }

        if (v.getWindowToken() == null) {
            LauncherLog.d(TAG, "Click on a view with no window token, directly return.");
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
            LauncherLog.d(TAG, "The workspace is in switching state"
                + " when clicking on view, directly return.");
            return;
        }

        if (v instanceof Workspace) {
            if (mWorkspace.isInOverviewMode()) {
                showWorkspace(true);
            }
            return;
        }

        if (v instanceof CellLayout) {
            if (mWorkspace.isInOverviewMode()) {
                showWorkspace(mWorkspace.indexOfChild(v), true);
            }
        }

        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            //modify by liuning for multi apps move on 2017/7/18 start
            if (mWorkspace.isInAppManageMode()) {
                mMoveViewsRestContainer.removeViewFromWorkSpace(v);
                if (((ShortcutInfo) tag).container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    mWorkspace.sortHotSeatView();
                    getHotseat().updateCount();
                }
                mMoveViewsRestContainer.addItemFirst((ShortcutInfo) tag);
            } else {
                onClickAppShortcut(v);
            }
            //modify by liuning for multi apps move on 2017/7/18 end
        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon) {
                onClickFolderIcon(v);
            }
        } else if (v == mAllAppsButton) {
            onClickAllAppsButton(v);
        } else if (tag instanceof AppInfo) {
            startAppShortcutOrInfoActivity(v);
        } else if (tag instanceof LauncherAppWidgetInfo) {
            if (v instanceof PendingAppWidgetHostView) {
                onClickPendingWidget((PendingAppWidgetHostView) v);
            }
        }
        /// M: add systrace to analyze application launche time.
        LauncherHelper.endSection();
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    /**
     * Event handler for the app widget view which has not fully restored.
     */
    public void onClickPendingWidget(final PendingAppWidgetHostView v) {
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
        if (v.isReadyForClickSetup()) {
            int widgetId = info.appWidgetId;
            LauncherAppWidgetProviderInfo appWidgetInfo =
                    mAppWidgetManager.getLauncherAppWidgetInfo(widgetId);
            if (appWidgetInfo != null) {
                mPendingAddWidgetInfo = appWidgetInfo;
                mPendingAddInfo.copyFrom(info);
                mPendingAddWidgetId = widgetId;

                AppWidgetManagerCompat.getInstance(this).startConfigActivity(appWidgetInfo,
                        info.appWidgetId, this, mAppWidgetHost, REQUEST_RECONFIGURE_APPWIDGET);
            }
        } else if (info.installProgress < 0) {
            // The install has not been queued
            final String packageName = info.providerName.getPackageName();
            showBrokenAppInstallDialog(packageName,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivitySafely(v, LauncherModel.getMarketIntent(packageName), info);
                    }
                });
        } else {
            // Download has started.
            final String packageName = info.providerName.getPackageName();
            startActivitySafely(v, LauncherModel.getMarketIntent(packageName), info);
        }
    }

    /**
     * Event handler for the "grid" button that appears on the home screen, which
     * enters all apps mode.
     *
     * @param v The view that was clicked.
     */
    protected void onClickAllAppsButton(View v) {
        if (LOGD) Log.d(TAG, "onClickAllAppsButton");
        if (!isAppsViewVisible()) {
            showAppsView(true /* animated */, false /* resetListToTop */,
                    true /* updatePredictedApps */, false /* focusSearchBar */);

            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "[All apps launch time][Start] onClickAllAppsButton.");
            }

            if (mLauncherCallbacks != null) {
                mLauncherCallbacks.onClickAllAppsButton(v);
            }
        }
    }

    protected void onLongClickAllAppsButton(View v) {
        if (LOGD) Log.d(TAG, "onLongClickAllAppsButton");
        if (!isAppsViewVisible()) {
            showAppsView(true /* animated */, false /* resetListToTop */,
                    true /* updatePredictedApps */, true /* focusSearchBar */);
        }
    }

    private void showBrokenAppInstallDialog(final String packageName,
            DialogInterface.OnClickListener onSearchClickListener) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.abandoned_promises_title)
            .setMessage(R.string.abandoned_promise_explanation)
            .setPositiveButton(R.string.abandoned_search, onSearchClickListener)
            .setNeutralButton(R.string.abandoned_clean_this,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final UserHandleCompat user = UserHandleCompat.myUserHandle();
                        mWorkspace.removeAbandonedPromise(packageName, user);
                    }
                })
            .create().show();
        return;
    }

    /**
     * Event handler for an app shortcut click.
     *
     * @param v The view that was clicked. Must be a tagged with a {@link ShortcutInfo}.
     */
    protected void onClickAppShortcut(final View v) {
        if (LOGD) Log.d(TAG, "onClickAppShortcut");
        Object tag = v.getTag();
        if (!(tag instanceof ShortcutInfo)) {
            throw new IllegalArgumentException("Input must be a Shortcut");
        }

        // Open shortcut
        final ShortcutInfo shortcut = (ShortcutInfo) tag;
        //add for dynamic download icon by louhan & weijiaqi 20170804
        Log.d(TAG, "shortcut + " + shortcut.getIntent().toString());

        if (shortcut.getIntent() == null || shortcut.getIntent().getAction() == Intent.ACTION_CREATE_SHORTCUT) {
            //add for dynamic download icon crash by louhan & weijiaqi 20170804
            if (mIForExService == null) {
                return;
            }
            //add for dynamic download icon crash by louhan & weijiaqi 20170804
            if (v instanceof BubbleTextView) {
                BubbleTextView bubbleTextView = (BubbleTextView) v;
                String packageName = "";
                String titleName = "";
                if (shortcut.getIntent() != null && shortcut.getIntent().getPackage() != null) {
                    packageName = shortcut.getIntent().getPackage();
                }
                if (packageName == null){
                    packageName = shortcut.getPkgName();
                }
                if (shortcut.title != null) {
                    titleName = shortcut.title.toString();
                }
                if (bubbleTextView.getBitmapInfo().getStatus() == BitmapInfo.DOWNLOADING) {
                    try {
                        bubbleTextView.onPause(titleName, packageName);
                        Log.d(TAG, "shortcut + pauseDownload + " +packageName);
                        mIForExService.pauseDownload(shortcut.getIntent().getPackage());
                        updateDownloadItemByPkgName(packageName, BitmapInfo.DOWNLOAD_PAUSE, -1, null, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (bubbleTextView.getBitmapInfo().getStatus() == BitmapInfo.DOWNLOAD_PAUSE) {
                    try {
                        bubbleTextView.onResume(titleName, packageName);
                        Log.d(TAG, "shortcut + continueDownload + " + shortcut.getIntent().toString());
                        mIForExService.continueDownload(shortcut.getIntent().getPackage());
                        updateDownloadItemByPkgName(packageName, BitmapInfo.DOWNLOADING, -1, null, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (bubbleTextView.getBitmapInfo().getStatus() == BitmapInfo.PRE_INSTALL) {
                    try {
                        Log.i(TAG, "等待安装");
                        bubbleTextView.onInstalling(packageName, 1f);
                        Log.d(TAG, "shortcut + startInstall + " + packageName);
                        mIForExService.startDownload(packageName);
                        updateDownloadItemByPkgName(packageName, BitmapInfo.INSTALLING, -1, null, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }
        //add for dynamic download icon by louhan & weijiaqi 20170804

        if (shortcut.isDisabled != 0) {
            if ((shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_SUSPENDED) != 0
                || (shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_QUIET_USER) != 0) {
                // Launch activity anyway, framework will tell the user why the app is suspended.
            } else {
                int error = R.string.activity_not_found;
                if ((shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_SAFEMODE) != 0) {
                    error = R.string.safemode_shortcut_error;
                }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Check for abandoned promise
        if ((v instanceof BubbleTextView)
                && shortcut.isPromise()
                && !shortcut.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE)) {
            showBrokenAppInstallDialog(
                    shortcut.getTargetComponent().getPackageName(),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startAppShortcutOrInfoActivity(v);
                        }
                    });
            return;
        }

        // Start activities
        startAppShortcutOrInfoActivity(v);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onClickAppShortcut(v);
        }
    }

    @Thunk void startAppShortcutOrInfoActivity(View v) {
        Object tag = v.getTag();
        final ShortcutInfo shortcut;
        final Intent intent;
        if (tag instanceof ShortcutInfo) {
            shortcut = (ShortcutInfo) tag;
            intent = shortcut.intent;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1],
                    pos[0] + v.getWidth(), pos[1] + v.getHeight()));

        } else if (tag instanceof AppInfo) {
            shortcut = null;
            intent = ((AppInfo) tag).intent;
        } else {
            /**
             * Modified by gaoquan 2017.8.8
             */
            //-------------------------------start--------------///
            Log.e(TAG, "Input must be a Shortcut or AppInfo");
            return;
            //-------------------------------end--------------///
        }



        boolean success = startActivitySafely(v, intent, tag);
        mStats.recordLaunch(v, intent, shortcut);

        if (success && v instanceof BubbleTextView) {
            mWaitingForResume = (BubbleTextView) v;
            mWaitingForResume.setStayPressed(true);
        }
    }

    /**
     * Event handler for a folder icon click.
     *
     * @param v The view that was clicked. Must be an instance of {@link FolderIcon}.
     */
    protected void onClickFolderIcon(View v) {
        if (LOGD) Log.d(TAG, "onClickFolder");
        if (!(v instanceof FolderIcon)){
            throw new IllegalArgumentException("Input must be a FolderIcon");
        }

        // TODO(sunnygoyal): Re-evaluate this code.
        FolderIcon folderIcon = (FolderIcon) v;
        final FolderInfo info = folderIcon.getFolderInfo();
        Folder openFolder = mWorkspace.getFolderForTag(info);

        // If the folder info reports that the associated folder is open, then verify that
        // it is actually opened. There have been a few instances where this gets out of sync.
        if (info.opened && openFolder == null) {
            Log.d(TAG, "Folder info marked as open, but associated folder is not open. Screen: "
                    + info.screenId + " (" + info.cellX + ", " + info.cellY + ")");
            //modify by chenchao 2017.11.9 when double click folderIcon, folder will not close because info.opened status error that closeFloder getOpenFolder is null
            closeFolder();
//            info.opened = false;
            //modify by chenchao 2017.11.9 when double click folderIcon, folder will not close because info.opened status error that closeFloder getOpenFolder is null
        }

        if (!info.opened && !folderIcon.getFolder().isDestroyed()) {
            // Close any open folder
            closeFolder();
            // Open the requested folder
            openFolder(folderIcon);
        } else {
            // Find the open folder...
            int folderScreen;
            if (openFolder != null) {
                folderScreen = mWorkspace.getPageForView(openFolder);
                // .. and close it
                closeFolder(openFolder, true);
                if (folderScreen != mWorkspace.getCurrentPage()) {
                    // Close any folder open on the current screen
                    closeFolder();
                    // Pull the folder onto this screen
                    openFolder(folderIcon);
                }
            }
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onClickFolderIcon(v);
        }
    }

    /**
     * Event handler for the (Add) Widgets button that appears after a long press
     * on the home screen.
     */
    protected void onClickAddWidgetButton(View view) {
        if (LOGD) Log.d(TAG, "onClickAddWidgetButton");
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
        } else {
            //showWidgetsView(true /* animated */, true /* resetPageToZero */); //delete by rongwenzhao show widget list horizontal 2017-6-27
            showHorizontalWidgetsView();//mod by rongwenzhao show widget list horizontal 2017-6-27
            //add by huanghaihao in 2017-7-27 for page settings start
            mWorkspace.setSettingsVisibility(View.GONE);
            //add by huanghaihao in 2017-7-27 for page settings end
            if (mLauncherCallbacks != null) {
                mLauncherCallbacks.onClickAddWidgetButton(view);
            }
        }
    }

    //add by rongwenzhao 2017-6-27 to show widgets horizontal begin

    /**
     * add by rongwenzhao 2017-6-27
     * exit horizontal widget state
     */
    public void exitHorizontalWidgetsView(boolean animated) {
        if(animated){
            LauncherAnimUtils.fadeAlphaInOrOut(mWidgetsView,false,300,0);
            LauncherAnimUtils.fadeAlphaInOrOut(mOverViewButtoContainer,true,300,300);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mState = Launcher.State.WORKSPACE;
                }
            },600);
        } else {
            mOverViewButtoContainer.setVisibility(View.VISIBLE);
            mWidgetsView.setVisibility(View.GONE);
            mState = State.WORKSPACE;
        }
    }

    /**
     * add by rongwenzhao 2017-6-27
     * show widgets list horizontal
     */
    public void showHorizontalWidgetsView() {
        if(mState == State.WORKSPACE && mWorkspace.getState() == Workspace.State.OVERVIEW){
            mState = State.WIDGETS;

            //mOverViewButtoContainer.setVisibility(View.GONE);
            //mTransitionEffectsContainer.setVisibility(View.VISIBLE);
            mWidgetsRecyclerView.setVisibility(View.VISIBLE);
            mWidgetsRecyclerView.setAlpha(1f); //bug修改[有时widget列表显示为空,原因是 alpha为0]
            mWidgetHorizontalScrollView.setVisibility(View.GONE);
            LauncherAnimUtils.fadeAlphaInOrOut(mOverViewButtoContainer,false,300,0);
            if(isFirstClickWidgetButton){
                isFirstClickWidgetButton = false;
                LauncherAnimUtils.fadeAlphaInOrOut(mWidgetsView,true,300,0);
            }else{
                LauncherAnimUtils.fadeAlphaInOrOut(mWidgetsView,true,300,300);
            }
        }else if(isSecondLevelWidgetVisible()){
            //mWidgetsView.findViewById(R.id.widgets_list_view).setVisibility(View.VISIBLE);
            //mWidgetsView.findViewById(R.id.widgets_scroll_container).setVisibility(View.GONE);
            LauncherAnimUtils.fadeAlphaInOrOut(mWidgetHorizontalScrollView,false,300,0);
            LauncherAnimUtils.fadeAlphaInOrOut(mWidgetsRecyclerView,true,300,300);
        }
        DLog.e(TAG,"Launcher======= mWidgetsView.getVisibility() = " + mWidgetsView.getVisibility() + "mWidgetsView.isShown() = " + mWidgetsView.isShown());

        mWidgetsView.post(new Runnable() {
            @Override
            public void run() {
                mWidgetsView.requestFocus();
            }
        });

    }
    //add by rongwenzhao 2017-6-27 to show widgets horizontal end

    /**
     * Event handler for the wallpaper picker button that appears after a long press
     * on the home screen.
     */
    protected void onClickWallpaperPicker(View v) {
        if (!Utilities.isWallapaperAllowed(this)) {
            Toast.makeText(this, R.string.msg_disabled_by_admin, Toast.LENGTH_SHORT).show();
            return;
        }

        if (LOGD) Log.d(TAG, "onClickWallpaperPicker");
        int pageScroll = mWorkspace.getScrollForPage(mWorkspace.getPageNearestToCenterOfScreen());
        float offset = mWorkspace.mWallpaperOffset.wallpaperOffsetForScroll(pageScroll);
        //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 start
        /*startActivityForResult(new Intent(Intent.ACTION_SET_WALLPAPER).setPackage(getPackageName())
                        .putExtra(WallpaperPickerActivity.EXTRA_WALLPAPER_OFFSET, offset),
                REQUEST_PICK_WALLPAPER);*/

        showWallpaperPickerView(offset);
        //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 end
        //add by huanghaihao in 2017-7-27 for page settings start
        mWorkspace.setSettingsVisibility(View.GONE);
        //add by huanghaihao in 2017-7-27 for page settings end
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onClickWallpaperPicker(v);
        }
    }

    /**
     * Event handler for a click on the settings button that appears after a long press
     * on the home screen.
     */
    protected void onClickSettingsButton(View v) {
        if (LOGD) Log.d(TAG, "onClickSettingsButton");
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onClickSettingsButton(v);
        } else {
            startActivity(new Intent(this, SettingsActivity.class));
        }
    }

    public View.OnTouchListener getHapticFeedbackTouchListener() {
        if (mHapticFeedbackTouchListener == null) {
            mHapticFeedbackTouchListener = new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    return false;
                }
            };
        }
        return mHapticFeedbackTouchListener;
    }

    public void onDragStarted(View view) {
        if (isOnCustomContent()) {
            // Custom content screen doesn't participate in drag and drop. If on custom
            // content screen, move to default.
            moveWorkspaceToDefaultScreen();
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDragStarted(view);
        }
    }

    /**
     * Called when the user stops interacting with the launcher.
     * This implies that the user is now on the homescreen and is not doing housekeeping.
     */
    protected void onInteractionEnd() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onInteractionEnd();
        }
    }

    /**
     * Called when the user starts interacting with the launcher.
     * The possible interactions are:
     *  - open all apps
     *  - reorder an app shortcut, or a widget
     *  - open the overview mode.
     * This is a good time to stop doing things that only make sense
     * when the user is on the homescreen and not doing housekeeping.
     */
    protected void onInteractionBegin() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onInteractionBegin();
        }
    }

    /** Updates the interaction state. */
    public void updateInteraction(Workspace.State fromState, Workspace.State toState) {
        // Only update the interacting state if we are transitioning to/from a view with an
        // overlay
        boolean fromStateWithOverlay = fromState != Workspace.State.NORMAL;
        boolean toStateWithOverlay = toState != Workspace.State.NORMAL;
        if (toStateWithOverlay) {
            onInteractionBegin();
        } else if (fromStateWithOverlay) {
            onInteractionEnd();
        }
    }

    void startApplicationDetailsActivity(ComponentName componentName, UserHandleCompat user) {
        try {
            LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
            launcherApps.showAppDetailsForProfile(componentName, user);
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have permission to launch settings");
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch settings");
        }
    }

    // returns true if the activity was started
    boolean startApplicationUninstallActivity(ComponentName componentName, int flags,
            UserHandleCompat user) {
        if ((flags & AppInfo.DOWNLOADED_FLAG) == 0) {
            // System applications cannot be installed. For now, show a toast explaining that.
            // We may give them the option of disabling apps this way.
            int messageId = R.string.uninstall_system_app_text;
            Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            String packageName = componentName.getPackageName();
            String className = componentName.getClassName();
            Intent intent = new Intent(
                    Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (user != null) {
                user.addToIntent(intent, Intent.EXTRA_USER);
            }
            startActivity(intent);
            return true;
        }
    }

    private boolean startActivity(View v, Intent intent, Object tag,boolean newTask) {
        Log.v(TAG, "PROCEDURE startActivity() intent:" + intent);
        if (newTask){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        /**
         * Added by gaoquan 2017.7.21
         */
        //-------------------------------start--------------///
        if(intent != null && mPackageCircle != null && intent.getComponent() != null){
            mPackageCircle.deleteCircle(intent.getComponent().getPackageName());
        }
        //-------------------------------end--------------///

        try {
            // Only launch using the new animation if the shortcut has not opted out (this is a
            // private contract between launcher and may be ignored in the future).
            boolean useLaunchAnimation = (v != null) &&
                    !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
            LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
            UserManagerCompat userManager = UserManagerCompat.getInstance(this);

            UserHandleCompat user = null;
            if (intent.hasExtra(AppInfo.EXTRA_PROFILE)) {
                long serialNumber = intent.getLongExtra(AppInfo.EXTRA_PROFILE, -1);
                user = userManager.getUserForSerialNumber(serialNumber);
            }

            Bundle optsBundle = null;
            if (useLaunchAnimation) {
                ActivityOptions opts = null;
                if (Utilities.ATLEAST_MARSHMALLOW) {
                    // added by jubingcheng for fix:[PRODUCTION-11367] on 2018/1/3 start
                    boolean needLauncherAnimation = false;
                    if (needLauncherAnimation) {
                        // added by jubingcheng for fix:[PRODUCTION-11367] on 2018/1/3 end
                        //modify by chenchao 2017.8.3
                        try {
                            int left = 0, top = 0;
                            int width = v.getMeasuredWidth(), height = v.getMeasuredHeight();
                            if (v instanceof TextView) {
                                // Launch from center of icon, not entire view
                                Drawable icon = Workspace.getTextViewIcon((TextView) v);
                                if (icon != null) {
                                    Rect bounds = icon.getBounds();
                                    left = (width - bounds.width()) / 2;
                                    top = v.getPaddingTop();
                                    width = bounds.width();
                                    height = bounds.height();
                                }
                            }
                            opts = ActivityOptions.makeLauncherScaleUpAnimation(v, left, top,
                                    v.getMeasuredWidth() - 2 * left, v.getMeasuredHeight() - 2 * top);
                        } catch (NoSuchMethodError ex) {
                            Log.v(TAG, "ex=" + ex.getStackTrace());
                            opts = ActivityOptions.makeCustomAnimation(this,
                                    R.anim.activity_open_enter, R.anim.activity_open_exit);
                        }
                        //modify by chenchao 2017.8.3
                    }
                    // added by jubingcheng for fix:[PRODUCTION-11367] on 2018/1/3 start
                    else {
                        opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                                v.getMeasuredWidth(), v.getMeasuredHeight());
                    }
                    // added by jubingcheng for fix:[PRODUCTION-11367] on 2018/1/3 end
                } else if (!Utilities.ATLEAST_LOLLIPOP) {
                    // Below L, we use a scale up animation
                    opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                                    v.getMeasuredWidth(), v.getMeasuredHeight());
                } else if (Utilities.ATLEAST_LOLLIPOP_MR1) {
                    // On L devices, we use the device default slide-up transition.
                    // On L MR1 devices, we a custom version of the slide-up transition which
                    // doesn't have the delay present in the device default.
                    opts = ActivityOptions.makeCustomAnimation(this,
                            R.anim.task_open_enter, R.anim.no_anim);
                }
                optsBundle = opts != null ? opts.toBundle() : null;
            }else{
                if (v==null&&!intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION)){
                    ActivityOptions opts = ActivityOptions.makeCustomAnimation(this,
                            R.anim.activity_open_enter, R.anim.activity_open_exit);
                    optsBundle = opts.toBundle();
                }
            }

            if (user == null || user.equals(UserHandleCompat.myUserHandle())) {
                StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
                try {
                    // Temporarily disable deathPenalty on all default checks. For eg, shortcuts
                    // containing file Uris would cause a crash as penaltyDeathOnFileUriExposure
                    // is enabled by default on NYC.
                    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                            .penaltyLog().build());
                    // Could be launching some bookkeeping activity
                    Log.i(TAG, "TOUCH_EVENT startActivity");
                    startActivity(intent, optsBundle);
                } finally {
                    StrictMode.setVmPolicy(oldPolicy);
                }
            } else {
                // TODO Component can be null when shortcuts are supported for secondary user
                launcherApps.startActivityForProfile(intent.getComponent(), user,
                        intent.getSourceBounds(), optsBundle);
            }
            return true;
        } catch (SecurityException e) {
            if (Utilities.ATLEAST_MARSHMALLOW && tag instanceof ItemInfo) {
                // Due to legacy reasons, direct call shortcuts require Launchers to have the
                // corresponding permission. Show the appropriate permission prompt if that
                // is the case.
                if (intent.getComponent() == null
                        && Intent.ACTION_CALL.equals(intent.getAction())
                        && checkSelfPermission(Manifest.permission.CALL_PHONE) !=
                            PackageManager.PERMISSION_GRANTED) {
                    // TODO: Rename sPendingAddItem to a generic name.
                    sPendingAddItem = preparePendingAddArgs(REQUEST_PERMISSION_CALL_PHONE, intent,
                            0, (ItemInfo) tag);
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE},
                            REQUEST_PERMISSION_CALL_PHONE);
                    return false;
                }
            }
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag="+ tag + " intent=" + intent, e);
        }
        return false;
    }

    /**
     * Modified by gaoquan 2017.9.29
     */
    //-------------------------------start--------------///
    public boolean startActivitySafelyNoToast(View v, Intent intent, Object tag) {
        return startActivitySafely(v, intent, tag, false,true);
    }

    public boolean startActivitySafely(View v, Intent intent, Object tag) {
        return startActivitySafely(v, intent, tag, true,true);
    }
    public boolean startActivitySafely(View v, Intent intent, Object tag,boolean newTask) {
        return startActivitySafely(v, intent, tag, true,newTask);
    }

    public boolean startActivitySafely(View v, Intent intent, Object tag, boolean needToast,boolean newTask) {
        boolean success = false;
        if (mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            if(needToast) {
                Toast.makeText(this, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        try {
            success = startActivity(v, intent, tag,newTask);
        } catch (ActivityNotFoundException e) {
            if(needToast) {
                Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            }
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        }
        return success;
    }
    //-------------------------------end--------------///


    
    public List<String> getHideAppList(){
        return LauncherAppState.getInstance().getHideAppList();
    }
    /**
     * This method draws the FolderIcon to an ImageView and then adds and positions that ImageView
     * in the DragLayer in the exact absolute location of the original FolderIcon.
     */
    private void copyFolderIconToImage(FolderIcon fi) {
        final int width = fi.getMeasuredWidth();
        final int height = fi.getMeasuredHeight();

        // Lazy load ImageView, Bitmap and Canvas
        if (mFolderIconImageView == null) {
            mFolderIconImageView = new ImageView(this);
        }
        if (mFolderIconBitmap == null || mFolderIconBitmap.getWidth() != width ||
                mFolderIconBitmap.getHeight() != height) {
            mFolderIconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mFolderIconCanvas = new Canvas(mFolderIconBitmap);
        }

        DragLayer.LayoutParams lp;
        if (mFolderIconImageView.getLayoutParams() instanceof DragLayer.LayoutParams) {
            lp = (DragLayer.LayoutParams) mFolderIconImageView.getLayoutParams();
        } else {
            lp = new DragLayer.LayoutParams(width, height);
        }

        // The layout from which the folder is being opened may be scaled, adjust the starting
        // view size by this scale factor.
        float scale = mDragLayer.getDescendantRectRelativeToSelf(fi, mRectForFolderAnimation);
        lp.customPosition = true;
        lp.x = mRectForFolderAnimation.left;
        lp.y = mRectForFolderAnimation.top;
        lp.width = (int) (scale * width);
        lp.height = (int) (scale * height);

        mFolderIconCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        fi.draw(mFolderIconCanvas);
        mFolderIconImageView.setImageBitmap(mFolderIconBitmap);
        if (fi.getFolder() != null) {
            mFolderIconImageView.setPivotX(fi.getFolder().getPivotXForIconAnimation());
            mFolderIconImageView.setPivotY(fi.getFolder().getPivotYForIconAnimation());
        }
        // Just in case this image view is still in the drag layer from a previous animation,
        // we remove it and re-add it.
        if (mDragLayer.indexOfChild(mFolderIconImageView) != -1) {
            mDragLayer.removeView(mFolderIconImageView);
        }
        mDragLayer.addView(mFolderIconImageView, lp);
        if (fi.getFolder() != null) {
            fi.getFolder().bringToFront();
        }
    }

    private AnimatorSet mGrowAndFadeOutAnimatorSet;
    private void growAndFadeOutFolderIcon(FolderIcon fi) {
        if (fi == null) return;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.5f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.5f);

        PropertyValuesHolder blurAlpha = PropertyValuesHolder.ofFloat("alpha", 1f);

        //linhai del PRODUCTION-12501 HOTSEAT上文件夹取消设置坑位绘制 start 2018/1/5
//        FolderInfo info = (FolderInfo) fi.getTag();
//        if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
//            CellLayout cl = (CellLayout) fi.getParent().getParent();
//            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) fi.getLayoutParams();
//            cl.setFolderLeaveBehindCell(lp.cellX, lp.cellY);
//        }
        //linhai del PRODUCTION-12501 HOTSEAT上文件夹取消设置坑位绘制 start 2018/1/5

        // Push an ImageView copy of the FolderIcon into the DragLayer and hide the original
        copyFolderIconToImage(fi);
        //linhai del 父布局已经设置隐藏 此处处理便于修改坑位重叠bug start 2017/7/18
        fi.setVisibility(View.INVISIBLE);
        //linhai end 017/7/18
        mGrowAndFadeOutAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(mFolderIconImageView, alpha,
                scaleX, scaleY);
        ObjectAnimator workspaceAlpha = LauncherAnimUtils.ofPropertyValuesHolder(mWorkspace, alpha);
        ObjectAnimator hotseatAlpha = LauncherAnimUtils.ofPropertyValuesHolder(mHotseat, alpha);
        ObjectAnimator pageIndicatorsAlpha = LauncherAnimUtils.ofPropertyValuesHolder(mPageIndicators, alpha);
        ObjectAnimator folderBlurAlpha = LauncherAnimUtils.ofPropertyValuesHolder(mFolderBlur, blurAlpha);
        mGrowAndFadeOutAnimatorSet.play(oa);
        mGrowAndFadeOutAnimatorSet.play(workspaceAlpha);
        mGrowAndFadeOutAnimatorSet.play(hotseatAlpha);
        mGrowAndFadeOutAnimatorSet.play(pageIndicatorsAlpha);
        mGrowAndFadeOutAnimatorSet.play(folderBlurAlpha);

        if (Utilities.ATLEAST_LOLLIPOP) {
            mGrowAndFadeOutAnimatorSet.setInterpolator(new LogDecelerateInterpolator(100, 0));
        }
        mGrowAndFadeOutAnimatorSet.setDuration(getResources().getInteger(R.integer.config_folderExpandDuration));
        mGrowAndFadeOutAnimatorSet.start();
    }

    private void shrinkAndFadeInFolderIcon(final FolderIcon fi, boolean animate) {
        if (fi == null) return;
        if (mGrowAndFadeOutAnimatorSet != null) {
            mGrowAndFadeOutAnimatorSet.cancel();
        }
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);

        PropertyValuesHolder blurAlpha = PropertyValuesHolder.ofFloat("alpha", 0f);

        final CellLayout cl = (CellLayout) fi.getParent().getParent();

        // We remove and re-draw the FolderIcon in-case it has changed
        mDragLayer.removeView(mFolderIconImageView);
        copyFolderIconToImage(fi);
        AnimatorSet animSet = LauncherAnimUtils.createAnimatorSet();

//        oa.setDuration(getResources().getInteger(R.integer.config_folderExpandDuration));
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (cl != null) {
                    mFolderIconImageView.setLayerType(View.LAYER_TYPE_NONE, null);
                    mWorkspace.setLayerType(View.LAYER_TYPE_NONE, null);
                    mHotseat.setLayerType(View.LAYER_TYPE_NONE, null);
                    mPageIndicators.setLayerType(View.LAYER_TYPE_NONE, null);
                    mFolderBlur.setLayerType(View.LAYER_TYPE_NONE, null);
                    cl.clearFolderLeaveBehind();
                    // Remove the ImageView copy of the FolderIcon and make the original visible.
                    mDragLayer.removeView(mFolderIconImageView);
                    fi.setVisibility(View.VISIBLE);
                    mFolderBlur.setVisibility(View.GONE);
                    Drawable drawable = mFolderBlur.getBackground();
                    if (drawable != null) {
                        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                        if (bitmapDrawable != null) {
                            Bitmap bitmap = bitmapDrawable.getBitmap();
                            if (bitmap != null && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                        }
                        mFolderBlur.setBackground(null);
                    }
                }
            }
        });
        mFolderIconImageView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mWorkspace.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mHotseat.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mPageIndicators.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mFolderBlur.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(mFolderIconImageView, alpha,
                scaleX, scaleY);
        ObjectAnimator workspaceAlpha = LauncherAnimUtils.ofPropertyValuesHolder(mWorkspace, alpha);
        ObjectAnimator hotseatAlpha = LauncherAnimUtils.ofPropertyValuesHolder(mHotseat, alpha);
        ObjectAnimator pageIndicatorsAlpha = LauncherAnimUtils.ofPropertyValuesHolder(mPageIndicators, alpha);
        ObjectAnimator folderBlurAlpha = LauncherAnimUtils.ofPropertyValuesHolder(mFolderBlur, blurAlpha);
        animSet.play(oa);
        animSet.play(workspaceAlpha);
        animSet.play(hotseatAlpha);
        animSet.play(pageIndicatorsAlpha);
        animSet.play(folderBlurAlpha);
        animSet.setDuration(getResources().getInteger(R.integer.config_folderExpandDuration));
        animSet.setStartDelay(getResources().getInteger(R.integer.config_materialFolderExpandStagger));
        animSet.setInterpolator(new LinearInterpolator());
        animSet.start();
        if (!animate) {
            animSet.end();
        }
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the folder
     * is animated relative to the specified View. If the View is null, no animation
     * is played.
     *
     * @param folderInfo The FolderInfo describing the folder to open.
     */
    public void openFolder(FolderIcon folderIcon) {
        /**
         * Added by gaoquan 2017.9.28
         */
        //-------------------------------start--------------///
        if(isShowFolderBlur || mWorkspace.mIsPageMoving || mIsFolderAnimate){
            return;
        }
//        mWorkspace.mIsFolderOpened = true;
        //-------------------------------end--------------///
        /**
         * Added by gaoquan 2017.7.12
         */
        //-------------------------------start--------------///
        DisplayMetricsUtils.hideStatusBar();
        isShowFolderBlur = true;
        mFolderBlur.setAlpha(0f);
        mFolderBlur.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            public void run() {
                Bitmap b = Utilities.getScreenImage(Launcher.this);
//                int statusH = DisplayMetricsUtils.getStatusBarStableHeight();
//                int navbarH = DisplayMetricsUtils.getNavigationBarStableHeight();
//                Bitmap newBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), null, false);
//                b.recycle();
                final Bitmap smallBitmap = Utilities.toSmallofBitmap(b, true);
                final Bitmap bitmap = Utilities.javaBlur(smallBitmap, 8, true);
                final Bitmap blurBitmap = BitmapUtils.drawColorOnBitmap(bitmap, R.color.custom_background);
                mFolderBlur.post(new Runnable() {
                    public void run() {
                        if(isShowFolderBlur) {
                            mFolderBlur.setBackground(new BitmapDrawable(blurBitmap));
                        }
                    }
                });

            }
        }).start();
        //-------------------------------end--------------///

        Folder folder = folderIcon.getFolder();
        Folder openFolder = mWorkspace != null ? mWorkspace.getOpenFolder() : null;
        if (openFolder != null && openFolder != folder) {
            // Close any open folder before opening a folder.
            closeFolder();
        }

        FolderInfo info = folder.mInfo;

        info.opened = true;

        // While the folder is open, the position of the icon cannot change.
        ((CellLayout.LayoutParams) folderIcon.getLayoutParams()).canReorder = false;

        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (folder.getParent() == null) {
            mDragLayer.addView(folder);
            mDragController.addDropTarget((DropTarget) folder);
        } else {
            Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" +
                    folder.getParent() + ").");
        }
        folder.animateOpen();
        growAndFadeOutFolderIcon(folderIcon);
        // updated by jubingcheng for fix:unable show uninstallDropTarget when drag app from folder start on 2017/7/15
//        //add by liuning for hide workapce and set the background of folder 2017/6/27 start
//        hideWorkspaceSearchAndHotseat();
//        //add by liuning end
//        if (mWorkspace != null) mWorkspace.setAlpha(0f);
//        if (mHotseat != null) mHotseat.setAlpha(0f);
//        if (mPageIndicators != null) mPageIndicators.setAlpha(0f);
        // updated by jubingcheng for fix:unable show uninstallDropTarget when drag app from folder end on 2017/7/15

        // Notify the accessibility manager that this folder "window" has appeared and occluded
        // the workspace items
        folder.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        //add by linhai 文件夹打开修改文件夹名关闭输入法 布局抖动问题 start for 2017/11/22
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        //add by linhai 文件夹打开修改文件夹名关闭输入法 布局抖动问题 start for 2017/11/22

        folder.setFolderNameEditAble();

    }

    public void closeFolder() {
        closeFolder(true);
    }

    public void closeFolder(boolean animate) {
        Folder folder = mWorkspace != null ? mWorkspace.getOpenFolder() : null;
        if (folder != null) {
            if (folder.isEditingName()) {
                folder.dismissEditingName();
            }
            closeFolder(folder, animate);
        }
    }

    public void closeFolder(Folder folder, boolean animate) {
        /**
         * Added by gaoquan 2017.7.12
         */
        //-------------------------------start--------------///
        if (!mWorkspace.isInAppManageMode() && !mWorkspace.mIsDragOccuring) {//added by liuning for app manage mode on 2017/7/20
            DisplayMetricsUtils.showStatusBar();
        }
//        this.mFolderBlur.setVisibility(View.GONE);
//        Drawable drawable = this.mFolderBlur.getBackground();
//        if (drawable != null) {
//            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
//            if (bitmapDrawable != null) {
//                Bitmap bitmap = bitmapDrawable.getBitmap();
//                if (bitmap != null && !bitmap.isRecycled()) {
//                    bitmap.recycle();
//                }
//            }
//        }
        isShowFolderBlur = false;
        //-------------------------------end--------------///
        folder.getInfo().opened = false;

        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            FolderIcon fi = (FolderIcon) mWorkspace.getViewForTag(folder.mInfo);
            LauncherLog.d(TAG, "closeFolder: fi = " + fi);
            shrinkAndFadeInFolderIcon(fi, animate);
            if (fi != null) {
                ((CellLayout.LayoutParams) fi.getLayoutParams()).canReorder = true;
            }
        }
        if (animate) {
            /**
             * Added by gaoquan 2017.9.28
             */
            //-------------------------------start--------------///
            mIsFolderAnimate = true;
            //-------------------------------end--------------///
            folder.animateClosed();
        } else {
            folder.close(false);
        }

        // updated by jubingcheng for fix:unable show uninstallDropTarget when drag app from folder start on 2017/7/15
//        //add by liuning for show workapce and delete the background of folder 2017/6/27 start
//        showWorkspaceSearchAndHotseat();
//        //add by liuning end
//        if (mWorkspace != null) mWorkspace.setAlpha(1f);
//        if (mHotseat != null) mHotseat.setAlpha(1f);
//        if (mPageIndicators != null) mPageIndicators.setAlpha(1f);
        // updated by jubingcheng for fix:unable show uninstallDropTarget when drag app from folder end on 2017/7/15

        // Notify the accessibility manager that this folder "window" has disappeared and no
        // longer occludes the workspace items
        getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        //add by linhai 关闭文件时置回状态防止 负一平加载缺失 start for 2017/11/22
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        //add by linhai 关闭文件时置回状态防止 负一平加载缺失 start for 2017/11/22
        /**
         * Added by gaoquan 2017.9.28
         */
        //-------------------------------start--------------///
//        mWorkspace.mIsFolderOpened = false;
        //-------------------------------end--------------///
    }

    public boolean onLongClick(View v) {
        Log.i(TAG, "TOUCH_EVENT onLongClick");
        if (!isDraggingEnabled()) return false;
        if (isWorkspaceLocked()) return false;
        //mod by rongwenzhao enable celllayout to reorder in widget state and TRANSITION_EFFECT state 2017-6-26
        if (mState != State.WORKSPACE && mState != State.WIDGETS
                && mState != State.TRANSITION_EFFECT && mState != State.WALLPAPER_PICKER) return false;

        if (v == mAllAppsButton) {
            onLongClickAllAppsButton(v);
            return true;
        }

        if (v instanceof Workspace) {
            if (!mWorkspace.isInOverviewMode() && !mWorkspace.isInAppManageMode()) {//modify by liuning for app manage mode on 2017/7/18
                if (!mWorkspace.isTouchActive()) {
                    showOverviewMode(true);
                    mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        CellLayout.CellInfo longClickCellInfo = null;
        View itemUnderLongClick = null;
        if (v.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) v.getTag();
            longClickCellInfo = new CellLayout.CellInfo(v, info);
            itemUnderLongClick = longClickCellInfo.cell;
            resetAddInfo();
        }

        // The hotseat touch handling does not go through Workspace, and we always allow long press
        // on hotseat items.
        final boolean inHotseat = isHotseatLayout(v);
        if (!mDragController.isDragging()) {
            if (itemUnderLongClick == null) {
                // User long pressed on empty space
                mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                if (mWorkspace.isInOverviewMode()) {
                    mWorkspace.startReordering(v);
                } else if(!mWorkspace.isInAppManageMode()){//modify by liuning for app manage mode on 2017/7/18
                    showOverviewMode(true);
                }
            } else {
                final boolean isAllAppsButton = inHotseat && isAllAppsButtonRank(
                        mHotseat.getOrderInHotseat(
                                longClickCellInfo.cellX,
                                longClickCellInfo.cellY));
                if (!(itemUnderLongClick instanceof Folder || isAllAppsButton)) {
                    // User long pressed on an item
                    mWorkspace.startDrag(longClickCellInfo);
                }
            }
        }
        return true;
    }

    boolean isHotseatLayout(View layout) {
        return mHotseat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }

    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    public CellLayout getCellLayout(long container, long screenId) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        } else {
            ///M. ALPS02039919, check null pointer.
            if (mWorkspace != null) {
            return mWorkspace.getScreenWithId(screenId);
            } else {
                return null;
            }
            ///M.
        }
    }

    /**
     * For overridden classes.
     */
    public boolean isAllAppsVisible() {
        return isAppsViewVisible();
    }

    public boolean isAppsViewVisible() {
        return (mState == State.APPS) || (mOnResumeState == State.APPS);
    }

    //mod by rongwenzhao begin 是否显示一级widget列表 2017-6-27
    public boolean isWidgetsViewVisible() {
        return mState == State.WIDGETS && (mWidgetsRecyclerView.isShown());
    }
    //mod by rongwenzhao end 是否显示一级widget列表 2017-6-27

    //add by rongwenzhao begin 是否显示二级widget列表 2017-6-27
    public boolean isSecondLevelWidgetVisible(){
        return mState == State.WIDGETS && (mWidgetHorizontalScrollView.isShown());
    }
    //add by rongwenzhao end 是否显示二级widget列表 2017-6-27

    public boolean isWidgetShow() {
        return mWidgetsView.isShown();
    }


    private void setWorkspaceBackground(int background) {
        switch (background) {
            case WORKSPACE_BACKGROUND_TRANSPARENT:
                getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                break;
            case WORKSPACE_BACKGROUND_BLACK:
                getWindow().setBackgroundDrawable(null);
                break;
            default:
                getWindow().setBackgroundDrawable(mWorkspaceBackgroundDrawable);
        }
    }

    /**
     * 设置数据库监听
     * add by huanghaihao in 2017-7-11 for adding more app in folder start
     *
     * @param listener
     */
    public void setLauncherProviderChangeListener(LauncherProviderChangeListener listener) {
        mLauncherProviderChangeListener = listener;
    }

    protected void changeWallpaperVisiblity(boolean visible) {
        int wpflags = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int curflags = getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
        setWorkspaceBackground(visible ? WORKSPACE_BACKGROUND_GRADIENT : WORKSPACE_BACKGROUND_BLACK);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onTrimMemory: level = " + level);
        }

        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // The widget preview db can result in holding onto over
            // 3MB of memory for caching which isn't necessary.
            SQLiteDatabase.releaseMemory();

            // This clears all widget bitmaps from the widget tray
            // TODO(hyunyoungs)
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onTrimMemory(level);
        }
    }

    private Runnable comRunnable = new Runnable(){
        @Override
        public void run() {
            // added by jubingcheng for hide statusbar on overview mode start on 2017/7/15
            if (!mWorkspace.isInAppManageMode() && !mWorkspace.mIsDragOccuring) {//added by liuning for app manage mode on 2017/7/20
                DisplayMetricsUtils.showStatusBar();
            }
            // added by jubingcheng for hide statusbar on overview mode end on 2017/7/15
        }
    };
    /**
     * @return whether or not the Launcher state changed.
     */
    public boolean showWorkspace(boolean animated) {
        return showWorkspace(WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE, animated);
    }

    /**
     * @return whether or not the Launcher state changed.
     */
    public boolean showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        return showWorkspace(WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE, animated,
                onCompleteRunnable);
    }

    /**
     * @return whether or not the Launcher state changed.
     */
    protected boolean showWorkspace(int snapToPage, boolean animated) {
        return showWorkspace(snapToPage, animated, comRunnable);
    }

    /**
     * @return whether or not the Launcher state changed.
     */
    boolean showWorkspace(int snapToPage, boolean animated, Runnable onCompleteRunnable) {
        LauncherHelper.beginSection("showWorkspace");
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "showWorkspace: animated = " + animated + ", mState = " + mState);
        }

        ///M: ALPS02461704, fix widget quick add JE.
        if (mWorkspace == null) {
            LauncherHelper.endSection();
            return false;
        }
		//modify by liuning for app manage mode on 2017/7/18 start
        boolean changed = mState != State.WORKSPACE ||
                (mWorkspace.getState() != Workspace.State.NORMAL && mWorkspace.getState() != Workspace.State.APP_MANAGE);
        //modify by liuning for app manage mode on 2017/7/18 end
		if (changed) {
            mWorkspace.setVisibility(View.VISIBLE);
            mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                    Workspace.State.NORMAL, snapToPage, animated, onCompleteRunnable);

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
        }

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateAutoAdvanceState();

        if (changed) {
            // Send an accessibility event to announce the context change
            getWindow().getDecorView()
                    .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
        LauncherHelper.endSection();
		//update by linhai 退出预览模式后在 添加负一屏幕 start on 2017/8/02
        // added by jubingcheng for remove customContent screen on overview mode start on 2017/7/15
//        if(!mWorkspace.hasCustomContent()){
//            refreshHasCustomContentToLeft();
//        }
        // added by jubingcheng for remove customContent screen on overview mode end on 2017/7/15
		//update by linhai 退出预览模式后在 添加负一屏幕 start on 2017/8/03
        return changed;
    }

    // added by jubingcheng for remove customContent screen on overview mode start on 2017/7/15
    public void refreshHasCustomContentToLeft(){
        if(needCustomContentToLeft()){
            invalidateHasCustomContentToLeft();
        }
        if (mWorkspace.hasCustomContent()) {
            // update by huanghaihao for remove customContent screen on overview mode start on 2017/7/27
            CellLayout cellLayout = mWorkspace.mWorkspaceScreens.get(Workspace.CUSTOM_CONTENT_SCREEN_ID);
            cellLayout.setAlpha(0);
            // update by huanghaihao for remove customContent screen on overview mode end on 2017/7/27
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mWorkspace.hasCustomContent()) {
                    // update by huanghaihao for remove customContent screen on overview mode start on 2017/7/27
                    CellLayout cellLayout = mWorkspace.mWorkspaceScreens.get(Workspace.CUSTOM_CONTENT_SCREEN_ID);
                    cellLayout.setAlpha(1);
                    // update by huanghaihao for remove customContent screen on overview mode end on 2017/7/27
                }
            }
        }, getResources().getInteger(R.integer.config_overviewTransitionTime));
    }
    // added by jubingcheng for remove customContent screen on overview mode end on 2017/7/15

    /**
     * Shows the overview button.
     */
    void showOverviewMode(boolean animated) {
        showOverviewMode(animated, false);
    }

    /**
     * Shows the overview button, and if {@param requestButtonFocus} is set, will force the focus
     * onto one of the overview panel buttons.
     */
    void  showOverviewMode( boolean animated, boolean requestButtonFocus) {
        // added by jubingcheng for hide statusbar on overview mode start on 2017/7/15
        DisplayMetricsUtils.hideStatusBar();
        // added by jubingcheng for hide statusbar on overview mode end on 2017/7/15
        Runnable postAnimRunnable = null;
        if (requestButtonFocus) {
            postAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    // Hitting the menu button when in touch mode does not trigger touch mode to
                    // be disabled, so if requested, force focus on one of the overview panel
                    // buttons.
                    mOverviewPanel.requestFocusFromTouch();
                }
            };
        }
        mWorkspace.setVisibility(View.VISIBLE);

        //linhai 设置进入缩放预览模式状态值 start 2017/6/16
        PagedView.TransitionEffect.setIsEnterToOverviewMode(true);
        mOverViewButtoContainer.setVisibility(View.VISIBLE);
        mTransitionEffectsContainer.setVisibility(View.GONE);
        //linhai end 2017/6/16

        //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 start
        mWallpaperPickerContainer.setVisibility(View.GONE);
        //   Added by wjq in 2017-7-5 for 增加底边壁纸选择栏 end

        //add by rongwenzhao from normal -> overview -> widgetload state -> normal -> overview, mOverViewButtoContainer can not see : alpha is 0. 2017-6-23 begin
        mOverViewButtoContainer.setAlpha(1);
        //add by rongwenzhao from normal -> overview -> widgetload state -> normal -> overview, mOverViewButtoContainer can not see : alpha is 0. 2017-6-23 end

        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                Workspace.State.OVERVIEW,
                WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE, animated,
                postAnimRunnable);
        mState = State.WORKSPACE;
    }

    /**
     * Shows the apps view.
     */
    void showAppsView(boolean animated, boolean resetListToTop, boolean updatePredictedApps,
            boolean focusSearchBar) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "showAppsView: animated = " + animated + ", mState = " + mState);
        }
//        if (resetListToTop) {
//            mAppsView.scrollToTop();
//        }
        if (updatePredictedApps) {
            tryAndUpdatePredictedApps();
        }
        showAppsOrWidgets(State.APPS, animated, focusSearchBar);
    }

    /**
     * Shows the widgets view.
     */
    void showWidgetsView(boolean animated, boolean resetPageToZero) {
        if (LOGD) Log.d(TAG, "showWidgetsView:" + animated + " resetPageToZero:" + resetPageToZero);
        if (resetPageToZero) {
            mWidgetsView.scrollToTop();
        }
        showAppsOrWidgets(State.WIDGETS, animated, false);

        mWidgetsView.post(new Runnable() {
            @Override
            public void run() {
                mWidgetsView.requestFocus();
            }
        });
    }

    /**
     * Sets up the transition to show the apps/widgets view.
     *
     * @return whether the current from and to state allowed this operation
     */
    // TODO: calling method should use the return value so that when {@code false} is returned
    // the workspace transition doesn't fall into invalid state.
    private boolean showAppsOrWidgets(State toState, boolean animated, boolean focusSearchBar) {
        if (mState != State.WORKSPACE &&  mState != State.APPS_SPRING_LOADED &&
                mState != State.WIDGETS_SPRING_LOADED) {
            return false;
        }
        if (toState != State.APPS && toState != State.WIDGETS) {
            return false;
        }

        if (toState == State.APPS) {
            mStateTransitionAnimation.startAnimationToAllApps(mWorkspace.getState(), animated,
                    focusSearchBar);
        } else {
            mStateTransitionAnimation.startAnimationToWidgets(mWorkspace.getState(), animated);
        }

        // Change the state *after* we've called all the transition code
        mState = toState;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateAutoAdvanceState();
        closeFolder();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        return true;
    }

    /**
     * Updates the workspace and interaction state on state change, and return the animation to this
     * new state.
     */
    public Animator startWorkspaceStateChangeAnimation(Workspace.State toState, int toPage,
                                                       boolean animated, HashMap<View, Integer> layerViews) {
        Workspace.State fromState = mWorkspace.getState();
        Animator anim = mWorkspace.setStateWithAnimation(toState, toPage, animated, layerViews);
        updateInteraction(fromState, toState);
        return anim;
    }

    public void enterSpringLoadedDragMode() {
        if (LOGD) Log.d(TAG, String.format("enterSpringLoadedDragMode [mState=%s", mState.name()));
        if (mState == State.WORKSPACE || mState == State.APPS_SPRING_LOADED ||
                mState == State.WIDGETS_SPRING_LOADED) {
            return;
        }

        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                Workspace.State.SPRING_LOADED,
                WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE, true /* animated */,
                null /* onCompleteRunnable */);
        mState = isAppsViewVisible() ? State.APPS_SPRING_LOADED : State.WIDGETS_SPRING_LOADED;
    }

    public void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, int delay,
            final Runnable onCompleteRunnable) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "exitSpringLoadedDragModeDelayed successfulDrop = "
                + successfulDrop + ", delay = " + delay + ", mState = " + mState);
        }
        if (mState != State.APPS_SPRING_LOADED && mState != State.WIDGETS_SPRING_LOADED) return;

        // add by rongwenzhao to exit widget load state 2017-6-23 begin
        if (mState == State.WIDGETS_SPRING_LOADED ) {
            mState = State.WIDGETS;
            getWorkspace().setState(Workspace.State.OVERVIEW);
            return;
        }
        // add by rongwenzhao to exit widget load state 2017-6-23 end

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (successfulDrop) {
                    // TODO(hyunyoungs): verify if this hack is still needed, if not, delete.
                    //
                    // Before we show workspace, hide all apps again because
                    // exitSpringLoadedDragMode made it visible. This is a bit hacky; we should
                    // clean up our state transition functions
                    mWidgetsView.setVisibility(View.GONE);
                    showWorkspace(true, onCompleteRunnable);
                } else {
                    exitSpringLoadedDragMode();
                }
            }
        }, delay);
    }

    void exitSpringLoadedDragMode() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "exitSpringLoadedDragMode mState = " + mState);
        }

        if (mState == State.APPS_SPRING_LOADED) {
            showAppsView(true /* animated */, false /* resetListToTop */,
                    false /* updatePredictedApps */, false /* focusSearchBar */);
        } else if (mState == State.WIDGETS_SPRING_LOADED) {
            showWidgetsView(true, false);
        }
    }

    /**
     * Updates the set of predicted apps if it hasn't been updated since the last time Launcher was
     * resumed.
     */
    private void tryAndUpdatePredictedApps() {
        if (mLauncherCallbacks != null) {
            List<ComponentKey> apps = mLauncherCallbacks.getPredictedApps();
            if (apps != null) {
//                mAppsView.setPredictedApps(apps);
            }
        }
    }

    void lockAllApps() {
        // TODO
    }

    void unlockAllApps() {
        // TODO
    }

    public boolean launcherCallbacksProvidesSearch() {
        return (mLauncherCallbacks != null && mLauncherCallbacks.providesSearch());
    }

    public View getOrCreateQsbBar() {
        //jubingcheng add for hide QsbBar start 2017/6/5
        if (LauncherAppState.isDisableQsbSearchBar())
            return null;
        //jubingcheng add for hide QsbBar end
        if (launcherCallbacksProvidesSearch()) {
            return mLauncherCallbacks.getQsbBar();
        }

        if (mQsb == null) {
            AppWidgetProviderInfo searchProvider = Utilities.getSearchWidgetProvider(this);
            if (searchProvider == null) {
                return null;
            }

            Bundle opts = new Bundle();
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX);

            // Determine the min and max dimensions of the widget.
            LauncherAppState app = LauncherAppState.getInstance();
            DeviceProfile portraitProfile = app.getInvariantDeviceProfile().portraitProfile;
            DeviceProfile landscapeProfile = app.getInvariantDeviceProfile().landscapeProfile;
            float density = getResources().getDisplayMetrics().density;
            Point searchDimens = portraitProfile.getSearchBarDimensForWidgetOpts(getResources());
            int maxHeight = (int) (searchDimens.y / density);
            int minHeight = maxHeight;
            int maxWidth = (int) (searchDimens.x / density);
            int minWidth = maxWidth;
            if (!landscapeProfile.isVerticalBarLayout()) {
                searchDimens = landscapeProfile.getSearchBarDimensForWidgetOpts(getResources());
                maxHeight = (int) Math.max(maxHeight, searchDimens.y / density);
                minHeight = (int) Math.min(minHeight, searchDimens.y / density);
                maxWidth = (int) Math.max(maxWidth, searchDimens.x / density);
                minWidth = (int) Math.min(minWidth, searchDimens.x / density);
            }
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight);
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight);
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth);
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth);
            if (LOGD) {
                Log.d(TAG, "QSB widget options: maxHeight=" + maxHeight + " minHeight=" + minHeight
                        + " maxWidth=" + maxWidth + " minWidth=" + minWidth);
            }

            if (mLauncherCallbacks != null) {
                opts.putAll(mLauncherCallbacks.getAdditionalSearchWidgetOptions());
            }

            int widgetId = mSharedPrefs.getInt(QSB_WIDGET_ID, -1);
            AppWidgetProviderInfo widgetInfo = mAppWidgetManager.getAppWidgetInfo(widgetId);
            if (!searchProvider.provider.flattenToString().equals(
                    mSharedPrefs.getString(QSB_WIDGET_PROVIDER, null))
                    || (widgetInfo == null)
                    || !widgetInfo.provider.equals(searchProvider.provider)) {
                // A valid widget is not already bound.
                if (widgetId > -1) {
                    mAppWidgetHost.deleteAppWidgetId(widgetId);
                    widgetId = -1;
                }

                // Try to bind a new widget
                widgetId = mAppWidgetHost.allocateAppWidgetId();

                if (!AppWidgetManagerCompat.getInstance(this)
                        .bindAppWidgetIdIfAllowed(widgetId, searchProvider, opts)) {
                    mAppWidgetHost.deleteAppWidgetId(widgetId);
                    widgetId = -1;
                }

                mSharedPrefs.edit()
                    .putInt(QSB_WIDGET_ID, widgetId)
                    .putString(QSB_WIDGET_PROVIDER, searchProvider.provider.flattenToString())
                    .apply();
            }

            mAppWidgetHost.setQsbWidgetId(widgetId);
            if (widgetId != -1) {
                mQsb = mAppWidgetHost.createView(this, widgetId, searchProvider);
                mQsb.setId(R.id.qsb_widget);
                mQsb.updateAppWidgetOptions(opts);
                mQsb.setPadding(0, 0, 0, 0);
                mSearchDropTargetBar.addView(mQsb);
                mSearchDropTargetBar.setQsbSearchBar(mQsb);
            }
        }
        return mQsb;
    }

    private void reinflateQSBIfNecessary() {
        if (mQsb instanceof LauncherAppWidgetHostView &&
                ((LauncherAppWidgetHostView) mQsb).isReinflateRequired()) {
            mSearchDropTargetBar.removeView(mQsb);
            mQsb = null;
            mSearchDropTargetBar.setQsbSearchBar(getOrCreateQsbBar());
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        if (mState == State.APPS) {
            text.add(getString(R.string.all_apps_button_label));
        } else if (mState == State.WIDGETS) {
            text.add(getString(R.string.widget_button_text));
        } else if (mWorkspace != null) {
            text.add(mWorkspace.getCurrentPageDescription());
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    /**
     * Receives notifications when system dialogs are to be closed.
     */
    @Thunk class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ///M:ALPS02429817. {@
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                 Object reason = bundle.get("reason");
                 if (reason != null) {
                     String  closeReason = reason.toString();
                     LauncherLog.d(TAG, "Close system dialogs: reason = " + closeReason);
                     if ("lock".equals(closeReason)) {
                        return;
                     }
                 }
            }
            ///@}
            closeSystemDialogs();

        }
    }

    /**
     * If the activity is currently paused, signal that we need to run the passed Runnable
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while the activity is paused. That is because the Configuration (e.g., rotation)  might be
     * wrong when we're not running, and if the activity comes back to what the configuration was
     * when we were paused, activity is not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return {@code true} if we are currently paused. The caller might be able to skip some work
     */
    @Thunk boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (mPaused) {
            if (LOGD) Log.d(TAG, "Deferring update until onResume");
            if (deletePreviousRunnables) {
                while (mBindOnResumeCallbacks.remove(run)) {
                }
            }

            /**
             *M: ALPS02085458-This change used to avoid launcher to handle a heavey number of
             *ACTION_PACKAGE_CHANGED type pending tasks while launcher resuming.@{
             **/
            if (run instanceof AppsUpdateTask) {
                ArrayList<AppInfo> curApps = ((AppsUpdateTask) run).getApps();
                int curAppsSize = curApps.size();
                if (curAppsSize <= 0) {
                    Log.e(TAG, "Error: curAppsSize is 0");
                } else {
                    ArrayList<Runnable> removeApps = new ArrayList<Runnable>();
                    for (Runnable oldRun : mBindOnResumeCallbacks) {
                        if (oldRun instanceof AppsUpdateTask) {
                            ArrayList<AppInfo> oldApps = ((AppsUpdateTask) oldRun).getApps();
                            int oldAppsSize = oldApps.size();
                            if (oldAppsSize <= 0) {
                                Log.e(TAG, "Error: oldAppsSize is 0");
                            } else {
                                boolean hasSameItem = false;
                                for (AppInfo oldAppInfo : oldApps) {
                                    ComponentName oldAppComponent = oldAppInfo.componentName;
                                    for (AppInfo curAppInfo : curApps) {
                                        ComponentName curAppComponent = curAppInfo.componentName;
                                        if (oldAppComponent != null
                                            || curAppComponent != null
                                            || oldAppComponent.toString()
                                                == curAppComponent.toString()) {
                                            hasSameItem = true;
                                            break;
                                        }
                                    }
                                    // If last traverse didn't find the same item, then break out
                                    if (!hasSameItem) {
                                        break;
                                    }
                                }
                                if (hasSameItem) {
                                    removeApps.add(oldRun);
                                }
                            }
                        }
                    }

                    for (Runnable rmRun : removeApps) {
                        Log.d(TAG, "Debug: 1 pending task was removed");
                        mBindOnResumeCallbacks.remove(rmRun);
                    }
                    removeApps.clear();
                }
            }
            /**@}**/
            mBindOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    public void addOnResumeCallback(Runnable run) {
        mOnResumeCallbacks.add(run);
    }

    /**
     * If the activity is currently paused, signal that we need to re-run the loader
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while we are paused.  That is becaues the Configuration might be wrong
     * when we're not running, and if it comes back to what it was when we
     * were paused, we are not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused.  The caller might be able to
     * skip some work in that case since we will come back again.
     */
    public boolean setLoadOnResume() {
        if (mPaused) {
            if (LOGD) Log.d(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return SCREEN_COUNT / 2;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        setWorkspaceLoading(true);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "startBinding: this = " + this);
        }

        // If we're starting binding all over again, clear any bind calls we'd postponed in
        // the past (see waitUntilResume) -- we don't need them since we're starting binding
        // from scratch again
        mBindOnResumeCallbacks.clear();

        // Clear the workspace because it's going to be rebound
        mWorkspace.clearDropTargets();
        mWorkspace.removeAllWorkspaceScreens();

        mWidgetsToAdvance.clear();
        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
    }

    @Override
    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        bindAddScreens(orderedScreenIds);

        // If there are no screens, we need to have an empty screen
        if (orderedScreenIds.size() == 0) {
            mWorkspace.addExtraEmptyScreen();
        }

        // Create the custom content page (this call updates mDefaultScreen which calls
        // setCurrentPage() so ensure that all pages are added before calling this).
        if (needCustomContentToLeft()) {
            mWorkspace.createCustomContentContainer();
            populateCustomContentContainer();
        }
        //Added by huanghaihao in 2017-6-26 for screen setting start
        //初始化首屏文本
        long defaultPageScreenId = mWorkspace.getDefaultPageScreenId();
        mWorkspace.performDefaultPage(defaultPageScreenId);
//        mWorkspace.setCurrentPage(mWorkspace.getPageIndexForScreenId(defaultPageScreenId));
        //Added by huanghaihao in 2017-6-26 for screen setting end
    }

    @Override
    public void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(orderedScreenIds.get(i));
        }
    }

    public void bindAppsAdded(final ArrayList<Long> newScreens,
                              final ArrayList<ItemInfo> addNotAnimated,
                              final ArrayList<ItemInfo> addAnimated,
                              final ArrayList<AppInfo> addedApps) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppsAdded(newScreens, addNotAnimated, addAnimated, addedApps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        // Add the new screens
        if (newScreens != null) {
            bindAddScreens(newScreens);
        }

        // We add the items without animation on non-visible pages, and with
        // animations on the new page (which we will try and snap to).
        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
            bindItems(addNotAnimated, 0,
                    addNotAnimated.size(), false);
        }
        if (addAnimated != null && !addAnimated.isEmpty()) {
            bindItems(addAnimated, 0,
                    addAnimated.size(), true);
        }

        // Remove the extra empty screen
        if (!mWorkspace.isInAppManageMode()) {
            mWorkspace.removeExtraEmptyScreen(false, false);
        }

        // updated by jubingcheng for disableAllApps start on 2017/6/12
        //if (addedApps != null && mAppsView != null) {
//        if (!LauncherAppState.isDisableAllApps() && addedApps != null && mAppsView != null) {
        // updated by jubingcheng for disableAllApps end on 2017/6/12
//            mAppsView.addApps(addedApps);
//        }
    }

    /**
     * add by chenchao 2017/11/22
     * bindAppAdded的时候会removeExtraEmptyScreen，此时如果在应用商城下载应用，有的应用可能还需要这一页，而这一页被删除，导致问题
     * PRODUCTION-7627  	GMOS2X1-2201
     * @param screenId
     * @return
     */
    public boolean findScreenIdContainWaitingAddedApp(long screenId) {
        Iterator iterator = mDownLoadAppMap.entrySet().iterator();
        while (iterator.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) iterator.next();
            ShortcutInfo shortcutInfo = (ShortcutInfo) entry.getKey();
            if (shortcutInfo.screenId  == screenId) {
                return true;
            }
        }
        return false;
    }
    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindItems(final ArrayList<ItemInfo> shortcuts, final int start, final int end,
                          final boolean forceAnimateIcons) {
        Runnable r = new Runnable() {
            public void run() {
                bindItems(shortcuts, start, end, forceAnimateIcons);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (mDeviceProfile != null && mDeviceProfile.inv.numHotseatIcons != LauncherModel.getHotseatCountFromItems()) {
            if (LauncherModel.getHotseatCountFromItems() == 0) {
                mDeviceProfile.inv.numHotseatIcons = 1;
            } else {
                mDeviceProfile.inv.numHotseatIcons = LauncherModel.getHotseatCountFromItems();
            }
            getHotseat().setHotSeatContent();
        }

        // Get the list of added shortcuts and intersect them with the set of shortcuts here
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<Animator>();
        final boolean animateIcons = forceAnimateIcons && canRunNewAppsAnimation();
        Workspace workspace = mWorkspace;
        long newShortcutsScreenId = -1;
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);
            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "bindItems: start = " + start + ", end = " + end
                        + "item = " + item + ", this = " + this);
            }

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                    mHotseat == null) {
                continue;
            }

            final View view;
            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    ShortcutInfo info = (ShortcutInfo) item;
                    // Modify by louhan in 2017-10-20 for fix bug PRODUCTION-3490
                    String pkgName = info.getPkgName();
                    if (pkgName != null && !QueryDownloadItemByPkgName(pkgName)) {
                        continue;
                    }
                    Log.e(TAG,info.title.toString() + " | "  + info.isQueryFromDB());
                    view = createShortcut(info);
                    if (info.isQueryFromDB()) {
//                        info.setPkgName(info.getIntent().getPackage());
                        info.setQueryFromDB(false);
                        mDownLoadAppMap.put(info,(BubbleTextView)view);
                    }

                    if (info.getPkgName() != null && info.getIntent() != null && info.getIntent().getComponent() == null) {
                        mDownLoadAppMap.put(info,(BubbleTextView)view);
                    }

                    /*
                     * TODO: FIX collision case
                     */
                    if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                        CellLayout cl = mWorkspace.getScreenWithId(item.screenId);
                        if (cl != null && cl.isOccupied(item.cellX, item.cellY)) {
                            View v = cl.getChildAt(item.cellX, item.cellY);
                            Object tag = v.getTag();
                            String desc = "Collision while binding workspace item: " + item
                                    + ". Collides with " + tag;
                            if (LauncherAppState.isDogfoodBuild()) {
                                throw (new RuntimeException(desc));
                            } else {
                                Log.d(TAG, desc);
                            }
                        }
                    }
                    break;
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                    FolderIcon.setBubbleTextViewCreateListener(new BubbleTextViewCreatedListener() {
                        @Override
                        public void onCreate(ShortcutInfo info,View view) {

                            if (info.isQueryFromDB() && view != null) {
                                Log.e(TAG,info.title.toString() + " | " + info.isQueryFromDB() + " | " + (view == null ? "true" : "false"));
                                info.setPkgName(info.getIntent().getPackage());
                                mDownLoadAppMap.put(info,(BubbleTextView)view);
                            }
                        }
                    });
                    view = FolderIcon.fromXml(R.layout.folder_icon, this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                            (FolderInfo) item, mIconCache);

                    //added by liuning for PRODUCTION-11861 on 2017/12/22 start
                    if (!sFolders.containsKey(((FolderInfo) item).id)) {
                        sFolders.put(((FolderInfo) item).id, (FolderInfo) item);
                    }
                    //added by liuning for PRODUCTION-11861 on 2017/12/22 end
                    
                    break;
                default:
                    throw new RuntimeException("Invalid Item Type");
            }

            workspace.addInScreenFromBind(view, item.container, item.screenId, item.cellX,
                    item.cellY, 1, 1);
            if (animateIcons) {
                // Animate all the applications up now
                view.setAlpha(0f);
                view.setScaleX(0f);
                view.setScaleY(0f);
                bounceAnims.add(createNewAppBounceAnimation(view, i));
                newShortcutsScreenId = item.screenId;
            }
        }

        //add for dynamic download icon by louhan & weijiaqi 20170804
        Iterator iterator = mDownLoadAppMap.entrySet().iterator();
        Log.e(TAG,"create size = " + mDownLoadAppMap.size());
        while (iterator.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) iterator.next();
            ShortcutInfo shortcutInfo = (ShortcutInfo) entry.getKey();
            BubbleTextView bubbleTextView = (BubbleTextView) entry.getValue();
            Log.e(TAG,"create shortCut = " + shortcutInfo.title.toString());
            if (shortcutInfo != null && bubbleTextView != null) {
                BubbleTextView currentBubbleTextView = bubbleTextView;
                currentBubbleTextView.onStart(shortcutInfo.title.toString(), shortcutInfo.getPkgName(), shortcutInfo.getIconUrl());
                mDownLoadAppMap.put(shortcutInfo, currentBubbleTextView);
            }
        }
        //add for dynamic download icon by louhan & weijiaqi 20170804

        if (animateIcons) {
            // Animate to the correct page
            if (newShortcutsScreenId > -1) {
                long currentScreenId = mWorkspace.getScreenIdForPageIndex(mWorkspace.getNextPage());
                final int newScreenIndex = mWorkspace.getPageIndexForScreenId(newShortcutsScreenId);
                final Runnable startBounceAnimRunnable = new Runnable() {
                    public void run() {
                        anim.playTogether(bounceAnims);
                        anim.start();
                    }
                };
                if (newShortcutsScreenId != currentScreenId) {
                    // We post the animation slightly delayed to prevent slowdowns
                    // when we are loading right after we return to launcher.
                    mWorkspace.postDelayed(new Runnable() {
                        public void run() {
                            if (mWorkspace != null) {
                                mWorkspace.snapToPage(newScreenIndex);
                                mWorkspace.postDelayed(startBounceAnimRunnable,
                                        NEW_APPS_ANIMATION_DELAY);
                            }
                        }
                    }, NEW_APPS_PAGE_MOVE_DELAY);
                } else {
                    mWorkspace.postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
                }
            }
        }
        workspace.requestLayout();
        if(mUnreadLoader != null){
            mUnreadLoader.initUnreadNums();
        }
        /**
         * Added by gaoquan 2017.7.20
         */
        //-------------------------------start--------------///
        if(mPackageCircle != null && mPackageCircle.mNewApp != null){
            mPackageCircle.updateCircleChanged(mPackageCircle.mNewApp);
            mPackageCircle.mNewApp = null;
        }
        //-------------------------------end--------------///
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindFolders(final LongArrayMap<FolderInfo> folders) {
      if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "bindFolders: this = " + this);
       }

       Runnable r = new Runnable() {
            public void run() {
                bindFolders(folders);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        sFolders = folders.clone();
    }

    private void bindSafeModeWidget(LauncherAppWidgetInfo item) {
        PendingAppWidgetHostView view = new PendingAppWidgetHostView(this, item, true);
        view.updateIcon(mIconCache);
        item.hostView = view;
        item.hostView.updateAppWidget(null);
        item.hostView.setOnClickListener(this);
        addAppWidgetToWorkspace(item, null, false);
        mWorkspace.requestLayout();
    }

    /**
     * Add the views for a widget to the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppWidget(final LauncherAppWidgetInfo item) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppWidget(item);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (mIsSafeModeEnabled) {
            bindSafeModeWidget(item);
            return;
        }

        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: " + item);
        }

        final LauncherAppWidgetProviderInfo appWidgetInfo;

        if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)) {
            // If the provider is not ready, bind as a pending widget.
            appWidgetInfo = null;
        } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
            // The widget id is not valid. Try to find the widget based on the provider info.
            appWidgetInfo = mAppWidgetManager.findProvider(item.providerName, item.user);
        } else {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(item.appWidgetId);
        }

        // If the provider is ready, but the width is not yet restored, try to restore it.
        if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY) &&
                (item.restoreStatus != LauncherAppWidgetInfo.RESTORE_COMPLETED)) {
            if (appWidgetInfo == null) {
                if (DEBUG_WIDGETS) {
                    Log.d(TAG, "Removing restored widget: id=" + item.appWidgetId
                            + " belongs to component " + item.providerName
                            + ", as the povider is null");
                }
                LauncherModel.deleteItemFromDatabase(this, item);
                return;
            }

            // If we do not have a valid id, try to bind an id.
            if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
                // Note: This assumes that the id remap broadcast is received before this step.
                // If that is not the case, the id remap will be ignored and user may see the
                // click to setup view.
                PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(this, appWidgetInfo, null);
                pendingInfo.spanX = item.spanX;
                pendingInfo.spanY = item.spanY;
                pendingInfo.minSpanX = item.minSpanX;
                pendingInfo.minSpanY = item.minSpanY;
                Bundle options = WidgetHostViewLoader.getDefaultOptionsForWidget(this, pendingInfo);

                int newWidgetId = mAppWidgetHost.allocateAppWidgetId();
                boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                        newWidgetId, appWidgetInfo, options);

                // TODO consider showing a permission dialog when the widget is clicked.
                if (!success) {
                    mAppWidgetHost.deleteAppWidgetId(newWidgetId);
                    if (DEBUG_WIDGETS) {
                        Log.d(TAG, "Removing restored widget: id=" + item.appWidgetId
                                + " belongs to component " + item.providerName
                                + ", as the launcher is unable to bing a new widget id");
                    }
                    LauncherModel.deleteItemFromDatabase(this, item);
                    return;
                }

                item.appWidgetId = newWidgetId;

                // If the widget has a configure activity, it is still needs to set it up, otherwise
                // the widget is ready to go.
                item.restoreStatus = (appWidgetInfo.configure == null)
                        ? LauncherAppWidgetInfo.RESTORE_COMPLETED
                        : LauncherAppWidgetInfo.FLAG_UI_NOT_READY;

                LauncherModel.updateItemInDatabase(this, item);
            } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_UI_NOT_READY)
                    && (appWidgetInfo.configure == null)) {
                // The widget was marked as UI not ready, but there is no configure activity to
                // update the UI.
                item.restoreStatus = LauncherAppWidgetInfo.RESTORE_COMPLETED;
                LauncherModel.updateItemInDatabase(this, item);
            }
        }

        if (item.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            if (DEBUG_WIDGETS) {
                Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component "
                        + appWidgetInfo.provider);
            }

            // Verify that we own the widget
            if (appWidgetInfo == null) {
                Log.e(TAG, "Removing invalid widget: id=" + item.appWidgetId);
                deleteWidgetInfo(item);
                return;
            }

            item.hostView = mAppWidgetHost.createView(this, item.appWidgetId, appWidgetInfo);
            item.minSpanX = appWidgetInfo.minSpanX;
            item.minSpanY = appWidgetInfo.minSpanY;
            addAppWidgetToWorkspace(item, appWidgetInfo, false);
        } else {
            PendingAppWidgetHostView view = new PendingAppWidgetHostView(this, item,
                    mIsSafeModeEnabled);
            view.updateIcon(mIconCache);
            item.hostView = view;
            item.hostView.updateAppWidget(null);
            item.hostView.setOnClickListener(this);
            addAppWidgetToWorkspace(item, null, false);
        }
        mWorkspace.requestLayout();

        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bound widget id="+item.appWidgetId+" in "
                    + (SystemClock.uptimeMillis()-start) + "ms");
        }
    }

    /**
     * Restores a pending widget.
     *
     * @param appWidgetId The app widget id
     * @param cellInfo The position on screen where to create the widget.
     */
    private void completeRestoreAppWidget(final int appWidgetId) {
        LauncherAppWidgetHostView view = mWorkspace.getWidgetForAppWidgetId(appWidgetId);
        if ((view == null) || !(view instanceof PendingAppWidgetHostView)) {
            Log.e(TAG, "Widget update called, when the widget no longer exists.");
            return;
        }

        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
        info.restoreStatus = LauncherAppWidgetInfo.RESTORE_COMPLETED;

        mWorkspace.reinflateWidgetsIfNecessary();
        LauncherModel.updateItemInDatabase(this, info);
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "finishBindingItems: mSavedState = " + mSavedState
                + ", mSavedInstanceState = " + mSavedInstanceState + ", this = " + this);
        }
        Runnable r = new Runnable() {
            public void run() {
                finishBindingItems();
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentPage()).requestFocus();
            }
            mSavedState = null;
        }

        mWorkspace.restoreInstanceStateForRemainingPages();

        setWorkspaceLoading(false);
        sendLoadingCompleteBroadcastIfNecessary();

        // If we received the result of any pending adds while the loader was running (e.g. the
        // widget configuration forced an orientation change), process them now.
        if (sPendingAddItem != null) {
            final long screenId = completeAdd(sPendingAddItem);

            // TODO: this moves the user to the page where the pending item was added. Ideally,
            // the screen would be guaranteed to exist after bind, and the page would be set through
            // the workspace restore process.
            mWorkspace.post(new Runnable() {
                @Override
                public void run() {
                    mWorkspace.snapToScreenId(screenId);
                }
            });
            sPendingAddItem = null;
        }

        /**
         * Added by gaoquan 2017.6.1
         */
        //-------------------------------start--------------///
        /** M: If unread information load completed, we need to bind it to workspace.@{**/
        if (mUnreadLoadCompleted) {
            bindWorkspaceUnreadInfo();
        }
        mBindingWorkspaceFinished = true;
        /**@}**/
        mUnreadLoader.initUnreadNums();

        //-------------------------------end--------------///

        InstallShortcutReceiver.disableAndFlushInstallQueue(this);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.finishBindingItems(false);
        }

        /// M. ALPS01833637, remove the empty screen.
        mWorkspace.removeExtraEmptyScreenDelayed(true, null,
                    10, false);
        /// M.
        /**
         * Added by gaoquan 2017.7.20
         */
        //-------------------------------start--------------///
        bindWorkspaceCircle();
        //-------------------------------end--------------///
    }

    private void sendLoadingCompleteBroadcastIfNecessary() {
        if (!mSharedPrefs.getBoolean(FIRST_LOAD_COMPLETE, false)) {
            String permission =
                    getResources().getString(R.string.receive_first_load_broadcast_permission);
            Intent intent = new Intent(ACTION_FIRST_LOAD_COMPLETE);
            sendBroadcast(intent, permission);
            SharedPreferences.Editor editor = mSharedPrefs.edit();
            editor.putBoolean(FIRST_LOAD_COMPLETE, true);
            editor.apply();
        }
    }

    public boolean isAllAppsButtonRank(int rank) {
        if (mHotseat != null) {
            return mHotseat.isAllAppsButtonRank(rank);
        }
        return false;
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000)
                && (mClings == null || !mClings.isVisible());
    }

    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat("alpha", 1f),
                PropertyValuesHolder.ofFloat("scaleX", 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f));
        bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
        bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        return bounceAnim;
    }

    public boolean useVerticalBarLayout() {
        return mDeviceProfile.isVerticalBarLayout();
    }

    /** Returns the search bar bounds in pixels. */
    protected Rect getSearchBarBounds() {
        return mDeviceProfile.getSearchBarBounds(Utilities.isRtl(getResources()));
    }

    public int getSearchBarHeight() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.getSearchBarHeight();
        }
        return LauncherCallbacks.SEARCH_BAR_HEIGHT_NORMAL;
    }

    public void bindSearchProviderChanged() {
        if (mSearchDropTargetBar == null) {
            return;
        }
        if (mQsb != null) {
            mSearchDropTargetBar.removeView(mQsb);
            mQsb = null;
        }
        mSearchDropTargetBar.setQsbSearchBar(getOrCreateQsbBar());
    }

    /**
     * A runnable that we can dequeue and re-enqueue when all applications are bound (to prevent
     * multiple calls to bind the same list.)
     */
    @Thunk ArrayList<AppInfo> mTmpAppsList;
    private Runnable mBindAllApplicationsRunnable = new Runnable() {
        public void run() {
            bindAllApplications(mTmpAppsList);
            mTmpAppsList = null;
        }
    };

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(final ArrayList<AppInfo> apps) {
       if (LauncherLog.DEBUG) {
           LauncherLog.d(TAG, "bindAllApplications: apps start");
       }
        // added by jubingcheng for disableAppApps start on 2017/6/12
        if (LauncherAppState.isDisableAllApps()) {
            return;
        }
        // added by jubingcheng for disableAppApps end on 2017/6/12
       if (waitUntilResume(mBindAllApplicationsRunnable, true)) {
            mTmpAppsList = apps;
            return;
        }

//        if (mAppsView != null) {
//            mAppsView.setApps(apps);
//        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.bindAllApplications(apps);
        }
    }

    /**
     *M: ALPS02085458-This change used to avoid launcher to handle a heavey number of
     *ACTION_PACKAGE_CHANGED type pending tasks while launcher resuming.@{
     **/
    private class AppsUpdateTask implements Runnable {
        private ArrayList<AppInfo> mApps = null;

        private AppsUpdateTask(){}

        public AppsUpdateTask(final ArrayList<AppInfo> apps) {
            mApps = apps;
        }

        public void run() {
            bindAppsUpdated(mApps);
        }

        public ArrayList<AppInfo> getApps() {
            return mApps;
        }
    }
    /**@}**/

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsUpdated(final ArrayList<AppInfo> apps) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "bindAppsUpdated: apps = " + apps);
        }
        AppsUpdateTask r = new AppsUpdateTask(apps);
        /**@}**/
        if (waitUntilResume(r)) {
            return;
        }

        // updated by jubingcheng for disableAllApps start on 2017/6/12
        //if (mAppsView != null) {
//        if (!LauncherAppState.isDisableAllApps() && mAppsView != null) {
//            // updated by jubingcheng for disableAllApps end on 2017/6/12
//            if (LauncherLog.DEBUG) {
//                LauncherLog.d(TAG, "bindAppsUpdated()");
//            }
//            mAppsView.updateApps(apps);
//        }
    }

    @Override
    public void bindWidgetsRestored(final ArrayList<LauncherAppWidgetInfo> widgets) {
        Runnable r = new Runnable() {
            public void run() {
                bindWidgetsRestored(widgets);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        mWorkspace.widgetsRestored(widgets);
    }

    /**
     * Some shortcuts were updated in the background.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindShortcutsChanged(final ArrayList<ShortcutInfo> updated,
            final ArrayList<ShortcutInfo> removed, final UserHandleCompat user) {
        Runnable r = new Runnable() {
            public void run() {
                bindShortcutsChanged(updated, removed, user);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (!updated.isEmpty()) {
            mWorkspace.updateShortcuts(updated);
        }

        if (!removed.isEmpty()) {
            HashSet<ComponentName> removedComponents = new HashSet<ComponentName>();
            for (ShortcutInfo si : removed) {
                removedComponents.add(si.getTargetComponent());
            }
            mWorkspace.removeItemsByComponentName(removedComponents, user);
            // Notify the drag controller
            mDragController.onAppsRemoved(new HashSet<String>(), removedComponents);
        }
    }

    /**
     * Update the state of a package, typically related to install state.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindRestoreItemsChange(final HashSet<ItemInfo> updates) {
        Runnable r = new Runnable() {
            public void run() {
                bindRestoreItemsChange(updates);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        mWorkspace.updateRestoreItems(updates);
    }

    /**
     * A package was uninstalled/updated.  We take both the super set of packageNames
     * in addition to specific applications to remove, the reason being that
     * this can be called when a package is updated as well.  In that scenario,
     * we only remove specific components from the workspace and hotseat, where as
     * package-removal should clear all items by package name.
     */
    @Override
    public void bindWorkspaceComponentsRemoved(
            final HashSet<String> packageNames, final HashSet<ComponentName> components,
            final UserHandleCompat user) {
        Runnable r = new Runnable() {
            public void run() {
                bindWorkspaceComponentsRemoved(packageNames, components, user);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (!packageNames.isEmpty()) {
            mWorkspace.removeItemsByPackageName(packageNames, user);
        }
        if (!components.isEmpty()) {
            mWorkspace.removeItemsByComponentName(components, user);
        }
        // Notify the drag controller
        mDragController.onAppsRemoved(packageNames, components);
    }

    @Override
    public void bindAppInfosRemoved(final ArrayList<AppInfo> appInfos) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppInfosRemoved(appInfos);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        // Update AllApps
//        if (mAppsView != null) {
//            mAppsView.removeApps(appInfos);
//        }
    }

    private Runnable mBindWidgetModelRunnable = new Runnable() {
            public void run() {
                bindWidgetsModel(mWidgetsModel);
            }
        };

    @Override
    public void bindWidgetsModel(WidgetsModel model) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "bindAllPackages()");
        }
        if (waitUntilResume(mBindWidgetModelRunnable, true)) {
            mWidgetsModel = model;
            return;
        }

        if (mWidgetsView != null && model != null) {
            mWidgetsView.addWidgets(model);
            mWidgetsModel = null;
        }
    }

    @Override
    public void notifyWidgetProvidersChanged() {
        if (mWorkspace != null && mWorkspace.getState().shouldUpdateWidget) {
            mModel.refreshAndBindWidgetsAndShortcuts(this, mWidgetsView.isEmpty());
        }
    }

    private int mapConfigurationOriActivityInfoOri(int configOri) {
        final Display d = getWindowManager().getDefaultDisplay();
        int naturalOri = Configuration.ORIENTATION_LANDSCAPE;
        switch (d.getRotation()) {
        case Surface.ROTATION_0:
        case Surface.ROTATION_180:
            // We are currently in the same basic orientation as the natural orientation
            naturalOri = configOri;
            break;
        case Surface.ROTATION_90:
        case Surface.ROTATION_270:
            // We are currently in the other basic orientation to the natural orientation
            naturalOri = (configOri == Configuration.ORIENTATION_LANDSCAPE) ?
                    Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            break;
        }

        int[] oriMap = {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        };
        // Since the map starts at portrait, we need to offset if this device's natural orientation
        // is landscape.
        int indexOffset = 0;
        if (naturalOri == Configuration.ORIENTATION_LANDSCAPE) {
            indexOffset = 1;
        }
        return oriMap[(d.getRotation() + indexOffset) % 4];
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void lockScreenOrientation() {
        if (mRotationEnabled) {
            if (Utilities.ATLEAST_JB_MR2) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            } else {
                setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources()
                        .getConfiguration().orientation));
            }
        }
    }

    public void unlockScreenOrientation(boolean immediate) {
        if (mRotationEnabled) {
            if (immediate) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }, mRestoreScreenOrientationDelay);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
    }

    protected boolean isLauncherPreinstalled() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.isLauncherPreinstalled();
        }
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(getComponentName().getPackageName(), 0);
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return true;
            } else {
                return false;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method indicates whether or not we should suggest default wallpaper dimensions
     * when our wallpaper cropper was not yet used to set a wallpaper.
     */
    protected boolean overrideWallpaperDimensions() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.overrideWallpaperDimensions();
        }
        return true;
    }

    /**
     * To be overridden by subclasses to indicate that there is an activity to launch
     * before showing the standard launcher experience.
     */
    protected boolean hasFirstRunActivity() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasFirstRunActivity();
        }
        return false;
    }

    /**
     * To be overridden by subclasses to launch any first run activity
     */
    protected Intent getFirstRunActivity() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.getFirstRunActivity();
        }
        return null;
    }

    private boolean shouldRunFirstRunActivity() {
        return !ActivityManager.isRunningInTestHarness() &&
                !mSharedPrefs.getBoolean(FIRST_RUN_ACTIVITY_DISPLAYED, false);
    }

    protected boolean hasRunFirstRunActivity() {
        return mSharedPrefs.getBoolean(FIRST_RUN_ACTIVITY_DISPLAYED, false);
    }

    public boolean showFirstRunActivity() {
        if (shouldRunFirstRunActivity() &&
                hasFirstRunActivity()) {
            Intent firstRunIntent = getFirstRunActivity();
            if (firstRunIntent != null) {
                startActivity(firstRunIntent);
                markFirstRunActivityShown();
                return true;
            }
        }
        return false;
    }

    private void markFirstRunActivityShown() {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(FIRST_RUN_ACTIVITY_DISPLAYED, true);
        editor.apply();
    }

    /**
     * To be overridden by subclasses to indicate that there is an in-activity full-screen intro
     * screen that must be displayed and dismissed.
     */
    protected boolean hasDismissableIntroScreen() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasDismissableIntroScreen();
        }
        return false;
    }

    /**
     * Full screen intro screen to be shown and dismissed before the launcher can be used.
     */
    protected View getIntroScreen() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.getIntroScreen();
        }
        return null;
    }

    /**
     * To be overriden by subclasses to indicate whether the in-activity intro screen has been
     * dismissed. This method is ignored if #hasDismissableIntroScreen returns false.
     */
    private boolean shouldShowIntroScreen() {
        return hasDismissableIntroScreen() &&
                !mSharedPrefs.getBoolean(INTRO_SCREEN_DISMISSED, false);
    }

    protected void showIntroScreen() {
        View introScreen = getIntroScreen();
        changeWallpaperVisiblity(false);
        if (introScreen != null) {
            mDragLayer.showOverlayView(introScreen);
        }
    }

    public void dismissIntroScreen() {
        markIntroScreenDismissed();
        if (showFirstRunActivity()) {
            // We delay hiding the intro view until the first run activity is showing. This
            // avoids a blip.
            mWorkspace.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDragLayer.dismissOverlayView();
                    showFirstRunClings();
                }
            }, ACTIVITY_START_DELAY);
        } else {
            mDragLayer.dismissOverlayView();
            showFirstRunClings();
        }
        changeWallpaperVisiblity(true);
    }

    private void markIntroScreenDismissed() {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(INTRO_SCREEN_DISMISSED, true);
        editor.apply();
    }

    //move by rongwenzhao from 6.0 to mtk7.0 2017-7-3 begin
    public void startShakeListening() {
        mShakeListener.start();
    }

    public void stopShakeListening() {
        mShakeListener.stop();
    }
    //move by rongwenzhao from 6.0 to mtk7.0 2017-7-3 end

    @Thunk void showFirstRunClings() {
        // The two first run cling paths are mutually exclusive, if the launcher is preinstalled
        // on the device, then we always show the first run cling experience (or if there is no
        // launcher2). Otherwise, we prompt the user upon started for migration
        LauncherClings launcherClings = new LauncherClings(this);
        if (launcherClings.shouldShowFirstRunOrMigrationClings()) {
            mClings = launcherClings;
            if (mModel.canMigrateFromOldLauncherDb(this)) {
                launcherClings.showMigrationCling();
            } else {
                launcherClings.showLongPressCling(true);
            }
        }
    }

    void showWorkspaceSearchAndHotseat() {
        if (mWorkspace != null) mWorkspace.setAlpha(1f);
        if (mHotseat != null) mHotseat.setAlpha(1f);
        if (mPageIndicators != null) mPageIndicators.setAlpha(1f);
        if (mSearchDropTargetBar != null) mSearchDropTargetBar.animateToState(
                SearchDropTargetBar.State.SEARCH_BAR, 0);
    }

    void hideWorkspaceSearchAndHotseat() {
        if (mWorkspace != null) mWorkspace.setAlpha(0f);
        if (mHotseat != null) mHotseat.setAlpha(0f);
        if (mPageIndicators != null) mPageIndicators.setAlpha(0f);
        if (mSearchDropTargetBar != null) mSearchDropTargetBar.animateToState(
                SearchDropTargetBar.State.INVISIBLE, 0);
    }

    // TODO: These method should be a part of LauncherSearchCallback
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ItemInfo createAppDragInfo(Intent appLaunchIntent) {
        // Called from search suggestion
        UserHandleCompat user = null;
        if (Utilities.ATLEAST_LOLLIPOP) {
            UserHandle userHandle = appLaunchIntent.getParcelableExtra(Intent.EXTRA_USER);
            if (userHandle != null) {
                user = UserHandleCompat.fromUser(userHandle);
            }
        }
        return createAppDragInfo(appLaunchIntent, user);
    }

    // TODO: This method should be a part of LauncherSearchCallback
    public ItemInfo createAppDragInfo(Intent intent, UserHandleCompat user) {
        if (user == null) {
            user = UserHandleCompat.myUserHandle();
        }

        // Called from search suggestion, add the profile extra to the intent to ensure that we
        // can launch it correctly
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
        LauncherActivityInfoCompat activityInfo = launcherApps.resolveActivity(intent, user);
        if (activityInfo == null) {
            return null;
        }
        return new AppInfo(this, activityInfo, user, mIconCache);
    }

    // TODO: This method should be a part of LauncherSearchCallback
    public ItemInfo createShortcutDragInfo(Intent shortcutIntent, CharSequence caption,
            Bitmap icon) {
        return new ShortcutInfo(shortcutIntent, caption, caption, icon,
                UserHandleCompat.myUserHandle());
    }

    // TODO: This method should be a part of LauncherSearchCallback
    public void startDrag(View dragView, ItemInfo dragInfo, DragSource source) {
        dragView.setTag(dragInfo);
        mWorkspace.onExternalDragStartedWithItem(dragView);
        mWorkspace.beginExternalDragShared(dragView, source);
    }

    protected void moveWorkspaceToDefaultScreen() {
        mWorkspace.moveToDefaultScreen(false);
    }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPageSwitch(newPage, newPageIndex);
        }
    }

    /**
     * Returns a FastBitmapDrawable with the icon, accurately sized.
     */
    public FastBitmapDrawable createIconDrawable(Bitmap icon) {
        FastBitmapDrawable d = new FastBitmapDrawable(icon);
        d.setFilterBitmap(true);
        resizeIconDrawable(d);
        return d;
    }

    /**
     * Resizes an icon drawable to the correct icon size.
     */
    public Drawable resizeIconDrawable(Drawable icon) {
        icon.setBounds(0, 0, mDeviceProfile.iconSizePx, mDeviceProfile.iconSizePx);
        return icon;
    }

    /**
     * Prints out out state for debugging.
     */
    public void dumpState() {
        Log.d(TAG, "BEGIN launcher dump state for launcher " + this);
        Log.d(TAG, "mSavedState=" + mSavedState);
        Log.d(TAG, "mWorkspaceLoading=" + mWorkspaceLoading);
        Log.d(TAG, "mRestoring=" + mRestoring);
        Log.d(TAG, "mWaitingForResult=" + mWaitingForResult);
        Log.d(TAG, "mSavedInstanceState=" + mSavedInstanceState);
        Log.d(TAG, "sFolders.size=" + sFolders.size());
        mModel.dumpState();
        // TODO(hyunyoungs): add mWidgetsView.dumpState(); or mWidgetsModel.dumpState();

        Log.d(TAG, "END launcher dump state");
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        synchronized (sDumpLogs) {
            writer.println(" ");
            writer.println("Debug logs: ");
            for (int i = 0; i < sDumpLogs.size(); i++) {
                writer.println("  " + sDumpLogs.get(i));
            }
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.dump(prefix, fd, writer, args);
        }
    }

    public static void dumpDebugLogsToConsole() {
        if (DEBUG_DUMP_LOG) {
            synchronized (sDumpLogs) {
                Log.d(TAG, "");
                Log.d(TAG, "*********************");
                Log.d(TAG, "Launcher debug logs: ");
                for (int i = 0; i < sDumpLogs.size(); i++) {
                    Log.d(TAG, "  " + sDumpLogs.get(i));
                }
                Log.d(TAG, "*********************");
                Log.d(TAG, "");
            }
        }
    }

    public static void addDumpLog(String tag, String log, boolean debugLog) {
        addDumpLog(tag, log, null, debugLog);
    }

    public static void addDumpLog(String tag, String log, Exception e, boolean debugLog) {
        if (debugLog) {
            if (e != null) {
                Log.d(tag, log, e);
            } else {
                Log.d(tag, log);
            }
        }
        if (DEBUG_DUMP_LOG) {
            sDateStamp.setTime(System.currentTimeMillis());
            synchronized (sDumpLogs) {
                sDumpLogs.add(sDateFormat.format(sDateStamp) + ": " + tag + ", " + log
                    + (e == null ? "" : (", Exception: " + e)));
            }
        }
    }

    public static CustomAppWidget getCustomAppWidget(String name) {
        return sCustomAppWidgets.get(name);
    }

    public static HashMap<String, CustomAppWidget> getCustomAppWidgets() {
        return sCustomAppWidgets;
    }

    public void dumpLogsToLocalData() {
        if (DEBUG_DUMP_LOG) {
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void ... args) {
                    boolean success = false;
                    sDateStamp.setTime(sRunStart);
                    String FILENAME = sDateStamp.getMonth() + "-"
                            + sDateStamp.getDay() + "_"
                            + sDateStamp.getHours() + "-"
                            + sDateStamp.getMinutes() + "_"
                            + sDateStamp.getSeconds() + ".txt";

                    FileOutputStream fos = null;
                    File outFile = null;
                    try {
                        outFile = new File(getFilesDir(), FILENAME);
                        outFile.createNewFile();
                        fos = new FileOutputStream(outFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (fos != null) {
                        PrintWriter writer = new PrintWriter(fos);

                        writer.println(" ");
                        writer.println("Debug logs: ");
                        synchronized (sDumpLogs) {
                            for (int i = 0; i < sDumpLogs.size(); i++) {
                                writer.println("  " + sDumpLogs.get(i));
                            }
                        }
                        writer.close();
                    }
                    try {
                        if (fos != null) {
                            fos.close();
                            success = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * Added by gaoquan 2017.6.1
     */
    //-------------------------------unread start--------------///

    /**M: Added for unread message feature.@{**/

    /**
     * M: Bind component unread information in workspace and all apps list.
     *
     * @param component the component name of the app.
     * @param unreadNum the number of the unread message.
     */
    public void bindComponentUnreadChanged(final ComponentName component, final int unreadNum) {
        // Post to message queue to avoid possible ANR.
        mHandler.post(new Runnable() {
            public void run() {
                final long start = System.currentTimeMillis();
                if (mWorkspace != null) {
                    mWorkspace.updateComponentUnreadChanged(component, unreadNum);
                }
            }
        });
    }

    private Runnable mBindUnreadInfoIfNeeded = new Runnable() {
        @Override
        public void run() {
            bindUnreadInfoIfNeeded();
        }
    };

    /**
     * M: Bind shortcuts unread number if binding process has finished.
     */
    public void bindUnreadInfoIfNeeded() {
        if (waitUntilResume(mBindUnreadInfoIfNeeded, true)) {
            return;
        }
        if (mBindingWorkspaceFinished) {
            bindWorkspaceUnreadInfo();
        }
        mUnreadLoadCompleted = true;
    }

    /**
     * M: Bind unread number to shortcuts with data in BadgeUnreadLoader.
     */
    private void bindWorkspaceUnreadInfo() {
        mHandler.post(new Runnable() {
            public void run() {
                final long start = System.currentTimeMillis();
                if (mWorkspace != null) {
                    mWorkspace.updateShortcutsAndFoldersUnread();
                }
            }
        });
    }
    /**@}**/
    //-------------------------------unread end--------------///
    /**
     * Added by gaoquan 2017.7.20
     */
    //-------------------------------start--------------///
    private Runnable mBindCircleIfNeeded = new Runnable() {
        @Override
        public void run() {
            bindCircleIfNeeded();
        }
    };
    public void bindCircleChanged(final String packageName, final boolean isDraw){
        mHandler.post(new Runnable() {
            public void run() {
                if (mWorkspace != null) {
                    mWorkspace.updateCircleChanged(packageName, isDraw);
                }
            }
        });
    }
    public void bindWorkspaceCircle(){
        mHandler.post(new Runnable() {
            public void run() {
                if (mWorkspace != null) {
                    mWorkspace.updateShortcutsCircle();
                }
            }
        });
    }

    public void bindCircleIfNeeded(){
        if (waitUntilResume(mBindCircleIfNeeded, true)) {
            return;
        }
        if (mBindingWorkspaceFinished) {
            bindWorkspaceCircle();
        }
    }
    //-------------------------------end--------------///

    public static List<View> getFolderContents(View icon) {
        if (icon instanceof FolderIcon) {
            return ((FolderIcon) icon).getFolder().getItemsInReadingOrder();
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    private boolean isMultiTouch = false;// added by jubingcheng for fix:[PRODUCTION-12054]非锁屏界面三指下滑，调出最近任务后点back键返回，待机界面自动弹出全局收藏 on 2017/12/28

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            Log.i(TAG, "TOUCH_EVENT dispatchTouchEvent ACTION_DOWN");
            // added by jubingcheng for fix:[PRODUCTION-12054]非锁屏界面三指下滑，调出最近任务后点back键返回，待机界面自动弹出全局收藏 on 2017/12/28 start
            isMultiTouch = false;
            // added by jubingcheng for fix:[PRODUCTION-12054]非锁屏界面三指下滑，调出最近任务后点back键返回，待机界面自动弹出全局收藏 on 2017/12/28 end
            LauncherHelper.beginSection("Launcher.dispatchTouchEvent:ACTION_DOWN");
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            Log.i(TAG, "TOUCH_EVENT dispatchTouchEvent ACTION_UP");
            LauncherHelper.beginSection("Launcher.dispatchTouchEvent:ACTION_UP");
        // added by jubingcheng for fix[enable open folder when press icon] start on 2017/12/16
        } else if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
            Log.i(TAG, "TOUCH_EVENT dispatchTouchEvent " + ev);
            // added by jubingcheng for fix:[PRODUCTION-12054]非锁屏界面三指下滑，调出最近任务后点back键返回，待机界面自动弹出全局收藏 on 2017/12/28 start
            isMultiTouch = true;
            // added by jubingcheng for fix:[PRODUCTION-12054]非锁屏界面三指下滑，调出最近任务后点back键返回，待机界面自动弹出全局收藏 on 2017/12/28 end
            return true;
        }else if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) {
            Log.i(TAG, "TOUCH_EVENT dispatchTouchEvent " + ev);
            // delete by jubingcheng for fix [PRODUCTION-11835] on 2017/12/21 start
//            return true;
            // delete by jubingcheng for fix [PRODUCTION-11835] on 2017/12/21 end
        }
        // added by jubingcheng for fix[enable open folder when press icon] end on 2017/12/16
        LauncherHelper.endSection();
        return super.dispatchTouchEvent(ev);
    }

    // Added by wjq in 2017-7-4 for hideOrShowSystemUIClock start
    private void showOrHideSystemUIClock(boolean isShow) {
        Intent intent = new Intent();
        intent.setAction(Utilities.DESK_CLOCK_ACTION_NAME);
        intent.putExtra(Utilities.DESK_CLOCK_STATUS,isShow);
        // Added by wjq in 2017-8-4 for add protected Broadcast permission start
        //Added by wjq in 2017-8-21 for fixed bug GMOS-1675 start
        String permission = "com.gome.launcher.permission.SYSTEM_CLOCK_STATUS";
        //Added by wjq in 2017-8-21 for fixed bug GMOS-1675 end
//        sendBroadcast(intent,permission);
        // Added by wjq in 2017-8-4 for add protected Broadcast permission end
        Log.e(TAG,"DESK_CLOCK_STATUS = " + isShow);
    }
    // Added by wjq in 2017-7-4 for hideOrShowSystemUIClock end

    // added by jubingcheng for fix[incorrect showOrHideSystemUIClock effect when launch apps] start on 2017/7/13
    private static Timer showOrHideSystemUIClockTimer = null;
    private static TimerTask showOrHideSystemUIClockTimerTask = null;
    private static final int SHOW_SYSTEMUI_CLOCK_DELAY = 500;

    public void startShowOrHideSystemUIClockTimer(boolean isShow, long delay) {
        cancelShowOrHideSystemUIClockTimer();
        if (delay > 0) {
            final boolean show = isShow;
            showOrHideSystemUIClockTimer = new Timer();
            showOrHideSystemUIClockTimerTask = new TimerTask() {
                public void run() {
                    showOrHideSystemUIClock(show);
                }
            };
            showOrHideSystemUIClockTimer.schedule(showOrHideSystemUIClockTimerTask, delay);
        } else {
            showOrHideSystemUIClock(isShow);
        }
    }

    public void cancelShowOrHideSystemUIClockTimer() {
        if (showOrHideSystemUIClockTimer != null) {
            showOrHideSystemUIClockTimer.cancel();
            showOrHideSystemUIClockTimer = null;
        }
        if (showOrHideSystemUIClockTimerTask != null) {
            showOrHideSystemUIClockTimerTask.cancel();
            showOrHideSystemUIClockTimerTask = null;
        }
    }
    // added by jubingcheng for fix[incorrect showOrHideSystemUIClock effect when launch apps] end on 2017/7/13

    //add for dynamic calender icon by louhan 20170714 end
    @Override
    public void updateAppIcon(final String pkgName) {
        Log.d(TAG, "updateAppIcon: " + pkgName);
        Runnable r = new Runnable() {
            public void run() {
                updateAppIcon(pkgName);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (null != mWorkspace) {
            mWorkspace.updateShortcut(pkgName);
        }
    }

    @Override
    public void appStoreDataCleared() {
        // Added by weijiaqi in 2017-8-11 for fix bug GMOS-3194 start
        Iterator iterator = mDownLoadAppMap.entrySet().iterator();
        while (iterator.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) iterator.next();
            ShortcutInfo shortcutInfo = (ShortcutInfo) entry.getKey();
            IDownLoadCallback iDownLoadCallback = (IDownLoadCallback) entry.getValue();
            if (shortcutInfo.getPkgName() != null) {
                Log.d(TAG,"appStoreDataCleared = " + shortcutInfo.getPkgName());
                DownloadAppInfo downloadAppInfo = new DownloadAppInfo(shortcutInfo, iDownLoadCallback);
                removeItem((View) downloadAppInfo.getDownLoadCallback(), downloadAppInfo.getShortcutInfo(), true);
            }
        }
        if (!mWorkspace.isInAppManageMode()) {
            mWorkspace.removeExtraEmptyScreen(false, false);
        }
        mDownLoadAppMap.clear();
        // Added by weijiaqi in 2017-8-11 for fix bug GMOS-3194 end
        // Added by louhan in 2017-12-08 for fix bug PRODUCTION-10682
        mDownLoadAppMap = null;
        unbindAppService();
        bindAppService();
    }

    /**
     * Modified by gaoquan 2017.10.16
     * fix:GMOS2.0GMOS-10023【Launcher】下载登录微信且有角标，进入微信的应用信息界面清除数据，桌面微信的角标还是存在
     */
    //-------------------------------start--------------///
    @Override
    public void clearUnreadNumsByPackageName(String unreadPackageName){
        mUnreadLoader.clearUnreadNumsByPackageName(unreadPackageName);
    }

    //-------------------------------end--------------///

    @Thunk static void runOnDownLoadThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }
    //add for dynamic calender icon by louhan 20170714 end

    public void setLightNavigationBar(boolean white){
        PrivateUtil.invoke(getWindow(), "setLightNavigationBar", new Class[] { boolean.class }, new Object[] { white });
    }
}

interface DebugIntents {
    static final String DELETE_DATABASE = "com.gome.launcher.action.DELETE_DATABASE";
    static final String MIGRATE_DATABASE = "com.gome.launcher.action.MIGRATE_DATABASE";
}
