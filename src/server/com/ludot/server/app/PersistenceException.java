package com.ludot.server.app;

/** A write to the store failed. The store may be unreachable or may have rejected the data. */
public class PersistenceException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
