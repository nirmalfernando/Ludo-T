package com.ludot.client.net;

public final class SessionExpiredException extends IllegalStateException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
