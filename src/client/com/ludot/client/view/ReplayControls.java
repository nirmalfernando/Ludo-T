package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.client.state.ReplayModel;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ReplayAction;
import com.ludot.protocol.Speed;
import java.util.UUID;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

final class ReplayControls extends VBox {

    private final ReplayModel model;
    private final Session session;
    private final Toasts toasts;
    private final Button pause = new Button("Pause");
    private final SpeedPicker speed;
    private final ProgressBar bar = new ProgressBar(0);
    private final Label progress = new Label();

    ReplayControls(ReplayModel model, Session session, Toasts toasts) {
        super(12);
        this.model = model;
        this.session = session;
        this.toasts = toasts;
        this.speed = new SpeedPicker(this::changeSpeed);
        speed.select(model.speedProperty().get());
        getChildren().addAll(winnerCard(), controlCard());
        model.indexProperty().addListener((obs, old, now) -> refreshProgress());
        model.totalProperty().addListener((obs, old, now) -> refreshProgress());
        model.pausedProperty().addListener((obs, old, now) -> pause.setText(now ? "Resume" : "Pause"));
        model.endedProperty().addListener((obs, old, now) -> pause.setDisable(now != null));
        refreshProgress();
    }

    private VBox winnerCard() {
        Label heading = new Label("Winner");
        heading.getStyleClass().add("muted");
        Label who = new Label(FinishedGamePresentation.winnerText(model.game().winner()));
        who.setStyle("-fx-font-size: 22; -fx-text-fill: " + Theme.hex(model.game().winner()) + ";");
        VBox card = new VBox(4, heading, new PawnMascot(model.game().winner(), 64), who);
        card.getStyleClass().add("panel");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        return card;
    }

    private VBox controlCard() {
        pause.getStyleClass().add("blue-button");
        pause.setOnAction(event -> send(model.pausedProperty().get() ? ReplayAction.RESUME : ReplayAction.PAUSE, null));
        speed.setAlignment(Pos.CENTER);
        bar.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("muted");
        progress.setStyle("-fx-font-size: 13;");
        VBox card = new VBox(10, pause, speed, bar, progress);
        card.getStyleClass().add("panel");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14));
        return card;
    }

    private void changeSpeed(Speed picked) {
        if (picked != model.speedProperty().get()) {
            // a speed change must not undo a pause: repeat the current state as the action
            send(model.pausedProperty().get() ? ReplayAction.PAUSE : ReplayAction.RESUME, picked);
        }
    }

    private void send(ReplayAction action, Speed newSpeed) {
        boolean pausing = action == ReplayAction.PAUSE;
        session.request(new ClientMessage.ControlReplay(UUID.randomUUID(), model.replayId(), action, newSpeed),
                () -> {
                    model.pausedProperty().set(pausing);
                    if (newSpeed != null) {
                        model.speedProperty().set(newSpeed);
                    }
                }, toasts::error);
    }

    private void refreshProgress() {
        int index = model.indexProperty().get();
        int total = model.totalProperty().get();
        bar.setProgress(FinishedGamePresentation.progress(index, total));
        progress.setText(FinishedGamePresentation.progressText(index, total));
    }
}
