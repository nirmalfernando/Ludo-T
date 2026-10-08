package com.ludot;

import com.ludot.factory.CellFactory;
import com.ludot.model.Board;

public final class TestBoard {

    private TestBoard() {
    }

    public static Board create() {
        Board board = new Board();
        CellFactory.initializeBoard(board);
        return board;
    }
}
