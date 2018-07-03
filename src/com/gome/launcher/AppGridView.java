package com.gome.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * Created by huanghaihao on 2017/7/12.
 */

public class AppGridView extends GridView {
    public AppGridView(Context context) {
        super(context);
    }

    public AppGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AppGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
