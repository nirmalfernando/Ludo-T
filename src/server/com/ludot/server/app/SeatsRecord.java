package com.ludot.server.app;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import java.util.List;

public record SeatsRecord(String roomId, List<SeatRow> seats) implements WriteOp {

    public record SeatRow(Colour colour, String playerId, Controller controller) {
    }

    public SeatsRecord {
        seats = List.copyOf(seats);
    }
}
