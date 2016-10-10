package com.beiying.supower;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import com.beiying.coreview.BYPathView;
import com.beiying.coreview.BYProgressButton;
import com.beiying.supower.framework.ui.BYControlCenter;


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
            case Constant.PROGRESS_BUTTON:
                final BYProgressButton progressButton = new BYProgressButton(mContext);
                progressButton.setCurrentText("下载中 ");
                progressButton.setBackgroundColor(Color.BLUE);
                progressButton.setBackgroundSecondColor(Color.GREEN);
                progressButton.setState(BYProgressButton.PROGRESSING);
                progressButton.setTextColor(Color.BLUE);
                progressButton.setInterval(0.1f);
                progressButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        progressButton.setToProgress(progressButton.getToProgress() + 8f);
                    }
                });
                BYControlCenter.getInstrance().showFeatureView(progressButton);
                break;
        }
    }
}
