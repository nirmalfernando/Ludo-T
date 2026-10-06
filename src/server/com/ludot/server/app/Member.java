package com.ludot.server.app;

/** Someone in a room: who they are and the connection they are using right now (replaced if they rejoin). */
final class Member {

    private final PlayerIdentity identity;
    private ClientSink sink;

    Member(PlayerIdentity identity, ClientSink sink) {
        this.identity = identity;
        this.sink = sink;
    }

    String id() {
        return identity.id();
    }

    String nickname() {
        return identity.nickname();
    }

    ClientSink sink() {
        return sink;
    }

    void attach(ClientSink newSink) {
        this.sink = newSink;
    }
}
