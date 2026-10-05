package com.example.marvel.game;

import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.NamedRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OwnedHero implements Serializable {

    public enum Stat { LIFE, STRENGTH, SPEED, INTELLIGENCE }

    private int characterId;
    private String name;
    private String realName;
    private String imageUrl;

    private int baseLife;
    private int baseStrength;
    private int baseSpeed;
    private int baseIntelligence;

    private int lifeUpgrades;
    private int strengthUpgrades;
    private int speedUpgrades;
    private int intelligenceUpgrades;

    private List<String> equippedArtifacts = new ArrayList<>();

    private List<NamedRef> teams;
    private List<NamedRef> powers;
    private NamedRef origin;

    private OwnedHero() {
    }

    OwnedHero(int characterId, GameAttributes base) {
        this.characterId = characterId;
        this.baseLife = base.getLife();
        this.baseStrength = base.getStrength();
        this.baseSpeed = base.getSpeed();
        this.baseIntelligence = base.getIntelligence();
    }

    public static OwnedHero starter() {
        return new OwnedHero(GameBalance.STARTER_HERO_ID, GameAttributes.of(
                GameBalance.STARTER_LIFE, GameBalance.STARTER_STRENGTH,
                GameBalance.STARTER_SPEED, GameBalance.STARTER_INTELLIGENCE));
    }

    public static OwnedHero fromCharacter(Character character) {
        OwnedHero hero = new OwnedHero(character.getId(), AttributeCalculator.calculate(character));
        hero.updateInfo(character.getName(), character.getRealName(), character.getCardImageUrl());
        hero.updateTags(character);
        return hero;
    }

    public boolean hasTags() {
        return teams != null && powers != null;
    }

    public List<NamedRef> getTeams() {
        return teams == null ? Collections.emptyList() : Collections.unmodifiableList(teams);
    }

    public List<NamedRef> getPowers() {
        return powers == null ? Collections.emptyList() : Collections.unmodifiableList(powers);
    }

    public NamedRef getOrigin() {
        return origin;
    }

    void updateTags(Character character) {
        if (character == null || !character.hasDetails()) return;
        teams = new ArrayList<>(character.getTeams());
        powers = new ArrayList<>(character.getPowers());
        origin = character.getOrigin();
    }

    public GameAttributes getAttributes() {
        return GameAttributes.of(
                current(baseLife, lifeUpgrades),
                current(baseStrength, strengthUpgrades),
                current(baseSpeed, speedUpgrades),
                current(baseIntelligence, intelligenceUpgrades));
    }

    public GameAttributes getBattleAttributes() {
        return Loadout.withBonuses(getAttributes(), Loadout.artifactsOf(this));
    }

    public List<String> getEquippedArtifactIds() {
        return equippedArtifacts == null
                ? Collections.emptyList() : Collections.unmodifiableList(equippedArtifacts);
    }

    public boolean hasEquipped(String artifactId) {
        return getEquippedArtifactIds().contains(artifactId);
    }

    void equip(String artifactId) {
        if (equippedArtifacts == null) equippedArtifacts = new ArrayList<>();
        if (!equippedArtifacts.contains(artifactId)) equippedArtifacts.add(artifactId);
    }

    boolean unequip(String artifactId) {
        return equippedArtifacts != null && equippedArtifacts.remove(artifactId);
    }

    List<String> mutableEquipped() {
        if (equippedArtifacts == null) equippedArtifacts = new ArrayList<>();
        return equippedArtifacts;
    }

    public int getUpgrades(Stat stat) {
        switch (stat) {
            case LIFE: return lifeUpgrades;
            case STRENGTH: return strengthUpgrades;
            case SPEED: return speedUpgrades;
            default: return intelligenceUpgrades;
        }
    }

    boolean addUpgrade(Stat stat) {
        if (isMaxed(stat)) return false;
        switch (stat) {
            case LIFE: lifeUpgrades++; break;
            case STRENGTH: strengthUpgrades++; break;
            case SPEED: speedUpgrades++; break;
            default: intelligenceUpgrades++; break;
        }
        return true;
    }

    public boolean isMaxed(Stat stat) {
        GameAttributes a = getAttributes();
        int value;
        switch (stat) {
            case LIFE: value = a.getLife(); break;
            case STRENGTH: value = a.getStrength(); break;
            case SPEED: value = a.getSpeed(); break;
            default: value = a.getIntelligence(); break;
        }
        return value >= GameBalance.MAX_ATTRIBUTE;
    }

    void updateInfo(String name, String realName, String imageUrl) {
        this.name = name;
        this.realName = realName;
        this.imageUrl = imageUrl;
    }

    public boolean hasInfo() {
        return name != null && !name.trim().isEmpty();
    }

    public int getCharacterId() {
        return characterId;
    }

    public String getName() {
        return name;
    }

    public String getRealName() {
        return realName == null ? "" : realName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    private static int current(int base, int upgrades) {
        return Math.min(GameBalance.MAX_ATTRIBUTE, base + upgrades * GameBalance.UPGRADE_STEP);
    }
}
