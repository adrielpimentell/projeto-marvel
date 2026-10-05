package com.example.marvel.ui.common;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.widget.TextView;

import com.example.marvel.R;
import com.example.marvel.game.PlayerState;
import com.example.marvel.ui.ranks.RankTrailActivity;

import java.text.NumberFormat;
import java.util.Locale;

public final class PlayerHud {

    private static final NumberFormat NUMBER_FORMAT =
            NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));

    private PlayerHud() {
    }

    public static void bind(View hud, PlayerState state) {
        Context context = hud.getContext();
        String rank = rankName(context, state.getRank());

        TextView rankView = hud.findViewById(R.id.hud_rank);
        rankView.setText(rank);
        rankView.setContentDescription(context.getString(R.string.rank_description_hud, rank));
        Activity activity = findActivity(context);
        if (activity != null && !(activity instanceof RankTrailActivity)) {
            rankView.setOnClickListener(v -> MainNav.openRanks(activity));
        }

        TextView trophiesView = hud.findViewById(R.id.hud_trophies);
        trophiesView.setText(format(state.getTrophies()));
        trophiesView.setContentDescription(
                context.getString(R.string.trophies_description, state.getTrophies()));

        showCoins(hud.findViewById(R.id.coin_chip), state.getCoins());
    }

    public static void showCoins(View coinChip, int coins) {
        TextView text = coinChip.findViewById(R.id.coin_text);
        text.setText(format(coins));
        coinChip.setContentDescription(
                coinChip.getContext().getString(R.string.coins_description, coins));
    }

    public static String rankName(Context context, int rank) {
        String[] names = context.getResources().getStringArray(R.array.rank_names);
        return names[Math.max(0, Math.min(names.length - 1, rank))];
    }

    public static String rankDescription(Context context, int rank) {
        String[] descriptions = context.getResources().getStringArray(R.array.rank_descriptions);
        return descriptions[Math.max(0, Math.min(descriptions.length - 1, rank))];
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static String format(int number) {
        return NUMBER_FORMAT.format(number);
    }
}
