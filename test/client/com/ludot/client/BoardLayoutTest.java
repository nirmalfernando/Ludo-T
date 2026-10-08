package com.ludot.client;

import com.ludot.client.view.BoardLayout;
import com.ludot.protocol.BoardGeometry;
import com.ludot.protocol.Colour;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BoardLayout")
class BoardLayoutTest {

    private static int distance(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    private static String key(int[] cell) {
        return cell[0] + "," + cell[1];
    }

    private static int steps(int[] a, int[] b) {
        return Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1]));
    }

    @Test @DisplayName("the 52 track cells are distinct and each is next to the next, all the way round")
    void trackIsAClosedLoop() {
        Set<String> seen = new HashSet<>();
        int diagonals = 0;
        for (int id = 0; id < BoardGeometry.TRACK_SIZE; id++) {
            assertTrue(seen.add(key(BoardLayout.trackGridCell(id))), "cell " + id + " repeats");
            int next = (id + 1) % BoardGeometry.TRACK_SIZE;
            int[] from = BoardLayout.trackGridCell(id);
            int[] to = BoardLayout.trackGridCell(next);
            assertEquals(1, steps(from, to), "cells " + id + " and " + next + " must be neighbours");
            if (distance(from, to) == 2) {
                diagonals++;
            }
        }
        assertEquals(52, seen.size());
        // like a real Ludo board, the track cuts across the four inner corners of the cross
        assertEquals(4, diagonals);
    }

    @Test @DisplayName("every track cell lies on the cross, never in a corner base or the centre")
    void trackStaysOnTheCross() {
        for (int id = 0; id < BoardGeometry.TRACK_SIZE; id++) {
            int[] c = BoardLayout.trackGridCell(id);
            boolean inColumnArm = c[0] >= 6 && c[0] <= 8;
            boolean inRowArm = c[1] >= 6 && c[1] <= 8;
            assertTrue(inColumnArm || inRowArm, "cell " + id + " is off the cross");
            assertFalse(inColumnArm && inRowArm, "cell " + id + " is in the centre");
        }
    }

    @Test @DisplayName("start cells sit where a standard Ludo board puts them")
    void startCells() {
        assertArrayEquals(new int[]{1, 6}, BoardLayout.trackGridCell(BoardGeometry.startCell(Colour.RED)));
        assertArrayEquals(new int[]{8, 1}, BoardLayout.trackGridCell(BoardGeometry.startCell(Colour.GREEN)));
        assertArrayEquals(new int[]{13, 8}, BoardLayout.trackGridCell(BoardGeometry.startCell(Colour.YELLOW)));
        assertArrayEquals(new int[]{6, 13}, BoardLayout.trackGridCell(BoardGeometry.startCell(Colour.BLUE)));
    }

    @Test @DisplayName("each home straight begins right beside its colour's approach cell and heads for the centre")
    void homeStraightsLeadFromApproach() {
        for (Colour colour : Colour.values()) {
            int[] approach = BoardLayout.trackGridCell(BoardGeometry.approachCell(colour));
            assertEquals(1, distance(approach, BoardLayout.homeGridCell(colour, 0)), colour + " straight must start at the approach");
            for (int step = 0; step < BoardGeometry.HOME_STRAIGHT_LENGTH - 1; step++) {
                assertEquals(1, distance(BoardLayout.homeGridCell(colour, step), BoardLayout.homeGridCell(colour, step + 1)));
            }
            int[] home = BoardLayout.homeGridCell(colour, BoardGeometry.HOME_STRAIGHT_LENGTH - 1);
            assertEquals(2, distance(home, new int[]{7, 7}), colour + " HOME must sit next to the centre block");
        }
    }

    @Test @DisplayName("home straight cells never overlap the track or each other")
    void homeStraightsAreDistinct() {
        Set<String> used = new HashSet<>();
        for (int id = 0; id < BoardGeometry.TRACK_SIZE; id++) {
            used.add(key(BoardLayout.trackGridCell(id)));
        }
        for (Colour colour : Colour.values()) {
            for (int step = 0; step < BoardGeometry.HOME_STRAIGHT_LENGTH; step++) {
                assertTrue(used.add(key(BoardLayout.homeGridCell(colour, step))), colour + " step " + step + " overlaps");
            }
        }
    }

    @Test @DisplayName("base slots are four distinct spots inside the colour's own corner")
    void baseSlots() {
        for (Colour colour : Colour.values()) {
            int[] origin = BoardLayout.baseOrigin(colour);
            Set<String> slots = new HashSet<>();
            for (int i = 0; i < BoardGeometry.PIECES_PER_PLAYER; i++) {
                BoardLayout.Point p = BoardLayout.base(colour, i);
                assertTrue(p.x() > origin[0] && p.x() < origin[0] + 6 && p.y() > origin[1] && p.y() < origin[1] + 6);
                slots.add(p.x() + "," + p.y());
            }
            assertEquals(4, slots.size());
        }
    }

    @Test @DisplayName("stacked pieces get different offsets; a lone piece stays centred")
    void stackOffsets() {
        assertEquals(0, BoardLayout.stackOffset(0, 1).x());
        Set<String> offsets = new HashSet<>();
        for (int slot = 0; slot < 4; slot++) {
            BoardLayout.Point p = BoardLayout.stackOffset(slot, 4);
            offsets.add(p.x() + "," + p.y());
        }
        assertEquals(4, offsets.size());
    }
}
