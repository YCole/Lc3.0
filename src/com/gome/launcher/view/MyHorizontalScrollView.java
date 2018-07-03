package com.gome.launcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.HorizontalScrollView;

/**
 * Created by linhai on 2017/6/19.
 */
public class MyHorizontalScrollView extends HorizontalScrollView {

    // Drag distance size = 4 That is to say, only to drag the screen. 1/4
    private static final float size = 1.0f;
    private View inner;
    private float x;
    private Rect normal = new Rect();

    private boolean isOpenReboundEffect = true;

    public MyHorizontalScrollView(Context context) {
        super(context);
    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            inner = getChildAt(0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (inner == null || !isOpenReboundEffect) {
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
                x = ev.getX();
                break;
            case MotionEvent.ACTION_UP:
                if (isNeedAnimation()) {
                    animation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final float preX = x;
                float nowX = ev.getX();
                /**
                 * size=4 Drag the distance to represent the height of the screen 1/4
                 */
                int deltaX = (int) (1.0f * (preX - nowX) / size);
                // roll
                //scrollBy(0, deltaX);

                x = nowX;
                // When scroll to the top or bottom when it will
                // not be rolling, then move the layout
                if (isNeedMove()) {
                    if (normal.isEmpty()) {
                        // Save the normal layout
                        normal.set(inner.getLeft(), inner.getTop(),
                                inner.getRight(), inner.getBottom());
                        return;
                    }
                    int xx = inner.getLeft() - deltaX;

                    // move layout
                    inner.layout(xx, inner.getTop(), inner.getRight() - deltaX,
                            inner.getBottom());

                }
                break;
            default:
                break;
        }
    }

    /**
     * Open animation movement
     */
    public void animation() {

        TranslateAnimation ta = new TranslateAnimation(inner.getLeft(), normal.top, 0, 0);
        ta.setDuration(200);
        inner.setAnimation(ta);
        ta.startNow();
        // Set back to normal layout
        inner.layout(normal.left, normal.top, normal.right, normal.bottom);
        normal.setEmpty();
    }


    /**
     * Do you need to turn on the animation
     *
     * @return
     */
    public boolean isNeedAnimation() {
        return !normal.isEmpty();
    }

    /**
     * Do you need to move the layout
     *
     * @return
     */
    public boolean isNeedMove() {
        int offset = inner.getMeasuredWidth() - getWidth();
        int scrollX = getScrollX();
        if (scrollX == 0 || scrollX == offset) {
            return true;
        }
        return false;
    }

}
