package com.beiying.supower.framework.ui;

import android.content.Context;
import android.view.View;

import com.beiying.supower.BYView;

/**
 * Created by beiying on 15/8/21.
 */
public class BYFeatureView extends BYView {

    public BYFeatureView(Context context) {

        super(context);
        setWillNotDraw(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        if (getChildCount() > 0) {
            for (int i = 0;i < getChildCount();i++) {
                View child = getChildAt(i);
                child.measure(width, height);
            }
        }
        setMeasuredDimension(width,height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() > 0) {
            for (int i = 0;i < getChildCount();i++) {
                View child = getChildAt(i);
                child.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        }
    }
}
