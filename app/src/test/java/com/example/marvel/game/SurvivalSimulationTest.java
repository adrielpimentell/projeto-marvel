package com.example.marvel.game;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class SurvivalSimulationTest {

    private static final int RUNS = 200;
    private static final int FLOOR_CAP = 100;
    private static final int STRONG_ID = 1440;
    private static final GameAttributes STRONG = GameAttributes.of(90, 85, 80, 85);

    private static final class Stats {
        double average;
        int min = Integer.MAX_VALUE;
        int max;
        int coins;
    }

    @Test
    public void strongHeroGoesFurtherAndEveryoneFallsSomeday() {
        Stats base = simulate(PlayerState.newGame(), new Random(11));

        PlayerState strongPlayer = PlayerState.newGame();
        strongPlayer.buy(new OwnedHero(STRONG_ID, STRONG), 0);
        strongPlayer.setActiveHero(STRONG_ID);
        Stats strong = simulate(strongPlayer, new Random(11));

        System.out.println(String.format(Locale.ROOT,
                "SOBREVIVENCIA base (Overall %d): andar medio %.1f, min %d, max %d, moedas/partida %d",
                PlayerState.newGame().getActiveHero().getAttributes().getOverall(),
                base.average, base.min, base.max, base.coins));
        System.out.println(String.format(Locale.ROOT,
                "SOBREVIVENCIA forte (Overall %d): andar medio %.1f, min %d, max %d, moedas/partida %d",
                STRONG.getOverall(), strong.average, strong.min, strong.max, strong.coins));

        assertTrue("o forte deve ir mais longe", strong.average > base.average + 3);
        assertTrue("todo mundo perde antes do andar " + FLOOR_CAP, strong.max < FLOOR_CAP);
    }

    private static Stats simulate(PlayerState state, Random random) {
        Stats stats = new Stats();
        int enemyId = 1_000_000;
        long floors = 0;
        int coinsBefore = state.getCoins();
        for (int i = 0; i < RUNS; i++) {
            assertTrue(state.startSurvival());
            while (state.getActiveSurvival() != null) {
                SurvivalRun run = state.getActiveSurvival();
                assertTrue("passou do andar " + FLOOR_CAP, run.getFloor() < FLOOR_CAP);
                OwnedHero hero = state.find(run.getHeroId());
                if (run.hasChoicePending()) {
                    assertTrue(state.chooseSurvivalBuff(pickBuff(run, hero)));
                    continue;
                }
                assertTrue(state.setSurvivalEnemy(++enemyId));
                BattleResult result = BattleEngine.fight(hero.getBattleAttributes(),
                        Loadout.artifactsOf(hero), randomEnemy(random), random,
                        Survival.modifiers(run, hero));
                BattleFacts facts = new BattleFacts(result.isHeroWinner(), 0, 0, 0, 0,
                        Set.of(), Set.of(), Set.of(), 0);
                assertNotNull(state.applySurvivalBattle(result, facts, 20260928,
                        run.getFloor(), enemyId, random));
            }
            int lostOn = state.getSurvival().getFloor();
            floors += lostOn;
            stats.min = Math.min(stats.min, lostOn);
            stats.max = Math.max(stats.max, lostOn);
        }
        stats.average = (double) floors / RUNS;
        stats.coins = (state.getCoins() - coinsBefore) / RUNS;
        return stats;
    }

    private static int pickBuff(SurvivalRun run, OwnedHero hero) {
        List<SurvivalBuff> offer = run.getOffer();
        boolean lowLife = Survival.currentHp(run, hero) < Survival.maxHp(run, hero) * 0.4;
        int medkit = offer.indexOf(SurvivalBuff.MEDKIT);
        if (lowLife && medkit >= 0) return medkit;
        for (int i = 0; i < offer.size(); i++) {
            if (offer.get(i).getEffect() != null) return i;
        }
        for (int i = 0; i < offer.size(); i++) {
            if (offer.get(i) != SurvivalBuff.MEDKIT) return i;
        }
        return 0;
    }

    private static GameAttributes randomEnemy(Random random) {
        double overall = 20 + random.nextDouble() * 50;
        double[] profile = new double[4];
        double sum = 0;
        for (int i = 0; i < 4; i++) {
            profile[i] = 0.6 + random.nextDouble() * 0.8;
            sum += profile[i];
        }
        int[] a = new int[4];
        for (int i = 0; i < 4; i++) {
            a[i] = (int) Math.max(1, Math.min(100, Math.round(overall * profile[i] / (sum / 4))));
        }
        return GameAttributes.of(a[0], a[1], a[2], a[3]);
    }
}
