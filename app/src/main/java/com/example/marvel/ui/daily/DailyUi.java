package com.example.marvel.ui.daily;

import android.content.Context;

import com.example.marvel.R;
import com.example.marvel.game.DailyChallenge;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerState;
import com.example.marvel.ui.common.OriginUi;

import java.util.ArrayList;
import java.util.List;

public final class DailyUi {

    private static final int MAX_HEROES_IN_HINT = 2;

    private DailyUi() {
    }

    public static String describe(Context context, DailyChallenge c) {
        int target = c.getTarget();
        switch (c.getType()) {
            case WIN_BATTLES:
                return context.getResources().getQuantityString(R.plurals.daily_win_battles, target, target);
            case PLAY_BATTLES:
                return context.getResources().getQuantityString(R.plurals.daily_play_battles, target, target);
            case LAND_CRITS:
                return context.getResources().getQuantityString(R.plurals.daily_land_crits, target, target);
            case DEFEAT_ORIGIN:
                return context.getString(R.string.daily_defeat_origin,
                        OriginUi.name(context, c.getParam(), c.getParamName()));
            case DEFEAT_TEAM_MEMBER:
                return context.getString(R.string.daily_defeat_team, c.getParamName());
            case WIN_WITH_TEAM:
                return context.getString(R.string.daily_win_with_team, c.getParamName());
            case WIN_WITH_POWER:
                return context.getString(R.string.daily_win_with_power, c.getParamName());
            case WIN_WITH_ARTIFACT:
                return context.getString(R.string.daily_win_with_artifact);
            case WIN_VS_STRONGER:
                return context.getString(R.string.daily_win_vs_stronger);
            default:
                return context.getString(R.string.daily_win_with_overall, c.getParam());
        }
    }

    public static String hint(Context context, DailyChallenge c, PlayerState state) {
        if (c.isComplete()) return "";
        switch (c.getType()) {
            case WIN_WITH_TEAM:
            case WIN_WITH_POWER: {
                List<String> names = new ArrayList<>();
                for (OwnedHero hero : c.heroesThatCount(state)) {
                    if (names.size() == MAX_HEROES_IN_HINT) break;
                    names.add(hero.hasInfo() ? hero.getName()
                            : context.getString(R.string.starter_hero_fallback));
                }
                return names.isEmpty() ? "" : context.getString(R.string.daily_hint_heroes,
                        String.join(", ", names));
            }
            case DEFEAT_ORIGIN:
            case DEFEAT_TEAM_MEMBER:
            case WIN_VS_STRONGER:
                return context.getString(R.string.daily_hint_enemy);
            case WIN_WITH_ARTIFACT:
                return c.isPossibleWith(state) ? context.getString(R.string.daily_hint_artifact) : "";
            case WIN_WITH_OVERALL: {
                int best = 0;
                for (OwnedHero hero : state.getHeroes()) {
                    best = Math.max(best, hero.getBattleAttributes().getOverall());
                }
                return best >= c.getParam() ? "" : context.getString(R.string.daily_hint_overall, best);
            }
            default:
                return "";
        }
    }

    public static int iconRes(DailyChallenge.Type type) {
        switch (type) {
            case WIN_BATTLES: return R.drawable.ic_trophy;
            case PLAY_BATTLES: return R.drawable.ic_bolt;
            case LAND_CRITS: return R.drawable.ic_star;
            case DEFEAT_ORIGIN: return R.drawable.ic_person;
            case DEFEAT_TEAM_MEMBER:
            case WIN_WITH_TEAM: return R.drawable.ic_group;
            case WIN_WITH_POWER: return R.drawable.ic_bolt;
            case WIN_WITH_ARTIFACT: return R.drawable.ic_gem;
            case WIN_VS_STRONGER: return R.drawable.ic_shield;
            default: return R.drawable.ic_medal;
        }
    }
}
