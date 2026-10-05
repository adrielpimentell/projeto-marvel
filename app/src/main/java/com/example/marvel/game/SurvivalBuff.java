package com.example.marvel.game;

public enum SurvivalBuff {
    FURY(null),
    ARMOR(null),
    FOCUS(Artifact.Effect.CRIT_BOOST),
    LIFESTEAL(Artifact.Effect.LIFESTEAL),
    THORNS(Artifact.Effect.THORNS),
    REGENERATION(Artifact.Effect.REGENERATION),
    REFLEXES(Artifact.Effect.DODGE),
    MEDKIT(null);

    private final Artifact.Effect effect;

    SurvivalBuff(Artifact.Effect effect) {
        this.effect = effect;
    }

    public Artifact.Effect getEffect() {
        return effect;
    }
}
