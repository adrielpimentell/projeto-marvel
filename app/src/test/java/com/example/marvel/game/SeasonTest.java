package com.example.marvel.game;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;

public class SeasonTest {

    @Test
    public void weekStartsMondayMidnightInBrasilia() {
        assertEquals("2026-W40", Season.idAt(Instant.parse("2026-10-05T02:59:59Z")));
        assertEquals("2026-W41", Season.idAt(Instant.parse("2026-10-05T03:00:00Z")));
    }

    @Test
    public void startAndEndAreMondaysAtMidnightInBrasilia() {
        Instant wednesday = Instant.parse("2026-10-07T15:00:00Z");
        assertEquals(Instant.parse("2026-10-05T03:00:00Z"), Season.startOf(wednesday));
        assertEquals(Instant.parse("2026-10-12T03:00:00Z"), Season.endOf(wednesday));
    }

    @Test
    public void previousSeasonIsTheWeekBefore() {
        assertEquals("2026-W40", Season.previousId(Instant.parse("2026-10-07T15:00:00Z")));
        assertEquals("2025-W52", Season.previousId(Instant.parse("2026-01-01T12:00:00Z")));
    }

    @Test
    public void usesIsoWeekYearAtTheTurnOfTheYear() {
        assertEquals("2026-W53", Season.idAt(Instant.parse("2026-12-31T12:00:00Z")));
        assertEquals("2026-W53", Season.idAt(Instant.parse("2027-01-03T12:00:00Z")));
        assertEquals("2027-W01", Season.idAt(Instant.parse("2027-01-04T12:00:00Z")));
    }
}
