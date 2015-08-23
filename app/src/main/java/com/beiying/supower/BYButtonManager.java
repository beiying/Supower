package com.beiying.supower;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import com.beiying.coreview.BYPathView;


/**
 * Created by beiying on 15/8/22.
 */
public class BYButtonManager implements View.OnClickListener{
    private Context mContext;

    public BYButtonManager(Context context) {
        mContext = context;
    }
    @Override
    public void onClick(View view) {
        String tag = (String) view.getTag();
        switch (tag) {
            case Constant.SVG:
                BYPathView pathView = new BYPathView(mContext);
                pathView.setSvgResource(R.raw.monitor);
                pathView.setFillAfter(true);
                pathView.useNaturalColors();
                pathView.setContentDescription("PathView");
                pathView.setBackgroundColor(Color.GREEN);
                BYControlCenter.getInstrance().showFeatureView(pathView);
                break;
        }
    }
}
