package com.ludot;

import com.ludot.engine.GameEngine;
import com.ludot.factory.GameBuilder;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        long seed = parseSeed(args);
        GameEngine engine = new GameBuilder()
                .withSeed(seed)
                .withPlayers(4)
                .build();
        engine.run();
    }

    // use first arg as seed if provided, otherwise fall back to a fixed default for reproducibility
    private static long parseSeed(String[] args) {
        if (args.length > 0) {
            try {
                return Long.parseLong(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid seed '" + args[0] + "' — using default seed 42.");
            }
        }
        return 42L;
    }
}
