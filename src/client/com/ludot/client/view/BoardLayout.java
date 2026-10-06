package com.ludot.client.view;

import com.ludot.protocol.BoardGeometry;
import com.ludot.protocol.Colour;
import java.util.ArrayList;
import java.util.List;

/**
 * Where everything sits on the classic 15x15 Ludo board, in cell units (a cell's centre is at column + 0.5,
 * row + 0.5). The 52 track cells run clockwise round the cross, ids increasing in the direction a
 * CLOCKWISE piece travels. Red is top-left, Green top-right, Yellow bottom-right, Blue bottom-left, which
 * puts each colour's start cell two steps after its approach cell, exactly as the game rules define them.
 */
public final class BoardLayout {

    /** A point in cell units. */
    public record Point(double x, double y) {
    }

    public static final int GRID = 15;

    // loop index 0 is the left arm's outer end on its top row; game id 26 (Red's start) is loop index 1
    private static final int ID_TO_LOOP_OFFSET = 25;
    private static final List<int[]> LOOP = buildLoop();

    private BoardLayout() {
    }

    private static List<int[]> buildLoop() {
        List<int[]> cells = new ArrayList<>();
        for (int x = 0; x <= 5; x++) {
            cells.add(new int[]{x, 6});
        }
        for (int y = 5; y >= 0; y--) {
            cells.add(new int[]{6, y});
        }
        cells.add(new int[]{7, 0});
        cells.add(new int[]{8, 0});
        for (int y = 1; y <= 5; y++) {
            cells.add(new int[]{8, y});
        }
        for (int x = 9; x <= 14; x++) {
            cells.add(new int[]{x, 6});
        }
        cells.add(new int[]{14, 7});
        cells.add(new int[]{14, 8});
        for (int x = 13; x >= 9; x--) {
            cells.add(new int[]{x, 8});
        }
        for (int y = 9; y <= 14; y++) {
            cells.add(new int[]{8, y});
        }
        cells.add(new int[]{7, 14});
        cells.add(new int[]{6, 14});
        for (int y = 13; y >= 9; y--) {
            cells.add(new int[]{6, y});
        }
        for (int x = 5; x >= 0; x--) {
            cells.add(new int[]{x, 8});
        }
        cells.add(new int[]{0, 7});
        return List.copyOf(cells);
    }

    /** Column and row of a track cell (0 to 51). */
    public static int[] trackGridCell(int cellId) {
        int index = ((cellId - ID_TO_LOOP_OFFSET) % BoardGeometry.TRACK_SIZE + BoardGeometry.TRACK_SIZE)
                % BoardGeometry.TRACK_SIZE;
        return LOOP.get(index);
    }

    /** Centre of a track cell. */
    public static Point track(int cellId) {
        int[] cell = trackGridCell(cellId);
        return new Point(cell[0] + 0.5, cell[1] + 0.5);
    }

    /** Column and row of a home-straight step: 0 is next to the approach cell, 4 is HOME. */
    public static int[] homeGridCell(Colour colour, int step) {
        return switch (colour) {
            case RED -> new int[]{1 + step, 7};
            case GREEN -> new int[]{7, 1 + step};
            case YELLOW -> new int[]{13 - step, 7};
            case BLUE -> new int[]{7, 13 - step};
        };
    }

    /** Centre of a home-straight cell. */
    public static Point homeStraight(Colour colour, int step) {
        int[] cell = homeGridCell(colour, step);
        return new Point(cell[0] + 0.5, cell[1] + 0.5);
    }

    /** Top-left corner (column, row) of a colour's 6x6 base. */
    public static int[] baseOrigin(Colour colour) {
        return switch (colour) {
            case RED -> new int[]{0, 0};
            case GREEN -> new int[]{9, 0};
            case YELLOW -> new int[]{9, 9};
            case BLUE -> new int[]{0, 9};
        };
    }

    /** Where a piece waits in its base (four slots in a 2x2 pattern). */
    public static Point base(Colour colour, int pieceIndex) {
        int[] origin = baseOrigin(colour);
        double dx = pieceIndex % 2 == 0 ? 2 : 4;
        double dy = pieceIndex < 2 ? 2 : 4;
        return new Point(origin[0] + dx, origin[1] + dy);
    }

    /** Offsets that keep several pieces on one cell apart; the first piece sits in the middle. */
    public static Point stackOffset(int slot, int occupants) {
        if (occupants <= 1) {
            return new Point(0, 0);
        }
        double d = 0.2;
        return switch (slot % 4) {
            case 0 -> new Point(-d, -d);
            case 1 -> new Point(d, -d);
            case 2 -> new Point(-d, d);
            default -> new Point(d, d);
        };
    }
}
