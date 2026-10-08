package com.ludot.server.app;

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
