package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SeasonEndTest {

    @Test
    public void prizeIsGrantedOnlyOncePerSeason() {
        PlayerState state = PlayerState.newGame();
        int coinsBefore = state.getCoins();

        assertTrue(state.grantSeasonPrize(SeasonResult.of("2026-W41", 1, 900)));
        assertFalse(state.grantSeasonPrize(SeasonResult.of("2026-W41", 1, 900)));

        assertEquals(coinsBefore + 500, state.getCoins());
        assertNotNull(state.openSeasonChest(new Random(1)));
        assertNull(state.openSeasonChest(new Random(1)));
    }

    @Test
    public void seasonChestGuaranteesItsRarity() {
        for (Artifact.Rarity rarity : Artifact.Rarity.values()) {
            for (int seed = 0; seed < 20; seed++) {
                ChestReward reward = Chests.ofRarity(0, rarity, new ArrayList<>(), new Random(seed));
                assertNotNull(reward.getArtifact());
                assertEquals(rarity, reward.getArtifact().getRarity());
            }
        }
    }

    @Test
    public void seasonChestBecomesCoinsWhenAllOfThatRarityAreOwned() {
        List<String> owned = new ArrayList<>();
        for (Artifact artifact : ArtifactCatalog.ofRarity(Artifact.Rarity.LEGENDARY)) {
            owned.add(artifact.getId());
        }
        ChestReward reward = Chests.ofRarity(0, Artifact.Rarity.LEGENDARY, owned, new Random(3));
        assertNull(reward.getArtifact());
        assertEquals(GameBalance.DUPLICATE_COINS[Artifact.Rarity.LEGENDARY.ordinal()], reward.getCoins());
    }

    @Test
    public void positionPicksTheRightChest() {
        assertEquals(Artifact.Rarity.EPIC, SeasonResult.of("2026-W41", 2, 500).getRarity());
        assertEquals(250, SeasonResult.of("2026-W41", 2, 500).getCoins());
        assertEquals(Artifact.Rarity.COMMON, SeasonResult.of("2026-W41", 17, 40).getRarity());
        assertEquals(0, SeasonResult.of("2026-W41", 17, 40).getCoins());
    }

    @Test
    public void resultIsClearedWhenTheChestIsOpened() {
        PlayerState state = PlayerState.newGame();
        state.grantSeasonPrize(SeasonResult.of("2026-W41", 3, 300));
        assertTrue(state.hasSeasonChest());
        assertNotNull(state.getSeasonResult());

        ChestReward reward = state.openSeasonChest(new Random(2));
        assertEquals(Artifact.Rarity.RARE, reward.getRarity());
        assertFalse(state.hasSeasonChest());
        assertNull(state.getSeasonResult());
    }

    @Test
    public void seasonIdTurnsBackIntoItsMonday() {
        assertEquals(Instant.parse("2026-10-05T03:00:00Z"), Season.startOfId("2026-W41"));
        assertEquals(Instant.parse("2026-12-28T03:00:00Z"), Season.startOfId("2026-W53"));
        assertEquals(Instant.parse("2027-01-04T03:00:00Z"), Season.startOfId("2027-W01"));
    }

    @Test
    public void seasonsCompareInOrder() {
        assertTrue(Season.isBefore("2026-W40", "2026-W41"));
        assertTrue(Season.isBefore("2026-W53", "2027-W01"));
        assertFalse(Season.isBefore("2026-W41", "2026-W41"));
    }
}
