package com.ludot.client;

import com.ludot.client.view.RoomPresentation;
import com.ludot.client.view.RoomPresentation.Action;
import com.ludot.client.view.RoomPresentation.SeatState;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.Variant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("RoomPresentation (how a room looks on its lobby card)")
class RoomPresentationTest {

    private static RoomSummary room(RoomMode mode, RoomStatus status, int people, int ai, int watching) {
        return new RoomSummary("R-0001", mode, Variant.LUDO_T, status, people, ai, watching, "host");
    }

    @Test @DisplayName("a play room that has not started offers JOIN")
    void waitingPlayRoomIsJoinable() {
        assertEquals(Action.JOIN, RoomPresentation.action(room(RoomMode.PLAY, RoomStatus.WAITING, 1, 0, 0)));
    }

    @Test @DisplayName("a running play room, a bot match and a finished room can only be watched")
    void everythingElseIsWatchOnly() {
        assertEquals(Action.WATCH, RoomPresentation.action(room(RoomMode.PLAY, RoomStatus.RUNNING, 2, 2, 0)));
        assertEquals(Action.WATCH, RoomPresentation.action(room(RoomMode.BOT_MATCH, RoomStatus.WAITING, 0, 4, 0)));
        assertEquals(Action.WATCH, RoomPresentation.action(room(RoomMode.PLAY, RoomStatus.FINISHED, 2, 2, 3)));
    }

    @Test @DisplayName("the seat strip lists people first, then AI, then the free seats")
    void seatStripOrder() {
        assertEquals(List.of(SeatState.PERSON, SeatState.AI, SeatState.AI, SeatState.FREE),
                RoomPresentation.seats(room(RoomMode.PLAY, RoomStatus.RUNNING, 1, 2, 0)));
        assertEquals(List.of(SeatState.FREE, SeatState.FREE, SeatState.FREE, SeatState.FREE),
                RoomPresentation.seats(room(RoomMode.PLAY, RoomStatus.WAITING, 0, 0, 0)));
        assertEquals(List.of(SeatState.AI, SeatState.AI, SeatState.AI, SeatState.AI),
                RoomPresentation.seats(room(RoomMode.BOT_MATCH, RoomStatus.RUNNING, 0, 4, 0)));
    }

    @Test @DisplayName("the strip always has exactly four seats, even for odd counts")
    void alwaysFourSeats() {
        assertEquals(4, RoomPresentation.seats(room(RoomMode.PLAY, RoomStatus.RUNNING, 4, 3, 0)).size());
    }

    @Test @DisplayName("every status has a label and its own badge colour (green waiting, orange running)")
    void statusBadges() {
        assertEquals("WAITING", RoomPresentation.statusLabel(RoomStatus.WAITING));
        assertEquals("#2eb872", RoomPresentation.statusColour(RoomStatus.WAITING));
        assertEquals("#ff9f1c", RoomPresentation.statusColour(RoomStatus.RUNNING));
        assertNotEquals(RoomPresentation.statusColour(RoomStatus.RUNNING), RoomPresentation.statusColour(RoomStatus.PAUSED));
        assertEquals(RoomPresentation.statusColour(RoomStatus.FINISHED), RoomPresentation.statusColour(RoomStatus.CLOSED));
    }

    @Test @DisplayName("mode, rules and button labels read naturally")
    void labels() {
        assertEquals("BOT MATCH", RoomPresentation.modeLabel(RoomMode.BOT_MATCH));
        assertEquals("PLAY", RoomPresentation.modeLabel(RoomMode.PLAY));
        assertEquals("LUDO-T", RoomPresentation.rulesLabel(Variant.LUDO_T));
        assertEquals("CLASSIC", RoomPresentation.rulesLabel(Variant.CLASSIC));
        assertEquals("JOIN", RoomPresentation.actionLabel(Action.JOIN));
        assertEquals("WATCH", RoomPresentation.actionLabel(Action.WATCH));
    }

    @Test @DisplayName("people and watcher texts handle singular, plural and none")
    void texts() {
        assertEquals("1 Person, 0 AI", RoomPresentation.peopleText(room(RoomMode.PLAY, RoomStatus.WAITING, 1, 0, 0)));
        assertEquals("2 People, 2 AI", RoomPresentation.peopleText(room(RoomMode.PLAY, RoomStatus.RUNNING, 2, 2, 0)));
        assertEquals("Nobody Watching", RoomPresentation.watchersText(0));
        assertEquals("3 Watching", RoomPresentation.watchersText(3));
    }
}
