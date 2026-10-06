package com.example.marvel.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TrophySync {

    public static final TrophySync NONE = new TrophySync(null, false, Collections.emptyList());

    private final String season;
    private final boolean create;
    private final List<Integer> values;

    private TrophySync(String season, boolean create, List<Integer> values) {
        this.season = season;
        this.create = create;
        this.values = Collections.unmodifiableList(values);
    }

    static TrophySync between(String season, Integer synced, int target) {
        List<Integer> values = new ArrayList<>();
        int current;
        boolean create = synced == null;
        if (create) {
            current = target > GameBalance.MIN_TROPHIES ? GameBalance.WIN_TROPHIES : GameBalance.MIN_TROPHIES;
            values.add(current);
        } else {
            current = synced;
        }
        while (current < target) {
            current += GameBalance.WIN_TROPHIES;
            values.add(current);
        }
        while (current > target) {
            current = Math.max(GameBalance.MIN_TROPHIES, current + GameBalance.LOSS_TROPHIES);
            values.add(current);
        }
        return values.isEmpty() ? NONE : new TrophySync(season, create, values);
    }

    public String getSeason() {
        return season;
    }

    public boolean isCreate() {
        return create;
    }

    public List<Integer> getValues() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
