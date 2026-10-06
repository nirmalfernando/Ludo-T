package com.ludot.server.app;

/**
 * One thing to persist. The repository port takes these one at a time and the asynchronous writer
 * queues and batches them, so the set is closed: every kind of write the server makes is listed here.
 */
public sealed interface WriteOp
        permits PlayerRecord, RoomRecord, SeatsRecord, RoomStatusRecord, DecisionRecord, ChatRecord {
}
