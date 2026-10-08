package com.ludot.server.app;

public class PersistenceException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
