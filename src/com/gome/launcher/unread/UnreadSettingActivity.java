package com.gome.launcher.unread;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import gome.widget.GomeSwitch;

import com.gome.launcher.ItemInfo;
import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherAppState;
import com.gome.launcher.LauncherApplication;
import com.gome.launcher.LauncherModel;
import com.gome.launcher.R;
import com.gome.launcher.ShortcutInfo;
import com.gome.launcher.util.PrivateUtil;

import java.util.ArrayList;

/**
 * Created by gaoquan on 2017/5/11.
 */

/**
 * settings for unread badge app
 */
public class UnreadSettingActivity extends Activity {

    private static final String TAG = "UnreadSettingActivity";
    private static final String UNREAD_ALL_CLOSE = "unread_all_close";
	public static final String BADGE_ACTION = "com.gome.launcher.update_badge";
    public static final String BADGE_URI = "content://com.gome.launcher.unread.badgecontentprovider/badges";
    private static final String UNREAD_PACKAGE_NAME = "unreadPackageName";
    private static final String UNREAD_CLASS_NAME = "unreadClassName";
    private static final String UNREAD_CLOSE = "unreadClose";
    public static int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 0x00000010;
    public GomeSwitch mSwitch;
    private RecyclerView mRecyclerView;
    private UnreadSettingAdapter mUnreadSettingAdapter;

    private Launcher mLauncher;
    private LauncherModel mLauncherModel;
    private BadgeUnreadLoader mBadgeUnreadLoader;
    private ArrayList<UnreadItem> mUnreadItems = new ArrayList<UnreadItem>();

    private SharedPreferences mSharedPrefs;

    public boolean mTouchAll = true;
	
