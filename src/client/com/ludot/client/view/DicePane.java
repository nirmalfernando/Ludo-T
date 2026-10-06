package com.ludot.client.view;

import java.util.Random;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** The die, a banner saying what is expected, and the Roll button. */
public final class DicePane extends VBox {

    private static final String[] FACES = {"⚀", "⚁", "⚂", "⚃", "⚄", "⚅"};
    private static final int TUMBLE_FRAMES = 8;

    private final Label banner = new Label("Waiting for the game to start");
    private final Label die = new Label("⚀");
    private final Label countdown = new Label();
    private final Button roll = new Button("Roll the Dice");
    private final Random random = new Random();
    private Timeline tumble;

    public DicePane(Runnable onRoll) {
        super(8);
        setAlignment(Pos.CENTER);
        getStyleClass().add("panel");
        setPadding(new javafx.geometry.Insets(14));
        banner.setWrapText(true);
        banner.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-alignment: center;");
        die.setStyle("-fx-font-size: 64; -fx-text-fill: #263238;");
        roll.getStyleClass().add("big-button");
        roll.setDefaultButton(true);
        roll.setDisable(true);
        roll.setOnAction(event -> {
            roll.setDisable(true);
            onRoll.run();
        });
        countdown.setStyle("-fx-font-size: 13; -fx-text-fill: #546e7a;");
        getChildren().addAll(banner, countdown, die, roll);
    }

    public void setBanner(String text) {
        banner.setText(text);
    }

    /** Shows the time left for whoever must act; an empty text hides it, urgent turns it red. */
    public void setCountdown(String text, boolean urgent) {
        countdown.setText(text);
        countdown.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + (urgent ? "#c62828" : "#546e7a") + ";");
    }

    public void setRollEnabled(boolean enabled) {
        roll.setDisable(!enabled);
    }

    /** Tumbles through random faces and settles on the rolled value. */
    public void showRoll(int value) {
        if (tumble != null) {
            tumble.stop();
        }
        tumble = new Timeline();
        for (int frame = 1; frame <= TUMBLE_FRAMES; frame++) {
            tumble.getKeyFrames().add(new KeyFrame(Duration.millis(frame * 55),
                    event -> die.setText(FACES[random.nextInt(FACES.length)])));
        }
        tumble.getKeyFrames().add(new KeyFrame(Duration.millis((TUMBLE_FRAMES + 1) * 55),
                event -> die.setText(FACES[Math.max(0, Math.min(5, value - 1))])));
        tumble.play();
    }
}
