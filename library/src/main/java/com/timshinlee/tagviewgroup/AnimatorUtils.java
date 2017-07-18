package com.timshinlee.tagviewgroup;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;

/**
 * author: shell
 * date 2016/12/22 下午12:53
 **/
public class AnimatorUtils {
    /**
     * tag显示动画，依次显示圆形动画、直线动画、文本动画
     */
    public static Animator getTagShowAnimator(final TagViewGroup target) {
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(circleRadiusAnimator(target), linesAnimator(target), tagTextAnimator(target));
        return set;
    }

    /**
     * tag隐藏动画
     */
    public static Animator getTagHideAnimator(final TagViewGroup target) {
        AnimatorSet together = new AnimatorSet();
        AnimatorSet sequential = new AnimatorSet();
        ObjectAnimator linesAnimator = ObjectAnimator.ofFloat(target, "LinesRatio", 1, 0);
        ObjectAnimator tagTextAnimator = ObjectAnimator.ofFloat(target, "TagAlpha", 1, 0);
        Animator circleAnimator = circleRadiusAnimator(target);
        together.playTogether(linesAnimator, tagTextAnimator);
        together.setDuration(400);
        together.setInterpolator(new DecelerateInterpolator());
        sequential.playSequentially(circleAnimator, together);
        return sequential;
    }

    /**
     * 文本透明度动画
     */
    private static Animator tagTextAnimator(TagViewGroup target) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "TagAlpha", 0, 1);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        return animator;
    }

    /**
     * 线条长度比例动画
     */
    private static Animator linesAnimator(TagViewGroup target) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "LinesRatio", 0, 1);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        return animator;
    }

    /**
     * 圆形弧度动画
     */
    private static AnimatorSet circleRadiusAnimator(TagViewGroup target) {
        int radius = target.getRadius();
        int innerRadius = target.getInnerRadius();
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ObjectAnimator.ofInt(target, "CircleRadius", radius - 10, radius + 10, radius),
                ObjectAnimator.ofInt(target, "CircleInnerRadius", innerRadius - 10, innerRadius + 10, innerRadius));
        set.setDuration(400);
        return set;
    }
}
