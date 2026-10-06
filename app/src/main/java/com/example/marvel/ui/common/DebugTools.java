package com.example.marvel.ui.common;

import android.view.View;

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
        bar.findViewById(R.id.debug_add_coins).setOnClickListener(v -> {
            store.addCoins(1000);
            onChanged.run();
        });
        bar.findViewById(R.id.debug_add_coins_big).setOnClickListener(v -> {
            store.addCoins(50_000);
            onChanged.run();
        });
    }
}
