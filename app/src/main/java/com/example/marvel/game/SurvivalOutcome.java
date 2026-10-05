package com.example.marvel.game;

import java.util.Collections;
import java.util.List;

public final class SurvivalOutcome {

    private final boolean won;
    private final int floor;
    private final int coins;
    private final boolean newRecord;
    private final boolean chestUnlocked;
    private final List<DailyChallenge> dailyCompleted;

    SurvivalOutcome(boolean won, int floor, int coins, boolean newRecord, boolean chestUnlocked,
                    List<DailyChallenge> dailyCompleted) {
        this.won = won;
        this.floor = floor;
        this.coins = coins;
        this.newRecord = newRecord;
        this.chestUnlocked = chestUnlocked;
        this.dailyCompleted = Collections.unmodifiableList(dailyCompleted);
    }

    public boolean isWon() {
        return won;
    }

    public int getFloor() {
        return floor;
    }

    public int getCoins() {
        return coins;
    }

    public boolean isNewRecord() {
        return newRecord;
    }

    public boolean isChestUnlocked() {
        return chestUnlocked;
    }

    public List<DailyChallenge> getDailyCompleted() {
        return dailyCompleted;
    }
}
