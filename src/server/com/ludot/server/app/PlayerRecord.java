package com.ludot.server.app;

public record PlayerRecord(String id, String nickname) implements WriteOp {
}
