package com.ludot.factory;

import com.ludot.engine.GameEngine;

// Builder pattern class for constructing a fully wired GameEngine with chosen seed and player count.

public class GameBuilder {

    private long seed;
    private int playerCount = 4;

    public GameBuilder() {
    }

    public GameBuilder withSeed(long seed) {
        this.seed = seed;
        return this;
    }

    public GameBuilder withPlayers(int playerCount) {
        this.playerCount = playerCount;
        return this;
    }

    public GameEngine build() {
        return null;
    }
}
