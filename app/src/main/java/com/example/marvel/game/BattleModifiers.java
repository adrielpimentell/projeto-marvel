package com.example.marvel.game;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class BattleModifiers {

    public static final BattleModifiers NONE =
            new BattleModifiers(0, 0, 0, EnumSet.noneOf(Artifact.Effect.class), 100);

    final int heroStartHp;
    final int heroHpBonusPercent;
    final int heroDamageBonusPercent;
    final Set<Artifact.Effect> heroExtraEffects;
    final int enemyPowerPercent;

    public BattleModifiers(int heroStartHp, int heroHpBonusPercent, int heroDamageBonusPercent,
                           Set<Artifact.Effect> heroExtraEffects, int enemyPowerPercent) {
        this.heroStartHp = heroStartHp;
        this.heroHpBonusPercent = Math.max(0, heroHpBonusPercent);
        this.heroDamageBonusPercent = Math.max(0, heroDamageBonusPercent);
        Set<Artifact.Effect> effects = EnumSet.noneOf(Artifact.Effect.class);
        if (heroExtraEffects != null) effects.addAll(heroExtraEffects);
        this.heroExtraEffects = Collections.unmodifiableSet(effects);
        this.enemyPowerPercent = Math.max(1, enemyPowerPercent);
    }
}
