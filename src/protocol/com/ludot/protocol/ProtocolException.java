package com.ludot.protocol;

import java.io.IOException;

/** The peer sent something the protocol forbids: an oversized frame, a disallowed class or the wrong message type. */
public class ProtocolException extends IOException {

    private static final long serialVersionUID = 1L;

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
