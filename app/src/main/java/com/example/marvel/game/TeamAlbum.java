package com.example.marvel.game;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class TeamAlbum {

    private TeamAlbum() {
    }

    public static boolean isAlbumTeam(int teamId) {
        for (int id : GameBalance.ALBUM_TEAM_IDS) {
            if (id == teamId) return true;
        }
        return false;
    }

    public static int milestoneCount() {
        return GameBalance.TEAM_MILESTONES.length;
    }

    public static int milestone(int index) {
        return GameBalance.TEAM_MILESTONES[index];
    }

    public static int milestoneCoins(int index) {
        return GameBalance.TEAM_MILESTONE_COINS[index];
    }

    public static int milestonesReached(int ownedMembers) {
        int reached = 0;
        for (int needed : GameBalance.TEAM_MILESTONES) {
            if (ownedMembers >= needed) reached++;
        }
        return reached;
    }

    public static int pendingCoins(int claimed, int reached) {
        int coins = 0;
        for (int i = Math.max(0, claimed); i < Math.min(reached, milestoneCount()); i++) {
            coins += GameBalance.TEAM_MILESTONE_COINS[i];
        }
        return coins;
    }

    public static int nextMilestone(int reached) {
        return reached < milestoneCount() ? GameBalance.TEAM_MILESTONES[reached] : -1;
    }

    public static int countOwned(PlayerState state, Collection<Integer> memberIds) {
        if (state == null || memberIds == null || memberIds.isEmpty()) return 0;
        Set<Integer> members = new HashSet<>(memberIds);
        int owned = 0;
        for (OwnedHero hero : state.getHeroes()) {
            if (members.contains(hero.getCharacterId())) owned++;
        }
        return owned;
    }
}
