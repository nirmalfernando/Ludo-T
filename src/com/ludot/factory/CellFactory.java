package com.ludot.factory;

import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;

// Factory Method class that creates typed Cell instances and initialises the full board layout.

public class CellFactory {

    private CellFactory() {
    }

    public static Cell createStandardCell(int id) {
        return null;
    }

    public static Cell createStartCell(int id, Colour colour) {
        return null;
    }

    public static Cell createApproachCell(int id, Colour colour) {
        return null;
    }

    public static Cell createHomeStraightCell(int id, Colour colour) {
        return null;
    }

    public static void initializeBoard(Board board) {
    }
}
