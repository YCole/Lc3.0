package com.gome.launcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by rongwenzhao on 2017/7/6.
 */

public class WidgetLinearLayout extends LinearLayout {

    private boolean childClickable = true;

    public WidgetLinearLayout(Context context) {
        super(context);
    }

    public WidgetLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WidgetLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setChildClickable(boolean childClickable){
        this.childClickable = childClickable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !childClickable;
    }
}
