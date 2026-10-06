package com.ludot.client.view;

import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What the screen does when game events arrive: tumble the die to the rolled number and toast the dramatic ones. */
final class RoomEventReactions {

    private static final Pattern ROLLED = Pattern.compile("rolled (\\d)");
    private static final Set<EventType> WORTH_A_TOAST = EnumSet.of(EventType.CAPTURE, EventType.MYSTERY_TELEPORT,
            EventType.HOME_REACH, EventType.TRIPLE_SIX, EventType.BRIEFING_ESCAPE);

    private final DicePane dice;
    private final Toasts toasts;

    RoomEventReactions(DicePane dice, Toasts toasts) {
        this.dice = dice;
        this.toasts = toasts;
    }

    void react(List<EventDto> events) {
        for (EventDto event : events) {
            if (event.type() == EventType.DICE_ROLL) {
                showRolled(event.text());
            } else if (WORTH_A_TOAST.contains(event.type())) {
                toasts.info(event.text());
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
