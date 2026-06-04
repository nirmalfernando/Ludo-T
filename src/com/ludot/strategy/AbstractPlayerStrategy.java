package com.ludot.strategy;

import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.state.InHomeStraightState;
import java.util.List;
import java.util.Optional;

public abstract class AbstractPlayerStrategy implements PlayerStrategy {

    
    protected Optional<Move> findCapture(List<Move> moves) {
        return moves.stream().filter(m -> m instanceof CaptureMove).findFirst();
    }

    
    protected Optional<Move> findBlock(List<Move> moves) {
        return moves.stream().filter(m -> m instanceof BlockMove).findFirst();
    }

    
    protected Optional<Move> findHomeStraight(List<Move> moves) {
        return moves.stream()
                .filter(m -> m instanceof MoveOnBoard mob
                        && mob.getPiece().getState() instanceof InHomeStraightState)
                .findFirst();
    }

    
    protected Move firstOf(List<Move> moves) {
        return moves.get(0);
    }
}
