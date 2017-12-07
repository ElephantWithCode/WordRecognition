package com.example.wordrecognition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.zip.CheckedOutputStream;

/**
 * Created by Ellly on 2017/11/30.
 */

interface OnDrawTextListener{
    void onDrawText(Canvas canvas, int tabCount);
}
interface OnTabScrollListener{
    void onScrollEnd(int currentItemPosition);
    void onPreScroll(int currentItemPosition);
}
public class HorizontalScrollingBarView extends View{

    private OnTabScrollListener mTabScrollListener = null;

    public void setOnTabScrollListener(OnTabScrollListener listener){
        mTabScrollListener = listener;
    }

    private CountDownTimer mHideTask = new CountDownTimer(3000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }
        @Override
        public void onFinish() {
            performHideOrShowAnimation(true);
        }
    };

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

                    mTimeRecord[1] = System.currentTimeMillis();

                    if (mTimeRecord[1] - mTimeRecord[0] < 3000) {
                        mHideTask.cancel();
                    }

                    mTimeRecord[0] = System.currentTimeMillis();
                    mHideTask.start();

                    performHideOrShowAnimation(false);

                    float distance = mDownX - mUpX;
                    if (isScrolling || Math.abs(distance) < 200){//是否正在滑动， 200 为检测最小滑动偏移量
                        return false;
                    }
                    if (distance > 0){
                        if (mCurrentCenterItemPosition >= mTabNames.size() - 1){
                            return false;
                        }
                        mCurrentCenterItemPosition++;
                    }else {
                        if (mCurrentCenterItemPosition <= 0){
                            return false;
                        }
                        mCurrentCenterItemPosition--;
                    }

                    int stringWidth = (int) mPaint.measureText(mTabNames.get(mCurrentCenterItemPosition)); // 当前文字的宽度
                    int centerPosition = mTabNames.size() / 2; // 中心的位置
                    int itemWidth = mMeasuredWidth / mTabNames.size(); // 屏幕宽度除以总数目
                    final int targetXOffset = ((itemWidth - stringWidth) / 2 + centerPosition * itemWidth) - mTabsXPosition.get(mCurrentCenterItemPosition);
                    ValueAnimator translateXAnimator = ValueAnimator.ofInt(0, targetXOffset);
                    translateXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            int offset = (int) animation.getAnimatedValue();
                            performScrollAnimation(offset);
                            Log.d(TAG+"ANIMAT", offset +"");
                        }
                    });
                    translateXAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            new android.os.Handler().post(new Runnable() {
                                @Override
                                public void run() {
                                    //更新横坐标的位置
                                    for (int i = 0; i < mTabsXPosition.size(); i++){
                                        int previousX = mTabsXPosition.get(i);
                                        mTabsXPosition.set(i, previousX + targetXOffset);
                                    }
                                    isScrolling = false;

                                    if (mTabScrollListener != null){
                                        mTabScrollListener.onScrollEnd(mCurrentCenterItemPosition);
                                    }
                                }
                            });
                        }
                    });
                    if (mTabScrollListener != null){
                        mTabScrollListener.onPreScroll(mCurrentCenterItemPosition);
                    }
                    translateXAnimator.setDuration(500);
                    translateXAnimator.start();
                    isScrolling = true;

                    break;
            }
            return false;
        }
    };

    private void performHideOrShowAnimation(boolean isHide) {
        ValueAnimator animator;
        float currentAlpha = getAlpha();
        if (isHide) {
            animator = ValueAnimator.ofFloat(currentAlpha, 0);
        } else {
            animator = ValueAnimator.ofFloat(currentAlpha, 1);
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                setAlpha(value);
            }
        });
        animator.setDuration(800).start();

    }

    private void delayedHide() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                performHideOrShowAnimation(true);
            }
        }, 5000);
    }


    /**
     * 偏移text
     * 产生动画时常用（其实可以通用）
     * @param offset 偏移量
     */
    private void performScrollAnimation(final int offset) {
        mTextListener = new OnDrawTextListener() {
            @Override
            public void onDrawText(Canvas canvas, int tabCounts) {
//                int gap = mMeasuredWidth / tabCounts;
                for (int i = 0; i < tabCounts; i++){
//                    String currentName = mTabNames.get(i);
//                    float stringWidth = mPaint.measureText(currentName);
//                    int x = (int) ((gap - stringWidth) / 2 + i * gap);
                    int x = mTabsXPosition.get(i);
                    if (mCurrentCenterItemPosition != i) {
                        mPaint.setColor(mTextColor);
                        canvas.drawText(mTabNames.get(i), x + offset, mDrawTextHeight, mPaint);
                    } else {
                        mPaint.setColor(mSelectedTextColor);
                        canvas.drawText(mTabNames.get(i), x + offset, mDrawTextHeight, mPaint);
                    }
                    Log.d(TAG+"_PER", offset +"     x:" + x);
                }
            }
        };
        invalidate();
    }

    /**
     * 起始时候应该画text的横坐标
     */
    private OnDrawTextListener mTextListener = new OnDrawTextListener() {
        @Override
        public void onDrawText(Canvas canvas, int tabCounts) {
            if (tabCounts == 0){
                Log.d(TAG, "SIZE: 0");
                return;
            }
            float gap = mMeasuredWidth / tabCounts;
            for (int i = 0; i < tabCounts; i ++){
                String currentName = mTabNames.get(i);
                float stringWidth = mPaint.measureText(currentName);
                int x = (int) ((gap - stringWidth) / 2 + i * gap);
                mTabsXPosition.add(x);//第一次画的时候添加，以后直接改动
                if (mCurrentCenterItemPosition == i){
                    mPaint.setColor(mSelectedTextColor);
                    canvas.drawText(currentName, x, mDrawTextHeight, mPaint);
                }else {
                    mPaint.setColor(mTextColor);
                    canvas.drawText(currentName, x, mDrawTextHeight, mPaint);
                }
            }
            mTimeRecord[0] = System.currentTimeMillis();
            mHideTask.start();
        }
    };


    private static final String TAG = "HSBV";


    /**
     * 判断当前是否在滚动
     */
    private boolean isScrolling = false;
    /**
     * 记录时间。
     */
    private long[] mTimeRecord = new long[2];

    /**
     * 标记当前在中间的item位置
     */
    private int mCurrentCenterItemPosition;

    private int mMeasuredWidth;
    private int mMeasuredHeight;
    /**
     * 表示画text的高度
     */
    private int mDrawTextHeight;

    private List<String> mTabNames = new ArrayList<>();
    /**
     * 保存所有Tabs的横坐标
     */
    private ArrayList<Integer> mTabsXPosition = new ArrayList<>();

    private Paint mPaint = new Paint();
    private int mTextColor = Color.WHITE;
    private int mSelectedTextColor = Color.parseColor("#47bafe");

    /**
     * 被监听滑动的View
     */
    private View mView;

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
        mCurrentCenterItemPosition = mTabNames.size()/ 2;
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
        mDrawTextHeight = mMeasuredHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setAntiAlias(true);
        mPaint.setColor(mTextColor);
        mPaint.setTextSize(mDrawTextHeight);

        mTextListener.onDrawText(canvas, mTabNames.size());

    }
}
