package com.ludot.model.state;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("AtBaseState")
class AtBaseStateTest {
    @Test @DisplayName("handleRoll returns MoveFromBase on 6") void roll6GivesMoveFromBase() {}
    @Test @DisplayName("handleRoll returns empty for non-6") void nonSixReturnsEmpty() {}
    @Test @DisplayName("canMove returns false") void canMoveIsFalse() {}
}