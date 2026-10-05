package com.ludot.strategy;

import com.ludot.TestBoard;
import com.ludot.command.Move;
import com.ludot.command.SkipMove;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.GameState;
import com.ludot.model.Player;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlueStrategy — stateless cycling via GameState.getTurnNumber()")
class BlueStrategyTest {

    private Board board;

    private Player player;

    @BeforeEach void setUp() {
        board = TestBoard.create();
        player = new Player(Colour.BLUE, List.of(), (gs, r, ps, m) -> m.get(0));
    }

    
    private GameState state(int turnNumber) {
        return new GameState(List.of(player), board, turnNumber);
    }

    @Test @DisplayName("cycles through moves by turn number") void cyclesByTurnNumber() {
        BlueStrategy s = new BlueStrategy();
        Move m1 = new SkipMove(player);
        Move m2 = new SkipMove(player);
        List<Move> moves = List.of(m1, m2);
        assertSame(m1, s.decideMoves(state(0), 1, List.of(), moves)); // turnNumber 0 → index 0
        assertSame(m2, s.decideMoves(state(1), 1, List.of(), moves)); // turnNumber 1 → index 1
    }

    @Test @DisplayName("index wraps when turn number exceeds list size") void indexWrapsAround() {
        BlueStrategy s = new BlueStrategy();
        Move m1 = new SkipMove(player);
        List<Move> moves = List.of(m1);
        assertSame(m1, s.decideMoves(state(0), 1, List.of(), moves));
        assertSame(m1, s.decideMoves(state(1), 1, List.of(), moves)); // still m1 — only one option
    }

    @Test @DisplayName("cycles across three moves and wraps") void cyclesAndWraps() {
        BlueStrategy s = new BlueStrategy();
        Move m1 = new SkipMove(player);
        Move m2 = new SkipMove(player);
        Move m3 = new SkipMove(player);
        List<Move> moves = List.of(m1, m2, m3);
        assertSame(m1, s.decideMoves(state(0), 1, List.of(), moves));
        assertSame(m2, s.decideMoves(state(1), 1, List.of(), moves));
        assertSame(m3, s.decideMoves(state(2), 1, List.of(), moves));
        assertSame(m1, s.decideMoves(state(3), 1, List.of(), moves)); // wraps back
    }

    @Test @DisplayName("same turn number always returns same move (pure function)") void deterministicForSameTurnNumber() {
        BlueStrategy s = new BlueStrategy();
        Move m1 = new SkipMove(player);
        Move m2 = new SkipMove(player);
        List<Move> moves = List.of(m1, m2);
        Move first  = s.decideMoves(state(5), 3, List.of(), moves);
        Move second = s.decideMoves(state(5), 3, List.of(), moves);
        assertSame(first, second, "Same turn number must always return same move");
    }
}
