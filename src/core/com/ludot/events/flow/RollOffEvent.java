package com.ludot.events.flow;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Player;

public record RollOffEvent(Player player, int roll) implements GameEvent {

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
