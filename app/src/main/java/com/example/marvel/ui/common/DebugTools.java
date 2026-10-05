package com.example.marvel.ui.common;

import android.view.View;

import com.example.marvel.BuildConfig;
import com.example.marvel.R;
import com.example.marvel.game.PlayerStore;

public final class DebugTools {

    public static final boolean ENABLED = BuildConfig.DEBUG;

    private static final int TROPHIES_SMALL = 1_000;
    private static final int TROPHIES_BIG = 10_000;

    public static boolean chestSlowMotion;

    private DebugTools() {
    }

    public static void setup(View bar, PlayerStore store, Runnable onChanged) {
        if (!ENABLED) {
            bar.setVisibility(View.GONE);
            return;
        }
        bar.setVisibility(View.VISIBLE);
        bar.findViewById(R.id.debug_add_trophies_small).setOnClickListener(v -> {
            store.addTrophies(TROPHIES_SMALL);
            onChanged.run();
        });
        bar.findViewById(R.id.debug_add_trophies_big).setOnClickListener(v -> {
            store.addTrophies(TROPHIES_BIG);
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
