package com.ludot.rules.home;

import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.rules.GameRule;
import com.ludot.rules.MoveContext;
import com.ludot.rules.RuleResult;

public class BaseHomeEntryRule implements GameRule {

    
    public final boolean canEnterHomeStraight(Piece piece) {
        return piece.canEnterHomeStraight() && meetsAdditionalRequirements(piece);
    }

    
    protected boolean meetsAdditionalRequirements(Piece piece) {
        return true;
    }

    /**
     * Rule 9: whether a piece that lands on or passes its approach cell turns into the home straight.
     * Classic always lets it; {@code approachPasses} is how many times the piece has reached the approach cell.
     */
    public boolean mayEnterHomeStraight(Piece piece, Direction direction, int approachPasses) {
        return meetsAdditionalRequirements(piece);
    }

    /** How many more captures the piece needs before it may enter the home straight (0 in Classic). */
    public int capturesNeeded(Piece piece) {
        return 0;
    }

    @Override
    public boolean isApplicable(MoveContext ctx) {
        return ctx.piece() != null && ctx.piece().canEnterHomeStraight();
    }

    @Override
    public RuleResult apply(MoveContext ctx) {
        if (!isApplicable(ctx)) {
            return RuleResult.notApplicable();
        }
        return canEnterHomeStraight(ctx.piece())
                ? RuleResult.success("Home straight entry allowed")
                : RuleResult.failure("Home straight entry denied: capture requirement not met (T-7)");
    }

    @Override
    public int getPriority() {
        return 60;
    }
}
