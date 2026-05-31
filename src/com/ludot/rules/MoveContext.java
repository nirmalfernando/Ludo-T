package com.ludot.rules;

import com.ludot.model.Piece;
import com.ludot.model.Player;

// Immutable context object passed through the Rule Chain carrying the current move details.

public class MoveContext {

    private final Piece piece;
    private final int roll;
    private final Player player;

    public MoveContext(Piece piece, int roll, Player player) {
        this.piece = piece;
        this.roll = roll;
        this.player = player;
    }

    public Piece getPiece() {
        return piece;
    }

    public int getRoll() {
        return roll;
    }

    public Player getPlayer() {
        return player;
    }
}
