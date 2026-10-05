package com.example.marvel.game;

public final class Economy {

    private Economy() {
    }

    public static int coinsForVictory(int heroOverall, int enemyOverall) {
        double ratio = (double) Math.max(0, enemyOverall) / Math.max(1, heroOverall);
        double progress = (ratio - GameBalance.RATIO_FOR_MIN_COINS)
                / (GameBalance.RATIO_FOR_MAX_COINS - GameBalance.RATIO_FOR_MIN_COINS);
        progress = Math.max(0, Math.min(1, progress));
        return (int) Math.round(GameBalance.VICTORY_COINS_MIN
                + progress * (GameBalance.VICTORY_COINS_MAX - GameBalance.VICTORY_COINS_MIN));
    }

    public static int heroPrice(int overall) {
        double raw = GameBalance.PRICE_BASE
                + GameBalance.PRICE_PER_OVERALL_SQUARED * overall * overall;
        return (int) (Math.round(raw / GameBalance.PRICE_ROUNDING) * GameBalance.PRICE_ROUNDING);
    }

    public static int upgradeCost(int upgradesDone) {
        return GameBalance.UPGRADE_BASE_COST * (Math.max(0, upgradesDone) + 1);
    }
}
