package com.ludot.engine;

import com.ludot.command.Move;
import java.util.List;

public record RollResult(int roll, List<Move> validMoves, boolean turnOver) {
}
