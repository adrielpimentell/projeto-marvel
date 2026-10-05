package com.example.marvel.ui.album;

import com.example.marvel.data.model.Team;

import java.util.Collections;
import java.util.List;

final class AlbumEntry {

    final int teamId;
    Team team;
    List<Integer> memberIds = Collections.emptyList();
    boolean failed;

    AlbumEntry(int teamId) {
        this.teamId = teamId;
    }

    boolean isReady() {
        return team != null;
    }

    void setTeam(Team team) {
        this.team = team;
        this.memberIds = team.getMemberIds();
        this.failed = false;
    }

    static boolean isPlayable(Team team) {
        return team != null && team.isMarvel() && !team.getMembers().isEmpty();
    }
}
