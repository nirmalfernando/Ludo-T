package com.ludot.events.effect;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Piece;

public record EffectAppliedEvent(Piece piece, String effectName, int duration) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}