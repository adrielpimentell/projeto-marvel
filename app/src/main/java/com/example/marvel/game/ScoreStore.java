package com.example.marvel.game;

import android.content.Context;
import android.content.SharedPreferences;

public class ScoreStore {

    private static final String PREFS_NAME = "marvel_battle";
    private static final String KEY_SCORE = "score";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";

    private final SharedPreferences prefs;

    public ScoreStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getScore() {
        return prefs.getInt(KEY_SCORE, 0);
    }

    public int getWins() {
        return prefs.getInt(KEY_WINS, 0);
    }

    public int getLosses() {
        return prefs.getInt(KEY_LOSSES, 0);
    }
}
