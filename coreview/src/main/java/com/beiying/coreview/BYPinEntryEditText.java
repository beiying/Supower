package com.beiying.coreview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * 带动画的类似屏幕解锁的输入框
 * 原理同BYAnimatedEditText
 * Created by beiying on 2016/4/7.
 */
public class BYPinEntryEditText extends EditText {
    public static final String XML_NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android";

    private String mMask = null;
    private StringBuilder mMaskChars = null;
    private int mAnimatedType = 0;
    private float mSpace = 24; //24 dp by default, space between the lines
    private float mCharSize;
    private float mNumChars = 4;
    private float mTextBottomPadding = 8; //8dp by default, height of the text from our lines
    private int mMaxLength = 4;
    private RectF[] mLineCoords;
    private float[] mCharBottom;
    private Paint mLastCharPaint;

    private OnClickListener mClickListener;
    private OnPinEnteredListener mOnPinEnteredListener = null;

    private float mLineStroke = 1; //1dp by default
    private float mLineStrokeSelected = 2; //2dp by default
    private Paint mLinesPaint;
    private boolean mAnimate = false;
    int[][] mStates = new int[][]{
            new int[]{android.R.attr.state_selected}, // selected
            new int[]{android.R.attr.state_focused}, // focused
            new int[]{-android.R.attr.state_focused}, // unfocused
    };

    int[] mColors = new int[]{
            Color.GREEN,
            Color.BLACK,
            Color.GRAY
    };

    ColorStateList mColorStates = new ColorStateList(mStates, mColors);

    public BYPinEntryEditText(Context context) {
        super(context);
    }

