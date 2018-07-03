/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.launcher.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherAnimUtils;
import com.gome.launcher.LauncherAppState;
import com.gome.launcher.LauncherAppWidgetProviderInfo;
import com.gome.launcher.R;
import com.gome.launcher.WidgetPreviewLoader;
import com.gome.launcher.model.PackageItemInfo;
import com.gome.launcher.model.WidgetsModel;
import com.gome.launcher.util.DLog;
import com.gome.launcher.view.WidgetHorizontalScrollView;

import java.util.List;

/**
 * List view adapter for the widget tray.
 *
 * <p>Memory vs. Performance:
 * The less number of types of views are inserted into a {@link RecyclerView}, the more recycling
 * happens and less memory is consumed. {@link #getItemViewType} was not overridden as there is
 * only a single type of view.
 */
public class WidgetsListAdapterGM extends Adapter<WidgetsCellViewHolder> {

    private static final String TAG = "WidgetsListAdapterGM";
    private static final boolean DEBUG = false;

    private Launcher mLauncher;
    private LayoutInflater mLayoutInflater;

    private View mContentView;//一个包名对应多个widget，点击进入此包的widget时，使用
    private int widgetWidth;

    private WidgetsModel mWidgetsModel;
    private WidgetPreviewLoader mWidgetPreviewLoader;

    private View.OnClickListener mIconClickListener;
    private View.OnLongClickListener mIconLongClickListener;

    public WidgetsListAdapterGM(Context context,
                                View.OnClickListener iconClickListener,
                                View.OnLongClickListener iconLongClickListener,
                                Launcher launcher) {
        mLayoutInflater = LayoutInflater.from(context);

        mIconClickListener = iconClickListener;
        mIconLongClickListener = iconLongClickListener;
        mLauncher = launcher;
        widgetWidth = context.getResources().getDimensionPixelSize(R.dimen.widget_cell_width);
    }

    public void setWidgetsModel(WidgetsModel w) {
        mWidgetsModel = w;
    }

    @Override
    public int getItemCount() {
        if (mWidgetsModel == null) {
            return 0;
        }
        return mWidgetsModel.getPackageSize();
    }

