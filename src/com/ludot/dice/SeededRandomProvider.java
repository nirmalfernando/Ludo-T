package com.ludot.dice;

import java.util.Random;

// Concrete Strategy providing reproducible randomness via a seeded {@link Random}.

public class SeededRandomProvider implements RandomProvider {

    private final Random random;

    public SeededRandomProvider(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public int rollDice() {
        return 0;
    }

    @Override
    public boolean tossCoin() {
        return false;
    }

    @Override
    public int getRandomCell(int bound) {
        return 0;
    }

    @Override
    public int getMysteryEffect() {
        return 0;
    }

    @Override
    public boolean getAlphaOutcome() {
        return false;
    }
}
