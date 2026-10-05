package com.example.marvel.ui.common;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

import com.example.marvel.R;

public final class Skeleton {

    private static final long PULSE_MS = 700;
    private static final float DIM_ALPHA = 0.45f;

    private Skeleton() {
    }

    public static void show(View skeleton) {
        skeleton.setVisibility(View.VISIBLE);
        if (skeleton.getTag(R.id.skeleton_pulse) != null) return;
        if (!ValueAnimator.areAnimatorsEnabled()) return;
        ObjectAnimator pulse = ObjectAnimator.ofFloat(skeleton, View.ALPHA, 1f, DIM_ALPHA);
        pulse.setDuration(PULSE_MS);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        skeleton.setTag(R.id.skeleton_pulse, pulse);
        if (skeleton.getTag(R.id.skeleton_watch) == null) {
            View.OnAttachStateChangeListener watch = new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    stop(v);
                }
            };
            skeleton.setTag(R.id.skeleton_watch, watch);
            skeleton.addOnAttachStateChangeListener(watch);
        }
        pulse.start();
    }

    public static void hide(View skeleton) {
        stop(skeleton);
        skeleton.setVisibility(View.GONE);
    }

    private static void stop(View skeleton) {
        Object pulse = skeleton.getTag(R.id.skeleton_pulse);
        if (pulse instanceof ObjectAnimator) ((ObjectAnimator) pulse).cancel();
        skeleton.setTag(R.id.skeleton_pulse, null);
        skeleton.setAlpha(1f);
    }
}
