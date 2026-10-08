package com.ludot.rules;

import com.ludot.command.Move;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import com.ludot.command.MoveOnBoard;
import com.ludot.strategy.BlueStrategy;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("Blue strategy (2.1.4): cyclic, B1 then B2 then B3 then B4, whatever the global turn number")
class BlueCycleTest {

    @Test @DisplayName("each turn Blue moves the piece after the one it moved last")
    void cyclesThroughItsPieces() {
        RuleScenario game = RuleScenario.ludoT();
        Piece b1 = game.piece("B1", Colour.BLUE, 3, Direction.CLOCKWISE);
        Piece b2 = game.piece("B2", Colour.BLUE, 9, Direction.CLOCKWISE);
        Piece b3 = game.piece("B3", Colour.BLUE, 15, Direction.CLOCKWISE);
        Piece b4 = game.piece("B4", Colour.BLUE, 20, Direction.CLOCKWISE);
        List<Piece> pieces = game.playerOf(Colour.BLUE).getPieces();
        BlueStrategy blue = new BlueStrategy();
        List<Move> moves = game.movesFor(Colour.BLUE, 2);
        GameState state = new GameState(game.players, game.board, 7);

        for (Piece expected : List.of(b1, b2, b3, b4, b1)) {
            Move chosen = blue.decideMoves(state, 2, pieces, moves);
            assertSame(expected, ((MoveOnBoard) chosen).getPiece());
        }
    }
}
