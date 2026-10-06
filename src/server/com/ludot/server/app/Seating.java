package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import java.util.Map;

/** Taking, leaving and taking back a seat. */
final class Seating {

    private final RoomState state;
    private final RoomGuards guards;
    private final Broadcaster broadcaster;
    private final RoomChat chat;
    private final TurnDriver turns;

    Seating(RoomState state, RoomGuards guards, Broadcaster broadcaster, RoomChat chat, TurnDriver turns) {
        this.state = state;
        this.guards = guards;
        this.broadcaster = broadcaster;
        this.chat = chat;
        this.turns = turns;
    }

    void register(Map<Class<?>, RequestHandler> handlers) {
        handlers.put(ClientMessage.ClaimSeat.class, (from, request) -> claim(from, (ClientMessage.ClaimSeat) request));
        handlers.put(ClientMessage.ReleaseSeat.class, this::release);
        handlers.put(ClientMessage.ReclaimSeat.class, this::reclaim);
    }

    // a claim is atomic because it is one command on the room's queue: the first sets the seat, the second sees it taken
    private Reply claim(Participant from, ClientMessage.ClaimSeat request) {
        guards.requireMember(from);
        requireSeatsOpen();
        state.seats.claim(from.identity(), request.colour());
        return Reply.ok(request).then(() -> {
            broadcaster.seats();
            broadcaster.members();
            chat.postSystem(from.identity().nickname() + " took " + request.colour());
            broadcaster.publishSummary();
        });
    }

    private Reply release(Participant from, ClientMessage.Request request) {
        guards.requireMember(from);
        requireSeatsOpen();
        if (!state.seats.releaseHeldBy(from.id())) {
            throw RoomGuards.reject(ErrorCode.NOT_A_PLAYER, "You do not hold a seat");
        }
        return Reply.ok(request).then(() -> {
            broadcaster.seats();
            broadcaster.members();
            broadcaster.publishSummary();
        });
    }

    private void requireSeatsOpen() {
        if (state.mode != RoomMode.PLAY || state.status != RoomStatus.WAITING) {
            throw RoomGuards.reject(ErrorCode.SEATS_LOCKED, "Seats can only change before a Play game starts");
        }
    }

    // takes the seat back from the AI after being away or disconnected
    private Reply reclaim(Participant from, ClientMessage.Request request) {
        guards.requireMember(from);
        SeatTable.Seat seat = state.seats.seatOf(from.id()).orElseThrow(
                () -> RoomGuards.reject(ErrorCode.NOT_A_PLAYER, "You do not hold a seat"));
        if (seat.controller() != Controller.AFK && seat.controller() != Controller.DISCONNECTED) {
            throw RoomGuards.reject(ErrorCode.NOT_A_PLAYER, "Your seat is not waiting to be reclaimed");
        }
        state.seats.setController(seat.colour(), Controller.HUMAN);
        turns.resetMisses(seat.colour());
        return Reply.ok(request).then(() -> {
            broadcaster.seats();
            turns.scheduleNext();
            broadcaster.turnRefresh();
        });
    }
}
