package com.example.marvel.ui.ranks;

import com.example.marvel.game.Artifact;
import com.example.marvel.game.GameBalance;

final class ChestAnimSpec {

    private ChestAnimSpec() {
    }

    static long suspenseMs(Artifact.Rarity rarity) {
        switch (rarity) {
            case EPIC: return GameBalance.CHEST_ANIM_SUSPENSE_EPIC_MS;
            case LEGENDARY: return GameBalance.CHEST_ANIM_SUSPENSE_LEGENDARY_MS;
            default: return GameBalance.CHEST_ANIM_SUSPENSE_MS;
        }
    }

    static int particles(Artifact.Rarity rarity) {
        switch (rarity) {
            case EPIC: return GameBalance.CHEST_PARTICLES_EPIC;
            case LEGENDARY: return GameBalance.CHEST_PARTICLES_LEGENDARY;
            default: return GameBalance.CHEST_PARTICLES;
        }
    }

    static boolean isLegendary(Artifact.Rarity rarity) {
        return rarity == Artifact.Rarity.LEGENDARY;
    }

    static long totalMs(Artifact.Rarity rarity) {
        return GameBalance.CHEST_ANIM_ENTRY_MS + suspenseMs(rarity) + GameBalance.CHEST_ANIM_OPEN_MS
                + GameBalance.CHEST_ANIM_REVEAL_MS + GameBalance.CHEST_ANIM_DETAILS_MS;
    }
}
