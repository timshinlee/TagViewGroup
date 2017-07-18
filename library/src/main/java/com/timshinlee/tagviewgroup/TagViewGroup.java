package com.timshinlee.tagviewgroup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;


/**
 * author: shell
 * date 2016/12/20 下午2:24
 **/
public class TagViewGroup extends ViewGroup {
    public static final int DEFAULT_RADIUS = 8;//默认外圆半径
    public static final int DEFAULT_INNER_RADIUS = 4;//默认内圆半径
    public static final int DEFAULT_V_DISTANCE = 28;//默认竖直(上/下)方向线条长度
    public static final int DEFAULT_TILT_DISTANCE = 30;//默认斜线长度
    public static final int DEFAULT_LINES_WIDTH = 1;//默认线宽
    public static final int DEFAULT_MAX_TAG = 3;//默认标签最大数量
    private static final int DEFAULT_RIPPLE_MAX_RADIUS = 20;//水波纹默认最大半径
    private static final int DEFAULT_RIPPLE_ALPHA = 100;//默认水波纹透明度
    private Paint mPaint;
    private Path mPath;
    private Path mDstPath;
    private PathMeasure mPathMeasure;
    private Animator mShowAnimator;
    private Animator mHideAnimator;
    private GestureDetectorCompat mGestureDetector;
    private OnTagGroupClickListener mClickListener;
    private RippleView mRippleView;
    /**
     * 水波纹最大半径
     */
    private int mRippleMaxRadius;
    /**
     * 水波纹最小半径
     */
    private int mRippleMinRadius;
    /**
     * 水波纹起始透明度
     */
    private int mRippleAlpha;
    /**
     * 外圆半径
     */
    private int mRadius;
    /**
     * 内圆半径
     */
    private int mInnerRadius;
    /**
     * 斜线水平长度
     */
    private int mTiltDistance;
    /**
     * 竖直(上/下)方向线条长度
     */
    private int mVerDistance;
    private RectF mCenterRect;
    private RectF[] mRectArray;
    private int[] mChildUsed;
    private int mTagCount;
    /**
     * 圆心 X 坐标
     */
    private int mCenterX;
    /**
     * 圆心 Y 坐标
     */
    private int mCenterY;
    /**
     * 圆心坐标在ViewGroup中的百分点
     */
    private float mPercentX;
    /**
     * 圆心坐标在ViewGroup中的百分点
     */
    private float mPercentY;
    /**
     * 线条宽度
     */
    private int mLinesWidth;
    /**
     * 当前ViewGroup是否隐藏
     */
    private boolean mIsHidden;
    /**
     * 线条比例
     */
    private float mLinesRatio = 1;
    /**
     * 方向模式，默认为第一种
     */
    private int mDirectionMode;

    /**
     * 不允许父布局拦截onTouchEvent
     */
    private boolean mDisallowIntercept;

    public TagViewGroup(Context context) {
        this(context, null);
    }

    public TagViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Resources.Theme theme = context.getTheme();
        TypedArray array = theme.obtainStyledAttributes(attrs, R.styleable.TagViewGroup, defStyleAttr, 0);
        mRadius = array.getDimensionPixelSize(R.styleable.TagViewGroup_radius, DipConvertUtils.dip2px(context, DEFAULT_RADIUS));
        mInnerRadius = array.getDimensionPixelSize(R.styleable.TagViewGroup_inner_radius, DipConvertUtils.dip2px(context, DEFAULT_INNER_RADIUS));
        mTiltDistance = array.getDimensionPixelSize(R.styleable.TagViewGroup_tilt_distance, DipConvertUtils.dip2px(context, DEFAULT_TILT_DISTANCE));
        mVerDistance = array.getDimensionPixelSize(R.styleable.TagViewGroup_v_distance, DipConvertUtils.dip2px(context, DEFAULT_V_DISTANCE));
        mLinesWidth = array.getDimensionPixelSize(R.styleable.TagViewGroup_line_width, DipConvertUtils.dip2px(context, DEFAULT_LINES_WIDTH));
        mRippleMaxRadius = array.getDimensionPixelSize(R.styleable.TagViewGroup_ripple_maxRadius, DipConvertUtils.dip2px(context, DEFAULT_RIPPLE_MAX_RADIUS));
        mRippleAlpha = array.getInteger(R.styleable.TagViewGroup_ripple_alpha, DEFAULT_RIPPLE_ALPHA);
        mRippleMinRadius = mInnerRadius + (mRadius - mInnerRadius) / 2;
        array.recycle();

