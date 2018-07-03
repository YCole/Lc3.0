package com.gome.launcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import com.gome.launcher.util.DLog;

/**
 * Created by rongwenzhao on 2017/6/27.
 * to show second level of widget list
 */

public class WidgetHorizontalScrollView extends HorizontalScrollView {

    private String TAG = "WidgetHorizontalScrollView";
    private View inner;
    private boolean shouldReturnFromLeft = false;
    private boolean shouldReturnFromRight = false;

    public WidgetHorizontalScrollView(Context context) {
        super(context);
    }

    /**
     * 未使用自定义属性时调用
     * */
    public WidgetHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            inner = getChildAt(0);
        }
    }

    private ViewChangeListener viewChangeListener;

    public interface ViewChangeListener{
        void onViewChange(WidgetHorizontalScrollView widgetHorizontalScrollView, boolean change);
    }

    public void setViewChangeListener(ViewChangeListener viewChangeListener) {
        this.viewChangeListener = viewChangeListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downState();
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (inner == null) {
            return super.onTouchEvent(ev);
        } else {
            commOnTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    public void commOnTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                /*if(isNeedMove()){
                    shouldReturn = true;
                }*/
                break;
            case MotionEvent.ACTION_UP:
                if (isNeedMove()) {
                    DLog.e(TAG,"WidgetHorizontalScrollView Math.abs(deltaX)  > 20  && isNeedMove() = true");
                    viewChangeListener.onViewChange(WidgetHorizontalScrollView.this, true);
                }
                shouldReturnFromLeft = false;
                shouldReturnFromRight = false;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            default:
                break;
        }
    }

    /**
     * Do you need to move the layout
     * @return
     */
    public boolean isNeedMove() {
        int offset = inner.getMeasuredWidth() - getWidth();
        int scrollX = getScrollX();
        if(scrollX == 0 && shouldReturnFromLeft){
            return true;
        }
        
        if(scrollX == offset && shouldReturnFromRight){
            return true;
        }
        /*if (scrollX == 0 || scrollX == offset) {
            return true;
        }*/
        return false;
    }

    /**
     * state of action DOWN
     */
    public void downState(){
        int offset = inner.getMeasuredWidth() - getWidth();
        int scrollX = getScrollX();
        if(scrollX == 0) shouldReturnFromLeft = true;
        if(scrollX == offset) shouldReturnFromRight = true;
    }
}
