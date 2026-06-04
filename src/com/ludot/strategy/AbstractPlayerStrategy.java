package com.ludot.strategy;

import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.state.InHomeStraightState;
import java.util.List;
import java.util.Optional;

// Retained for backward compatibility only. New strategies implement PlayerStrategy directly
// and use StrategyHelper for shared logic — inheritance between strategies is avoided per
// the coding guidelines ("Strategies are independent implementations").
@Deprecated
public abstract class AbstractPlayerStrategy implements PlayerStrategy {

    protected Optional<Move> findCapture(List<Move> moves) {
        return StrategyHelper.findCapture(moves);
    }

    protected Optional<Move> findBlock(List<Move> moves) {
        return StrategyHelper.findBlock(moves);
    }

    protected Optional<Move> findHomeStraight(List<Move> moves) {
        return StrategyHelper.findHomeStraight(moves);
    }

    protected Move firstOf(List<Move> moves) {
        return StrategyHelper.firstOf(moves);
    }
}
