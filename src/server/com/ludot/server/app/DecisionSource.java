package com.ludot.server.app;

/** Who produced a recorded decision. */
public enum DecisionSource {
    HUMAN,
    AI,
    /** The person ran out of time, so their seat's own strategy decided for them. */
    TIMEOUT,
    /** A forced skip applied for a human who had nothing to decide. */
    AUTO_SKIP
}
