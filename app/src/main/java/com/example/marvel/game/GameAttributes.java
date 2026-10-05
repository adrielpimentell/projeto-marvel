package com.example.marvel.game;

import java.io.Serializable;

public final class GameAttributes implements Serializable {

    private final int life;
    private final int strength;
    private final int speed;
    private final int intelligence;
    private final int overall;

    public static GameAttributes of(int life, int strength, int speed, int intelligence) {
        int overall = Math.round((life + strength + speed + intelligence) / 4f);
        return new GameAttributes(life, strength, speed, intelligence, overall);
    }

    GameAttributes(int life, int strength, int speed, int intelligence, int overall) {
        this.life = life;
        this.strength = strength;
        this.speed = speed;
        this.intelligence = intelligence;
        this.overall = overall;
    }

    public int getLife() {
        return life;
    }

    public int getStrength() {
        return strength;
    }

    public int getSpeed() {
        return speed;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getOverall() {
        return overall;
    }
}
