package com.example.marvel.data.auth;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PlayerName {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 20;

    private static final Pattern VALID =
            Pattern.compile("[A-Za-z0-9_]{" + MIN_LENGTH + "," + MAX_LENGTH + "}");

    private PlayerName() {
    }

    public static String clean(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    public static String key(String name) {
        return clean(name).toLowerCase(Locale.ROOT);
    }
}
