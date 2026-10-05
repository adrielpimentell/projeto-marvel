package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.marvel.data.model.Character;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AttributeCalculatorTest {

    private static final List<String> ALL_API_POWERS = Arrays.asList(
            "Adaptive", "Agility", "Animal Control", "Animation", "Astral Projection",
            "Berserker Strength", "Blast Power", "Blood Control", "Chameleon",
            "Chemical Absorbtion", "Chemical Secretion", "Claws", "Controlled Bone Growth",
            "Cosmic Awareness", "Danger Sense", "Darkforce Manipulation", "Darkness Manipulation",
            "Death Touch", "Density Control", "Dimensional Manipulation", "Divine Powers",
            "Duplication", "Earth Manipulation", "Elasticity", "Electricity Control",
            "Electronic Disruption", "Electronic interaction", "Emotion Control", "Empathy",
            "Energy Absorption", "Energy Based Constructs", "Energy Manipulation", "Energy Shield",
            "Energy-Enhanced Strike", "Enhance Mutation", "Escape Artist", "Feral", "Fire Control",
            "Flame Breath", "Flight", "Force Field", "Gadgets", "Genetic Manipulation",
            "Gravity control", "Healing", "Heat Generation", "Heat Vision", "Hellfire Control",
            "Holographic Projection", "Hypnosis", "Ice Breath", "Ice Control", "Illusion Casting",
            "Immortal", "Implants", "Inertia Absorption", "Insanely Rich", "Intellect",
            "Invisibility", "Invulnerability", "Leadership", "Levitation", "Light Projection",
            "Longevity", "Magic", "Magnetism", "Marksmanship", "Matter Absorption", "Mesmerize",
            "Necromancy", "Omni-lingual", "Penance Stare", "Phasing / Ghost", "Pheromone Control",
            "Plant Control", "Poisonous", "Possession", "Postcognition", "Power Item",
            "Power Mimicry", "Power Suit", "Precognition", "Prehensile Hair",
            "Probability Manipulation", "Psionic", "Psychic", "Psychometry", "Radar Sense",
            "Radiation", "Reality Manpulation", "Sand manipulation", "Sense Death", "Shadowmeld",
            "Shape Shifter", "Siphon Abilities", "Siphon Lifeforce", "Size Manipulation",
            "Sonic Scream", "Soul Absorption", "Stamina", "Stealth", "Sub-Mariner", "Super Eating",
            "Super Hearing", "Super Sight", "Super Smell", "Super Speed", "Super Strength",
            "Swordsmanship", "Synaesthesia", "Technopathy", "Telekinesis", "Telepathy", "Teleport",
            "Time Manipulation", "Time Travel", "Tracking", "Unarmed Combat", "Vampirism",
            "Vibration Wave", "Voice-induced Manipulation", "Wall Clinger", "Water Control",
            "Weapon Master", "Weather Control", "Webslinger", "Willpower-Based Constructs",
            "Wind Bursts");

    private final Gson gson = new Gson();

    @Test
    public void everyApiPowerBelongsToAGroup() {
        assertEquals(128, ALL_API_POWERS.size());
        for (String power : ALL_API_POWERS) {
            assertNotNull("Poder sem grupo: " + power, AttributeCalculator.groupOf(power));
        }
        assertNull(AttributeCalculator.groupOf("Poder Inventado"));
        assertNull(AttributeCalculator.groupOf(null));
    }

    @Test
    public void sameCharacterAlwaysGetsSameAttributes() {
        Character character = character(16944, 17, 58,
                "Super Strength", "Healing", "Agility", "Leadership");
        GameAttributes first = AttributeCalculator.calculate(character);
        GameAttributes second = AttributeCalculator.calculate(character);

        assertEquals(first.getLife(), second.getLife());
        assertEquals(first.getStrength(), second.getStrength());
        assertEquals(first.getSpeed(), second.getSpeed());
        assertEquals(first.getIntelligence(), second.getIntelligence());
        assertEquals(first.getOverall(), second.getOverall());
    }

    @Test
    public void formulaMatchesDocumentedValues() {
        Character character = character(999, 4, 7,
                "Healing", "Super Strength", "Claws", "Flight", "Intellect");
        GameAttributes a = AttributeCalculator.calculate(character);

        assertEquals(53, a.getLife());
        assertEquals(38, a.getStrength());
        assertEquals(30, a.getSpeed());
        assertEquals(36, a.getIntelligence());
        assertEquals(39, a.getOverall());
    }

    @Test
    public void missingDataGivesBaseValues() {
        Character empty = gson.fromJson("{\"id\":1,\"powers\":null,\"teams\":null,\"movies\":null}",
                Character.class);
        GameAttributes a = AttributeCalculator.calculate(empty);

        assertEquals(20, a.getLife());
        assertEquals(20, a.getStrength());
        assertEquals(20, a.getSpeed());
        assertEquals(20, a.getIntelligence());
        assertEquals(20, a.getOverall());
    }

    @Test
    public void valuesNeverLeaveZeroToHundred() {
        String[] manyPowers = ALL_API_POWERS.toArray(new String[0]);
        GameAttributes a = AttributeCalculator.calculate(character(1_000_000, 500, 500, manyPowers));

        for (int value : new int[]{a.getLife(), a.getStrength(), a.getSpeed(),
                a.getIntelligence(), a.getOverall()}) {
            assertTrue(value >= 0 && value <= 100);
        }
        assertEquals(100, a.getOverall());
    }

    private Character character(int issues, int movies, int teams, String... powers) {
        StringBuilder json = new StringBuilder("{\"id\":99,\"count_of_issue_appearances\":")
                .append(issues).append(",\"powers\":[");
        for (int i = 0; i < powers.length; i++) {
            if (i > 0) json.append(',');
            json.append("{\"id\":").append(i).append(",\"name\":\"").append(powers[i]).append("\"}");
        }
        json.append("],\"movies\":").append(refs(movies)).append(",\"teams\":").append(refs(teams));
        return gson.fromJson(json.append('}').toString(), Character.class);
    }

    private static String refs(int count) {
        StringBuilder list = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) list.append(',');
            list.append("{\"id\":").append(i).append(",\"name\":\"Item ").append(i).append("\"}");
        }
        return list.append(']').toString();
    }
}
