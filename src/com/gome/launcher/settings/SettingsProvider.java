/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.gome.launcher.settings;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by linhai on 2017/6/19.
 */

public final class SettingsProvider {
    public static final String SETTINGS_KEY = "settings_provider_preferences";
    public static final String PRIVATE_MODE_KEY = "private_mode_preferences";

    public static final String SETTINGS_UI_HOMESCREEN_SCROLLING_TRANSITION_EFFECT = "ui_homescreen_scrolling_transition_effect";
    public static final String SETTINGS_UI_ICON_AUTO_COVER = "icon_auto_cover";
    public static final String SETTINGS_UI_SHAKE_ARRANGE_ICONS = "shake_arrange_icons";

    //linhai add HOTSEAT 动态坑位保存键值 默认值 start 2017/7/6
    public static final String NUM_HOTSEAT_ICONS  = "num_hotseat_Icons";
    //linhai end 2017/7/6



    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(SETTINGS_KEY, Context.MODE_MULTI_PROCESS);
    }

    public static SharedPreferences getForPrivateMode(Context context) {
        return context.getSharedPreferences(PRIVATE_MODE_KEY, Context.MODE_MULTI_PROCESS);
    }

    public static int getIntCustomDefault(Context context, String key, int def) {
        return get(context).getInt(key, def);
    }

    public static int getInt(Context context, String key, int resource) {
        return getIntCustomDefault(context, key, context.getResources().getInteger(resource));
    }

    public static long getLongCustomDefault(Context context, String key, long def) {
        return get(context).getLong(key, def);
    }

    public static long getLong(Context context, String key, int resource) {
        return getLongCustomDefault(context, key, context.getResources().getInteger(resource));
    }

    public static boolean getBooleanCustomDefault(Context context, String key, boolean def) {
        return get(context).getBoolean(key, def);
    }

    public static boolean getBoolean(Context context, String key, int resource) {
        return getBooleanCustomDefault(context, key, context.getResources().getBoolean(resource));
    }

    public static String getStringCustomDefault(Context context, String key, String def) {
        return get(context).getString(key, def);
    }

    public static String getString(Context context, String key, int resource) {
        return getStringCustomDefault(context, key, context.getResources().getString(resource));
    }

    public static void putString(Context context, String key, String value) {
        get(context).edit().putString(key, value).commit();
    }

    public static void putInt(Context context, String key, int value) {
        get(context).edit().putInt(key, value).commit();
    }

    public static void putLong(Context context, String key, long value) {
        get(context).edit().putLong(key, value).commit();
    }

    public static void putBoolean(Context context, String key, boolean value) {
        get(context).edit().putBoolean(key, value).commit();
    }
}
