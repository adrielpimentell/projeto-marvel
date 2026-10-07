package com.example.marvel.ui.ranking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.marvel.data.season.RankingEntry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RankingAdapterTest {

    private static final String ME = "me";

    @Test
    public void nothingBeforeTheFirstLoad() {
        assertFalse(RankingLayout.LOADING.isLoaded());
        assertEquals(0, RankingLayout.LOADING.itemCount());
    }

    @Test
    public void emptyRankingShowsEmptyPodiumAndMessage() {
        RankingLayout layout = load(0, null);
        assertEquals(2, layout.itemCount());
        assertEquals(RankingLayout.TYPE_PODIUM, layout.typeAt(0));
        assertEquals(RankingLayout.TYPE_MESSAGE, layout.typeAt(1));
        assertNull(layout.podium(1));
        assertEquals(RankingLayout.NONE, layout.positionOf(ME));
    }

    @Test
    public void onlyMeShowsPodiumAndMessage() {
        RankingLayout layout = load(1, 1);
        assertEquals(2, layout.itemCount());
        assertEquals(0, layout.positionOf(ME));
        assertTrue(layout.isMe(layout.podium(1)));
        assertNull(layout.podium(2));
    }

    @Test
    public void oneOtherPlayerShowsOnlyThePodium() {
        RankingLayout layout = load(1, null);
        assertEquals(1, layout.itemCount());
        assertEquals(RankingLayout.NONE, layout.positionOf(ME));
    }

    @Test
    public void twoAndThreePlayersFitInThePodium() {
        assertEquals(1, load(2, 2).itemCount());
        assertNull(load(2, 2).podium(3));
        assertEquals(1, load(3, 3).itemCount());
        assertEquals(0, load(3, 3).positionOf(ME));
    }

    @Test
    public void fourthPlayerIsTheFirstPill() {
        RankingLayout layout = load(4, 4);
        assertEquals(2, layout.itemCount());
        assertEquals(RankingLayout.TYPE_ROW, layout.typeAt(1));
        assertEquals(1, layout.positionOf(ME));
        assertEquals(4, layout.rowAt(1).getPosition());
    }

    @Test
    public void fiftyPlayersMakeFortySevenPills() {
        RankingLayout layout = load(50, 50);
        assertEquals(48, layout.itemCount());
        assertEquals(47, layout.positionOf(ME));
        assertEquals(50, layout.rowAt(47).getPosition());
        assertEquals(3, load(50, 6).positionOf(ME));
    }

    @Test
    public void avatarUsesTheFirstLetterAndAStableColor() {
        assertEquals("T", RankingViews.initial("tony_99"));
        assertEquals("?", RankingViews.initial(""));
        assertEquals(RankingViews.avatarColor("Tony_99"), RankingViews.avatarColor("tony_99"));
    }

    private static RankingLayout load(int players, Integer myPosition) {
        List<RankingEntry> entries = new ArrayList<>();
        for (int position = 1; position <= players; position++) {
            String uid = myPosition != null && myPosition == position ? ME : "player" + position;
            entries.add(new RankingEntry(uid, "jogador_" + position, 1000 - position * 10, position));
        }
        return RankingLayout.of(entries, ME);
    }
}
