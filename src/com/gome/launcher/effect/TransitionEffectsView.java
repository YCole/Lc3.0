package com.gome.launcher.effect;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gome.launcher.Launcher;
import com.gome.launcher.LauncherApplication;
import com.gome.launcher.R;
import com.gome.launcher.common.ViewCommom;
import com.gome.launcher.settings.SettingsProvider;


/**
 * Created by linhai on 2017/6/19.
 */

public class TransitionEffectsView extends LinearLayout {

    private LinearLayout content;
    private TextView title;
    private ImageView imageView;
    private Launcher mLauncher;


    private String[] mTransitionStates;
    private TypedArray mTransitionDrawables;
    private String mCurrentState;
    private int mCurrentPosition;
    private int mPreferenceValue;

    private View[] selects;

    private  Toast mToast = null;



    public TransitionEffectsView(Context context) {
        this(context, null);
    }

    public TransitionEffectsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransitionEffectsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (context instanceof Launcher) {
            mLauncher = (Launcher) context;
        } else {
            mLauncher = LauncherApplication.getLauncher();
        }

        setWillNotDraw(false);
        setClipToPadding(false);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        content = (LinearLayout) findViewById(R.id.content);
        final String[] titles = getResources().getStringArray(
                R.array.transition_effect_entries);
        //icons = getResources().getIntArray(R.array.transition_effect_drawables2);
        mTransitionDrawables = getResources().obtainTypedArray(
                R.array.transition_effect_drawables);
        mTransitionStates = getResources().getStringArray(
                R.array.transition_effect_values);
        mPreferenceValue = R.string.preferences_interface_homescreen_scrolling_transition_effect;
        mCurrentState = SettingsProvider.getString(mLauncher,
                SettingsProvider.SETTINGS_UI_HOMESCREEN_SCROLLING_TRANSITION_EFFECT, mPreferenceValue);

        mCurrentPosition = mapEffectToPosition(mCurrentState);
        if (mCurrentPosition < 0) {
            mCurrentPosition = 0;
        }

        int count = titles.length;
        selects = new View[count];
        content.removeAllViews();
        for (int i = 0; i < count; i++) {
            View v = LayoutInflater.from(mLauncher).inflate(R.layout.transition_effect_view_item, null);
            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            View select = v.findViewById(R.id.select);
            TextView title = (TextView) v.findViewById(R.id.title);
            selects[i] = select;
            title.setText(titles[i]);
            icon.setImageResource(mTransitionDrawables
                    .getResourceId(i, R.drawable.scroll_type_default));
            select.setVisibility(mCurrentPosition == i ? View.VISIBLE : View.GONE);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            //linhai del 自适应布局 start 2017/7/27
//            if (i == 0) {
//                llp.setMargins(getResources().getDimensionPixelSize(R.dimen.overview_panel_effects_margin__item_left),0, getResources().getDimensionPixelSize(R.dimen.overview_panel_effects_margin__item_right), 0);
//            } else {
//                llp.setMargins(0, 0, getResources().getDimensionPixelSize(R.dimen.overview_panel_effects_margin__item_right), 0);
//            }
            //linhai del 自适应布局 end 2017/7/27

            content.addView(v, llp);
            v.setTag(i);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewCommom.setEnable(v, 500);
                    if (!mLauncher.getWorkspace().isPageMoving())
                    {
                        int pos = (int) v.getTag();
                        mCurrentPosition = pos;
                        mCurrentState = mTransitionStates[mCurrentPosition];

                        setSelect(pos);
                        setTransitionEffect(mCurrentState);
                    }else
                    {
                        showToast(mLauncher.getApplicationContext(),mLauncher.getResources().getString(R.string.user_wait_tips),
                                Toast.LENGTH_SHORT);
                    }
                }
            });
        }
    }

    public  void showToast(Context context, String text, int duration) {
        if (mToast == null) {
            mToast = Toast.makeText(context, text, duration);
        } else {
            mToast.setText(text);
            mToast.setDuration(duration);
        }
        mToast.show();
    }

    private void setSelect(int position) {
        for (int i = 0; i < selects.length; i++) {
            if (position < selects.length && selects[i] != null) {
                if (i == position) {
                    selects[i].setVisibility(View.VISIBLE);
                } else {
                    selects[i].setVisibility(View.GONE);
                }
            }
        }
    }

    public void setTransitionEffect(String newTransitionEffect) {
        SettingsProvider.get(mLauncher).edit()
                .putString(SettingsProvider.SETTINGS_UI_HOMESCREEN_SCROLLING_TRANSITION_EFFECT,
                        newTransitionEffect).commit();
        mLauncher.getWorkspace().resetTransitionEffect();
        mLauncher.getWorkspace().switchingEffectModel();
    }

    private int mapEffectToPosition(String effect) {
        int length = mTransitionStates.length;
        for (int i = 0; i < length; i++) {
            if (effect.equals(mTransitionStates[i])) {
                return i;
            }
        }
        return -1;
    }

}
