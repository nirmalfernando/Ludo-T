package com.ludot.events;

import java.util.List;
import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Event record published when a block is forcibly broken apart (T-4).

public record BlockBreakEvent(List<Piece> block, Cell cell) implements GameEvent {
}
