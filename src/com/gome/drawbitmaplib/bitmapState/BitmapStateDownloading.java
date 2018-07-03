package com.gome.drawbitmaplib.bitmapState;

import com.gome.drawbitmaplib.BitmapInfo;

/**
 * Created by admin on 2017/7/23.
 */

public class BitmapStateDownloading extends BaseBitmapSateProgress {
    public BitmapStateDownloading(BitmapInfo bitmapInfo) {
        super(bitmapInfo);
        bitmapInfo.setDescription("正在载入...");
    }
}
