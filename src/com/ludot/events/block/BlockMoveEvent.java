package com.ludot.events.block;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import java.util.List;

public record BlockMoveEvent(List<Piece> block, Cell from, Cell to, int steps) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}