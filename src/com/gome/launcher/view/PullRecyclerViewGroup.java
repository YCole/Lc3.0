package com.gome.launcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import com.gome.launcher.util.DLog;

/**
 * Created by rongwenzhao on 2017/6/29.
 */

public class PullRecyclerViewGroup extends LinearLayout {
    /**
     * 滚动时间
     */
    private static final long ANIM_TIME = 200;

    //listview 或者recyclerview或者ScrollView
    private View childView;

    // 用于记录正常的布局位置
    private Rect originalRect = new Rect();

    // 在手指滑动的过程中记录是否移动了布局
    private boolean isMoved = false;

    // 如果按下时不能左拉和右拉， 会在手指移动时更新为当前手指的X值
    private float startX;

    //阻尼
    private static final float OFFSET_RADIO = 0.5f;
    //阈值 added by liuning on 2017/8/9
    private static final int OFFSET_VALUE = 5;

    private boolean isRecyclerReuslt = false;

    private String TAG = "PullRecyclerViewGroup";


    public PullRecyclerViewGroup(Context context) {
        this(context, null);
    }

    public PullRecyclerViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRecyclerViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //关闭滚动条
        this.setHorizontalScrollBarEnabled(false);
    }

    /**
     * 加载布局后初始化,这个方法会在加载完布局后调用
     */
    @Override
    protected void onFinishInflate() {
        //此处为容器中的子view   必须有RecyclerView、ListView、ScrollView，当然这里忽略ListView和ScrollView
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                if (getChildAt(i) instanceof RecyclerView || getChildAt(i) instanceof ListView || getChildAt(i) instanceof ScrollView) {
                    if (childView == null) {
                        childView = getChildAt(i);
                    } else {
                        throw new RuntimeException("PullRecyclerViewGroup 中只能存在一个RecyclerView、ListView或者ScrollView");
                    }
                }
            }
        }
        if (childView == null) {
            throw new RuntimeException("PullRecyclerViewGroup 子容器中必须有一个RecyclerView、ListView或者ScrollView");
        }
        super.onFinishInflate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        //ScrollView中唯一的子控件的位置信息，这个位置在整个控件的生命周期中保持不变
        originalRect.set(childView.getLeft(), childView.getTop(), childView.getRight(), childView.getBottom());
    }

    /**
     * 事件分发
     */

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                float nowX = ev.getX();
                int scrollX = (int) (nowX - startX);
                DLog.d(TAG,"onTouchEvent onMove == isCanPullLeft() = " + isCanPullLeft() + " scrollX = " + scrollX );
                if ((isCanPullRight() && scrollX > OFFSET_VALUE) || (isCanPullLeft() && scrollX < -OFFSET_VALUE)) {
                    int offset = (int) (scrollX * OFFSET_RADIO);
                    childView.layout(originalRect.left + offset, originalRect.top, originalRect.right + offset, originalRect.bottom);
                    isMoved = true;
                    isRecyclerReuslt = false;
                }else{
                    startX = ev.getX();
                    isMoved = false;
                    isRecyclerReuslt = true;
                    childView.layout(originalRect.left, originalRect.top, originalRect.right, originalRect.bottom);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isMoved) {
                    recoverLayout();
                }
                break;
        }
        return true;
    }

    /**
     * 根据是否左拉，右拉，进行事件拦截处理
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (childView == null) {
            return super.onInterceptTouchEvent(ev);
        }

        boolean isTouchOutOfScrollView = ev.getX() >= originalRect.right || ev.getX() <= originalRect.left; //如果当前view的X上的位置
        if (isTouchOutOfScrollView) {//如果不在view的范围内
            if (isMoved) {      //当前容器已经被移动
                recoverLayout();
            }
            return true;
        }

        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //记录按下时的X
                startX = ev.getX();
            case MotionEvent.ACTION_MOVE:
                float nowX = ev.getX();
                int scrollX = (int) (nowX - startX);
                DLog.d(TAG,"onInterceptTouchEvent onMove == isCanPullLeft() = " + isCanPullLeft() + " scrollX = " + scrollX );
                if ((isCanPullRight() && scrollX > OFFSET_VALUE) || (isCanPullLeft() && scrollX < -OFFSET_VALUE)) {
                    isMoved = true;
                    isRecyclerReuslt = false;
                    return true;
                } else {
                    startX = ev.getX();
                    isMoved = false;
                    isRecyclerReuslt = true;
                    //recoverLayout();
                    return super.onInterceptTouchEvent(ev);
                }
            case MotionEvent.ACTION_UP:
                if (isRecyclerReuslt) {
                    return super.onInterceptTouchEvent(ev);
                } else {
                    return true;
                }
            default:
                return true;
        }
    }

    /**
     * 位置还原
     */
    private void recoverLayout() {
        if (!isMoved) {
            return;//如果没有移动布局，则跳过执行
        }
        TranslateAnimation anim = new TranslateAnimation(childView.getLeft() - originalRect.left, 0, 0, 0);
        anim.setDuration(ANIM_TIME);
        childView.startAnimation(anim);
        childView.layout(originalRect.left, originalRect.top, originalRect.right, originalRect.bottom);
        isMoved = false;
    }

    /**
     * 判断是否可以右拉
     * @return
     */
    private boolean isCanPullRight() {

        final RecyclerView.Adapter adapter = ((RecyclerView) childView).getAdapter();
        if (null == adapter) {
            return true;
        }
        final int firstVisiblePosition = ((LinearLayoutManager) ((RecyclerView) childView).getLayoutManager()).findFirstVisibleItemPosition();
        if (firstVisiblePosition != 0 && adapter.getItemCount() != 0) {
            return false;
        }
        int mostLeft = (((RecyclerView) childView).getChildCount() > 0) ? ((RecyclerView) childView).getChildAt(0).getLeft() : 0;
        return mostLeft >= 0;
    }

    /**
     * 判断是否可以左拉
     * @return
     */
    private boolean isCanPullLeft() {
        final RecyclerView.Adapter adapter = ((RecyclerView) childView).getAdapter();

        if (null == adapter) {
            return true;
        }

        final int lastItemPosition = adapter.getItemCount() - 1;
        //final int lastVisiblePosition = ((LinearLayoutManager) ((RecyclerView) childView).getLayoutManager()).findLastVisibleItemPosition();
        final int lastCompleteVisiblePosition = ((LinearLayoutManager) ((RecyclerView) childView).getLayoutManager()).findLastCompletelyVisibleItemPosition();
        final View lastChild = ((RecyclerView) childView).getChildAt(lastItemPosition);
        if(lastChild != null) {
            DLog.d(TAG, "================lastItemPosition = " + lastItemPosition + "lastCompleteVisiblePosition = " + lastCompleteVisiblePosition + "lastView Right = "
                    + lastChild.getRight() + " && childView getRight = " + (childView.getRight()) + " && childView getLeft = " + childView.getLeft());
        }
        if (lastCompleteVisiblePosition >= lastItemPosition) {
            final int childIndex = lastCompleteVisiblePosition - ((LinearLayoutManager) ((RecyclerView) childView).getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            final int childCount = ((RecyclerView) childView).getChildCount();
            final int index = Math.min(childIndex, childCount - 1);
            final View lastVisibleChild = ((RecyclerView) childView).getChildAt(index);
            DLog.d(TAG,"lastVisibleChild = " + lastVisibleChild);
            if (lastVisibleChild != null) {
                DLog.d(TAG,"lastVisibleChild.getRight() = " + lastVisibleChild.getRight() + "childView.getRight() - childView.getLeft() = "
                        + (childView.getRight() - childView.getLeft()));
                return lastVisibleChild.getRight() <= childView.getRight() - childView.getLeft();
            }
        }
        return false;
    }
}