package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SurvivalTest {

    private static final int DAY = 20260928;
    private static final GameAttributes ANY = GameAttributes.of(50, 50, 50, 50);
    private static final BattleFacts NO_FACTS =
            new BattleFacts(true, 0, 0, 0, 0, Set.of(), Set.of(), Set.of(), 0);

    private static BattleResult result(boolean won, int hpEnd, int heroMaxHp) {
        BattleResult result = new BattleResult(won, Collections.emptyList(), heroMaxHp, 100,
                ANY, ANY, GameBalance.WIN_TROPHIES, 300);
        result.setHeroStartHp(hpEnd);
        return result;
    }

    private static SurvivalOutcome fightFloor(PlayerState state, boolean won, int hpEnd) {
        SurvivalRun run = state.getActiveSurvival();
        int enemyId = 900_000 + run.getFloor();
        assertTrue(state.setSurvivalEnemy(enemyId));
        OwnedHero hero = state.find(run.getHeroId());
        return state.applySurvivalBattle(result(won, hpEnd, Survival.maxHp(run, hero)), NO_FACTS, DAY,
                run.getFloor(), enemyId, new Random(1));
    }

    @Test
    public void enemyPowerStartsAtHalfAndGrowsTenPercentPerFloor() {
        assertEquals(50, Survival.powerPercent(1));
        assertEquals(55, Survival.powerPercent(2));
        assertEquals(118, Survival.powerPercent(10));
        assertEquals(306, Survival.powerPercent(20));
        assertEquals(100, Survival.coinsForFloor(5));
        assertTrue(Survival.isChestFloor(10));
        assertFalse(Survival.isChestFloor(11));
    }

    @Test
    public void winningPaysTheFloorHealsAndOffersThreeBuffsButNoTrophies() {
        PlayerState state = PlayerState.newGame();
        assertTrue(state.startSurvival());
        assertFalse("uma partida por vez", state.startSurvival());
        int coins = state.getCoins();
        int trophies = state.getTrophies();
        OwnedHero hero = state.getActiveHero();
        int max = Survival.maxHp(state.getActiveSurvival(), hero);

        SurvivalOutcome outcome = fightFloor(state, true, 50);

        assertNotNull(outcome);
        assertTrue(outcome.isWon());
        assertTrue(outcome.isNewRecord());
        assertEquals(coins + Survival.coinsForFloor(1), state.getCoins());
        assertEquals(trophies, state.getTrophies());
        assertEquals(0, state.getWins());
        SurvivalRun run = state.getActiveSurvival();
        assertEquals(2, run.getFloor());
        assertEquals(50 + Math.round(max * GameBalance.SURVIVAL_HEAL_PERCENT / 100f), run.getHeroHp());
        assertEquals(GameBalance.SURVIVAL_BUFF_CHOICES, new HashSet<>(run.getOffer()).size());
        assertFalse("precisa escolher o bônus antes de lutar", state.setSurvivalEnemy(123));
        assertEquals(1, state.getSurvivalBestFloor());
    }

    @Test
    public void theSameFightCannotBeAppliedTwice() {
        PlayerState state = PlayerState.newGame();
        state.startSurvival();
        SurvivalRun run = state.getActiveSurvival();
        assertTrue(state.setSurvivalEnemy(777));
        OwnedHero hero = state.getActiveHero();
        BattleResult win = result(true, 100, Survival.maxHp(run, hero));
        assertNotNull(state.applySurvivalBattle(win, NO_FACTS, DAY, 1, 777, new Random(1)));
        int coins = state.getCoins();
        assertNull(state.applySurvivalBattle(win, NO_FACTS, DAY, 1, 777, new Random(1)));
        assertEquals(coins, state.getCoins());
        assertEquals(2, state.getActiveSurvival().getFloor());
    }

    @Test
    public void losingEndsTheRunAndANewRunStartsCleanWithoutBuffs() {
        PlayerState state = PlayerState.newGame();
        state.startSurvival();
        fightFloor(state, true, 100);
        assertTrue(state.chooseSurvivalBuff(0));
        int coins = state.getCoins();

        SurvivalOutcome loss = fightFloor(state, false, 0);
        assertFalse(loss.isWon());
        assertEquals(0, loss.getCoins());
        assertEquals(coins, state.getCoins());
        assertNull(state.getActiveSurvival());
        assertTrue(state.getSurvival().isOver());
        assertEquals(0, state.getLosses());

        assertTrue(state.startSurvival());
        SurvivalRun fresh = state.getActiveSurvival();
        assertEquals(1, fresh.getFloor());
        assertTrue("bônus somem no fim da partida", fresh.getBuffs().isEmpty());
        assertEquals(Survival.maxHp(fresh, state.getActiveHero()),
                Survival.currentHp(fresh, state.getActiveHero()));
    }

    @Test
    public void floorTenGivesAChestOnlyTheFirstTime() {
        PlayerState state = PlayerState.newGame();
        for (int run = 0; run < 2; run++) {
            state.startSurvival();
            for (int i = 1; i < 10; i++) assertTrue(state.skipSurvivalFloor());
            SurvivalOutcome outcome = fightFloor(state, true, 50);
            assertEquals(10, outcome.getFloor());
            assertEquals(run == 0, outcome.isChestUnlocked());
            state.endSurvival();
        }
        assertEquals(1, state.getSurvivalChestsToOpen());
        assertNotNull(state.openSurvivalChest(new Random(2)));
        assertNull(state.openSurvivalChest(new Random(2)));
    }

    @Test
    public void medkitHealsAndArmorRaisesMaxAndCurrentLife() {
        PlayerState state = PlayerState.newGame();
        state.startSurvival();
        SurvivalRun run = state.getActiveSurvival();
        OwnedHero hero = state.getActiveHero();
        int max = Survival.maxHp(run, hero);

        run.heroHp = 10;
        run.offer = new java.util.ArrayList<>(List.of(SurvivalBuff.MEDKIT));
        assertTrue(state.chooseSurvivalBuff(0));
        assertEquals(10 + Math.round(max * GameBalance.SURVIVAL_MEDKIT_HEAL_PERCENT / 100f), run.heroHp);
        assertTrue(run.getBuffs().isEmpty());

        int before = run.heroHp;
        run.offer = new java.util.ArrayList<>(List.of(SurvivalBuff.ARMOR));
        assertTrue(state.chooseSurvivalBuff(0));
        int newMax = Survival.maxHp(run, hero);
        assertTrue(newMax > max);
        assertEquals(before + (newMax - max), run.heroHp);
    }

    @Test
    public void effectsTheHeroAlreadyHasAreNotOffered() {
        PlayerState state = PlayerState.newGame();
        state.startSurvival();
        SurvivalRun run = state.getActiveSurvival();
        run.buffs.add(SurvivalBuff.LIFESTEAL);
        for (int seed = 0; seed < 200; seed++) {
            List<SurvivalBuff> offer = Survival.rollOffer(run, state.getActiveHero(), new Random(seed));
            assertFalse(offer.contains(SurvivalBuff.LIFESTEAL));
        }
    }

    @Test
    public void runSurvivesClosingTheApp() {
        Gson gson = new Gson();
        PlayerState state = PlayerState.newGame();
        state.startSurvival();
        fightFloor(state, true, 80);
        state.chooseSurvivalBuff(0);
        PlayerState reopened = gson.fromJson(gson.toJson(state), PlayerState.class);
        reopened.repair();
        SurvivalRun run = reopened.getActiveSurvival();
        assertNotNull(run);
        assertEquals(2, run.getFloor());
        assertEquals(state.getActiveSurvival().getHeroHp(), run.getHeroHp());
        assertEquals(state.getActiveSurvival().getBuffs(), run.getBuffs());
    }

    @Test
    public void withoutModifiersTheBattleIsExactlyTheNormalOne() {
        GameAttributes hero = GameAttributes.of(60, 55, 50, 70);
        GameAttributes enemy = GameAttributes.of(65, 60, 45, 40);
        for (int seed = 0; seed < 50; seed++) {
            BattleResult normal = BattleEngine.fight(hero, Collections.emptyList(), enemy, new Random(seed));
            BattleResult modified = BattleEngine.fight(hero, Collections.emptyList(), enemy, new Random(seed),
                    BattleModifiers.NONE);
            assertEquals(normal.isHeroWinner(), modified.isHeroWinner());
            assertEquals(normal.getTurns().size(), modified.getTurns().size());
            assertEquals(normal.getHeroHpEnd(), modified.getHeroHpEnd());
            assertEquals(normal.getHeroMaxHp(), normal.getHeroStartHp());
        }
    }

    @Test
    public void modifiersSetStartLifeAndEnemyPower() {
        GameAttributes a = GameAttributes.of(50, 50, 50, 50);
        BattleModifiers mods = new BattleModifiers(90, 20, 0,
                EnumSet.of(Artifact.Effect.LIFESTEAL), 50);
        BattleResult result = BattleEngine.fight(a, Collections.emptyList(), a, new Random(3), mods);
        assertEquals(90, result.getHeroStartHp());
        assertEquals(Math.round(BattleEngine.maxHp(a) * 1.2f), result.getHeroMaxHp());
        assertEquals(Math.round(BattleEngine.maxHp(a) * 0.5f), result.getEnemyMaxHp());
    }
}
