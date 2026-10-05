package com.example.marvel.game;

import java.io.Serializable;

public final class ChestReward implements Serializable {

    private final int rank;
    private final Artifact.Rarity rarity;
    private final String artifactId;
    private final int coins;

    ChestReward(int rank, Artifact.Rarity rarity, String artifactId, int coins) {
        this.rank = rank;
        this.rarity = rarity;
        this.artifactId = artifactId;
        this.coins = coins;
    }

    public int getRank() {
        return rank;
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
