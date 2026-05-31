package com.ludot.events;

import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Event record published when a piece moves from one cell to another.

public record MoveEvent(Piece piece, Cell from, Cell to, int steps) implements GameEvent {
}
