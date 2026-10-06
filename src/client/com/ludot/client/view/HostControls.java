package com.ludot.client.view;

import com.ludot.client.state.RoomModel;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.Speed;
import java.util.UUID;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Start, pause or resume, and AI speed: shown to the host only, and only the parts that make sense right now. */
final class HostControls extends HBox {

    private final RoomModel model;
    private final Button start = new Button("Start Game");
    private final Button pause = new Button("Pause");
    private final ToggleGroup speed = new ToggleGroup();
    private boolean updatingSpeed;

    HostControls(RoomModel model, Consumer<ClientMessage.Request> send) {
        super(8);
        this.model = model;
        setAlignment(Pos.CENTER);
        start.getStyleClass().add("blue-button");
        pause.getStyleClass().add("blue-button");
        speed.selectedToggleProperty().addListener((obs, old, now) -> {
            if (!updatingSpeed && now != null && now.getUserData() != model.speedProperty().get()) {
                send.accept(new ClientMessage.SetSpeed(UUID.randomUUID(), model.roomId(), (Speed) now.getUserData()));
            }
        });
        start.setOnAction(event -> send.accept(new ClientMessage.StartGame(UUID.randomUUID(), model.roomId())));
        pause.setOnAction(event -> send.accept(model.statusProperty().get() == RoomStatus.PAUSED
                ? new ClientMessage.ResumeGame(UUID.randomUUID(), model.roomId())
                : new ClientMessage.PauseGame(UUID.randomUUID(), model.roomId())));
        Label speedLabel = new Label("AI Speed");
        speedLabel.getStyleClass().add("muted");
        getChildren().addAll(start, pause, speedLabel, segments());
        managedProperty().bind(visibleProperty());
        visibleProperty().bind(Bindings.createBooleanBinding(model::isHost, model.hostIdProperty()));
    }

    /** Shows and labels the buttons for the room's current status. */
    void refresh() {
        RoomStatus status = model.statusProperty().get();
        boolean waiting = status == RoomStatus.WAITING;
        boolean running = status == RoomStatus.RUNNING || status == RoomStatus.PAUSED;
        show(start, waiting && model.modeProperty().get() == RoomMode.PLAY);
        show(pause, running);
        pause.setText(status == RoomStatus.PAUSED ? "Resume" : "Pause");
        updatingSpeed = true;
        speed.getToggles().stream().filter(toggle -> toggle.getUserData() == model.speedProperty().get())
                .findFirst().ifPresent(toggle -> toggle.setSelected(true));
        updatingSpeed = false;
    }

    // Slow | Normal | Fast as one rounded segmented control
    private HBox segments() {
        HBox box = new HBox();
        Speed[] order = {Speed.SLOW, Speed.NORMAL, Speed.FAST};
        for (int i = 0; i < order.length; i++) {
            Speed value = order[i];
            ToggleButton segment = new ToggleButton(value.name().charAt(0) + value.name().substring(1).toLowerCase());
            segment.setUserData(value);
            segment.setToggleGroup(speed);
            segment.getStyleClass().add("segment");
            segment.getStyleClass().add(i == 0 ? "segment-first" : i == order.length - 1 ? "segment-last" : "segment-middle");
            Toggles.keepSelected(segment);
            box.getChildren().add(segment);
        }
        return box;
    }

    private static void show(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }
}
