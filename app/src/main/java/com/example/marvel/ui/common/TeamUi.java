package com.example.marvel.ui.common;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.example.marvel.R;
import com.example.marvel.data.model.NamedRef;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.TeamAlbum;

import java.util.ArrayList;
import java.util.List;

public final class TeamUi {

    private static final int MAX_TEAMS_ON_CARD = 2;

    private static final int[] SEAL_COLORS = {R.color.seal_bronze, R.color.seal_silver, R.color.seal_gold};
    private static final int[] SEAL_NAMES = {R.string.seal_bronze, R.string.seal_silver, R.string.seal_gold};

    private TeamUi() {
    }

    public static int sealColor(Context context, int index) {
        return context.getColor(SEAL_COLORS[Math.max(0, Math.min(SEAL_COLORS.length - 1, index))]);
    }

    public static String sealName(Context context, int index) {
        return context.getString(SEAL_NAMES[Math.max(0, Math.min(SEAL_NAMES.length - 1, index))]);
    }

    public static CharSequence marketLine(Context context, List<NamedRef> teams) {
        if (teams == null || teams.isEmpty()) return context.getString(R.string.market_teams_none);

        List<NamedRef> ordered = new ArrayList<>();
        for (int albumId : GameBalance.ALBUM_TEAM_IDS) {
            for (NamedRef team : teams) {
                if (team != null && team.getId() == albumId) ordered.add(team);
            }
        }
        int albumCount = ordered.size();
        for (NamedRef team : teams) {
            if (team != null && !TeamAlbum.isAlbumTeam(team.getId())) ordered.add(team);
        }

        SpannableStringBuilder line = new SpannableStringBuilder();
        int shown = Math.min(MAX_TEAMS_ON_CARD, ordered.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) line.append(" · ");
            int start = line.length();
            line.append(ordered.get(i).getName());
            boolean inAlbum = i < albumCount;
            line.setSpan(new ForegroundColorSpan(context.getColor(
                            inAlbum ? R.color.text_primary : R.color.text_secondary)),
                    start, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (inAlbum) {
                line.setSpan(new StyleSpan(Typeface.BOLD), start, line.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (ordered.size() > shown) {
            line.append("  ").append(context.getString(R.string.market_teams_more, ordered.size() - shown));
        }
        return line;
    }
}
