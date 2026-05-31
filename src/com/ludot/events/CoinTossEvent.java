package com.ludot.events;

import com.ludot.model.Direction;
import com.ludot.model.Piece;

// Event record published when a coin toss determines a piece's movement direction.

public record CoinTossEvent(Piece piece, Direction direction) implements GameEvent {
}
