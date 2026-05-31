package com.ludot.events;

import java.util.List;
import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Event record published when an entire block moves together (T-5/6).

public record BlockMoveEvent(List<Piece> block, Cell from, Cell to, int steps) implements GameEvent {
}
