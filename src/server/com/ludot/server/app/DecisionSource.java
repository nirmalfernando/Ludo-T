package com.ludot.server.app;

/** Who produced a recorded decision. */
public enum DecisionSource {
    HUMAN,
    AI,
    /** A forced skip applied for a human who had nothing to decide. */
    AUTO_SKIP
}
