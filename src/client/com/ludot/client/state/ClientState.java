package com.ludot.client.state;

import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ChatMessage;
import java.util.function.Consumer;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class ClientState {

    public enum Connection {
        DISCONNECTED, RECONNECTING, CONNECTED
    }

    private static final int LOBBY_CHAT_LIMIT = 300;

    private final ObjectProperty<Connection> connection = new SimpleObjectProperty<>(Connection.DISCONNECTED);
    private final LongProperty latency = new SimpleLongProperty(-1);
    private final StringProperty nickname = new SimpleStringProperty("");
    private final ObservableList<RoomSummary> rooms = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> lobbyChat = FXCollections.observableArrayList();
    private final ObjectProperty<RoomModel> room = new SimpleObjectProperty<>();
    private final ObjectProperty<ReplayModel> replay = new SimpleObjectProperty<>();
    private final Consumer<String> requestSnapshot;
    private final ClockSync clock = new ClockSync();
    private String myId = "";
    private String token = "";

    public ClientState(Consumer<String> requestSnapshot) {
        this.requestSnapshot = requestSnapshot;
    }

    public void apply(ServerMessage message) {
        if (message instanceof ServerMessage.Welcome welcome) {
            myId = welcome.playerId();
            token = welcome.token();
            nickname.set(welcome.nickname());
        } else if (message instanceof ServerMessage.Pong pong) {
            clock.update(pong.serverTimeMs(), System.currentTimeMillis());
        } else if (message instanceof ServerMessage.LobbyUpdate update) {
            rooms.setAll(update.rooms());
        } else if (message instanceof ServerMessage.ChatMessage chat && chat.roomId() == null) {
            lobbyChat.add(chat);
            if (lobbyChat.size() > LOBBY_CHAT_LIMIT) {
                lobbyChat.remove(0, lobbyChat.size() - LOBBY_CHAT_LIMIT);
            }
        } else if (message instanceof ServerMessage.RoomSnapshot snapshot) {
            enterIfNew(snapshot).apply(snapshot);
        } else if (message instanceof ServerMessage.ReplayEvent || message instanceof ServerMessage.ReplayEnded) {
            routeToReplay(message);
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

    private void routeToReplay(ServerMessage message) {
        ReplayModel current = replay.get();
        if (current == null) {
            return;
        }
        if (message instanceof ServerMessage.ReplayEvent step) {
            current.apply(step);
        } else if (message instanceof ServerMessage.ReplayEnded finish) {
            current.apply(finish);
        }
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

    public ReplayModel beginReplay(String replayId, com.ludot.protocol.FinishedGame game, com.ludot.protocol.Speed speed) {
        ReplayModel model = new ReplayModel(replayId, game, speed);
        replay.set(model);
        return model;
    }

    public void endReplay() {
        replay.set(null);
    }

    public ObjectProperty<ReplayModel> replayProperty() {
        return replay;
    }

    public void leaveRoom() {
        room.set(null);
    }

    public void showLobby(java.util.List<RoomSummary> list, java.util.List<ChatMessage> recentChat) {
        rooms.setAll(list);
        lobbyChat.setAll(recentChat);
    }

    public ClockSync clock() {
        return clock;
    }

    public String token() {
        return token;
    }

    public String myId() {
        return myId;
    }

    public LongProperty latencyProperty() {
        return latency;
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
