package com.ludot.protocol;

import java.util.Objects;

public record RoomSummary(String roomId, RoomMode mode, Variant variant, RoomStatus status, int humanSeats, int aiSeats, int spectators, String hostNickname) implements java.io.Serializable {

    public RoomSummary {
        Objects.requireNonNull(roomId, "roomId");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(hostNickname, "hostNickname");
    }
}
