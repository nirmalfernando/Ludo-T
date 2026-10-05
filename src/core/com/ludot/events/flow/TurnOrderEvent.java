package com.ludot.events.flow;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.util.List;
import java.util.Map;

public record TurnOrderEvent(Player first, List<Player> order, Map<Colour, Integer> rollResults) implements GameEvent {

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
