package com.example.marvel.ui.battle;

import static org.junit.Assert.assertTrue;

import com.example.marvel.game.GameBalance;

import org.junit.Test;

public class BattleAnimatorTimingTest {

    @Test
    public void animationAlwaysLastsBetween5And15Seconds() {
        for (int turns = 1; turns <= GameBalance.MAX_TURNS; turns++) {
            long total = GameBalance.BATTLE_INTRO_MS
                    + BattleAnimator.turnDuration(turns) * turns
                    + GameBalance.BATTLE_OUTRO_MS;
            assertTrue(turns + " turnos: " + total + " ms", total >= 4_950 && total <= 15_000);
        }
    }
}
