package com.ludot.client.view;

import com.ludot.client.feedback.Feedback;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Node;

final class RoomEventReactions {

    private static final Pattern ROLLED = Pattern.compile("rolled (\\d)");
    private static final Set<EventType> WORTH_A_TOAST = EnumSet.of(EventType.CAPTURE, EventType.MYSTERY_TELEPORT,
            EventType.HOME_REACH, EventType.TRIPLE_SIX, EventType.BRIEFING_ESCAPE);
    private static final Set<EventType> SHAKES_THE_BOARD = EnumSet.of(EventType.CAPTURE, EventType.TRIPLE_SIX);

    private final DicePane dice;
    private final Toasts toasts;
    private final BoardView board;
    private final RollOffPanel rollOffPanel;

    RoomEventReactions(DicePane dice, Toasts toasts, BoardView board, RoomModel model) {
        new RoomCues(model); // the room-level sounds (your turn, someone joined) listen to the model themselves
        this.dice = dice;
        this.toasts = toasts;
        this.board = board;
        this.rollOffPanel = new RollOffPanel(() -> model.turnProperty().get(),
                () -> model.mySeat().map(com.ludot.protocol.SeatDto::colour));
    }

    void react(List<EventDto> events) {
        Feedback.get().playEvents(events);
        rollOffPanel.react(events);
        for (EventDto event : events) {
            narrate(event);
            boardEffects(event);
        }
    }

    Node rollOffPanel() {
        return rollOffPanel;
    }

    void refreshTurn() {
        rollOffPanel.refresh();
    }

    // the die, the coin and the toasts for one event
    private void narrate(EventDto event) {
        if (event.type() == EventType.DICE_ROLL) {
            showRolled(event.text());
        } else if (event.type() == EventType.ROLL_OFF && event.value() != null) {
            dice.showRoll(event.value()); // the die tumbles to each contender's roll for the playing order
        } else if (isCoinToss(event)) {
            coinToss(event);
        } else if (WORTH_A_TOAST.contains(event.type())) {
            toasts.info(event.text());
        }
    }

    private static boolean isCoinToss(EventDto event) {
        return event.type() == EventType.COIN_TOSS && event.colour() != null && event.pieceIndex() != null
                && event.value() != null;
    }

    // the board shakes on a capture and a piece that reaches home pops
    private void boardEffects(EventDto event) {
        if (SHAKES_THE_BOARD.contains(event.type())) {
            board.jolt();
        } else if (event.type() == EventType.HOME_REACH && event.colour() != null && event.pieceIndex() != null) {
            board.celebrate(com.ludot.protocol.Colour.valueOf(event.colour().name()), event.pieceIndex());
        }
    }

    // the coin that decides a piece's direction flips over the piece, and a toast says what it showed
    private void coinToss(EventDto event) {
        boolean clockwise = event.value() == 1;
        board.coinFlip(event.colour(), event.pieceIndex(), clockwise);
        toasts.info(DirectionMark.tossMessage(event.colour(), event.pieceIndex(), clockwise));
    }

    private void showRolled(String narration) {
        Matcher matcher = ROLLED.matcher(narration);
        if (matcher.find()) {
            dice.showRoll(Integer.parseInt(matcher.group(1)));
        }
    }
}
