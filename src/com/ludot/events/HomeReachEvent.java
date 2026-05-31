package com.ludot.events;

import com.ludot.model.Piece;

// Event record published when a piece reaches the home cell and is retired.

public record HomeReachEvent(Piece piece) implements GameEvent {
}
