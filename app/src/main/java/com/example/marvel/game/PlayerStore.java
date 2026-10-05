package com.example.marvel.game;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.marvel.data.model.Character;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class PlayerStore {

    private static final String PREFS_NAME = "player_save";
    private static final String KEY_STATE = "state_json";

    private static PlayerStore instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private PlayerState state;

    public static synchronized PlayerStore getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerStore(context.getApplicationContext());
        }
        return instance;
    }

    private PlayerStore(Context appContext) {
        this.appContext = appContext;
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        state = load();
    }

    public PlayerState get() {
        return state;
    }

    public boolean addCoins(int amount) {
        return change(s -> {
            s.addCoins(amount);
            return true;
        });
    }

    public boolean applyBattle(BattleResult result) {
        return change(s -> {
            s.applyBattle(result);
            return true;
        });
    }

    public List<DailyChallenge> applyBattle(BattleResult result, BattleFacts facts, int today) {
        List<DailyChallenge> completed = new ArrayList<>();
        boolean saved = change(s -> {
            s.applyBattle(result);
            completed.addAll(s.recordDaily(facts, today));
            return true;
        });
        return saved ? completed : Collections.emptyList();
    }

    public void ensureDailyChallenges(int today) {
        change(s -> s.ensureDaily(today));
    }

    public ChestReward openDailyChest() {
        ChestReward[] reward = new ChestReward[1];
        boolean saved = change(s -> {
            reward[0] = s.openDailyChest(new Random());
            return reward[0] != null;
        });
        return saved ? reward[0] : null;
    }

    public boolean startSurvival() {
        return change(PlayerState::startSurvival);
    }

    public boolean setSurvivalEnemy(int enemyId) {
        return change(s -> s.setSurvivalEnemy(enemyId));
    }

    public SurvivalOutcome applySurvivalBattle(BattleResult result, BattleFacts facts, int today,
                                               int floor, int enemyId) {
        SurvivalOutcome[] outcome = new SurvivalOutcome[1];
        boolean saved = change(s -> {
            outcome[0] = s.applySurvivalBattle(result, facts, today, floor, enemyId, new Random());
            return outcome[0] != null;
        });
        return saved ? outcome[0] : null;
    }

    public boolean chooseSurvivalBuff(int index) {
        return change(s -> s.chooseSurvivalBuff(index));
    }

    public boolean skipSurvivalFloor() {
        return change(PlayerState::skipSurvivalFloor);
    }

    public boolean endSurvival() {
        return change(PlayerState::endSurvival);
    }

    public ChestReward openSurvivalChest() {
        ChestReward[] reward = new ChestReward[1];
        boolean saved = change(s -> {
            reward[0] = s.openSurvivalChest(new Random());
            return reward[0] != null;
        });
        return saved ? reward[0] : null;
    }

    public boolean addTrophies(int delta) {
        return change(s -> {
            s.addTrophies(delta);
            return true;
        });
    }

    public ChestReward openChest(int rank) {
        ChestReward[] reward = new ChestReward[1];
        boolean saved = change(s -> {
            reward[0] = s.openChest(rank, new Random());
            return reward[0] != null;
        });
        return saved ? reward[0] : null;
    }

    public PlayerState.EquipResult equipArtifact(String artifactId) {
        PlayerState.EquipResult[] result = new PlayerState.EquipResult[1];
        boolean saved = change(s -> {
            result[0] = s.equipOnActive(artifactId);
            return result[0] == PlayerState.EquipResult.OK;
        });
        if (result[0] == PlayerState.EquipResult.OK && !saved) return PlayerState.EquipResult.NOT_OWNED;
        return result[0];
    }

    public boolean unequipArtifact(String artifactId) {
        return change(s -> s.unequipFromActive(artifactId));
    }

    public RouletteReward spinRoulette(Roulette roulette) {
        RouletteReward[] reward = new RouletteReward[1];
        boolean saved = change(s -> {
            reward[0] = s.spinRoulette(roulette, new Random());
            return reward[0] != null;
        });
        return saved ? reward[0] : null;
    }

    public int claimTeamRewards(int teamId, Collection<Integer> memberIds) {
        int[] coins = new int[1];
        boolean saved = change(s -> {
            coins[0] = s.claimTeamRewards(teamId, memberIds);
            return coins[0] > 0;
        });
        return saved ? coins[0] : 0;
    }

    public boolean buyHero(OwnedHero hero, int price) {
        return change(s -> s.buy(hero, price));
    }

    public boolean upgrade(int characterId, OwnedHero.Stat stat) {
        return change(s -> s.upgrade(characterId, stat));
    }

    public boolean setActiveHero(int characterId) {
        return change(s -> s.setActiveHero(characterId));
    }

    public boolean updateHeroInfo(int characterId, String name, String realName, String imageUrl) {
        return change(s -> s.updateHeroInfo(characterId, name, realName, imageUrl));
    }

    public boolean updateHeroDetails(Character character) {
        return change(s -> s.updateHeroDetails(character));
    }

    private synchronized boolean change(Predicate<PlayerState> mutation) {
        String before = gson.toJson(state);
        if (!mutation.test(state)) {
            if (!gson.toJson(state).equals(before)) {
                state = gson.fromJson(before, PlayerState.class);
            }
            return false;
        }
        boolean saved = prefs.edit().putString(KEY_STATE, gson.toJson(state)).commit();
        if (!saved) {
            state = gson.fromJson(before, PlayerState.class);
        }
        return saved;
    }

    private PlayerState load() {
        String json = prefs.getString(KEY_STATE, null);
        PlayerState loaded = null;
        if (json != null) {
            try {
                loaded = gson.fromJson(json, PlayerState.class);
            } catch (JsonParseException e) {
                loaded = null;
            }
        }
        if (loaded == null) {
            loaded = PlayerState.newGame();
        }
        loaded.repair();
        if (loaded.needsLegacyScoreImport()) {
            ScoreStore legacy = new ScoreStore(appContext);
            loaded.importLegacyScore(legacy.getScore(), legacy.getWins(), legacy.getLosses());
        }
        prefs.edit().putString(KEY_STATE, gson.toJson(loaded)).apply();
        return loaded;
    }
}
