package com.ludot.protocol;

import java.util.List;

public record RoomList(List<RoomSummary> rooms, long lobbySeq, List<ServerMessage.ChatMessage> recentLobbyChat) implements java.io.Serializable {

    public RoomList {
        rooms = List.copyOf(rooms);
        recentLobbyChat = List.copyOf(recentLobbyChat);
    }
}
