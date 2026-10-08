package com.ludot.client;

import com.ludot.client.state.ClientState;
import com.ludot.client.state.ReplayModel;
import com.ludot.protocol.BoardDto;
import com.ludot.protocol.Colour;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import com.ludot.protocol.FinishedGame;
import com.ludot.protocol.ReplayEndReason;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.Speed;
import com.ludot.protocol.Variant;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReplayModel and how ClientState routes a replay's steps")
class ReplayModelTest {

    private static final FinishedGame GAME = new FinishedGame("R-0003", RoomMode.BOT_MATCH, Variant.LUDO_T, 1L,
            Colour.GREEN, Instant.parse("2026-10-06T12:00:00Z"), 120);

    private static BoardDto board() {
        return new BoardDto(List.of(), List.of(), Map.of(), 0);
    }

    private static ServerMessage.ReplayEvent step(String replayId, int index, EventDto... events) {
        return new ServerMessage.ReplayEvent(replayId, index, 120, List.of(events), board());
    }

    private static EventDto event(EventType type, String text) {
        return new EventDto(type, Colour.RED, null, null, null, text);
    }

    @Test @DisplayName("a new replay starts at step 0 of the game's recorded length, not ended and not paused")
    void startsClean() {
        ReplayModel model = new ReplayModel("RP-1", GAME, Speed.NORMAL);
        assertEquals(0, model.indexProperty().get());
        assertEquals(120, model.totalProperty().get());
        assertNull(model.endedProperty().get());
        assertEquals(Speed.NORMAL, model.speedProperty().get());
        assertTrue(!model.pausedProperty().get());
    }

    @Test @DisplayName("a step updates the board, progress and both logs, keeping each event's type")
    void stepUpdatesEverything() {
        ReplayModel model = new ReplayModel("RP-1", GAME, Speed.NORMAL);
        model.apply(step("RP-1", 5, event(EventType.DICE_ROLL, "Red rolled 4."), event(EventType.MOVE, "R1 moves")));
        assertEquals(5, model.indexProperty().get());
        assertEquals(List.of("Red rolled 4.", "R1 moves"), model.log());
        assertEquals(EventType.DICE_ROLL, model.logLines().get(0).type());
        assertEquals(EventType.MOVE, model.logLines().get(1).type());
        assertEquals(2, model.latestEventsProperty().get().size());
    }

    @Test @DisplayName("blank narration is not logged, and steps of another replay are ignored")
    void blankAndForeignIgnored() {
        ReplayModel model = new ReplayModel("RP-1", GAME, Speed.NORMAL);
        model.apply(step("RP-1", 1, event(EventType.TURN_START, "  ")));
        assertTrue(model.log().isEmpty());
        model.apply(step("RP-2", 9, event(EventType.MOVE, "from somewhere else")));
        assertEquals(1, model.indexProperty().get());
        assertTrue(model.log().isEmpty());
    }

    @Test @DisplayName("the log keeps only the most recent 400 lines")
    void logIsBounded() {
        ReplayModel model = new ReplayModel("RP-1", GAME, Speed.NORMAL);
        for (int i = 0; i < 450; i++) {
            model.apply(step("RP-1", i, event(EventType.MOVE, "line " + i)));
        }
        assertEquals(400, model.log().size());
        assertEquals("line 449", model.log().get(399));
        assertEquals(400, model.logLines().size());
    }

    @Test @DisplayName("the end of a replay is recorded with its reason; an end for another replay is ignored")
    void ending() {
        ReplayModel model = new ReplayModel("RP-1", GAME, Speed.NORMAL);
        model.apply(new ServerMessage.ReplayEnded("RP-2", ReplayEndReason.STOPPED));
        assertNull(model.endedProperty().get());
        model.apply(new ServerMessage.ReplayEnded("RP-1", ReplayEndReason.COMPLETED));
        assertEquals(ReplayEndReason.COMPLETED, model.endedProperty().get());
    }

    @Test @DisplayName("ClientState routes steps and the end to the current replay, and ignores them when none is open")
    void clientStateRoutes() {
        ClientState state = new ClientState(id -> { });
        state.apply(step("RP-1", 1, event(EventType.MOVE, "ignored: no replay open yet")));
        assertNull(state.replayProperty().get());

        ReplayModel model = state.beginReplay("RP-1", GAME, Speed.FAST);
        assertSame(model, state.replayProperty().get());
        state.apply(step("RP-1", 2, event(EventType.CAPTURE, "Red captures Blue")));
        assertEquals(List.of("Red captures Blue"), model.log());
        state.apply(new ServerMessage.ReplayEnded("RP-1", ReplayEndReason.COMPLETED));
        assertEquals(ReplayEndReason.COMPLETED, model.endedProperty().get());

        state.endReplay();
        assertNull(state.replayProperty().get());
        state.apply(step("RP-1", 3, event(EventType.MOVE, "after closing")));
        assertEquals(1, model.log().size());
    }
}
