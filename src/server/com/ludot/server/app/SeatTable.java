package com.ludot.server.app;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.SeatDto;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The four seats of a room. Confined to the room thread, so it needs no locking; the room's command
 * queue is what makes claims atomic.
 */
final class SeatTable {

    /** One seat; playerId and nickname stay set while the seat is AFK or DISCONNECTED so it can be reclaimed. */
    record Seat(Colour colour, Controller controller, String playerId, String nickname) {

        boolean heldBy(String id) {
            return id != null && id.equals(playerId);
        }
    }

    private final Map<Colour, Seat> seats = new EnumMap<>(Colour.class);

    SeatTable() {
        for (Colour colour : Colour.values()) {
            seats.put(colour, new Seat(colour, Controller.FREE, null, null));
        }
    }

    Seat seat(Colour colour) {
        return seats.get(colour);
    }

    Optional<Seat> seatOf(String playerId) {
        return seats.values().stream().filter(seat -> seat.heldBy(playerId)).findFirst();
    }

    /** Gives the seat to the player (moving them off any seat they already hold) or throws SEAT_TAKEN. */
    void claim(PlayerIdentity player, Colour colour) {
        Seat wanted = seats.get(colour);
        if (wanted.controller() != Controller.FREE) {
            throw new RequestRejected(ErrorCode.SEAT_TAKEN, colour + " is already taken");
        }
        releaseHeldBy(player.id());
        seats.put(colour, new Seat(colour, Controller.HUMAN, player.id(), player.nickname()));
    }

    /** Frees whatever seat the player holds; returns true if there was one. */
    boolean releaseHeldBy(String playerId) {
        Optional<Seat> held = seatOf(playerId);
        held.ifPresent(seat -> seats.put(seat.colour(), new Seat(seat.colour(), Controller.FREE, null, null)));
        return held.isPresent();
    }

    void setController(Colour colour, Controller controller) {
        Seat seat = seats.get(colour);
        seats.put(colour, new Seat(colour, controller, seat.playerId(), seat.nickname()));
    }

    void fillFreeSeatsWithAi() {
        for (Seat seat : List.copyOf(seats.values())) {
            if (seat.controller() == Controller.FREE) {
                setController(seat.colour(), Controller.AI);
            }
        }
    }

    int count(Controller controller) {
        return (int) seats.values().stream().filter(seat -> seat.controller() == controller).count();
    }

    /** Seats occupied by a person, whether currently attached or not. */
    int humanSeats() {
        return (int) seats.values().stream().filter(seat -> seat.playerId() != null).count();
    }

    /** The seats as persisted when a game starts. */
    List<SeatsRecord.SeatRow> rows() {
        List<SeatsRecord.SeatRow> rows = new ArrayList<>();
        for (Seat seat : seats.values()) {
            rows.add(new SeatsRecord.SeatRow(seat.colour(), seat.playerId(), seat.controller()));
        }
        return rows;
    }

    List<SeatDto> dtos() {
        List<SeatDto> dtos = new ArrayList<>();
        for (Seat seat : seats.values()) {
            dtos.add(new SeatDto(seat.colour(), seat.controller(), seat.playerId(), seat.nickname(),
                    seat.controller() != Controller.DISCONNECTED));
        }
        return dtos;
    }
}
