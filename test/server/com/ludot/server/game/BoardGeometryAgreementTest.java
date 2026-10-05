package com.ludot.server.game;

import com.ludot.GameConstants;
import com.ludot.protocol.BoardGeometry;
import com.ludot.protocol.Colour;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The client's copy of the board layout must never drift from the core's constants. */
@DisplayName("BoardGeometry agrees with GameConstants")
class BoardGeometryAgreementTest {

    @Test @DisplayName("track and home-straight sizes agree")
    void sizesAgree() {
        assertEquals(GameConstants.BOARD_SIZE, BoardGeometry.TRACK_SIZE);
        assertEquals(GameConstants.HOME_STRAIGHT_LENGTH, BoardGeometry.HOME_STRAIGHT_LENGTH);
    }

    @Test @DisplayName("start cells agree for every colour")
    void startCellsAgree() {
        assertEquals(GameConstants.RED_START_CELL_ID, BoardGeometry.startCell(Colour.RED));
        assertEquals(GameConstants.GREEN_START_CELL_ID, BoardGeometry.startCell(Colour.GREEN));
        assertEquals(GameConstants.YELLOW_START_CELL_ID, BoardGeometry.startCell(Colour.YELLOW));
        assertEquals(GameConstants.BLUE_START_CELL_ID, BoardGeometry.startCell(Colour.BLUE));
    }

    @Test @DisplayName("approach cells agree for every colour")
    void approachCellsAgree() {
        assertEquals(GameConstants.RED_APPROACH_CELL_ID, BoardGeometry.approachCell(Colour.RED));
        assertEquals(GameConstants.GREEN_APPROACH_CELL_ID, BoardGeometry.approachCell(Colour.GREEN));
        assertEquals(GameConstants.YELLOW_APPROACH_CELL_ID, BoardGeometry.approachCell(Colour.YELLOW));
        assertEquals(GameConstants.BLUE_APPROACH_CELL_ID, BoardGeometry.approachCell(Colour.BLUE));
    }
}
