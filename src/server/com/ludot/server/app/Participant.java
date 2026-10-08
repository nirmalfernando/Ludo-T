package com.ludot.server.app;

import java.util.Objects;

public record Participant(PlayerIdentity identity, ClientSink sink) {

    public Participant {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(sink, "sink");
    }

    public String id() {
        return identity.id();
    }
}
