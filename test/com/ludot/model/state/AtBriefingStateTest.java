package com.ludot.model.state;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("AtBriefingState (T-13)")
class AtBriefingStateTest {
    @Test @DisplayName("canMove returns false") void canMoveIsFalse() {
        assertFalse(new AtBriefingState(4).canMove());
    }
    @Test @DisplayName("transitions to OnBoardState when roundsLeft zero") void releasesAfterDuration() {
        Piece p = new Piece("R1", Colour.RED);
        AtBriefingState state = new AtBriefingState(1);
        p.setState(state);
        state.handleRoll(p, 2); // non-3 roll, roundsLeft goes 1->0, sets OnBoardState
        assertInstanceOf(OnBoardState.class, p.getState());
    }
    @Test @DisplayName("triple-3 sends piece to base") void tripleThreeSendsToBase() {
        Piece p = new Piece("R1", Colour.RED);
        AtBriefingState state = new AtBriefingState(10);
        p.setState(state);
        state.handleRoll(p, 3);
        state.handleRoll(p, 3);
        state.handleRoll(p, 3);
        assertNull(p.getPosition());
        assertInstanceOf(AtBaseState.class, p.getState());
    }
    @Test @DisplayName("triple-3 calls resetInfo before sending to base (T-9)")
    void tripleThreeResetsInfo() {
        Piece p = new Piece("R1", Colour.RED);
        p.setDirection(Direction.CLOCKWISE);
        p.incrementCaptureCount();
        p.incrementApproachPassCount();
        AtBriefingState state = new AtBriefingState(10);
        p.setState(state);
        state.handleRoll(p, 3);
        state.handleRoll(p, 3);
        state.handleRoll(p, 3);
        // resetInfo must have been called: direction null, counts zeroed
        assertNull(p.getDirection(), "direction should be null after resetInfo");
        assertEquals(0, p.getCaptureCount(), "captureCount should be 0 after resetInfo");
        assertEquals(0, p.getApproachPassCount(), "approachPassCount should be 0 after resetInfo");
        assertNull(p.getPosition(), "piece should be at base");
    }

    @Test @DisplayName("consecutive-three counter resets on non-3 roll") void counterResets() {
        Piece p = new Piece("R1", Colour.RED);
        AtBriefingState state = new AtBriefingState(10);
        p.setState(state);
        state.handleRoll(p, 3);
        state.handleRoll(p, 3);
        state.handleRoll(p, 5); // breaks streak
        state.handleRoll(p, 3);
        state.handleRoll(p, 3);
        // only 2 consecutive threes now — should NOT have gone to base
        assertInstanceOf(AtBriefingState.class, p.getState());
    }
}