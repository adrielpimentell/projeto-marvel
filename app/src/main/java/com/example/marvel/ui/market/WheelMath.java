package com.example.marvel.ui.market;

final class WheelMath {

    private final float[] start;
    private final float[] sweep;

    WheelMath(int[] oddsPerMille) {
        int total = 0;
        for (int odd : oddsPerMille) total += odd;
        start = new float[oddsPerMille.length];
        sweep = new float[oddsPerMille.length];
        float angle = 0f;
        for (int i = 0; i < oddsPerMille.length; i++) {
            start[i] = angle;
            sweep[i] = total == 0 ? 0f : 360f * oddsPerMille[i] / total;
            angle += sweep[i];
        }
    }

    float startOf(int slice) {
        return start[slice];
    }

    float sweepOf(int slice) {
        return sweep[slice];
    }

    int sliceCount() {
        return start.length;
    }

    float targetRotation(int slice, float position, int fullTurns) {
        float landing = start[slice] + sweep[slice] * clamp01(position);
        return fullTurns * 360f + (360f - landing) % 360f;
    }

    int sliceUnderPointer(float rotation) {
        float angle = ((-rotation) % 360f + 360f) % 360f;
        for (int i = 0; i < start.length; i++) {
            if (sweep[i] > 0 && angle >= start[i] && angle < start[i] + sweep[i]) return i;
        }
        return start.length - 1;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
