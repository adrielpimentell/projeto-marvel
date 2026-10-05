package com.example.marvel.ui.ranks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.marvel.game.Artifact;

import org.junit.Test;

public class ChestAnimationTimingTest {

    @Test
    public void everyRarityLastsBetween2And5And4Seconds() {
        for (Artifact.Rarity rarity : Artifact.Rarity.values()) {
            long total = ChestAnimSpec.totalMs(rarity);
            assertTrue(rarity + ": " + total + " ms", total >= 2_500 && total <= 4_000);
        }
    }

    @Test
    public void rarerChestsBuildMoreSuspense() {
        long normal = ChestAnimSpec.suspenseMs(Artifact.Rarity.COMMON);
        assertTrue(ChestAnimSpec.suspenseMs(Artifact.Rarity.RARE) == normal);
        assertTrue(ChestAnimSpec.suspenseMs(Artifact.Rarity.EPIC) > normal);
        assertTrue(ChestAnimSpec.suspenseMs(Artifact.Rarity.LEGENDARY)
                > ChestAnimSpec.suspenseMs(Artifact.Rarity.EPIC));
        assertTrue(ChestAnimSpec.particles(Artifact.Rarity.EPIC)
                > ChestAnimSpec.particles(Artifact.Rarity.COMMON));
    }

    @Test
    public void particleCountIsBetween12And20() {
        for (Artifact.Rarity rarity : Artifact.Rarity.values()) {
            int count = ChestAnimSpec.particles(rarity);
            assertTrue(rarity + ": " + count, count >= 12 && count <= 20);
        }
    }

    @Test
    public void onlyLegendaryGetsTheExtras() {
        assertTrue(ChestAnimSpec.isLegendary(Artifact.Rarity.LEGENDARY));
        assertFalse(ChestAnimSpec.isLegendary(Artifact.Rarity.EPIC));
    }
}
