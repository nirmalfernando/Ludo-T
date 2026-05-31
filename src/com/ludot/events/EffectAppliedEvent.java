package com.ludot.events;

import com.ludot.model.Piece;

// Event record published when a status effect is applied to a piece (T-12/14).

public record EffectAppliedEvent(Piece piece, String effectName, int duration) implements GameEvent {
}
