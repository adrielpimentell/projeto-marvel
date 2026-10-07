package com.example.marvel.game;

public final class SeasonResult {

    private String season;
    private int position;
    private int trophies;
    private String rarity;
    private int coins;

    private SeasonResult() {
    }

    public static SeasonResult of(String season, int position, int trophies) {
        SeasonPrize prize = SeasonPrize.forPosition(position);
        SeasonResult result = new SeasonResult();
        result.season = season;
        result.position = position;
        result.trophies = trophies;
        result.rarity = prize.getRarity().name();
        result.coins = prize.getCoins();
        return result;
    }

    boolean isValid() {
        if (season == null || !season.matches("\\d{4}-W\\d{2}") || position < 1 || coins < 0) return false;
        for (Artifact.Rarity value : Artifact.Rarity.values()) {
            if (value.name().equals(rarity)) return true;
        }
        return false;
    }

    public String getSeason() {
        return season;
    }

    public int getPosition() {
        return position;
    }

    public int getTrophies() {
        return trophies;
    }

    public Artifact.Rarity getRarity() {
        return Artifact.Rarity.valueOf(rarity);
    }

    public int getCoins() {
        return coins;
    }
}
