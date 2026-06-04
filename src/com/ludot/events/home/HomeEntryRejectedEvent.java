package com.ludot.events.home;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Piece;

public record HomeEntryRejectedEvent(Piece piece, int capturesNeeded) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}
