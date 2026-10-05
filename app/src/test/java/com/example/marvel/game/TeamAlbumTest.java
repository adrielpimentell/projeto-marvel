package com.example.marvel.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TeamAlbumTest {

    private static final int TEAM = GameBalance.ALBUM_TEAM_IDS[0];
    private static final List<Integer> MEMBERS = Arrays.asList(1440, 1441, 1442, 1443, 1444, 1445);

    private static PlayerState withMembers(int count) {
        PlayerState state = PlayerState.newGame();
        for (int i = 0; i < count; i++) {
            assertTrue(state.buy(new OwnedHero(MEMBERS.get(i), GameAttributes.of(50, 50, 50, 50)), 0));
        }
        return state;
    }

    private static int allCoins() {
        int total = 0;
        for (int coins : GameBalance.TEAM_MILESTONE_COINS) total += coins;
        return total;
    }

    @Test
    public void milestonesAreOneThreeAndFiveMembers() {
        assertEquals(0, TeamAlbum.milestonesReached(0));
        assertEquals(1, TeamAlbum.milestonesReached(1));
        assertEquals(1, TeamAlbum.milestonesReached(2));
        assertEquals(2, TeamAlbum.milestonesReached(3));
        assertEquals(3, TeamAlbum.milestonesReached(5));
        assertEquals(3, TeamAlbum.milestonesReached(200));
        assertEquals(3, TeamAlbum.nextMilestone(1));
        assertEquals(-1, TeamAlbum.nextMilestone(3));
    }

    @Test
    public void onlyOwnedMembersCount() {
        PlayerState state = withMembers(2);
        assertEquals(2, TeamAlbum.countOwned(state, MEMBERS));
        assertEquals(0, TeamAlbum.countOwned(state, Arrays.asList(1, 2, 3)));
        assertEquals(0, TeamAlbum.countOwned(state, null));
    }

    @Test
    public void eachMilestoneIsPaidOnlyOnce() {
        PlayerState state = withMembers(1);
        int coinsBefore = state.getCoins();

        assertEquals(GameBalance.TEAM_MILESTONE_COINS[0], state.claimTeamRewards(TEAM, MEMBERS));
        assertEquals(0, state.claimTeamRewards(TEAM, MEMBERS));
        assertEquals(coinsBefore + GameBalance.TEAM_MILESTONE_COINS[0], state.getCoins());
        assertEquals(1, state.getClaimedMilestones(TEAM));
        assertEquals(1, state.countTeamSeals());
    }

    @Test
    public void reachingSeveralMilestonesPaysTheMissingOnesTogether() {
        PlayerState later = withMembers(5);
        later.claimTeamRewards(TEAM, MEMBERS.subList(0, 1));
        assertEquals(allCoins() - GameBalance.TEAM_MILESTONE_COINS[0],
                later.claimTeamRewards(TEAM, MEMBERS));
        assertEquals(3, later.getClaimedMilestones(TEAM));
        assertEquals(0, later.claimTeamRewards(TEAM, MEMBERS));
    }

    @Test
    public void nothingToPayWithoutMembersOrOutsideTheAlbum() {
        PlayerState state = withMembers(0);
        assertEquals(0, state.claimTeamRewards(TEAM, MEMBERS));
        assertEquals(0, withMembers(5).claimTeamRewards(-1, MEMBERS));
        assertEquals(0, state.getClaimedMilestones(TEAM));
    }

    @Test
    public void claimsSurviveClosingAndReopeningTheApp() {
        Gson gson = new Gson();
        PlayerState state = withMembers(3);
        int paid = state.claimTeamRewards(TEAM, MEMBERS);
        assertEquals(GameBalance.TEAM_MILESTONE_COINS[0] + GameBalance.TEAM_MILESTONE_COINS[1], paid);

        PlayerState reopened = gson.fromJson(gson.toJson(state), PlayerState.class);
        reopened.repair();
        assertEquals(2, reopened.getClaimedMilestones(TEAM));
        assertEquals(0, reopened.claimTeamRewards(TEAM, MEMBERS));
        assertEquals(state.getCoins(), reopened.getCoins());
    }

    @Test
    public void oldOrBrokenSavesAreRepaired() {
        Gson gson = new Gson();
        PlayerState old = gson.fromJson("{\"coins\":10}", PlayerState.class);
        old.repair();
        assertEquals(0, old.getClaimedMilestones(TEAM));

        PlayerState edited = gson.fromJson(
                "{\"teamMilestonesClaimed\":{\"" + TEAM + "\":9,\"3173\":-4}}", PlayerState.class);
        edited.repair();
        assertEquals(TeamAlbum.milestoneCount(), edited.getClaimedMilestones(TEAM));
        assertEquals(0, edited.getClaimedMilestones(3173));
        assertFalse(edited.claimTeamRewards(TEAM, MEMBERS) > 0);
    }

    @Test
    public void pendingCoinsAddUpOnlyTheUnpaidMilestones() {
        assertEquals(allCoins(), TeamAlbum.pendingCoins(0, 3));
        assertEquals(GameBalance.TEAM_MILESTONE_COINS[1], TeamAlbum.pendingCoins(1, 2));
        assertEquals(0, TeamAlbum.pendingCoins(2, 2));
        assertEquals(0, TeamAlbum.pendingCoins(3, 1));
    }
}
