package com.ludot.server.app;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import java.util.List;

/** Who held each seat when the game started; replaces any earlier rows for the room. */
public record SeatsRecord(String roomId, List<SeatRow> seats) implements WriteOp {

    /** One seat; playerId is null for an AI seat. */
    public record SeatRow(Colour colour, String playerId, Controller controller) {
    }

    public SeatsRecord {
        seats = List.copyOf(seats);
    }
}
