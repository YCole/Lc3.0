package com.gome.launcher;

import com.gome.drawbitmaplib.IDownLoadCallback;

/**
 * Created by admin on 2017/8/1.
 */

public class DownloadAppInfo {
    private ShortcutInfo shortcutInfo;
    private IDownLoadCallback downLoadCallback;

    public DownloadAppInfo(ShortcutInfo shortcutInfo, IDownLoadCallback iDownLoadCallback) {
        this.shortcutInfo = shortcutInfo;
        this.downLoadCallback = iDownLoadCallback;
    }

    public ShortcutInfo getShortcutInfo() {
        return shortcutInfo;
    }

    public void setShortcutInfo(ShortcutInfo shortcutInfo) {
        this.shortcutInfo = shortcutInfo;
    }

    public IDownLoadCallback getDownLoadCallback() {
        return downLoadCallback;
    }

    public void setDownLoadCallback(IDownLoadCallback downLoadCallback) {
        this.downLoadCallback = downLoadCallback;
    }
}
