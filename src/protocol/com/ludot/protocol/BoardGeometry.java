package com.ludot.protocol;

import java.util.Map;

/**
 * Board layout facts the client needs to draw the board. They duplicate the core's GameConstants on
 * purpose (the client cannot see the core); a server-side test asserts the two stay in agreement.
 */
public final class BoardGeometry {

    /** Cells on the circular track, numbered 0 to 51. */
    public static final int TRACK_SIZE = 52;

    /** Cells in each colour's home straight, the final one being HOME. */
    public static final int HOME_STRAIGHT_LENGTH = 5;

    public static final int PIECES_PER_PLAYER = 4;

    private static final Map<Colour, Integer> START_CELLS = Map.of(
            Colour.YELLOW, 0, Colour.BLUE, 13, Colour.RED, 26, Colour.GREEN, 39);

    private static final Map<Colour, Integer> APPROACH_CELLS = Map.of(
            Colour.YELLOW, 50, Colour.BLUE, 11, Colour.RED, 24, Colour.GREEN, 37);

    private BoardGeometry() {
    }

    /** Track cell where the colour's pieces enter the board. */
    public static int startCell(Colour colour) {
        return START_CELLS.get(colour);
    }

    /** Track cell from which the colour's pieces turn into their home straight. */
    public static int approachCell(Colour colour) {
        return APPROACH_CELLS.get(colour);
    }
}
