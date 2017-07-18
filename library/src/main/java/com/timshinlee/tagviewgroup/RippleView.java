package com.timshinlee.tagviewgroup;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * author: shell
 * date 2016/12/25 下午7:18
 **/
public class RippleView extends View implements ITagView {

    private int mRadius;
    private int mAlpha;
    private DIRECTION mDirection;
    private Paint mPaint;
    private AnimatorSet mAnimator;
    private int mX, mY;

    public RippleView(Context context) {
        this(context, null);
    }

    public RippleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setAlpha(mAlpha); // 设置透明度
        canvas.drawCircle(mX, mY, mRadius, mPaint); // 画圆
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopRipple(); // detached时停止动画
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startRipple(); // attached时播放动画
    }

    @Override
    public void setDirection(DIRECTION direction) {
        mDirection = direction;
    }

    @Override
    public DIRECTION getDirection() {
        return mDirection;
    }

    /**
     * 开始动画
     */
    public void startRipple() {
        mAnimator.start();
    }

    /**
     * 停止动画
     */
    public void stopRipple() {
        mAnimator.end();
    }

    /**
     * 设置水波纹半径
     */
    public void setRippleRadius(int radius) {
        mRadius = radius;
        invalidate();
    }

    /**
     * 设置水波纹 alpha 范围[0-255]
     */
    public void setRippleAlpha(int alpha) {
        mAlpha = alpha;
        invalidate();
    }

    /**
     * 设置中心点
     */
    public void setCenterPoint(int x, int y) {
        mX = x;
        mY = y;
    }

    /**
     * 初始化水波纹动画，半径变大的同时透明度变小
     *
     * @param minRadius 波纹最小半径
     * @param maxRadius 波纹最大半径
     * @param alpha     透明度
     */
    public void initAnimator(int minRadius, int maxRadius, int alpha) {
        ObjectAnimator radiusAnimator = ObjectAnimator.ofInt(this, "RippleRadius", minRadius, maxRadius);
        radiusAnimator.setRepeatMode(ValueAnimator.RESTART); // 正向播放
        radiusAnimator.setRepeatCount(ValueAnimator.INFINITE); // 无限播放
        ObjectAnimator alphaAnimator = ObjectAnimator.ofInt(this, "RippleAlpha", alpha, 0);
        alphaAnimator.setRepeatMode(ValueAnimator.RESTART);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator = new AnimatorSet();
        mAnimator.playTogether(radiusAnimator, alphaAnimator);
        mAnimator.setDuration(1500);
//        mAnimator.setInterpolator(new AccelerateInterpolator()); // 加速播放
        mAnimator.setInterpolator(new LinearInterpolator()); // 匀速播放
    }
}
