package com.ludot.rules;

import com.ludot.command.MoveOnBoard;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The 52-cell ring wraps round in both directions")
class RingWrapTest {

    private static int destinationOf(Colour colour, int from, Direction direction, int roll) {
        RuleScenario game = RuleScenario.ludoT();
        Piece piece = game.piece(colour.name().charAt(0) + "1", colour, from, direction);
        piece.incrementCaptureCount();
        List<com.ludot.command.Move> moves = game.movesFor(colour, roll);
        MoveOnBoard move = (MoveOnBoard) moves.get(0);
        List<com.ludot.model.Cell> route = move.getRoute();
        return route.get(route.size() - 1).getId();
    }

    @Test @DisplayName("clockwise from cell 51, three steps ends on cell 2")
    void clockwiseWrapsAtCell51() {
        assertEquals(2, destinationOf(Colour.GREEN, 51, Direction.CLOCKWISE, 3));
    }

    @Test @DisplayName("counter-clockwise from cell 2, three steps ends on cell 51")
    void counterClockwiseWrapsAtCellZero() {
        assertEquals(51, destinationOf(Colour.GREEN, 2, Direction.COUNTER_CLOCKWISE, 3));
    }
}
