package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SeasonPrizeTest {

    @Test
    public void podiumGetsChestAndCoins() {
        assertPrize(1, Artifact.Rarity.LEGENDARY, 500);
        assertPrize(2, Artifact.Rarity.EPIC, 250);
        assertPrize(3, Artifact.Rarity.RARE, 100);
    }

    @Test
    public void everyoneElseGetsACommonChest() {
        assertPrize(4, Artifact.Rarity.COMMON, 0);
        assertPrize(50, Artifact.Rarity.COMMON, 0);
        assertPrize(1234, Artifact.Rarity.COMMON, 0);
    }

    @Test
    public void podiumIsTheFirstThree() {
        assertTrue(SeasonPrize.isPodium(1));
        assertTrue(SeasonPrize.isPodium(3));
        assertFalse(SeasonPrize.isPodium(4));
        assertFalse(SeasonPrize.isPodium(0));
    }

    private static void assertPrize(int position, Artifact.Rarity rarity, int coins) {
        SeasonPrize prize = SeasonPrize.forPosition(position);
        assertEquals(rarity, prize.getRarity());
        assertEquals(coins, prize.getCoins());
    }
}
