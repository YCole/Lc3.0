package com.gome.launcher.unread;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;

/**
 * Created by gaoquan on 2017/5/10.
 */

/**
 * listen the unread badges
 */
public class BadgeContentObserver extends ContentObserver {

    private static final String TAG = "BadgeContentObserver";
    private Context mContext;
    private BadgeUnreadLoader mBadgeUnreadLoader;

    public BadgeContentObserver(Context mContext, Handler handler, BadgeUnreadLoader mBadgeUnreadLoader) {
        super(handler);
        this.mContext = mContext;
        this.mBadgeUnreadLoader = mBadgeUnreadLoader;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        mBadgeUnreadLoader.initUnreadNums();
    }
}
