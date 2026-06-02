package com.ludot.events.move;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Cell;
import com.ludot.model.Piece;

public record CaptureEvent(Piece attacker, Piece victim, Cell cell) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}