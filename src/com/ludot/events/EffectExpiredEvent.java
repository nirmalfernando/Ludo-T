package com.ludot.events;

import com.ludot.model.Piece;

// Event record published when a status effect expires on a piece.

public record EffectExpiredEvent(Piece piece, String effectName) implements GameEvent {
}
