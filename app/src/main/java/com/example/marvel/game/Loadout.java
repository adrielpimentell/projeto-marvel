package com.example.marvel.game;

import java.util.ArrayList;
import java.util.List;

public final class Loadout {

    private Loadout() {
    }

    public static List<Artifact> artifactsOf(OwnedHero hero) {
        List<Artifact> result = new ArrayList<>();
        for (String id : hero.getEquippedArtifactIds()) {
            Artifact artifact = ArtifactCatalog.byId(id);
            if (artifact != null) result.add(artifact);
        }
        return result;
    }

    public static GameAttributes withBonuses(GameAttributes base, List<Artifact> artifacts) {
        int life = 0;
        int strength = 0;
        int speed = 0;
        int intelligence = 0;
        for (Artifact artifact : artifacts) {
            life += artifact.getLifePercent();
            strength += artifact.getStrengthPercent();
            speed += artifact.getSpeedPercent();
            intelligence += artifact.getIntelligencePercent();
        }
        return GameAttributes.of(
                boost(base.getLife(), life),
                boost(base.getStrength(), strength),
                boost(base.getSpeed(), speed),
                boost(base.getIntelligence(), intelligence));
    }

    static int boost(int value, int percent) {
        if (percent <= 0) return value;
        int bonus = Math.max(1, Math.round(value * percent / 100f));
        return Math.min(GameBalance.MAX_ATTRIBUTE, value + bonus);
    }
}
