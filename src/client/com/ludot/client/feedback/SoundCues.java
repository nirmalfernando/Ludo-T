package com.ludot.client.feedback;

import com.ludot.protocol.EventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Which sound goes with which game event. */
public final class SoundCues {

    private SoundCues() {
    }

    /** The sound for one event, if it deserves one (most narration lines are silent). */
    public static Optional<Cue> forEvent(EventType type) {
        return switch (type) {
            case DICE_ROLL -> Optional.of(Cue.DICE);
            case MOVE, BLOCK_MOVE -> Optional.of(Cue.STEP);
            case CAPTURE -> Optional.of(Cue.CAPTURE);
            case MYSTERY_TELEPORT -> Optional.of(Cue.TELEPORT);
            case HOME_REACH -> Optional.of(Cue.HOME);
            case WIN -> Optional.of(Cue.WIN);
            case TRIPLE_SIX -> Optional.of(Cue.ERROR);
            default -> Optional.empty();
        };
    }

    /**
     * The sounds for the events of one update, in order. A capture or a home arrival already ends with its own
     * "landing" sound, so the plain step sound is left out when one of them is in the same batch.
     */
    public static List<Cue> forEvents(List<EventType> types) {
        List<Cue> cues = new ArrayList<>();
        for (EventType type : types) {
            forEvent(type).ifPresent(cues::add);
        }
        if (cues.contains(Cue.CAPTURE) || cues.contains(Cue.HOME)) {
            cues.removeIf(cue -> cue == Cue.STEP);
        }
        return cues;
    }
}
