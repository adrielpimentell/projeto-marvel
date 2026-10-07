package com.example.marvel.game;

import com.example.marvel.data.model.Character;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class PlayerState {

    public enum EquipResult { OK, NOT_OWNED, ALREADY_EQUIPPED, SLOTS_FULL }

    private int coins;
    private int activeHeroId;
    private List<OwnedHero> heroes = new ArrayList<>();

    private int trophies;
    private int wins;
    private int losses;
    private int bestRank;
    private boolean legacyScoreImported;

    private String season;
    private boolean seasonPlayed;
    private Integer seasonSynced;
    private List<String> seasonPrizesGranted = new ArrayList<>();
    private List<String> seasonChestRarities = new ArrayList<>();
    private SeasonResult seasonResult;

    private List<Integer> openedChests = new ArrayList<>();
    private List<String> artifacts = new ArrayList<>();
    private Map<Integer, Integer> teamMilestonesClaimed = new HashMap<>();
    private DailyChallenges daily;
    private int dailyChestsToOpen;

    private SurvivalRun survival;
    private int survivalBestFloor;
    private List<Integer> survivalChestFloors = new ArrayList<>();
    private int survivalChestsToOpen;

    public static PlayerState newGame() {
        PlayerState state = new PlayerState();
        state.coins = GameBalance.STARTING_COINS;
        state.heroes.add(OwnedHero.starter());
        state.activeHeroId = GameBalance.STARTER_HERO_ID;
        return state;
    }

    void repair() {
        if (heroes == null) heroes = new ArrayList<>();
        heroes.removeIf(hero -> hero == null);
        if (heroes.isEmpty()) heroes.add(OwnedHero.starter());
        if (coins < 0) coins = 0;
        if (find(activeHeroId) == null) activeHeroId = heroes.get(0).getCharacterId();
        trophies = Math.max(GameBalance.MIN_TROPHIES, trophies);
        wins = Math.max(0, wins);
        losses = Math.max(0, losses);
        bestRank = Ranks.clamp(Math.max(bestRank, getRank()));
        if (openedChests == null) openedChests = new ArrayList<>();
        if (seasonPrizesGranted == null) seasonPrizesGranted = new ArrayList<>();
        if (seasonChestRarities == null) seasonChestRarities = new ArrayList<>();
        seasonChestRarities.removeIf(rarity -> !isRarityName(rarity));
        if (seasonChestRarities.isEmpty()) seasonResult = null;
        openedChests = withoutNullsOrRepeats(openedChests);
        if (artifacts == null) artifacts = new ArrayList<>();
        artifacts = withoutNullsOrRepeats(artifacts);
        artifacts.removeIf(id -> ArtifactCatalog.byId(id) == null);
        if (teamMilestonesClaimed == null) teamMilestonesClaimed = new HashMap<>();
        teamMilestonesClaimed.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        teamMilestonesClaimed.replaceAll((team, claimed) ->
                Math.max(0, Math.min(TeamAlbum.milestoneCount(), claimed)));
        if (daily != null && !daily.isValid()) daily = null;
        dailyChestsToOpen = Math.max(0, dailyChestsToOpen);
        if (survival != null && (!survival.repair() || find(survival.heroId) == null)) survival = null;
        survivalBestFloor = Math.max(0, survivalBestFloor);
        if (survivalChestFloors == null) survivalChestFloors = new ArrayList<>();
        survivalChestFloors.removeIf(floor -> floor == null);
        survivalChestsToOpen = Math.max(0, survivalChestsToOpen);
        Set<String> used = new HashSet<>();
        for (OwnedHero hero : heroes) {
            List<String> equipped = hero.mutableEquipped();
            equipped.removeIf(id -> id == null || !artifacts.contains(id) || !used.add(id));
            while (equipped.size() > GameBalance.MAX_EQUIPPED_ARTIFACTS) {
                equipped.remove(equipped.size() - 1);
            }
        }
    }

    private static <T> List<T> withoutNullsOrRepeats(List<T> list) {
        List<T> clean = new ArrayList<>();
        Set<T> seen = new HashSet<>();
        for (T item : list) {
            if (item != null && seen.add(item)) clean.add(item);
        }
        return clean;
    }

    public int getCoins() {
        return coins;
    }

    public int getTrophies() {
        return trophies;
    }

    public int getRank() {
        return Ranks.indexFor(trophies);
    }

    public int getBestRank() {
        return bestRank;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public boolean isChestOpened(int rank) {
        return openedChests.contains(rank);
    }

    public boolean isChestAvailable(int rank) {
        return Chests.hasChest(rank) && rank <= bestRank && !isChestOpened(rank);
    }

    public int countAvailableChests() {
        int count = 0;
        for (int rank = 0; rank < Ranks.count(); rank++) {
            if (isChestAvailable(rank)) count++;
        }
        return count;
    }

    public List<String> getArtifactIds() {
        return Collections.unmodifiableList(artifacts);
    }

    public boolean ownsArtifact(String artifactId) {
        return artifacts.contains(artifactId);
    }

    public OwnedHero whoHasEquipped(String artifactId) {
        for (OwnedHero hero : heroes) {
            if (hero.hasEquipped(artifactId)) return hero;
        }
        return null;
    }

    public int getClaimedMilestones(int teamId) {
        Integer claimed = teamMilestonesClaimed.get(teamId);
        return claimed == null ? 0 : claimed;
    }

    public int countTeamSeals() {
        int seals = 0;
        for (int teamId : GameBalance.ALBUM_TEAM_IDS) {
            seals += getClaimedMilestones(teamId);
        }
        return seals;
    }

    public DailyChallenges getDaily() {
        return daily;
    }

    public int getDailyChestsToOpen() {
        return dailyChestsToOpen;
    }

    public SurvivalRun getSurvival() {
        return survival;
    }

    public SurvivalRun getActiveSurvival() {
        return survival != null && !survival.isOver() ? survival : null;
    }

    public int getSurvivalBestFloor() {
        return survivalBestFloor;
    }

    public int getSurvivalChestsToOpen() {
        return survivalChestsToOpen;
    }

    public List<OwnedHero> getHeroes() {
        return Collections.unmodifiableList(heroes);
    }

    public OwnedHero getActiveHero() {
        OwnedHero active = find(activeHeroId);
        return active != null ? active : heroes.get(0);
    }

    public boolean owns(int characterId) {
        return find(characterId) != null;
    }

    public OwnedHero find(int characterId) {
        for (OwnedHero hero : heroes) {
            if (hero.getCharacterId() == characterId) return hero;
        }
        return null;
    }

    void addCoins(int amount) {
        if (amount <= 0) return;
        long total = (long) coins + amount;
        coins = (int) Math.min(Integer.MAX_VALUE, total);
    }

    void addTrophies(int delta) {
        long total = (long) trophies + delta;
        trophies = (int) Math.max(GameBalance.MIN_TROPHIES, Math.min(Integer.MAX_VALUE, total));
        bestRank = Math.max(bestRank, getRank());
    }

    void applyBattle(BattleResult result) {
        addCoins(result.getCoins());
        addTrophies(result.getTrophies());
        seasonPlayed = true;
        if (result.isHeroWinner()) wins++;
        else losses++;
    }

    public String getSeason() {
        return season;
    }

    public boolean isSeasonPlayed() {
        return seasonPlayed;
    }

    boolean ensureSeason(String currentSeason) {
        if (currentSeason.equals(season)) return false;
        season = currentSeason;
        trophies = GameBalance.MIN_TROPHIES;
        bestRank = 0;
        openedChests.clear();
        seasonPlayed = false;
        seasonSynced = null;
        return true;
    }

    public SeasonResult getSeasonResult() {
        return seasonResult;
    }

    public boolean hasSeasonChest() {
        return !seasonChestRarities.isEmpty();
    }

    boolean grantSeasonPrize(SeasonResult result) {
        if (seasonPrizesGranted.contains(result.getSeason())) return false;
        seasonPrizesGranted.add(result.getSeason());
        addCoins(result.getCoins());
        seasonChestRarities.add(result.getRarity().name());
        seasonResult = result;
        return true;
    }

    ChestReward openSeasonChest(Random random) {
        if (seasonChestRarities.isEmpty()) return null;
        Artifact.Rarity rarity = Artifact.Rarity.valueOf(seasonChestRarities.remove(0));
        if (seasonChestRarities.isEmpty()) seasonResult = null;
        return grant(Chests.ofRarity(0, rarity, artifacts, random));
    }

    private static boolean isRarityName(String name) {
        if (name == null) return false;
        for (Artifact.Rarity rarity : Artifact.Rarity.values()) {
            if (rarity.name().equals(name)) return true;
        }
        return false;
    }

    void addDebugTrophies(int amount) {
        addTrophies(amount);
        seasonPlayed = true;
    }

    TrophySync claimTrophySync() {
        if (season == null || !seasonPlayed) return TrophySync.NONE;
        TrophySync sync = TrophySync.between(season, seasonSynced, trophies);
        if (!sync.isEmpty()) seasonSynced = trophies;
        return sync;
    }

    boolean adoptServerTrophies(String serverSeason, Integer serverTrophies) {
        if (!serverSeason.equals(season)) return false;
        if (serverTrophies == null) {
            if (seasonSynced == null) return false;
            seasonSynced = null;
            return true;
        }
        int value = Math.max(GameBalance.MIN_TROPHIES, serverTrophies);
        if (value == trophies && Integer.valueOf(value).equals(seasonSynced) && seasonPlayed) return false;
        trophies = value;
        bestRank = Math.max(bestRank, getRank());
        seasonPlayed = true;
        seasonSynced = value;
        return true;
    }

    boolean needsLegacyScoreImport() {
        return !legacyScoreImported;
    }

    void importLegacyScore(int score, int oldWins, int oldLosses) {
        if (legacyScoreImported) return;
        legacyScoreImported = true;
        addTrophies(Math.max(0, score));
        wins += Math.max(0, oldWins);
        losses += Math.max(0, oldLosses);
    }

    ChestReward openChest(int rank, Random random) {
        if (!isChestAvailable(rank)) return null;
        openedChests.add(rank);
        return grant(Chests.roll(rank, artifacts, random));
    }

    private ChestReward grant(ChestReward reward) {
        if (reward.getArtifact() != null) {
            artifacts.add(reward.getArtifact().getId());
        } else {
            addCoins(reward.getCoins());
        }
        return reward;
    }

    boolean ensureDaily(int today) {
        if (daily != null && daily.getDay() >= today) return false;
        daily = DailyChallenges.generate(today, this);
        return true;
    }

    List<DailyChallenge> recordDaily(BattleFacts facts, int today) {
        ensureDaily(today);
        List<DailyChallenge> completed = daily.record(facts);
        for (DailyChallenge challenge : completed) {
            addCoins(challenge.getCoins());
        }
        if (daily.isAllComplete() && !daily.isChestEarned()) {
            daily.markChestEarned();
            dailyChestsToOpen++;
        }
        return completed;
    }

    ChestReward openDailyChest(Random random) {
        if (dailyChestsToOpen <= 0) return null;
        dailyChestsToOpen--;
        int rank = Ranks.clamp(Math.max(GameBalance.DAILY_CHEST_MIN_RANK, getRank()));
        return grant(Chests.roll(rank, artifacts, random));
    }

    EquipResult equipOnActive(String artifactId) {
        if (!ownsArtifact(artifactId)) return EquipResult.NOT_OWNED;
        OwnedHero active = getActiveHero();
        if (active.hasEquipped(artifactId)) return EquipResult.ALREADY_EQUIPPED;
        if (active.getEquippedArtifactIds().size() >= GameBalance.MAX_EQUIPPED_ARTIFACTS) {
            return EquipResult.SLOTS_FULL;
        }
        OwnedHero previous = whoHasEquipped(artifactId);
        if (previous != null) previous.unequip(artifactId);
        active.equip(artifactId);
        return EquipResult.OK;
    }

    boolean unequipFromActive(String artifactId) {
        return getActiveHero().unequip(artifactId);
    }

    RouletteReward spinRoulette(Roulette roulette, Random random) {
        if (!spend(roulette.getPrice())) return null;
        Artifact.Rarity rarity = roulette.rollRarity(random);
        Artifact prize = ArtifactDrops.pick(rarity, artifacts, random);
        if (prize == null) {
            int coins = ArtifactDrops.duplicateCoins(rarity);
            addCoins(coins);
            return new RouletteReward(roulette, rarity, null, coins);
        }
        artifacts.add(prize.getId());
        return new RouletteReward(roulette, rarity, prize.getId(), 0);
    }

    int claimTeamRewards(int teamId, Collection<Integer> memberIds) {
        if (!TeamAlbum.isAlbumTeam(teamId)) return 0;
        int claimed = getClaimedMilestones(teamId);
        int reached = TeamAlbum.milestonesReached(TeamAlbum.countOwned(this, memberIds));
        if (reached <= claimed) return 0;
        int coins = TeamAlbum.pendingCoins(claimed, reached);
        teamMilestonesClaimed.put(teamId, reached);
        addCoins(coins);
        return coins;
    }

    boolean startSurvival() {
        if (getActiveSurvival() != null) return false;
        survival = new SurvivalRun(getActiveHero().getCharacterId());
        return true;
    }

    boolean setSurvivalEnemy(int enemyId) {
        SurvivalRun run = getActiveSurvival();
        if (run == null || run.hasChoicePending() || run.enemyId != 0) return false;
        if (enemyId <= 0 || enemyId == run.heroId || run.usedEnemyIds.contains(enemyId)) return false;
        run.enemyId = enemyId;
        return true;
    }

    SurvivalOutcome applySurvivalBattle(BattleResult result, BattleFacts facts, int today,
                                        int expectedFloor, int expectedEnemyId, Random random) {
        SurvivalRun run = getActiveSurvival();
        if (run == null || run.hasChoicePending() || run.floor != expectedFloor
                || expectedEnemyId == 0 || run.enemyId != expectedEnemyId) {
            return null;
        }
        OwnedHero hero = find(run.heroId);
        if (hero == null) return null;

        List<DailyChallenge> daily = recordDaily(facts, today);
        run.usedEnemyIds.add(run.enemyId);
        run.enemyId = 0;
        int floor = run.floor;
        if (!result.isHeroWinner()) {
            run.over = true;
            run.heroHp = 0;
            return new SurvivalOutcome(false, floor, 0, false, false, daily);
        }

        int coins = Survival.coinsForFloor(floor);
        addCoins(coins);
        run.coinsEarned += coins;
        boolean record = floor > survivalBestFloor;
        survivalBestFloor = Math.max(survivalBestFloor, floor);
        boolean chest = Survival.isChestFloor(floor) && !survivalChestFloors.contains(floor);
        if (chest) {
            survivalChestFloors.add(floor);
            survivalChestsToOpen++;
        }
        int max = Survival.maxHp(run, hero);
        int healed = Math.round(max * GameBalance.SURVIVAL_HEAL_PERCENT / 100f);
        run.heroHp = Math.max(1, Math.min(max, result.getHeroHpEnd() + healed));
        run.floor++;
        run.offer = Survival.rollOffer(run, hero, random);
        return new SurvivalOutcome(true, floor, coins, record, chest, daily);
    }

    boolean chooseSurvivalBuff(int index) {
        SurvivalRun run = getActiveSurvival();
        if (run == null || index < 0 || index >= run.offer.size()) return false;
        OwnedHero hero = find(run.heroId);
        if (hero == null) return false;
        SurvivalBuff buff = run.offer.get(index);
        int maxBefore = Survival.maxHp(run, hero);
        int hp = Survival.currentHp(run, hero);
        if (buff == SurvivalBuff.MEDKIT) {
            hp = Math.min(maxBefore, hp + Math.round(maxBefore * GameBalance.SURVIVAL_MEDKIT_HEAL_PERCENT / 100f));
        } else {
            run.buffs.add(buff);
            if (buff == SurvivalBuff.ARMOR) hp += Survival.maxHp(run, hero) - maxBefore;
        }
        run.heroHp = hp;
        run.offer.clear();
        return true;
    }

    boolean skipSurvivalFloor() {
        SurvivalRun run = getActiveSurvival();
        if (run == null || run.hasChoicePending()) return false;
        run.floor++;
        run.enemyId = 0;
        return true;
    }

    boolean endSurvival() {
        SurvivalRun run = getActiveSurvival();
        if (run == null) return false;
        run.over = true;
        run.offer.clear();
        return true;
    }

    ChestReward openSurvivalChest(Random random) {
        if (survivalChestsToOpen <= 0) return null;
        survivalChestsToOpen--;
        int rank = Ranks.clamp(Math.max(GameBalance.DAILY_CHEST_MIN_RANK, getRank()));
        return grant(Chests.roll(rank, artifacts, random));
    }

    boolean spend(int amount) {
        if (amount < 0 || amount > coins) return false;
        coins -= amount;
        return true;
    }

    boolean buy(OwnedHero hero, int price) {
        if (hero == null || owns(hero.getCharacterId())) return false;
        if (!spend(price)) return false;
        heroes.add(hero);
        return true;
    }

    boolean upgrade(int characterId, OwnedHero.Stat stat) {
        OwnedHero hero = find(characterId);
        if (hero == null || hero.isMaxed(stat)) return false;
        if (!spend(Economy.upgradeCost(hero.getUpgrades(stat)))) return false;
        return hero.addUpgrade(stat);
    }

    boolean setActiveHero(int characterId) {
        if (!owns(characterId)) return false;
        activeHeroId = characterId;
        return true;
    }

    boolean updateHeroInfo(int characterId, String name, String realName, String imageUrl) {
        OwnedHero hero = find(characterId);
        if (hero == null) return false;
        hero.updateInfo(name, realName, imageUrl);
        return true;
    }

    boolean updateHeroDetails(Character character) {
        OwnedHero hero = character == null ? null : find(character.getId());
        if (hero == null || !character.hasDetails()) return false;
        hero.updateInfo(character.getName(), character.getRealName(), character.getCardImageUrl());
        hero.updateTags(character);
        return true;
    }
}
