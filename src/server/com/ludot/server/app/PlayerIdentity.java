package com.ludot.server.app;

import java.util.Objects;

public record PlayerIdentity(String id, String nickname, String token) {

    public PlayerIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nickname, "nickname");
        Objects.requireNonNull(token, "token");
    }
}