    public BYPinEntryEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BYPinEntryEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BYPinEntryEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        float multi = context.getResources().getDisplayMetrics().density;
        mLineStroke = multi * mLineStroke;
        mLineStrokeSelected = multi * mLineStrokeSelected;
        mSpace = multi * mSpace; //convert to pixels for our density
        mTextBottomPadding = multi * mTextBottomPadding; //convert to pixels for our density

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BYPinEntryEditText, 0, 0);
        try {
            TypedValue outValue = new TypedValue();
            ta.getValue(R.styleable.BYPinEntryEditText_pinAnimationType, outValue);
            mAnimatedType = outValue.data;
            mMask = ta.getString(R.styleable.BYPinEntryEditText_pinCharacterMask);
            mLineStroke = ta.getDimension(R.styleable.BYPinEntryEditText_pinLineStroke, mLineStroke);
            mLineStrokeSelected = ta.getDimension(R.styleable.BYPinEntryEditText_pinLineStrokeSelected, mLineStrokeSelected);
            mSpace = ta.getDimension(R.styleable.BYPinEntryEditText_pinCharacterSpacing, mSpace);
            mTextBottomPadding = ta.getDimension(R.styleable.BYPinEntryEditText_pinTextBottomPadding, mTextBottomPadding);
            ColorStateList colors = ta.getColorStateList(R.styleable.BYPinEntryEditText_pinLineColors);
            if (colors != null) {
                mColorStates = colors;
            }
        } finally {
            ta.recycle();
        }

        mLinesPaint = new Paint(getPaint());
        mLinesPaint.setStrokeWidth(mLineStroke);

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorControlActivated,
                outValue, true);
        int colorSelected = outValue.data;
        mColors[0] = colorSelected;

        int colorFocused = isInEditMode() ? Color.GRAY : Color.parseColor("#6B767E");
        mColors[1] = colorFocused;

        int colorUnfocused = isInEditMode() ? Color.GRAY : Color.parseColor("#6B767E");
        mColors[2] = colorUnfocused;

        setBackgroundResource(0);

        mMaxLength = attrs.getAttributeIntValue(XML_NAMESPACE_ANDROID, "maxLength", 4);
        mNumChars = mMaxLength;

        //Disable copy paste
        super.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }
        });
        // When tapped, move cursor to end of text.
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelection(getText().length());
                if (mClickListener != null) {
                    mClickListener.onClick(v);
                }
            }
        });

        super.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setSelection(getText().length());
                return true;
            }
        });

        //If input type is password and no mask is set, use a default mask
        if ((getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD && TextUtils.isEmpty(mMask)) {
            mMask = "\u25CF";
        } else if ((getInputType() & InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD && TextUtils.isEmpty(mMask)) {
            mMask = "\u25CF";
        }

        if (!TextUtils.isEmpty(mMask)) {
            mMaskChars = getMaskChars();
        }

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int availableWidth = getWidth() - getPaddingRight() - getPaddingLeft();
        if (mSpace < 0) {
            mCharSize = (availableWidth / (mNumChars * 2 - 1));
        } else {
            mCharSize = (availableWidth - (mSpace * (mNumChars - 1))) / mNumChars;
        }
        mLineCoords = new RectF[(int) mNumChars];
        mCharBottom = new float[(int) mNumChars];
        int startX = getPaddingLeft();
        int bottom = getHeight() - getPaddingBottom();
        for (int i = 0; i < mNumChars; i++) {
            mLineCoords[i] = new RectF(startX, bottom, startX + mCharSize, bottom);
            if (mSpace < 0) {
                startX += mCharSize * 2;
            } else {
                startX += mCharSize + mSpace;
            }
            mCharBottom[i] = mLineCoords[i].bottom - mTextBottomPadding;
        }
        mLastCharPaint = new Paint(getPaint());
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mClickListener = l;
    }

    @Override
    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
        throw new RuntimeException("setCustomSelectionActionModeCallback() not supported.");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        CharSequence text = getFullText();
        int textLength = text.length();
        float[] textWidths = new float[textLength];
        getPaint().getTextWidths(text, 0, textLength, textWidths);

        for (int i = 0; i < mNumChars; i++) {
            if (textLength > i) {
                float middle = mLineCoords[i].left + mCharSize / 2;
                if (!mAnimate || i != textLength - 1) {
                    canvas.drawText(text, i, i + 1, middle - textWidths[i] / 2, mCharBottom[i], getPaint());
                } else {
                    canvas.drawText(text, i, i + 1, middle - textWidths[i] / 2, mCharBottom[i], mLastCharPaint);
                }
            }
            updateColorForLines(i <= textLength);
            canvas.drawLine(mLineCoords[i].left, mLineCoords[i].top, mLineCoords[i].right, mLineCoords[i].bottom, mLinesPaint);
        }
    }

    private CharSequence getFullText() {
        if (mMask == null) {
            return getText();
        } else {
            return getMaskChars();
        }
    }

    private StringBuilder getMaskChars() {
        if (mMaskChars == null) {
            mMaskChars = new StringBuilder();
        }
        int textLength = getText().length();
        while (mMaskChars.length() != textLength) {
            if (mMaskChars.length() < textLength) {
                mMaskChars.append(mMask);
            } else {
                mMaskChars.deleteCharAt(mMaskChars.length() - 1);
            }
        }
        return mMaskChars;
    }


    private int getColorForState(int... states) {
        return mColorStates.getColorForState(states, Color.GRAY);
    }

    /**
     * @param hasTextOrIsNext Is the color for a character that has been typed or is
     *                        the next character to be typed?
     */
    private void updateColorForLines(boolean hasTextOrIsNext) {
        if (isFocused()) {
            mLinesPaint.setStrokeWidth(mLineStrokeSelected);
            mLinesPaint.setColor(getColorForState(android.R.attr.state_focused));
            if (hasTextOrIsNext) {
                mLinesPaint.setColor(getColorForState(android.R.attr.state_selected));
            }
        } else {
            mLinesPaint.setStrokeWidth(mLineStroke);
            mLinesPaint.setColor(getColorForState(-android.R.attr.state_focused));
        }
    }

    public void focus() {
        requestFocus();

        // Show keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(this, 0);
    }

    @Override
    protected void onTextChanged(CharSequence text, final int start, int lengthBefore, final int lengthAfter) {
        if (mLineCoords == null || !mAnimate) {
            if (mOnPinEnteredListener != null && text.length() == mMaxLength) {
                mOnPinEnteredListener.onPinEntered(text);
            }
            return;
        }

        if (mAnimatedType == -1) {
            invalidate();
            return;
        }

        if (lengthAfter > lengthBefore) {
            if (mAnimatedType == 0) {
                animatePopIn();
            } else {
                animateBottomUp(text, start);
            }
        }
    }

    private void animatePopIn() {
        ValueAnimator va = ValueAnimator.ofFloat(1, getPaint().getTextSize());
        va.setDuration(200);
        va.setInterpolator(new OvershootInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLastCharPaint.setTextSize((Float) animation.getAnimatedValue());
                BYPinEntryEditText.this.invalidate();
            }
        });
        if (getText().length() == mMaxLength && mOnPinEnteredListener != null) {
            va.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mOnPinEnteredListener.onPinEntered(getText());
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
        va.start();
    }

    private void animateBottomUp(CharSequence text, final int start) {
        mCharBottom[start] = mLineCoords[start].bottom - mTextBottomPadding;
        ValueAnimator animUp = ValueAnimator.ofFloat(mCharBottom[start] + getPaint().getTextSize(), mCharBottom[start]);
        animUp.setDuration(300);
        animUp.setInterpolator(new OvershootInterpolator());
        animUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                mCharBottom[start] = value;
                BYPinEntryEditText.this.invalidate();
            }
        });

        mLastCharPaint.setAlpha(255);
        ValueAnimator animAlpha = ValueAnimator.ofInt(0, 255);
        animAlpha.setDuration(300);
        animAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                mLastCharPaint.setAlpha(value);
            }
        });

        AnimatorSet set = new AnimatorSet();
        if (text.length() == mMaxLength && mOnPinEnteredListener != null) {
            set.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mOnPinEnteredListener.onPinEntered(getText());
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        set.playTogether(animUp, animAlpha);
        set.start();
    }

    public void setAnimateText(boolean animate) {
        mAnimate = animate;
    }

    public void setOnPinEnteredListener(OnPinEnteredListener l) {
        mOnPinEnteredListener = l;
    }

    public interface OnPinEnteredListener {
        void onPinEntered(CharSequence str);
    }
}
