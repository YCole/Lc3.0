package com.gome.launcher;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.gome.launcher.util.DisplayMetricsUtils;

/**
 * Created by admin on 2017/6/28.
 */

public class WallpaperDialog extends DialogFragment {

    private ViewGroup mLabelContent;
    private ItemSelectListener mItemSelectListener;
    private DialogInterface.OnCancelListener mOnCancelListener;

    public WallpaperDialog() {
    }

    @SuppressLint("ValidFragment")
    public WallpaperDialog(ItemSelectListener itemSelectListener) {
        mItemSelectListener = itemSelectListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        mOnCancelListener = onCancelListener;
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mOnCancelListener = null;
        mItemSelectListener = null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (null != mOnCancelListener) {
            mOnCancelListener.onCancel(dialog);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        DisplayMetricsUtils.init(getActivity());
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.BOTTOM;
        params.width = DisplayMetricsUtils.getDeviceWidth();
        params.dimAmount = 0f;
        params.windowAnimations = R.style.dialogAnim;
        window.setAttributes(params);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View parent  =  inflater.inflate(R.layout.dialog_fragment, container, false);
        initViews(parent);
        return parent;
    }

    private void initViews(View parent) {
        mLabelContent = (ViewGroup) parent.findViewById(R.id.label_content);
        TextView labelHomeScreen = (TextView) parent.findViewById(R.id.label_home_screen);
        TextView labelLockScreen = (TextView) parent.findViewById(R.id.label_lock_screen);
        TextView labelBothScreen = (TextView) parent.findViewById(R.id.label_both_screen);
        labelLockScreen.setTag(0);
        labelHomeScreen.setTag(1);
        labelBothScreen.setTag(2);
        labelHomeScreen.setOnClickListener(mOnClickListener);
        labelLockScreen.setOnClickListener(mOnClickListener);
        labelBothScreen.setOnClickListener(mOnClickListener);
    }


    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.label_home_screen:
                case R.id.label_lock_screen:
                case R.id.label_both_screen:
                    mLabelContent.setVisibility(View.GONE);
                    if (null != mItemSelectListener)
                        mItemSelectListener.itemSelected((Integer) v.getTag());
                    break;
                default:
                    break;
            }
        }
    };
}

interface ItemSelectListener {
    void itemSelected(int selectedItemIndex);
}