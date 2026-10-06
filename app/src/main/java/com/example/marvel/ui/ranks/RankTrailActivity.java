package com.example.marvel.ui.ranks;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.example.marvel.ui.common.Cards;
import com.example.marvel.ui.common.UiTokens;
import com.example.marvel.ui.common.Screens;
import com.example.marvel.R;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Ranks;
import com.example.marvel.ui.common.DebugTools;
import com.example.marvel.ui.common.PlayerHud;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class RankTrailActivity extends AppCompatActivity {


    private PlayerStore playerStore;
    private TrailMath math;

    private View playerHud;
    private HorizontalScrollView scroll;
    private View fill;
    private View marker;
    private TextView markerLabel;
    private TextView totalValue;
    private TextView totalHint;
    private final List<View> cards = new ArrayList<>();

    private float shownMarkerX = -1f;
    private ValueAnimator fillAnimator;
    private ValueAnimator chestPulse;
    private final List<View> pulsingChests = new ArrayList<>();
    private final Rect fillClip = new Rect();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_rank_trail);

        playerStore = PlayerStore.getInstance(this);
        Resources res = getResources();
        math = new TrailMath(res.getDimension(R.dimen.trail_side_padding),
                res.getDimension(R.dimen.trail_column_width));

        View root = findViewById(R.id.trail_root);
        Screens.padForSystemBars(root);

        playerHud = findViewById(R.id.player_hud);
        scroll = findViewById(R.id.trail_scroll);
        fill = findViewById(R.id.trail_fill);
        marker = findViewById(R.id.trail_marker);
        markerLabel = findViewById(R.id.trail_marker_label);
        totalValue = findViewById(R.id.trail_total_value);
        totalHint = findViewById(R.id.trail_total_hint);

        buildTrail();

        findViewById(R.id.bottom_nav).setVisibility(View.GONE);
        findViewById(R.id.trail_back).setOnClickListener(v -> finish());
        DebugTools.setup(findViewById(R.id.debug_bar), playerStore, this::refresh);
        playerHud.findViewById(R.id.hud_rank).setOnClickListener(v -> scrollToMarker(true));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onStop() {
        stopChestPulses();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (fillAnimator != null) fillAnimator.cancel();
        stopChestPulses();
        super.onDestroy();
    }

    private void buildTrail() {
        Resources res = getResources();
        int columnWidth = res.getDimensionPixelSize(R.dimen.trail_column_width);
        int cardWidth = res.getDimensionPixelSize(R.dimen.trail_card_width);
        int sideMargin = (columnWidth - cardWidth) / 2;
        int barHeight = res.getDimensionPixelSize(R.dimen.trail_bar_height);

        LinearLayout cardsRow = findViewById(R.id.trail_cards);
        LinearLayout thresholdsRow = findViewById(R.id.trail_thresholds);
        FrameLayout ticks = findViewById(R.id.trail_ticks);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int rank = 0; rank < Ranks.count(); rank++) {
            View card = inflater.inflate(R.layout.item_rank_card, cardsRow, false);
            LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) card.getLayoutParams();
            cardParams.leftMargin = sideMargin;
            cardParams.rightMargin = sideMargin;
            cardsRow.addView(card, cardParams);
            cards.add(card);

            TextView threshold = new TextView(this);
            threshold.setLayoutParams(new LinearLayout.LayoutParams(columnWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            threshold.setGravity(Gravity.CENTER);
            TextViewCompat.setTextAppearance(threshold, R.style.TextAppearance_Marvel_BodySmall);
            threshold.setTextColor(getColor(R.color.text_muted));
            String number = PlayerHud.format(Ranks.minTrophies(rank));
            threshold.setText(Ranks.isLast(rank) ? getString(R.string.trail_last_threshold, number) : number);
            thresholdsRow.addView(threshold);

            if (rank > 0) {
                View tick = new View(this);
                int tickWidth = Math.max(2, Math.round(3 * res.getDisplayMetrics().density));
                FrameLayout.LayoutParams tickParams = new FrameLayout.LayoutParams(tickWidth, barHeight);
                tickParams.leftMargin = Math.round(math.centerOf(rank)) - tickWidth / 2;
                tick.setBackgroundColor(getColor(R.color.bg));
                ticks.addView(tick, tickParams);
            }
        }

        int trackWidth = Math.round(math.trackEnd() - math.trackStart());
        View track = findViewById(R.id.trail_track);
        ViewGroup.LayoutParams trackParams = track.getLayoutParams();
        trackParams.width = trackWidth;
        track.setLayoutParams(trackParams);
        ViewGroup.LayoutParams fillParams = fill.getLayoutParams();
        fillParams.width = trackWidth;
        fill.setLayoutParams(fillParams);
        fillClip.set(0, 0, 1, barHeight);
        fill.setClipBounds(fillClip);

        View content = findViewById(R.id.trail_content);
        content.setMinimumWidth(Math.round(math.contentWidth()));
    }

    private void refresh() {
        PlayerState state = playerStore.get();
        PlayerHud.bind(playerHud, state);
        bindCards(state);
        bindTotal(state);
        animateMarkerTo(math.markerX(state.getTrophies()), state.getTrophies());
    }

    private void bindCards(PlayerState state) {
        int currentRank = state.getRank();
        stopChestPulses();

        for (int rank = 0; rank < cards.size(); rank++) {
            View card = cards.get(rank);
            String name = PlayerHud.rankName(this, rank);
            boolean reached = rank <= currentRank;
            boolean current = rank == currentRank;
            boolean chestReady = state.isChestAvailable(rank);
            boolean chestOpened = state.isChestOpened(rank);

            ((TextView) card.findViewById(R.id.rank_number))
                    .setText(getString(R.string.trail_rank_number, rank + 1));
            TextView nameView = card.findViewById(R.id.rank_name);
            nameView.setText(name);
            nameView.setTextColor(getColor(current ? R.color.red : R.color.text_primary));
            ((TextView) card.findViewById(R.id.rank_description))
                    .setText(PlayerHud.rankDescription(this, rank));

            ImageView chest = card.findViewById(R.id.rank_chest);
            View lock = card.findViewById(R.id.rank_chest_lock);
            View openButton = card.findViewById(R.id.rank_chest_open);
            TextView chestLabel = card.findViewById(R.id.rank_chest_label);
            String chestText;
            chest.setScaleX(1f);
            chest.setScaleY(1f);
            openButton.setVisibility(View.GONE);
            chestLabel.setVisibility(View.VISIBLE);
            if (rank == 0) {
                chest.setVisibility(View.INVISIBLE);
                lock.setVisibility(View.GONE);
                chestText = getString(R.string.trail_chest_none);
                chestLabel.setTextColor(getColor(R.color.text_secondary));
            } else if (chestReady) {
                chest.setVisibility(View.VISIBLE);
                chest.setImageResource(R.drawable.ic_chest);
                chest.setColorFilter(getColor(R.color.red));
                lock.setVisibility(View.GONE);
                chestText = getString(R.string.trail_chest_ready);
                chestLabel.setVisibility(View.GONE);
                openButton.setVisibility(View.VISIBLE);
                final int chestRank = rank;
                openButton.setOnClickListener(v ->
                        startActivity(ChestOpenActivity.newIntent(this, chestRank)));
                pulse(chest);
            } else if (chestOpened) {
                chest.setVisibility(View.VISIBLE);
                chest.setImageResource(R.drawable.ic_chest_body);
                chest.setColorFilter(getColor(R.color.icon_inactive));
                lock.setVisibility(View.GONE);
                chestText = getString(R.string.trail_chest_opened);
                chestLabel.setTextColor(getColor(R.color.text_secondary));
            } else {
                chest.setVisibility(View.VISIBLE);
                chest.setImageResource(R.drawable.ic_chest);
                chest.setColorFilter(getColor(R.color.icon_inactive));
                lock.setVisibility(View.VISIBLE);
                chestText = getString(R.string.trail_chest_locked);
                chestLabel.setTextColor(getColor(R.color.text_secondary));
            }
            chestLabel.setText(chestText);

            Cards.highlight((MaterialCardView) card, chestReady);
            card.setAlpha(reached || chestReady ? 1f : UiTokens.ALPHA_NOT_REACHED);
            card.setContentDescription(getString(R.string.trail_card_description, name,
                    PlayerHud.format(Ranks.minTrophies(rank)), chestText));
        }
    }

    private void pulse(View chest) {
        pulsingChests.add(chest);
        if (chestPulse != null) return;
        chestPulse = ValueAnimator.ofFloat(1f, UiTokens.PULSE_SCALE);
        chestPulse.setDuration(UiTokens.DURATION_PULSE_MS);
        chestPulse.setRepeatMode(ValueAnimator.REVERSE);
        chestPulse.setRepeatCount(ValueAnimator.INFINITE);
        chestPulse.addUpdateListener(a -> {
            float scale = (float) a.getAnimatedValue();
            for (View view : pulsingChests) {
                view.setScaleX(scale);
                view.setScaleY(scale);
            }
        });
        chestPulse.start();
    }

    private void stopChestPulses() {
        if (chestPulse != null) chestPulse.cancel();
        chestPulse = null;
        for (View view : pulsingChests) {
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
        pulsingChests.clear();
    }

    private void bindTotal(PlayerState state) {
        int trophies = state.getTrophies();
        totalValue.setText(PlayerHud.format(trophies));
        int rank = state.getRank();
        totalHint.setText(Ranks.isLast(rank)
                ? getString(R.string.trail_max)
                : getString(R.string.trail_next, PlayerHud.format(Ranks.trophiesToNext(trophies)),
                        PlayerHud.rankName(this, rank + 1)));
        markerLabel.setText(getString(R.string.trail_you, PlayerHud.format(trophies)));
    }

    private void animateMarkerTo(float targetX, int trophies) {
        if (fillAnimator != null) fillAnimator.cancel();
        float startX = shownMarkerX < 0 ? math.trackStart() : shownMarkerX;
        fillAnimator = ValueAnimator.ofFloat(startX, targetX);
        fillAnimator.setDuration(UiTokens.DURATION_FILL_MS);
        fillAnimator.setInterpolator(new DecelerateInterpolator());
        fillAnimator.addUpdateListener(a -> placeMarker((float) a.getAnimatedValue()));
        fillAnimator.start();
        shownMarkerX = targetX;
        scroll.post(() -> scrollToMarker(true));
    }

    private void placeMarker(float x) {
        fillClip.right = Math.max(1, Math.round(x - math.trackStart()));
        fill.setClipBounds(fillClip);

        marker.setTranslationX(x - marker.getWidth() / 2f);
        float labelX = x - markerLabel.getWidth() / 2f;
        float maxX = math.contentWidth() - markerLabel.getWidth();
        markerLabel.setTranslationX(Math.max(0f, Math.min(maxX, labelX)));
    }

    void scrollToMarker(boolean smooth) {
        if (shownMarkerX < 0) return;
        int target = Math.max(0, Math.round(shownMarkerX - scroll.getWidth() / 2f));
        if (smooth) scroll.smoothScrollTo(target, 0);
        else scroll.scrollTo(target, 0);
    }
}
