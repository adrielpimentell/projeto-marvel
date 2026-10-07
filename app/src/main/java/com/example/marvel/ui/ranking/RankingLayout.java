package com.example.marvel.ui.ranking;

import com.example.marvel.data.season.RankingEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RankingLayout {

    static final int PODIUM_SIZE = 3;
    static final int TYPE_PODIUM = 0;
    static final int TYPE_MESSAGE = 1;
    static final int TYPE_ROW = 2;
    static final int NONE = -1;

    static final RankingLayout LOADING = new RankingLayout(Collections.emptyList(), null, false);

    private final List<RankingEntry> entries;
    private final String myUid;
    private final boolean loaded;
    private final boolean showMessage;

    private RankingLayout(List<RankingEntry> entries, String myUid, boolean loaded) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.myUid = myUid;
        this.loaded = loaded;
        this.showMessage = loaded && (entries.isEmpty()
                || (entries.size() == 1 && entries.get(0).getUid().equals(myUid)));
    }

    static RankingLayout of(List<RankingEntry> entries, String myUid) {
        return new RankingLayout(entries, myUid, true);
    }

    boolean isLoaded() {
        return loaded;
    }

    int itemCount() {
        if (!loaded) return 0;
        return firstRowPosition() + Math.max(0, entries.size() - PODIUM_SIZE);
    }

    int typeAt(int position) {
        if (position == 0) return TYPE_PODIUM;
        if (showMessage && position == 1) return TYPE_MESSAGE;
        return TYPE_ROW;
    }

    RankingEntry rowAt(int position) {
        return entries.get(PODIUM_SIZE + position - firstRowPosition());
    }

    RankingEntry podium(int place) {
        return entries.size() >= place ? entries.get(place - 1) : null;
    }

    boolean isMe(RankingEntry entry) {
        return entry != null && entry.getUid().equals(myUid);
    }

    int positionOf(String uid) {
        for (int i = 0; i < entries.size(); i++) {
            if (!entries.get(i).getUid().equals(uid)) continue;
            return i < PODIUM_SIZE ? 0 : firstRowPosition() + i - PODIUM_SIZE;
        }
        return NONE;
    }

    private int firstRowPosition() {
        return showMessage ? 2 : 1;
    }
}
