package com.ludot.dice;

import java.util.Random;

// Production randomness backed by a seeded Random — same seed always produces the same game.
public class SeededRandomProvider implements RandomProvider {

    private final Random random;

    public SeededRandomProvider(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public int rollDice() {
        return random.nextInt(6) + 1; // 1–6
    }

    @Override
    public boolean tossCoin() {
        return random.nextBoolean(); // true = CLOCKWISE, false = COUNTER_CLOCKWISE
    }

    @Override
    public int getRandomCell(int bound) {
        return random.nextInt(bound); // 0 to bound-1
    }

    @Override
    public int getMysteryEffect() {
        return random.nextInt(6) + 1; // 1–6, maps to TeleportDestination ordinal+1
    }

    @Override
    public boolean getAlphaOutcome() {
        return random.nextBoolean(); // true = ENERGISED, false = SICK
    }
}
