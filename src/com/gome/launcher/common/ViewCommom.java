package com.gome.launcher.common;

import android.view.View;


/**
 * Created by Administrator on 2017/6/19.
 */
public class ViewCommom {




    public static void setEnable(final View v, final long time) {
        v.setEnabled(false);
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                v.setEnabled(true);
            }
        }, time);
    }

    public static void setclickAble(final View v, final long time) {
        v.setClickable(false);
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                v.setClickable(true);
            }
        }, time);
    }
}
