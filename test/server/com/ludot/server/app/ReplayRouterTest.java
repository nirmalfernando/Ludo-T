package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.FinishedGameList;
import com.ludot.protocol.ReplayAction;
import com.ludot.protocol.ReplayEndReason;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.ServerMessage.ReplayEnded;
import com.ludot.protocol.ServerMessage.ReplayEvent;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Speed;
import com.ludot.protocol.Variant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Finished games and replays through the request router")
class ReplayRouterTest {

    private RouterKit kit;

    @AfterEach
    void close() {
        if (kit != null) {
            kit.close();
        }
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    // a finished game without decisions: enough to be listed
    private void listedOnly(String roomId, Colour winner) {
        kit.repository.record(new RoomRecord(roomId, RoomMode.BOT_MATCH, Variant.LUDO_T, 1L, "host"));
        kit.repository.record(new RoomStatusRecord(roomId, RoomStatus.FINISHED, winner, 10));
    }

    @Test @DisplayName("the list of finished games comes back newest first, eight to a page, with a 'more' flag")
    void listsFinishedGames() {
        kit = new RouterKit();
        for (int i = 1; i <= 11; i++) {
            listedOnly(String.format("R-%04d", i), Colour.RED);
        }
        kit.repository.record(new RoomRecord("R-0099", RoomMode.PLAY, Variant.LUDO_T, 1L, "host")); // never finished
        RouterKit.Client viewer = kit.hello("viewer");

        FinishedGameList first = (FinishedGameList) viewer.call(new ClientMessage.ListFinishedGames(id(), 0)).payload();
        assertEquals(8, first.games().size());
        assertTrue(first.hasMore());
        assertEquals("R-0011", first.games().get(0).gameId());

        FinishedGameList second = (FinishedGameList) viewer.call(new ClientMessage.ListFinishedGames(id(), 1)).payload();
        assertEquals(3, second.games().size());
        assertFalse(second.hasMore());
    }

    @Test @DisplayName("with nothing stored the list is simply empty")
    void emptyList() {
        kit = new RouterKit();
        FinishedGameList list = (FinishedGameList) kit.hello("viewer").call(new ClientMessage.ListFinishedGames(id(), 0)).payload();
        assertTrue(list.games().isEmpty());
    }

    @Test @DisplayName("a replay started through the router answers first, then streams, then ends COMPLETED")
    void replayThroughTheRouter() {
        kit = new RouterKit();
        ReplayFixtures.Played played = ReplayFixtures.play("R-0001", Variant.LUDO_T, 3L);
        ReplayFixtures.store(kit.repository, played);
        RouterKit.Client viewer = kit.hello("viewer");

        Response started = viewer.call(new ClientMessage.StartReplay(id(), "R-0001", Speed.FAST));
        assertTrue(started.isOk());
        String replayId = (String) started.payload();
        assertEquals(ReplayEndReason.COMPLETED, viewer.inbox.await(ReplayEnded.class, 30_000).reason());

        var events = viewer.inbox.of(ReplayEvent.class);
        assertEquals(played.data().steps().size() + 1, events.size());
        assertTrue(events.stream().allMatch(e -> e.replayId().equals(replayId)));
        assertEquals(played.finalBoard(), events.get(events.size() - 1).board());
    }

    @Test @DisplayName("an unknown game id is refused")
    void unknownGame() {
        kit = new RouterKit();
        Response refused = kit.hello("viewer").call(new ClientMessage.StartReplay(id(), "R-4040", Speed.FAST));
        assertEquals(ErrorCode.UNKNOWN_ROOM, refused.errorCode());
    }

    @Test @DisplayName("a replay can be paused and stopped from the client, and ends when the connection closes")
    void controlAndDisconnect() throws Exception {
        kit = new RouterKit(SessionConfig.defaults().withDelays(5, 5, 5));
        ReplayFixtures.store(kit.repository, ReplayFixtures.play("R-0001", Variant.LUDO_T, 3L));
        RouterKit.Client viewer = kit.hello("viewer");
        String replayId = (String) viewer.call(new ClientMessage.StartReplay(id(), "R-0001", Speed.NORMAL)).payload();
        viewer.inbox.await(ReplayEvent.class, e -> e.index() >= 2, 5_000);

        assertTrue(viewer.call(new ClientMessage.ControlReplay(id(), replayId, ReplayAction.PAUSE, null)).isOk());
        int frozen = viewer.inbox.of(ReplayEvent.class).size();
        Thread.sleep(100);
        assertEquals(frozen, viewer.inbox.of(ReplayEvent.class).size());
        assertEquals(1, kit.replays.running());

        kit.router.disconnected(viewer.session);
        assertEquals(0, kit.replays.running(), "a replay must not outlive its connection");
    }
}
