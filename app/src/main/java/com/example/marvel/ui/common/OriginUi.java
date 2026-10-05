package com.example.marvel.ui.common;

import android.content.Context;

import com.example.marvel.R;

public final class OriginUi {

    private OriginUi() {
    }

    public static String name(Context context, int originId, String fallback) {
        switch (originId) {
            case 1: return context.getString(R.string.origin_mutant);
            case 2: return context.getString(R.string.origin_cyborg);
            case 3: return context.getString(R.string.origin_alien);
            case 4: return context.getString(R.string.origin_human);
            case 5: return context.getString(R.string.origin_robot);
            case 6: return context.getString(R.string.origin_radiation);
            case 7: return context.getString(R.string.origin_god);
            case 8: return context.getString(R.string.origin_animal);
            case 9: return context.getString(R.string.origin_other);
            case 10: return context.getString(R.string.origin_infection);
            default: return fallback == null ? "?" : fallback;
        }
    }
}
