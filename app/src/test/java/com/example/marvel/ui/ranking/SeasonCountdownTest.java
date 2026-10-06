package com.example.marvel.ui.ranking;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import java.time.Duration;

public class SeasonCountdownTest {

    @Test
    public void splitsIntoDaysHoursAndMinutes() {
        assertArrayEquals(new long[]{4, 12, 30},
                SeasonCountdown.parts(Duration.ofDays(4).plusHours(12).plusMinutes(30)));
    }

    @Test
    public void roundsSecondsUpToTheNextMinute() {
        assertArrayEquals(new long[]{0, 0, 1}, SeasonCountdown.parts(Duration.ofSeconds(5)));
        assertArrayEquals(new long[]{0, 1, 0}, SeasonCountdown.parts(Duration.ofMinutes(59).plusSeconds(30)));
    }

    @Test
    public void neverGoesNegative() {
        assertArrayEquals(new long[]{0, 0, 0}, SeasonCountdown.parts(Duration.ofMinutes(-3)));
    }
}
