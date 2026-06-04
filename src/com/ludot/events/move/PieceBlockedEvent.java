package com.ludot.events.move;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import java.util.List;

public record PieceBlockedEvent(Piece blocked, Cell from, Cell intendedDest, List<Piece> blockingPieces) implements GameEvent {

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
