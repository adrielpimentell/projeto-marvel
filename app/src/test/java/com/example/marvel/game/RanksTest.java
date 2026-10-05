package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RanksTest {

    private static final int RECRUTA = 0;
    private static final int HEROI = 1;
    private static final int CAMPEAO = 5;
    private static final int LENDA = 6;
    private static final int CELESTIAL = 8;
    private static final int GUARDIAO_GALACTICO = 9;
    private static final int SENHOR_DA_MANOPLA = 15;
    private static final int ENTIDADE_COSMICA = 16;

    @Test
    public void rankChangesExactlyAtTheLimits() {
        assertEquals(RECRUTA, Ranks.indexFor(0));
        assertEquals(RECRUTA, Ranks.indexFor(49));
        assertEquals(HEROI, Ranks.indexFor(50));
        assertEquals(HEROI, Ranks.indexFor(149));
        assertEquals(2, Ranks.indexFor(150));
        assertEquals(3, Ranks.indexFor(300));
        assertEquals(4, Ranks.indexFor(500));
        assertEquals(CAMPEAO, Ranks.indexFor(750));
        assertEquals(CAMPEAO, Ranks.indexFor(999));
        assertEquals(LENDA, Ranks.indexFor(1000));
        assertEquals(7, Ranks.indexFor(1500));
        assertEquals(CELESTIAL, Ranks.indexFor(2500));
        assertEquals(CELESTIAL, Ranks.indexFor(4999));
        assertEquals(GUARDIAO_GALACTICO, Ranks.indexFor(5000));
        assertEquals(GUARDIAO_GALACTICO, Ranks.indexFor(7499));
        assertEquals(10, Ranks.indexFor(7500));
        assertEquals(10, Ranks.indexFor(9999));
        assertEquals(11, Ranks.indexFor(10_000));
        assertEquals(11, Ranks.indexFor(14_999));
        assertEquals(12, Ranks.indexFor(15_000));
        assertEquals(12, Ranks.indexFor(19_999));
        assertEquals(13, Ranks.indexFor(20_000));
        assertEquals(13, Ranks.indexFor(29_999));
        assertEquals(14, Ranks.indexFor(30_000));
        assertEquals(14, Ranks.indexFor(39_999));
        assertEquals(SENHOR_DA_MANOPLA, Ranks.indexFor(40_000));
        assertEquals(SENHOR_DA_MANOPLA, Ranks.indexFor(49_999));
        assertEquals(ENTIDADE_COSMICA, Ranks.indexFor(50_000));
        assertEquals(ENTIDADE_COSMICA, Ranks.indexFor(1_000_000));
        assertEquals(RECRUTA, Ranks.indexFor(-10));
        assertEquals(17, Ranks.count());
    }

    @Test
    public void oldPatentsKeepTheirLimits() {
        int[] old = {0, 50, 150, 300, 500, 750, 1000, 1500, 2500, 5000};
        for (int rank = 0; rank < old.length; rank++) {
            assertEquals(old[rank], Ranks.minTrophies(rank));
        }
    }

    @Test
    public void trophiesToNextRank() {
        assertEquals(50, Ranks.trophiesToNext(0));
        assertEquals(1, Ranks.trophiesToNext(49));
        assertEquals(100, Ranks.trophiesToNext(50));
        assertEquals(2500, Ranks.trophiesToNext(5000));
        assertEquals(1, Ranks.trophiesToNext(49_999));
        assertEquals(0, Ranks.trophiesToNext(50_000));
        assertTrue(Ranks.isLast(ENTIDADE_COSMICA));
        assertFalse(Ranks.isLast(SENHOR_DA_MANOPLA));
        assertFalse(Ranks.isLast(GUARDIAO_GALACTICO));
    }

    @Test
    public void namesAndDescriptionsMatchTheLimits() throws Exception {
        String xml = new String(Files.readAllBytes(
                new File("src/main/res/values/strings.xml").toPath()), StandardCharsets.UTF_8);
        assertEquals(Ranks.count(), countItems(xml, "rank_names"));
        assertEquals(Ranks.count(), countItems(xml, "rank_descriptions"));
    }

    @Test
    public void trophiesNeverGoBelowZero() {
        PlayerState s = PlayerState.newGame();
        s.addTrophies(10);
        s.addTrophies(-5);
        assertEquals(5, s.getTrophies());
        s.addTrophies(-5);
        s.addTrophies(-5);
        assertEquals(0, s.getTrophies());

        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            s.addTrophies(random.nextBoolean() ? GameBalance.WIN_TROPHIES : GameBalance.LOSS_TROPHIES);
            assertTrue(s.getTrophies() >= 0);
        }
    }

    @Test
    public void bestRankNeverDropsWhenTrophiesDrop() {
        PlayerState s = PlayerState.newGame();
        s.addTrophies(1000);
        assertEquals(LENDA, s.getRank());
        s.addTrophies(-2);
        assertEquals(CAMPEAO, s.getRank());
        assertEquals(LENDA, s.getBestRank());
    }

    @Test
    public void battleRewardsAreSavedTogether() {
        GameAttributes strong = GameAttributes.of(94, 83, 90, 76);
        GameAttributes weak = OwnedHero.starter().getAttributes();
        Random random = new Random(11);
        PlayerState s = PlayerState.newGame();
        int expectedCoins = 0;
        int expectedTrophies = 0;
        for (int i = 0; i < 50; i++) {
            BattleResult r = BattleEngine.fight(strong, weak, random);
            s.applyBattle(r);
            expectedCoins += r.getCoins();
            expectedTrophies = Math.max(0, expectedTrophies + r.getTrophies());
        }
        assertEquals(expectedCoins, s.getCoins());
        assertEquals(expectedTrophies, s.getTrophies());
        assertEquals(50, s.getWins() + s.getLosses());

        Gson gson = new Gson();
        PlayerState loaded = gson.fromJson(gson.toJson(s), PlayerState.class);
        loaded.repair();
        assertEquals(s.getTrophies(), loaded.getTrophies());
        assertEquals(s.getBestRank(), loaded.getBestRank());
        assertEquals(s.getWins(), loaded.getWins());
    }

    @Test
    public void oldSaveWithTheOldTopChestOpenedLosesNothing() {
        Gson gson = new Gson();
        PlayerState old = gson.fromJson("{\"trophies\":6000,\"bestRank\":9,"
                + "\"openedChests\":[1,2,3,4,5,6,7,8,9],\"artifacts\":[]}", PlayerState.class);
        old.repair();
        assertEquals(GUARDIAO_GALACTICO, old.getRank());
        assertTrue(old.isChestOpened(GUARDIAO_GALACTICO));
        assertEquals(0, old.countAvailableChests());
        assertFalse(old.isChestAvailable(ENTIDADE_COSMICA));

        old.addTrophies(50_000 - 6000);
        assertEquals(ENTIDADE_COSMICA, old.getRank());
        assertEquals(7, old.countAvailableChests());
        assertTrue(old.isChestAvailable(ENTIDADE_COSMICA));
    }

    @Test
    public void oldSaveAboveTheNewLimitsUnlocksTheNewChests() {
        Gson gson = new Gson();
        PlayerState old = gson.fromJson("{\"trophies\":12000,\"bestRank\":9,"
                + "\"openedChests\":[9]}", PlayerState.class);
        old.repair();
        assertEquals(11, old.getBestRank());
        assertTrue(old.isChestAvailable(10));
        assertTrue(old.isChestAvailable(11));
        assertFalse(old.isChestAvailable(12));
        assertTrue(old.isChestOpened(9));
    }

    @Test
    public void oldScoreIsImportedOnlyOnce() {
        PlayerState s = PlayerState.newGame();
        assertTrue(s.needsLegacyScoreImport());
        s.importLegacyScore(120, 14, 4);
        assertEquals(120, s.getTrophies());
        assertEquals(14, s.getWins());
        assertEquals(4, s.getLosses());
        assertEquals(HEROI, s.getBestRank());

        assertFalse(s.needsLegacyScoreImport());
        s.importLegacyScore(500, 1, 1);
        assertEquals(120, s.getTrophies());
        assertEquals(14, s.getWins());
    }

    private static int countItems(String xml, String arrayName) {
        Matcher array = Pattern.compile("<string-array name=\"" + arrayName + "\">(.*?)</string-array>",
                Pattern.DOTALL).matcher(xml);
        assertTrue("array " + arrayName + " não encontrado", array.find());
        Matcher item = Pattern.compile("<item>").matcher(array.group(1));
        int count = 0;
        while (item.find()) count++;
        return count;
    }
}
