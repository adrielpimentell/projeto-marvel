package com.example.marvel.game;

import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.NamedRef;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class BattleFacts {

    final boolean won;
    final int heroCrits;
    final int heroOverall;
    final int enemyOverall;
    final int equippedArtifacts;
    final Set<Integer> heroTeamIds;
    final Set<Integer> heroPowerIds;
    final Set<Integer> enemyTeamIds;
    final int enemyOriginId;

    BattleFacts(boolean won, int heroCrits, int heroOverall, int enemyOverall, int equippedArtifacts,
                Set<Integer> heroTeamIds, Set<Integer> heroPowerIds,
                Set<Integer> enemyTeamIds, int enemyOriginId) {
        this.won = won;
        this.heroCrits = heroCrits;
        this.heroOverall = heroOverall;
        this.enemyOverall = enemyOverall;
        this.equippedArtifacts = equippedArtifacts;
        this.heroTeamIds = heroTeamIds;
        this.heroPowerIds = heroPowerIds;
        this.enemyTeamIds = enemyTeamIds;
        this.enemyOriginId = enemyOriginId;
    }

    public static BattleFacts of(BattleResult result, OwnedHero hero, Character enemy) {
        int crits = 0;
        for (BattleTurn turn : result.getTurns()) {
            if (turn.isHeroAttacking() && turn.isCritical()) crits++;
        }
        NamedRef origin = enemy == null ? null : enemy.getOrigin();
        return new BattleFacts(result.isHeroWinner(), crits,
                result.getHero().getOverall(), result.getEnemy().getOverall(),
                hero.getEquippedArtifactIds().size(),
                ids(hero.getTeams()), ids(hero.getPowers()),
                enemy == null ? new HashSet<>() : ids(enemy.getTeams()),
                origin == null ? 0 : origin.getId());
    }

    private static Set<Integer> ids(Collection<NamedRef> refs) {
        Set<Integer> ids = new HashSet<>();
        for (NamedRef ref : refs) {
            if (ref != null) ids.add(ref.getId());
        }
        return ids;
    }
}
