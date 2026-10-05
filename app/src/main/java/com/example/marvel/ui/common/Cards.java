package com.example.marvel.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;

import com.example.marvel.R;
import com.google.android.material.card.MaterialCardView;

public final class Cards {

    private static final int HIGHLIGHT_DP = 2;

    private Cards() {
    }

    public static void highlight(MaterialCardView card, boolean on) {
        Context context = card.getContext();
        if (on) {
            float density = context.getResources().getDisplayMetrics().density;
            card.setStrokeColor(ColorStateList.valueOf(context.getColor(R.color.red)));
            card.setStrokeWidth(Math.round(HIGHLIGHT_DP * density));
        } else {
            card.setStrokeColor(ColorStateList.valueOf(context.getColor(R.color.card_stroke)));
            card.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.card_stroke_width));
        }
    }
}
