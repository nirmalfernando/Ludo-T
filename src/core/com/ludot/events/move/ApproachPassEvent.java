package com.ludot.events.move;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Piece;

public record ApproachPassEvent(Piece piece, int passCount) implements GameEvent {

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
