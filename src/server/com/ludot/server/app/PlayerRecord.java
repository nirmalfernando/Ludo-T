package com.ludot.server.app;

/** A person who connected; written once, and ignored if already stored. */
public record PlayerRecord(String id, String nickname) implements WriteOp {
}
