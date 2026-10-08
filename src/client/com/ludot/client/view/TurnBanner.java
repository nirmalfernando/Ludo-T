package com.ludot.client.view;

import com.ludot.client.state.RoomModel;
import com.ludot.protocol.Controller;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;

/** The sentence under the seats that says what the game is waiting for. */
final class TurnBanner {

    private TurnBanner() {
    }

    static String describe(RoomModel model) {
        var turn = model.turnProperty().get();
        RoomStatus status = model.statusProperty().get();
        if (status == RoomStatus.WAITING) {
            return model.modeProperty().get() == RoomMode.PLAY
                    ? "Take a seat, then the host starts the game" : "Starting…";
        }
        if (status == RoomStatus.FINISHED) {
            return "Game over";
        }
        if (turn == null) {
            return "";
        }
        String controller = model.seats().stream().filter(s -> s.colour() == turn.colour()).findFirst()
                .map(s -> s.controller() == Controller.HUMAN ? s.nickname() : "AI").orElse("");
        if (turn.phase() == com.ludot.protocol.Phase.ROLL_OFF) {
            return "Deciding who goes first: waiting for " + Theme.display(turn.colour()) + " (" + controller + ") to roll";
        }
        return "Waiting for " + Theme.display(turn.colour()) + " (" + controller + ")";
    }
}
