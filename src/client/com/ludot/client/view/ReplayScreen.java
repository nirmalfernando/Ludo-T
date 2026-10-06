package com.ludot.client.view;

import com.ludot.client.feedback.Feedback;
import com.ludot.client.net.Session;
import com.ludot.client.state.ReplayModel;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import com.ludot.protocol.ReplayAction;
import java.util.UUID;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** A finished game played again: the board moves on its own, with the narration, a filtered log and playback controls. */
public final class ReplayScreen extends StackPane {

    private static final double LEFT_WIDTH = 245;
    private static final double RIGHT_WIDTH = 310;

    private final ReplayModel model;
    private final Session session;
    private final BoardView board = new BoardView();
    private final VBox endCard = new VBox(12);
    private final Label endText = new Label();

    /** @param onClose leaves the replay (the replay is stopped on the server first) */
    public ReplayScreen(ReplayModel model, Session session, Toasts toasts, Runnable onClose) {
        this.model = model;
        this.session = session;
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(14, 12, 10, 12));
        layout.setTop(topBar(onClose));
        layout.setLeft(leftColumn(toasts));
        layout.setCenter(boardArea(onClose));
        layout.setRight(rightColumn());
        getChildren().addAll(new PlayfulBackground(55), layout);
        Theme.applyPlayful(this);
        wire();
    }

    private HBox topBar(Runnable onClose) {
        Label id = new Label("Replay " + model.game().gameId());
        id.getStyleClass().add("room-id");
        HBox heading = new HBox(10, id, Chips.chip(RoomPresentation.rulesLabel(model.game().variant()), Chips.RULES));
        heading.setAlignment(Pos.CENTER_LEFT);
        Button stop = new Button("Stop Replay");
        stop.getStyleClass().add("ghost-button");
        stop.setOnAction(event -> close(onClose));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(14, heading, spacer, new FeedbackToggles(), stop);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));
        return bar;
    }

    private VBox leftColumn(Toasts toasts) {
        VBox left = new VBox(12, new ReplayControls(model, session, toasts), new NarrationPane(model.log()));
        left.setPrefWidth(LEFT_WIDTH);
        left.setPadding(new Insets(0, 10, 0, 0));
        return left;
    }

    private VBox rightColumn() {
        Label title = new Label("Event Log");
        title.getStyleClass().add("panel-title");
        VBox box = new VBox(8, title, new EventLogPane(model.logLines()));
        VBox.setVgrow(box.getChildren().get(1), Priority.ALWAYS);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(14));
        box.setPrefWidth(RIGHT_WIDTH);
        BorderPane.setMargin(box, new Insets(0, 0, 0, 10));
        return box;
    }

    private StackPane boardArea(Runnable onClose) {
        endCard.getStyleClass().add("panel");
        endCard.setAlignment(Pos.CENTER);
        endCard.setPadding(new Insets(24, 40, 24, 40));
        endCard.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        endCard.setVisible(false);
        endText.setStyle("-fx-font-size: 26;");
        endText.setWrapText(true);
        Button back = new Button("Back to Finished Games");
        back.getStyleClass().add("big-button");
        back.setOnAction(event -> close(onClose));
        endCard.getChildren().addAll(endText, back);
        StackPane area = new StackPane(board, endCard);
        area.getStyleClass().add("board-frame");
        area.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return area;
    }

    private void wire() {
        model.boardProperty().addListener((obs, old, now) -> {
            if (now != null) {
                board.update(now, true);
            }
        });
        model.latestEventsProperty().addListener((obs, old, events) -> react(events));
        model.endedProperty().addListener((obs, old, reason) -> {
            if (reason != null) {
                endText.setText(FinishedGamePresentation.endText(reason));
                endCard.setVisible(true);
            }
        });
    }

    // the same sounds and shake as live play, so a replay feels like the real thing
    private void react(java.util.List<EventDto> events) {
        Feedback.get().playEvents(events);
        if (events.stream().anyMatch(event -> event.type() == EventType.CAPTURE || event.type() == EventType.TRIPLE_SIX)) {
            board.jolt();
        }
    }

    private void close(Runnable onClose) {
        if (model.endedProperty().get() == null) {
            session.request(new ClientMessage.ControlReplay(UUID.randomUUID(), model.replayId(), ReplayAction.STOP, null),
                    response -> { });
        }
        onClose.run();
    }
}
