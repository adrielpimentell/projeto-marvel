package com.example.marvel.ui.ranks;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import java.util.Random;

public class ParticleBurstView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final float density;

    private float[] angle = new float[0];
    private float[] distance = new float[0];
    private float[] size = new float[0];
    private int[] colors = new int[0];
    private float originX;
    private float originY;
    private float progress = 1f;
    private ValueAnimator animator;

    public ParticleBurstView(Context context) {
        this(context, null);
    }

    public ParticleBurstView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);
    }

    void burst(float x, float y, int count, int color, long durationMs) {
        stop();
        originX = x;
        originY = y;
        angle = new float[count];
        distance = new float[count];
        size = new float[count];
        colors = new int[count];
        for (int i = 0; i < count; i++) {
            angle[i] = (float) Math.toRadians(-165 + random.nextFloat() * 150);
            distance[i] = (90 + random.nextFloat() * 110) * density;
            size[i] = (2.5f + random.nextFloat() * 4f) * density;
            colors[i] = i % 4 == 0 ? Color.WHITE : ColorUtils.blendARGB(color, Color.WHITE, random.nextFloat() * 0.3f);
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(durationMs);
        animator.setInterpolator(new DecelerateInterpolator(1.6f));
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    void stop() {
        if (animator != null) animator.cancel();
        animator = null;
        progress = 1f;
        angle = new float[0];
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (angle.length == 0 || progress >= 1f) return;
        float gravity = 60 * density * progress * progress;
        int alpha = (int) (255 * (1f - (float) Math.pow(progress, 1.5)));
        for (int i = 0; i < angle.length; i++) {
            float x = originX + (float) Math.cos(angle[i]) * distance[i] * progress;
            float y = originY + (float) Math.sin(angle[i]) * distance[i] * progress + gravity;
            paint.setColor(ColorUtils.setAlphaComponent(colors[i], alpha));
            canvas.drawCircle(x, y, size[i] * (1f - 0.4f * progress), paint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}
