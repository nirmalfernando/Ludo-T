package com.ludot.client;

import com.ludot.client.state.ClientState;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.BoardDto;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import com.ludot.protocol.MemberDto;
import com.ludot.protocol.Phase;
import com.ludot.protocol.Role;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.SeatDto;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.Speed;
import com.ludot.protocol.TurnInfo;
import com.ludot.protocol.Variant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoomModel and ClientState")
class RoomModelTest {

    private static final String ROOM = "R-0001";
    private static final String ME = "me";

    private static BoardDto board() {
        return new BoardDto(List.of(), List.of(), Map.of(), 0);
    }

    private static TurnInfo turn(int number) {
        return new TurnInfo(number, Colour.RED, Phase.AWAITING_ROLL, null, null);
    }

    private static ServerMessage.RoomSnapshot snapshot(long seq) {
        return new ServerMessage.RoomSnapshot(ROOM, seq, RoomMode.PLAY, Variant.LUDO_T, 1L, RoomStatus.RUNNING, ME,
                false, Speed.NORMAL,
                List.of(new SeatDto(Colour.RED, Controller.HUMAN, ME, "me", true)), board(), turn(0),
                List.of(new MemberDto(ME, "me", Role.PLAYER, true)), List.of("earlier line"), List.of(), 7L);
    }

    private static ServerMessage.StateUpdate update(long seq, int turnNumber, String text) {
        return new ServerMessage.StateUpdate(ROOM, seq, turn(turnNumber),
                List.of(new EventDto(EventType.DICE_ROLL, Colour.RED, null, null, null, text)), board(), seq);
    }

    private static ServerMessage.ChatMessage chat(long seq, String text) {
        return new ServerMessage.ChatMessage(ROOM, seq, "x", "x", Role.SPECTATOR, null, text, 0L, false);
    }

    @Test @DisplayName("a snapshot seeds everything and the next push in sequence is applied")
    void snapshotThenPush() {
        RoomModel model = new RoomModel(ROOM, ME, () -> fail("no gap expected"));
        model.apply(snapshot(10));
        assertEquals(RoomStatus.RUNNING, model.statusProperty().get());
        assertEquals(List.of("earlier line"), model.log());
        assertTrue(model.isHost());
        assertEquals(Colour.RED, model.mySeat().orElseThrow().colour());

        model.apply(update(11, 1, "Red rolled 4."));
        assertEquals(11, model.seqProperty().get());
        assertEquals(1, model.turnProperty().get().turnNumber());
        assertEquals(List.of("earlier line", "Red rolled 4."), model.log());
    }

    @Test @DisplayName("a missing push triggers a resync and is not applied")
    void gapTriggersResync() {
        AtomicInteger gaps = new AtomicInteger();
        RoomModel model = new RoomModel(ROOM, ME, gaps::incrementAndGet);
        model.apply(snapshot(10));
        model.apply(update(12, 2, "skipped 11"));
        assertEquals(1, gaps.get());
        assertEquals(10, model.seqProperty().get());
        assertEquals(1, model.log().size());
    }

    @Test @DisplayName("pushes already covered by the snapshot are ignored, never repeated")
    void staleAndDuplicatePushesIgnored() {
        RoomModel model = new RoomModel(ROOM, ME, () -> fail("no gap expected"));
        model.apply(snapshot(10));
        model.apply(update(9, 1, "old"));
        model.apply(update(10, 1, "same as snapshot"));
        model.apply(update(11, 2, "new"));
        model.apply(update(11, 2, "new again"));
        assertEquals(List.of("earlier line", "new"), model.log());
    }

    @Test @DisplayName("pushes before any snapshot are ignored")
    void nothingBeforeSnapshot() {
        RoomModel model = new RoomModel(ROOM, ME, () -> fail("no gap expected"));
        model.apply(update(1, 1, "too early"));
        assertTrue(model.log().isEmpty());
    }

    @Test @DisplayName("an offer for our turn is cleared once the game moves past it")
    void turnOfferClears() {
        RoomModel model = new RoomModel(ROOM, ME, () -> { });
        model.apply(snapshot(10));
        model.apply(new ServerMessage.YourTurn(ROOM, 3, Phase.AWAITING_ROLL, null, List.of(), null));
        assertNotNull(model.myTurnProperty().get());
        model.apply(update(11, 3, "still on 3"));
        assertNotNull(model.myTurnProperty().get());
        model.apply(update(12, 4, "moved on"));
        assertNull(model.myTurnProperty().get());
    }

