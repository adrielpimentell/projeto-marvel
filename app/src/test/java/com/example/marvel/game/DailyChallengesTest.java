package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.marvel.data.model.Character;
import com.google.gson.Gson;

import org.junit.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DailyChallengesTest {

    private static final int DAY = 20260928;
    private static final Gson GSON = new Gson();

    private static PlayerState newPlayer() {
        return PlayerState.newGame();
    }

    private static PlayerState veteran() {
        PlayerState state = PlayerState.newGame();
        state.buy(OwnedHero.fromCharacter(character(1440, "{\"id\":3806,\"name\":\"Avengers\"},"
                + "{\"id\":3173,\"name\":\"X-Men\"}", "{\"id\":5,\"name\":\"Claws\"}", 1)), 0);
        state.buy(OwnedHero.fromCharacter(character(1444, "{\"id\":3173,\"name\":\"X-Men\"}",
                "{\"id\":9,\"name\":\"Flight\"}", 1)), 0);
        state.buy(OwnedHero.fromCharacter(character(1455, "",
                "{\"id\":9,\"name\":\"Flight\"}", 4)), 0);
        state.addCoins(1_000_000);
        state.spinRoulette(Roulette.values()[0], new java.util.Random(1));
        return state;
    }

    private static Character character(int id, String teams, String powers, int origin) {
        return GSON.fromJson("{\"id\":" + id + ",\"name\":\"Heroi " + id + "\","
                + "\"publisher\":{\"id\":31,\"name\":\"Marvel\"},"
                + "\"teams\":[" + teams + "],\"powers\":[" + powers + "],\"movies\":[],"
                + "\"origin\":{\"id\":" + origin + ",\"name\":\"X\"}}", Character.class);
    }

    private static String describe(DailyChallenges daily) {
        StringBuilder text = new StringBuilder();
        for (DailyChallenge c : daily.getChallenges()) {
            text.append(c.getType()).append(':').append(c.getParam()).append('/')
                    .append(c.getTarget()).append(' ');
        }
        return text.toString();
    }

    private static int dayPlus(int days) {
        return DailyClock.toKey(LocalDate.of(2026, 9, 28).plusDays(days));
    }

    private static BattleFacts factsFor(DailyChallenge c) {
        Set<Integer> param = new HashSet<>();
        param.add(c.getParam());
        switch (c.getType()) {
            case LAND_CRITS: return facts(true, 5, 50, 40, 0, Set.of(), Set.of(), Set.of(), 0);
            case DEFEAT_ORIGIN: return facts(true, 0, 50, 40, 0, Set.of(), Set.of(), Set.of(), c.getParam());
            case DEFEAT_TEAM_MEMBER: return facts(true, 0, 50, 40, 0, Set.of(), Set.of(), param, 0);
            case WIN_WITH_TEAM: return facts(true, 0, 50, 40, 0, param, Set.of(), Set.of(), 0);
            case WIN_WITH_POWER: return facts(true, 0, 50, 40, 0, Set.of(), param, Set.of(), 0);
            case WIN_WITH_ARTIFACT: return facts(true, 0, 50, 40, 1, Set.of(), Set.of(), Set.of(), 0);
            case WIN_VS_STRONGER: return facts(true, 0, 40, 60, 0, Set.of(), Set.of(), Set.of(), 0);
            case WIN_WITH_OVERALL: return facts(true, 0, 100, 40, 0, Set.of(), Set.of(), Set.of(), 0);
            default: return facts(true, 0, 50, 40, 0, Set.of(), Set.of(), Set.of(), 0);
        }
    }

    private static BattleFacts facts(boolean won, int crits, int hero, int enemy, int artifacts,
                                     Set<Integer> heroTeams, Set<Integer> heroPowers,
                                     Set<Integer> enemyTeams, int enemyOrigin) {
        return new BattleFacts(won, crits, hero, enemy, artifacts,
                heroTeams, heroPowers, enemyTeams, enemyOrigin);
    }

    @Test
    public void sameDayAndHeroesDrawTheSameChallenges() {
        assertEquals(describe(DailyChallenges.generate(DAY, veteran())),
                describe(DailyChallenges.generate(DAY, veteran())));
        assertEquals(describe(DailyChallenges.generate(DAY, newPlayer())),
                describe(DailyChallenges.generate(DAY, newPlayer())));
    }

    @Test
    public void challengesChangeFromOneDayToTheNext() {
        int repeatedDays = 0;
        Set<String> distinct = new HashSet<>();
        for (int i = 0; i < 60; i++) {
            String today = describe(DailyChallenges.generate(dayPlus(i), veteran()));
            String tomorrow = describe(DailyChallenges.generate(dayPlus(i + 1), veteran()));
            if (today.equals(tomorrow)) repeatedDays++;
            distinct.add(today);
        }
        assertTrue("dias iguais seguidos: " + repeatedDays, repeatedDays <= 3);
        assertTrue("combinações diferentes: " + distinct.size(), distinct.size() >= 30);
    }

    @Test
    public void atLeastTwoOfThreeArePossibleWithTheHeroesYouHave() {
        for (PlayerState player : new PlayerState[]{newPlayer(), veteran()}) {
            for (int i = 0; i < 365; i++) {
                DailyChallenges daily = DailyChallenges.generate(dayPlus(i), player);
                assertEquals(DailyChallenges.COUNT, daily.getChallenges().size());
                int possible = 0;
                Set<DailyChallenge.Type> types = new HashSet<>();
                for (DailyChallenge c : daily.getChallenges()) {
                    if (c.isPossibleWith(player)) possible++;
                    types.add(c.getType());
                }
                assertTrue(describe(daily), possible >= 2);
                assertEquals("tipos repetidos: " + describe(daily), 3, types.size());
            }
        }
    }

    @Test
    public void veteransGetChallengesWithTheirTeamsAndPowers() {
        Set<DailyChallenge.Type> seen = new HashSet<>();
        for (int i = 0; i < 365; i++) {
            seen.add(DailyChallenges.generate(dayPlus(i), veteran()).getChallenges().get(1).getType());
        }
        assertTrue(seen.contains(DailyChallenge.Type.WIN_WITH_TEAM));
        assertTrue(seen.contains(DailyChallenge.Type.WIN_WITH_POWER));
        assertTrue(seen.contains(DailyChallenge.Type.DEFEAT_TEAM_MEMBER));
        assertTrue(seen.contains(DailyChallenge.Type.WIN_WITH_ARTIFACT));
        assertTrue(seen.contains(DailyChallenge.Type.DEFEAT_ORIGIN));
    }

    @Test
    public void openingTheAppTwiceTheSameDayKeepsChallengesAndProgress() {
        PlayerState state = veteran();
        assertTrue(state.ensureDaily(DAY));
        DailyChallenge first = state.getDaily().getChallenges().get(0);
        state.recordDaily(factsFor(first), DAY);
        String before = describe(state.getDaily());
        int progress = first.getProgress();

        assertFalse("mesmo dia não sorteia de novo", state.ensureDaily(DAY));
        PlayerState reopened = GSON.fromJson(GSON.toJson(state), PlayerState.class);
        reopened.repair();
        assertFalse(reopened.ensureDaily(DAY));
        assertEquals(before, describe(reopened.getDaily()));
        assertEquals(progress, reopened.getDaily().getChallenges().get(0).getProgress());
    }

    @Test
    public void midnightBringsNewChallenges() {
        PlayerState state = veteran();
        state.ensureDaily(DAY);
        assertTrue(state.ensureDaily(dayPlus(1)));
        assertEquals(dayPlus(1), state.getDaily().getDay());
        assertEquals(0, state.getDaily().countComplete());
    }

    @Test
    public void movingTheClockBackDoesNotDrawOrPayAgain() {
        PlayerState state = veteran();
        state.ensureDaily(dayPlus(1));
        for (DailyChallenge c : state.getDaily().getChallenges()) {
            for (int i = 0; i < c.getTarget(); i++) state.recordDaily(factsFor(c), dayPlus(1));
        }
        int coins = state.getCoins();
        assertFalse(state.ensureDaily(DAY));
        assertEquals(dayPlus(1), state.getDaily().getDay());
        for (DailyChallenge c : state.getDaily().getChallenges()) {
            assertTrue(state.recordDaily(factsFor(c), DAY).isEmpty());
        }
        assertEquals(coins, state.getCoins());
        assertEquals(1, state.getDailyChestsToOpen());
    }

    @Test
    public void eachChallengePaysOnceAndAllThreeGiveOneChest() {
        PlayerState state = veteran();
        state.ensureDaily(DAY);
        int coinsBefore = state.getCoins();
        int expected = 0;
        for (DailyChallenge c : state.getDaily().getChallenges()) {
            for (int i = 0; i < c.getTarget(); i++) state.recordDaily(factsFor(c), DAY);
            assertTrue(c.getType() + " não completou", c.isComplete());
            expected += c.getCoins();
        }
        assertEquals(coinsBefore + expected, state.getCoins());
        assertEquals(1, state.getDailyChestsToOpen());

        for (DailyChallenge c : state.getDaily().getChallenges()) {
            assertTrue(state.recordDaily(factsFor(c), DAY).isEmpty());
        }
        assertEquals(coinsBefore + expected, state.getCoins());
        assertEquals(1, state.getDailyChestsToOpen());

        state.ensureDaily(dayPlus(1));
        assertEquals(1, state.getDailyChestsToOpen());
        assertNotNull(state.openDailyChest(new java.util.Random(3)));
        assertNull(state.openDailyChest(new java.util.Random(3)));
        assertEquals(0, state.getDailyChestsToOpen());
    }

    @Test
    public void lossesOnlyCountForPlayingAndCrits() {
        PlayerState state = veteran();
        state.ensureDaily(DAY);
        BattleFacts loss = facts(false, 2, 40, 90, 1, Set.of(3806, 3173), Set.of(5, 9),
                Set.of(3806, 3173), 1);
        List<DailyChallenge> challenges = state.getDaily().getChallenges();
        state.recordDaily(loss, DAY);
        for (DailyChallenge c : challenges) {
            boolean countsLosses = c.getType() == DailyChallenge.Type.PLAY_BATTLES
                    || c.getType() == DailyChallenge.Type.LAND_CRITS;
            assertEquals(c.getType().toString(), countsLosses, c.getProgress() > 0);
        }
    }

    @Test
    public void brokenDailySaveIsDrawnAgain() {
        PlayerState state = GSON.fromJson("{\"daily\":{\"day\":" + DAY + ",\"challenges\":[]},"
                + "\"dailyChestsToOpen\":-2}", PlayerState.class);
        state.repair();
        assertNull(state.getDaily());
        assertEquals(0, state.getDailyChestsToOpen());
        assertTrue(state.ensureDaily(DAY));
        assertEquals(DailyChallenges.COUNT, state.getDaily().getChallenges().size());
    }
}
