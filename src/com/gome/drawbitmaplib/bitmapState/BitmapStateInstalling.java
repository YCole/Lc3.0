package com.gome.drawbitmaplib.bitmapState;

import android.graphics.Bitmap;

import com.gome.drawbitmaplib.BitmapInfo;


/**
 * Created by admin on 2017/7/23.
 */

public class BitmapStateInstalling extends BaseBitmapSateProgress {
    public BitmapStateInstalling(BitmapInfo bitmapInfo) {
        super(bitmapInfo);
        bitmapInfo.setDescription("正在安装...");
    }

    @Override
    public Bitmap decodeBitmap() {
        mCircleSchedule = -90 + mBitmapInfo.getInstallProcess() * 22.5f;
        return super.decodeBitmap();
    }
}
