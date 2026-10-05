package com.example.marvel.ui.ranks;

import com.example.marvel.game.Ranks;

final class TrailMath {

    private final float sidePadding;
    private final float columnWidth;

    TrailMath(float sidePadding, float columnWidth) {
        this.sidePadding = sidePadding;
        this.columnWidth = columnWidth;
    }

    float contentWidth() {
        return sidePadding * 2 + columnWidth * Ranks.count();
    }

    float centerOf(int rank) {
        return sidePadding + columnWidth * rank + columnWidth / 2f;
    }

    float trackStart() {
        return sidePadding;
    }

    float trackEnd() {
        return contentWidth() - sidePadding;
    }

    float markerX(int trophies) {
        int rank = Ranks.indexFor(trophies);
        int min = Ranks.minTrophies(rank);
        if (Ranks.isLast(rank)) {
            int previousSpan = min - Ranks.minTrophies(rank - 1);
            float extra = Math.min(1f, (trophies - min) / (float) Math.max(1, previousSpan));
            return centerOf(rank) + extra * columnWidth / 2f;
        }
        int span = Ranks.minTrophies(rank + 1) - min;
        float fraction = Math.max(0f, (trophies - min) / (float) span);
        return centerOf(rank) + fraction * columnWidth;
    }
}
