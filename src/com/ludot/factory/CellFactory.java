package com.ludot.factory;

import com.ludot.GameConstants;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Factory Method — creates typed Cell instances and wires up the complete board layout.
public class CellFactory {

    private CellFactory() {}

    public static Cell createStandardCell(int id) {
        return new Cell(id, CellType.STANDARD, null);
    }

    public static Cell createStartCell(int id, Colour colour) {
        return new Cell(id, CellType.START, colour);
    }

    public static Cell createApproachCell(int id, Colour colour) {
        return new Cell(id, CellType.APPROACH, colour);
    }

    public static Cell createHomeStraightCell(int id, Colour colour) {
        return new Cell(id, CellType.HOME_STRAIGHT, colour);
    }

    // builds the full BOARD_SIZE-cell standard path + HOME_STRAIGHT_LENGTH-cell home straights for all colours
    public static void initializeBoard(Board board) {
        List<Cell> cells = new ArrayList<>(GameConstants.BOARD_SIZE);
        for (int id = 0; id < GameConstants.BOARD_SIZE; id++) {
            cells.add(buildStandardPathCell(id));
        }

        Map<Colour, List<Cell>> homeStraights = new EnumMap<>(Colour.class);
        int hsId = GameConstants.BOARD_SIZE;
        for (Colour c : Colour.values()) {
            List<Cell> hs = new ArrayList<>(GameConstants.HOME_STRAIGHT_LENGTH);
            for (int j = 0; j < GameConstants.HOME_STRAIGHT_LENGTH - 1; j++) {
                hs.add(createHomeStraightCell(hsId++, c));
            }
            hs.add(new Cell(hsId++, CellType.HOME, c));
            homeStraights.put(c, hs);
        }

        board.initialize(cells, homeStraights);
    }

    // explicit colour mapping — avoids any dependence on Colour.values() enum order
    private static Cell buildStandardPathCell(int id) {
        return switch (id) {
            case  0 -> createStartCell(id,    Colour.YELLOW);
            case 13 -> createStartCell(id,    Colour.BLUE);
            case 26 -> createStartCell(id,    Colour.RED);
            case 39 -> createStartCell(id,    Colour.GREEN);
            case 50 -> createApproachCell(id, Colour.YELLOW);
            case 11 -> createApproachCell(id, Colour.BLUE);
            case 24 -> createApproachCell(id, Colour.RED);
            case 37 -> createApproachCell(id, Colour.GREEN);
            default -> createStandardCell(id);
        };
    }
}
