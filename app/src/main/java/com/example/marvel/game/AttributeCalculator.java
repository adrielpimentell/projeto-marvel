package com.example.marvel.game;

import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.NamedRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AttributeCalculator {

    public enum PowerGroup { RESISTANCE, STRENGTH, MOBILITY, MIND }

    private static final int BASE = 20;
    private static final double LIFE_PER_LOG_ISSUE = 8;
    private static final int LIFE_PER_RESISTANCE_POWER = 5;
    private static final int LIFE_MAX_MOVIE_BONUS = 10;
    private static final int STRENGTH_PER_POWER = 9;
    private static final int SPEED_PER_POWER = 10;
    private static final int INTELLIGENCE_PER_POWER = 9;
    private static final int INTELLIGENCE_MAX_TEAM_BONUS = 20;

    private static final Map<String, PowerGroup> GROUP_BY_POWER = new HashMap<>();

    static {
        register(PowerGroup.RESISTANCE,
                "Adaptive", "Chemical Absorbtion", "Controlled Bone Growth", "Density Control",
                "Divine Powers", "Duplication", "Elasticity", "Energy Absorption", "Energy Shield",
                "Force Field", "Healing", "Immortal", "Implants", "Inertia Absorption",
                "Invulnerability", "Longevity", "Matter Absorption", "Shape Shifter",
                "Siphon Lifeforce", "Soul Absorption", "Stamina", "Sub-Mariner", "Super Eating",
                "Vampirism");
        register(PowerGroup.STRENGTH,
                "Animation", "Berserker Strength", "Blast Power", "Blood Control",
                "Chemical Secretion", "Claws", "Darkforce Manipulation", "Darkness Manipulation",
                "Death Touch", "Earth Manipulation", "Electricity Control", "Energy Based Constructs",
                "Energy Manipulation", "Energy-Enhanced Strike", "Feral", "Fire Control",
                "Flame Breath", "Gravity control", "Heat Generation", "Heat Vision",
                "Hellfire Control", "Ice Breath", "Ice Control", "Light Projection", "Magnetism",
                "Marksmanship", "Plant Control", "Poisonous", "Power Item", "Power Suit",
                "Radiation", "Sand manipulation", "Size Manipulation", "Sonic Scream",
                "Super Strength", "Swordsmanship", "Unarmed Combat", "Vibration Wave",
                "Water Control", "Weapon Master", "Weather Control", "Wind Bursts");
        register(PowerGroup.MOBILITY,
                "Agility", "Chameleon", "Danger Sense", "Escape Artist", "Flight", "Invisibility",
                "Levitation", "Phasing / Ghost", "Prehensile Hair", "Radar Sense", "Sense Death",
                "Shadowmeld", "Stealth", "Super Hearing", "Super Sight", "Super Smell",
                "Super Speed", "Synaesthesia", "Teleport", "Time Travel", "Tracking",
                "Wall Clinger", "Webslinger");
        register(PowerGroup.MIND,
                "Animal Control", "Astral Projection", "Cosmic Awareness",
                "Dimensional Manipulation", "Electronic Disruption", "Electronic interaction",
                "Emotion Control", "Empathy", "Enhance Mutation", "Gadgets", "Genetic Manipulation",
                "Holographic Projection", "Hypnosis", "Illusion Casting", "Insanely Rich",
                "Intellect", "Leadership", "Magic", "Mesmerize", "Necromancy", "Omni-lingual",
                "Penance Stare", "Pheromone Control", "Possession", "Postcognition",
                "Power Mimicry", "Precognition", "Probability Manipulation", "Psionic", "Psychic",
                "Psychometry", "Reality Manpulation", "Siphon Abilities", "Technopathy",
                "Telekinesis", "Telepathy", "Time Manipulation", "Voice-induced Manipulation",
                "Willpower-Based Constructs");
    }

    private AttributeCalculator() {
    }

    public static GameAttributes calculate(Character character) {
        Map<PowerGroup, Integer> powers = countPowersByGroup(character);
        int issues = character.getIssueAppearances();
        int movies = character.getMovies().size();
        int teams = character.getTeams().size();

        int life = clamp(BASE
                + LIFE_PER_LOG_ISSUE * Math.log10(issues + 1)
                + LIFE_PER_RESISTANCE_POWER * powers.get(PowerGroup.RESISTANCE)
                + Math.min(movies, LIFE_MAX_MOVIE_BONUS));
        int strength = clamp(BASE + STRENGTH_PER_POWER * powers.get(PowerGroup.STRENGTH));
        int speed = clamp(BASE + SPEED_PER_POWER * powers.get(PowerGroup.MOBILITY));
        int intelligence = clamp(BASE
                + INTELLIGENCE_PER_POWER * powers.get(PowerGroup.MIND)
                + Math.min(teams, INTELLIGENCE_MAX_TEAM_BONUS));
        return GameAttributes.of(life, strength, speed, intelligence);
    }

    public static PowerGroup groupOf(String powerName) {
        return powerName == null ? null : GROUP_BY_POWER.get(normalize(powerName));
    }

    private static Map<PowerGroup, Integer> countPowersByGroup(Character character) {
        Map<PowerGroup, Integer> counts = new HashMap<>();
        for (PowerGroup group : PowerGroup.values()) {
            counts.put(group, 0);
        }
        for (NamedRef power : character.getPowers()) {
            PowerGroup group = groupOf(power.getName());
            if (group != null) {
                counts.put(group, counts.get(group) + 1);
            }
        }
        return Collections.unmodifiableMap(counts);
    }

    private static void register(PowerGroup group, String... powerNames) {
        for (String name : powerNames) {
            GROUP_BY_POWER.put(normalize(name), group);
        }
    }

    private static String normalize(String powerName) {
        return powerName.trim().toLowerCase(Locale.ROOT);
    }

    private static int clamp(double value) {
        return (int) Math.max(0, Math.min(GameBalance.MAX_ATTRIBUTE, Math.round(value)));
    }
}
