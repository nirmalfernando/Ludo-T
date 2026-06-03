package com.ludot.events.flow;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Player;
import java.util.List;

public record TurnOrderEvent(Player first, List<Player> order) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}
