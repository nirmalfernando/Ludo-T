package com.ludot.rules.home;

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

    @Override
    public boolean isApplicable(MoveContext ctx) {
        return ctx.piece() != null && ctx.piece().canEnterHomeStraight();
    }

    @Override
    public RuleResult apply(MoveContext ctx) {
        if (!isApplicable(ctx)) return RuleResult.notApplicable();
        return canEnterHomeStraight(ctx.piece())
                ? RuleResult.success("Home straight entry allowed")
                : RuleResult.failure("Home straight entry denied: capture requirement not met (T-7)");
    }

    @Override
    public int getPriority() { return 60; }
}
