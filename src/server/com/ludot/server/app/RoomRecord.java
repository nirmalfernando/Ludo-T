package com.ludot.server.app;

import com.ludot.protocol.RoomMode;
import com.ludot.protocol.Variant;

public record RoomRecord(String roomId, RoomMode mode, Variant variant, long seed, String hostId) implements WriteOp {
}
