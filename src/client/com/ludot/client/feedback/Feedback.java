package com.ludot.client.feedback;

import com.ludot.protocol.EventDto;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Sound and motion effects in one place, with the two on/off switches the player controls. The choices are remembered
 * between runs. Use {@link #get()} from the UI; everything else just asks "play this" or "is motion on?".
 */
public final class Feedback {

    private static final double VOLUME = 0.7;
    private static final String SOUND_KEY = "sound";
    private static final String MOTION_KEY = "motion";

    private static volatile Feedback shared;

    private final BooleanProperty sound = new SimpleBooleanProperty(true);
    private final BooleanProperty motion = new SimpleBooleanProperty(true);
    private final SoundPlayer player;

    /** @param player null in tests, where nothing should reach a sound card */
    Feedback(Preferences preferences, SoundPlayer player) {
        this.player = player;
        sound.set(preferences.getBoolean(SOUND_KEY, true));
        motion.set(preferences.getBoolean(MOTION_KEY, true));
        remember(sound, preferences, SOUND_KEY);
        remember(motion, preferences, MOTION_KEY);
    }

    /** The one instance the whole app shares. */
    public static Feedback get() {
        Feedback result = shared;
        if (result == null) {
            synchronized (Feedback.class) {
                if (shared == null) {
                    SoundPlayer player = new SoundPlayer();
                    player.warmUp();
                    shared = new Feedback(Preferences.userNodeForPackage(Feedback.class), player);
                }
                result = shared;
            }
        }
        return result;
    }

    private static void remember(BooleanProperty property, Preferences preferences, String key) {
        property.addListener((obs, old, now) -> preferences.putBoolean(key, now));
    }

    public BooleanProperty soundProperty() {
        return sound;
    }

    public BooleanProperty motionProperty() {
        return motion;
    }

    public boolean motionOn() {
        return motion.get();
    }

    /** Plays one sound, unless sound is switched off. */
    public void play(Cue cue) {
        if (sound.get() && player != null) {
            player.play(cue, VOLUME);
        }
    }

    /** Plays the sounds that go with a batch of game events. */
    public void playEvents(List<EventDto> events) {
        for (Cue cue : SoundCues.forEvents(events.stream().map(EventDto::type).toList())) {
            play(cue);
        }
    }
}
