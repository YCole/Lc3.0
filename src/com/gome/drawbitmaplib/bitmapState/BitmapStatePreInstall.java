package com.gome.drawbitmaplib.bitmapState;

import android.graphics.Bitmap;

import com.gome.drawbitmaplib.BitmapInfo;

/**
 * Created by admin on 2017/7/23.
 */

public class BitmapStatePreInstall extends BaseBitmapSateProgress {

    public BitmapStatePreInstall(BitmapInfo bitmapInfo) {
        super(bitmapInfo);
        bitmapInfo.setDescription("正在安装...");
    }

    @Override
    public Bitmap decodeBitmap() {
        mCircleSchedule = -90;
        return super.decodeBitmap();
    }
}
