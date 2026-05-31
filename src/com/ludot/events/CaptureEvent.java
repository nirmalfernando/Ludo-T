package com.ludot.events;

import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Event record published when one piece captures another.

public record CaptureEvent(Piece attacker, Piece victim, Cell cell) implements GameEvent {
}
