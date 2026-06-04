package com.ludot.model.state;
import com.ludot.model.Piece;

import java.util.List;
import com.ludot.command.Move;

public interface PieceState {

    
    List<Move> handleRoll(Piece piece, int roll);

    
    boolean canMove();

    
    String getStateName();
}
