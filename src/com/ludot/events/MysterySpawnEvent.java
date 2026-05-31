package com.ludot.events;

import com.ludot.model.Cell;

// Event record published when a mystery cell spawns on the board (T-10).

public record MysterySpawnEvent(Cell cell, int roundCount) implements GameEvent {
}
