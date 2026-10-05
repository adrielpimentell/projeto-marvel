package com.example.marvel.game;

import com.example.marvel.data.model.NamedRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public final class DailyChallenges {

    public static final int COUNT = 3;

    private int day;
    private List<DailyChallenge> challenges = new ArrayList<>();
    private boolean chestEarned;

    private DailyChallenges() {
    }

    private DailyChallenges(int day, List<DailyChallenge> challenges) {
        this.day = day;
        this.challenges = challenges;
    }

    public int getDay() {
        return day;
    }

    public List<DailyChallenge> getChallenges() {
        return Collections.unmodifiableList(challenges);
    }

    public int countComplete() {
        int done = 0;
        for (DailyChallenge challenge : challenges) {
            if (challenge.isComplete()) done++;
        }
        return done;
    }

    public boolean isAllComplete() {
        return countComplete() == challenges.size();
    }

    public boolean isChestEarned() {
        return chestEarned;
    }

    void markChestEarned() {
        chestEarned = true;
    }

    boolean isValid() {
        if (challenges == null || challenges.size() != COUNT) return false;
        for (DailyChallenge challenge : challenges) {
            if (challenge == null || !challenge.isValid()) return false;
        }
        return true;
    }

    List<DailyChallenge> record(BattleFacts facts) {
        List<DailyChallenge> completed = new ArrayList<>();
        for (DailyChallenge challenge : challenges) {
            if (challenge.record(facts)) completed.add(challenge);
        }
        return completed;
    }

    static DailyChallenges generate(int day, PlayerState state) {
        Random random = new Random(seed(day));
        List<DailyChallenge> list = new ArrayList<>();
        list.add(battleCountChallenge(random));
        list.add(comicVineChallenge(random, state));
        list.add(extraChallenge(random, state));
        return new DailyChallenges(day, list);
    }

    static long seed(int day) {
        return day * 2_654_435_761L;
    }

    private static DailyChallenge battleCountChallenge(Random random) {
        int coins = GameBalance.DAILY_COINS[0];
        switch (random.nextInt(3)) {
            case 0:
                return new DailyChallenge(DailyChallenge.Type.WIN_BATTLES, 0, null,
                        pick(random, GameBalance.DAILY_WIN_TARGETS), coins);
            case 1:
                return new DailyChallenge(DailyChallenge.Type.PLAY_BATTLES, 0, null,
                        pick(random, GameBalance.DAILY_PLAY_TARGETS), coins);
            default:
                return new DailyChallenge(DailyChallenge.Type.LAND_CRITS, 0, null,
                        pick(random, GameBalance.DAILY_CRIT_TARGETS), coins);
        }
    }

    private static DailyChallenge comicVineChallenge(Random random, PlayerState state) {
        Map<Integer, String> albumTeams = new TreeMap<>();
        Map<Integer, String> teams = new TreeMap<>();
        Map<Integer, String> powers = new TreeMap<>();
        for (OwnedHero hero : state.getHeroes()) {
            for (NamedRef team : hero.getTeams()) {
                if (team == null || team.getName().isEmpty()) continue;
                teams.put(team.getId(), team.getName());
                if (TeamAlbum.isAlbumTeam(team.getId())) albumTeams.put(team.getId(), team.getName());
            }
            for (NamedRef power : hero.getPowers()) {
                if (power != null && !power.getName().isEmpty()) powers.put(power.getId(), power.getName());
            }
        }

        List<DailyChallenge.Type> types = new ArrayList<>();
        types.add(DailyChallenge.Type.DEFEAT_ORIGIN);
        if (!albumTeams.isEmpty()) types.add(DailyChallenge.Type.DEFEAT_TEAM_MEMBER);
        if (!teams.isEmpty()) types.add(DailyChallenge.Type.WIN_WITH_TEAM);
        if (!powers.isEmpty()) types.add(DailyChallenge.Type.WIN_WITH_POWER);
        if (!state.getArtifactIds().isEmpty()) types.add(DailyChallenge.Type.WIN_WITH_ARTIFACT);

        int coins = GameBalance.DAILY_COINS[1];
        DailyChallenge.Type type = types.get(random.nextInt(types.size()));
        switch (type) {
            case DEFEAT_TEAM_MEMBER: {
                Map.Entry<Integer, String> team = nth(albumTeams, random);
                return new DailyChallenge(type, team.getKey(), team.getValue(), 1, coins);
            }
            case WIN_WITH_TEAM: {
                Map.Entry<Integer, String> team = nth(teams, random);
                return new DailyChallenge(type, team.getKey(), team.getValue(), 1, coins);
            }
            case WIN_WITH_POWER: {
                Map.Entry<Integer, String> power = nth(powers, random);
                return new DailyChallenge(type, power.getKey(), power.getValue(), 1, coins);
            }
            case WIN_WITH_ARTIFACT:
                return new DailyChallenge(type, 0, null, 1, coins);
            default:
                return new DailyChallenge(DailyChallenge.Type.DEFEAT_ORIGIN,
                        pick(random, GameBalance.DAILY_ORIGIN_IDS), null, 1, coins);
        }
    }

    private static DailyChallenge extraChallenge(Random random, PlayerState state) {
        int coins = GameBalance.DAILY_COINS[2];
        int best = 0;
        for (OwnedHero hero : state.getHeroes()) {
            best = Math.max(best, hero.getBattleAttributes().getOverall());
        }
        int goal = Math.min(GameBalance.MAX_ATTRIBUTE,
                roundUpTo5(best + GameBalance.DAILY_OVERALL_STEP));
        boolean overallPossible = goal > best;
        if (overallPossible && random.nextBoolean()) {
            return new DailyChallenge(DailyChallenge.Type.WIN_WITH_OVERALL, goal, null, 1, coins);
        }
        return new DailyChallenge(DailyChallenge.Type.WIN_VS_STRONGER, 0, null, 1, coins);
    }

    private static int roundUpTo5(int value) {
        return (value + 4) / 5 * 5;
    }

    private static int pick(Random random, int[] options) {
        return options[random.nextInt(options.length)];
    }

    private static Map.Entry<Integer, String> nth(Map<Integer, String> sorted, Random random) {
        int index = random.nextInt(sorted.size());
        for (Map.Entry<Integer, String> entry : sorted.entrySet()) {
            if (index-- == 0) return entry;
        }
        throw new IllegalStateException("mapa vazio");
    }
}
