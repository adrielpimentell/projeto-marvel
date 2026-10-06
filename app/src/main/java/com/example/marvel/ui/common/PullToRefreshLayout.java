package com.example.marvel.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;

import com.example.marvel.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class PullToRefreshLayout extends FrameLayout implements NestedScrollingParent3 {

    private static final float DRAG_RATE = 0.5f;

    private final NestedScrollingParentHelper parentHelper = new NestedScrollingParentHelper(this);
    private final CircularProgressIndicator indicator;
    private final float threshold;
    private final float maxPull;
    private final float hiddenOffset;
    private Runnable onRefresh;
    private float pull;
    private boolean refreshing;

    public PullToRefreshLayout(@NonNull Context context) {
        this(context, null);
    }

    public PullToRefreshLayout(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        threshold = getResources().getDimension(R.dimen.pull_refresh_threshold);
        maxPull = threshold * 1.6f;

        indicator = new CircularProgressIndicator(context);
        indicator.setIndeterminate(true);
        indicator.setIndicatorSize(getResources().getDimensionPixelSize(R.dimen.icon_md));
        indicator.setTrackThickness(Math.round(3 * density));
        indicator.setIndicatorColor(context.getColor(R.color.red));
        indicator.setBackgroundResource(R.drawable.bg_circle);
        indicator.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.surface)));
        int padding = getResources().getDimensionPixelSize(R.dimen.space_sm);
        indicator.setPadding(padding, padding, padding, padding);
        indicator.setElevation(getResources().getDimension(R.dimen.space_xs));
        indicator.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        hiddenOffset = -(getResources().getDimensionPixelSize(R.dimen.icon_md) + 2f * padding);
        indicator.setTranslationY(hiddenOffset);
        indicator.setVisibility(INVISIBLE);
        addView(indicator, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        bringChildToFront(indicator);
        ViewCompat.addAccessibilityAction(this, getContext().getString(R.string.ranking_refresh),
                (AccessibilityViewCommand) (view, arguments) -> {
                    startRefresh();
                    return true;
                });
    }

    public void setOnRefreshListener(Runnable onRefresh) {
        this.onRefresh = onRefresh;
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    public void setRefreshing(boolean refreshing) {
        if (this.refreshing == refreshing) return;
        this.refreshing = refreshing;
        if (refreshing) {
            showAt(threshold);
        } else {
            hide();
        }
    }

    private void startRefresh() {
        if (refreshing) return;
        setRefreshing(true);
        if (onRefresh != null) onRefresh.run();
    }

    private void showAt(float offset) {
        indicator.setVisibility(VISIBLE);
        indicator.animate().translationY(hiddenOffset + offset).alpha(1f)
                .setDuration(UiTokens.DURATION_SHORT_MS).start();
    }

    private void hide() {
        pull = 0;
        indicator.animate().translationY(hiddenOffset).alpha(0f)
                .setDuration(UiTokens.DURATION_SHORT_MS)
                .withEndAction(() -> {
                    if (!refreshing) indicator.setVisibility(INVISIBLE);
                })
                .start();
    }

    private void follow() {
        indicator.animate().cancel();
        indicator.setVisibility(pull > 0 ? VISIBLE : INVISIBLE);
        indicator.setTranslationY(hiddenOffset + pull);
        indicator.setAlpha(Math.min(1f, pull / threshold));
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return isEnabled() && !refreshing && type == ViewCompat.TYPE_TOUCH
                && (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type);
        pull = 0;
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        parentHelper.onStopNestedScroll(target, type);
        if (refreshing) return;
        if (pull >= threshold) {
            startRefresh();
        } else if (pull > 0) {
            hide();
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        if (type != ViewCompat.TYPE_TOUCH || refreshing || dyUnconsumed >= 0) return;
        pull = Math.min(maxPull, pull - dyUnconsumed * DRAG_RATE);
        consumed[1] += dyUnconsumed;
        follow();
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, new int[2]);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (type != ViewCompat.TYPE_TOUCH || refreshing || dy <= 0 || pull <= 0) return;
        float used = Math.min(pull, dy * DRAG_RATE);
        pull -= used;
        consumed[1] = Math.round(used / DRAG_RATE);
        follow();
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        return pull > 0;
    }

    @Override
    public int getNestedScrollAxes() {
        return parentHelper.getNestedScrollAxes();
    }
}
