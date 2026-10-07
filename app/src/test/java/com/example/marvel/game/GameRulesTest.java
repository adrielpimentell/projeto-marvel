package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

public class GameRulesTest {

    @Test
    public void victoryCoinsGoFrom100To500ByEnemyStrength() {
        assertEquals(100, Economy.coinsForVictory(40, 10));
        assertEquals(100, Economy.coinsForVictory(40, 20));
        assertEquals(300, Economy.coinsForVictory(40, 40));
        assertEquals(500, Economy.coinsForVictory(40, 60));
        assertEquals(500, Economy.coinsForVictory(40, 99));
        assertEquals(0, GameBalance.DEFEAT_COINS);
    }

    @Test
    public void upgradeCostGrowsBy75EachTime() {
        assertEquals(75, Economy.upgradeCost(0));
        assertEquals(150, Economy.upgradeCost(1));
        assertEquals(225, Economy.upgradeCost(2));
        assertEquals(75, Economy.upgradeCost(-3));
    }

    @Test
    public void battleGivesPlusTenOrMinusFiveAndNeverBelowZero() {
        PlayerState state = PlayerState.newGame();
        state.ensureSeason("2026-W41");
        state.applyBattle(result(true, 300));
        assertEquals(10, state.getTrophies());
        assertEquals(300, state.getCoins());
        state.applyBattle(result(false, 0));
        assertEquals(5, state.getTrophies());
        state.applyBattle(result(false, 0));
        state.applyBattle(result(false, 0));
        assertEquals(0, state.getTrophies());
        assertEquals(1, state.getWins());
        assertEquals(3, state.getLosses());
    }

    @Test
    public void seasonFlipsAtMondayMidnightInBrasiliaAcrossTheYear() {
        assertEquals("2026-W52", Season.idAt(Instant.parse("2026-12-28T02:59:59Z")));
        assertEquals("2026-W53", Season.idAt(Instant.parse("2026-12-28T03:00:00Z")));
        assertEquals("2026-W53", Season.idAt(Instant.parse("2027-01-04T02:59:59Z")));
        assertEquals("2027-W01", Season.idAt(Instant.parse("2027-01-04T03:00:00Z")));
    }

    @Test
    public void seasonNeverGoesBackWhenTheClockIsMovedBack() {
        PlayerState state = PlayerState.newGame();
        state.ensureSeason("2026-W42");
        state.applyBattle(result(true, 0));

        assertFalse(state.ensureSeason("2026-W41"));
        assertEquals("2026-W42", state.getSeason());
        assertEquals(10, state.getTrophies());
        assertTrue(state.ensureSeason("2026-W43"));
        assertEquals(0, state.getTrophies());
    }

    @Test
    public void dailyKeyKeepsGrowingAcrossTheYear() {
        assertTrue(DailyClock.toKey(LocalDate.of(2027, 1, 1)) > DailyClock.toKey(LocalDate.of(2026, 12, 31)));
    }

    @Test
    public void brokenSeasonResultInTheSaveIsDropped() {
        String json = "{\"seasonChestRarities\":[\"EPIC\"],"
                + "\"seasonResult\":{\"season\":\"semana\",\"position\":2,\"trophies\":40,\"rarity\":\"EPIC\",\"coins\":250}}";
        PlayerState state = new Gson().fromJson(json, PlayerState.class);
        state.repair();
        assertNull(state.getSeasonResult());
        assertTrue(state.hasSeasonChest());
    }

    @Test
    public void validSeasonResultSurvivesRepair() {
        PlayerState state = PlayerState.newGame();
        state.grantSeasonPrize(SeasonResult.of("2026-W41", 1, 900));
        PlayerState reloaded = new Gson().fromJson(new Gson().toJson(state), PlayerState.class);
        reloaded.repair();
        assertEquals(1, reloaded.getSeasonResult().getPosition());
        assertEquals(Artifact.Rarity.LEGENDARY, reloaded.getSeasonResult().getRarity());
    }

    private static BattleResult result(boolean won, int coins) {
        int trophies = won ? GameBalance.WIN_TROPHIES : GameBalance.LOSS_TROPHIES;
        return new BattleResult(won, Collections.emptyList(), 100, 100, null, null, trophies, coins);
    }
}
