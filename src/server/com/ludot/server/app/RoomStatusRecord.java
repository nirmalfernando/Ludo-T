package com.ludot.server.app;

import com.ludot.protocol.Colour;
import com.ludot.protocol.RoomStatus;

// A change of the room's status; winner is null unless the game finished
public record RoomStatusRecord(String roomId, RoomStatus status, Colour winner, int totalTurns) implements WriteOp {
}
