package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.client.state.ClientState;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.RoomList;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomSummary;
import java.util.UUID;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The lobby: the live list of rooms (no refreshing needed), a button to create one, and the lobby chat. */
public final class LobbyScreen extends BorderPane {

    private final ClientState state;
    private final Session session;
    private final Toasts toasts;
    private final TableView<RoomSummary> table = new TableView<>();
    private final Button join = new Button("Join / watch");

    public LobbyScreen(ClientState state, Session session, Toasts toasts) {
        this.state = state;
        this.session = session;
        this.toasts = toasts;
        setPadding(new Insets(14));
        setStyle(Theme.PANEL_STYLE);
        setTop(topBar());
        buildTable();
        setCenter(table);
        setRight(chatColumn());
        subscribe();
    }

    private HBox topBar() {
        Label title = new Label("Lobby");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        Label who = new Label();
        who.textProperty().bind(state.nicknameProperty().map(name -> "signed in as " + name));
        who.setStyle("-fx-text-fill: #607d8b;");
        Button create = new Button("New room…");
        create.setOnAction(event -> NewRoomDialog.show().ifPresent(this::createRoom));
        join.setDisable(true);
        join.setOnAction(event -> joinSelected());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, title, who, spacer, create, join);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(0, 0, 10, 0));
        return top;
    }

    private VBox chatColumn() {
        ChatPane chat = new ChatPane(state.lobbyChat(), state.myId(), (text, onRejected) -> session.request(
                new ClientMessage.SendChat(UUID.randomUUID(), null, text), response -> {
                    if (!response.isOk()) {
                        onRejected.accept(Session.describe(response));
                    }
                }));
        Label chatTitle = new Label("Lobby chat");
        chatTitle.setStyle("-fx-font-weight: bold;");
        VBox column = new VBox(6, chatTitle, chat);
        VBox.setVgrow(chat, Priority.ALWAYS);
        column.setPrefWidth(320);
        column.setPadding(new Insets(0, 0, 0, 12));
        return column;
    }

    private void buildTable() {
        table.setItems(state.rooms());
        table.setPlaceholder(new Label("No rooms yet: create one!"));
        table.getColumns().add(column("Room", 90, RoomSummary::roomId));
        table.getColumns().add(column("Mode", 100, r -> r.mode() == RoomMode.BOT_MATCH ? "Bot match" : "Play"));
        table.getColumns().add(column("Rules", 80, r -> r.variant().name()));
        table.getColumns().add(column("Status", 90, r -> r.status().name()));
        table.getColumns().add(column("Players", 110, r -> r.humanSeats() + " people, " + r.aiSeats() + " AI"));
        table.getColumns().add(column("Watching", 80, r -> String.valueOf(r.spectators())));
        table.getColumns().add(column("Host", 110, RoomSummary::hostNickname));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> join.setDisable(now == null));
        table.setRowFactory(view -> {
            javafx.scene.control.TableRow<RoomSummary> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    joinSelected();
                }
            });
            return row;
        });
    }

    private static TableColumn<RoomSummary, String> column(String name, double width, Function<RoomSummary, String> text) {
        TableColumn<RoomSummary, String> column = new TableColumn<>(name);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return column;
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

    private void joinSelected() {
        RoomSummary selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            session.request(new ClientMessage.JoinRoom(UUID.randomUUID(), selected.roomId()), () -> { }, toasts::error);
        }
    }
}
