package com.gome.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.Time;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Created by louhan on 2017/7/13.
 */

public class ClockScript extends IconScript {
    Rect mRect = null;
    private View mView;
    private ClockThread mClockThread = null;
    private static boolean mIsShowInScreen = false;
    private static float sStartAngle = -180.0f;
    private static float sCircleAngle = 360.0f;
    private static BitmapDrawable sDrawableClockPointerHour;
    private static BitmapDrawable sDrawableClockPointerMinute;
    private static BitmapDrawable sDrawableClockPointerSecond;

    private static enum sTimeType {hour, minute, second}


    ;
    private static int[] sColors = {Color.WHITE, Color.RED, Color.GREEN};

    public ClockScript() {
        super();
        ThemeHelper themeHelper = ThemeHelper.getInstance(LauncherApplication.getAppContext());
        sDrawableClockPointerHour = themeHelper.getClockPointerHour();
        sDrawableClockPointerMinute = themeHelper.getClockPointerMinute();
        sDrawableClockPointerSecond = themeHelper.getClockPointerSecond();
    }

    public void run(View view) {
        mView = view;
        mRect = getBounds();
        if (mClockThread == null) {
            mClockThread = new ClockThread(mView);
            mClockThread.start();
        }
    }

    @Override
    public void onPause() {
        mClockThread.pauseRun();
        super.onPause();
    }

    @Override
    public void onResume() {
        mClockThread.resumeRun();
        super.onResume();
    }

    @Override
    public void onStop() {
        if(mClockThread != null){
            mClockThread.stopRun();
            super.onStop();
            mClockThread.interrupt();
            mClockThread = null;
            mView = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mIsShowInScreen = true;

        drawIndicator(canvas, mRect.centerX(), mRect.centerY(), mPaint);
//        canvas.drawCircle(mRect.centerX(), mRect.centerY(), 3, mPaint);

        if (mClockThread != null && mClockThread.wait) {
            mClockThread.resumeRun();
        }

    }

    private float getAngle(sTimeType timeType) {
        float angle = 0;
        switch (timeType) {
            case hour:
                angle = 0;
                break;

            case minute:
                angle = 0;
                break;

            case second:
                angle = 0;
                break;

            default:
                angle = 0;
                break;
        }
        return angle;
    }

    public static float dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return  (dpValue * scale + 0.5f);
    }

    private void drawIndicator(Canvas canvas, int centerX, int centerY, Paint p) {
        Time t = new Time();
        t.setToNow();
        Context context = LauncherApplication.getAppContext();

        //modified by liuning for dynamic clock size in the moveViewRestContainer on 2017/7/25 start
        float scale = 1.0f;
        if (mFastBitmapDrawable != null && !mFastBitmapDrawable.getBitmap().isRecycled()) {
            scale = getBounds().width() * 1.0f / mFastBitmapDrawable.getBitmap().getWidth();
        }
        // Added by wjq for fix bug PRODUCTION-9407
        double hourAngle = (t.hour + t.minute / 60.0f) / 12.0f * sCircleAngle + sStartAngle;
        drawPointer(canvas,sDrawableClockPointerHour,hourAngle,centerX,centerY,scale);

        double minuteAngle = (t.minute + t.second / 60.0f) / 60.0f * sCircleAngle + sStartAngle;
        drawPointer(canvas,sDrawableClockPointerMinute,minuteAngle,centerX,centerY,scale);

        double secondAngle = t.second / 60.0f * sCircleAngle + sStartAngle;
        drawPointer(canvas,sDrawableClockPointerSecond,secondAngle,centerX,centerY,scale);

    }

    private void drawPointer(Canvas canvas, Drawable drawable, double angle, int x, int y, float scale) {
        canvas.save();
        canvas.rotate((float) angle,x,y);
        drawable.setBounds(
                (int)(x- drawable.getIntrinsicWidth() / 2 * scale),
                (int)(y - drawable.getIntrinsicHeight() / 2 * scale),
                (int)(x + drawable.getIntrinsicWidth() / 2 * scale),
                (int)(y + drawable.getIntrinsicHeight() / 2 * scale));
        drawable.draw(canvas);
        canvas.restore();
    }

    private static class ClockThread extends Thread {
        int times = 0;
        boolean running = true;
        public boolean wait = false;
        WeakReference<View> mViewReference;
        public ClockThread(View view) {
            mViewReference =  new WeakReference<View>(view);
        }

        public void stopRun() {
            running = false;
            synchronized (this) {
                this.notify();
            }
            mViewReference = null;
        }

        public void pauseRun() {
            this.wait = true;
            synchronized (this) {
                this.notify();
            }
        }

        public void resumeRun() {
            this.wait = false;
            synchronized (this) {
                this.notify();
            }
        }

        public void run() {
            while (running) {
                if (mViewReference != null && mViewReference.get() != null) {
                    synchronized (mViewReference.get()) {
                        mViewReference.get().postInvalidate();
                    }
                    if (!mIsShowInScreen) {
                        pauseRun();
                    }
                    mIsShowInScreen = false;
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        System.out.println(e);
                    }

                    synchronized (this) {
                        if (wait) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                }
            }

        }
    }
}
