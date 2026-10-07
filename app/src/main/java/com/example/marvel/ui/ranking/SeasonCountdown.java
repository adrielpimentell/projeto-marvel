package com.example.marvel.ui.ranking;

import android.content.Context;

import com.example.marvel.R;

import java.time.Duration;

public final class SeasonCountdown {

    private SeasonCountdown() {
    }

    static long[] parts(Duration left) {
        long minutes = Math.max(0, (left.getSeconds() + 59) / 60);
        return new long[]{minutes / (24 * 60), (minutes / 60) % 24, minutes % 60};
    }

    public static String format(Context context, Duration left) {
        return context.getString(R.string.ranking_ends_in, time(context, left));
    }

    public static String time(Context context, Duration left) {
        long[] parts = parts(left);
        if (parts[0] > 0) {
            return context.getString(R.string.ranking_time_days, parts[0], parts[1]);
        }
        if (parts[1] > 0) {
            return context.getString(R.string.ranking_time_hours, parts[1], parts[2]);
        }
        return context.getString(R.string.ranking_time_minutes, parts[2]);
    }
}
