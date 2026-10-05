package com.example.marvel.game;

import java.util.Collection;
import java.util.Random;

public final class Chests {

    private Chests() {
    }

    public static boolean hasChest(int rank) {
        return rank >= 1 && rank < Ranks.count();
    }

    static Artifact.Rarity rollRarity(int rank, Random random) {
        int[] weights = GameBalance.CHEST_RARITY_WEIGHTS[Ranks.clamp(rank)];
        return Artifact.Rarity.values()[WeightedDraw.index(weights, random)];
    }

    static ChestReward roll(int rank, Collection<String> ownedArtifactIds, Random random) {
        Artifact.Rarity rarity = rollRarity(rank, random);
        Artifact prize = ArtifactDrops.pick(rarity, ownedArtifactIds, random);
        if (prize == null) {
            return new ChestReward(rank, rarity, null, ArtifactDrops.duplicateCoins(rarity));
        }
        return new ChestReward(rank, rarity, prize.getId(), 0);
    }
}
