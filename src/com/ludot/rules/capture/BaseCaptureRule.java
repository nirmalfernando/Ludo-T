package com.ludot.rules.capture;

import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.rules.GameRule;
import com.ludot.rules.MoveContext;
import com.ludot.rules.RuleResult;
import java.util.List;

public class BaseCaptureRule implements GameRule {

    
    public final boolean capture(Piece attacker, Piece victim) {
        resetPiece(victim);
        attacker.incrementCaptureCount();
        postCapture(attacker);
        return true;
    }

    
    protected void resetPiece(Piece piece) {
        piece.sendToBase();
    }

    
    protected void postCapture(Piece attacker) {}

    @Override
    public boolean isApplicable(MoveContext ctx) {
        Cell dest = ctx.destination();
        if (dest == null || !dest.isOccupied() || dest.hasBlock()) return false;
        List<Piece> occupants = dest.getPieces();
        return occupants.size() == 1 && occupants.get(0).getColour() != ctx.piece().getColour();
    }

    @Override
    public RuleResult apply(MoveContext ctx) {
        return isApplicable(ctx) ? RuleResult.success("Capture available at destination")
                                 : RuleResult.notApplicable();
    }

    @Override
    public int getPriority() { return 40; }
}
