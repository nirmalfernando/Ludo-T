package com.ludot.client.feedback;

import com.ludot.protocol.EventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SoundCues {

    private SoundCues() {
    }

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
