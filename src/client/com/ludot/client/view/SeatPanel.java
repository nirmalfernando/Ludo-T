package com.ludot.client.view;

import com.ludot.client.state.RoomModel;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.SeatDto;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** Four seat cards: who plays each colour, whose turn it is, and buttons to take or leave a seat before the game starts. */
public final class SeatPanel extends VBox {

    private final RoomModel model;
    private final Consumer<Colour> onClaim;
    private final Runnable onRelease;
    private final Runnable onReclaim;

    public SeatPanel(RoomModel model, Consumer<Colour> onClaim, Runnable onRelease, Runnable onReclaim) {
        super(8);
        this.model = model;
        this.onClaim = onClaim;
        this.onRelease = onRelease;
        this.onReclaim = onReclaim;
        model.seats().addListener((javafx.collections.ListChangeListener<SeatDto>) change -> rebuild());
        model.turnProperty().addListener((obs, old, now) -> rebuild());
        model.statusProperty().addListener((obs, old, now) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        boolean joinable = model.statusProperty().get() == RoomStatus.WAITING && model.modeProperty().get() == RoomMode.PLAY;
        for (SeatDto seat : model.seats()) {
            getChildren().add(card(seat, joinable));
        }
    }

    private HBox card(SeatDto seat, boolean joinable) {
        boolean mine = model.mySeat().map(s -> s.colour() == seat.colour()).orElse(false);
        boolean active = model.statusProperty().get() == RoomStatus.RUNNING && model.turnProperty().get() != null
                && model.turnProperty().get().colour() == seat.colour();

        Circle dot = new Circle(9, Theme.strong(seat.colour()));
        Label name = new Label(Theme.display(seat.colour()) + (mine ? "  (you)" : ""));
        name.setStyle("-fx-font-weight: bold;");
        Label who = new Label(describe(seat));
        who.setStyle("-fx-text-fill: #546e7a; -fx-font-size: 12;");
        VBox text = new VBox(2, name, who);
        Label turnMark = new Label(active ? "▶" : "");
        turnMark.setStyle("-fx-text-fill: #ff8f00; -fx-font-size: 16;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, dot, text, spacer, turnMark);
        row.setAlignment(Pos.CENTER_LEFT);
        if (joinable && seat.controller() == Controller.FREE) {
            Button take = new Button("Take");
            take.setOnAction(event -> onClaim.accept(seat.colour()));
            row.getChildren().add(take);
        } else if (joinable && mine) {
            Button leave = new Button("Leave");
            leave.setOnAction(event -> onRelease.run());
            row.getChildren().add(leave);
        }
        boolean away = mine && (seat.controller() == Controller.AFK || seat.controller() == Controller.DISCONNECTED)
                && model.statusProperty().get() == RoomStatus.RUNNING;
        if (away) {
            Button takeBack = new Button("Take back");
            takeBack.setStyle("-fx-base: #ffb300;");
            takeBack.setOnAction(event -> onReclaim.run());
            text.getChildren().add(takeBack); // under the labels: the card is too narrow for it beside them
        }
        row.setStyle(Theme.CARD_STYLE + (active ? "-fx-border-color: #ffb300; -fx-border-width: 2;" : ""));
        return row;
    }

    private static String describe(SeatDto seat) {
        return switch (seat.controller()) {
            case FREE -> "free";
            case AI -> "AI player";
            case HUMAN -> seat.nickname();
            case AFK -> seat.nickname() + " (away)";
            case DISCONNECTED -> seat.nickname() + " (offline)";
        };
    }
}
