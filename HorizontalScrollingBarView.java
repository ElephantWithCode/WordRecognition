package com.example.wordrecognition;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ellly on 2017/11/30.
 */

interface OnDrawTextListener{
    void onDrawText(Canvas canvas, int tabCount);
}

public class HorizontalScrollingBarView extends View{

    private OnTouchListener mScrollListener = new OnTouchListener() {
        float mDownX = 0;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    mDownX = event.getX();
                    return true;
                case MotionEvent.ACTION_UP:
                    float mUpX = event.getX();
                    Log.d(TAG, mDownX + "   " + mUpX + "    ");
                    ValueAnimator animator = ValueAnimator.ofFloat(0,mDownX - mUpX);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float value = (float) animation.getAnimatedValue();
                            performScrollAnimation((int) value);
                        }
                    });
                    animator.start();
                    if (mDownX - mUpX > 0){
                    }
                    break;
            }
            return false;
        }
    };

    private void performScrollAnimation(final int offset) {
        mTextListener = new OnDrawTextListener() {
            @Override
            public void onDrawText(Canvas canvas, int tabCounts) {
                int gap = mMeasuredWidth / tabCounts;
                for (int i = 0; i < tabCounts; i++){
                    canvas.drawText(mTabNames.get(i), i * gap - offset, mDrawTextHeight, mPaint);
                }
            }
        };
        invalidate();
    }

    private OnDrawTextListener mTextListener = new OnDrawTextListener() {
        @Override
        public void onDrawText(Canvas canvas, int tabCounts) {
            if (tabCounts == 0){
                Log.d(TAG, "SIZE: 0");
                return;
            }
            float gap = mMeasuredWidth / tabCounts;
            for (int i = 0; i < tabCounts; i ++){
                canvas.drawText(mTabNames.get(i), i * gap, mMeasuredHeight / 2, mPaint);
            }
        }
    };


    private static final String TAG = "HSBV";
    
    private int mMeasuredWidth;
    private int mMeasuredHeight;
    private int mDrawTextHeight;

    private List<String> mTabNames = new ArrayList<>();
    
    private Paint mPaint = new Paint();
    private int mTextColor = Color.parseColor("#47bafe");

    private View mView;

    public void setOnDrawTextListener(OnDrawTextListener l){
        mTextListener = l;
    }

    public void attachView(View view){
        mView = view;
        init();
    }
    public void detachView(){
        if (isAttached()) {
            mView.setOnTouchListener(null);
        }
        mView = null;
    }
    private boolean isAttached(){
        return mView != null;
    }
    public void setTabName(ArrayList<String> names){
        mTabNames = names;
    }

    public HorizontalScrollingBarView(Context context) {
        super(context);
    }

    public HorizontalScrollingBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalScrollingBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        if (isAttached()){
            mView.setOnTouchListener(mScrollListener);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mMeasuredWidth = getMeasuredWidth();
        mMeasuredHeight = getMeasuredHeight();
        mDrawTextHeight = mMeasuredHeight / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setAntiAlias(true);
        mPaint.setColor(mTextColor);
        mPaint.setTextSize(mMeasuredHeight / 5);
        
        mTextListener.onDrawText(canvas, mTabNames.size());
    }
}
