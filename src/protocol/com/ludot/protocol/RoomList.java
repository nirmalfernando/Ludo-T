package com.ludot.protocol;

import java.util.List;

/** Payload of the ListRooms response. */
public record RoomList(List<RoomSummary> rooms, long lobbySeq, List<ServerMessage.ChatMessage> recentLobbyChat) implements java.io.Serializable {

    public RoomList {
        rooms = List.copyOf(rooms);
        recentLobbyChat = List.copyOf(recentLobbyChat);
    }
}
