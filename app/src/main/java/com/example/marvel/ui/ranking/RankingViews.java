package com.example.marvel.ui.ranking;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.marvel.R;
import com.example.marvel.data.season.RankingEntry;
import com.example.marvel.game.Ranks;
import com.example.marvel.game.SeasonPrize;
import com.example.marvel.ui.common.ArtifactUi;
import com.example.marvel.ui.common.Avatars;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.UiTokens;

final class RankingViews {

    private static final int[] PODIUM_COLORS = {R.color.seal_gold, R.color.seal_silver, R.color.seal_bronze};

    private RankingViews() {
    }

    static void bindPill(View pill, RankingEntry entry, boolean me) {
        Context context = pill.getContext();
        int secondary = context.getColor(me ? R.color.ranking_me_secondary : R.color.text_secondary);
        String position = PlayerHud.format(entry.getPosition());
        String rankName = rankName(context, entry);
        String trophies = PlayerHud.format(entry.getTrophies());

        pill.setBackgroundResource(me ? R.drawable.bg_ranking_pill_me : R.drawable.bg_ranking_pill);
        TextView positionView = pill.findViewById(R.id.pill_position);
        positionView.setText(position);
        positionView.setTextColor(me ? context.getColor(R.color.text_primary) : secondary);
        bindAvatar(pill.findViewById(R.id.pill_avatar), entry.getPlayerName());
        TextView name = bindName(pill.findViewById(R.id.pill_name), entry.getPlayerName(), me);

        ImageView rankIcon = pill.findViewById(R.id.pill_rank_icon);
        rankIcon.setVisibility(View.VISIBLE);
        rankIcon.setImageTintList(ColorStateList.valueOf(secondary));
        TextView rank = pill.findViewById(R.id.pill_rank);
        rank.setText(rankName);
        rank.setTextColor(secondary);

        ImageView prizeIcon = pill.findViewById(R.id.pill_prize_icon);
        prizeIcon.setVisibility(View.VISIBLE);
        prizeIcon.setImageTintList(ColorStateList.valueOf(secondary));
        TextView prize = pill.findViewById(R.id.pill_prize);
        prize.setVisibility(View.VISIBLE);
        prize.setText(prizeShort(context, entry.getPosition()));
        prize.setTextColor(secondary);

        View badge = pill.findViewById(R.id.pill_trophies_badge);
        badge.setVisibility(View.VISIBLE);
        badge.setBackgroundTintList(ColorStateList.valueOf(
                context.getColor(me ? R.color.ranking_me_badge : R.color.surface_high)));
        ((ImageView) pill.findViewById(R.id.pill_trophies_icon)).setImageTintList(ColorStateList.valueOf(
                context.getColor(me ? R.color.text_primary : R.color.red)));
        ((TextView) pill.findViewById(R.id.pill_trophies)).setText(trophies);

        pill.setContentDescription(context.getString(R.string.ranking_row_description,
                position, name.getText(), rankName, trophies) + ". " + prizeLong(context, entry.getPosition()));
    }

    static void bindPillMessage(View pill, String myName, int messageRes) {
        Context context = pill.getContext();
        pill.setBackgroundResource(R.drawable.bg_ranking_pill_me);
        TextView positionView = pill.findViewById(R.id.pill_position);
        positionView.setText(R.string.ranking_no_position);
        positionView.setTextColor(context.getColor(R.color.text_primary));
        bindAvatar(pill.findViewById(R.id.pill_avatar), myName == null ? "" : myName);
        TextView name = pill.findViewById(R.id.pill_name);
        if (myName == null || myName.isEmpty()) {
            name.setText(R.string.ranking_you_alone);
        } else {
            bindName(name, myName, true);
        }
        pill.findViewById(R.id.pill_rank_icon).setVisibility(View.GONE);
        TextView message = pill.findViewById(R.id.pill_rank);
        message.setText(messageRes);
        message.setTextColor(context.getColor(R.color.ranking_me_secondary));
        pill.findViewById(R.id.pill_prize_icon).setVisibility(View.GONE);
        pill.findViewById(R.id.pill_prize).setVisibility(View.GONE);
        pill.findViewById(R.id.pill_trophies_badge).setVisibility(View.GONE);
        pill.setContentDescription(name.getText() + ". " + message.getText());
    }

