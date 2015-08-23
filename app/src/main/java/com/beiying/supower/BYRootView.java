package com.beiying.supower;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by beiying on 15/8/22.
 */
public class BYRootView extends BYView {
    private LinearLayout mMainView;
    private BYFeatureView mFeatureView;
    public BYRootView(Context context) {
        super(context);

        initResource();
        initView(context);

        BYControlCenter.getInstrance().init(this);
    }

    private void initView(Context context) {
        mMainView = new LinearLayout(context);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mMainView.setBackgroundColor(Color.YELLOW);
        addView(mMainView, params);

        mFeatureView = new BYFeatureView(context);
        mFeatureView.setVisibility(View.GONE);
        addView(mFeatureView);
    }

    private void initResource() {
        setBackgroundColor(Color.BLUE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height= MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        for (int i = 0 ;i < getChildCount();i++) {
            View child = getChildAt(i);
            child.measure(width,height);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount();i++) {
            View child = getChildAt(i);
            child.layout(0,0,getMeasuredWidth(), getMeasuredHeight());
        }
    }

    public LinearLayout getMainView() {
        return mMainView;
    }

    public BYFeatureView getFeatureView() {
        return mFeatureView;
    }
}
