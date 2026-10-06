package com.ludot.server.app;

import com.ludot.protocol.RoomMode;
import com.ludot.protocol.Variant;

/** A room as created: everything needed to replay its game later (seed and rules). */
public record RoomRecord(String roomId, RoomMode mode, Variant variant, long seed, String hostId) implements WriteOp {
}
