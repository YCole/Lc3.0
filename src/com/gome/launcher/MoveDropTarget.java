package com.gome.launcher;

import android.content.Context;
import android.util.AttributeSet;


/**
 * Created by liuning on 2017/7/18.
 * for multi apps move
 */

public class MoveDropTarget extends ButtonDropTarget {

    public MoveDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoveDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Get the hover color
        mHoverColor = getResources().getColor(R.color.move_target_hover_tint);

        setDrawable(R.drawable.gome_move_launcher);
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        return (info instanceof ShortcutInfo) || (info instanceof FolderInfo);
    }

    @Override
    public void onDrop(DragObject d) {
        if (d.dragSource instanceof MoveSource) {
            ((MoveSource) d.dragSource).onDropToMoveTarget();
        }
        super.onDrop(d);
    }

    @Override
    void completeDrop(DragObject d) {
        mLauncher.enterAppManageMode();
        mLauncher.getMoveViewsRestContainer().addItemFirst((ItemInfo) (d.dragInfo));
    }

    /**
     * Interface defining an object that can provide moveable drag objects.
     */
    public static interface MoveSource {

        /**
         * Indicates that drop the drag object into the move target
         */
        void onDropToMoveTarget();
    }

}
