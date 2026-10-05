package com.example.marvel.game;

import java.io.Serializable;

public final class RouletteReward implements Serializable {

    private final Roulette roulette;
    private final Artifact.Rarity rarity;
    private final String artifactId;
    private final int coins;

    RouletteReward(Roulette roulette, Artifact.Rarity rarity, String artifactId, int coins) {
        this.roulette = roulette;
        this.rarity = rarity;
        this.artifactId = artifactId;
        this.coins = coins;
    }

    public Roulette getRoulette() {
        return roulette;
    }

    public Artifact.Rarity getRarity() {
        return rarity;
    }

    public Artifact getArtifact() {
        return artifactId == null ? null : ArtifactCatalog.byId(artifactId);
    }

    public int getCoins() {
        return coins;
    }
}
