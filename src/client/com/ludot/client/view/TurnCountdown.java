package com.ludot.client.view;

import com.ludot.client.state.ClockSync;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.RoomStatus;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

final class TurnCountdown {

    private static final long URGENT_MS = 5_000;
    private static final long TICK_MS = 250;

    private final RoomModel model;
    private final ClockSync clock;
    private final DicePane dice;

    TurnCountdown(RoomModel model, ClockSync clock, DicePane dice) {
        this.model = model;
        this.clock = clock;
        this.dice = dice;
    }

    void start(Node owner) {
        Timeline ticker = new Timeline(new KeyFrame(Duration.millis(TICK_MS), event -> update()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
        owner.sceneProperty().addListener((obs, old, now) -> {
            if (now == null) {
                ticker.stop();
            }
        });
    }

    private void update() {
        var turn = model.turnProperty().get();
        if (turn == null || turn.deadlineEpochMs() == null || model.statusProperty().get() != RoomStatus.RUNNING) {
            dice.setCountdown("", false);
            return;
        }
        long remaining = clock.remainingMs(turn.deadlineEpochMs(), System.currentTimeMillis());
        dice.setCountdown("⏱ " + Theme.display(turn.colour()) + ": " + ClockSync.format(remaining),
                remaining <= URGENT_MS);
    }
}
