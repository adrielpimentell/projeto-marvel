package com.example.marvel.data.season;

public final class RankingEntry {

    private final String uid;
    private final String playerName;
    private final int trophies;
    private final int position;

    public RankingEntry(String uid, String playerName, int trophies, int position) {
        this.uid = uid;
        this.playerName = playerName;
        this.trophies = trophies;
        this.position = position;
    }

    public String getUid() {
        return uid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getTrophies() {
        return trophies;
    }

    public int getPosition() {
        return position;
    }
}
