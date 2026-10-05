package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ChestsTest {

    private static final int HEROI = 1;
    private static final int VINGADOR = 2;
    private static final int GUARDIAO_GALACTICO = 9;
    private static final int ENTIDADE_COSMICA = 16;

    @Test
    public void catalogHasAtLeast15ValidArtifacts() {
        List<Artifact> all = ArtifactCatalog.all();
        assertTrue(all.size() >= 15);

        Set<String> ids = new HashSet<>();
        Set<Artifact.Rarity> rarities = new HashSet<>();
        Set<Artifact.Type> types = new HashSet<>();
        for (Artifact a : all) {
            assertTrue("id repetido: " + a.getId(), ids.add(a.getId()));
            assertFalse(a.getName().isEmpty());
            assertFalse("sem descrição: " + a.getId(), a.describe().isEmpty());
            rarities.add(a.getRarity());
            types.add(a.getType());
            if (a.getType() == Artifact.Type.SPECIAL) {
                assertTrue(a.getEffect() != Artifact.Effect.NONE);
            }
        }
        assertEquals(4, rarities.size());
        assertEquals(2, types.size());
    }

    @Test
    public void chanceTableIsValidAndBetterForHigherRanks() {
        int[][] w = GameBalance.CHEST_RARITY_WEIGHTS;
        assertEquals(Ranks.count(), w.length);
        for (int rank = HEROI; rank < w.length; rank++) {
            int total = w[rank][0] + w[rank][1] + w[rank][2] + w[rank][3];
            assertEquals("patente " + rank, 100, total);
            assertTrue(w[rank][0] <= w[rank - 1][0] || rank == HEROI);
            assertTrue(w[rank][3] >= w[rank - 1][3]);
        }
    }

    @Test
    public void higherRankChestsGiveRarerArtifacts() {
        Map<Artifact.Rarity, Integer> low = countRarities(HEROI, 10_000);
        Map<Artifact.Rarity, Integer> high = countRarities(GUARDIAO_GALACTICO, 10_000);
        assertEquals(0, (int) low.get(Artifact.Rarity.LEGENDARY));
        assertTrue(high.get(Artifact.Rarity.LEGENDARY) > 3000);
        assertTrue(low.get(Artifact.Rarity.COMMON) > high.get(Artifact.Rarity.COMMON));
    }

    @Test
    public void legendaryChanceNeverDropsAndTheTopChestIsAlwaysLegendary() {
        int[][] w = GameBalance.CHEST_RARITY_WEIGHTS;
        int previous = 0;
        for (int rank = 1; rank < w.length; rank++) {
            int sum = 0;
            for (int weight : w[rank]) sum += weight;
            assertEquals("patente " + rank, 100, sum);
            assertTrue("patente " + rank, w[rank][3] >= previous);
            previous = w[rank][3];
        }
        Map<Artifact.Rarity, Integer> top = countRarities(ENTIDADE_COSMICA, 2_000);
        assertEquals(2_000, (int) top.get(Artifact.Rarity.LEGENDARY));
    }

    @Test
    public void recruitHasNoChestAndHeroUnlocksOne() {
        PlayerState s = PlayerState.newGame();
        assertFalse(s.isChestAvailable(0));
        assertFalse(s.isChestAvailable(HEROI));
        assertNull(s.openChest(HEROI, new Random(1)));

        s.addTrophies(50);
        assertTrue(s.isChestAvailable(HEROI));
        assertEquals(1, s.countAvailableChests());
    }

    @Test
    public void eachChestOpensOnlyOnceEvenAfterFallingRank() {
        PlayerState s = PlayerState.newGame();
        s.addTrophies(150);
        assertEquals(2, s.countAvailableChests());

        ChestReward reward = s.openChest(HEROI, new Random(2));
        assertNotNull(reward);
        assertTrue(s.isChestOpened(HEROI));
        assertNull("não abre duas vezes", s.openChest(HEROI, new Random(3)));

        s.addTrophies(-150);
        assertTrue("o baú aberto continua aberto", s.isChestOpened(HEROI));
        assertTrue("o baú liberado continua liberado", s.isChestAvailable(VINGADOR));

        s.addTrophies(150);
        assertNull(s.openChest(HEROI, new Random(4)));
    }

    @Test
    public void openedChestGivesArtifactOnce() {
        PlayerState s = PlayerState.newGame();
        s.addTrophies(50_000);
        Set<String> seen = new HashSet<>();
        for (int rank = HEROI; rank <= ENTIDADE_COSMICA; rank++) {
            ChestReward reward = s.openChest(rank, new Random(rank));
            assertNotNull(reward);
            if (reward.getArtifact() != null) {
                assertTrue("artefato repetido", seen.add(reward.getArtifact().getId()));
                assertTrue(s.ownsArtifact(reward.getArtifact().getId()));
            }
        }
        assertEquals(0, s.countAvailableChests());
    }

    @Test
    public void completeRarityTurnsIntoCoins() {
        Set<String> ownsAllCommons = new HashSet<>();
        for (Artifact a : ArtifactCatalog.ofRarity(Artifact.Rarity.COMMON)) ownsAllCommons.add(a.getId());
        Random random = new Random(5);
        for (int i = 0; i < 200; i++) {
            ChestReward reward = Chests.roll(HEROI, ownsAllCommons, random);
            if (reward.getRarity() == Artifact.Rarity.COMMON) {
                assertNull(reward.getArtifact());
                assertEquals(GameBalance.DUPLICATE_COINS[0], reward.getCoins());
                return;
            }
        }
        throw new AssertionError("nenhum Comum sorteado em 200 tentativas");
    }

    @Test
    public void openedChestsAndArtifactsSurviveSaving() {
        PlayerState s = PlayerState.newGame();
        s.addTrophies(300);
        s.openChest(HEROI, new Random(8));
        s.openChest(VINGADOR, new Random(9));

        Gson gson = new Gson();
        PlayerState loaded = gson.fromJson(gson.toJson(s), PlayerState.class);
        loaded.repair();
        assertTrue(loaded.isChestOpened(HEROI));
        assertTrue(loaded.isChestOpened(VINGADOR));
        assertEquals(s.getArtifactIds(), loaded.getArtifactIds());
        assertEquals(s.getCoins(), loaded.getCoins());
        assertTrue(loaded.isChestAvailable(3));
    }

    private static Map<Artifact.Rarity, Integer> countRarities(int rank, int rolls) {
        Map<Artifact.Rarity, Integer> counts = new EnumMap<>(Artifact.Rarity.class);
        for (Artifact.Rarity r : Artifact.Rarity.values()) counts.put(r, 0);
        Random random = new Random(42);
        for (int i = 0; i < rolls; i++) {
            Artifact.Rarity r = Chests.rollRarity(rank, random);
            counts.put(r, counts.get(r) + 1);
        }
        return counts;
    }
}
