package com.ludot.command;

import com.ludot.model.Player;

public class SkipMove implements Move {

    private final Player player;

    
    public SkipMove(Player player) {
        this.player = player;
    }

    @Override
    public MoveResult execute() {
        return new MoveResult(true, null, false, false, describe());
    }

    @Override
    public String describe() {
        return player.getColour() + " has no valid moves — turn skipped";
    }
}
