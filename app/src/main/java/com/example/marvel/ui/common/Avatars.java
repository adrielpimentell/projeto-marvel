package com.example.marvel.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.marvel.R;
import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerStore;

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

    public static void bind(TextView initialView, ImageView imageView, String playerName, String imageUrl) {
        bindInitial(initialView, playerName);
        if (imageUrl == null) {
            imageView.setVisibility(View.GONE);
            imageView.setImageDrawable(null);
        } else {
            imageView.setVisibility(View.VISIBLE);
            Images.load(imageView, imageUrl);
        }
    }

    public static void bindMine(TextView initialView, ImageView imageView) {
        Context context = initialView.getContext();
        AuthRepository auth = AuthRepository.getInstance(context);
        bind(initialView, imageView, auth.cachedPlayerName(), myImageUrl(context));
    }

    public static String myImageUrl(Context context) {
        int heroId = AuthRepository.getInstance(context).cachedAvatarHeroId();
        if (heroId <= 0) return null;
        OwnedHero hero = PlayerStore.getInstance(context).get().find(heroId);
        return hero != null && hero.hasInfo() ? hero.getImageUrl() : null;
    }
}
