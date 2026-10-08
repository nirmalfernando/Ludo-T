package com.ludot.model;

import com.ludot.TestBoard;
import com.ludot.effects.EnergisedEffect;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.state.OnBoardState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Piece")
class PieceTest {

    private Board board;
    private Piece piece;
    @BeforeEach void setUp() {
        board = TestBoard.create();
        piece = new Piece("RED1", Colour.RED, board);
    }
    @Test @DisplayName("new piece starts at base") void startsAtBase() {
        assertNull(piece.getPosition());
        assertInstanceOf(AtBaseState.class, piece.getState());
    }
    @Test @DisplayName("sendToBase clears position and resets state") void sendToBaseResetsState() {
        Cell cell = new Cell(0, CellType.STANDARD, null);
        cell.addPiece(piece);
        piece.setPosition(cell);
        piece.setState(new OnBoardState());
        piece.sendToBase();
        assertNull(piece.getPosition());
        assertInstanceOf(AtBaseState.class, piece.getState());
    }
    @Test @DisplayName("resetInfo clears all accumulated data") void resetInfoClearsAll() {
        piece.setDirection(Direction.CLOCKWISE);
        piece.setOriginalDirection(Direction.CLOCKWISE);
        piece.incrementCaptureCount();
        piece.setActiveEffect(new EnergisedEffect(4));
        piece.resetInfo();
        assertNull(piece.getDirection());
        assertNull(piece.getOriginalDirection());
        assertEquals(0, piece.getCaptureCount());
        assertEquals(1, piece.getEffectiveSteps(1)); // NoEffect passthrough
    }
    @Test @DisplayName("getEffectiveSteps delegates to active effect") void effectiveStepsDelegates() {
        piece.setActiveEffect(new EnergisedEffect(4));
        assertEquals(6, piece.getEffectiveSteps(3));
    }
    @Test @DisplayName("tickEffect removes expired effect") void tickEffectClearsExpired() {
        piece.setActiveEffect(new EnergisedEffect(1));
        piece.tickEffect(); // 1 tick — effect expires and gets replaced with NoEffect
        assertEquals(3, piece.getEffectiveSteps(3)); // NoEffect passes through
    }

    // ── T-1: CCW pass-through of approach cell ───────────────────────────────

    @Test @DisplayName("T-1: CCW piece increments approachPassCount when passing THROUGH approach cell mid-move")
    void ccwPassThroughApproachCellIncrementsCount() {

        Cell cell27 = board.getCell(27);
        piece.setState(new OnBoardState());
        piece.setDirection(Direction.COUNTER_CLOCKWISE);
        cell27.addPiece(piece);
        piece.setPosition(cell27);

        piece.move(5);

        assertEquals(22, piece.getPosition().getId(), "Piece should end at cell 22");
        assertEquals(1, piece.getApproachPassCount(),
                "T-1: passing through approach cell mid-move must increment approachPassCount");
    }

    @Test @DisplayName("T-1: CW piece does NOT increment approachPassCount when passing approach cell")
    void cwPieceDoesNotIncrementApproachPassCount() {

        // CW piece passing through RED approach cell should have no count change
        Cell cell21 = board.getCell(21);
        piece.setState(new OnBoardState());
        piece.setDirection(Direction.CLOCKWISE);
        cell21.addPiece(piece);
        piece.setPosition(cell21);

        piece.move(5); // 21→22→23→24(approach)→25→26

        assertEquals(26, piece.getPosition().getId());
        assertEquals(0, piece.getApproachPassCount(),
                "T-1: CW pieces must never increment approachPassCount");
    }

    }