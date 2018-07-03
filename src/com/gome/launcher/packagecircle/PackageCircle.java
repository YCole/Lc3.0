package com.gome.launcher.packagecircle;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;

import com.gome.launcher.BubbleTextView;
import com.gome.launcher.DeviceProfile;
import com.gome.launcher.ItemInfo;
import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherSettings;
import com.gome.launcher.R;
import com.gome.launcher.ShortcutInfo;
import com.gome.launcher.Utilities;
import com.gome.launcher.Workspace;

import java.lang.ref.WeakReference;

/**
 * Created by gaoquan on 2017/7/20.
 */

public class PackageCircle {

    private int circleRadius;
    private int circleTitleSpacing;
    public String mNewApp = null;
    private WeakReference<CircleCallbacks> mCallbacks;
    private CircleSQL mCircleSQL;
    private Context mContext;
    DeviceProfile mGrid;

    public PackageCircle(Context context){
        mCircleSQL = new CircleSQL(context);
        mContext = context;
        Resources res = context.getResources();
        mGrid = ((Launcher) context).getDeviceProfile();
        circleRadius = res.getDimensionPixelSize(R.dimen.circle_radius);
        circleTitleSpacing = res.getDimensionPixelSize(R.dimen.circle_title_spacing);
    }

    public int getCircleRadius() {
        return circleRadius;
    }

    public int getCircleTitleSpacing() {
        return circleTitleSpacing;
    }

    public void initialize(CircleCallbacks callbacks) {
        mCallbacks = new WeakReference<CircleCallbacks>(callbacks);
    }

    public void initCircles(){
        if (mCallbacks != null) {
            CircleCallbacks callbacks = mCallbacks.get();
            if(callbacks != null){
                callbacks.bindCircleIfNeeded();
            }
        }
    }

    public boolean circleExist(String packageName){
        if(packageName == null){
            return false;
        }
        return mCircleSQL.queryExist(packageName);
    }

    public void drawCircle(Canvas canvas, BubbleTextView icon){
        ItemInfo info = (ItemInfo) icon.getTag();
        if (info != null && info.isNewApp) {
            canvas.save();
            Resources res = icon.getContext().getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            float x = 0;
            float y = 0;
            // updated by jubingcheng for specify text padding start on 2017/7/24
            int paddingLeftRight = res.getDimensionPixelSize(R.dimen.dynamic_grid_text_left_right_padding);
            // updated by jubingcheng for specify text padding start end 2017/7/24
            if (icon instanceof BubbleTextView) {
                if (icon.getTextBound().width() > icon.getWidth() - (paddingLeftRight + circleRadius * 2 + circleTitleSpacing ) * 2) {
                    x = paddingLeftRight + circleRadius;
                } else {
                    x = (icon.getWidth() - icon.getTextBound().width()) / 2 - circleTitleSpacing - circleRadius;
                }
                if (info instanceof ShortcutInfo) {
                    y = icon.getHeight() -  icon.getPaddingTop() - (icon.getPaint().getFontMetrics().bottom - icon.getPaint().getFontMetrics().top) / 2;
                    canvas.translate(icon.getScrollX(), icon.getScrollY());
                    Paint circlePaint = new Paint();
                    circlePaint.setAntiAlias(true);
                    circlePaint.setColor(0xE5FFFFFF);
                    canvas.drawCircle(x, y, circleRadius, circlePaint);
                    canvas.restore();
                }
            }
        }
    }

    public void updateCircleChanged(String packageName){
        if (mCallbacks != null) {
            if(packageName != null){
                CircleCallbacks callbacks = mCallbacks.get();
                if(callbacks != null){
                    callbacks.bindCircleChanged(packageName, true);
                    mCircleSQL.addOne(packageName);
                }
            }
        }
    }

    public void deleteCircle(final String packageName){
        if (mCallbacks != null) {
            if(packageName != null){
                CircleCallbacks callbacks = mCallbacks.get();
                if(callbacks != null){
                    callbacks.bindCircleChanged(packageName, false);
                    new Thread(new Runnable() {
                        public void run() {
                            mCircleSQL.deleteOne(packageName);
                        }
                    }).start();
                }
            }
        }
    }

    public interface CircleCallbacks {
        /**
         * Bind shortcuts and application icons with the given package name, and
         * update shortcuts and application icons circle.
         *
         *
         */
        void bindCircleChanged(String packageName, boolean isDraw);

        /**
         * Bind circle shortcut information if needed, this call back is used to
         * update shortcuts when launcher first created.
         */
        void bindCircleIfNeeded();
    }
}
