package com.beiying.supower.framework.ui;

import android.view.View;

/**
 * Created by beiying on 15/8/22.
 */
public class BYControlCenter {
    private static BYControlCenter sIntance;
    private BYRootView mRootView;

    private BYControlCenter() {

    }
    public static BYControlCenter getInstrance() {
        if (sIntance == null) {
            sIntance = new BYControlCenter();
        }
        return sIntance;
    }

    public void init(BYRootView rootView) {
        mRootView = rootView;
    }

    public BYRootView getRootView() {
        return mRootView;
    }

    public void showFeatureView(View view) {
        if (mRootView != null) {
            mRootView.getFeatureView().setVisibility(View.VISIBLE);
            mRootView.getFeatureView().addView(view);
        }
    }
}
