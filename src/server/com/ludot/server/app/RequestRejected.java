package com.ludot.server.app;

import com.ludot.protocol.ErrorCode;

/** Thrown inside a room command to refuse a request; becomes an ERROR Response with the given code. */
public final class RequestRejected extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ErrorCode code;

    public RequestRejected(ErrorCode code, String message) {
        super(message, null, false, false);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
