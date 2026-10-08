package com.ludot.protocol;

import java.util.Map;

// they duplicate the core's GameConstants on purpose (the client cannot see the core)
public final class BoardGeometry {

    public static final int TRACK_SIZE = 52;

    public static final int HOME_STRAIGHT_LENGTH = 5;

    public static final int PIECES_PER_PLAYER = 4;

    private static final Map<Colour, Integer> START_CELLS = Map.of(
            Colour.YELLOW, 0, Colour.BLUE, 13, Colour.RED, 26, Colour.GREEN, 39);

    private static final Map<Colour, Integer> APPROACH_CELLS = Map.of(
            Colour.YELLOW, 50, Colour.BLUE, 11, Colour.RED, 24, Colour.GREEN, 37);

    private BoardGeometry() {
    }

    public static int startCell(Colour colour) {
        return START_CELLS.get(colour);
    }

    public static int approachCell(Colour colour) {
        return APPROACH_CELLS.get(colour);
    }
}
