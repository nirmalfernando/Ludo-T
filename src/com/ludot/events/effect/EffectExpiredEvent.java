package com.ludot.events.effect;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Piece;

public record EffectExpiredEvent(Piece piece, String effectName) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}