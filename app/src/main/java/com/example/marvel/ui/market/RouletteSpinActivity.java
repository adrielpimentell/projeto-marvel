package com.example.marvel.ui.market;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.marvel.ui.common.UiTokens;
import com.example.marvel.ui.common.Screens;
import com.example.marvel.R;
import com.example.marvel.game.Artifact;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.Roulette;
import com.example.marvel.game.RouletteReward;
import com.example.marvel.ui.common.ArtifactUi;
import com.example.marvel.ui.common.RewardCard;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class RouletteSpinActivity extends AppCompatActivity {

    private static final String EXTRA_REWARD = "extra_reward";

    private RouletteReward reward;
    private RouletteWheelView wheel;
    private MaterialCardView card;
    private View doneButton;
    private ValueAnimator spin;
    private float targetRotation;
    private boolean finished;

    public static Intent newIntent(Context context, RouletteReward reward) {
        return new Intent(context, RouletteSpinActivity.class).putExtra(EXTRA_REWARD, reward);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_roulette_spin);

        reward = getIntent().getSerializableExtra(EXTRA_REWARD, RouletteReward.class);
        if (reward == null) {
            finish();
            return;
        }

        View root = findViewById(R.id.roulette_root);
        Screens.padForSystemBars(root);

        wheel = findViewById(R.id.roulette_wheel);
        card = findViewById(R.id.artifact_card);
        doneButton = findViewById(R.id.roulette_done);
        root.setOnClickListener(v -> showFinal());
        doneButton.setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.roulette_title))
                .setText(ArtifactUi.rouletteName(this, reward.getRoulette()));
        RewardCard.bind(card, reward.getRarity(), reward.getArtifact(), reward.getCoins());
        setupWheel(reward.getRoulette());

        long seed = reward.getRarity().ordinal() * 31L
                + (reward.getArtifact() == null ? 0 : reward.getArtifact().getId().hashCode());
        float position = 0.25f + new Random(seed).nextFloat() * 0.5f;
        targetRotation = wheel.getMath().targetRotation(reward.getRarity().ordinal(), position,
                GameBalance.ROULETTE_FULL_TURNS);

        if (savedInstanceState != null) {
            showFinal();
        } else {
            wheel.post(this::playSpin);
        }
    }

    @Override
    protected void onDestroy() {
        if (spin != null) spin.cancel();
        super.onDestroy();
    }

    private void setupWheel(Roulette roulette) {
        Artifact.Rarity[] rarities = Artifact.Rarity.values();
        int[] odds = new int[rarities.length];
        int[] colors = new int[rarities.length];
        String[] labels = new String[rarities.length];
        NumberFormat percent = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        percent.setMaximumFractionDigits(1);
        for (Artifact.Rarity rarity : rarities) {
            int i = rarity.ordinal();
            odds[i] = roulette.oddsPerMille(rarity);
            colors[i] = ArtifactUi.rarityColor(this, rarity);
            labels[i] = ArtifactUi.rarityName(this, rarity) + " "
                    + getString(R.string.roulette_percent, percent.format(odds[i] / 10.0));
        }
        wheel.setSlices(odds, colors, labels);
    }

    private void playSpin() {
        if (finished) return;
        spin = ValueAnimator.ofFloat(0f, targetRotation);
        spin.setDuration(GameBalance.ROULETTE_SPIN_MS);
        spin.setInterpolator(new DecelerateInterpolator(2.2f));
        spin.addUpdateListener(a -> wheel.setWheelRotation((float) a.getAnimatedValue()));
        spin.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) revealCard();
            }
        });
        spin.start();
    }

    private void revealCard() {
        if (finished) return;
        wheel.setHighlight(reward.getRarity().ordinal());
        float density = getResources().getDisplayMetrics().density;
        card.setTranslationY(40 * density);
        card.setScaleX(0.85f);
        card.setScaleY(0.85f);
        card.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(GameBalance.ROULETTE_REVEAL_MS)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(this::showFinal)
                .start();
    }

    private void showFinal() {
        if (finished) return;
        finished = true;
        if (spin != null) spin.cancel();
        card.animate().cancel();
        wheel.setWheelRotation(targetRotation);
        wheel.setHighlight(reward.getRarity().ordinal());
        card.setAlpha(1f);
        card.setTranslationY(0f);
        card.setScaleX(1f);
        card.setScaleY(1f);
        ((TextView) findViewById(R.id.roulette_hint)).setText(
                reward.getArtifact() != null ? getString(R.string.chest_done_hint) : "");
        doneButton.setAlpha(0f);
        doneButton.setVisibility(View.VISIBLE);
        doneButton.animate().alpha(1f).setDuration(UiTokens.DURATION_SHORT_MS).start();
    }
}
