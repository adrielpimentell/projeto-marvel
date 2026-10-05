package com.example.marvel.ui.market;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.marvel.R;
import com.example.marvel.game.Artifact;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Roulette;
import com.example.marvel.game.RouletteReward;
import com.example.marvel.ui.common.ArtifactUi;
import com.example.marvel.ui.common.PlayerHud;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RoulettePanel {

    interface Host {
        void onCoinsChanged();

        void showMessage(String text);
    }

    private static final NumberFormat PERCENT_FORMAT =
            NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));

    static {
        PERCENT_FORMAT.setMaximumFractionDigits(1);
    }

    private final Activity activity;
    private final PlayerStore playerStore;
    private final Host host;
    private final List<View> cards = new ArrayList<>();
    private boolean spinInProgress;

    RoulettePanel(Activity activity, LinearLayout container, PlayerStore playerStore, Host host) {
        this.activity = activity;
        this.playerStore = playerStore;
        this.host = host;
        buildCards(container);
    }

    void onResume() {
        spinInProgress = false;
    }

    void bindButtons() {
        int coins = playerStore.get().getCoins();
        for (int i = 0; i < cards.size(); i++) {
            Roulette roulette = Roulette.values()[i];
            cards.get(i).findViewById(R.id.roulette_spin)
                    .setEnabled(!spinInProgress && coins >= roulette.getPrice());
        }
    }

    private void buildCards(LinearLayout container) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        int[] oddsColumns = {R.id.odds_common, R.id.odds_rare, R.id.odds_epic, R.id.odds_legendary};

        for (Roulette roulette : Roulette.values()) {
            View card = inflater.inflate(R.layout.item_roulette_card, container, false);
            int color = ArtifactUi.rouletteColor(activity, roulette);
            ((MaterialCardView) card).setStrokeColor(ColorStateList.valueOf(color));
            ((ImageView) card.findViewById(R.id.roulette_icon)).setColorFilter(color);
            ((TextView) card.findViewById(R.id.roulette_name)).setText(ArtifactUi.rouletteName(activity, roulette));
            ((TextView) card.findViewById(R.id.roulette_hint)).setText(ArtifactUi.rouletteHint(activity, roulette));
            ((TextView) card.findViewById(R.id.roulette_price)).setText(PlayerHud.format(roulette.getPrice()));

            StringBuilder oddsDescription = new StringBuilder();
            for (Artifact.Rarity rarity : Artifact.Rarity.values()) {
                View column = card.findViewById(oddsColumns[rarity.ordinal()]);
                String percent = activity.getString(R.string.roulette_percent,
                        PERCENT_FORMAT.format(roulette.oddsPerMille(rarity) / 10.0));
                String rarityName = ArtifactUi.rarityName(activity, rarity);
                TextView value = column.findViewById(R.id.stat_value);
                value.setText(percent);
                value.setTextColor(ArtifactUi.rarityColor(activity, rarity));
                column.findViewById(R.id.stat_bonus).setVisibility(View.GONE);
                ((TextView) column.findViewById(R.id.stat_label)).setText(rarityName);
                oddsDescription.append(activity.getString(R.string.roulette_odds_description, rarityName, percent))
                        .append(". ");
            }
            card.findViewById(R.id.roulette_odds).setContentDescription(oddsDescription.toString());
            card.findViewById(R.id.roulette_spin).setOnClickListener(v -> spin(roulette));
            container.addView(card);
            cards.add(card);
        }
    }

    private void spin(Roulette roulette) {
        if (spinInProgress) return;
        spinInProgress = true;
        bindButtons();

        RouletteReward reward = playerStore.spinRoulette(roulette);
        if (reward == null) {
            spinInProgress = false;
            host.onCoinsChanged();
            host.showMessage(activity.getString(R.string.roulette_no_coins));
            return;
        }
        host.onCoinsChanged();
        activity.startActivity(RouletteSpinActivity.newIntent(activity, reward));
    }
}
