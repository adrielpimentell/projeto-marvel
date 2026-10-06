package com.example.marvel.game;

public final class SeasonPrize {

    private final int position;
    private final Artifact.Rarity rarity;
    private final int coins;

    private SeasonPrize(int position, Artifact.Rarity rarity, int coins) {
        this.position = position;
        this.rarity = rarity;
        this.coins = coins;
    }

    public static SeasonPrize forPosition(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("position " + position);
        }
        int index = position - 1;
        if (index < GameBalance.SEASON_PRIZE_RARITIES.length) {
            return new SeasonPrize(position, GameBalance.SEASON_PRIZE_RARITIES[index],
                    GameBalance.SEASON_PRIZE_COINS[index]);
        }
        return new SeasonPrize(position, GameBalance.SEASON_PRIZE_OTHERS_RARITY, 0);
    }

    public static boolean isPodium(int position) {
        return position >= 1 && position <= GameBalance.SEASON_PRIZE_RARITIES.length;
    }

    public int getPosition() {
        return position;
    }

    public Artifact.Rarity getRarity() {
        return rarity;
    }

    public int getCoins() {
        return coins;
    }
}
