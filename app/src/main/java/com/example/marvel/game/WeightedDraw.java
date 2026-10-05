package com.example.marvel.game;

import java.util.Random;

final class WeightedDraw {

    private WeightedDraw() {
    }

    static int index(int[] weights, Random random) {
        int total = 0;
        for (int weight : weights) total += weight;
        int roll = random.nextInt(Math.max(1, total));
        for (int i = 0; i < weights.length; i++) {
            if (roll < weights[i]) return i;
            roll -= weights[i];
        }
        return 0;
    }
}
