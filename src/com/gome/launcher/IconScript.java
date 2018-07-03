package com.gome.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Created by louhan on 2017/7/13.
 */

public class IconScript extends Drawable {
    public boolean isRuning = false;
    public FastBitmapDrawable mFastBitmapDrawable = null;
    protected Paint mPaint = new Paint();

    public IconScript() {
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
    }

    public void draw(Canvas canvas) {
        if (mFastBitmapDrawable != null) {
            canvas.drawBitmap(mFastBitmapDrawable.getBitmap(), null, getBounds(), mPaint);//画底图
        }
    }

    public void run(View view) {
        isRuning = true;
    }

    public void onStop() {
        isRuning = false;
    }

    public void onPause() {
        isRuning = false;
    }

    public void onResume() {
        isRuning = true;
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public void setAlpha(int arg0) {

    }

    @Override
    public void setColorFilter(ColorFilter arg0) {

    }

    @Override
    public int getIntrinsicWidth() {
        int width = getBounds().width();
        if (width == 0) {
            width = mFastBitmapDrawable.getBitmap().getWidth();
        }
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        int height = getBounds().height();
        if (height == 0) {
            height = mFastBitmapDrawable.getBitmap().getHeight();
        }
        return height;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public int getMinimumHeight() {
        return getBounds().height();
    }

    @Override
    public int getMinimumWidth() {
        return getBounds().width();
    }

    @Override
    public void setFilterBitmap(boolean filterBitmap) {
        mPaint.setFilterBitmap(filterBitmap);
        mPaint.setAntiAlias(filterBitmap);
    }

    public void setFastBitmapDrawable(FastBitmapDrawable drawable) {
        mFastBitmapDrawable = drawable;
    }

    public FastBitmapDrawable getFastBitmapDrawable() {
        return mFastBitmapDrawable;
    }
}
