package com.example.marvel.game;

import java.io.Serializable;

public final class BattleTurn implements Serializable {

    boolean heroAttacks;

    int regen;
    int poisonTick;
    int actorHpAfterStart;
    boolean skipped;

    boolean dodged;
    boolean blocked;
    int damage;
    boolean critical;
    boolean execute;
    int targetHpAfterHit;

    int lifesteal;
    int thorns;
    boolean poisonApplied;
    boolean stunApplied;

    int heroHpEnd;
    int enemyHpEnd;
    boolean heroPoisoned;
    boolean enemyPoisoned;
    boolean heroStunned;
    boolean enemyStunned;
    boolean heroShieldReady;
    boolean enemyShieldReady;

    BattleTurn(boolean heroAttacks) {
        this.heroAttacks = heroAttacks;
    }

    public boolean isHeroAttacking() {
        return heroAttacks;
    }

    public int getRegen() {
        return regen;
    }

    public int getPoisonTick() {
        return poisonTick;
    }

    public int getActorHpAfterStart() {
        return actorHpAfterStart;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean isDodged() {
        return dodged;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isCritical() {
        return critical;
    }

    public boolean isExecute() {
        return execute;
    }

    public int getTargetHpAfter() {
        return targetHpAfterHit;
    }

    public int getLifesteal() {
        return lifesteal;
    }

    public int getThorns() {
        return thorns;
    }

    public boolean isPoisonApplied() {
        return poisonApplied;
    }

    public boolean isStunApplied() {
        return stunApplied;
    }

    public int getHeroHpEnd() {
        return heroHpEnd;
    }

    public int getEnemyHpEnd() {
        return enemyHpEnd;
    }

    public boolean isHeroPoisoned() {
        return heroPoisoned;
    }

    public boolean isEnemyPoisoned() {
        return enemyPoisoned;
    }

    public boolean isHeroStunned() {
        return heroStunned;
    }

    public boolean isEnemyStunned() {
        return enemyStunned;
    }

    public boolean isHeroShieldReady() {
        return heroShieldReady;
    }

    public boolean isEnemyShieldReady() {
        return enemyShieldReady;
    }
}
