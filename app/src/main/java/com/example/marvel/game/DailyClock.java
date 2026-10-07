package com.example.marvel.game;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DailyClock {

    private DailyClock() {
    }

    public static int today() {
        return toKey(LocalDate.now(ZoneId.systemDefault()));
    }

    public static Duration untilNextDay() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight);
    }

    public static int toKey(LocalDate date) {
        return date.getYear() * 10_000 + date.getMonthValue() * 100 + date.getDayOfMonth();
    }
}
