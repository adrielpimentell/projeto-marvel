package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ArtifactBalanceTest {

    private static final GameAttributes EQUAL = GameAttributes.of(60, 60, 60, 60);
    private static final GameAttributes STRONG = GameAttributes.of(94, 83, 90, 76);
    private static final GameAttributes WEAK = OwnedHero.starter().getAttributes();
    private static final int BATTLES = 1000;

    @Test
    public void noArtifactMakesAHeroUnbeatable() {
        List<Artifact> ranked = new ArrayList<>(ArtifactCatalog.all());
        List<double[]> rates = new ArrayList<>();
        StringBuilder report = new StringBuilder("Cada artefato sozinho (60 x 60, sem artefato = ~50%):\n");
        for (Artifact artifact : ranked) {
            double rate = winRate(EQUAL, Collections.singletonList(artifact), EQUAL, 2000, 7);
            rates.add(new double[]{rate});
            report.append(String.format(Locale.ROOT, "  %-28s %5.1f%%%n", artifact.getName(), rate * 100));
            assertTrue(artifact.getName() + " sozinho não pode passar de 75%", rate <= 0.75);
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) order.add(i);
        order.sort((a, b) -> Double.compare(rates.get(b)[0], rates.get(a)[0]));
        List<Artifact> bestPair = Arrays.asList(ranked.get(order.get(0)), ranked.get(order.get(1)));
        report.append("2 mais fortes: ").append(bestPair.get(0).getName())
                .append(" + ").append(bestPair.get(1).getName()).append('\n');

        double equalWith = winRate(EQUAL, bestPair, EQUAL, BATTLES, 11);
        double equalWithout = winRate(EQUAL, Collections.emptyList(), EQUAL, BATTLES, 11);
        double strongWithout = winRate(STRONG, Collections.emptyList(), WEAK, BATTLES, 13);
        double strongWith = winRate(STRONG, bestPair, WEAK, BATTLES, 13);
        double weakWithout = winRate(WEAK, Collections.emptyList(), STRONG, BATTLES, 17);
        double weakWith = winRate(WEAK, bestPair, STRONG, BATTLES, 17);

        report.append(String.format(Locale.ROOT,
                "Igual x igual (60): sem artefatos %.1f%% | com os 2 mais fortes %.1f%%%n"
                        + "Forte (86) x fraco (24): sem %.1f%% | forte com os 2 %.1f%% (fraco ainda vence %.1f%%)%n"
                        + "Fraco (24) x forte (86): sem %.1f%% | fraco com os 2 %.1f%%%n",
                equalWithout * 100, equalWith * 100,
                strongWithout * 100, strongWith * 100, (1 - strongWith) * 100,
                weakWithout * 100, weakWith * 100));
        System.out.print(report);

        assertTrue("sem artefatos, luta igual fica perto de 50%", Math.abs(equalWithout - 0.5) < 0.06);
        assertTrue("com os 2 mais fortes, ainda perde às vezes numa luta igual", equalWith <= 0.85);
        assertTrue("o fraco ainda vence o forte equipado às vezes", strongWith < 0.99);
        assertTrue("artefatos ajudam o fraco, mas não viram o jogo", weakWith > weakWithout && weakWith < 0.5);
    }

    @Test
    public void effectsReallyHappenInBattle() {
        Random random = new Random(3);
        int dodges = 0, blocks = 0, poison = 0, lifesteal = 0, thorns = 0, regen = 0, stuns = 0, skips = 0;
        for (String id : new String[]{"sentido_aranha", "escudo_vibranium", "veneno_simbionte",
                "lamina_vampirica", "armadura_espinhos", "fator_cura", "martelo_trovao"}) {
            List<Artifact> gear = Collections.singletonList(ArtifactCatalog.byId(id));
            for (int i = 0; i < 100; i++) {
                BattleResult r = BattleEngine.fight(EQUAL, gear, EQUAL, random);
                for (BattleTurn t : r.getTurns()) {
                    if (t.isDodged()) dodges++;
                    if (t.isBlocked()) blocks++;
                    if (t.getPoisonTick() > 0) poison++;
                    if (t.getLifesteal() > 0) lifesteal++;
                    if (t.getThorns() > 0) thorns++;
                    if (t.getRegen() > 0) regen++;
                    if (t.isStunApplied()) stuns++;
                    if (t.isSkipped()) skips++;
                }
            }
        }
        assertTrue(dodges > 0 && blocks > 0 && poison > 0 && lifesteal > 0);
        assertTrue(thorns > 0 && regen > 0 && stuns > 0);
        assertEquals("todo atordoamento faz perder um turno (ou a luta acaba antes)", true, skips <= stuns);
    }

    @Test
    public void shieldBlocksOnlyTheFirstHitAndHpNeverBreaks() {
        List<Artifact> shield = Collections.singletonList(ArtifactCatalog.byId("escudo_vibranium"));
        Random random = new Random(5);
        for (int i = 0; i < 300; i++) {
            BattleResult r = BattleEngine.fight(EQUAL, shield, STRONG, random);
            int blocks = 0;
            for (BattleTurn t : r.getTurns()) {
                if (t.isBlocked()) blocks++;
                assertTrue(t.getHeroHpEnd() >= 0 && t.getHeroHpEnd() <= r.getHeroMaxHp());
                assertTrue(t.getEnemyHpEnd() >= 0 && t.getEnemyHpEnd() <= r.getEnemyMaxHp());
            }
            assertTrue(blocks <= 1);
            BattleTurn last = r.getTurns().get(r.getTurns().size() - 1);
            int loserHp = r.isHeroWinner() ? last.getEnemyHpEnd() : last.getHeroHpEnd();
            if (r.getTurns().size() < GameBalance.MAX_TURNS) assertEquals(0, loserHp);
        }
    }

    private static double winRate(GameAttributes hero, List<Artifact> gear, GameAttributes enemy,
                                  int battles, long seed) {
        Random random = new Random(seed);
        GameAttributes withBonuses = Loadout.withBonuses(hero, gear);
        int wins = 0;
        for (int i = 0; i < battles; i++) {
            if (BattleEngine.fight(withBonuses, gear, enemy, random).isHeroWinner()) wins++;
        }
        return wins / (double) battles;
    }
}
