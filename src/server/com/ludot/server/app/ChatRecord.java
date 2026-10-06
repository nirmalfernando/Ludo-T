package com.ludot.server.app;

/** One chat line to persist; roomId is null for the lobby channel, playerId is null for system messages. */
public record ChatRecord(String roomId, long channelSeq, String playerId, String text,
                         boolean system, long sentAt) implements WriteOp {
}
