package com.example.marvel.ui.common;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.example.marvel.ui.season.SeasonEndActivity;
import com.example.marvel.game.Season;
import com.example.marvel.data.season.SeasonRepository;
import com.example.marvel.BuildConfig;
import com.example.marvel.R;
import com.example.marvel.game.PlayerStore;

public final class DebugTools {

    public static final boolean ENABLED = BuildConfig.DEBUG;

    private static final int DEBUG_TROPHIES = 100;

    public static boolean chestSlowMotion;

    private DebugTools() {
    }

    public static void setup(View bar, PlayerStore store, Runnable onChanged) {
        if (!ENABLED) {
            bar.setVisibility(View.GONE);
            return;
        }
        bar.setVisibility(View.VISIBLE);
        bar.findViewById(R.id.debug_add_trophies).setOnClickListener(v -> {
            store.ensureSeason(Season.currentId());
            store.addDebugTrophies(DEBUG_TROPHIES);
            SeasonRepository.getInstance(v.getContext()).sync();
            onChanged.run();
        });
        bar.findViewById(R.id.debug_end_season).setOnClickListener(v -> endSeason(v, store, onChanged));
        bar.findViewById(R.id.debug_add_coins).setOnClickListener(v -> {
            store.addCoins(1000);
            onChanged.run();
        });
        bar.findViewById(R.id.debug_add_coins_big).setOnClickListener(v -> {
            store.addCoins(50_000);
            onChanged.run();
        });
    }

    private static void endSeason(View button, PlayerStore store, Runnable onChanged) {
        Context context = button.getContext();
        button.setEnabled(false);
        Season.debugAdvanceWeek();
        store.ensureSeason(Season.currentId());
        onChanged.run();
        SeasonRepository.getInstance(context).checkSeasonEnd(new SeasonRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean granted) {
                button.setEnabled(true);
                onChanged.run();
                if (granted) {
                    context.startActivity(SeasonEndActivity.newIntent(context));
                } else {
                    Toast.makeText(context, R.string.debug_end_season_none, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(int messageRes) {
                button.setEnabled(true);
                Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show();
            }
        });
    }
}