	private BroadcastReceiver mBroadcastReceiver;
	/**
     * listen app uninstall
     */
    public class AppBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mUnreadItems.clear();
            getAllUnreadShortcutInfo();
            if(mUnreadSettingAdapter != null) {
                mUnreadSettingAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unread_setting);
        mLauncher = LauncherApplication.getLauncher();
        if (mLauncher == null) {
            finish();
        }
        mLauncherModel = LauncherApplication.getLauncherModel();
        mBadgeUnreadLoader = mLauncher.getUnreadLoader();
        mSharedPrefs = getSharedPreferences(LauncherAppState.getSharedPreferencesKey(),
                Context.MODE_PRIVATE);
        init();
		mBroadcastReceiver = new AppBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BADGE_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUnreadItems.clear();
        getAllUnreadShortcutInfo();
        if(mUnreadSettingAdapter != null) {
            mUnreadSettingAdapter.notifyDataSetChanged();
        }
    }

	@Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    public void setNavigationStatusBarColor() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility()
                            |SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
            PrivateUtil.invoke(getWindow(), "setNavigationBarDividerColor",
                    new Class[] { int.class }, new Object[] { getResources().getColor(R.color.navigationbar_divider_color) });
        }
    }

    public BadgeUnreadLoader getMTKUnreadLoader() {
        return mBadgeUnreadLoader;
    }

    /**
     * set open or close unread badge app by SharedPreferences
     */
    public void setUnreadAllClose(boolean close) {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(UNREAD_ALL_CLOSE, close);
        editor.commit();
    }

    /**
     * get open or close unread badge app by SharedPreferences
     */
    public boolean getUnreadAllClose() {
        return mSharedPrefs.getBoolean(UNREAD_ALL_CLOSE, false);
    }

    /**
     * get all support unread badge app
     */
    public void getAllUnreadShortcutInfo() {
        ArrayList<ItemInfo> items =  mLauncherModel.getAllAppShortcutinfo();
        Cursor cursor = mBadgeUnreadLoader.getBadgeSQL().queryAll();
        while (cursor.moveToNext()) {
            String unreadPackageName = cursor.getString(1);
            String unreadClassName = cursor.getString(2);
            int unreadClose = cursor.getInt(5);
            for(ItemInfo item : items) {
                if(item instanceof ShortcutInfo) {
                    ShortcutInfo si = (ShortcutInfo)item;
                    if(unreadPackageName.equals(
                            si.intent.getComponent().getPackageName())
                            && unreadClassName.equals(si.intent.getComponent().getClassName())) {
                        ComponentName mComponentName = new ComponentName(unreadPackageName, unreadClassName);
                        UnreadItem ui = new UnreadItem();
                        ui.mShortcutInfo = si;
                        ui.mComponentName = mComponentName;
                        ui.mClose = unreadClose;
                        mUnreadItems.add(ui);
                    }
                }
            }
        }
    }

    public void init(){
        initCustomActionBar();
        mRecyclerView = (RecyclerView) findViewById(R.id.unread_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mUnreadSettingAdapter = new UnreadSettingAdapter(mUnreadItems, this);
        mRecyclerView.setAdapter(mUnreadSettingAdapter);
        mSwitch = (GomeSwitch) findViewById(R.id.all_switch);
        mSwitch.setOnCheckedChangeListener(null);
        boolean close = getUnreadAllClose();
        mSwitch.setChecked(!close);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mTouchAll) {
                    if (isChecked) { // 开启switch
                        setUnreadAllClose(false);
                        for (UnreadItem ui : mUnreadItems) {
                            ui.mClose = 0;
                        }
                        mUnreadSettingAdapter.notifyDataSetChanged();
                    } else { // 关闭swtich
                        setUnreadAllClose(true);
                        for (UnreadItem ui : mUnreadItems) {
                            ui.mClose = 1;
                        }
                        mUnreadSettingAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
        setNavigationStatusBarColor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        new Thread(new Runnable() {
            public void run() {
                for (UnreadItem ui : mUnreadItems) {
                    if(ui.mClose == 0){
                        updateAppChecked(ui.mComponentName.getPackageName(), ui.mComponentName.getClassName());
                    }else{
                        updateAppNoChecked(ui.mComponentName.getPackageName(), ui.mComponentName.getClassName());
                    }
                }
            }
        }).start();
    }

    /**
     * Added by gaoquan 2017.9.30
     * fix GM12B_量产PRODUCTION-1221【小部件】长按桌面空白处，全选开启/关闭图标角标，返回后再次进入，部分开关自动开启/关闭
     */
    //-------------------------------start--------------///
    public void updateAppNoChecked(String pName, String cName){
        Uri uri = Uri.parse(BADGE_URI);
        ContentValues cv = new ContentValues();
        cv.put(UNREAD_CLOSE, 1);
        getContentResolver().update(uri, cv,
                UNREAD_PACKAGE_NAME + "=? and " + UNREAD_CLASS_NAME + "=?",
                new String[] {pName, cName});
        final ComponentName mComponentName = new ComponentName(pName, cName);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getMTKUnreadLoader().updateUnreadNums(
                        mComponentName, 0);
            }
        });
    }

    public void updateAppChecked(String pName, String cName){
        Uri uri = Uri.parse(BADGE_URI);
        ContentValues cv = new ContentValues();
        cv.put(UNREAD_CLOSE, 0);
        getContentResolver().update(uri, cv,
                UNREAD_PACKAGE_NAME + "=? and " + UNREAD_CLASS_NAME + "=?",
                new String[] {pName, cName});
    }
    //-------------------------------end--------------///

    private void initCustomActionBar() {
//        ActionBar myActionbar = getActionBar();
//        if (myActionbar != null) {
//            myActionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//            myActionbar.setDisplayShowCustomEnabled(true);
//            myActionbar.setCustomView(R.layout.action_bar_back_unread);
//            myActionbar .setElevation(0);
//            TextView actionbar_title = (TextView) myActionbar.getCustomView().findViewById(R.id.actionbar_title);
//            ImageButton back_btn = (ImageButton) myActionbar.getCustomView().findViewById(R.id.back_btn);

            TextView actionbar_title = (TextView) findViewById(R.id.actionbar_title);
            ImageButton back_btn = (ImageButton) findViewById(R.id.back_btn);
            back_btn.setVisibility(View.VISIBLE);
            actionbar_title.setText(getResources().getString(R.string.unread_badges));

            back_btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish();
                }
            });
//        }
    }
}
