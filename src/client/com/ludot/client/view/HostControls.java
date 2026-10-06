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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Start, pause or resume, and AI speed: shown to the host only, and only the parts that make sense right now. */
final class HostControls extends HBox {

    private final RoomModel model;
    private final Button start = new Button("Start game");
    private final Button pause = new Button("Pause");
    private final ChoiceBox<Speed> speed = new ChoiceBox<>();
    private boolean updatingSpeed;

    HostControls(RoomModel model, Consumer<ClientMessage.Request> send) {
        super(8);
        this.model = model;
        setAlignment(Pos.CENTER);
        speed.getItems().addAll(Speed.values());
        speed.setOnAction(event -> {
            if (!updatingSpeed && speed.getValue() != null && speed.getValue() != model.speedProperty().get()) {
                send.accept(new ClientMessage.SetSpeed(UUID.randomUUID(), model.roomId(), speed.getValue()));
            }
        });
        start.setOnAction(event -> send.accept(new ClientMessage.StartGame(UUID.randomUUID(), model.roomId())));
        pause.setOnAction(event -> send.accept(model.statusProperty().get() == RoomStatus.PAUSED
                ? new ClientMessage.ResumeGame(UUID.randomUUID(), model.roomId())
                : new ClientMessage.PauseGame(UUID.randomUUID(), model.roomId())));
        getChildren().addAll(start, pause, new Label("AI speed"), speed);
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
        speed.setValue(model.speedProperty().get());
        updatingSpeed = false;
    }

    private static void show(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }
}
