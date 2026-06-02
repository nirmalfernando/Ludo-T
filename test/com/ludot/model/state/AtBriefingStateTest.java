package com.ludot.model.state;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("AtBriefingState (T-13)")
class AtBriefingStateTest {
    @Test @DisplayName("canMove returns false") void canMoveIsFalse() {}
    @Test @DisplayName("transitions to OnBoardState when roundsLeft zero") void releasesAfterDuration() {}
    @Test @DisplayName("triple-3 sends piece to base") void tripleThreeSendsToBase() {}
    @Test @DisplayName("consecutive-three counter resets on non-3 roll") void counterResets() {}
}