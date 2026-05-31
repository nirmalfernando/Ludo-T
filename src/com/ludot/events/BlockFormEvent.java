package com.ludot.events;

import java.util.List;
import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Event record published when two pieces form a block on a cell (T-3).

public record BlockFormEvent(List<Piece> block, Cell cell) implements GameEvent {
}
