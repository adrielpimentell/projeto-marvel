package com.example.marvel.game;

import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.NamedRef;

import java.util.ArrayList;
import java.util.List;

public final class DailyChallenge {

    public enum Type {
        WIN_BATTLES,
        PLAY_BATTLES,
        LAND_CRITS,
        DEFEAT_ORIGIN,
        DEFEAT_TEAM_MEMBER,
        WIN_WITH_TEAM,
        WIN_WITH_POWER,
        WIN_WITH_ARTIFACT,
        WIN_VS_STRONGER,
        WIN_WITH_OVERALL
    }

    private Type type;
    private int param;
    private String paramName;
    private int target;
    private int progress;
    private int coins;

    private DailyChallenge() {
    }

    DailyChallenge(Type type, int param, String paramName, int target, int coins) {
        this.type = type;
        this.param = param;
        this.paramName = paramName;
        this.target = target;
        this.coins = coins;
    }

    public Type getType() {
        return type;
    }

    public int getParam() {
        return param;
    }

    public String getParamName() {
        return paramName == null ? "" : paramName;
    }

    public int getTarget() {
        return target;
    }

    public int getProgress() {
        return Math.min(progress, target);
    }

    public int getCoins() {
        return coins;
    }

    public boolean isComplete() {
        return progress >= target;
    }

    boolean isValid() {
        return type != null && target > 0 && coins >= 0 && progress >= 0;
    }

    boolean record(BattleFacts facts) {
        if (isComplete()) return false;
        int gained = gain(facts);
        if (gained <= 0) return false;
        progress = Math.min(target, progress + gained);
        return isComplete();
    }

    private int gain(BattleFacts f) {
        switch (type) {
            case WIN_BATTLES: return f.won ? 1 : 0;
            case PLAY_BATTLES: return 1;
            case LAND_CRITS: return f.heroCrits;
            case DEFEAT_ORIGIN: return f.won && f.enemyOriginId == param ? 1 : 0;
            case DEFEAT_TEAM_MEMBER: return f.won && f.enemyTeamIds.contains(param) ? 1 : 0;
            case WIN_WITH_TEAM: return f.won && f.heroTeamIds.contains(param) ? 1 : 0;
            case WIN_WITH_POWER: return f.won && f.heroPowerIds.contains(param) ? 1 : 0;
            case WIN_WITH_ARTIFACT: return f.won && f.equippedArtifacts > 0 ? 1 : 0;
            case WIN_VS_STRONGER: return f.won && f.enemyOverall > f.heroOverall ? 1 : 0;
            case WIN_WITH_OVERALL: return f.won && f.heroOverall >= param ? 1 : 0;
            default: return 0;
        }
    }

    public boolean isPossibleWith(PlayerState state) {
        switch (type) {
            case WIN_WITH_TEAM:
            case WIN_WITH_POWER:
                return !heroesThatCount(state).isEmpty();
            case WIN_WITH_ARTIFACT:
                return !state.getArtifactIds().isEmpty();
            case WIN_WITH_OVERALL:
                for (OwnedHero hero : state.getHeroes()) {
                    if (hero.getBattleAttributes().getOverall() >= param) return true;
                }
                return false;
            default:
                return true;
        }
    }

    public List<OwnedHero> heroesThatCount(PlayerState state) {
        List<OwnedHero> heroes = new ArrayList<>();
        if (type != Type.WIN_WITH_TEAM && type != Type.WIN_WITH_POWER) return heroes;
        for (OwnedHero hero : state.getHeroes()) {
            for (NamedRef ref : type == Type.WIN_WITH_TEAM ? hero.getTeams() : hero.getPowers()) {
                if (ref != null && ref.getId() == param) {
                    heroes.add(hero);
                    break;
                }
            }
        }
        return heroes;
    }

    public boolean countsEnemy(Character enemy, int enemyOverall, int heroOverall) {
        if (enemy == null || isComplete()) return false;
        switch (type) {
            case DEFEAT_ORIGIN:
                return enemy.getOrigin() != null && enemy.getOrigin().getId() == param;
            case DEFEAT_TEAM_MEMBER:
                for (NamedRef team : enemy.getTeams()) {
                    if (team != null && team.getId() == param) return true;
                }
                return false;
            case WIN_VS_STRONGER:
                return enemyOverall > heroOverall;
            default:
                return false;
        }
    }
}
