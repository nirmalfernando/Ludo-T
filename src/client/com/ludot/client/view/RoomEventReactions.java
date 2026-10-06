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

/**
 * What the screen does when game events arrive: tumble the die to the rolled number, toast the dramatic ones, make
 * their sounds, shake the board on a capture and pop a piece that reaches home.
 */
final class RoomEventReactions {

    private static final Pattern ROLLED = Pattern.compile("rolled (\\d)");
    private static final Set<EventType> WORTH_A_TOAST = EnumSet.of(EventType.CAPTURE, EventType.MYSTERY_TELEPORT,
            EventType.HOME_REACH, EventType.TRIPLE_SIX, EventType.BRIEFING_ESCAPE);
    private static final Set<EventType> SHAKES_THE_BOARD = EnumSet.of(EventType.CAPTURE, EventType.TRIPLE_SIX);

    private final DicePane dice;
    private final Toasts toasts;
    private final BoardView board;

    RoomEventReactions(DicePane dice, Toasts toasts, BoardView board, RoomModel model) {
        new RoomCues(model); // the room-level sounds (your turn, someone joined) listen to the model themselves
        this.dice = dice;
        this.toasts = toasts;
        this.board = board;
    }

    void react(List<EventDto> events) {
        Feedback.get().playEvents(events);
        for (EventDto event : events) {
            if (event.type() == EventType.DICE_ROLL) {
                showRolled(event.text());
            } else if (WORTH_A_TOAST.contains(event.type())) {
                toasts.info(event.text());
            }
            if (SHAKES_THE_BOARD.contains(event.type())) {
                board.jolt();
            } else if (event.type() == EventType.HOME_REACH && event.colour() != null && event.pieceIndex() != null) {
                board.celebrate(com.ludot.protocol.Colour.valueOf(event.colour().name()), event.pieceIndex());
            }
        }
    }

    private void showRolled(String narration) {
        Matcher matcher = ROLLED.matcher(narration);
        if (matcher.find()) {
            dice.showRoll(Integer.parseInt(matcher.group(1)));
        }
    }
}
