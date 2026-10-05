package com.ludot.engine;

import com.ludot.command.MoveResult;

/** Outcome of the move phase: what the move did, and whether the player's turn (bonus rolls included) is over. */
public record StepResult(MoveResult moveResult, boolean turnOver) {
}
