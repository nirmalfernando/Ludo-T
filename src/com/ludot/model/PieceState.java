package com.ludot.model;

import java.util.List;
import com.ludot.command.Move;

// State interface implementing the State design pattern for a piece's lifecycle position.

public interface PieceState {

    List<Move> handleRoll(Piece piece, int roll);

    boolean canMove();

    String getStateName();
}
