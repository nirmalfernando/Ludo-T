package com.ludot.events.dice;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Player;

public record TripleSixEvent(Player player, boolean hasBlock) implements GameEvent {

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
