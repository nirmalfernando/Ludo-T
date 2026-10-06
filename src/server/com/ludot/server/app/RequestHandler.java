package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;

/** Handles one kind of request on the room thread. It may refuse by throwing {@link RequestRejected}. */
@FunctionalInterface
interface RequestHandler {

    Reply handle(Participant from, ClientMessage.Request request);
}
