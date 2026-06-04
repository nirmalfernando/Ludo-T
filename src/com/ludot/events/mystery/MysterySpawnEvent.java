package com.ludot.events.mystery;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Cell;

public record MysterySpawnEvent(Cell cell, int roundCount) implements GameEvent {

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
