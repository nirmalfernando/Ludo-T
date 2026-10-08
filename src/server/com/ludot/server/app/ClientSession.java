package com.ludot.server.app;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientSession {

    private final ClientSink sink;
    private final Set<String> rooms = ConcurrentHashMap.newKeySet();
    private final RequestLog requests = new RequestLog(64);
    private final AtomicBoolean disconnected = new AtomicBoolean();
    private volatile Participant participant;

    public ClientSession(ClientSink sink) {
        this.sink = sink;
    }

    public ClientSink sink() {
        return sink;
    }

    public Participant participant() {
        return participant;
    }

    public boolean isIdentified() {
        return participant != null;
    }

    void identify(PlayerIdentity identity) {
        this.participant = new Participant(identity, sink);
    }

    Set<String> rooms() {
        return rooms;
    }

    RequestLog requests() {
        return requests;
    }

    // true exactly once, for whichever thread notices the disconnect first
    boolean markDisconnected() {
        return disconnected.compareAndSet(false, true);
    }
}
