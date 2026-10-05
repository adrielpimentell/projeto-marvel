package com.example.marvel.ui.ranks;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.marvel.ui.common.Screens;
import com.example.marvel.R;
import com.example.marvel.game.Artifact;
import com.example.marvel.game.ChestReward;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.common.ArtifactUi;
import com.example.marvel.ui.common.DebugTools;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.RewardCard;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class ChestOpenActivity extends AppCompatActivity {

    private static final String EXTRA_RANK = "extra_rank";
    private static final String EXTRA_DAILY = "extra_daily";
    private static final String EXTRA_SURVIVAL = "extra_survival";
    private static final String STATE_REWARD = "state_reward";

    private ChestReward reward;
    private ChestAnimator animator;
    private ChestAnimator.Views views;
    private int rarityColor;

    public static Intent newIntent(Context context, int rank) {
        return new Intent(context, ChestOpenActivity.class).putExtra(EXTRA_RANK, rank);
    }

    public static Intent newDailyIntent(Context context) {
        return new Intent(context, ChestOpenActivity.class).putExtra(EXTRA_DAILY, true);
    }

    public static Intent newSurvivalIntent(Context context) {
        return new Intent(context, ChestOpenActivity.class).putExtra(EXTRA_SURVIVAL, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_chest_open);

        boolean restored = false;
        if (savedInstanceState != null) {
            reward = savedInstanceState.getSerializable(STATE_REWARD, ChestReward.class);
            restored = reward != null;
        }
        if (reward == null) {
            PlayerStore store = PlayerStore.getInstance(this);
            if (isDaily()) reward = store.openDailyChest();
            else if (isSurvival()) reward = store.openSurvivalChest();
            else reward = store.openChest(getIntent().getIntExtra(EXTRA_RANK, -1));
        }
        if (reward == null) {
            finish();
            return;
        }

        View root = findViewById(R.id.chest_root);
        View scene = findViewById(R.id.chest_scene);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            scene.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        rarityColor = ArtifactUi.rarityColor(this, reward.getRarity());
        bindViews();
        bindReward();
        setupDebug();

        root.setOnClickListener(v -> animator.skip());
        findViewById(R.id.chest_done).setOnClickListener(v -> finish());

        animator = newAnimator();
        final boolean showFinalNow = restored;
        root.post(() -> {
            if (animator == null) return;
            if (showFinalNow) {
                animator.showResult(this::onAnimationFinished);
            } else {
                animator.play(this::onAnimationFinished);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (reward != null) outState.putSerializable(STATE_REWARD, reward);
    }

    @Override
    protected void onDestroy() {
        if (animator != null) animator.cancel();
        animator = null;
        super.onDestroy();
    }

    private void bindViews() {
        views = new ChestAnimator.Views();
        views.root = findViewById(R.id.chest_root);
        views.dim = findViewById(R.id.chest_dim);
        views.scene = findViewById(R.id.chest_scene);
        views.chestGroup = findViewById(R.id.chest_group);
        views.lid = findViewById(R.id.chest_lid);
        views.seam = findViewById(R.id.chest_seam);
        views.rays = findViewById(R.id.chest_rays);
        views.glow = findViewById(R.id.chest_glow);
        views.particles = findViewById(R.id.chest_particles);
        views.flash = findViewById(R.id.chest_flash);
        views.cardFlip = findViewById(R.id.card_flip);
        views.cardBack = findViewById(R.id.card_back);
        views.cardFront = findViewById(R.id.artifact_card);
        views.details = new View[]{
                findViewById(R.id.artifact_rarity),
                findViewById(R.id.artifact_name),
                findViewById(R.id.artifact_type),
                findViewById(R.id.artifact_effect)};
        views.doneButton = findViewById(R.id.chest_done);
        views.legendaryBanner = findViewById(R.id.legendary_banner);
    }

    private void bindReward() {
        String title;
        if (isDaily()) title = getString(R.string.chest_daily_title);
        else if (isSurvival()) title = getString(R.string.chest_survival_title);
        else title = getString(R.string.chest_title, PlayerHud.rankName(this, reward.getRank()));
        ((TextView) findViewById(R.id.chest_title)).setText(title);
        RewardCard.bind((MaterialCardView) views.cardFront, reward.getRarity(), reward.getArtifact(),
                reward.getCoins());

        MaterialCardView back = (MaterialCardView) views.cardBack;
        back.setStrokeColor(ColorStateList.valueOf(rarityColor));
        ImageView backIcon = findViewById(R.id.card_back_icon);
        backIcon.setImageResource(reward.getArtifact() != null ? R.drawable.ic_gem : R.drawable.ic_coin);
        backIcon.setColorFilter(rarityColor);

        ((ImageView) views.glow).setImageDrawable(radialLight(rarityColor, 150));
        ((ImageView) views.seam).setImageDrawable(radialLight(rarityColor, 110));
        views.rays.setRayColor(reward.getRarity() == Artifact.Rarity.LEGENDARY
                ? getColor(R.color.rarity_legendary) : rarityColor);
    }

    private void setupDebug() {
        View debug = findViewById(R.id.chest_debug);
        if (!DebugTools.ENABLED) {
            debug.setVisibility(View.GONE);
            return;
        }
        debug.setVisibility(View.VISIBLE);
        MaterialSwitch slow = findViewById(R.id.debug_slow_motion);
        slow.setChecked(DebugTools.chestSlowMotion);
        slow.setOnCheckedChangeListener((button, checked) -> DebugTools.chestSlowMotion = checked);
        findViewById(R.id.debug_replay).setOnClickListener(v -> {
            animator.cancel();
            animator = newAnimator();
            ((TextView) findViewById(R.id.chest_hint)).setText(R.string.chest_skip_hint);
            animator.play(this::onAnimationFinished);
        });
    }

    private boolean isDaily() {
        return getIntent().getBooleanExtra(EXTRA_DAILY, false);
    }

    private boolean isSurvival() {
        return getIntent().getBooleanExtra(EXTRA_SURVIVAL, false);
    }

    private ChestAnimator newAnimator() {
        float timeScale = DebugTools.ENABLED && DebugTools.chestSlowMotion
                ? GameBalance.CHEST_SLOW_MOTION_FACTOR : 1f;
        return new ChestAnimator(views, reward.getRarity(), rarityColor, timeScale);
    }

    private void onAnimationFinished() {
        ((TextView) findViewById(R.id.chest_hint)).setText(
                reward.getArtifact() != null ? getString(R.string.chest_done_hint) : "");
        TextView name = findViewById(R.id.artifact_name);
        ViewCompat.setAccessibilityLiveRegion(views.cardFront,
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        views.cardFront.setContentDescription(ArtifactUi.rarityName(this, reward.getRarity()) + ": "
                + name.getText());
    }

    private GradientDrawable radialLight(int color, int radiusDp) {
        GradientDrawable light = new GradientDrawable();
        light.setShape(GradientDrawable.OVAL);
        light.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        light.setGradientRadius(radiusDp * getResources().getDisplayMetrics().density);
        light.setColors(new int[]{ColorUtils.setAlphaComponent(color, 0xDD),
                ColorUtils.setAlphaComponent(color, 0x00)});
        return light;
    }
}
