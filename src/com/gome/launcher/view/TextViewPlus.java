package com.gome.launcher.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import com.gome.launcher.R;

/**
 * Created by chenchao on 2017/7/20.
 */

public class TextViewPlus extends TextView {
    private int topHeight = -1;
    private int topWidth = -1;

    public TextViewPlus(Context context) {
        super(context);
    }

    public TextViewPlus(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public TextViewPlus(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.TextViewPlus, defStyle, 0);
        if (a != null) {
            int count = a.getIndexCount();
            int index = 0;
            for (int i = 0; i < count; i++) {
                index = a.getIndex(i);
                switch (index) {
                    case R.styleable.TextViewPlus_top_height:
                        topHeight = a.getDimensionPixelSize(index, -1);
                        break;
                    case R.styleable.TextViewPlus_top_width:
                        topWidth = a.getDimensionPixelSize(index, -1);
                        break;
                }
            }

            Drawable[] drawables = getCompoundDrawables();
            int dir = 0;
            // 0-left; 1-top; 2-right; 3-bottom;
            for (Drawable drawable : drawables) {
                setImageSize(drawable);
            }
            // 将图片放回到TextView中
            setCompoundDrawables(drawables[0], drawables[1], drawables[2],
                    drawables[3]);

        }

    }

    private void setImageSize(Drawable d) {
        if (d == null) {
            return;
        }

        int height = topHeight;
        int width = topWidth;
        d.setBounds(0, 0, width, height);
    }
}