    static void bindSpot(View spot, RankingEntry entry, int place, boolean me) {
        Context context = spot.getContext();
        int medal = context.getColor(PODIUM_COLORS[place - 1]);
        int size = context.getResources().getDimensionPixelSize(
                place == 1 ? R.dimen.ranking_avatar_first : R.dimen.ranking_avatar_podium);
        View area = spot.findViewById(R.id.spot_avatar_area);
        ViewGroup.LayoutParams params = area.getLayoutParams();
        if (params.width != size) {
            params.width = size;
            params.height = size;
            area.setLayoutParams(params);
        }
        ImageView crown = spot.findViewById(R.id.spot_crown);
        crown.setVisibility(place == 1 ? View.VISIBLE : View.GONE);

        View ring = spot.findViewById(R.id.spot_ring);
        TextView avatar = spot.findViewById(R.id.spot_avatar);
        TextView badge = spot.findViewById(R.id.spot_badge);
        TextView name = spot.findViewById(R.id.spot_name);
        View rankRow = spot.findViewById(R.id.spot_rank_row);
        View trophiesRow = spot.findViewById(R.id.spot_trophies_row);
        View prizeRow = spot.findViewById(R.id.spot_prize_row);
        badge.setText(String.valueOf(place));

        if (entry == null) {
            int empty = context.getColor(R.color.divider);
            crown.setAlpha(UiTokens.ALPHA_MISSING);
            ring.setBackgroundTintList(ColorStateList.valueOf(empty));
            avatar.setText(R.string.ranking_empty_spot);
            avatar.setTextColor(context.getColor(R.color.text_secondary));
            avatar.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.surface_high)));
            badge.setBackgroundTintList(ColorStateList.valueOf(empty));
            badge.setTextColor(context.getColor(R.color.text_secondary));
            name.setText(R.string.ranking_empty_spot);
            name.setTextColor(context.getColor(R.color.text_secondary));
            rankRow.setVisibility(View.INVISIBLE);
            trophiesRow.setVisibility(View.INVISIBLE);
            prizeRow.setVisibility(View.INVISIBLE);
            spot.setContentDescription(context.getString(R.string.ranking_spot_empty_description,
                    String.valueOf(place)));
            return;
        }

        crown.setAlpha(1f);
        ring.setBackgroundTintList(ColorStateList.valueOf(medal));
        bindAvatar(avatar, entry.getPlayerName());
        avatar.setTextColor(context.getColor(R.color.text_primary));
        badge.setBackgroundTintList(ColorStateList.valueOf(medal));
        badge.setTextColor(context.getColor(R.color.black));
        bindName(name, entry.getPlayerName(), me);
        name.setTextColor(context.getColor(me ? R.color.red : R.color.text_primary));

        String rankName = rankName(context, entry);
        String trophies = PlayerHud.format(entry.getTrophies());
        rankRow.setVisibility(View.VISIBLE);
        ((TextView) spot.findViewById(R.id.spot_rank)).setText(rankName);
        trophiesRow.setVisibility(View.VISIBLE);
        ((TextView) spot.findViewById(R.id.spot_trophies)).setText(trophies);
        prizeRow.setVisibility(View.VISIBLE);
        ((ImageView) spot.findViewById(R.id.spot_prize_icon)).setImageTintList(ColorStateList.valueOf(medal));
        TextView prize = spot.findViewById(R.id.spot_prize);
        prize.setText(prizeShort(context, place));
        prize.setTextColor(medal);

        spot.setContentDescription(context.getString(R.string.ranking_row_description,
                String.valueOf(place), name.getText(), rankName, trophies) + ". " + prizeLong(context, place));
    }

    private static TextView bindName(TextView view, String playerName, boolean me) {
        Context context = view.getContext();
        view.setText(me ? context.getString(R.string.ranking_you, playerName) : playerName);
        view.setEllipsize(me ? TextUtils.TruncateAt.MIDDLE : TextUtils.TruncateAt.END);
        return view;
    }

    private static void bindAvatar(TextView avatar, String playerName) {
        Avatars.bindInitial(avatar, playerName);
    }

    private static String rankName(Context context, RankingEntry entry) {
        return PlayerHud.rankName(context, Ranks.indexFor(entry.getTrophies()));
    }

    private static String prizeShort(Context context, int position) {
        SeasonPrize prize = SeasonPrize.forPosition(position);
        String rarity = ArtifactUi.rarityName(context, prize.getRarity());
        if (prize.getCoins() > 0) {
            return context.getString(R.string.ranking_prize_short, rarity, PlayerHud.format(prize.getCoins()));
        }
        return context.getString(R.string.ranking_chest, rarity);
    }

    private static String prizeLong(Context context, int position) {
        SeasonPrize prize = SeasonPrize.forPosition(position);
        String rarity = ArtifactUi.rarityName(context, prize.getRarity());
        if (prize.getCoins() > 0) {
            return context.getString(R.string.ranking_prize, rarity, PlayerHud.format(prize.getCoins()));
        }
        return context.getString(R.string.ranking_chest, rarity);
    }
}
