package com.ludot.model.state;

import com.ludot.TestBoard;
import com.ludot.command.MoveOnBoard;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("InHomeStraightState")
class InHomeStraightStateTest {

    private Board board;
    private Piece piece;
    @BeforeEach void setUp() {
        board = TestBoard.create();
        piece = new Piece("R1", Colour.RED, board);
        // place on first home-straight cell
        List<Cell> hs = board.getHomeStraight(Colour.RED);
        hs.get(0).addPiece(piece);
        piece.setPosition(hs.get(0));
        piece.setState(new InHomeStraightState());
    }
        @Test @DisplayName("handleRoll returns MoveOnBoard when steps fit") void moveFits() {
        var moves = new InHomeStraightState().handleRoll(piece, 2);
        assertEquals(1, moves.size());
        assertInstanceOf(MoveOnBoard.class, moves.get(0));
    }
    @Test @DisplayName("handleRoll returns empty on overshoot") void overshootReturnsEmpty() {
        // HOME_STRAIGHT has 5 cells (index 0-4); from index 0, steps=5 would overshoot
        var moves = new InHomeStraightState().handleRoll(piece, 5);
        assertTrue(moves.isEmpty());
    }
    @Test @DisplayName("canMove returns true") void canMoveIsTrue() {
        assertTrue(new InHomeStraightState().canMove());
    }

    @Test @DisplayName("exact steps to HOME cell (index 4) produces a valid move")
    void exactStepsToHome() {
        // piece at index 0, 4 steps reaches the HOME cell exactly — must be allowed
        var moves = new InHomeStraightState().handleRoll(piece, 4);
        assertEquals(1, moves.size(), "Exact roll to HOME must produce one move");
        assertInstanceOf(MoveOnBoard.class, moves.get(0));
    }

    @Test @DisplayName("one step past HOME from penultimate cell is an overshoot")
    void oneOverFromPenultimateIsEmpty() {
        // move piece to index 3 (one step from HOME), then roll 2 — overshoots
        List<Cell> hs = board.getHomeStraight(Colour.RED);
        piece.getPosition().removePiece(piece);
        hs.get(3).addPiece(piece);
        piece.setPosition(hs.get(3));
        var moves = new InHomeStraightState().handleRoll(piece, 2);
        assertTrue(moves.isEmpty(), "Overshooting HOME must return empty list");
    }
}