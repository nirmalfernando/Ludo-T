package com.ludot.server.app;

public record ChatRecord(String roomId, long channelSeq, String playerId, String text,
                         boolean system, long sentAt) implements WriteOp {
}
