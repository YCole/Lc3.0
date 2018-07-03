package com.gome.drawbitmaplib.bitmapState;

import android.graphics.Bitmap;

import com.gome.drawbitmaplib.BitmapInfo;


/**
 * Created by admin on 2017/7/22.
 */

public class BitmapStateIconReady extends BaseBitmapSateProgress {

    public BitmapStateIconReady(BitmapInfo bitmapInfo) {
        super(bitmapInfo);
        bitmapInfo.setDescription("正在载入...");
        mCircleInterpolator = 0f;
        mTransLateCircleInterpolator = 0f;
        mCircleRadius = 0f;
        mTransLateCircleRadius = 0f;

    }

    @Override
    public Bitmap decodeBitmap() {
        return super.decodeBitmap();
    }
}
