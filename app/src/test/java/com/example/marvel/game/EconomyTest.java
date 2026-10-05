package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class EconomyTest {

    @Test
    public void starterHeroIsWeak() {
        GameAttributes a = OwnedHero.starter().getAttributes();
        assertTrue("Overall do herói inicial deve ser ≤ 25", a.getOverall() <= 25);
        assertEquals(GameBalance.STARTER_HERO_ID, OwnedHero.starter().getCharacterId());
    }

    @Test
    public void newGameStartsWithZeroCoinsAndStarterActive() {
        PlayerState s = PlayerState.newGame();
        assertEquals(0, s.getCoins());
        assertEquals(1, s.getHeroes().size());
        assertEquals(GameBalance.STARTER_HERO_ID, s.getActiveHero().getCharacterId());
    }

    @Test
    public void victoryCoinsFollowEnemyStrength() {
        assertEquals(100, Economy.coinsForVictory(80, 20));
        assertEquals(100, Economy.coinsForVictory(80, 40));
        assertEquals(300, Economy.coinsForVictory(50, 50));
        assertEquals(500, Economy.coinsForVictory(40, 60));
        assertEquals(500, Economy.coinsForVictory(24, 86));
        assertEquals(500, Economy.coinsForVictory(0, 50));
    }

    @Test
    public void victoryCoinsGrowWithEnemy() {
        int previous = 0;
        for (int enemy = 1; enemy <= 100; enemy++) {
            int coins = Economy.coinsForVictory(50, enemy);
            assertTrue(coins >= previous);
            assertTrue(coins >= GameBalance.VICTORY_COINS_MIN && coins <= GameBalance.VICTORY_COINS_MAX);
            previous = coins;
        }
    }

    @Test
    public void priceGrowsWithOverall() {
        assertEquals(1000, Economy.heroPrice(24));
        assertEquals(3550, Economy.heroPrice(52));
        assertEquals(5350, Economy.heroPrice(65));
        assertEquals(9200, Economy.heroPrice(86));
        assertEquals(10700, Economy.heroPrice(93));
        int previous = 0;
        for (int overall = 0; overall <= 100; overall++) {
            int price = Economy.heroPrice(overall);
            assertTrue(price >= previous);
            assertEquals(0, price % GameBalance.PRICE_ROUNDING);
            previous = price;
        }
    }

    @Test
    public void boughtHeroKeepsFormulaAttributesAndInfo() {
        com.example.marvel.data.model.Character wolverine = new Gson().fromJson(
                "{\"id\":1440,\"name\":\"Wolverine\",\"count_of_issue_appearances\":999,"
                        + "\"powers\":[{\"id\":1,\"name\":\"Healing\"}],\"teams\":[],\"movies\":[]}",
                com.example.marvel.data.model.Character.class);
        OwnedHero hero = OwnedHero.fromCharacter(wolverine);

        assertEquals(1440, hero.getCharacterId());
        assertEquals("Wolverine", hero.getName());
        assertEquals(AttributeCalculator.calculate(wolverine).getOverall(),
                hero.getAttributes().getOverall());
    }

    @Test
    public void spendingMoreThanYouHaveChangesNothing() {
        PlayerState s = PlayerState.newGame();
        s.addCoins(120);
        assertFalse(s.spend(121));
        assertEquals(120, s.getCoins());
        assertFalse(s.spend(-10));
        assertTrue(s.spend(120));
        assertEquals(0, s.getCoins());
    }

    @Test
    public void buyingNeedsCoinsAndHappensOnce() {
        PlayerState s = PlayerState.newGame();
        OwnedHero hero = new OwnedHero(1440, GameAttributes.of(90, 80, 90, 70));

        assertFalse(s.buy(hero, 500));
        assertEquals(1, s.getHeroes().size());
        assertEquals(0, s.getCoins());

        s.addCoins(600);
        assertTrue(s.buy(hero, 500));
        assertEquals(100, s.getCoins());
        assertTrue(s.owns(1440));
        assertFalse(s.buy(hero, 0));
    }

    @Test
    public void upgradesCostMoreEachTimeAndStopAtMax() {
        int base = GameBalance.UPGRADE_BASE_COST;
        assertEquals(base, Economy.upgradeCost(0));
        assertEquals(2 * base, Economy.upgradeCost(1));
        assertEquals(3 * base, Economy.upgradeCost(2));
        assertTrue(Economy.upgradeCost(1) > Economy.upgradeCost(0));

        PlayerState s = PlayerState.newGame();
        s.addCoins(1_000_000);
        int id = GameBalance.STARTER_HERO_ID;
        while (s.upgrade(id, OwnedHero.Stat.STRENGTH)) {
        }
        assertEquals(100, s.find(id).getAttributes().getStrength());
        assertTrue(s.find(id).isMaxed(OwnedHero.Stat.STRENGTH));
    }

    @Test
    public void upgradeRaisesAttributeRecalculatesOverallAndGetsPricier() {
        PlayerState s = PlayerState.newGame();
        int id = GameBalance.STARTER_HERO_ID;
        int overallBefore = s.find(id).getAttributes().getOverall();

        assertFalse("sem moedas não melhora", s.upgrade(id, OwnedHero.Stat.SPEED));
        assertEquals(0, s.find(id).getUpgrades(OwnedHero.Stat.SPEED));

        int base = GameBalance.UPGRADE_BASE_COST;
        s.addCoins(base + 2 * base + base);
        assertTrue(s.upgrade(id, OwnedHero.Stat.SPEED));
        assertTrue(s.upgrade(id, OwnedHero.Stat.SPEED));
        assertTrue(s.upgrade(id, OwnedHero.Stat.LIFE));
        assertEquals(0, s.getCoins());

        GameAttributes after = s.find(id).getAttributes();
        assertEquals(GameBalance.STARTER_SPEED + 2 * GameBalance.UPGRADE_STEP, after.getSpeed());
        assertEquals(GameBalance.STARTER_LIFE + GameBalance.UPGRADE_STEP, after.getLife());
        assertTrue(after.getOverall() > overallBefore);
        assertEquals(3 * base, Economy.upgradeCost(s.find(id).getUpgrades(OwnedHero.Stat.SPEED)));
    }

    @Test
    public void onlyOwnedHeroCanBeActive() {
        PlayerState s = PlayerState.newGame();
        assertFalse(s.setActiveHero(1440));
        assertEquals(GameBalance.STARTER_HERO_ID, s.getActiveHero().getCharacterId());

        s.addCoins(5000);
        s.buy(new OwnedHero(1440, GameAttributes.of(90, 80, 90, 70)), 3000);
        assertTrue(s.setActiveHero(1440));
        assertEquals(1440, s.getActiveHero().getCharacterId());
    }

    @Test
    public void saveRoundTripKeepsEverything() {
        Gson gson = new Gson();
        PlayerState s = PlayerState.newGame();
        s.addCoins(900);
        s.buy(new OwnedHero(1440, GameAttributes.of(90, 80, 90, 70)), 500);
        s.upgrade(GameBalance.STARTER_HERO_ID, OwnedHero.Stat.LIFE);
        s.setActiveHero(1440);
        s.updateHeroInfo(1440, "Wolverine", "James Howlett", "https://exemplo.com/w.jpg");

        PlayerState loaded = gson.fromJson(gson.toJson(s), PlayerState.class);
        loaded.repair();

        assertEquals(900 - 500 - GameBalance.UPGRADE_BASE_COST, loaded.getCoins());
        assertEquals(2, loaded.getHeroes().size());
        assertEquals(1440, loaded.getActiveHero().getCharacterId());
        assertEquals("Wolverine", loaded.getActiveHero().getName());
        OwnedHero starter = loaded.find(GameBalance.STARTER_HERO_ID);
        assertNotNull(starter);
        assertEquals(1, starter.getUpgrades(OwnedHero.Stat.LIFE));
        assertEquals(GameBalance.STARTER_LIFE + GameBalance.UPGRADE_STEP,
                starter.getAttributes().getLife());
    }

    @Test
    public void brokenSaveIsRepaired() {
        PlayerState broken = new Gson().fromJson("{\"coins\":-40,\"heroes\":[],\"activeHeroId\":7}",
                PlayerState.class);
        broken.repair();

        assertEquals(0, broken.getCoins());
        assertEquals(GameBalance.STARTER_HERO_ID, broken.getActiveHero().getCharacterId());
    }
}
