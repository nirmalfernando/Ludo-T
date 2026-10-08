package com.ludot.command;

import com.ludot.TestBoard;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.AtHomeState;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.state.OnBoardState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("MoveOnBoard")
class MoveOnBoardTest {

    private Board board;
    private Piece piece;
    @BeforeEach void setUp() {
        board = TestBoard.create();
        piece = new Piece("RED1", Colour.RED, board);
        Cell start = board.getStartCell(Colour.RED); // cell 26
        start.addPiece(piece);
        piece.setPosition(start);
        piece.setState(new OnBoardState());
        piece.setDirection(Direction.CLOCKWISE);
    }
        @Test @DisplayName("circular move advances piece on standard path") void circularMove() {
        new MoveOnBoard(piece, 3).execute();
        assertEquals(29, piece.getPosition().getId()); // 26 + 3 = 29
    }
    @Test @DisplayName("home-straight move uses linear index") void homeStraightLinear() {
        List<Cell> hs = board.getHomeStraight(Colour.RED);
        piece.getPosition().removePiece(piece);
        hs.get(0).addPiece(piece);
        piece.setPosition(hs.get(0));
        piece.setState(new InHomeStraightState());
        new MoveOnBoard(piece, 1).execute();
        assertEquals(hs.get(1), piece.getPosition());
    }
    @Test @DisplayName("CCW move wraps correctly across cell 0")
    void ccwWrapsAtCellZero() {
        // piece at cell 2, direction CCW, 3 steps: (2 - 3 + 52) % 52 = 51
        Cell c2 = board.getCell(2);
        piece.getPosition().removePiece(piece);
        c2.addPiece(piece); piece.setPosition(c2);
        piece.setDirection(Direction.COUNTER_CLOCKWISE);
        new MoveOnBoard(piece, 3).execute();
        assertEquals(51, piece.getPosition().getId());
    }

    @Test @DisplayName("reaching last home cell sets AtHomeState") void setsAtHomeOnLast() {
        List<Cell> hs = board.getHomeStraight(Colour.RED);
        Cell penultimate = hs.get(3); // 1 step from HOME (index 4)
        piece.getPosition().removePiece(piece);
        penultimate.addPiece(piece);
        piece.setPosition(penultimate);
        piece.setState(new InHomeStraightState());
        new MoveOnBoard(piece, 1).execute();
        assertInstanceOf(AtHomeState.class, piece.getState());
    }
}