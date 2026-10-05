package com.example.marvel.ui.ranks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

public class RaysView extends View {

    private static final int RAYS = 14;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path rays = new Path();
    private int color = Color.WHITE;

    public RaysView(Context context) {
        this(context, null);
    }

    public RaysView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.FILL);
    }

    void setRayColor(int rayColor) {
        color = rayColor;
        rebuild(getWidth(), getHeight());
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuild(w, h);
    }

    private void rebuild(int w, int h) {
        if (w == 0 || h == 0) return;
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f;
        paint.setShader(new RadialGradient(cx, cy, radius,
                new int[]{ColorUtils.setAlphaComponent(color, 190), ColorUtils.setAlphaComponent(color, 0)},
                null, Shader.TileMode.CLAMP));

        rays.reset();
        float step = 360f / RAYS;
        for (int i = 0; i < RAYS; i++) {
            float half = (i % 2 == 0 ? 0.30f : 0.18f) * step;
            double a1 = Math.toRadians(i * step - half);
            double a2 = Math.toRadians(i * step + half);
            rays.moveTo(cx, cy);
            rays.lineTo(cx + (float) Math.cos(a1) * radius, cy + (float) Math.sin(a1) * radius);
            rays.lineTo(cx + (float) Math.cos(a2) * radius, cy + (float) Math.sin(a2) * radius);
            rays.close();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(rays, paint);
    }
}
