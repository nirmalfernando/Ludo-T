package com.ludot.client.view;

import com.ludot.client.state.RoomModel;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** The room id with its status, mode and rules badges: the same ones the lobby cards use. */
final class RoomHeading extends HBox {

    private final RoomModel model;

    RoomHeading(RoomModel model) {
        super(10);
        this.model = model;
        setAlignment(Pos.CENTER_LEFT);
    }

    /** Rebuilds the badges from the model's current status. */
    void refresh() {
        Label id = new Label(model.roomId());
        id.getStyleClass().add("room-id");
        getChildren().setAll(id);
        RoomStatus status = model.statusProperty().get();
        if (status != null) {
            getChildren().add(Chips.chip(RoomPresentation.statusLabel(status), RoomPresentation.statusColour(status)));
        }
        RoomMode mode = model.modeProperty().get();
        if (mode != null) {
            getChildren().add(Chips.chip(RoomPresentation.modeLabel(mode), Chips.MODE));
        }
        if (model.variantProperty().get() != null) {
            getChildren().add(Chips.chip(RoomPresentation.rulesLabel(model.variantProperty().get()), Chips.RULES));
        }
    }
}
