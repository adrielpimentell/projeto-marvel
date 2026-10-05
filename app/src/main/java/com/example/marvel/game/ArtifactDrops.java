package com.example.marvel.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public final class ArtifactDrops {

    private ArtifactDrops() {
    }

    static Artifact pick(Artifact.Rarity rarity, Collection<String> ownedIds, Random random) {
        List<Artifact> candidates = new ArrayList<>();
        for (Artifact artifact : ArtifactCatalog.ofRarity(rarity)) {
            if (!ownedIds.contains(artifact.getId())) candidates.add(artifact);
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    static int duplicateCoins(Artifact.Rarity rarity) {
        return GameBalance.DUPLICATE_COINS[rarity.ordinal()];
    }
}
