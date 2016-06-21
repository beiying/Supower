package com.beiying.coreview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by beiying on 2015/10/28.
 */
public class BYProgressButton extends TextView {
    public static final int PROGRESSED = 1;//进度完成
    public static final int PROGRESSING = 2;//进度进行中

    private RectF mBackgroundBounds;
    private float mButtonRadius;
    private LinearGradient mProgressBgGradient;
    private LinearGradient mProgressTextGradient;
    private Paint mBackgroundPaint;
    private Paint mTextPaint;
    private int mBackgroundColor;
    private int mBackgroundSecondColor;
    private float mProgressPercent;
    private int mTextColor;
    private int mTextCoverColor;

    private String mCurrentText;
    private int mState;
    private int mMaxProgress;
    private int mMinProgress;
    private float mProgress;
    private float mToProgress;
    private float mInterval;

    public BYProgressButton(Context context) {
        this(context, null);
    }

    public BYProgressButton(Context context,AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            initAttrs(context, attrs);
            init();
        }
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BYProgressButton);
        mBackgroundColor = a.getColor(R.styleable.BYProgressButton_progressbtn_backgroud_color, Color.parseColor("#6699ff"));
        mBackgroundSecondColor = a.getColor(R.styleable.BYProgressButton_progressbtn_backgroud_second_color, Color.LTGRAY);
        mButtonRadius = a.getFloat(R.styleable.BYProgressButton_progressbtn_radius, getMeasuredHeight() / 2);
        mTextColor = a.getColor(R.styleable.BYProgressButton_progressbtn_text_color, mBackgroundColor);
        mTextCoverColor = a.getColor(R.styleable.BYProgressButton_progressbtn_text_covercolor, Color.WHITE);
        a.recycle();
    }

    private void init() {
        mMaxProgress = 100;
        mMinProgress = 0;
        mProgress = 0;

        mInterval = 1;

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(50f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //解决文字有时候画不出问题
            setLayerType(LAYER_TYPE_SOFTWARE, mTextPaint);
        }

        //初始化状态设为NORMAL
        mState = PROGRESSING;
        invalidate();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = 100;
        setMeasuredDimension(width, height);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInEditMode()) {
            drawing(canvas);
        }
    }

    private void drawing(Canvas canvas) {
        drawBackground(canvas);
        drawTextAbove(canvas);
    }

    private void drawTextAbove(Canvas canvas) {
        final float y = getMeasuredHeight() / 2 - (mTextPaint.descent() / 2 + mTextPaint.ascent() / 2);

        if (TextUtils.isEmpty(mCurrentText)) {
            mCurrentText = "";
        }

        final float textWidth = mTextPaint.measureText(mCurrentText.toString());
        switch (mState) {
            case PROGRESSED:
                mTextPaint.setShader(null);
                mTextPaint.setColor(mTextCoverColor);
                break;
            case PROGRESSING:
                float coverlength = getMeasuredWidth() * mProgressPercent;//进度条压过的距离
                float indicator1 = getMeasuredWidth() / 2 - textWidth / 2;//文字显示的左边界
                float indicator2 = getMeasuredWidth() / 2 + textWidth / 2;//文字显示的右边界
                float coverTextLength = textWidth / 2 - getMeasuredWidth() / 2 + coverlength;//文字变色部分的距离
                float textProgress = coverTextLength / textWidth;
                if(coverlength < indicator1) {
                    mTextPaint.setShader(null);
                    mTextPaint.setColor(mTextColor);
                } else if(coverlength >= indicator1 && coverlength <= indicator2) {
                    mProgressTextGradient = new LinearGradient((getMeasuredWidth() - textWidth) / 2, 0, (getMeasuredWidth() + textWidth) / 2, 0, new int[] {mTextCoverColor, mTextColor}, new float[] {textProgress, textProgress + 0.001f}, Shader.TileMode.CLAMP);
                    mTextPaint.setShader(mProgressTextGradient);
                    mTextPaint.setColor(mTextColor);
                } else {
                    mTextPaint.setShader(null);
                    mTextPaint.setColor(mTextCoverColor);
                }
                break;
        }

        canvas.drawText(mCurrentText.toString(), (getMeasuredWidth() - textWidth) / 2, y, mTextPaint);

    }

    private void drawBackground(Canvas canvas) {
        mBackgroundBounds = new RectF();

        if (mButtonRadius == 0) {
            mButtonRadius = getMeasuredHeight() / 2;
        }

        mBackgroundBounds.left = 2;
        mBackgroundBounds.top = 2;
        mBackgroundBounds.right = getMeasuredWidth() - 2;
        mBackgroundBounds.bottom = getMeasuredHeight() - 2;

        switch (mState) {
            case PROGRESSED:
                mBackgroundPaint.setShader(null);
                mBackgroundPaint.setColor(mBackgroundColor);
                break;
            case PROGRESSING:
                mProgressPercent = mProgress / (mMaxProgress + 0f);
                mProgressBgGradient = new LinearGradient(0, 0, getMeasuredWidth(), 0, new int[] {mBackgroundColor, mBackgroundSecondColor}, new float[] {mProgressPercent, mProgressPercent + 0.001f}, Shader.TileMode.CLAMP);
                mBackgroundPaint.setColor(mBackgroundColor);
                mBackgroundPaint.setShader(mProgressBgGradient);
                break;
        }
        canvas.drawRoundRect(mBackgroundBounds, mButtonRadius, mButtonRadius, mBackgroundPaint);
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        if (mState != state) {//状态确实有改变
            this.mState = state;
            invalidate();
        }

    }

    public float getToProgress() {
        return mToProgress;
    }

    public void setToProgress(float toProgress) {
        mToProgress = toProgress;
        if (mToProgress >= mMaxProgress) {
            mToProgress = mMaxProgress;
        }
        int steps = (int)((mToProgress - mProgress) / mInterval);
        for (int i = 0;i < steps;i++) {
            mProgress += mInterval;
            mCurrentText = "下载进度为" + mProgress;
            invalidate();
        }
    }

    public float getInterval() {
        return mInterval;
    }

    public void setInterval(float interval) {
        mInterval = interval;
    }

    /**
     * 设置按钮文字
     */
    public void setCurrentText(String text) {
        mCurrentText = text;
        invalidate();
    }


    /**
     * 设置带下载进度的文字
     */
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    public void setProgressText(String text, float progress) {
//        if (progress >= mMinProgress && progress <= mMaxProgress) {
//            mCurrentText = text + getResources().getString(R.string.downloaded, (int) progress);
//            mToProgress = progress;
//            if (mProgressAnimation.isRunning()) {
//                mProgressAnimation.resume();
//                mProgressAnimation.start();
//            } else {
//                mProgressAnimation.start();
//            }
//        } else if (progress < mMinProgress) {
//            mProgress = 0;
//        } else if (progress > mMaxProgress) {
//            mProgress = 100;
//            mCurrentText = text + getResources().getString(R.string.downloaded, (int) mProgress);
//            invalidate();
//        }
//    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        this.mProgress = progress;

    }

    public float getButtonRadius() {
        return mButtonRadius;
    }

    public void setButtonRadius(float buttonRadius) {
        mButtonRadius = buttonRadius;
    }

    public int getTextColor() {
        return mTextColor;
    }

    @Override
    public void setTextColor(int textColor) {
        mTextColor = textColor;
    }

    public int getBackgroundColor() { return mBackgroundColor; }

    public void setBackgroundColor(int backgroundColor) { mBackgroundColor = backgroundColor; }

    public int getBackgroundSecondColor() { return mBackgroundSecondColor; }

    public void setBackgroundSecondColor(int backgroundColor) { mBackgroundSecondColor = backgroundColor; }

    public int getTextCoverColor() {
        return mTextCoverColor;
    }

    public void setTextCoverColor(int textCoverColor) {
        mTextCoverColor = textCoverColor;
    }

    public int getMinProgress() {
        return mMinProgress;
    }

    public void setMinProgress(int minProgress) {
        mMinProgress = minProgress;
    }

    public int getMaxProgress() {
        return mMaxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        mMaxProgress = maxProgress;
    }
}
