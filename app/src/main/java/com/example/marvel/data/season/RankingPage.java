package com.example.marvel.data.season;

import java.util.Collections;
import java.util.List;

public final class RankingPage {

    private final List<RankingEntry> entries;
    private final boolean fromCache;

    public RankingPage(List<RankingEntry> entries, boolean fromCache) {
        this.entries = Collections.unmodifiableList(entries);
        this.fromCache = fromCache;
    }

    public List<RankingEntry> getEntries() {
        return entries;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public RankingEntry find(String uid) {
        for (RankingEntry entry : entries) {
            if (entry.getUid().equals(uid)) return entry;
        }
        return null;
    }
}
