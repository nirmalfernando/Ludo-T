package com.ludot.server.app;

public sealed interface WriteOp
        permits PlayerRecord, RoomRecord, SeatsRecord, RoomStatusRecord, DecisionRecord, ChatRecord {
}
