package com.ludot.protocol;

// why a replay ended; DIVERGED means the re-simulation disagreed with the record
public enum ReplayEndReason {
    COMPLETED,
    STOPPED,
    DIVERGED
}
