package com.example.marvel.game;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public final class BattleResult implements Serializable {

    private final boolean heroWon;
    private final List<BattleTurn> turns;
    private final int heroMaxHp;
    private final int enemyMaxHp;
    private final GameAttributes hero;
    private final GameAttributes enemy;
    private final int trophies;
    private final int coins;
    private boolean heroShieldAtStart;
    private int heroStartHp;

    BattleResult(boolean heroWon, List<BattleTurn> turns, int heroMaxHp, int enemyMaxHp,
                 GameAttributes hero, GameAttributes enemy, int trophies, int coins) {
        this.heroWon = heroWon;
        this.turns = Collections.unmodifiableList(turns);
        this.heroMaxHp = heroMaxHp;
        this.enemyMaxHp = enemyMaxHp;
        this.hero = hero;
        this.enemy = enemy;
        this.trophies = trophies;
        this.coins = coins;
    }

    void setHeroShieldAtStart(boolean ready) {
        this.heroShieldAtStart = ready;
    }

    public boolean isHeroShieldAtStart() {
        return heroShieldAtStart;
    }

    void setHeroStartHp(int hp) {
        this.heroStartHp = hp;
    }

    public int getHeroStartHp() {
        return heroStartHp > 0 ? Math.min(heroStartHp, heroMaxHp) : heroMaxHp;
    }

    public int getHeroHpEnd() {
        return turns.isEmpty() ? getHeroStartHp() : turns.get(turns.size() - 1).getHeroHpEnd();
    }

    public boolean isHeroWinner() {
        return heroWon;
    }

    public List<BattleTurn> getTurns() {
        return turns;
    }

    public int getHeroMaxHp() {
        return heroMaxHp;
    }

    public int getEnemyMaxHp() {
        return enemyMaxHp;
    }

    public GameAttributes getHero() {
        return hero;
    }

    public GameAttributes getEnemy() {
        return enemy;
    }

    public int getTrophies() {
        return trophies;
    }

    public int getCoins() {
        return coins;
    }
}