    @Test @DisplayName("seats, members, room state and chat updates change the model")
    void otherPushes() {
        RoomModel model = new RoomModel(ROOM, ME, () -> fail("no gap expected"));
        model.apply(snapshot(10));
        model.apply(new ServerMessage.SeatsUpdated(ROOM, 11, List.of()));
        assertTrue(model.seats().isEmpty());
        model.apply(new ServerMessage.RoomStateChanged(ROOM, 12, RoomStatus.PAUSED, true, Speed.SLOW, "someoneElse"));
        assertEquals(RoomStatus.PAUSED, model.statusProperty().get());
        assertEquals(Speed.SLOW, model.speedProperty().get());
        assertFalse(model.isHost());
        model.apply(chat(13, "hello"));
        assertEquals("hello", model.chat().get(0).text());
        model.apply(new ServerMessage.GameOver(ROOM, 14, List.of(Colour.RED, Colour.BLUE), ROOM));
        assertEquals(Colour.RED, model.standingsProperty().get().get(0));
    }

    @Test @DisplayName("the chat and log lists are capped so a long game cannot grow without bound")
    void listsAreCapped() {
        RoomModel model = new RoomModel(ROOM, ME, () -> fail("no gap expected"));
        model.apply(snapshot(0));
        for (int i = 1; i <= 600; i++) {
            model.apply(chat(i, "m" + i));
        }
        assertEquals(300, model.chat().size());
        assertEquals("m600", model.chat().get(299).text());
    }

    @Test @DisplayName("a snapshot for a new room makes ClientState enter that room; a gap asks for that room's snapshot")
    void clientStateEntersRooms() {
        List<String> resyncs = new ArrayList<>();
        ClientState state = new ClientState(resyncs::add);
        state.apply(new ServerMessage.Welcome(ME, "me", "token"));
        assertNull(state.roomProperty().get());

        state.apply(snapshot(5));
        assertEquals(ROOM, state.roomProperty().get().roomId());

        state.apply(update(9, 1, "gap"));
        assertEquals(List.of(ROOM), resyncs);

        state.leaveRoom();
        assertNull(state.roomProperty().get());
        state.apply(update(6, 1, "ignored, no room"));
    }

    @Test @DisplayName("lobby updates and lobby chat go to the lobby lists, not to a room")
    void lobbyMessages() {
        ClientState state = new ClientState(id -> { });
        state.apply(new ServerMessage.LobbyUpdate(1, List.of(
                new com.ludot.protocol.RoomSummary(ROOM, RoomMode.PLAY, Variant.LUDO_T, RoomStatus.WAITING, 0, 0, 0, "h"))));
        assertEquals(1, state.rooms().size());
        state.apply(new ServerMessage.ChatMessage(null, 2, "a", "alice", null, null, "hi lobby", 0L, false));
        assertEquals("hi lobby", state.lobbyChat().get(0).text());
    }

    @Test @DisplayName("typed log lines: snapshot history has no type, live events keep theirs, and they mirror the plain log")
    void typedLogLines() {
        RoomModel model = new RoomModel(ROOM, ME, () -> fail("no gap expected"));
        model.apply(snapshot(10));
        model.apply(update(11, 1, "Red rolled 4."));
        assertEquals(2, model.logLines().size());
        assertNull(model.logLines().get(0).type());
        assertEquals("earlier line", model.logLines().get(0).text());
        assertEquals(EventType.DICE_ROLL, model.logLines().get(1).type());
        assertEquals(model.log(), model.logLines().stream().map(line -> line.text()).toList());
    }

    @Test @DisplayName("a fresh snapshot replaces the typed lines too, so a resync cannot leave stale lines behind")
    void snapshotResetsTypedLines() {
        RoomModel model = new RoomModel(ROOM, ME, () -> fail("no gap expected"));
        model.apply(snapshot(10));
        model.apply(update(11, 1, "Red rolled 4."));
        model.apply(snapshot(20));
        assertEquals(1, model.logLines().size());
        assertNull(model.logLines().get(0).type());
    }

    @Test @DisplayName("ClientState keeps the token from Welcome, which is what a reconnect shows the server")
    void tokenFromWelcome() {
        ClientState state = new ClientState(id -> { });
        assertEquals("", state.token());
        state.apply(new ServerMessage.Welcome("player-1", "alice", "secret-token"));
        assertEquals("secret-token", state.token());
        assertEquals("player-1", state.myId());
    }
}
