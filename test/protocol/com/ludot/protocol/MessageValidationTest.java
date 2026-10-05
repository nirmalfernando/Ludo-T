package com.ludot.protocol;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Message validation and immutability")
class MessageValidationTest {

    @Test @DisplayName("a request without a request id is rejected")
    void nullRequestIdRejected() {
        assertThrows(NullPointerException.class, () -> new ClientMessage.Hello(null, "alice"));
        assertThrows(NullPointerException.class, () -> new ClientMessage.Roll(null, "R-1", 1));
    }

    @Test @DisplayName("a room request without a room id is rejected, but lobby chat may omit it")
    void roomIdRequiredExceptForChat() {
        assertThrows(NullPointerException.class, () -> new ClientMessage.JoinRoom(Samples.ID, null));
        assertDoesNotThrow(() -> new ClientMessage.SendChat(Samples.ID, null, "hi"));
    }

    @Test @DisplayName("lists inside a message are copied, so later changes to the source do not leak in")
    void listsAreDefensivelyCopied() {
        List<RoomSummary> source = new ArrayList<>();
        ServerMessage.LobbyUpdate update = new ServerMessage.LobbyUpdate(1, source);
        source.add(new RoomSummary("R-1", RoomMode.PLAY, Variant.CLASSIC, RoomStatus.WAITING, 0, 4, 0, "x"));
        assertTrue(update.rooms().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> update.rooms().add(null));
    }

    @Test @DisplayName("an error response needs a code; an ok response reports success")
    void responseFactories() {
        assertThrows(NullPointerException.class, () -> ServerMessage.Response.error(Samples.ID, null, "x"));
        assertTrue(ServerMessage.Response.ok(Samples.ID).isOk());
        assertFalse(ServerMessage.Response.error(Samples.ID, ErrorCode.NOT_HOST, "x").isOk());
    }

    @Test @DisplayName("every request is a Request and room requests expose their room id")
    void requestHierarchy() {
        for (ClientMessage message : Samples.clientMessages()) {
            if (message instanceof ClientMessage.Request request) {
                assertEquals(Samples.ID, request.requestId());
            } else {
                assertInstanceOf(ClientMessage.Ping.class, message);
            }
            if (message instanceof ClientMessage.RoomRequest room) {
                assertEquals("R-0001", room.roomId());
            }
        }
    }

    @Test @DisplayName("room pushes carry strictly addressable room ids and sequence numbers")
    void roomPushesHaveSeq() {
        long seq = 120;
        for (ServerMessage message : Samples.serverMessages()) {
            if (message instanceof ServerMessage.RoomPush push && !(push instanceof ServerMessage.ChatMessage)) {
                assertEquals("R-0001", push.roomId());
                assertTrue(push.seq() >= seq);
            }
        }
    }

    @Test @DisplayName("speed delays follow the spec")
    void speedDelays() {
        assertEquals(150, Speed.FAST.delayMs());
        assertEquals(700, Speed.NORMAL.delayMs());
        assertEquals(1500, Speed.SLOW.delayMs());
    }

    @Test @DisplayName("board geometry matches the documented cells")
    void geometry() {
        assertEquals(26, BoardGeometry.startCell(Colour.RED));
        assertEquals(24, BoardGeometry.approachCell(Colour.RED));
        assertEquals(0, BoardGeometry.startCell(Colour.YELLOW));
        assertEquals(50, BoardGeometry.approachCell(Colour.YELLOW));
    }
}
