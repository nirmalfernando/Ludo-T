package com.ludot.client.view;

import com.ludot.client.state.RoomModel;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

final class RoomHeading extends HBox {

    private final RoomModel model;

    RoomHeading(RoomModel model) {
        super(10);
        this.model = model;
        setAlignment(Pos.CENTER_LEFT);
    }

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
            if (model.variantProperty().get() == com.ludot.protocol.Variant.LUDO_T) {
                Label legend = new Label(DirectionMark.LEGEND);
                legend.setStyle("-fx-text-fill: #546e7a; -fx-font-size: 12;");
                Tooltip.install(legend, new Tooltip("A coin toss decides each piece's direction when it leaves base"));
                getChildren().add(legend);
            }
        }
    }
}
