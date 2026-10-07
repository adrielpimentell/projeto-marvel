package com.example.marvel.ui.ranks;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.example.marvel.game.Artifact;
import com.example.marvel.game.GameBalance;

import java.util.ArrayList;
import java.util.List;

final class ChestAnimator {

    interface Listener {
        void onFinished();
    }

    static final class Views {
        View root;
        View dim;
        View scene;
        View chestGroup;
        View lid;
        View seam;
        RaysView rays;
        View glow;
        ParticleBurstView particles;
        View flash;
        View cardFlip;
        View cardBack;
        View cardFront;
        View[] details;
        View doneButton;
        View legendaryBanner;
    }

    private static final float DIM_ALPHA = 0.7f;
    private static final float LID_OPEN_DEGREES = -100f;
    private static final long PARTICLES_MS = 900;

    private final Views v;
    private final Artifact.Rarity rarity;
    private final boolean legendary;
    private final int rarityColor;
    private final int particleCount;
    private final float density;
    private final List<Animator> idle = new ArrayList<>();

    private AnimatorSet sequence;
    private Listener listener;
    private boolean finished;

    ChestAnimator(Views views, Artifact.Rarity rarity, int rarityColor) {
        this.v = views;
        this.rarity = rarity;
        this.legendary = ChestAnimSpec.isLegendary(rarity);
        this.rarityColor = rarityColor;
        this.particleCount = ChestAnimSpec.particles(rarity);
        this.density = views.root.getResources().getDisplayMetrics().density;
    }

