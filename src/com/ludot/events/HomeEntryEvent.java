package com.ludot.events;

import com.ludot.model.Piece;

// Event record published when a piece enters the home straight.

public record HomeEntryEvent(Piece piece) implements GameEvent {
}
