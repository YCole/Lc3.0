/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gome.launcher.settings.SettingsProvider;
import com.gome.launcher.unread.UnreadSettingActivity;
import com.gome.launcher.util.DLog;
import com.gome.launcher.util.PrivateUtil;

import gome.widget.GomeSwitch;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity {
    private static final String TAG = "SettingsActivity";

    public static int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 0x00000010;
    /**
     * Added by gaoquan 2017.6.1
     */
    //-------------------------------start--------------///
    private LinearLayout mLinearLayout;

    //-------------------------------end--------------///
    /**
     * Added by gaoquan 2017.8.22
     */
    //-------------------------------start--------------///
    public GomeSwitch mCoverSwitch;
    public GomeSwitch mShakeSwitch;
    //-------------------------------end--------------///

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Added by gaoquan 2017.6.1
         */
        //-------------------------------start--------------///
        setContentView(R.layout.activity_settings);
        initCustomActionBar();

        // Display the fragment as the main content.
//        getFragmentManager().beginTransaction()
//                .replace(R.id.content_settings, new LauncherSettingsFragment())
//                .commit();

        /**
         * Added by gaoquan 2017.8.22
         */
        //-------------------------------start--------------///
        mCoverSwitch = (GomeSwitch) findViewById(R.id.cover_switch);
        mCoverSwitch.setOnCheckedChangeListener(null);
        mCoverSwitch.setCheckedImmediately(SettingsProvider.getBooleanCustomDefault(this, SettingsProvider.SETTINGS_UI_ICON_AUTO_COVER, false));
        mCoverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsProvider.putBoolean(SettingsActivity.this, SettingsProvider.SETTINGS_UI_ICON_AUTO_COVER, isChecked);
            }
        });
        mShakeSwitch = (GomeSwitch) findViewById(R.id.shake_switch);
        mShakeSwitch.setOnCheckedChangeListener(null);
        mShakeSwitch.setCheckedImmediately(SettingsProvider.getBooleanCustomDefault(this, SettingsProvider.SETTINGS_UI_SHAKE_ARRANGE_ICONS, false));
        mShakeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsProvider.putBoolean(SettingsActivity.this, SettingsProvider.SETTINGS_UI_SHAKE_ARRANGE_ICONS, isChecked);
            }
        });
        //-------------------------------end--------------///

        mLinearLayout = (LinearLayout) findViewById(R.id.unread);
        mLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, UnreadSettingActivity.class);
                startActivity(intent);
            }
        });
        setNavigationStatusBarColor();
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
            actionbar_title.setText(getResources().getString(R.string.settings_button_text));

            back_btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish();
                }
            });
//        }
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragment
            implements OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.launcher_preferences);

            PreferenceScreen settingsContainer = (PreferenceScreen)findPreference("settings_container");

            SwitchPreference pref = (SwitchPreference) findPreference(
                    Utilities.ALLOW_ROTATION_PREFERENCE_KEY);
            pref.setPersistent(false);

            //rongwenzhao add for disable allowRotation begin date:2017-6-22
            if(LauncherAppState.isDisableLauncherRotate()) {//rongwenzhao mod for disable allowRotation date:2017-6-22
                if (settingsContainer != null) {
                    settingsContainer.removePreference(pref);
                }
            }else{
                Bundle extras = new Bundle();
                extras.putBoolean(LauncherSettings.Settings.EXTRA_DEFAULT_VALUE, false);
                Bundle value = getActivity().getContentResolver().call(
                        LauncherSettings.Settings.CONTENT_URI,
                        LauncherSettings.Settings.METHOD_GET_BOOLEAN,
                        Utilities.ALLOW_ROTATION_PREFERENCE_KEY, extras);
                pref.setChecked(value.getBoolean(LauncherSettings.Settings.EXTRA_VALUE));

                pref.setOnPreferenceChangeListener(this);
            }
            //rongwenzhao add for disable allowRotation  end date:2017-6-22

            //rongwenzhao add for auto arrange icons after delete icon from screen begin date:2017-6-20
            SwitchPreference autoCoverPref = (SwitchPreference) findPreference(
                    SettingsProvider.SETTINGS_UI_ICON_AUTO_COVER);
            autoCoverPref.setChecked(SettingsProvider.getBooleanCustomDefault(getActivity(), SettingsProvider.SETTINGS_UI_ICON_AUTO_COVER, false));
            autoCoverPref.setOnPreferenceChangeListener(this);
            //rongwenzhao add for auto arrange icons after delete icon from screen end date:2017-6-20

            //rongwenzhao add date:2017-7-3 Shake automatically align the desktop icon begin
            SwitchPreference shakeArrangePref = (SwitchPreference) findPreference(
                    SettingsProvider.SETTINGS_UI_SHAKE_ARRANGE_ICONS);
            shakeArrangePref.setChecked(SettingsProvider.getBooleanCustomDefault(getActivity(), SettingsProvider.SETTINGS_UI_SHAKE_ARRANGE_ICONS, false));
            shakeArrangePref.setOnPreferenceChangeListener(this);
            //rongwenzhao add date:2017-7-3 Shake automatically align the desktop icon end

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean value = (boolean) newValue;
            DLog.e(TAG,"SettingsActivity -onPreferenceChange- getKey = "+preference.getKey() + " newValue = "+newValue);
            if(Utilities.ALLOW_ROTATION_PREFERENCE_KEY.equals(preference.getKey())) {
                Bundle extras = new Bundle();
                extras.putBoolean(LauncherSettings.Settings.EXTRA_VALUE, value);
                getActivity().getContentResolver().call(
                        LauncherSettings.Settings.CONTENT_URI,
                        LauncherSettings.Settings.METHOD_SET_BOOLEAN,
                        preference.getKey(), extras);
            //rongwenzhao add for auto arrange icons after delete icon from screen begin date:2017-6-20
            }else if(SettingsProvider.SETTINGS_UI_ICON_AUTO_COVER.equals(preference.getKey())){
                SettingsProvider.putBoolean(getActivity(), SettingsProvider.SETTINGS_UI_ICON_AUTO_COVER, value);
                //rongwenzhao add for auto arrange icons after delete icon from screen end date:2017-6-20
            }else if(SettingsProvider.SETTINGS_UI_SHAKE_ARRANGE_ICONS.equals(preference.getKey())){
                //add by rongwenzhao Shake automatically align the desktop icon begin 2017-7-3
                SettingsProvider.putBoolean(getActivity(), SettingsProvider.SETTINGS_UI_SHAKE_ARRANGE_ICONS, value);
                //add by rongwenzhao Shake automatically align the desktop icon end 2017-7-3
            }

            return true;
        }
    }
}
