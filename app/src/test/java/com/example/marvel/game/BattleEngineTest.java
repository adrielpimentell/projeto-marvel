package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Random;

public class BattleEngineTest {

    private static final GameAttributes STRONG = GameAttributes.of(94, 83, 90, 76);
    private static final GameAttributes WEAK = OwnedHero.starter().getAttributes();

    @Test
    public void thousandBattles_strongWinsMost_weakWinsSometimes() {
        Random random = new Random(2026);
        int strongWins = 0;
        int weakWins = 0;
        int totalTurns = 0;
        int maxTurns = 0;

        for (int i = 0; i < 1000; i++) {
            boolean strongIsHero = i % 2 == 0;
            BattleResult r = strongIsHero
                    ? BattleEngine.fight(STRONG, WEAK, random)
                    : BattleEngine.fight(WEAK, STRONG, random);
            boolean strongWon = r.isHeroWinner() == strongIsHero;
            if (strongWon) strongWins++; else weakWins++;
            totalTurns += r.getTurns().size();
            maxTurns = Math.max(maxTurns, r.getTurns().size());
        }

        System.out.printf("1000 batalhas | forte (Overall %d): %d vitórias (%.1f%%) | "
                        + "fraco (Overall %d): %d vitórias (%.1f%%) | turnos: média %.1f, máx %d%n",
                STRONG.getOverall(), strongWins, strongWins / 10.0,
                WEAK.getOverall(), weakWins, weakWins / 10.0, totalTurns / 1000.0, maxTurns);

        assertTrue("O forte precisa vencer a maioria", strongWins >= 800);
        assertTrue("O fraco precisa vencer às vezes", weakWins >= 20);
    }

    @Test
    public void equalFightersWinAboutHalf() {
        Random random = new Random(7);
        int heroWins = 0;
        for (int i = 0; i < 1000; i++) {
            if (BattleEngine.fight(WEAK, WEAK, random).isHeroWinner()) heroWins++;
        }
        assertTrue("Esperado perto de 50%, veio " + heroWins, heroWins > 420 && heroWins < 580);
    }

    @Test
    public void fasterFighterAttacksFirst() {
        GameAttributes fast = GameAttributes.of(50, 50, 90, 50);
        GameAttributes slow = GameAttributes.of(50, 50, 10, 50);
        assertTrue(BattleEngine.fight(fast, slow, new Random(1)).getTurns().get(0).isHeroAttacking());
        assertFalse(BattleEngine.fight(slow, fast, new Random(1)).getTurns().get(0).isHeroAttacking());
    }

    @Test
    public void turnsAreConsistentWithWinner() {
        Random random = new Random(99);
        for (int i = 0; i < 300; i++) {
            BattleResult r = BattleEngine.fight(STRONG, WEAK, random);
            List<BattleTurn> turns = r.getTurns();
            assertTrue(turns.size() <= GameBalance.MAX_TURNS);

            BattleTurn last = turns.get(turns.size() - 1);
            assertEquals(r.isHeroWinner(), last.isHeroAttacking());
            assertEquals(0, last.getTargetHpAfter());
            for (BattleTurn t : turns) {
                assertTrue(t.getDamage() >= 1);
                assertTrue(t.getTargetHpAfter() >= 0);
            }
        }
    }

    @Test
    public void sameSeedGivesSameBattle() {
        BattleResult a = BattleEngine.fight(STRONG, WEAK, new Random(5));
        BattleResult b = BattleEngine.fight(STRONG, WEAK, new Random(5));
        assertEquals(a.isHeroWinner(), b.isHeroWinner());
        assertEquals(a.getTurns().size(), b.getTurns().size());
    }

    @Test
    public void rewardsFollowTheRules() {
        Random random = new Random(3);
        boolean sawWin = false;
        boolean sawLoss = false;
        for (int i = 0; i < 200 && !(sawWin && sawLoss); i++) {
            BattleResult r = BattleEngine.fight(WEAK, STRONG, random);
            if (r.isHeroWinner()) {
                sawWin = true;
                assertEquals(10, r.getTrophies());
                assertEquals(Economy.coinsForVictory(WEAK.getOverall(), STRONG.getOverall()), r.getCoins());
                assertEquals(500, r.getCoins());
            } else {
                sawLoss = true;
                assertEquals(-5, r.getTrophies());
                assertEquals(0, r.getCoins());
            }
        }
        assertTrue(sawWin && sawLoss);
    }
}
