package com.example.marvel.ui.ranks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.marvel.game.Ranks;

import org.junit.Test;

public class TrailMathTest {

    private final TrailMath math = new TrailMath(24f, 192f);

    @Test
    public void markerSitsOnTheRankCenterAtEachLimit() {
        for (int rank = 0; rank < Ranks.count(); rank++) {
            assertEquals(math.centerOf(rank), math.markerX(Ranks.minTrophies(rank)), 0.001f);
        }
    }

    @Test
    public void markerMovesHalfwayAtHalfTheTrophies() {
        float expected = (math.centerOf(1) + math.centerOf(2)) / 2f;
        assertEquals(expected, math.markerX(100), 0.001f);
    }

    @Test
    public void everyRankGetsTheSameSpace() {
        for (int rank = 1; rank < Ranks.count(); rank++) {
            assertEquals(192f, math.centerOf(rank) - math.centerOf(rank - 1), 0.001f);
        }
        assertEquals(24f * 2 + 192f * 17, math.contentWidth(), 0.001f);
    }

    @Test
    public void markerCrossesTheNewLimitsAtTheCardCenters() {
        assertTrue(math.markerX(4999) < math.centerOf(9));
        assertEquals(math.centerOf(9), math.markerX(5000), 0.001f);
        assertTrue(math.markerX(49_999) < math.centerOf(16));
        assertEquals(math.centerOf(16), math.markerX(50_000), 0.001f);
    }

    @Test
    public void markerOnlyMovesForwardAndStaysInsideTheTrack() {
        float previous = -1f;
        for (int trophies = 0; trophies <= 60_000; trophies += 7) {
            float x = math.markerX(trophies);
            assertTrue(x >= previous);
            assertTrue(x >= math.trackStart() && x <= math.trackEnd());
            previous = x;
        }
    }
}
