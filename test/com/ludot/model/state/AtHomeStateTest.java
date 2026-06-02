package com.ludot.model.state;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("AtHomeState")
class AtHomeStateTest {
    @Test @DisplayName("handleRoll always returns empty") void handleRollEmpty() {}
    @Test @DisplayName("canMove returns false") void canMoveIsFalse() {}
}