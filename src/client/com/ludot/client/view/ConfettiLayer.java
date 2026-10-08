package com.ludot.client.view;

import com.ludot.client.feedback.ConfettiField;
import com.ludot.client.feedback.Feedback;
import com.ludot.protocol.Colour;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

final class ConfettiLayer extends Pane {

    private static final int PIECES = 170;
    private static final double MAX_SECONDS = 7;
    private static final Color[] PALETTE = {
        Theme.strong(Colour.RED), Theme.strong(Colour.GREEN), Theme.strong(Colour.YELLOW), Theme.strong(Colour.BLUE),
        Color.web("#ff7ab8"), Color.web("#8f7bd8")};

    private final Canvas canvas = new Canvas();
    private AnimationTimer timer;

    ConfettiLayer() {
        setMouseTransparent(true);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                stop();
            }
        });
    }

    // starts a burst (unless motion effects are switched off)
    void burst() {
        if (!Feedback.get().motionOn() || getWidth() <= 0) {
            return;
        }
        stop();
        ConfettiField field = new ConfettiField(PIECES, getWidth(), System.nanoTime());
        timer = new AnimationTimer() {
            private long last;
            private double elapsed;

            @Override
            public void handle(long now) {
                double dt = last == 0 ? 0 : Math.min(0.05, (now - last) / 1e9);
                last = now;
                elapsed += dt;
                field.step(dt);
                draw(field);
                if (elapsed > MAX_SECONDS || field.finished(getHeight())) {
                    stop();
                }
            }
        };
        timer.start();
    }

    private void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void draw(ConfettiField field) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (int i = 0; i < field.count(); i++) {
            g.save();
            g.translate(field.x(i), field.y(i));
            g.rotate(Math.toDegrees(field.angle(i)));
            g.setFill(PALETTE[field.colour(i)]);
            g.fillRect(-field.size(i) / 2, -field.size(i) / 4, field.size(i), field.size(i) / 2);
            g.restore();
        }
    }
}
