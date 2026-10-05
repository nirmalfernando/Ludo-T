package com.ludot.protocol;

import java.util.Objects;

/** A person in a room. */
public record MemberDto(String playerId, String nickname, Role role, boolean host) implements java.io.Serializable {

    public MemberDto {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(nickname, "nickname");
        Objects.requireNonNull(role, "role");
    }
}
