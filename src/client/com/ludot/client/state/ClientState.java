package com.ludot.client.state;

import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ChatMessage;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * What the client knows about the server: connection status, who we are, the live room list, the
 * lobby chat and the room we are in. Every server push is applied here, on the FX thread, and the
 * views simply bind to these properties. Touch it only from the FX application thread.
 */
public final class ClientState {

    /** Connection status shown in the status bar. */
    public enum Connection {
        DISCONNECTED, CONNECTED
    }

    private static final int LOBBY_CHAT_LIMIT = 300;

    private final ObjectProperty<Connection> connection = new SimpleObjectProperty<>(Connection.DISCONNECTED);
    private final StringProperty nickname = new SimpleStringProperty("");
    private final ObservableList<RoomSummary> rooms = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> lobbyChat = FXCollections.observableArrayList();
    private final ObjectProperty<RoomModel> room = new SimpleObjectProperty<>();
    private final Consumer<String> requestSnapshot;
    private String myId = "";

    /** @param requestSnapshot called with a room id when that room's pushes have a gap */
    public ClientState(Consumer<String> requestSnapshot) {
        this.requestSnapshot = requestSnapshot;
    }

    /** Applies one message from the server. */
    public void apply(ServerMessage message) {
        if (message instanceof ServerMessage.Welcome welcome) {
            myId = welcome.playerId();
            nickname.set(welcome.nickname());
        } else if (message instanceof ServerMessage.LobbyUpdate update) {
            rooms.setAll(update.rooms());
        } else if (message instanceof ServerMessage.ChatMessage chat && chat.roomId() == null) {
            lobbyChat.add(chat);
            if (lobbyChat.size() > LOBBY_CHAT_LIMIT) {
                lobbyChat.remove(0, lobbyChat.size() - LOBBY_CHAT_LIMIT);
            }
        } else if (message instanceof ServerMessage.RoomSnapshot snapshot) {
            enterIfNew(snapshot).apply(snapshot);
        } else {
            routeToRoom(message);
        }
    }

    // a snapshot for a room we are not showing yet means we have joined it
    private RoomModel enterIfNew(ServerMessage.RoomSnapshot snapshot) {
        RoomModel current = room.get();
        if (current == null || !current.roomId().equals(snapshot.roomId())) {
            current = new RoomModel(snapshot.roomId(), myId, () -> requestSnapshot.accept(snapshot.roomId()));
            room.set(current);
        }
        return current;
    }

    private void routeToRoom(ServerMessage message) {
        RoomModel current = room.get();
        if (current == null) {
            return;
        }
        if (message instanceof ServerMessage.RoomPush push && current.roomId().equals(push.roomId())) {
            current.apply(push);
        } else if (message instanceof ServerMessage.YourTurn turn && current.roomId().equals(turn.roomId())) {
            current.apply(turn);
        }
    }

    /** Called after we leave a room. */
    public void leaveRoom() {
        room.set(null);
    }

    /** Fills the lobby from the answer to ListRooms. */
    public void showLobby(java.util.List<RoomSummary> list, java.util.List<ChatMessage> recentChat) {
        rooms.setAll(list);
        lobbyChat.setAll(recentChat);
    }

    public String myId() {
        return myId;
    }

    public ObjectProperty<Connection> connectionProperty() {
        return connection;
    }

    public StringProperty nicknameProperty() {
        return nickname;
    }

    public ObservableList<RoomSummary> rooms() {
        return rooms;
    }

    public ObservableList<ChatMessage> lobbyChat() {
        return lobbyChat;
    }

    public ObjectProperty<RoomModel> roomProperty() {
        return room;
    }
}
