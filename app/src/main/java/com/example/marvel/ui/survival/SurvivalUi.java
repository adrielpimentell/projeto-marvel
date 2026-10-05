package com.example.marvel.ui.survival;

import android.content.Context;

import com.example.marvel.R;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.SurvivalBuff;
import com.example.marvel.game.SurvivalRun;

import java.util.ArrayList;
import java.util.List;

final class SurvivalUi {

    private SurvivalUi() {
    }

    static String name(Context context, SurvivalBuff buff) {
        switch (buff) {
            case FURY: return context.getString(R.string.survival_buff_fury);
            case ARMOR: return context.getString(R.string.survival_buff_armor);
            case FOCUS: return context.getString(R.string.survival_buff_focus);
            case LIFESTEAL: return context.getString(R.string.survival_buff_lifesteal);
            case THORNS: return context.getString(R.string.survival_buff_thorns);
            case REGENERATION: return context.getString(R.string.survival_buff_regen);
            case REFLEXES: return context.getString(R.string.survival_buff_reflexes);
            default: return context.getString(R.string.survival_buff_medkit);
        }
    }

    static String description(Context context, SurvivalBuff buff) {
        switch (buff) {
            case FURY:
                return context.getString(R.string.survival_buff_fury_desc, GameBalance.SURVIVAL_FURY_DAMAGE_PERCENT);
            case ARMOR:
                return context.getString(R.string.survival_buff_armor_desc, GameBalance.SURVIVAL_ARMOR_HP_PERCENT);
            case FOCUS:
                return context.getString(R.string.survival_buff_focus_desc, GameBalance.CRIT_BOOST_PERCENT);
            case LIFESTEAL:
                return context.getString(R.string.survival_buff_lifesteal_desc, GameBalance.LIFESTEAL_PERCENT);
            case THORNS:
                return context.getString(R.string.survival_buff_thorns_desc, GameBalance.THORNS_PERCENT);
            case REGENERATION:
                return context.getString(R.string.survival_buff_regen_desc, GameBalance.REGENERATION_PERCENT);
            case REFLEXES:
                return context.getString(R.string.survival_buff_reflexes_desc, GameBalance.DODGE_CHANCE_PERCENT);
            default:
                return context.getString(R.string.survival_buff_medkit_desc,
                        GameBalance.SURVIVAL_MEDKIT_HEAL_PERCENT);
        }
    }

    static int icon(SurvivalBuff buff) {
        switch (buff) {
            case FURY: return R.drawable.ic_bolt;
            case ARMOR:
            case THORNS: return R.drawable.ic_shield;
            case FOCUS: return R.drawable.ic_star;
            case LIFESTEAL:
            case REGENERATION: return R.drawable.ic_drop;
            case REFLEXES: return R.drawable.ic_person;
            default: return R.drawable.ic_add;
        }
    }

    static int color(Context context, SurvivalBuff buff) {
        switch (buff) {
            case FURY:
            case LIFESTEAL: return context.getColor(R.color.red);
            case ARMOR:
            case REFLEXES: return context.getColor(R.color.effect_shield);
            case FOCUS: return context.getColor(R.color.effect_stun);
            case THORNS: return context.getColor(R.color.effect_thorns);
            default: return context.getColor(R.color.effect_green);
        }
    }

    static String buffList(Context context, SurvivalRun run) {
        List<String> parts = new ArrayList<>();
        for (SurvivalBuff buff : SurvivalBuff.values()) {
            int count = run.count(buff);
            if (count == 1) parts.add(name(context, buff));
            if (count > 1) parts.add(context.getString(R.string.survival_buff_count, name(context, buff), count));
        }
        return parts.isEmpty() ? context.getString(R.string.survival_no_buffs)
                : context.getString(R.string.survival_buffs, String.join(" · ", parts));
    }
}
