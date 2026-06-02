package com.ludot.model;
import com.ludot.effects.EnergisedEffect;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.state.OnBoardState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Piece")
class PieceTest {
    private Piece piece;
    @BeforeEach void setUp() { piece = new Piece("RED1", Colour.RED); }
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
}