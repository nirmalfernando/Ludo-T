package com.ludot.dice;

import java.util.ArrayDeque;
import java.util.Queue;

// Test-double Strategy that serves pre-loaded values from queues, enabling deterministic tests.

public class FixedRandomProvider implements RandomProvider {

    private final Queue<Integer> diceQueue;
    private final Queue<Boolean> coinQueue;
    private final Queue<Integer> cellQueue;
    private final Queue<Integer> mysteryEffectQueue;
    private final Queue<Boolean> alphaOutcomeQueue;

    public FixedRandomProvider() {
        this.diceQueue = new ArrayDeque<>();
        this.coinQueue = new ArrayDeque<>();
        this.cellQueue = new ArrayDeque<>();
        this.mysteryEffectQueue = new ArrayDeque<>();
        this.alphaOutcomeQueue = new ArrayDeque<>();
    }

    public FixedRandomProvider addDice(int... values) {
        for (int v : values) {
            diceQueue.add(v);
        }
        return this;
    }

    public FixedRandomProvider addCoins(boolean... values) {
        for (boolean v : values) {
            coinQueue.add(v);
        }
        return this;
    }

    public FixedRandomProvider addCells(int... values) {
        for (int v : values) {
            cellQueue.add(v);
        }
        return this;
    }

    public FixedRandomProvider addMysteryEffects(int... values) {
        for (int v : values) {
            mysteryEffectQueue.add(v);
        }
        return this;
    }

    public FixedRandomProvider addAlphaOutcomes(boolean... values) {
        for (boolean v : values) {
            alphaOutcomeQueue.add(v);
        }
        return this;
    }

    @Override
    public int rollDice() {
        Integer value = diceQueue.poll();
        return value != null ? value : 1;
    }

    @Override
    public boolean tossCoin() {
        Boolean value = coinQueue.poll();
        return value != null ? value : true;
    }

    @Override
    public int getRandomCell(int bound) {
        Integer value = cellQueue.poll();
        return value != null ? value : 0;
    }

    @Override
    public int getMysteryEffect() {
        Integer value = mysteryEffectQueue.poll();
        return value != null ? value : 0;
    }

    @Override
    public boolean getAlphaOutcome() {
        Boolean value = alphaOutcomeQueue.poll();
        return value != null ? value : true;
    }
}
