package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.FinishedGame;
import com.ludot.protocol.FinishedGameList;
import com.ludot.protocol.Speed;
import java.util.UUID;
import java.util.function.BiConsumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public final class FinishedGamesScreen extends StackPane {

    private final Session session;
    private final Toasts toasts;
    private final BiConsumer<String, FinishedGame> onReplay;
    private final ObservableList<FinishedGame> games = FXCollections.observableArrayList();
    private final ListView<FinishedGame> list = new ListView<>(games);
    private final Label placeholder = new Label("Loading...");
    private final Button more = new Button("Load More");
    private int nextPage;

    public FinishedGamesScreen(Session session, Toasts toasts, Runnable onBack, BiConsumer<String, FinishedGame> onReplay) {
        this.session = session;
        this.toasts = toasts;
        this.onReplay = onReplay;
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(22, 28, 22, 28));
        layout.setTop(topBar(onBack));
        buildList();
        layout.setCenter(list);
        more.getStyleClass().add("ghost-button");
        more.setVisible(false);
        more.setManaged(false);
        more.setOnAction(event -> loadNextPage());
        HBox bottom = new HBox(more);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        layout.setBottom(bottom);
        getChildren().addAll(new PlayfulBackground(44), layout);
        Theme.applyPlayful(this);
        loadNextPage();
    }

    private static HBox topBar(Runnable onBack) {
        Label title = new Label("FINISHED GAMES");
        title.setStyle("-fx-font-size: 40;");
        Button back = new Button("Back to Lobby");
        back.getStyleClass().add("ghost-button");
        back.setOnAction(event -> onBack.run());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(16, title, spacer, back);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(0, 0, 14, 0));
        return top;
    }

    private void buildList() {
        list.setFocusTraversable(false);
        list.getStyleClass().add("room-list");
        placeholder.getStyleClass().add("muted");
        placeholder.setStyle("-fx-font-size: 20;");
        VBox empty = new VBox(10, new PawnMascot(com.ludot.protocol.Colour.BLUE, 90).bob(0), placeholder);
        empty.setAlignment(Pos.CENTER);
        list.setPlaceholder(empty);
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(FinishedGame game, boolean isEmpty) {
                super.updateItem(game, isEmpty);
                setGraphic(isEmpty || game == null ? null : new FinishedGameCard(game, FinishedGamesScreen.this::startReplay));
            }
        });
    }

    private void loadNextPage() {
        more.setDisable(true);
        session.request(new ClientMessage.ListFinishedGames(UUID.randomUUID(), nextPage), response -> {
            more.setDisable(false);
            if (response.isOk() && response.payload() instanceof FinishedGameList page) {
                games.addAll(page.games());
                nextPage = page.page() + 1;
                more.setVisible(page.hasMore());
                more.setManaged(page.hasMore());
                placeholder.setText("No finished games yet. Let a bot match run to the end, then come back!");
            } else {
                placeholder.setText("The finished games could not be loaded.");
                toasts.error(Session.describe(response));
            }
        });
    }

    private void startReplay(FinishedGame game) {
        session.request(new ClientMessage.StartReplay(UUID.randomUUID(), game.gameId(), Speed.NORMAL), response -> {
            if (response.isOk() && response.payload() instanceof String replayId) {
                onReplay.accept(replayId, game);
            } else {
                toasts.error(Session.describe(response));
            }
        });
    }
}
