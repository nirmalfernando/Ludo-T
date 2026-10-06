package com.ludot.client.view;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Short-lived messages shown at the bottom of the window (errors, captures, teleports). */
public final class Toasts {

    private static final double SHOW_SECONDS = 2.6;

    private final StackPane host;

    /** @param host the window's root stack; toasts are layered on top of it */
    public Toasts(StackPane host) {
        this.host = host;
    }

    public void info(String message) {
        show(message, "#37474f");
    }

    public void error(String message) {
        show(message, "#c62828");
    }

    private void show(String message, String background) {
        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: " + background + "; -fx-text-fill: white; -fx-padding: 8 16;"
                + "-fx-background-radius: 18; -fx-font-size: 13;");
        toast.setMouseTransparent(true);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 36, 0));
        host.getChildren().add(toast);
        PauseTransition wait = new PauseTransition(Duration.seconds(SHOW_SECONDS));
        wait.setOnFinished(done -> {
            FadeTransition fade = new FadeTransition(Duration.millis(400), toast);
            fade.setToValue(0);
            fade.setOnFinished(finished -> host.getChildren().remove(toast));
            fade.play();
        });
        wait.play();
    }
}
