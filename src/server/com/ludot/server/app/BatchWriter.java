package com.ludot.server.app;

import java.util.List;

public interface BatchWriter {

    void writeAll(List<WriteOp> operations) throws PersistenceException;
}
