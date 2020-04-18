package com.kotlin.cloud.usefulClass;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.animation.AnimationUtils;

import org.jetbrains.annotations.NotNull;

public class ScrollAwareFABBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    private int height = 0;
    private int currentState = 2;
    private ViewPropertyAnimator currentAnimator;

    public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onLayoutChild(@NotNull CoordinatorLayout parent, V child, int layoutDirection) {
        this.height = child.getMeasuredHeight();
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    public boolean onStartNestedScroll(@NotNull CoordinatorLayout coordinatorLayout, @NotNull V child, @NotNull View directTargetChild, @NotNull View target, int nestedScrollAxes) {
        return nestedScrollAxes == 2;
    }

    public void onNestedScroll(@NotNull CoordinatorLayout coordinatorLayout, @NotNull V child, @NotNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (this.currentState != 1 && dyConsumed > 0) {
            this.slideDown(child);
        } else if (this.currentState != 2 && dyConsumed < 0) {
            this.slideUp(child);
        }

    }

    private void slideUp(V child) {
        if (this.currentAnimator != null) {
            this.currentAnimator.cancel();
            child.clearAnimation();
        }

        this.currentState = 2;
        this.animateChildTo(child, 0, AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR);
    }

    private void slideDown(V child) {
        if (this.currentAnimator != null) {
            this.currentAnimator.cancel();
            child.clearAnimation();
        }

        this.currentState = 1;
        this.animateChildTo(child, this.height+200, AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR);
    }

    private void animateChildTo(V child, int targetX, TimeInterpolator interpolator) {
        this.currentAnimator = child.animate().translationX((float)targetX).setInterpolator(interpolator).setDuration(225).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ScrollAwareFABBehavior.this.currentAnimator = null;
            }
        });
    }
}
