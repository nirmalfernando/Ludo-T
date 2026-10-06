package com.ludot.client.view;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.Variant;
import java.util.Optional;
import java.util.UUID;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

/** Asks for the settings of a new room: who plays, which rules, and an optional seed for a repeatable game. */
final class NewRoomDialog {

    private NewRoomDialog() {
    }

    static Optional<ClientMessage.CreateRoom> show() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New room");
        dialog.setHeaderText("Create a room");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ChoiceBox<RoomMode> mode = modeChoice();
        ChoiceBox<Variant> variant = variantChoice();
        TextField seed = new TextField();
        seed.setPromptText("random");
        dialog.getDialogPane().setContent(form(mode, variant, seed));
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
                Bindings.createBooleanBinding(() -> !validSeed(seed.getText()), seed.textProperty()));

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return Optional.empty();
        }
        Long seedValue = seed.getText().isBlank() ? null : Long.parseLong(seed.getText().trim());
        return Optional.of(new ClientMessage.CreateRoom(UUID.randomUUID(), mode.getValue(), variant.getValue(), seedValue));
    }

    private static ChoiceBox<RoomMode> modeChoice() {
        ChoiceBox<RoomMode> mode = new ChoiceBox<>();
        mode.getItems().addAll(RoomMode.PLAY, RoomMode.BOT_MATCH);
        mode.setValue(RoomMode.PLAY);
        mode.setConverter(new StringConverter<>() {
            @Override
            public String toString(RoomMode value) {
                return value == RoomMode.PLAY ? "Play (people claim seats, AI fills the rest)"
                        : "Bot match (four AI players, everyone watches)";
            }

            @Override
            public RoomMode fromString(String text) {
                return null;
            }
        });
        return mode;
    }

    private static ChoiceBox<Variant> variantChoice() {
        ChoiceBox<Variant> variant = new ChoiceBox<>();
        variant.getItems().addAll(Variant.LUDO_T, Variant.CLASSIC);
        variant.setValue(Variant.LUDO_T);
        return variant;
    }

    private static GridPane form(ChoiceBox<RoomMode> mode, ChoiceBox<Variant> variant, TextField seed) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14));
        grid.addRow(0, new Label("Mode"), mode);
        grid.addRow(1, new Label("Rules"), variant);
        grid.addRow(2, new Label("Seed"), seed);
        return grid;
    }

    private static boolean validSeed(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        try {
            Long.parseLong(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
