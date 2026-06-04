package com.ludot.engine;

import com.ludot.model.Board;
import com.ludot.model.Player;
import java.util.List;

/**
 * @deprecated Moved to {@link com.ludot.model.GameState}. This alias exists only so that
 *             engine-package callers (TurnManager, GameStateTest) continue to compile without
 *             modification. New code should import com.ludot.model.GameState directly.
 */
@Deprecated
public class GameState extends com.ludot.model.GameState {

    /** Constructs a deprecated engine-package alias delegating to the canonical model GameState. */
    public GameState(List<Player> players, Board board) {
        super(players, board);
    }

    /** Constructs a deprecated engine-package alias with turn-number support, delegating to model GameState. */
    public GameState(List<Player> players, Board board, int turnNumber) {
        super(players, board, turnNumber);
    }
}
