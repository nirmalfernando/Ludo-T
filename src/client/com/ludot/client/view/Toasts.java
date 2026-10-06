package com.ludot.client.view;

import com.ludot.client.feedback.Cue;
import com.ludot.client.feedback.Feedback;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Short-lived messages shown at the bottom of the window (errors, captures, teleports). They stack one above the
 * other instead of landing on top of each other; at most three are shown, the oldest giving way to a new one, and a
 * message that repeats the one already showing is not added again, so a fast game cannot bury the screen.
 */
public final class Toasts {

    private static final int MAX_VISIBLE = 3;
    private static final double SHOW_SECONDS = 3.2;
    private static final double BOTTOM_MARGIN = 36;

    private final StackPane host;
    private final VBox stack = new VBox(6);

    /** @param host the window's root stack; toasts are layered on top of it */
    public Toasts(StackPane host) {
        this.host = host;
        stack.setAlignment(Pos.BOTTOM_CENTER);
        stack.setMouseTransparent(true);
        stack.setPickOnBounds(false);
        stack.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        StackPane.setAlignment(stack, Pos.BOTTOM_CENTER);
        StackPane.setMargin(stack, new Insets(0, 0, BOTTOM_MARGIN, 0));
    }

    public void info(String message) {
        show(message, "#37474f");
    }

    public void error(String message) {
        Feedback.get().play(Cue.ERROR);
        show(message, "#c62828");
    }

    private void show(String message, String background) {
        attach();
        if (isShowing(message)) {
            return;
        }
        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: " + background + "; -fx-text-fill: white; -fx-padding: 8 16;"
                + "-fx-background-radius: 18; -fx-font-size: 13;");
        stack.getChildren().add(toast);
        while (stack.getChildren().size() > MAX_VISIBLE) {
            stack.getChildren().remove(0);
        }
        PauseTransition wait = new PauseTransition(Duration.seconds(SHOW_SECONDS));
        wait.setOnFinished(done -> fadeOut(toast));
        wait.play();
    }

    // the stack is added on first use, so it sits above whatever screen is showing by then
    private void attach() {
        if (stack.getParent() == null) {
            host.getChildren().add(stack);
        }
        stack.toFront();
    }

    private boolean isShowing(String message) {
        return stack.getChildren().stream().anyMatch(node -> node instanceof Label label && label.getText().equals(message));
    }

    private void fadeOut(Label toast) {
        FadeTransition fade = new FadeTransition(Duration.millis(400), toast);
        fade.setToValue(0);
        fade.setOnFinished(finished -> stack.getChildren().remove(toast));
        fade.play();
    }
}
