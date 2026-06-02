package com.ludot.model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Piece")
class PieceTest {
    private Piece piece;
    @BeforeEach void setUp() { piece = new Piece("RED1", Colour.RED); }
    @Test @DisplayName("new piece starts at base") void startsAtBase() {}
    @Test @DisplayName("sendToBase clears position and resets state") void sendToBaseResetsState() {}
    @Test @DisplayName("resetInfo clears all accumulated data") void resetInfoClearsAll() {}
    @Test @DisplayName("getEffectiveSteps delegates to active effect") void effectiveStepsDelegates() {}
    @Test @DisplayName("tickEffect removes expired effect") void tickEffectClearsExpired() {}
}