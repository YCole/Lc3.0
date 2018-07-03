package com.gome.drawbitmaplib.bitmapState;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.gome.drawbitmaplib.BitmapInfo;

/**
 * Created by admin on 2017/7/23.
 */

public class BaseBitmapSateProgress extends BaseBitmapState {

    protected boolean mIsPaused = false;

    protected final static Point sOriginalPoint = new Point(0, 0);
    protected Rect mRendererRect;
    protected Point mViewCenterPoint;
    protected float mCircleRadius;
    protected float mCircleSchedule;
    protected float mTransLateCircleInterpolator;
    protected float mCircleInterpolator;
    protected float mTransLateCircleRadius;
    protected float mPausedCircleRadius;


    protected Canvas mCanvas;
    protected Paint mArcPaint;


    public BaseBitmapSateProgress(BitmapInfo bitmapInfo) {
        super(bitmapInfo);

        if (mRendererBitmap != null) {
            mRendererRect = bitmapInfo.getRendererRect();
            mViewCenterPoint = bitmapInfo.getCenterPoint();
            mCanvas = new Canvas(mRendererBitmap);
            mCircleInterpolator = bitmapInfo.getCircleInterpolator();
            mTransLateCircleInterpolator = bitmapInfo.getTransLateInterpolator();
            mCircleRadius = (mRendererRect.width() / 4) * mCircleInterpolator;
            mTransLateCircleRadius = (mRendererRect.width() * 1 / 3) * mTransLateCircleInterpolator;
            mCircleSchedule = -360 * ((100 - (bitmapInfo.getCircleSchedule() * 3 / 4)) / 100);
            mPausedCircleRadius = bitmapInfo.getPauseBitmap().getWidth() / 2;

            mArcPaint = new Paint();
            mArcPaint.setAntiAlias(true);
            mArcPaint.setStyle(Paint.Style.FILL);
            mArcPaint.setColor(Color.parseColor("#65373D45"));

        }

    }

    @Override
    public Bitmap decodeBitmap() {
        if (mCanvas == null) {
            return null;
        }
        int canvasWidth = mCanvas.getWidth();
        int canvasHeight = mCanvas.getHeight();
        int layerId = mCanvas.saveLayer(0, 0, canvasWidth, canvasHeight, null, Canvas.ALL_SAVE_FLAG);

        mCanvas.clipRect(0, 0, mRendererRect.width(), mRendererRect.height());

        drawMaskIcon();
        drawTransLateBigCircle();
        drawArc();
        if (mIsPaused) {
            drawTransLateSmallCircle();
            drawPauseIcon();
        }
        mCanvas.restoreToCount(layerId);
        return mRendererBitmap;
    }


    protected void drawArc() {

        RectF ovalRectF = new RectF(
                mViewCenterPoint.x - mCircleRadius,
                mViewCenterPoint.y - mCircleRadius,
                mViewCenterPoint.x + mCircleRadius,
                mViewCenterPoint.y + mCircleRadius);

        Path path = new Path();
        path.moveTo(mViewCenterPoint.x, mViewCenterPoint.y);
        path.lineTo(mViewCenterPoint.x + mCircleRadius, mViewCenterPoint.y);
        path.addArc(ovalRectF, 270, mCircleSchedule);
        path.lineTo(mViewCenterPoint.x, mViewCenterPoint.y);
        path.close();

        mCanvas.drawPath(path, mArcPaint);
    }


    protected void drawMaskIcon() {
        float maskInterpolator = 1f;
        Matrix matrix = new Matrix();
        matrix.postScale(maskInterpolator, maskInterpolator);
        if (sOriginalPoint.x + mRendererRect.width() <= mBitmapInfo.getMaskBitmap().getWidth()) {
            Bitmap reSizeBmp = Bitmap.createBitmap(mBitmapInfo.getMaskBitmap(),
                    sOriginalPoint.x,
                    sOriginalPoint.y,
                    mRendererRect.width(),
                    mRendererRect.height(), matrix, true);
            mCanvas.drawBitmap(reSizeBmp,
                    (int) (mRendererRect.width() * (1 - maskInterpolator) / 2),
                    (int) (mRendererRect.height() * (1 - maskInterpolator) / 2), mDrawPaint);
        }
    }


    protected void drawTransLateBigCircle() {
        mDrawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCanvas.drawCircle(mViewCenterPoint.x, mViewCenterPoint.y, mTransLateCircleRadius, mDrawPaint);
        mDrawPaint.setXfermode(null);
    }


    protected void drawTransLateSmallCircle() {
        mDrawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCanvas.drawCircle(mViewCenterPoint.x, mViewCenterPoint.y, mPausedCircleRadius - 0.1f, mDrawPaint);
        mDrawPaint.setXfermode(null);
    }


    protected void drawPauseIcon() {
        Bitmap pauseBitmap = mBitmapInfo.getPauseBitmap();
        mCanvas.drawBitmap(pauseBitmap,
                (mViewCenterPoint.x - mPausedCircleRadius),
                (mViewCenterPoint.y - mPausedCircleRadius), mDrawPaint);
    }




}
