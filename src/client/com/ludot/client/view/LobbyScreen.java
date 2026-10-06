package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.client.state.ClientState;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.RoomList;
import com.ludot.protocol.RoomSummary;
import java.util.UUID;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The lobby: the live list of games as cards (no refreshing needed), a button to create one, and the lobby chat. */
public final class LobbyScreen extends StackPane {

    private static final double CHAT_WIDTH = 330;

    private final ClientState state;
    private final Session session;
    private final Toasts toasts;
    private final ListView<RoomSummary> rooms = new ListView<>();

    public LobbyScreen(ClientState state, Session session, Toasts toasts) {
        this.state = state;
        this.session = session;
        this.toasts = toasts;
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(22, 28, 22, 28));
        layout.setTop(topBar());
        buildList();
        layout.setCenter(rooms);
        layout.setRight(chatColumn());
        getChildren().addAll(new PlayfulBackground(21), layout);
        Theme.applyPlayful(this);
        subscribe();
    }

    private HBox topBar() {
        Label title = new Label("GAMES");
        title.setStyle("-fx-font-size: 40;");
        Button create = new Button("CREATE GAME");
        create.getStyleClass().add("big-button");
        create.setOnAction(event -> NewRoomDialog.show().ifPresent(this::createRoom));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(16, title, spacer, who(), create);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(0, 0, 14, 0));
        return top;
    }

    // a pawn in a colour picked from the nickname, and the nickname itself
    private HBox who() {
        String name = state.nicknameProperty().get() == null ? "" : state.nicknameProperty().get();
        Colour colour = Colour.values()[Math.floorMod(name.hashCode(), Colour.values().length)];
        Label nickname = new Label();
        nickname.textProperty().bind(state.nicknameProperty());
        nickname.setStyle("-fx-font-size: 18;");
        nickname.setPadding(new Insets(0, 4, 0, 4));
        HBox chip = new HBox(14, new PawnMascot(colour, 34), nickname);
        chip.getStyleClass().add("panel");
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(6, 18, 4, 14));
        return chip;
    }

    private VBox chatColumn() {
        ChatPane chat = new ChatPane(state.lobbyChat(), state.myId(), (text, onRejected) -> session.request(
                new ClientMessage.SendChat(UUID.randomUUID(), null, text), response -> {
                    if (!response.isOk()) {
                        onRejected.accept(Session.describe(response));
                    }
                }));
        Label chatTitle = new Label("Lobby chat");
        chatTitle.getStyleClass().add("panel-title");
        VBox column = new VBox(8, chatTitle, chat);
        VBox.setVgrow(chat, Priority.ALWAYS);
        column.getStyleClass().add("panel");
        column.setPrefWidth(CHAT_WIDTH);
        column.setMinWidth(CHAT_WIDTH);
        column.setPadding(new Insets(14, 14, 14, 14));
        BorderPane.setMargin(column, new Insets(0, 0, 0, 20));
        return column;
    }

    private void buildList() {
        rooms.setItems(state.rooms());
        rooms.setFocusTraversable(false);
        rooms.setPlaceholder(emptyState());
        rooms.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(RoomSummary room, boolean empty) {
                super.updateItem(room, empty);
                setGraphic(empty || room == null ? null : new RoomCard(room, LobbyScreen.this::open));
            }
        });
    }

    private static VBox emptyState() {
        Label line = new Label("No games yet. Create one!");
        line.getStyleClass().add("muted");
        line.setStyle("-fx-font-size: 20;");
        VBox box = new VBox(10, new PawnMascot(Colour.BLUE, 90).bob(0), line);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // asking for the list also subscribes us to live updates and to lobby chat
    private void subscribe() {
        session.request(new ClientMessage.ListRooms(UUID.randomUUID()), response -> {
            if (response.isOk() && response.payload() instanceof RoomList list) {
                state.showLobby(list.rooms(), list.recentLobbyChat());
            } else {
                toasts.error(Session.describe(response));
            }
        });
    }

    private void createRoom(ClientMessage.CreateRoom request) {
        session.request(request, () -> { }, toasts::error);
    }

    private void open(RoomSummary room) {
        session.request(new ClientMessage.JoinRoom(UUID.randomUUID(), room.roomId()), () -> { }, toasts::error);
    }
}
