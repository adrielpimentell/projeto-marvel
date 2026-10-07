package com.example.marvel.ui.daily;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.example.marvel.R;
import com.example.marvel.game.DailyChallenges;
import com.example.marvel.game.DailyClock;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.ranks.ChestOpenActivity;
import com.example.marvel.ui.survival.SurvivalActivity;

import java.time.Duration;

public final class DailyHeader implements DailyChallengesAdapter.Listener {

    private static final long TICK_MS = 30_000;

    private final Activity activity;
    private final PlayerStore playerStore;
    private final DailyChallengesAdapter adapter = new DailyChallengesAdapter(this);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            DailyChallenges daily = playerStore.get().getDaily();
            if (daily == null || daily.getDay() < DailyClock.today()) {
                refresh();
            } else {
                updateCountdown();
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    public DailyHeader(Activity activity, PlayerStore playerStore) {
        this.activity = activity;
        this.playerStore = playerStore;
    }

    public DailyChallengesAdapter getAdapter() {
        return adapter;
    }

    public void setVisible(boolean visible) {
        adapter.setVisible(visible);
    }

    public void onResume() {
        refresh();
        handler.postDelayed(tick, TICK_MS);
    }

    public void onPause() {
        handler.removeCallbacks(tick);
    }

    private void refresh() {
        playerStore.ensureDailyChallenges(DailyClock.today());
        adapter.bind(playerStore.get());
        updateCountdown();
    }

    private void updateCountdown() {
        Duration left = DailyClock.untilNextDay();
        long hours = left.toHours();
        int minutes = Math.max(1, left.toMinutesPart());
        adapter.setCountdown(hours > 0
                ? activity.getString(R.string.daily_new_in, hours, minutes)
                : activity.getString(R.string.daily_new_in_minutes, minutes));
    }

    @Override
    public void onOpenDailyChest() {
        activity.startActivity(ChestOpenActivity.newDailyIntent(activity));
    }

    @Override
    public void onOpenSurvival() {
        activity.startActivity(SurvivalActivity.newIntent(activity));
    }
}
