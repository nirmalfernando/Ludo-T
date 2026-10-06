package com.ludot.server.app;

/** The stored games could not be read (the database is unreachable, for instance). */
public final class HistoryException extends RuntimeException {

    public HistoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
