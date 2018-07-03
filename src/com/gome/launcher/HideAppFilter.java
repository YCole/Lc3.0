package com.gome.launcher;

import android.content.ComponentName;
import android.util.Log;


import java.util.List;

/**
 * Created by chen_chao_pc on 2017/8/7.
 */

public class HideAppFilter extends AppFilter {
    private static final String TAG = "HideAppFilter";
    private List<String> mHideAppList;
    @Override
    public void setHideAppList(List<String> list){
        mHideAppList =  list;
    }

    /**
     *  Exclude some app by local black list
     * @param app
     * @return
     */
    @Override
    public boolean shouldShowApp(ComponentName app) {
        if (mHideAppList != null && mHideAppList.contains(app.flattenToShortString())) {
            Log.i(TAG,"HideAppFilter exclude app="+app.flattenToShortString());
            return false;
        }
        return true;
    }
}
