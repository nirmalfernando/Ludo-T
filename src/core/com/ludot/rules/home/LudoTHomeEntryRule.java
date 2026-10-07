package com.ludot.rules.home;

import com.ludot.model.Direction;
import com.ludot.model.Piece;

public class LudoTHomeEntryRule extends BaseHomeEntryRule {

    
    // T-7: at least one capture is needed before the home straight may be entered
    @Override
    protected boolean meetsAdditionalRequirements(Piece piece) {
        return piece.getCaptureCount() >= 1;
    }

    // T-1: a piece travelling counter-clockwise must have reached the approach cell for the second time
    @Override
    public boolean mayEnterHomeStraight(Piece piece, Direction direction, int approachPasses) {
        boolean secondPassDone = direction != Direction.COUNTER_CLOCKWISE || approachPasses >= 2;
        return meetsAdditionalRequirements(piece) && secondPassDone;
    }

    @Override
    public int capturesNeeded(Piece piece) {
        return Math.max(1 - piece.getCaptureCount(), 0);
    }
}
