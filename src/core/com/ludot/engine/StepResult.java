package com.ludot.engine;

import com.ludot.command.MoveResult;

public record StepResult(MoveResult moveResult, boolean turnOver) {
}
