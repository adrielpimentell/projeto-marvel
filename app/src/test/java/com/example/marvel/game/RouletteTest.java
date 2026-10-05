package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class RouletteTest {

    private static final int SPINS = 10_000;
    private static final int TOLERANCE_PER_MILLE = 20;

    @Test
    public void oddsSumTo100AndBetterWheelsAreNeverWorse() {
        int epic = Artifact.Rarity.EPIC.ordinal();
        int legendary = Artifact.Rarity.LEGENDARY.ordinal();
        Roulette previous = null;
        for (Roulette roulette : Roulette.values()) {
            int total = 0;
            for (Artifact.Rarity rarity : Artifact.Rarity.values()) total += roulette.oddsPerMille(rarity);
            assertEquals(roulette + " precisa somar 100%", 1000, total);
            if (previous != null) {
                int[] now = GameBalance.ROULETTE_ODDS_PER_MILLE[roulette.ordinal()];
                int[] before = GameBalance.ROULETTE_ODDS_PER_MILLE[previous.ordinal()];
                assertTrue(roulette + ": Épico não pode cair", now[epic] >= before[epic]);
                assertTrue(roulette + ": Lendário não pode cair", now[legendary] >= before[legendary]);
                assertTrue("roleta melhor custa mais", roulette.getPrice() > previous.getPrice());
            }
            previous = roulette;
        }
    }

    @Test
    public void tenThousandSpinsPerWheelMatchTheTable() {
        StringBuilder report = new StringBuilder("10.000 giros por roleta (sorteado × tabela):\n");
        Random random = new Random(2026);
        for (Roulette roulette : Roulette.values()) {
            int[] counts = new int[Artifact.Rarity.values().length];
            for (int i = 0; i < SPINS; i++) counts[roulette.rollRarity(random).ordinal()]++;
            report.append(String.format(Locale.ROOT, "  %-9s", roulette));
            for (Artifact.Rarity rarity : Artifact.Rarity.values()) {
                int gotPerMille = counts[rarity.ordinal()] * 1000 / SPINS;
                int expected = roulette.oddsPerMille(rarity);
                report.append(String.format(Locale.ROOT, " | %s %5.1f%% × %4.1f%%",
                        rarity, counts[rarity.ordinal()] * 100.0 / SPINS, expected / 10.0));
                assertTrue(roulette + " " + rarity + ": " + gotPerMille + " x " + expected,
                        Math.abs(gotPerMille - expected) <= TOLERANCE_PER_MILLE);
            }
            report.append('\n');
        }
        System.out.print(report);
    }

    @Test
    public void spinChargesTheExactPriceAndGivesAPrize() {
        PlayerState s = PlayerState.newGame();
        s.addCoins(Roulette.EPIC.getPrice() + 123);
        RouletteReward reward = s.spinRoulette(Roulette.EPIC, new Random(1));
        assertNotNull(reward);
        assertNotNull(reward.getArtifact());
        assertEquals(123, s.getCoins());
        assertTrue(s.ownsArtifact(reward.getArtifact().getId()));
    }

    @Test
    public void notEnoughCoinsChargesNothingAndDoubleTapChargesOnce() {
        PlayerState s = PlayerState.newGame();
        s.addCoins(Roulette.COMMON.getPrice() - 1);
        assertNull(s.spinRoulette(Roulette.COMMON, new Random(2)));
        assertEquals(Roulette.COMMON.getPrice() - 1, s.getCoins());

        s.addCoins(1);
        assertNotNull(s.spinRoulette(Roulette.COMMON, new Random(3)));
        assertNull(s.spinRoulette(Roulette.COMMON, new Random(4)));
        assertTrue(s.getCoins() >= 0);
    }

    @Test
    public void duplicatesFollowTheChestRule() {
        PlayerState s = PlayerState.newGame();
        s.addCoins(Roulette.LEGENDARY.getPrice() * 60);
        Set<String> seen = new HashSet<>();
        Random random = new Random(5);
        int coinPrizes = 0;
        for (int i = 0; i < 60; i++) {
            int before = s.getCoins();
            RouletteReward r = s.spinRoulette(Roulette.LEGENDARY, random);
            assertNotNull(r);
            if (r.getArtifact() != null) {
                assertTrue("artefato repetido", seen.add(r.getArtifact().getId()));
            } else {
                coinPrizes++;
                assertEquals(GameBalance.DUPLICATE_COINS[r.getRarity().ordinal()], r.getCoins());
                assertEquals(before - Roulette.LEGENDARY.getPrice() + r.getCoins(), s.getCoins());
            }
        }
        assertTrue("depois de muitos giros, raridades completas viram moedas", coinPrizes > 0);
    }

    @Test
    public void spinIsSavedLikeEverythingElse() {
        PlayerState s = PlayerState.newGame();
        s.addCoins(Roulette.RARE.getPrice());
        RouletteReward r = s.spinRoulette(Roulette.RARE, new Random(6));
        Gson gson = new Gson();
        PlayerState loaded = gson.fromJson(gson.toJson(s), PlayerState.class);
        loaded.repair();
        assertEquals(0, loaded.getCoins());
        assertTrue(loaded.ownsArtifact(r.getArtifact().getId()));
    }
}
