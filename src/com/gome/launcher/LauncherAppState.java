/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gome.launcher.accessibility.LauncherAccessibilityDelegate;
import com.gome.launcher.compat.LauncherAppsCompat;
import com.gome.launcher.compat.PackageInstallerCompat;
import com.gome.launcher.compat.UserManagerCompat;
import com.gome.launcher.config.FeatureFlags;
import com.gome.launcher.util.ConfigMonitor;
import com.gome.launcher.util.TestingUtils;
import com.gome.launcher.util.Thunk;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

public class LauncherAppState {

    private final AppFilter mAppFilter;
    @Thunk final LauncherModel mModel;
    private final IconCache mIconCache;
    private final WidgetPreviewLoader mWidgetCache;

    private boolean mWallpaperChangedSinceLastCheck;

    private static WeakReference<LauncherProvider> sLauncherProvider;
    private static Context sContext;

    private static LauncherAppState INSTANCE;

    private InvariantDeviceProfile mInvariantDeviceProfile;

    private LauncherAccessibilityDelegate mAccessibilityDelegate;
    private List<String> mHideAppList;

    public static LauncherAppState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LauncherAppState();
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return sContext;
    }

    public static void setApplicationContext(Context context) {
        if (sContext != null) {
            Log.w(Launcher.TAG, "setApplicationContext called twice! old=" + sContext + " new=" + context);
        }
        sContext = context.getApplicationContext();
    }

    private LauncherAppState() {
        if (sContext == null) {
            throw new IllegalStateException("LauncherAppState inited before app context set");
        }

        Log.v(Launcher.TAG, "LauncherAppState inited");

        if (TestingUtils.MEMORY_DUMP_ENABLED) {
            TestingUtils.startTrackingMemory(sContext);
        }

        mInvariantDeviceProfile = new InvariantDeviceProfile(sContext);
        mIconCache = new IconCache(sContext, mInvariantDeviceProfile);
        mWidgetCache = new WidgetPreviewLoader(sContext, mIconCache);

        mAppFilter = AppFilter.loadByName(sContext.getString(R.string.app_filter_class));
        //add by chenchao 2017.8.7 init hide app black list
        String[] hideAppArrays = sContext.getResources().getStringArray(R.array.hide_app_list);
        mHideAppList =  Arrays.asList(hideAppArrays);
        mAppFilter.setHideAppList(mHideAppList);
        //add by chenchao 2017.8.7 init hide app black list
        mModel = new LauncherModel(this, mIconCache, mAppFilter);

        LauncherAppsCompat.getInstance(sContext).addOnAppsChangedCallback(mModel);

        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        // For handling managed profiles
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_REMOVED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        //add for dynamic calender icon by louhan 20170714 start
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(mModel.ACTION_PACKAGE_CLEARED);
        //add for dynamic calender icon by louhan 20170714 end
        sContext.registerReceiver(mModel, filter);
        UserManagerCompat.getInstance(sContext).enableAndResetCache();
        new ConfigMonitor(sContext).register();

        sContext.registerReceiver(
                new WallpaperChangedReceiver(), new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED));
    }

    /**
     * Call from Application.onTerminate(), which is not guaranteed to ever be called.
     */
    public void onTerminate() {
        sContext.unregisterReceiver(mModel);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
        launcherApps.removeOnAppsChangedCallback(mModel);
        PackageInstallerCompat.getInstance(sContext).onStop();
    }

    /**
     * Reloads the workspace items from the DB and re-binds the workspace. This should generally
     * not be called as DB updates are automatically followed by UI update
     */
    public void reloadWorkspace() {
        mModel.resetLoadedState(false, true);
        mModel.startLoaderFromBackground();
    }

    LauncherModel setLauncher(Launcher launcher) {
        getLauncherProvider().setLauncherProviderChangeListener(launcher);
        mModel.initialize(launcher);
        mAccessibilityDelegate = ((launcher != null) && Utilities.ATLEAST_LOLLIPOP) ?
            new LauncherAccessibilityDelegate(launcher) : null;
        return mModel;
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    static void setLauncherProvider(LauncherProvider provider) {
        sLauncherProvider = new WeakReference<LauncherProvider>(provider);
    }

    public static LauncherProvider getLauncherProvider() {
        return sLauncherProvider.get();
    }

    /**
     * Added by gaoquan 2017.6.1
     */
    //-------------------------------start--------------///
    public static String getSharedPreferencesKey() {
        return LauncherFiles.SHARED_PREFERENCES_KEY;
    }
    //-------------------------------end--------------///

    public WidgetPreviewLoader getWidgetCache() {
        return mWidgetCache;
    }

    public void onWallpaperChanged() {
        mWallpaperChangedSinceLastCheck = true;
    }

    public boolean hasWallpaperChangedSinceLastCheck() {
        boolean result = mWallpaperChangedSinceLastCheck;
        mWallpaperChangedSinceLastCheck = false;
        return result;
    }

    public InvariantDeviceProfile getInvariantDeviceProfile() {
        return mInvariantDeviceProfile;
    }

    public static boolean isDogfoodBuild() {
        return FeatureFlags.IS_ALPHA_BUILD || FeatureFlags.IS_DEV_BUILD;
    }

    /**
     * added by jubingcheng on 2017/6/5
     * @return true if we don't need QsbSearchBar
     */
    public static boolean isDisableQsbSearchBar() {
        return true;
    }

    /**
     * added by jubingcheng on 2017/6/12
     * @return true if we don't need AllAppsList
     */
    public static boolean isDisableAllApps() {
        return true;
    }

    /**
     * added by rongwenzhao on 2017/6/22
     * @return true if we do not need launcher rotate
     */
    public static boolean isDisableLauncherRotate(){
        return true;
    }
    public List<String> getHideAppList() {
        return mHideAppList;
    }
}
