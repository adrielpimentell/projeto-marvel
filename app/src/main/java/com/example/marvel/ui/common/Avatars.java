package com.example.marvel.ui.common;

import android.content.res.ColorStateList;
import android.widget.TextView;

import com.example.marvel.R;

import java.util.Locale;

public final class Avatars {

    private static final int[] COLORS = {R.color.avatar_1, R.color.avatar_2, R.color.avatar_3,
            R.color.avatar_4, R.color.avatar_5, R.color.avatar_6};

    private Avatars() {
    }

    public static String initial(String playerName) {
        if (playerName == null || playerName.isEmpty()) return "?";
        return playerName.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public static int colorRes(String playerName) {
        String key = playerName == null ? "" : playerName.toLowerCase(Locale.ROOT);
        return COLORS[Math.floorMod(key.hashCode(), COLORS.length)];
    }

    public static void bindInitial(TextView avatar, String playerName) {
        avatar.setText(initial(playerName));
        avatar.setBackgroundTintList(ColorStateList.valueOf(avatar.getContext().getColor(colorRes(playerName))));
    }
}
