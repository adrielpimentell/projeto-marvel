package com.example.marvel.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class Survival {

    private Survival() {
    }

    public static int powerPercent(int floor) {
        double power = GameBalance.SURVIVAL_START_POWER_PERCENT
                * Math.pow(GameBalance.SURVIVAL_POWER_GROWTH, Math.max(0, floor - 1));
        return (int) Math.round(Math.min(power, 1_000_000));
    }

    public static int coinsForFloor(int floor) {
        return GameBalance.SURVIVAL_COINS_PER_FLOOR * Math.max(0, floor);
    }

    public static boolean isChestFloor(int floor) {
        for (int chestFloor : GameBalance.SURVIVAL_CHEST_FLOORS) {
            if (chestFloor == floor) return true;
        }
        return false;
    }

    public static int maxHp(SurvivalRun run, OwnedHero hero) {
        return BattleEngine.maxHp(hero.getBattleAttributes(),
                run.count(SurvivalBuff.ARMOR) * GameBalance.SURVIVAL_ARMOR_HP_PERCENT);
    }

    public static int currentHp(SurvivalRun run, OwnedHero hero) {
        int max = maxHp(run, hero);
        return run.heroHp > 0 ? Math.min(max, run.heroHp) : max;
    }

    public static BattleModifiers modifiers(SurvivalRun run, OwnedHero hero) {
        Set<Artifact.Effect> effects = EnumSet.noneOf(Artifact.Effect.class);
        for (SurvivalBuff buff : run.buffs) {
            if (buff.getEffect() != null) effects.add(buff.getEffect());
        }
        return new BattleModifiers(currentHp(run, hero),
                run.count(SurvivalBuff.ARMOR) * GameBalance.SURVIVAL_ARMOR_HP_PERCENT,
                run.count(SurvivalBuff.FURY) * GameBalance.SURVIVAL_FURY_DAMAGE_PERCENT,
                effects, powerPercent(run.floor));
    }

    static List<SurvivalBuff> rollOffer(SurvivalRun run, OwnedHero hero, Random random) {
        Set<Artifact.Effect> active = BattleEngine.effectsOf(Loadout.artifactsOf(hero));
        for (SurvivalBuff buff : run.buffs) {
            if (buff.getEffect() != null) active.add(buff.getEffect());
        }
        List<SurvivalBuff> pool = new ArrayList<>();
        for (SurvivalBuff buff : SurvivalBuff.values()) {
            if (buff.getEffect() == null || !active.contains(buff.getEffect())) pool.add(buff);
        }
        Collections.shuffle(pool, random);
        return new ArrayList<>(pool.subList(0, Math.min(GameBalance.SURVIVAL_BUFF_CHOICES, pool.size())));
    }
}
