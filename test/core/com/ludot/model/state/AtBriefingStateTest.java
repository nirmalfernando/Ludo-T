package com.ludot.model.state;

import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("AtBriefingState (T-13)")
class AtBriefingStateTest {

    private Board board;
    @Test @DisplayName("canMove returns false") void canMoveIsFalse() {
        assertFalse(new AtBriefingState(4).canMove());
    }
    @Test @DisplayName("T-13: the briefing lasts four ROUNDS, not four rolls: bonus rolls do not shorten it")
    void releasesAfterFourRounds() {
        Piece p = new Piece("R1", Colour.RED, board);
        AtBriefingState state = new AtBriefingState(4);
        p.setState(state);
        for (int roll = 0; roll < 6; roll++) {
            assertTrue(state.handleRoll(p, 2).isEmpty(), "locked whatever is rolled");
            state.registerRoll(p, 2);
        }
        assertInstanceOf(AtBriefingState.class, p.getState(), "rolls alone never end the briefing");
        for (int round = 0; round < 3; round++) {
            state.tickRound(p);
            assertInstanceOf(AtBriefingState.class, p.getState());
        }
        state.tickRound(p);
        assertInstanceOf(OnBoardState.class, p.getState(), "free after the fourth round");
    }

    @Test @DisplayName("asking for a briefed piece's moves does not change it") void queryDoesNotMutate() {
        Piece p = new Piece("R1", Colour.RED, board);
        AtBriefingState state = new AtBriefingState(2);
        p.setState(state);
        for (int i = 0; i < 10; i++) {
            state.handleRoll(p, 3);
        }
        assertInstanceOf(AtBriefingState.class, p.getState());
        assertEquals(2, state.getRoundsLeft());
    }
    @Test @DisplayName("triple-3 sends piece to base") void tripleThreeSendsToBase() {
        Piece p = new Piece("R1", Colour.RED, board);
        AtBriefingState state = new AtBriefingState(10);
        p.setState(state);
        state.registerRoll(p, 3);
        state.registerRoll(p, 3);
        assertTrue(state.registerRoll(p, 3), "the third 3 in a row frees the piece");
        assertNull(p.getPosition());
        assertInstanceOf(AtBaseState.class, p.getState());
    }
    @Test @DisplayName("triple-3 calls resetInfo before sending to base (T-9)")
    void tripleThreeResetsInfo() {
        Piece p = new Piece("R1", Colour.RED, board);
        p.setDirection(Direction.CLOCKWISE);
        p.incrementCaptureCount();
        p.incrementApproachPassCount();
        AtBriefingState state = new AtBriefingState(10);
        p.setState(state);
        state.registerRoll(p, 3);
        state.registerRoll(p, 3);
        state.registerRoll(p, 3);
        // resetInfo must have been called: direction null, counts zeroed
        assertNull(p.getDirection(), "direction should be null after resetInfo");
        assertEquals(0, p.getCaptureCount(), "captureCount should be 0 after resetInfo");
        assertEquals(0, p.getApproachPassCount(), "approachPassCount should be 0 after resetInfo");
        assertNull(p.getPosition(), "piece should be at base");
    }

    @Test @DisplayName("consecutive-three counter resets on non-3 roll") void counterResets() {
        Piece p = new Piece("R1", Colour.RED, board);
        AtBriefingState state = new AtBriefingState(10);
        p.setState(state);
        state.registerRoll(p, 3);
        state.registerRoll(p, 3);
        state.registerRoll(p, 5); // breaks streak
        state.registerRoll(p, 3);
        state.registerRoll(p, 3);
        // only 2 consecutive threes now — should NOT have gone to base
        assertInstanceOf(AtBriefingState.class, p.getState());
    }
}