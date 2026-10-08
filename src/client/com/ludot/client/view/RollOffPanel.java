package com.ludot.client.view;

import com.ludot.protocol.Colour;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import com.ludot.protocol.Phase;
import com.ludot.protocol.TurnInfo;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

final class RollOffPanel extends VBox {

    private static final double RESULT_SECONDS = 4.5;

    private final RollOffTable table = new RollOffTable();
    private final Supplier<TurnInfo> turn;
    private final Supplier<Optional<Colour>> mine;
    private final Label headline = new Label();
    private final VBox rows = new VBox(6);
    private final PauseTransition hideLater = new PauseTransition(Duration.seconds(RESULT_SECONDS));
    private boolean showingResult;

    RollOffPanel(Supplier<TurnInfo> turn, Supplier<Optional<Colour>> mine) {
        super(10);
        this.turn = turn;
        this.mine = mine;
        Label title = new Label("Who Goes First?");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #263238;");
        Label rule = new Label("Everyone rolls once. The highest roll starts; a tie rolls again.");
        rule.setStyle("-fx-font-size: 12; -fx-text-fill: #546e7a;");
        rule.setWrapText(true);
        headline.setWrapText(true);
        headline.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ef6c00;");
        getChildren().addAll(title, rule, rows, headline);
        setPadding(new Insets(16, 20, 16, 20));
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setPrefWidth(360);
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: rgba(255,255,255,0.96); -fx-background-radius: 20;"
                + "-fx-border-color: #ffb300; -fx-border-width: 3; -fx-border-radius: 20;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 18, 0.2, 0, 5);");
        setOnMouseClicked(event -> dismiss());
        hideLater.setOnFinished(event -> dismiss());
        setVisible(false);
        setManaged(false);
    }

    void react(List<EventDto> events) {
        boolean changed = false;
        for (EventDto event : events) {
            if (event.type() == EventType.GAME_START) {
                table.reset();
                showingResult = false;
                hideLater.stop();
                changed = true;
            } else if (event.type() == EventType.ROLL_OFF || event.type() == EventType.TURN_ORDER) {
                table.accept(event);
                changed = true;
                if (event.type() == EventType.TURN_ORDER) {
                    showingResult = true;
                    hideLater.playFromStart();
                }
            }
        }
        if (changed) {
            refresh();
        }
    }

    void refresh() {
        TurnInfo now = turn.get();
        boolean rollingOff = (now != null && now.phase() == Phase.ROLL_OFF) || (table.hasRolls() && !table.isDecided());
        boolean show = rollingOff || showingResult;
        setVisible(show);
        setManaged(show);
        if (!show) {
            return;
        }
        rows.getChildren().setAll(table.rows().stream().map(row -> rowNode(row, now)).toList());
        headline.setText(headlineText(now));
    }

    private void dismiss() {
        showingResult = false;
        hideLater.stop();
        refresh();
    }

    private String headlineText(TurnInfo now) {
        if (table.isDecided()) {
            return "🎉 " + Theme.display(table.winner().orElseThrow()) + " goes first!   Order: "
                    + String.join(" → ", table.playOrder().stream().map(Theme::display).toList());
        }
        if (table.leader().isPresent()) {
            return Theme.display(table.leader().get()) + " has the highest roll!";
        }
        Optional<Colour> next = table.nextRoller();
        String tie = table.rows().stream().anyMatch(row -> row.out()) ? "Tie for the highest roll: the tied players roll again.  " : "";
        if (next.isEmpty()) {
            return tie;
        }
        boolean me = mine.get().filter(next.get()::equals).isPresent();
        return tie + (me ? "Your turn: press Roll!" : "Waiting for " + Theme.display(next.get()) + " to roll…");
    }

    private HBox rowNode(RollOffTable.Row row, TurnInfo now) {
        boolean rolling = !table.isDecided() && table.nextRoller().filter(row.colour()::equals).isPresent();
        Label name = new Label(Theme.display(row.colour()));
        name.setPrefWidth(70);
        name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #37474f;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox line = new HBox(10, new PawnMascot(row.colour(), 30), name, diceOf(row, rolling), spacer, noteOf(row));
        line.setAlignment(Pos.CENTER_LEFT);
        line.setPadding(new Insets(4, 8, 4, 8));
        line.setStyle(lineStyle(row, rolling));
        line.setOpacity(row.out() && !row.winner() ? 0.5 : 1);
        return line;
    }

    // everything this colour rolled, oldest first, and a waiting die if it is its turn
    private static HBox diceOf(RollOffTable.Row row, boolean rolling) {
        HBox dice = new HBox(6);
        dice.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < row.rolls().size(); i++) {
            if (i > 0) {
                dice.getChildren().add(new Label("→"));
            }
            dice.getChildren().add(chip(row.rolls().get(i), row.colour(), i == row.rolls().size() - 1));
        }
        if (rolling) {
            Label waiting = new Label("🎲 …");
            waiting.setStyle("-fx-font-size: 14; -fx-text-fill: #ef6c00;");
            dice.getChildren().add(waiting);
        }
        return dice;
    }

    private static Label noteOf(RollOffTable.Row row) {
        Label note = new Label(row.winner() ? "★ first" : row.out() ? "out" : "");
        note.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " + (row.winner() ? "#ef6c00" : "#90a4ae") + ";");
        return note;
    }

    private static String lineStyle(RollOffTable.Row row, boolean rolling) {
        if (row.winner()) {
            return "-fx-background-color: #fff8e1; -fx-background-radius: 12; -fx-border-color: #ffb300;"
                    + "-fx-border-radius: 12; -fx-border-width: 2;";
        }
        return rolling ? "-fx-background-color: #e3f2fd; -fx-background-radius: 12;" : "-fx-background-radius: 12;";
    }

    // a die as a rounded chip in the colour of the player; the last roll is the one that counts
    private static Label chip(int value, Colour colour, boolean latest) {
        Color main = Theme.strong(colour);
        Label chip = new Label(String.valueOf(value));
        chip.setMinSize(30, 30);
        chip.setAlignment(Pos.CENTER);
        String text = colour == Colour.YELLOW ? "#263238" : "white";
        chip.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: " + text + ";"
                + "-fx-background-radius: 8; -fx-background-color: " + toWeb(main) + ";"
                + (latest ? "" : "-fx-opacity: 0.55;"));
        return chip;
    }

    private static String toWeb(Color color) {
        return String.format("#%02x%02x%02x", (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255));
    }
}
