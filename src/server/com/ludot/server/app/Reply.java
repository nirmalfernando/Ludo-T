package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ServerMessage.Response;
import java.io.Serializable;

/**
 * What a request handler returns: the response to send, then the work to do once it has been sent. Keeping
 * the follow-up separate is what lets a room send every response before the pushes it caused.
 */
record Reply(Response response, Runnable followUp) {

    static Reply ok(ClientMessage.Request request) {
        return new Reply(Response.ok(request.requestId()), null);
    }

    static Reply ok(ClientMessage.Request request, Serializable payload) {
        return new Reply(Response.ok(request.requestId(), payload), null);
    }

    Reply then(Runnable work) {
        return new Reply(response, work);
    }
}
