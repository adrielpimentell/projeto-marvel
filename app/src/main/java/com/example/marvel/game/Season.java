package com.example.marvel.game;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public final class Season {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private Season() {
    }

    public static Instant now() {
        return Instant.now();
    }

    public static String currentId() {
        return idAt(now());
    }

    public static Instant currentEnd() {
        return endOf(now());
    }

    public static String idAt(Instant instant) {
        ZonedDateTime local = instant.atZone(ZONE);
        int year = local.get(IsoFields.WEEK_BASED_YEAR);
        int week = local.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format(Locale.ROOT, "%d-W%02d", year, week);
    }

    public static Instant startOf(Instant instant) {
        return instant.atZone(ZONE)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

    public static Instant endOf(Instant instant) {
        return startOf(instant).atZone(ZONE).plusWeeks(1).toInstant();
    }

    public static String previousId(Instant instant) {
        return idAt(startOf(instant).minusSeconds(1));
    }
}
