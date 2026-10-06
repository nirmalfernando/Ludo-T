package com.ludot.server.app;

import java.util.List;

/**
 * Port for a store that can write several operations atomically: all of them land or none do.
 * The asynchronous repository drains its queue into batches and hands each to one of these.
 */
public interface BatchWriter {

    /** Writes the operations in order as one transaction. */
    void writeAll(List<WriteOp> operations) throws PersistenceException;
}
