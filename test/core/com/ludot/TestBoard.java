package com.ludot;

import com.ludot.factory.CellFactory;
import com.ludot.model.Board;

/** Test helper: builds a fresh, fully initialised Board so every test owns an isolated instance. */
public final class TestBoard {

    private TestBoard() {
    }

    public static Board create() {
        Board board = new Board();
        CellFactory.initializeBoard(board);
        return board;
    }
}
