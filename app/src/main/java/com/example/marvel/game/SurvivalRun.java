package com.example.marvel.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SurvivalRun {

    int heroId;
    int floor = 1;
    int heroHp;
    List<SurvivalBuff> buffs = new ArrayList<>();
    List<SurvivalBuff> offer = new ArrayList<>();
    List<Integer> usedEnemyIds = new ArrayList<>();
    int enemyId;
    int coinsEarned;
    boolean over;

    SurvivalRun() {
    }

    SurvivalRun(int heroId) {
        this.heroId = heroId;
    }

    public int getHeroId() {
        return heroId;
    }

    public int getFloor() {
        return floor;
    }

    public int getFloorsWon() {
        return floor - 1;
    }

    public int getHeroHp() {
        return heroHp;
    }

    public List<SurvivalBuff> getBuffs() {
        return Collections.unmodifiableList(buffs);
    }

    public List<SurvivalBuff> getOffer() {
        return Collections.unmodifiableList(offer);
    }

    public boolean hasChoicePending() {
        return !offer.isEmpty();
    }

    public List<Integer> getUsedEnemyIds() {
        return Collections.unmodifiableList(usedEnemyIds);
    }

    public int getEnemyId() {
        return enemyId;
    }

    public int getCoinsEarned() {
        return coinsEarned;
    }

    public boolean isOver() {
        return over;
    }

    public int count(SurvivalBuff buff) {
        int count = 0;
        for (SurvivalBuff b : buffs) {
            if (b == buff) count++;
        }
        return count;
    }

    boolean repair() {
        if (buffs == null) buffs = new ArrayList<>();
        if (offer == null) offer = new ArrayList<>();
        if (usedEnemyIds == null) usedEnemyIds = new ArrayList<>();
        buffs.removeIf(b -> b == null || b == SurvivalBuff.MEDKIT);
        offer.removeIf(b -> b == null);
        usedEnemyIds.removeIf(id -> id == null);
        coinsEarned = Math.max(0, coinsEarned);
        return floor >= 1;
    }
}