    private void bindViewWithWidgetInfo(Object infoTemp, WidgetCell widget){
        if (infoTemp instanceof LauncherAppWidgetProviderInfo) {
            LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) infoTemp;
            PendingAddWidgetInfo pawi = new PendingAddWidgetInfo(mLauncher, info, null);
            widget.setTag(pawi);
            widget.applyFromAppWidgetProviderInfo(info, mWidgetPreviewLoader);
        } else if (infoTemp instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) infoTemp;
            PendingAddShortcutInfo pasi = new PendingAddShortcutInfo(info.activityInfo);
            widget.setTag(pasi);
            widget.applyFromResolveInfo(mLauncher.getPackageManager(), info, mWidgetPreviewLoader);
        }
        widget.ensurePreview();
        widget.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBindViewHolder(WidgetsCellViewHolder holder, int pos) {
        final List<Object> infoList = mWidgetsModel.getSortedWidgets(pos);
        //Object widgetInfo = mWidgetsModel.getRawList().get(pos);

        DLog.e(TAG," WidgesListAdapterGM infoList = " + infoList);

        WidgetCell widget =  (WidgetCell)holder.getContent();

        // Bind the view in the widget horizontal tray region.
        if (getWidgetPreviewLoader() == null) {
            return;
        }
        Object infoTemp = infoList.get(0);
        bindViewWithWidgetInfo(infoTemp,widget);

        if(infoList.size() > 1) {
            PackageItemInfo packageInfo = mWidgetsModel.getPackageItemInfo(pos);
            ((TextView) widget.findViewById(R.id.widget_name)).setText(packageInfo.title);
            ((TextView) widget.findViewById(R.id.widget_dims)).setText("(" + infoList.size()+ ")");
        }

        widget.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(infoList.size() == 1){
                    mIconClickListener.onClick(view);
                }else{
                    WidgetHorizontalScrollView horizontalScrollView = (WidgetHorizontalScrollView)mContentView.findViewById(R.id.widgets_scroll_container);
                    horizontalScrollView.setViewChangeListener(new WidgetHorizontalScrollView.ViewChangeListener() {
                        @Override
                        public void onViewChange(WidgetHorizontalScrollView widgetHorizontalScrollView, boolean change) {
                            if(change){
                                //widgetHorizontalScrollView.setVisibility(View.GONE);
                                // mContentView.findViewById(R.id.widgets_list_view).setVisibility(View.VISIBLE);

                                LauncherAnimUtils.fadeAlphaInOrOut(widgetHorizontalScrollView,false,300,0);
                                //LauncherAnimUtils.fadeAlphaInOrOut(mContentView.findViewById(R.id.widgets_list_view),true,300,300);
                                //bug here ,rongwenzhao need fix later 2017-7-6 begin
                                mContentView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mContentView.findViewById(R.id.widgets_list_view).setAlpha(1);
                                        mContentView.findViewById(R.id.widgets_list_view).setVisibility(View.VISIBLE);
                                    }
                                },600);
                                //bug here ,rongwenzhao need fix later 2017-7-6 end
                            }
                        }
                    });
                    LinearLayout widgetListContainer = (LinearLayout)mContentView.findViewById(R.id.widgets_cell_list);
                    if(widgetListContainer.getChildCount() > 0){
                        widgetListContainer.removeAllViews();
                    }

                    int count = infoList.size();
                    for(int i = 0; i < count; i++) {
                        WidgetCell widget = (WidgetCell) mLayoutInflater.inflate(
                                R.layout.widget_cell_horizontal, widgetListContainer, false);

                        // set up touch.
                        widget.setOnClickListener(mIconClickListener);
                        widget.setOnLongClickListener(mIconLongClickListener);
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)widget.getLayoutParams();
                        lp.width = widgetWidth;
                        if(i != 0){
                            lp.setMargins(mLauncher.getResources().getDimensionPixelSize(R.dimen.widget_cell_margin),0,0,0);
                        }
                        widget.setLayoutParams(lp);

                        widgetListContainer.addView(widget);
                        widgetListContainer.setGravity(Gravity.CENTER);

                        bindViewWithWidgetInfo(infoList.get(i),widget);
                    }
                    //horizontalScrollView.setVisibility(View.VISIBLE);
                    //mContentView.findViewById(R.id.widgets_list_view).setVisibility(View.GONE);

                    LauncherAnimUtils.fadeAlphaInOrOut(mContentView.findViewById(R.id.widgets_list_view),false,300,0);
                    LauncherAnimUtils.fadeAlphaInOrOut(horizontalScrollView,true,300,300);
                }
            }
        });
        if(infoList.size() == 1) {
            widget.setOnLongClickListener(mIconLongClickListener);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public WidgetsCellViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (DEBUG) {
            Log.v(TAG, "\nonCreateViewHolder");
        }

        WidgetCell widgetCell = (WidgetCell) mLayoutInflater.inflate(
                R.layout.widget_cell_horizontal, parent, false);
        // LinearLayout cellList = (LinearLayout) container.findViewById(R.id.widgets_cell_list);

        // if the end padding is 0, then container view (horizontal scroll view) doesn't respect
        // the end of the linear layout width + the start padding and doesn't allow scrolling.
        /*if (Utilities.ATLEAST_JB_MR1) {
            /cellList.setPaddingRelative(mIndent, 0, 1, 0);
        } else {
            //cellList.setPadding(mIndent, 0, 1, 0);
        }*/
        ViewGroup.LayoutParams lp = widgetCell.getLayoutParams();
        lp.width = widgetWidth;
        widgetCell.setLayoutParams(lp);
        return new WidgetsCellViewHolder(widgetCell);
    }

    @Override
    public void onViewRecycled(WidgetsCellViewHolder holder) {
        WidgetCell widget = (WidgetCell) holder.getContent();
        widget.setOnLongClickListener(null);//add by rongwenzhao bugfix : disable longclick in first widget list level whose package has mut items. 2017-7-6
        widget.clear();
    }

    public boolean onFailedToRecycleView(WidgetsCellViewHolder holder) {
        // If child views are animating, then the RecyclerView may choose not to recycle the view,
        // causing extraneous onCreateViewHolder() calls.  It is safe in this case to continue
        // recycling this view, and take care in onViewRecycled() to cancel any existing
        // animations.
        return true;
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    private WidgetPreviewLoader getWidgetPreviewLoader() {
        if (mWidgetPreviewLoader == null) {
            mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        }
        return mWidgetPreviewLoader;
    }

    public void setmContentView(View mContentView) {
        this.mContentView = mContentView;
    }
}
