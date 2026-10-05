package com.ludot.events.move;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Player;

public record SkipTurnEvent(Player player) implements GameEvent {

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
