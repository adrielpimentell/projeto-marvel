package com.example.marvel.game;

import java.util.Random;

public enum Roulette {
    COMMON, RARE, EPIC, LEGENDARY;

    public int getPrice() {
        return GameBalance.ROULETTE_PRICES[ordinal()];
    }

    public int oddsPerMille(Artifact.Rarity rarity) {
        return GameBalance.ROULETTE_ODDS_PER_MILLE[ordinal()][rarity.ordinal()];
    }

    Artifact.Rarity rollRarity(Random random) {
        int[] odds = GameBalance.ROULETTE_ODDS_PER_MILLE[ordinal()];
        return Artifact.Rarity.values()[WeightedDraw.index(odds, random)];
    }
}
