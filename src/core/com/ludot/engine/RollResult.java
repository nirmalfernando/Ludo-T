package com.ludot.engine;

import com.ludot.command.Move;
import java.util.List;

/**
 * Outcome of the roll phase of a turn.
 * {@code turnOver} is true when no move choice follows (a triple six forfeits the turn).
 */
public record RollResult(int roll, List<Move> validMoves, boolean turnOver) {
}
