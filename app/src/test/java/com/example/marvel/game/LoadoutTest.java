package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LoadoutTest {

    private static final String GARRAS = "garras_adamantium";
    private static final String SORO = "soro_supersoldado";
    private static final String FRAGMENTO = "fragmento_cosmico";
    private static final String ESCUDO = "escudo_vibranium";

    private static PlayerState playerWithAllArtifacts() {
        PlayerState s = PlayerState.newGame();
        s.addTrophies(5000);
        Random random = new Random(1);
        for (int rank = 1; rank < Ranks.count(); rank++) s.openChest(rank, random);
        Gson gson = new Gson();
        String json = gson.toJson(s).replaceFirst("\"artifacts\":\\[[^\\]]*\\]",
                "\"artifacts\":" + gson.toJson(allIds()));
        PlayerState full = gson.fromJson(json, PlayerState.class);
        full.repair();
        return full;
    }

    private static List<String> allIds() {
        List<String> ids = new ArrayList<>();
        for (Artifact a : ArtifactCatalog.all()) ids.add(a.getId());
        return ids;
    }

    @Test
    public void attributeBonusesAddUpAndRespectTheLimits() {
        GameAttributes base = GameAttributes.of(94, 83, 90, 76);
        Artifact garras = ArtifactCatalog.byId(GARRAS);
        Artifact soro = ArtifactCatalog.byId(SORO);

        assertEquals(91, Loadout.withBonuses(base, Collections.singletonList(garras)).getStrength());
        assertEquals(95, Loadout.withBonuses(base, Arrays.asList(garras, soro)).getStrength());
        assertEquals(19, Loadout.boost(18, 5));
        assertEquals(100, Loadout.boost(98, 10));
        assertEquals(50, Loadout.boost(50, 0));

        GameAttributes all = Loadout.withBonuses(base,
                Collections.singletonList(ArtifactCatalog.byId(FRAGMENTO)));
        assertTrue(all.getLife() > base.getLife() && all.getIntelligence() > base.getIntelligence());
        assertTrue(all.getOverall() > base.getOverall());
    }

    @Test
    public void onlyOwnedArtifactsAndAtMostTwo() {
        PlayerState s = PlayerState.newGame();
        assertEquals(PlayerState.EquipResult.NOT_OWNED, s.equipOnActive(GARRAS));

        PlayerState full = playerWithAllArtifacts();
        assertEquals(PlayerState.EquipResult.OK, full.equipOnActive(GARRAS));
        assertEquals(PlayerState.EquipResult.ALREADY_EQUIPPED, full.equipOnActive(GARRAS));
        assertEquals(PlayerState.EquipResult.OK, full.equipOnActive(ESCUDO));
        assertEquals(PlayerState.EquipResult.SLOTS_FULL, full.equipOnActive(SORO));
        assertEquals(2, full.getActiveHero().getEquippedArtifactIds().size());

        assertTrue(full.unequipFromActive(GARRAS));
        assertEquals(PlayerState.EquipResult.OK, full.equipOnActive(SORO));
    }

    @Test
    public void equippingMovesTheArtifactFromAnotherHero() {
        PlayerState s = playerWithAllArtifacts();
        s.addCoins(10_000);
        s.buy(new OwnedHero(1440, GameAttributes.of(90, 80, 90, 70)), 0);

        s.equipOnActive(GARRAS);
        s.setActiveHero(1440);
        assertEquals(PlayerState.EquipResult.OK, s.equipOnActive(GARRAS));
        assertTrue(s.find(1440).hasEquipped(GARRAS));
        assertFalse(s.find(GameBalance.STARTER_HERO_ID).hasEquipped(GARRAS));
        assertEquals(1440, s.whoHasEquipped(GARRAS).getCharacterId());
    }

    @Test
    public void battleAttributesUseEquippedBonuses() {
        PlayerState s = playerWithAllArtifacts();
        OwnedHero hero = s.getActiveHero();
        GameAttributes before = hero.getBattleAttributes();
        s.equipOnActive(FRAGMENTO);
        assertTrue(hero.getBattleAttributes().getOverall() > before.getOverall());
        assertEquals(hero.getAttributes().getOverall(), before.getOverall());
    }

    @Test
    public void equipmentSurvivesSavingAndBrokenSavesAreFixed() {
        PlayerState s = playerWithAllArtifacts();
        s.equipOnActive(GARRAS);
        s.equipOnActive(ESCUDO);

        Gson gson = new Gson();
        PlayerState loaded = gson.fromJson(gson.toJson(s), PlayerState.class);
        loaded.repair();
        assertEquals(Arrays.asList(GARRAS, ESCUDO), loaded.getActiveHero().getEquippedArtifactIds());

        String broken = gson.toJson(PlayerState.newGame()).replace("\"heroes\":[{",
                "\"heroes\":[{\"equippedArtifacts\":[\"" + GARRAS + "\",\"nao_existe\",\"" + ESCUDO + "\",\""
                        + SORO + "\"],");
        PlayerState fixed = gson.fromJson(broken, PlayerState.class);
        fixed.repair();
        assertTrue(fixed.getActiveHero().getEquippedArtifactIds().isEmpty());
        assertNull(fixed.whoHasEquipped(GARRAS));
    }
}
