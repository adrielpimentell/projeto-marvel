package com.example.marvel.ui.ranking;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;

import com.example.marvel.R;
import com.example.marvel.data.season.RankingEntry;
import com.example.marvel.game.Ranks;
import com.example.marvel.game.SeasonPrize;
import com.example.marvel.ui.common.ArtifactUi;
import com.example.marvel.ui.common.Cards;
import com.example.marvel.ui.common.PlayerHud;
import com.google.android.material.card.MaterialCardView;

final class RankingRows {

    private static final int[] PODIUM_COLORS = {R.color.seal_gold, R.color.seal_silver, R.color.seal_bronze};

    private RankingRows() {
    }

    static void bind(View row, RankingEntry entry, boolean isMe) {
        Context context = row.getContext();
        int position = entry.getPosition();
        String positionText = PlayerHud.format(position);
        String rankName = PlayerHud.rankName(context, Ranks.indexFor(entry.getTrophies()));
        String trophies = PlayerHud.format(entry.getTrophies());
        boolean podium = SeasonPrize.isPodium(position);
        int medal = podium ? PODIUM_COLORS[position - 1] : 0;

        TextView positionView = row.findViewById(R.id.ranking_position);
        positionView.setText(positionText);
        positionView.setBackgroundTintList(ColorStateList.valueOf(
                context.getColor(podium ? medal : R.color.divider)));
        positionView.setTextColor(context.getColor(podium ? R.color.black : R.color.text_primary));

        TextView name = row.findViewById(R.id.ranking_name);
        name.setText(isMe ? context.getString(R.string.ranking_you, entry.getPlayerName()) : entry.getPlayerName());
        ((TextView) row.findViewById(R.id.ranking_rank)).setText(rankName);
        TextView trophiesView = row.findViewById(R.id.ranking_trophies);
        trophiesView.setText(trophies);
        trophiesView.setVisibility(View.VISIBLE);

        TextView prizeView = row.findViewById(R.id.ranking_prize);
        String prizeText = null;
        if (podium) {
            SeasonPrize prize = SeasonPrize.forPosition(position);
            prizeText = context.getString(R.string.ranking_prize,
                    ArtifactUi.rarityName(context, prize.getRarity()), PlayerHud.format(prize.getCoins()));
            prizeView.setText(prizeText);
            prizeView.setTextColor(context.getColor(medal));
            TextViewCompat.setCompoundDrawableTintList(prizeView, ColorStateList.valueOf(context.getColor(medal)));
        }
        prizeView.setVisibility(podium ? View.VISIBLE : View.GONE);

        MaterialCardView card = (MaterialCardView) row;
        if (isMe) {
            Cards.highlight(card, true);
        } else if (podium) {
            Cards.outline(card, medal);
        } else {
            Cards.highlight(card, false);
        }

        String description = context.getString(R.string.ranking_row_description,
                positionText, name.getText(), rankName, trophies);
        card.setContentDescription(prizeText == null ? description : description + ". " + prizeText);
    }

    static void bindMessage(View row, String playerName, int messageRes) {
        Context context = row.getContext();
        TextView positionView = row.findViewById(R.id.ranking_position);
        positionView.setText(R.string.ranking_no_position);
        positionView.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.divider)));
        positionView.setTextColor(context.getColor(R.color.text_primary));
        TextView name = row.findViewById(R.id.ranking_name);
        name.setText(playerName == null ? "" : context.getString(R.string.ranking_you, playerName));
        TextView rank = row.findViewById(R.id.ranking_rank);
        rank.setText(messageRes);
        row.findViewById(R.id.ranking_trophies).setVisibility(View.GONE);
        row.findViewById(R.id.ranking_prize).setVisibility(View.GONE);
        Cards.highlight((MaterialCardView) row, true);
        row.setContentDescription(name.getText() + ". " + rank.getText());
    }
}
