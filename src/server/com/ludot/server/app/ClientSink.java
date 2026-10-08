package com.ludot.server.app;

import com.ludot.protocol.ServerMessage;

public interface ClientSink {

    boolean offer(ServerMessage message);

    void close();
}
