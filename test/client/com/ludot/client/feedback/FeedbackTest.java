package com.ludot.client.feedback;

import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Feedback (the sound and motion switches)")
class FeedbackTest {

    private Preferences node;

    @BeforeEach
    void freshPreferences() {
        node = Preferences.userRoot().node("ludot-feedback-test-" + UUID.randomUUID());
    }

    @AfterEach
    void cleanUp() throws BackingStoreException {
        node.removeNode();
    }

    @Test @DisplayName("sound and motion are on the first time")
    void onByDefault() {
        Feedback feedback = new Feedback(node, null);
        assertTrue(feedback.soundProperty().get());
        assertTrue(feedback.motionOn());
    }

    @Test @DisplayName("turning a switch off is remembered by the next run")
    void remembered() {
        Feedback first = new Feedback(node, null);
        first.soundProperty().set(false);
        first.motionProperty().set(false);

        Feedback second = new Feedback(node, null);
        assertFalse(second.soundProperty().get());
        assertFalse(second.motionOn());
    }

    @Test @DisplayName("switching back on is remembered too")
    void rememberedBothWays() {
        Feedback first = new Feedback(node, null);
        first.soundProperty().set(false);
        first.soundProperty().set(true);
        assertTrue(new Feedback(node, null).soundProperty().get());
    }

    @Test @DisplayName("playing with no audio device or player does nothing and does not throw")
    void silentWithoutPlayer() {
        Feedback feedback = new Feedback(node, null);
        feedback.play(Cue.CAPTURE);
        feedback.playEvents(List.of(new EventDto(EventType.CAPTURE, null, null, null, null, "Red captured Blue")));
    }
}
