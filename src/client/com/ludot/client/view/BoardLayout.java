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

    // the loop as twelve straight runs, clockwise from the left arm's outer end; the track cuts across the four
    // inner corners of the cross, so some runs begin diagonally next to where the last one ended
    private static List<int[]> buildLoop() {
        List<int[]> cells = new ArrayList<>();
        addRun(cells, 0, 6, 5, 6);      // left arm, top row, heading in
        addRun(cells, 6, 5, 6, 0);      // up the top arm's left column
        addRun(cells, 7, 0, 8, 0);      // across the top end
        addRun(cells, 8, 1, 8, 5);      // down the top arm's right column
        addRun(cells, 9, 6, 14, 6);     // right arm, top row, heading out
        addRun(cells, 14, 7, 14, 8);    // across the right end
        addRun(cells, 13, 8, 9, 8);     // right arm, bottom row, heading in
        addRun(cells, 8, 9, 8, 14);     // down the bottom arm's right column
        addRun(cells, 7, 14, 6, 14);    // across the bottom end
        addRun(cells, 6, 13, 6, 9);     // up the bottom arm's left column
        addRun(cells, 5, 8, 0, 8);      // left arm, bottom row, heading out
        addRun(cells, 0, 7, 0, 7);      // the last cell, closing the loop
        return List.copyOf(cells);
    }

    // appends the straight run of cells from (fromX, fromY) to (toX, toY), both included
    private static void addRun(List<int[]> cells, int fromX, int fromY, int toX, int toY) {
        int dx = Integer.signum(toX - fromX);
        int dy = Integer.signum(toY - fromY);
        int length = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
        for (int i = 0; i <= length; i++) {
            cells.add(new int[]{fromX + dx * i, fromY + dy * i});
        }
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
