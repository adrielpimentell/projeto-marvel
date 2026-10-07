package com.example.marvel.ui.season;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.marvel.R;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Season;
import com.example.marvel.game.SeasonPrize;
import com.example.marvel.game.SeasonResult;
import com.example.marvel.ui.common.ArtifactUi;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.Screens;
import com.example.marvel.ui.ranks.ChestOpenActivity;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SeasonEndActivity extends AppCompatActivity {

    private static final DateTimeFormatter DAY_MONTH =
            DateTimeFormatter.ofPattern("dd/MM", Locale.ROOT).withZone(Season.ZONE);
    private static final int[] PODIUM_COLORS = {R.color.seal_gold, R.color.seal_silver, R.color.seal_bronze};

    private static boolean shownThisSession;

    public static Intent newIntent(Context context) {
        return new Intent(context, SeasonEndActivity.class);
    }

    public static void showIfPending(Context context) {
        PlayerStore store = PlayerStore.getInstance(context);
        if (shownThisSession || store.get().getSeasonResult() == null || !store.get().hasSeasonChest()) {
            return;
        }
        context.startActivity(newIntent(context));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlayerStore store = PlayerStore.getInstance(this);
        SeasonResult result = store.get().getSeasonResult();
        if (result == null || !store.get().hasSeasonChest()) {
            finish();
            return;
        }
        shownThisSession = true;
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_season_end);
        Screens.padForSystemBars(findViewById(R.id.season_end_root));
        bind(result);

        findViewById(R.id.season_end_open).setOnClickListener(v -> {
            startActivity(ChestOpenActivity.newSeasonIntent(this));
            finish();
        });
    }

    private void bind(SeasonResult result) {
        Instant start = Season.startOfId(result.getSeason());
        Instant end = Season.endOf(start).minusSeconds(1);
        ((TextView) findViewById(R.id.season_end_week)).setText(getString(R.string.season_end_week,
                DAY_MONTH.format(start), DAY_MONTH.format(end)));

        int position = result.getPosition();
        boolean podium = SeasonPrize.isPodium(position);
        TextView positionView = findViewById(R.id.season_end_position);
        positionView.setText(getString(R.string.season_end_position, PlayerHud.format(position)));
        positionView.setTextColor(getColor(podium ? PODIUM_COLORS[position - 1] : R.color.text_primary));
        ImageView medal = findViewById(R.id.season_end_medal);
        medal.setColorFilter(getColor(podium ? PODIUM_COLORS[position - 1] : R.color.text_secondary));

        ((TextView) findViewById(R.id.season_end_trophies)).setText(
                getString(R.string.season_end_trophies, PlayerHud.format(result.getTrophies())));

        int rarityColor = ArtifactUi.rarityColor(this, result.getRarity());
        ((ImageView) findViewById(R.id.season_end_chest)).setColorFilter(rarityColor);
        TextView chestName = findViewById(R.id.season_end_chest_name);
        chestName.setText(getString(R.string.season_end_chest, ArtifactUi.rarityName(this, result.getRarity())));
        chestName.setTextColor(rarityColor);
        TextView coins = findViewById(R.id.season_end_coins);
        if (result.getCoins() > 0) {
            coins.setText(getString(R.string.season_end_coins, PlayerHud.format(result.getCoins())));
        } else {
            coins.setVisibility(View.GONE);
        }
    }
}
