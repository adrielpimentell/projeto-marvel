package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TrophySyncTest {

    private static final String SEASON = "2026-W41";

    @Test
    public void firstWinCreatesWithTen() {
        TrophySync sync = TrophySync.between(SEASON, null, 10);
        assertTrue(sync.isCreate());
        assertEquals(Collections.singletonList(10), sync.getValues());
    }

    @Test
    public void firstLossCreatesWithZero() {
        TrophySync sync = TrophySync.between(SEASON, null, 0);
        assertTrue(sync.isCreate());
        assertEquals(Collections.singletonList(0), sync.getValues());
    }

    @Test
    public void everyStepIsPlusTenOrMinusFive() {
        assertEquals(Arrays.asList(10, 5), TrophySync.between(SEASON, null, 5).getValues());
        assertEquals(Arrays.asList(40, 50, 60, 70, 80, 90, 100, 110, 120, 130),
                TrophySync.between(SEASON, 30, 130).getValues());
        assertEquals(Arrays.asList(75, 70), TrophySync.between(SEASON, 80, 70).getValues());
    }

    @Test
    public void lossAtFiveGoesToZero() {
        assertEquals(Collections.singletonList(0), TrophySync.between(SEASON, 5, 0).getValues());
    }

    @Test
    public void nothingToSendWhenServerIsUpToDate() {
        assertTrue(TrophySync.between(SEASON, 40, 40).isEmpty());
    }

    @Test
    public void newSeasonResetsTrophiesAndTrailChests() {
        PlayerState state = PlayerState.newGame();
        state.ensureSeason("2026-W40");
        state.addDebugTrophies(200);
        state.openChest(1, new java.util.Random(1));
        assertEquals(200, state.getTrophies());

        assertTrue(state.ensureSeason("2026-W41"));
        assertEquals(0, state.getTrophies());
        assertEquals(0, state.getBestRank());
        assertFalse(state.isChestOpened(1));
        assertFalse(state.isSeasonPlayed());
        assertFalse(state.ensureSeason("2026-W41"));
    }

    @Test
    public void claimSendsOnlyWhatChangedSinceLastSync() {
        PlayerState state = PlayerState.newGame();
        state.ensureSeason(SEASON);
        assertTrue(state.claimTrophySync().isEmpty());

        state.addDebugTrophies(20);
        TrophySync first = state.claimTrophySync();
        assertTrue(first.isCreate());
        assertEquals(Arrays.asList(10, 20), first.getValues());
        assertTrue(state.claimTrophySync().isEmpty());
    }

    @Test
    public void serverValueWins() {
        PlayerState state = PlayerState.newGame();
        state.ensureSeason(SEASON);
        state.addDebugTrophies(50);
        state.claimTrophySync();

        assertTrue(state.adoptServerTrophies(SEASON, 30));
        assertEquals(30, state.getTrophies());
        assertTrue(state.claimTrophySync().isEmpty());
        assertFalse(state.adoptServerTrophies("2026-W40", 90));
    }

    @Test
    public void missingServerDocIsRecreated() {
        PlayerState state = PlayerState.newGame();
        state.ensureSeason(SEASON);
        state.addDebugTrophies(10);
        state.claimTrophySync();

        assertTrue(state.adoptServerTrophies(SEASON, null));
        TrophySync again = state.claimTrophySync();
        assertTrue(again.isCreate());
        assertEquals(Collections.singletonList(10), again.getValues());
    }
}