        mPaint = new Paint();
        mPath = new Path();
        mDstPath = new Path();
        mPathMeasure = new PathMeasure();
        mPaint.setAntiAlias(true);
        mGestureDetector = new GestureDetectorCompat(context, new TagOnGestureListener());
        mChildUsed = new int[4];
        mCenterRect = new RectF();
        mRectArray = new RectF[6];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec); // 测量子控件
        mChildUsed = getChildUsed(); // 获取中心圆上下左右各个方向的宽度
        // 圆心刚开始默认在左上角 (0,0)
        mCenterX = (int) (getMeasuredWidth() * mPercentX);
        mCenterY = (int) (getMeasuredHeight() * mPercentY);
        // 设置中心矩形的坐标
        mCenterRect.set(mCenterX - mRadius, mCenterY - mRadius, mCenterX + mRadius, mCenterY + mRadius);
        // 如果有波纹就设置波纹中心在圆心
        if (mRippleView != null) {
            mRippleView.setCenterPoint(mCenterX, mCenterY);
        }
    }

    /**
     * 获取中心圆上下左右各个方向的宽度
     *
     * @return int[]{left,top,right,bottom}
     */
    private int[] getChildUsed() {
        int childCount = getChildCount();
        // 设置各方向宽度初始值为垂直线条长度
        int leftMax = mVerDistance, topMax = mVerDistance, rightMax = mVerDistance, bottomMax = mVerDistance;
        for (int i = 0; i < childCount; i++) { // 遍历每个child，对比到比当前max值大的value则更新max值
            ITagView child = (ITagView) getChildAt(i);
            switch (child.getDirection()) {
                case RIGHT_TOP_TILT://右上斜线
                    rightMax = Math.max(rightMax, mTiltDistance + child.getMeasuredWidth() + 2 * mInnerRadius);
                    topMax = Math.max(topMax, child.getMeasuredHeight() + mTiltDistance);
                    break;
                case TOP_RIGHT://右上
                    rightMax = Math.max(rightMax, child.getMeasuredWidth());
                    topMax = Math.max(topMax, child.getMeasuredHeight() + mVerDistance);
                    break;
                case RIGHT_CENTER://右中
                    rightMax = Math.max(rightMax, child.getMeasuredWidth() + 2 * mInnerRadius);
                    topMax = Math.max(topMax, Math.max(mVerDistance, child.getMeasuredHeight()));
                    break;
                case RIGHT_BOTTOM://右下
                    rightMax = Math.max(rightMax, child.getMeasuredWidth() + 2 * mInnerRadius);
                    bottomMax = mVerDistance;
                    break;
                case RIGHT_BOTTOM_TILT:
                    rightMax = Math.max(rightMax, mTiltDistance + child.getMeasuredWidth() + 2 * mInnerRadius);
                    bottomMax = mTiltDistance;
                    break;
                case LEFT_TOP://左上
                    leftMax = Math.max(leftMax, child.getMeasuredWidth() + 2 * mInnerRadius);
                    topMax = Math.max(topMax, child.getMeasuredHeight() + mVerDistance);
                    break;
                case LEFT_TOP_TILT://左上斜线
                    leftMax = Math.max(leftMax, child.getMeasuredWidth() + mTiltDistance + 2 * mInnerRadius);
                    topMax = Math.max(topMax, child.getMeasuredHeight() + mTiltDistance);
                    break;
                case LEFT_CENTER://左中
                    leftMax = Math.max(leftMax, child.getMeasuredWidth() + 2 * mInnerRadius);
                    topMax = Math.max(topMax, Math.max(mVerDistance, child.getMeasuredHeight()));
                    break;
                case LEFT_BOTTOM://左下
                    leftMax = Math.max(leftMax, child.getMeasuredWidth() + 2 * mInnerRadius);
                    bottomMax = mVerDistance;
                    break;
                case LEFT_BOTTOM_TILT://左下斜线
                    leftMax = Math.max(leftMax, child.getMeasuredWidth() + mTiltDistance + 2 * mInnerRadius);
                    bottomMax = mTiltDistance;
                    break;
            }
        }
        return new int[]{leftMax, topMax, rightMax, bottomMax};
    }

    /**
     * 设置tag的位置
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = 0, top = 0;
        for (int i = 0; i < getChildCount(); i++) { // 遍历TagView
            ITagView child = (ITagView) getChildAt(i);
            switch (child.getDirection()) {
                case RIGHT_TOP_TILT://右上斜线
                    top = mCenterY - mTiltDistance - child.getMeasuredHeight();
                    left = mCenterX + mTiltDistance;
                    break;
                case TOP_RIGHT://右上
                    left = mCenterX;
                    top = mCenterY - mVerDistance - child.getMeasuredHeight();
                    break;
                case RIGHT_CENTER://右中
//                    left = mCenterX;
                    left = mCenterX + mTiltDistance; // 设置中间tag对齐上下斜tag
                    top = mCenterY - child.getMeasuredHeight();
                    break;
                case RIGHT_BOTTOM://右下
                    left = mCenterX;
                    top = mVerDistance + mCenterY - child.getMeasuredHeight();
                    break;
                case RIGHT_BOTTOM_TILT://右下斜线
                    left = mCenterX + mTiltDistance;
                    top = mTiltDistance + mCenterY - child.getMeasuredHeight();
                    break;
                case LEFT_TOP://左上
                    left = mCenterX - child.getMeasuredWidth() - mTiltDistance;
                    top = mCenterY - mVerDistance - child.getMeasuredHeight();
                    break;
                case LEFT_TOP_TILT://左上斜线
                    left = mCenterX - child.getMeasuredWidth() - mTiltDistance;
                    top = mCenterY - mTiltDistance - child.getMeasuredHeight();
                    break;
                case LEFT_CENTER://左中
                    left = mCenterX - child.getMeasuredWidth() - mTiltDistance;
                    top = mCenterY - child.getMeasuredHeight();
                    break;
                case LEFT_BOTTOM://左下
                    left = mCenterX - child.getMeasuredWidth() - mTiltDistance;
                    top = mVerDistance + mCenterY - child.getMeasuredHeight();
                    break;
                case LEFT_BOTTOM_TILT://左下斜线
                    left = mCenterX - child.getMeasuredWidth() - mTiltDistance;
                    top = mTiltDistance + mCenterY - child.getMeasuredHeight();
                    break;
                case CENTER:
                    left = 0;
                    top = 0;
                    break;
            }
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }
        refreshTagsRect();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //绘制折线
        drawLines(canvas);
        //绘制外圆
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
        //绘制内圆
//        mPaint.setStyle(Paint.Style.FILL);
//        mPaint.setColor(Color.WHITE);
//        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mPaint);
    }

    private void drawTagAlpha(float alpha) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setAlpha(alpha);
        }
    }

    private void drawLines(Canvas canvas) {
        // 设置画笔
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(mLinesWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        // 遍历子控件
        for (int i = 0; i < getChildCount(); i++) {
            ITagView child = (ITagView) getChildAt(i);
            mPath.reset(); // 重置path
            mPath.moveTo(mCenterX, mCenterY); // 移动到圆心坐标
            mDstPath.reset();
            switch (child.getDirection()) {
                case TOP_RIGHT://右上
                case RIGHT_BOTTOM://右下
                case RIGHT_TOP_TILT://右上斜线
                case RIGHT_BOTTOM_TILT://右下斜线
                    mPath.lineTo(child.getLeft(), child.getBottom()); // 分成两段画，先画到tag左下角
                case RIGHT_CENTER://右中
                    mPath.lineTo(child.getRight(), child.getBottom()); // 直接或继续画到tag右下角
                    // 绘制小圆
                    final RectF rectF = new RectF(child.getRight(), child.getBottom() - mInnerRadius, child.getRight() + 2 * mInnerRadius, child.getBottom() + mInnerRadius);
                    mPath.arcTo(rectF, 180, 359, false); //  sweepAngle还会 mod 360,如果是360就等于0
                    break;
                case LEFT_TOP://左上
                case LEFT_TOP_TILT://左上斜线
                case LEFT_BOTTOM://左下
                case LEFT_BOTTOM_TILT://左下斜线
                    mPath.lineTo(child.getRight(), child.getBottom()); // 分成两段画，先画到tag右下角
                case LEFT_CENTER://左中
                    mPath.lineTo(child.getLeft(), child.getBottom()); // 直接或继续画到tag左下角
                    // 绘制小圆
                    final RectF rectF2 = new RectF(child.getLeft() - 2 * mInnerRadius, child.getBottom() - mInnerRadius, child.getLeft(), child.getBottom() + mInnerRadius);
                    mPath.arcTo(rectF2, 0, 359, false); // sweepAngle还会自动 mod 360,如果是360就会等于0
                    break;
            }
            mPathMeasure.setPath(mPath, false); // 设置path不强制闭合
            mPathMeasure.getSegment(0, mPathMeasure.getLength() * mLinesRatio, mDstPath, true);
            canvas.drawPath(mDstPath, mPaint);
        }
    }

    /**
     * 检查当前是否有动画或者动画是否正在播放
     */
    private boolean checkAnimating() {
        return mShowAnimator == null || mHideAnimator == null
                || mShowAnimator.isRunning() || mHideAnimator.isRunning();
    }

    /**
     * 添加 Tag 列表
     *
     * @param tagList 要添加的 Tag 列表
     * @return 返回 标签组
     */
    public TagViewGroup addTagList(@NonNull List<ITagView> tagList) {
        for (ITagView tag : tagList) {
            addTag(tag);
        }
        return this;
    }

    /**
     * 添加单个 Tag
     *
     * @param tag 要添加的 Tag
     * @return 返回 标签组
     */
    public TagViewGroup addTag(@NonNull ITagView tag) {
        if (mTagCount >= DEFAULT_MAX_TAG) {
            throw new RuntimeException("The number of tags exceeds the maximum value(6)");
        }
        tag.setTag(mTagCount); // 绑定标志
        addView((View) tag);
        mRectArray[mTagCount] = new RectF();
        mTagCount++;
        return this;
    }

    /**
     * 得到 TagViewGroup 中的所有标签列表
     */
    public List<ITagView> getTagList() {
        List<ITagView> list = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            ITagView tag = (ITagView) getChildAt(i);
            if (tag.getDirection() != DIRECTION.CENTER) {
                list.add(tag);
            }
        }
        return list;
    }

    /**
     * 得到 TagViewGroup 中的标签数量
     */
    public int getTagCount() {
        return mTagCount;
    }

    /**
     * 添加水波纹，ripple也是TagViewGroup的child，只是方向是CENTER
     */
    public void addRipple() {
        mRippleView = new RippleView(getContext());
        mRippleView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mRippleView.setDirection(DIRECTION.CENTER);
        mRippleView.initAnimator(mRippleMinRadius, mRippleMaxRadius, mRippleAlpha);
        addView(mRippleView);
    }

    /**
     * 刷新Tag的边界
     */
    private void refreshTagsRect() {
        for (int i = 0; i < getChildCount(); i++) {
            ITagView child = (ITagView) getChildAt(i);
            if (child.getDirection() != DIRECTION.CENTER) {
                int index = (int) child.getTag();
                if (mRectArray[index] == null) {
                    mRectArray[index] = new RectF();
                }
                mRectArray[index].set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            }
        }
    }

    /**
     * 检测 Touch 事件发生在哪个 Tag 上
     *
     * @param x 触摸x坐标
     * @param y 触摸y坐标
     */
    private ITagView isTouchingTags(float x, float y) {
        for (int i = 0; i < getChildCount(); i++) {
            ITagView child = (ITagView) getChildAt(i);
            if (child.getDirection() != DIRECTION.CENTER && mRectArray[(int) child.getTag()].contains(x, y)) {
                return child; // 如果child不是ripple而且该child所在矩形区域包括触摸点，则返回该child
            }
        }
        return null;
    }

    /**
     * 设置中心圆点坐标在整个ViewGroup 的比例点
     */
    public void setPercent(float percentX, float percentY) {
        mPercentX = percentX;
        mPercentY = percentY;
    }

    /**
     * 设置显示动画
     */
    public TagViewGroup setShowAnimator(Animator animator) {
        mShowAnimator = animator;
        mShowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setHidden(false); // 保存可见标志变量
            }
        });
        return this;
    }

    public TagViewGroup setHideAnimator(Animator animator) {
        mHideAnimator = animator;
        mHideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(INVISIBLE); // 播放完毕设为不可见
                setHidden(true); // 保存不可见标志变量
                updateDirection(); // 更新方向
                requestLayout(); // 更新布局
                setVisibility(View.VISIBLE); // 设置可见
                mShowAnimator.start(); // 开始动画
            }
        });
        return this;
    }

    /**
     * 设置TagGroup为可见并播放动画
     */
    public void showWithAnimation() {
        if (!checkAnimating()) {
            setVisibility(View.VISIBLE);
            mShowAnimator.start();
        }
    }

    /**
     * 播放动画
     */
    public void hideWithAnimation() {
        if (!checkAnimating()) {
            mHideAnimator.start();
        }
    }

    /**
     * 属性 CircleRadius 的属性动画调用，设置中心圆的半径
     */
    @SuppressWarnings("unused")
    public void setCircleRadius(int radius) {
        mRadius = radius;
        invalidate();
    }

    /**
     * 属性 CircleInnerRadius 的属性动画调用,设置中心内圆半径
     */
    @SuppressWarnings("unused")
    public void setCircleInnerRadius(int innerRadius) {
        mInnerRadius = innerRadius;
        invalidate();
    }

    /**
     * 属性 LinesRatio 的属性动画调用，设置线条显示比例
     */
    @SuppressWarnings("unused")
    public void setLinesRatio(float ratio) {
        mLinesRatio = ratio;
        invalidate();
    }

    /**
     * 属性 TagAlpha 的属性动画调用，设置 Tag 的透明度
     */
    @SuppressWarnings("unused")
    public void setTagAlpha(float alpha) {
        drawTagAlpha(alpha);
    }


    public boolean isHidden() {
        return mIsHidden;
    }

    public void setHidden(boolean hidden) {
        mIsHidden = hidden;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mClickListener != null) {
            float x = event.getX();
            float y = event.getY();
            if (mCenterRect.contains(x, y) || isTouchingTags(x, y) != null) {
                // 如果触碰在中心点上或者tag上就拦截，否则不拦截使上下滑动可行，解决无法上下滑动的bug
                getParent().requestDisallowInterceptTouchEvent(mDisallowIntercept);
            }
            return mGestureDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    public void setOnTagGroupClickListener(OnTagGroupClickListener listener) {
        mClickListener = listener;
    }

    public interface OnTagGroupClickListener {
        //TagGroup 中心圆点被点击
        void onCircleClick(TagViewGroup group);

        //TagGroup Tag子view被点击
        void onTagClick(TagViewGroup group, ITagView tag, int index);

        //TagGroup 被长按
        void onLongPress(TagViewGroup group);

        //TagGroup 移动
        void onScroll(TagViewGroup group, float percentX, float percentY);
    }

    //内部处理 touch 事件监听器
    private class TagOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            if (mCenterRect.contains(x, y) || isTouchingTags(x, y) != null) {
                return true; // 如果触碰在中心点上或者tag上就继续传递触摸事件，否则就拦截触摸事件
            }
            return super.onDown(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            if (mCenterRect.contains(x, y)) { // 如果点击的是中心点
                mClickListener.onCircleClick(TagViewGroup.this);
            } else { // 否则点击的就是除了ripple以外的tag
                ITagView clickedTag = isTouchingTags(x, y); // 获取点击的tag
                mClickListener.onTagClick(TagViewGroup.this, clickedTag, (int) clickedTag.getTag());
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float currentX = mCenterX - distanceX; // 新的中心坐标是原中心坐标减去每次移动的距离
            float currentY = mCenterY - distanceY;
            currentX = Math.min(Math.max(currentX, mChildUsed[0]), getMeasuredWidth() - mChildUsed[2]);
            currentY = Math.min(Math.max(currentY, mChildUsed[1]), getMeasuredHeight() - mChildUsed[3]);
            mPercentX = currentX / getMeasuredWidth();
            mPercentY = currentY / getMeasuredHeight();
            requestLayout();
            mClickListener.onScroll(TagViewGroup.this, mPercentX, mPercentY);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            float x = e.getX();
            float y = e.getY();
            if (mCenterRect.contains(x, y) || isTouchingTags(x, y) != null) {
                mClickListener.onLongPress(TagViewGroup.this);
            }
        }
    }

    /**
     * 设置线条宽度
     */
    public void setLineWidth(int lineWidth) {
        mLinesWidth = lineWidth;
    }

    /**
     * 得到线条宽度
     */
    public int getLineWidth() {
        return mLinesWidth;
    }

    /**
     * 设置中心外圆半径
     */
    public void setRadius(int radius) {
        mRadius = radius;
    }

    /**
     * 得到中心圆半径
     */
    public int getRadius() {
        return mRadius;
    }

    public void setInnerRadius(int innerRadius) {
        mInnerRadius = innerRadius;
    }

    public int getInnerRadius() {
        return mInnerRadius;
    }

    /**
     * 设置圆心到折点的垂直距离
     *
     * @param vDistance 垂直距离
     */
    public void setVDistance(int vDistance) {
        mVerDistance = vDistance;
    }

    public int getVDistance() {
        return mVerDistance;
    }

    /**
     * 设置圆心到斜线折点的垂直距离
     *
     * @param tiltDistance 垂直距离
     */
    public void setTiltDistance(int tiltDistance) {
        mTiltDistance = tiltDistance;
    }

    public int getTiltDistance() {
        return mTiltDistance;
    }

    /**
     * 设置水波纹最大半径
     */
    @SuppressWarnings("unused")
    public void setRippleMaxRadius(int radius) {
        mRippleMaxRadius = radius;
    }

    @SuppressWarnings("unused")
    public int getRippleMaxRadius() {
        return mRippleMaxRadius;
    }

    /**
     * 设置水波纹起始透明度
     */
    @SuppressWarnings("unused")
    public void setRippleAlpha(int alpha) {
        mRippleAlpha = alpha;
    }

    @SuppressWarnings("unused")
    public int getRippleAlpha() {
        return mRippleAlpha;
    }

    public void changeDirection(List<TagGroupModel.Tag> tagList) {
//       updateDirection();
//        requestLayout(); // 更新layout
//        hideWithAnimation();
//        invalidate(); // 调用onDraw
//        showWithAnimation();
        hideWithAnimation();
    }

    private void updateDirection() {
        setVisibility(View.INVISIBLE);
        final int tagCount = getTagCount(); // tag数目
        final List<DIRECTION[][]> modes = DirectionUtil.getInstance().getModes();
        final DIRECTION[][] directions = modes.get(tagCount - 1); // 获取tag数目对应的方向模式
        mDirectionMode = (++mDirectionMode) % 4; // 取值[0,3)
        for (int i = 0; i < getChildCount(); i++) {
            final ITagView child = (ITagView) getChildAt(i);
            if (child.getDirection() != DIRECTION.CENTER) { // 如果不是ripple
                child.setDirection(directions[mDirectionMode][i]);
//                tagList.get(i).setDirection(directions[mDirectionMode][i].getValue()); // 修改保存的模型列表的值
            }
        }
    }

    /**
     * 设置是否允许父布局拦截onTouchEvent，在设置了OnTagGroupClickListener的情况下才生效
     */
    public void setDisallowIntercept(boolean disallowIntercept) {
        mDisallowIntercept = disallowIntercept;
    }
}
