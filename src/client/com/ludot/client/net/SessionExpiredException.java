package com.ludot.client.net;

/** The server no longer knows the token: retrying is pointless, the person has to sign in again. */
public final class SessionExpiredException extends IllegalStateException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
