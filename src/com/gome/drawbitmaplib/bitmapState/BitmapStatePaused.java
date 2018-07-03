package com.gome.drawbitmaplib.bitmapState;

import android.graphics.Bitmap;

import com.gome.drawbitmaplib.BitmapInfo;

/**
 * Created by admin on 2017/8/2.
 */

public class BitmapStatePaused extends BaseBitmapSateProgress {

    public BitmapStatePaused(BitmapInfo bitmapInfo) {
        super(bitmapInfo);
        bitmapInfo.setDescription("暂停");
        mIsPaused = true;
    }

    @Override
    public Bitmap decodeBitmap() {
        return super.decodeBitmap();
    }
}
