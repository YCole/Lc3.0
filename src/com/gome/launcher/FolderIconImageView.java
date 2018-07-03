package com.gome.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Administrator on 2017/6/20.
 * create iconImageView for icons in the FolderIcon
 */
public class FolderIconImageView extends ImageView{

    private int mIntrinsicIconSize;

    private Rect mOldBounds = new Rect();

    private PreviewItemDrawingParams mParams = new PreviewItemDrawingParams(0, 0, 0, 0);

    private PaintFlagsDrawFilter mSetfil = new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG);

    public FolderIconImageView(Context context) {
        super(context);
    }

    public FolderIconImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderIconImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDrawable(Drawable mDrawable, DeviceProfile grid, boolean isMoveFolderIcon) {
        computePreviewItemDrawingParams(mParams, grid);
        mParams.drawable = mDrawable;
        mIntrinsicIconSize = isMoveFolderIcon ? grid.moveIconSizePx : grid.folderIconSizePx;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawPreviewItem(canvas,mParams);
    }


    class PreviewItemDrawingParams {
        PreviewItemDrawingParams(float transX, float transY, float scale, int overlayAlpha) {
            this.transX = transX;
            this.transY = transY;
            this.scale = scale;
            this.overlayAlpha = overlayAlpha;
        }
        float transX;
        float transY;
        float scale;
        int overlayAlpha;
        Drawable drawable;
    }

    private void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams params) {
        canvas.save();
        canvas.setDrawFilter(mSetfil);
        canvas.translate(params.transX, params.transY);
        canvas.scale(params.scale, params.scale);
        Drawable d = params.drawable;
        if (d != null) {
            d.setFilterBitmap(true);
            mOldBounds.set(d.getBounds());
            d.setBounds(0, 0, mIntrinsicIconSize, mIntrinsicIconSize);
            if (d instanceof FastBitmapDrawable) {
                FastBitmapDrawable fd = (FastBitmapDrawable) d;
                float oldBrightness = fd.getBrightness();
                fd.setBrightness(params.overlayAlpha);
                d.draw(canvas);
                fd.setBrightness(oldBrightness);
            } else {
                d.setColorFilter(Color.argb(params.overlayAlpha, 255, 255, 255),
                        PorterDuff.Mode.SRC_ATOP);
                d.draw(canvas);
                d.clearColorFilter();
            }
            d.setBounds(mOldBounds);
        }
        canvas.restore();
    }

    private void computePreviewItemDrawingParams(PreviewItemDrawingParams params,DeviceProfile grid) {
        final int overlayAlpha = 1;
        params.transX = getResources().getDimensionPixelSize(R.dimen.folder_icon_image_view_transX);
        params.transY = getResources().getDimensionPixelSize(R.dimen.folder_icon_image_view_transY);
        params.scale = grid.mFolderIconImageShrinkFactor;
        params.overlayAlpha = overlayAlpha;
    }
}
