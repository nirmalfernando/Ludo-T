package com.ludot.server.app;

import com.ludot.protocol.ServerMessage;

/**
 * Where messages for one connected client go. Implementations must be thread-safe and must never
 * block: {@link #offer} puts the message on the connection's bounded outbound queue and reports
 * whether there was room. A false answer means the client is too slow and will be dropped.
 */
public interface ClientSink {

    boolean offer(ServerMessage message);

    void close();
}
