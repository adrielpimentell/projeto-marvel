package com.example.marvel.game;

public final class Ranks {

    private Ranks() {
    }

    public static int count() {
        return GameBalance.RANK_MIN_TROPHIES.length;
    }

    public static int indexFor(int trophies) {
        int rank = 0;
        for (int i = 0; i < GameBalance.RANK_MIN_TROPHIES.length; i++) {
            if (trophies >= GameBalance.RANK_MIN_TROPHIES[i]) rank = i;
        }
        return rank;
    }

    public static int minTrophies(int rank) {
        return GameBalance.RANK_MIN_TROPHIES[clamp(rank)];
    }

    public static boolean isLast(int rank) {
        return rank >= count() - 1;
    }

    public static int trophiesToNext(int trophies) {
        int rank = indexFor(trophies);
        return isLast(rank) ? 0 : Math.max(0, minTrophies(rank + 1) - trophies);
    }

    static int clamp(int rank) {
        return Math.max(0, Math.min(count() - 1, rank));
    }
}
