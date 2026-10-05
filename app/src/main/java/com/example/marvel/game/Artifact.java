package com.example.marvel.game;

import java.io.Serializable;
import java.util.Locale;

public final class Artifact implements Serializable {

    public enum Rarity { COMMON, RARE, EPIC, LEGENDARY }

    public enum Type { ATTRIBUTE, SPECIAL }

    public enum Effect { NONE, DODGE, POISON, LIFESTEAL, THORNS, REGENERATION, SHIELD, EXECUTE, STUN, CRIT_BOOST }

    private final String id;
    private final String name;
    private final Rarity rarity;
    private final Type type;
    private final int lifePercent;
    private final int strengthPercent;
    private final int speedPercent;
    private final int intelligencePercent;
    private final Effect effect;

    Artifact(String id, String name, Rarity rarity, Type type,
             int lifePercent, int strengthPercent, int speedPercent, int intelligencePercent,
             Effect effect) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.type = type;
        this.lifePercent = lifePercent;
        this.strengthPercent = strengthPercent;
        this.speedPercent = speedPercent;
        this.intelligencePercent = intelligencePercent;
        this.effect = effect;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public Type getType() {
        return type;
    }

    public Effect getEffect() {
        return effect;
    }

    public int getLifePercent() {
        return lifePercent;
    }

    public int getStrengthPercent() {
        return strengthPercent;
    }

    public int getSpeedPercent() {
        return speedPercent;
    }

    public int getIntelligencePercent() {
        return intelligencePercent;
    }

    public String describe() {
        if (type == Type.ATTRIBUTE) return describeBonus();
        switch (effect) {
            case DODGE:
                return String.format(Locale.ROOT, "%d%% de chance de desviar de cada golpe",
                        GameBalance.DODGE_CHANCE_PERCENT);
            case POISON:
                return String.format(Locale.ROOT,
                        "Cada ataque envenena: o inimigo leva %d de dano nos %d turnos seguintes",
                        GameBalance.POISON_DAMAGE, GameBalance.POISON_TURNS);
            case LIFESTEAL:
                return String.format(Locale.ROOT, "Recupera %d%% do dano que causa",
                        GameBalance.LIFESTEAL_PERCENT);
            case THORNS:
                return String.format(Locale.ROOT, "Devolve %d%% do dano recebido ao atacante",
                        GameBalance.THORNS_PERCENT);
            case REGENERATION:
                return String.format(Locale.ROOT, "Recupera %d%% da vida máxima no início de cada turno seu",
                        GameBalance.REGENERATION_PERCENT);
            case SHIELD:
                return "Bloqueia o 1º golpe recebido";
            case EXECUTE:
                return String.format(Locale.ROOT, "+%d%% de dano quando o inimigo tem menos de %d%% de vida",
                        GameBalance.EXECUTE_BONUS_PERCENT, GameBalance.EXECUTE_THRESHOLD_PERCENT);
            case STUN:
                return String.format(Locale.ROOT, "%d%% de chance de atordoar: o inimigo perde o próximo turno",
                        GameBalance.STUN_CHANCE_PERCENT);
            case CRIT_BOOST:
                return String.format(Locale.ROOT, "+%d%% de chance de golpe crítico",
                        GameBalance.CRIT_BOOST_PERCENT);
            default:
                return "";
        }
    }

    private String describeBonus() {
        if (lifePercent > 0 && lifePercent == strengthPercent && lifePercent == speedPercent
                && lifePercent == intelligencePercent) {
            return String.format(Locale.ROOT, "+%d%% em todos os atributos", lifePercent);
        }
        StringBuilder text = new StringBuilder();
        appendBonus(text, lifePercent, "Vida");
        appendBonus(text, strengthPercent, "Força");
        appendBonus(text, speedPercent, "Velocidade");
        appendBonus(text, intelligencePercent, "Inteligência");
        return text.toString();
    }

    private static void appendBonus(StringBuilder text, int percent, String stat) {
        if (percent <= 0) return;
        if (text.length() > 0) text.append(", ");
        text.append('+').append(percent).append("% de ").append(stat);
    }
}
