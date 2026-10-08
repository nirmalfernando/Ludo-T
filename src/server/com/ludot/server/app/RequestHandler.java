package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;

@FunctionalInterface
interface RequestHandler {

    Reply handle(Participant from, ClientMessage.Request request);
}
