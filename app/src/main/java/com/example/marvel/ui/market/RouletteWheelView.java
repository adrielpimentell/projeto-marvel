package com.example.marvel.ui.market;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.example.marvel.R;

public class RouletteWheelView extends View {

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path pointer = new Path();
    private final float density;

    private WheelMath math;
    private int[] colors = new int[0];
    private String[] labels = new String[0];
    private float rotation;
    private int highlight = -1;

    public RouletteWheelView(Context context) {
        this(context, null);
    }

    public RouletteWheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        slicePaint.setStyle(Paint.Style.FILL);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2 * density);
        linePaint.setColor(context.getColor(R.color.bg));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(4 * density);
        ringPaint.setColor(context.getColor(R.color.divider));
        hubPaint.setStyle(Paint.Style.FILL);
        hubPaint.setColor(context.getColor(R.color.surface));
        textPaint.setColor(context.getColor(R.color.bg));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12,
                getResources().getDisplayMetrics()));
        pointerPaint.setStyle(Paint.Style.FILL);
        pointerPaint.setColor(context.getColor(R.color.red));
        pointerStroke.setStyle(Paint.Style.STROKE);
        pointerStroke.setStrokeWidth(2 * density);
        pointerStroke.setColor(context.getColor(R.color.white));
    }

    void setSlices(int[] oddsPerMille, int[] sliceColors, String[] sliceLabels) {
        math = new WheelMath(oddsPerMille);
        colors = sliceColors;
        labels = sliceLabels;
        invalidate();
    }

    WheelMath getMath() {
        return math;
    }

    void setWheelRotation(float degrees) {
        rotation = degrees;
        invalidate();
    }

    void setHighlight(int slice) {
        highlight = slice;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (math == null) return;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - 16 * density;
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        canvas.save();
        canvas.rotate(rotation, cx, cy);
        for (int i = 0; i < math.sliceCount(); i++) {
            float sweep = math.sweepOf(i);
            if (sweep <= 0f) continue;
            float start = -90f + math.startOf(i);
            boolean dim = highlight >= 0 && highlight != i;
            slicePaint.setColor(dim ? ColorUtils.blendARGB(colors[i], getContext().getColor(R.color.bg), 0.65f)
                    : colors[i]);
            canvas.drawArc(oval, start, sweep, true, slicePaint);
            drawSeparator(canvas, cx, cy, radius, start);
            if (sweep >= 26f) drawLabel(canvas, cx, cy, radius, start + sweep / 2f, labels[i]);
        }
        canvas.restore();

        canvas.drawCircle(cx, cy, radius, ringPaint);
        canvas.drawCircle(cx, cy, radius * 0.16f, hubPaint);
        canvas.drawCircle(cx, cy, radius * 0.16f, ringPaint);
        float half = 12 * density;
        float top = cy - radius - 10 * density;
        pointer.reset();
        pointer.moveTo(cx - half, top);
        pointer.lineTo(cx + half, top);
        pointer.lineTo(cx, top + 26 * density);
        pointer.close();
        canvas.drawPath(pointer, pointerPaint);
        canvas.drawPath(pointer, pointerStroke);
    }

    private void drawSeparator(Canvas canvas, float cx, float cy, float radius, float angle) {
        double rad = Math.toRadians(angle);
        canvas.drawLine(cx, cy, cx + (float) Math.cos(rad) * radius, cy + (float) Math.sin(rad) * radius,
                linePaint);
    }

    private void drawLabel(Canvas canvas, float cx, float cy, float radius, float angle, String text) {
        double rad = Math.toRadians(angle);
        float x = cx + (float) Math.cos(rad) * radius * 0.62f;
        float y = cy + (float) Math.sin(rad) * radius * 0.62f;
        canvas.save();
        canvas.rotate(angle + 90f, x, y);
        canvas.drawText(text, x, y + textPaint.getTextSize() / 3f, textPaint);
        canvas.restore();
    }
}
