package com.gome.launcher.move;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by gome on 2017/7/3.
 */

public class MoveViewsRecyclerView extends RecyclerView{

    public static final String TAG = "MoveViewsRecyclerView";
    public static final int INVALID_POSITION = -1;

    public MoveViewsRecyclerView(Context context) {
        this(context, null);
    }

    public MoveViewsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoveViewsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    /**
     * @param x
     * @param y
     */
    public int pointToPosition(int x ,int y) {
        Rect frame = new Rect();
        int mFirstPosition = getFirstVisiblePosition();

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);

                if (frame.left < x && x <= frame.right) {
                    Log.v("ln", "liuning pointToPosition mFirstPosition: " + mFirstPosition + "i:  " + i);
                    return mFirstPosition + i;
                }
            }
        }

        return INVALID_POSITION;
//        int addPosition = 0;//最终需要添加的位置
//        int lastVisiblePosition = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
//        int upPosition = x / iconWidth;//此时松手以后应该的位置
//        int allWidthItem = allWidth / iconWidth;//一屏可以存放多少icon
//        if (lastVisiblePosition >= allWidthItem) {
//            addPosition = lastVisiblePosition - allWidthItem + upPosition;
//        } else {
//            addPosition = upPosition;//此时表示所有的icon未占据一屏
//        }
//        return addPosition;
    }

    public int getFirstVisiblePosition(){
        return ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
    }

    public int getLastVisiblePosition(){
        return ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
    }

    public View getViewByPosition(int position){
        return ((LinearLayoutManager) getLayoutManager()).findViewByPosition(position);
    }

    public int getPosition(View view){
        return ((LinearLayoutManager) getLayoutManager()).getPosition(view);
    }


}
