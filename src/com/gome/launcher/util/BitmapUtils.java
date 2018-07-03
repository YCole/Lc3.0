package com.gome.launcher.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by Administrator on 2017/5/31.
 * bitmap工具类
 */

public class BitmapUtils {
    private static final String TAG = "BitmapUtils";
    public static Bitmap drawColorOnBitmap(Bitmap blurBitmap, int color) {
        int outWidth = blurBitmap.getWidth();
        int outHeight = blurBitmap.getHeight();
        Bitmap foreground = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        foreground.eraseColor(color);//填充颜色
        // 创建一个新的和SRC长度宽度一样的位图
        Bitmap newBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newBitmap);
        //draw bg into
        cv.drawBitmap(blurBitmap, 0, 0, null);
        //画前景
        cv.drawBitmap(foreground, 0, 0, null);
        foreground.recycle();
        blurBitmap.recycle();
        return newBitmap;
    }
}
