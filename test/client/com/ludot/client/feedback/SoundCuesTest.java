package com.ludot.client.feedback;

import com.ludot.protocol.EventType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SoundCues (which event makes which sound)")
class SoundCuesTest {

    @Test @DisplayName("the dramatic events have their own sound")
    void dramaticEvents() {
        assertEquals(Optional.of(Cue.DICE), SoundCues.forEvent(EventType.DICE_ROLL));
        assertEquals(Optional.of(Cue.CAPTURE), SoundCues.forEvent(EventType.CAPTURE));
        assertEquals(Optional.of(Cue.TELEPORT), SoundCues.forEvent(EventType.MYSTERY_TELEPORT));
        assertEquals(Optional.of(Cue.HOME), SoundCues.forEvent(EventType.HOME_REACH));
        assertEquals(Optional.of(Cue.WIN), SoundCues.forEvent(EventType.WIN));
        assertEquals(Optional.of(Cue.ERROR), SoundCues.forEvent(EventType.TRIPLE_SIX));
    }

    @Test @DisplayName("both a plain move and a block move make the step sound")
    void movesStep() {
        assertEquals(Optional.of(Cue.STEP), SoundCues.forEvent(EventType.MOVE));
        assertEquals(Optional.of(Cue.STEP), SoundCues.forEvent(EventType.BLOCK_MOVE));
    }

    @Test @DisplayName("bookkeeping events stay silent")
    void narrationIsSilent() {
        for (EventType quiet : new EventType[] {EventType.TURN_START, EventType.ROUND_END, EventType.MYSTERY_TICK,
                EventType.EFFECT_EXPIRED, EventType.TURN_ORDER, EventType.SKIP_TURN}) {
            assertTrue(SoundCues.forEvent(quiet).isEmpty(), quiet + " should not make a sound");
        }
    }

    @Test @DisplayName("a roll then a move gives the rattle and then the step, in order")
    void batchKeepsOrder() {
        assertEquals(List.of(Cue.DICE, Cue.STEP), SoundCues.forEvents(List.of(EventType.DICE_ROLL, EventType.MOVE)));
    }

    @Test @DisplayName("a capture swallows the step sound of the move that caused it")
    void captureDropsStep() {
        assertEquals(List.of(Cue.DICE, Cue.CAPTURE),
                SoundCues.forEvents(List.of(EventType.DICE_ROLL, EventType.MOVE, EventType.CAPTURE)));
    }

    @Test @DisplayName("reaching home swallows the step sound too")
    void homeDropsStep() {
        assertEquals(List.of(Cue.HOME), SoundCues.forEvents(List.of(EventType.MOVE, EventType.HOME_REACH)));
    }

    @Test @DisplayName("an update with nothing to hear gives no sounds")
    void emptyBatch() {
        assertTrue(SoundCues.forEvents(List.of()).isEmpty());
        assertTrue(SoundCues.forEvents(List.of(EventType.TURN_START, EventType.ROUND_END)).isEmpty());
    }
}
