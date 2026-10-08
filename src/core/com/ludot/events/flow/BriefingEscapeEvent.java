package com.ludot.events.flow;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Piece;
import com.ludot.model.Player;

public record BriefingEscapeEvent(Player player, Piece piece) implements GameEvent {

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
