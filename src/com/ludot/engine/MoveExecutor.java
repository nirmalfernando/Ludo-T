package com.ludot.engine;

import java.util.List;
import com.ludot.command.Move;
import com.ludot.command.MoveResult;
import com.ludot.events.EventBus;
import com.ludot.model.Board;
import com.ludot.model.Piece;
import com.ludot.rules.RuleEngine;

// Executes validated Command objects and coordinates board state updates via the rule engine.

public class MoveExecutor {

    private final Board board;
    private final RuleEngine ruleEngine;
    private final EventBus eventBus;

    public MoveExecutor(Board board, RuleEngine ruleEngine, EventBus eventBus) {
        this.board = board;
        this.ruleEngine = ruleEngine;
        this.eventBus = eventBus;
    }

    public MoveResult execute(Move move) {
        return null;
    }

    public void moveFromBase(Piece piece) {
    }

    public void moveOnBoard(Piece piece, int steps) {
    }

    public void handleCapture(Piece attacker, Piece victim) {
    }

    public void handleBlockMovement(List<Piece> block, int steps) {
    }
}
