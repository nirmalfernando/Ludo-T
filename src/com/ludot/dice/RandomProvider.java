package com.ludot.dice;

// Strategy interface abstracting all random-number sources for testability.

public interface RandomProvider {

    int rollDice();

    boolean tossCoin();

    int getRandomCell(int bound);

    int getMysteryEffect();

    boolean getAlphaOutcome();
}
