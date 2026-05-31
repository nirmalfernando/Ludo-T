package com.ludot.events;

import com.ludot.model.Cell;

// Event record published when an active mystery cell expires without being triggered (T-11).

public record MysteryExpireEvent(Cell cell) implements GameEvent {
}
