package com.ludot.command;

import com.ludot.model.Player;

// Command representing a no-op turn skip; always succeeds with no capture or side effects.

public class SkipMove implements Move {

    private final Player player;

    public SkipMove(Player player) {
        this.player = player;
    }

    @Override
    public MoveResult execute() {
        return new MoveResult(true, null, false, false, "Turn skipped.");
    }

    @Override
    public String describe() {
        return null;
    }
}
