package com.ludot.server.app;

import java.util.Objects;

/** Who a connected person is. The token lets them reconnect and keep their identity. */
public record PlayerIdentity(String id, String nickname, String token) {

    public PlayerIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nickname, "nickname");
        Objects.requireNonNull(token, "token");
    }
}
