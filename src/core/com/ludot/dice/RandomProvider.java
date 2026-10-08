package com.ludot.dice;

// Strategy pattern — swap SeededRandomProvider in production, FixedRandomProvider in tests.
public interface RandomProvider {

    int rollDice();

    boolean tossCoin();

    int getRandomCell(int bound);

    int getMysteryEffect();

    boolean getAlphaOutcome();
}
