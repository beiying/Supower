package com.beiying.supower;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.beiying.supower.framework.ui.BYRootView;


public class MainActivity extends ActionBarActivity {

    private FrameLayout mRoot;
    private LinearLayout mMainView;
    private BYRootView mRootView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        mRoot = (FrameLayout) findViewById(R.id.main_root);

        mMainView = new LinearLayout(this);
        Button btn1 = new Button(this);
        btn1.setText("SVG动画");
        mMainView.addView(btn1);
//        mRoot.addView(mMainView);
        mRootView = new BYRootView(this);
        setContentView(mRootView);

        initView();
    }

    private void initView() {
        LinearLayout mainView = mRootView.getMainView();
        BYButtonManager btnManager = new BYButtonManager(this);

        Button btn1 = new Button(this);
        btn1.setText("SVG动画");
        btn1.setTag(Constant.SVG);
        btn1.setOnClickListener(btnManager);
        mainView.addView(btn1);

        Button btn2 = new Button(this);
        btn2.setText("进度按钮");
        btn2.setTag(Constant.PROGRESS_BUTTON);
        btn2.setOnClickListener(btnManager);
        mainView.addView(btn2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
