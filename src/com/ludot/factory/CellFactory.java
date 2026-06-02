package com.ludot.factory;

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

    // standard-path positions for each colour (start, approach)
    private static final int[] STARTS    = {0,  13, 26, 39}; // YELLOW,BLUE,RED,GREEN
    private static final int[] APPROACHES = {50, 11, 24, 37};

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

    // builds the full 52-cell standard path + 5-cell home straights for all colours
    public static void initializeBoard(Board board) {
        Colour[] colours = Colour.values(); // YELLOW, BLUE, RED, GREEN (enum declaration order)

        // standard 52-cell ring
        List<Cell> cells = new ArrayList<>(52);
        for (int id = 0; id < 52; id++) {
            cells.add(buildStandardPathCell(id, colours));
        }

        // home straights — 4 HOME_STRAIGHT cells then 1 HOME cell per colour
        Map<Colour, List<Cell>> homeStraights = new EnumMap<>(Colour.class);
        int hsId = 52;
        for (Colour c : colours) {
            List<Cell> hs = new ArrayList<>(5);
            for (int j = 0; j < 4; j++) {
                hs.add(createHomeStraightCell(hsId++, c));
            }
            hs.add(new Cell(hsId++, CellType.HOME, c)); // last cell is HOME
            homeStraights.put(c, hs);
        }

        board.initialize(cells, homeStraights);
    }

    private static Cell buildStandardPathCell(int id, Colour[] colours) {
        for (int i = 0; i < colours.length; i++) {
            if (id == STARTS[i])    return createStartCell(id, colours[i]);
            if (id == APPROACHES[i]) return createApproachCell(id, colours[i]);
        }
        return createStandardCell(id);
    }
}