    void play(Listener onFinished) {
        listener = onFinished;
        stopEverything();
        finished = false;
        resetToStart();
        if (!ValueAnimator.areAnimatorsEnabled()) {
            showFinal();
            return;
        }
        sequence = new AnimatorSet();
        sequence.playSequentially(entry(), suspense(), opening(), reveal(), details());
        sequence.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) showFinal();
            }
        });
        sequence.start();
    }

    void showResult(Listener onFinished) {
        listener = onFinished;
        stopEverything();
        finished = false;
        resetToStart();
        showFinal();
    }

    void skip() {
        if (finished) return;
        showFinal();
    }

    void cancel() {
        finished = true;
        listener = null;
        stopEverything();
    }

    void showFinal() {
        if (finished) return;
        finished = true;
        stopEverything();

        v.dim.setAlpha(DIM_ALPHA);
        v.flash.setAlpha(0f);
        set(v.chestGroup, 0f, 60 * density, 1f, 0.45f);
        v.chestGroup.setRotation(0f);
        v.lid.setRotationX(LID_OPEN_DEGREES);
        v.lid.setTranslationY(-6 * density);
        v.seam.setAlpha(0f);
        v.scene.setTranslationX(0f);
        v.scene.setTranslationY(0f);
        v.rays.setAlpha(1f);
        v.rays.setScaleX(raysScale());
        v.rays.setScaleY(raysScale());
        v.glow.setAlpha(1f);
        v.glow.setScaleX(1.2f);
        v.glow.setScaleY(1.2f);
        set(v.cardFlip, 0f, 0f, 1f, 1f);
        v.cardBack.setAlpha(0f);
        v.cardBack.setRotationY(90f);
        v.cardFront.setAlpha(1f);
        v.cardFront.setRotationY(0f);
        for (View detail : v.details) {
            detail.setAlpha(1f);
            detail.setTranslationY(0f);
        }
        if (legendary) {
            placeBanner();
            v.legendaryBanner.setVisibility(View.VISIBLE);
            set(v.legendaryBanner, 0f, v.legendaryBanner.getTranslationY(), 1f, 1f);
            v.legendaryBanner.setRotation(-6f);
        }

        v.doneButton.setVisibility(View.VISIBLE);
        if (ValueAnimator.areAnimatorsEnabled()) {
            v.doneButton.setAlpha(0f);
            v.doneButton.animate().alpha(1f).setDuration(200).start();
            startIdle();
        } else {
            v.doneButton.setAlpha(1f);
        }
        if (listener != null) listener.onFinished();
    }

    private Animator entry() {
        long d = GameBalance.CHEST_ANIM_ENTRY_MS;
        ObjectAnimator dim = ObjectAnimator.ofFloat(v.dim, View.ALPHA, 0f, DIM_ALPHA);
        dim.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator rise = ObjectAnimator.ofFloat(v.chestGroup, View.TRANSLATION_Y, 420 * density, 0f);
        rise.setInterpolator(new OvershootInterpolator(1.6f));
        ObjectAnimator squash = ObjectAnimator.ofPropertyValuesHolder(v.chestGroup,
                frames(View.SCALE_Y, 0f, 1f, 0.72f, 1f, 0.86f, 0.9f, 1f, 1f),
                frames(View.SCALE_X, 0f, 1f, 0.72f, 1f, 0.86f, 1.06f, 1f, 1f));
        return together(d, dim, rise, squash);
    }

    private Animator suspense() {
        long d = ChestAnimSpec.suspenseMs(rarity);
        ObjectAnimator shake = ObjectAnimator.ofPropertyValuesHolder(v.chestGroup, frames(View.ROTATION,
                0f, 0f, 0.08f, -3f, 0.16f, 3f, 0.25f, 0f,
                0.33f, 0f, 0.42f, -6f, 0.51f, 6f, 0.6f, 0f,
                0.67f, 0f, 0.77f, -10f, 0.87f, 10f, 1f, 0f));
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(v.chestGroup,
                frames(View.SCALE_X, 0f, 1f, 0.16f, 1.03f, 0.3f, 1f, 0.51f, 1.06f, 0.63f, 1f, 0.87f, 1.1f, 1f, 1.04f),
                frames(View.SCALE_Y, 0f, 1f, 0.16f, 1.03f, 0.3f, 1f, 0.51f, 1.06f, 0.63f, 1f, 0.87f, 1.1f, 1f, 1.04f));
        ObjectAnimator light = ObjectAnimator.ofPropertyValuesHolder(v.seam,
                frames(View.ALPHA, 0f, 0f, 0.2f, 0.35f, 0.3f, 0.15f, 0.55f, 0.6f, 0.65f, 0.35f, 0.9f, 1f, 1f, 0.9f),
                frames(View.SCALE_X, 0f, 0.6f, 1f, 1.1f));
        return together(d, shake, pulse, light);
    }

    private Animator opening() {
        long d = GameBalance.CHEST_ANIM_OPEN_MS;
        ObjectAnimator lidTurn = ObjectAnimator.ofFloat(v.lid, View.ROTATION_X, 0f, LID_OPEN_DEGREES);
        lidTurn.setInterpolator(new DecelerateInterpolator(2.5f));
        ObjectAnimator lidUp = ObjectAnimator.ofFloat(v.lid, View.TRANSLATION_Y, 0f, -6 * density);
        lidUp.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator settle = ObjectAnimator.ofPropertyValuesHolder(v.chestGroup,
                PropertyValuesHolder.ofFloat(View.ROTATION, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f));
        settle.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator flash = ObjectAnimator.ofPropertyValuesHolder(v.flash,
                frames(View.ALPHA, 0f, 0f, 0.12f, 0.85f, 1f, 0f));
        ObjectAnimator rays = ObjectAnimator.ofPropertyValuesHolder(v.rays,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.4f, raysScale()),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.4f, raysScale()),
                PropertyValuesHolder.ofFloat(View.ROTATION, 0f, legendary ? 180f : 90f));
        rays.setInterpolator(new DecelerateInterpolator(1.5f));
        ObjectAnimator glow = ObjectAnimator.ofPropertyValuesHolder(v.glow,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.3f, 1.2f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.3f, 1.2f));
        glow.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator seamOff = ObjectAnimator.ofFloat(v.seam, View.ALPHA, 0f);
        seamOff.setInterpolator(new AccelerateInterpolator());

        AnimatorSet set = legendary
                ? together(d, lidTurn, lidUp, settle, flash, rays, glow, seamOff, screenShake())
                : together(d, lidTurn, lidUp, settle, flash, rays, glow, seamOff);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                v.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                burstParticles();
            }
        });
        return set;
    }

    private Animator reveal() {
        long d = GameBalance.CHEST_ANIM_REVEAL_MS;
        ObjectAnimator rise = ObjectAnimator.ofPropertyValuesHolder(v.cardFlip,
                frames(View.ALPHA, 0f, 0f, 0.15f, 1f, 1f, 1f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 110 * density, 0f),
                frames(View.SCALE_X, 0f, 0.35f, 0.7f, 1.08f, 1f, 1f),
                frames(View.SCALE_Y, 0f, 0.35f, 0.7f, 1.08f, 1f, 1f));
        rise.setInterpolator(new DecelerateInterpolator(1.4f));
        ObjectAnimator back = ObjectAnimator.ofPropertyValuesHolder(v.cardBack,
                frames(View.ROTATION_Y, 0f, 0f, 0.5f, 90f, 1f, 90f),
                frames(View.ALPHA, 0f, 1f, 0.5f, 1f, 0.501f, 0f, 1f, 0f));
        ObjectAnimator front = ObjectAnimator.ofPropertyValuesHolder(v.cardFront,
                frames(View.ROTATION_Y, 0f, -90f, 0.5f, -90f, 1f, 0f),
                frames(View.ALPHA, 0f, 0f, 0.499f, 0f, 0.5f, 1f, 1f, 1f));
        ObjectAnimator chestAway = ObjectAnimator.ofPropertyValuesHolder(v.chestGroup,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 60 * density),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.45f));
        chestAway.setInterpolator(new DecelerateInterpolator());
        return together(d, rise, back, front, chestAway);
    }

    private Animator details() {
        long d = GameBalance.CHEST_ANIM_DETAILS_MS;
        List<Animator> parts = new ArrayList<>();
        for (int i = 0; i < v.details.length; i++) {
            ObjectAnimator show = ObjectAnimator.ofPropertyValuesHolder(v.details[i],
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 12 * density, 0f));
            show.setInterpolator(new DecelerateInterpolator());
            show.setStartDelay((long) (d * 0.15f * i));
            show.setDuration((long) (d * 0.55f));
            parts.add(show);
        }
        if (legendary) parts.add(bannerPop(d));
        AnimatorSet set = new AnimatorSet();
        set.playTogether(parts);
        return set;
    }

    private Animator screenShake() {
        float s = GameBalance.CHEST_SCREEN_SHAKE_DP * density;
        return ObjectAnimator.ofPropertyValuesHolder(v.scene,
                frames(View.TRANSLATION_X, 0f, 0f, 0.08f, -s, 0.16f, s * 0.9f, 0.26f, -s * 0.7f,
                        0.36f, s * 0.55f, 0.48f, -s * 0.4f, 0.6f, s * 0.25f, 0.75f, -s * 0.1f, 1f, 0f),
                frames(View.TRANSLATION_Y, 0f, 0f, 0.1f, s * 0.6f, 0.22f, -s * 0.5f, 0.34f, s * 0.35f,
                        0.5f, -s * 0.2f, 0.7f, s * 0.08f, 1f, 0f));
    }

    private Animator bannerPop(long d) {
        ObjectAnimator pop = ObjectAnimator.ofPropertyValuesHolder(v.legendaryBanner,
                frames(View.ALPHA, 0f, 0f, 0.3f, 1f, 1f, 1f),
                frames(View.SCALE_X, 0f, 2.2f, 0.6f, 0.9f, 1f, 1f),
                frames(View.SCALE_Y, 0f, 2.2f, 0.6f, 0.9f, 1f, 1f),
                frames(View.ROTATION, 0f, -18f, 0.6f, -4f, 1f, -6f));
        pop.setInterpolator(new DecelerateInterpolator());
        pop.setDuration(d);
        pop.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                placeBanner();
                v.legendaryBanner.setVisibility(View.VISIBLE);
            }
        });
        return pop;
    }

    private void placeBanner() {
        float cardTop = v.cardFlip.getTop() - v.legendaryBanner.getTop();
        v.legendaryBanner.setTranslationY(cardTop - v.legendaryBanner.getHeight() * 0.55f);
    }

    private float raysScale() {
        return legendary ? 1.25f : 1f;
    }

    private void startIdle() {
        ObjectAnimator sway = ObjectAnimator.ofFloat(v.rays, View.ROTATION,
                v.rays.getRotation(), v.rays.getRotation() + 14f);
        ObjectAnimator breathe = ObjectAnimator.ofFloat(v.glow, View.ALPHA, 1f, 0.65f);
        for (ObjectAnimator loop : new ObjectAnimator[]{sway, breathe}) {
            loop.setDuration(loop == sway ? 2600 : 1300);
            loop.setRepeatCount(ValueAnimator.INFINITE);
            loop.setRepeatMode(ValueAnimator.REVERSE);
            loop.setInterpolator(new AccelerateDecelerateInterpolator());
            loop.start();
            idle.add(loop);
        }
        if (legendary) {
            ObjectAnimator beat = ObjectAnimator.ofPropertyValuesHolder(v.legendaryBanner,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.07f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.07f));
            beat.setDuration(700);
            beat.setRepeatCount(ValueAnimator.INFINITE);
            beat.setRepeatMode(ValueAnimator.REVERSE);
            beat.setInterpolator(new AccelerateDecelerateInterpolator());
            beat.start();
            idle.add(beat);
        }
    }

    private void burstParticles() {
        int[] chest = new int[2];
        int[] layer = new int[2];
        v.chestGroup.getLocationInWindow(chest);
        v.particles.getLocationInWindow(layer);
        float x = chest[0] - layer[0] + v.chestGroup.getWidth() / 2f;
        float y = chest[1] - layer[1] + v.lid.getHeight();
        v.particles.burst(x, y, particleCount, rarityColor, PARTICLES_MS);
    }

    private void resetToStart() {
        float camera = 12000 * density;
        v.dim.setAlpha(0f);
        v.flash.setAlpha(0f);
        v.scene.setTranslationX(0f);
        v.scene.setTranslationY(0f);
        set(v.chestGroup, 0f, 420 * density, 1f, 1f);
        v.chestGroup.setRotation(0f);
        v.lid.setPivotX(v.lid.getWidth() / 2f);
        v.lid.setPivotY(v.lid.getHeight());
        v.lid.setCameraDistance(camera);
        v.lid.setRotationX(0f);
        v.lid.setTranslationY(0f);
        v.seam.setAlpha(0f);
        v.seam.setScaleX(0.6f);
        v.rays.setAlpha(0f);
        v.rays.setRotation(0f);
        v.glow.setAlpha(0f);
        set(v.cardFlip, 0f, 110 * density, 0.35f, 0f);
        v.cardBack.setCameraDistance(camera);
        v.cardFront.setCameraDistance(camera);
        v.cardBack.setRotationY(0f);
        v.cardBack.setAlpha(1f);
        v.cardFront.setRotationY(-90f);
        v.cardFront.setAlpha(0f);
        for (View detail : v.details) {
            detail.setAlpha(0f);
            detail.setTranslationY(12 * density);
        }
        v.doneButton.setVisibility(View.INVISIBLE);
        v.legendaryBanner.setAlpha(0f);
        v.legendaryBanner.setVisibility(legendary ? View.INVISIBLE : View.GONE);
    }

    private void stopEverything() {
        if (sequence != null) {
            sequence.cancel();
            sequence = null;
        }
        for (Animator loop : idle) loop.cancel();
        idle.clear();
        v.particles.stop();
        v.doneButton.animate().cancel();
    }

    private static void set(View view, float translationX, float translationY, float scale, float alpha) {
        view.setTranslationX(translationX);
        view.setTranslationY(translationY);
        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setAlpha(alpha);
    }

    private static AnimatorSet together(long duration, Animator... animators) {
        for (Animator animator : animators) animator.setDuration(duration);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        return set;
    }

    private static PropertyValuesHolder frames(Property<View, Float> property, float... pairs) {
        Keyframe[] keyframes = new Keyframe[pairs.length / 2];
        TimeInterpolator ease = new AccelerateDecelerateInterpolator();
        for (int i = 0; i < keyframes.length; i++) {
            keyframes[i] = Keyframe.ofFloat(pairs[2 * i], pairs[2 * i + 1]);
            if (i > 0) keyframes[i].setInterpolator(ease);
        }
        return PropertyValuesHolder.ofKeyframe(property, keyframes);
    }
}
