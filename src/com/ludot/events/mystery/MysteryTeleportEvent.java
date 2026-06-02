package com.ludot.events.mystery;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Cell;
import com.ludot.model.Piece;

public record MysteryTeleportEvent(Piece piece, Cell from, Cell to, String effectName) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}