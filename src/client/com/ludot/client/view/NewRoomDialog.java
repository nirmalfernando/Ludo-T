package com.ludot.client.view;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.Variant;
import java.util.Optional;
import java.util.UUID;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Asks for the settings of a new game: who plays, which rules, and an optional seed for a repeatable game. */
final class NewRoomDialog {

    private NewRoomDialog() {
    }

    static Optional<ClientMessage.CreateRoom> show() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New game");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ToggleGroup mode = modeChoices();
        ToggleGroup rules = ruleChoices();
        TextField seed = new TextField();
        seed.setPromptText("random");
        pane.setContent(content(mode, rules, seed));
        Theme.applyPlayful(pane);
        styleButtons(pane, seed);

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return Optional.empty();
        }
        Long seedValue = seed.getText().isBlank() ? null : Long.parseLong(seed.getText().trim());
        return Optional.of(new ClientMessage.CreateRoom(UUID.randomUUID(), (RoomMode) mode.getSelectedToggle().getUserData(),
                (Variant) rules.getSelectedToggle().getUserData(), seedValue));
    }

    private static ToggleGroup modeChoices() {
        ToggleGroup group = new ToggleGroup();
        card(group, RoomMode.PLAY, "Play", "People take seats, AI fills the rest", true);
        card(group, RoomMode.BOT_MATCH, "Bot match", "Four AI players, you watch", false);
        return group;
    }

    private static ToggleGroup ruleChoices() {
        ToggleGroup group = new ToggleGroup();
        card(group, Variant.LUDO_T, "LUDO-T", "Blockades and mystery cells", true);
        card(group, Variant.CLASSIC, "Classic", "Plain Ludo, no twists", false);
        return group;
    }

    // a big selectable card; the group never ends up with nothing selected
    private static void card(ToggleGroup group, Object value, String title, String subtitle, boolean selected) {
        Label name = new Label(title);
        name.setStyle("-fx-font-size: 18;");
        Label what = new Label(subtitle);
        what.getStyleClass().add("muted");
        what.setStyle("-fx-font-size: 13;");
        ToggleButton card = new ToggleButton();
        card.setGraphic(new VBox(2, name, what));
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.getStyleClass().add("choice-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(250);
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setUserData(value);
        card.setToggleGroup(group);
        card.setSelected(selected);
        card.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
            if (card.isSelected()) {
                event.consume();
            }
        });
    }

    private static VBox content(ToggleGroup mode, ToggleGroup rules, TextField seed) {
        Label heading = new Label("Create a game");
        heading.setStyle("-fx-font-size: 28;");
        HBox pawns = new HBox(8);
        for (Colour colour : Colour.values()) {
            pawns.getChildren().add(new PawnMascot(colour, 38));
        }
        HBox top = new HBox(16, heading, pawns);
        top.setAlignment(Pos.CENTER_LEFT);
        Label seedHint = new Label("Same seed, same dice: leave empty for a random game");
        seedHint.getStyleClass().add("muted");
        seedHint.setStyle("-fx-font-size: 13;");
        VBox box = new VBox(10, top, section("Who plays?"), row(mode), section("Which rules?"), row(rules),
                section("Seed (optional)"), seed, seedHint);
        box.setPadding(new Insets(22, 24, 8, 24));
        box.setPrefWidth(560);
        return box;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16; -fx-padding: 8 0 0 0;");
        return label;
    }

    private static HBox row(ToggleGroup group) {
        HBox row = new HBox(12);
        for (Toggle toggle : group.getToggles()) {
            HBox.setHgrow((Node) toggle, Priority.ALWAYS);
            row.getChildren().add((Node) toggle);
        }
        return row;
    }

    private static void styleButtons(DialogPane pane, TextField seed) {
        Button create = (Button) pane.lookupButton(ButtonType.OK);
        create.setText("CREATE");
        create.getStyleClass().add("big-button");
        create.disableProperty().bind(Bindings.createBooleanBinding(() -> !validSeed(seed.getText()), seed.textProperty()));
        Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancel.setText("CANCEL");
        cancel.getStyleClass().add("ghost-button");
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
