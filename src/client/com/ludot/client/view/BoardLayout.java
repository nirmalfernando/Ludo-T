package com.ludot.client.view;

import com.ludot.protocol.BoardGeometry;
import com.ludot.protocol.Colour;
import java.util.ArrayList;
import java.util.List;

public final class BoardLayout {

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

    public static int[] trackGridCell(int cellId) {
        int index = ((cellId - ID_TO_LOOP_OFFSET) % BoardGeometry.TRACK_SIZE + BoardGeometry.TRACK_SIZE)
                % BoardGeometry.TRACK_SIZE;
        return LOOP.get(index);
    }

    public static Point track(int cellId) {
        int[] cell = trackGridCell(cellId);
        return new Point(cell[0] + 0.5, cell[1] + 0.5);
    }

    public static int[] homeGridCell(Colour colour, int step) {
        return switch (colour) {
            case RED -> new int[]{1 + step, 7};
            case GREEN -> new int[]{7, 1 + step};
            case YELLOW -> new int[]{13 - step, 7};
            case BLUE -> new int[]{7, 13 - step};
        };
    }

    public static Point homeStraight(Colour colour, int step) {
        int[] cell = homeGridCell(colour, step);
        return new Point(cell[0] + 0.5, cell[1] + 0.5);
    }

    public static int[] baseOrigin(Colour colour) {
        return switch (colour) {
            case RED -> new int[]{0, 0};
            case GREEN -> new int[]{9, 0};
            case YELLOW -> new int[]{9, 9};
            case BLUE -> new int[]{0, 9};
        };
    }

    public static Point base(Colour colour, int pieceIndex) {
        int[] origin = baseOrigin(colour);
        double dx = pieceIndex % 2 == 0 ? 2 : 4;
        double dy = pieceIndex < 2 ? 2 : 4;
        return new Point(origin[0] + dx, origin[1] + dy);
    }

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
