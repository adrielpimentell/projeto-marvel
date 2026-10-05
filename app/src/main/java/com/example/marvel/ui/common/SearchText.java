package com.example.marvel.ui.common;

import java.text.Normalizer;
import java.util.Locale;

public final class SearchText {

    private SearchText() {
    }

    public static String normalize(String text) {
        return withoutAccents(text).toLowerCase(Locale.ROOT).trim();
    }

    public static String withoutAccents(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    public static boolean matches(String text, String query) {
        String q = normalize(query);
        return q.isEmpty() || normalize(text).contains(q);
    }
}
