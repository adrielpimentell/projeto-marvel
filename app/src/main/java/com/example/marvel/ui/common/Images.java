package com.example.marvel.ui.common;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.marvel.R;

public final class Images {

    private static final int PLACEHOLDER = R.drawable.placeholder_character;

    private Images() {
    }

    public static void load(ImageView view, String url) {
        request(view, url).into(view);
    }

    public static void loadWithFade(ImageView view, String url) {
        request(view, url).transition(DrawableTransitionOptions.withCrossFade()).into(view);
    }

    public static void loadLarge(ImageView view, String largeUrl, String smallUrl) {
        request(view, largeUrl).thumbnail(Glide.with(view).load(smallUrl)).into(view);
    }

    private static RequestBuilder<Drawable> request(ImageView view, String url) {
        return Glide.with(view)
                .load(url)
                .placeholder(PLACEHOLDER)
                .error(PLACEHOLDER)
                .fallback(PLACEHOLDER);
    }
}
