package com.gome.drawbitmaplib.bitmapState;

import android.graphics.Bitmap;

import com.gome.drawbitmaplib.BitmapInfo;

/**
 * Created by admin on 2017/7/23.
 */

public class BitmapStateInstalled extends BaseBitmapSateProgress{
    public BitmapStateInstalled(BitmapInfo bitmapInfo) {
        super(bitmapInfo);
        bitmapInfo.setDescription(bitmapInfo.getAppName());
    }

    @Override
    public Bitmap decodeBitmap() {
        return null;
    }
}
