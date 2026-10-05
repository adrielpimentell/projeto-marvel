package com.example.marvel.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class BattleEngine {

    private BattleEngine() {
    }

    public static BattleResult fight(GameAttributes hero, GameAttributes enemy, Random random) {
        return fight(hero, Collections.emptyList(), enemy, random);
    }

    public static BattleResult fight(GameAttributes hero, List<Artifact> heroArtifacts,
                                     GameAttributes enemy, Random random) {
        return fight(hero, heroArtifacts, enemy, random, BattleModifiers.NONE);
    }

    public static BattleResult fight(GameAttributes hero, List<Artifact> heroArtifacts,
                                     GameAttributes enemy, Random random, BattleModifiers mods) {
        Set<Artifact.Effect> heroEffects = effectsOf(heroArtifacts);
        heroEffects.addAll(mods.heroExtraEffects);
        Fighter h = new Fighter(hero, heroEffects,
                percentOf(maxHp(hero), 100 + mods.heroHpBonusPercent), 100 + mods.heroDamageBonusPercent);
        if (mods.heroStartHp > 0) h.hp = Math.min(h.maxHp, mods.heroStartHp);
        Fighter e = new Fighter(enemy, EnumSet.noneOf(Artifact.Effect.class),
                percentOf(maxHp(enemy), mods.enemyPowerPercent), mods.enemyPowerPercent);
        int heroStartHp = h.hp;

        boolean heroTurn = hero.getSpeed() != enemy.getSpeed()
                ? hero.getSpeed() > enemy.getSpeed()
                : random.nextBoolean();

        List<BattleTurn> turns = new ArrayList<>();
        Boolean heroWon = null;

        while (heroWon == null) {
            Fighter actor = heroTurn ? h : e;
            Fighter target = heroTurn ? e : h;
            BattleTurn turn = new BattleTurn(heroTurn);
            playTurn(actor, target, turn, random);
            snapshot(turn, h, e);
            turns.add(turn);

            if (e.hp == 0 && h.hp == 0) {
                heroWon = heroTurn;
            } else if (e.hp == 0) {
                heroWon = true;
            } else if (h.hp == 0) {
                heroWon = false;
            } else if (turns.size() >= GameBalance.MAX_TURNS) {
                heroWon = (double) h.hp / h.maxHp >= (double) e.hp / e.maxHp;
            }
            heroTurn = !heroTurn;
        }

        int trophies = heroWon ? GameBalance.WIN_TROPHIES : GameBalance.LOSS_TROPHIES;
        int coins = heroWon
                ? Economy.coinsForVictory(hero.getOverall(), enemy.getOverall())
                : GameBalance.DEFEAT_COINS;
        BattleResult result = new BattleResult(heroWon, turns, h.maxHp, e.maxHp, hero, enemy, trophies, coins);
        result.setHeroShieldAtStart(h.has(Artifact.Effect.SHIELD));
        result.setHeroStartHp(heroStartHp);
        return result;
    }

    private static void playTurn(Fighter actor, Fighter target, BattleTurn turn, Random random) {
        if (actor.has(Artifact.Effect.REGENERATION) && actor.hp < actor.maxHp) {
            int heal = Math.max(1, Math.round(actor.maxHp * GameBalance.REGENERATION_PERCENT / 100f));
            turn.regen = actor.heal(heal);
        }
        if (actor.poisonTurns > 0) {
            actor.poisonTurns--;
            turn.poisonTick = actor.hurt(GameBalance.POISON_DAMAGE);
        }
        turn.actorHpAfterStart = actor.hp;
        if (actor.hp == 0) return;

        if (actor.stunned) {
            actor.stunned = false;
            actor.stunImmune = true;
            turn.skipped = true;
            turn.targetHpAfterHit = target.hp;
            return;
        }

        boolean critical = random.nextDouble() < critChance(actor.attributes, actor.effects);
        double damage = rollDamage(actor.attributes, critical, random);
        if (actor.damagePercent != 100) damage *= actor.damagePercent / 100.0;
        if (actor.has(Artifact.Effect.EXECUTE)
                && target.hp < target.maxHp * GameBalance.EXECUTE_THRESHOLD_PERCENT / 100f) {
            damage *= 1 + GameBalance.EXECUTE_BONUS_PERCENT / 100.0;
            turn.execute = true;
        }

        if (target.has(Artifact.Effect.DODGE) && random.nextInt(100) < GameBalance.DODGE_CHANCE_PERCENT) {
            turn.dodged = true;
        } else if (target.has(Artifact.Effect.SHIELD) && !target.shieldUsed) {
            target.shieldUsed = true;
            turn.blocked = true;
        } else {
            turn.damage = target.hurt((int) Math.max(1, Math.round(damage)));
            turn.critical = critical;
        }
        turn.targetHpAfterHit = target.hp;

        boolean canStun = !target.stunImmune;
        target.stunImmune = false;
        if (turn.damage == 0 || target.hp == 0) return;

        if (actor.has(Artifact.Effect.LIFESTEAL)) {
            turn.lifesteal = actor.heal(Math.max(1, Math.round(turn.damage * GameBalance.LIFESTEAL_PERCENT / 100f)));
        }
        if (actor.has(Artifact.Effect.POISON)) {
            target.poisonTurns = GameBalance.POISON_TURNS;
            turn.poisonApplied = true;
        }
        if (actor.has(Artifact.Effect.STUN) && canStun
                && random.nextInt(100) < GameBalance.STUN_CHANCE_PERCENT) {
            target.stunned = true;
            turn.stunApplied = true;
        }
        if (target.has(Artifact.Effect.THORNS)) {
            turn.thorns = actor.hurt(Math.max(1, Math.round(turn.damage * GameBalance.THORNS_PERCENT / 100f)));
        }
    }

    private static void snapshot(BattleTurn turn, Fighter h, Fighter e) {
        turn.heroHpEnd = h.hp;
        turn.enemyHpEnd = e.hp;
        turn.heroPoisoned = h.poisonTurns > 0;
        turn.enemyPoisoned = e.poisonTurns > 0;
        turn.heroStunned = h.stunned;
        turn.enemyStunned = e.stunned;
        turn.heroShieldReady = h.has(Artifact.Effect.SHIELD) && !h.shieldUsed;
        turn.enemyShieldReady = e.has(Artifact.Effect.SHIELD) && !e.shieldUsed;
    }

    static Set<Artifact.Effect> effectsOf(List<Artifact> artifacts) {
        Set<Artifact.Effect> effects = EnumSet.noneOf(Artifact.Effect.class);
        for (Artifact artifact : artifacts) {
            if (artifact.getEffect() != Artifact.Effect.NONE) effects.add(artifact.getEffect());
        }
        return effects;
    }

    static int maxHp(GameAttributes a) {
        return GameBalance.HP_BASE + a.getLife() * GameBalance.HP_PER_LIFE;
    }

    static int maxHp(GameAttributes a, int bonusPercent) {
        return percentOf(maxHp(a), 100 + Math.max(0, bonusPercent));
    }

    private static int percentOf(int value, int percent) {
        if (percent == 100) return value;
        return Math.max(1, Math.round(value * percent / 100f));
    }

    static double critChance(GameAttributes a) {
        return critChance(a, EnumSet.noneOf(Artifact.Effect.class));
    }

    static double critChance(GameAttributes a, Set<Artifact.Effect> effects) {
        double chance = GameBalance.CRIT_CHANCE_BASE
                + a.getIntelligence() * GameBalance.CRIT_CHANCE_PER_INTELLIGENCE;
        if (effects.contains(Artifact.Effect.CRIT_BOOST)) {
            chance += GameBalance.CRIT_BOOST_PERCENT / 100.0;
        }
        return Math.min(GameBalance.CRIT_CHANCE_MAX, chance);
    }

    private static double rollDamage(GameAttributes attacker, boolean critical, Random random) {
        double base = GameBalance.DAMAGE_BASE + attacker.getStrength() * GameBalance.DAMAGE_PER_STRENGTH;
        double factor = GameBalance.DAMAGE_RANDOM_MIN
                + random.nextDouble() * (GameBalance.DAMAGE_RANDOM_MAX - GameBalance.DAMAGE_RANDOM_MIN);
        return base * factor * (critical ? GameBalance.CRIT_MULTIPLIER : 1);
    }

    private static final class Fighter {
        final GameAttributes attributes;
        final Set<Artifact.Effect> effects;
        final int maxHp;
        final int damagePercent;
        int hp;
        int poisonTurns;
        boolean stunned;
        boolean stunImmune;
        boolean shieldUsed;

        Fighter(GameAttributes attributes, Set<Artifact.Effect> effects, int maxHp, int damagePercent) {
            this.attributes = attributes;
            this.effects = effects;
            this.maxHp = maxHp;
            this.hp = maxHp;
            this.damagePercent = damagePercent;
        }

        boolean has(Artifact.Effect effect) {
            return effects.contains(effect);
        }

        int hurt(int amount) {
            int before = hp;
            hp = Math.max(0, hp - amount);
            return before - hp;
        }

        int heal(int amount) {
            int before = hp;
            hp = Math.min(maxHp, hp + amount);
            return hp - before;
        }
    }
}
