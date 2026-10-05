package com.example.marvel.ui.market;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.marvel.game.GameBalance;

import org.junit.Test;

public class WheelMathTest {

    @Test
    public void slicesAreProportionalToTheOdds() {
        for (int[] odds : GameBalance.ROULETTE_ODDS_PER_MILLE) {
            WheelMath wheel = new WheelMath(odds);
            float total = 0f;
            for (int i = 0; i < wheel.sliceCount(); i++) {
                assertEquals(360f * odds[i] / 1000f, wheel.sweepOf(i), 0.01f);
                total += wheel.sweepOf(i);
            }
            assertEquals(360f, total, 0.01f);
        }
    }

    @Test
    public void wheelAlwaysStopsOnTheDrawnRarity() {
        for (int[] odds : GameBalance.ROULETTE_ODDS_PER_MILLE) {
            WheelMath wheel = new WheelMath(odds);
            for (int slice = 0; slice < wheel.sliceCount(); slice++) {
                if (odds[slice] == 0) continue;
                for (float position = 0.2f; position <= 0.8f; position += 0.1f) {
                    float rotation = wheel.targetRotation(slice, position, GameBalance.ROULETTE_FULL_TURNS);
                    assertEquals(slice, wheel.sliceUnderPointer(rotation));
                    assertTrue("gira várias voltas", rotation >= GameBalance.ROULETTE_FULL_TURNS * 360f);
                }
            }
        }
    }

    @Test
    public void animationLastsBetween3And5Seconds() {
        long total = GameBalance.ROULETTE_SPIN_MS + GameBalance.ROULETTE_REVEAL_MS;
        assertTrue(total >= 3_000 && total <= 5_000);
    }
}
