package com.ludot.events;

import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Event record published when a mystery cell teleports a piece to a new location (T-15).

public record MysteryTeleportEvent(Piece piece, Cell from, Cell to, String effectName) implements GameEvent {
}
