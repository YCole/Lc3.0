package com.gome.drawbitmaplib;


import com.gome.drawbitmaplib.bitmapState.BaseBitmapState;
import com.gome.drawbitmaplib.bitmapState.BitmapStateDownloaded;
import com.gome.drawbitmaplib.bitmapState.BitmapStateDownloading;
import com.gome.drawbitmaplib.bitmapState.BitmapStateError;
import com.gome.drawbitmaplib.bitmapState.BitmapStateIconReady;
import com.gome.drawbitmaplib.bitmapState.BitmapStateInstalled;
import com.gome.drawbitmaplib.bitmapState.BitmapStateInstalling;
import com.gome.drawbitmaplib.bitmapState.BitmapStatePaused;
import com.gome.drawbitmaplib.bitmapState.BitmapStatePreDownload;
import com.gome.drawbitmaplib.bitmapState.BitmapStatePreInstall;

/**
 * Created by admin on 2017/7/22.
 */

public class BitmapStateFactory {
    public static BitmapStateFactory sBitmapStateFactory;

    public static BitmapStateFactory getInstance() {
        if (null == sBitmapStateFactory) {
            sBitmapStateFactory = new BitmapStateFactory();
        }
        return sBitmapStateFactory;
    }

    public BaseBitmapState createBitmapState(BitmapInfo bitmapInfo) {
        if (bitmapInfo == null) {
            return null;
        }

        BaseBitmapState bitmapState = null;
        switch (bitmapInfo.getStatus()) {

            case BitmapInfo.ICON_READY:
                bitmapState = new BitmapStateIconReady(bitmapInfo);
                break;

            case BitmapInfo.PRE_DOWNLOAD:
                bitmapState = new BitmapStatePreDownload(bitmapInfo);
                break;

            case BitmapInfo.DOWNLOADING:
                bitmapState = new BitmapStateDownloading(bitmapInfo);
                break;

            case BitmapInfo.DOWNLOADED:
                bitmapState = new BitmapStateDownloaded(bitmapInfo);
                break;


            case BitmapInfo.PRE_INSTALL:
                bitmapState = new BitmapStatePreInstall(bitmapInfo);
                break;

            case BitmapInfo.INSTALLING:
                bitmapState = new BitmapStateInstalling(bitmapInfo);
                break;

            case BitmapInfo.INSTALLED:
                bitmapState = new BitmapStateInstalled(bitmapInfo);
                break;

            case BitmapInfo.DOWNLOAD_PAUSE:
                bitmapState = new BitmapStatePaused(bitmapInfo);
                break;

            case BitmapInfo.DOWNLOAD_ERROR:
            case BitmapInfo.INSTALL_ERROR:
                bitmapState = new BitmapStateError(bitmapInfo);
                break;

            default:
                break;
        }

        return bitmapState;
    }
}
