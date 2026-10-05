package com.example.marvel.ui.common;

import android.content.Context;

import com.example.marvel.R;
import com.example.marvel.game.Artifact;
import com.example.marvel.game.Roulette;

public final class ArtifactUi {

    private ArtifactUi() {
    }

    public static String rarityName(Context context, Artifact.Rarity rarity) {
        switch (rarity) {
            case RARE: return context.getString(R.string.rarity_rare);
            case EPIC: return context.getString(R.string.rarity_epic);
            case LEGENDARY: return context.getString(R.string.rarity_legendary);
            default: return context.getString(R.string.rarity_common);
        }
    }

    public static int rarityColor(Context context, Artifact.Rarity rarity) {
        switch (rarity) {
            case RARE: return context.getColor(R.color.rarity_rare);
            case EPIC: return context.getColor(R.color.rarity_epic);
            case LEGENDARY: return context.getColor(R.color.rarity_legendary);
            default: return context.getColor(R.color.rarity_common);
        }
    }

    public static String rouletteName(Context context, Roulette roulette) {
        switch (roulette) {
            case RARE: return context.getString(R.string.roulette_rare);
            case EPIC: return context.getString(R.string.roulette_epic);
            case LEGENDARY: return context.getString(R.string.roulette_legendary);
            default: return context.getString(R.string.roulette_common);
        }
    }

    public static String rouletteHint(Context context, Roulette roulette) {
        switch (roulette) {
            case RARE: return context.getString(R.string.roulette_hint_rare);
            case EPIC: return context.getString(R.string.roulette_hint_epic);
            case LEGENDARY: return context.getString(R.string.roulette_hint_legendary);
            default: return context.getString(R.string.roulette_hint_common);
        }
    }

    public static int rouletteColor(Context context, Roulette roulette) {
        return rarityColor(context, Artifact.Rarity.values()[roulette.ordinal()]);
    }

    public static String typeName(Context context, Artifact.Type type) {
        return context.getString(type == Artifact.Type.ATTRIBUTE
                ? R.string.artifact_type_attribute : R.string.artifact_type_special);
    }
}
