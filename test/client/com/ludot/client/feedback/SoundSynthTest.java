package com.ludot.client.feedback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SoundSynth (the sounds are made from maths, so they can be tested without a sound card)")
class SoundSynthTest {

    private static double seconds(short[] samples) {
        return samples.length / (double) SoundSynth.SAMPLE_RATE;
    }

    private static int peak(short[] samples) {
        int loudest = 0;
        for (short sample : samples) {
            loudest = Math.max(loudest, Math.abs(sample));
        }
        return loudest;
    }

    @Test @DisplayName("every cue renders a short, audible sound")
    void everyCueIsAudibleAndShort() {
        for (Cue cue : Cue.values()) {
            short[] samples = SoundSynth.render(cue);
            assertTrue(seconds(samples) >= 0.02, cue + " is too short to hear");
            assertTrue(seconds(samples) <= 1.2, cue + " is too long for a game sound");
            assertTrue(peak(samples) > 2_000, cue + " is practically silent");
        }
    }

    @Test @DisplayName("no cue ever reaches full scale, so nothing clips or crackles")
    void neverClips() {
        int ceiling = (int) Math.ceil(SoundSynth.PEAK * Short.MAX_VALUE);
        for (Cue cue : Cue.values()) {
            assertTrue(peak(SoundSynth.render(cue)) <= ceiling, cue + " is louder than the ceiling");
        }
    }

    @Test @DisplayName("rendering is deterministic: the same cue gives the same samples every time")
    void deterministic() {
        for (Cue cue : Cue.values()) {
            assertArrayEquals(SoundSynth.render(cue), SoundSynth.render(cue), cue + " changed between renders");
        }
    }

    @Test @DisplayName("every cue sounds different from every other")
    void cuesAreDistinct() {
        List<short[]> all = new ArrayList<>();
        for (Cue cue : Cue.values()) {
            short[] samples = SoundSynth.render(cue);
            for (short[] other : all) {
                assertFalse(Arrays.equals(samples, other), cue + " is identical to another cue");
            }
            all.add(samples);
        }
    }

    @Test @DisplayName("a sound starts and ends near silence, so it does not click")
    void startsAndEndsSoftly() {
        for (Cue cue : Cue.values()) {
            short[] samples = SoundSynth.render(cue);
            assertTrue(Math.abs(samples[0]) < 1_500, cue + " starts with a click");
            assertTrue(Math.abs(samples[samples.length - 1]) < 4_000, cue + " ends abruptly");
        }
    }

    @Test @DisplayName("the win fanfare is the longest cue and the click the shortest")
    void relativeLengths() {
        assertEquals(Cue.WIN, longest());
        assertEquals(Cue.CLICK, shortest());
    }

    private static Cue longest() {
        Cue best = Cue.CLICK;
        for (Cue cue : Cue.values()) {
            if (SoundSynth.render(cue).length > SoundSynth.render(best).length) {
                best = cue;
            }
        }
        return best;
    }

    private static Cue shortest() {
        Cue best = Cue.WIN;
        for (Cue cue : Cue.values()) {
            if (SoundSynth.render(cue).length < SoundSynth.render(best).length) {
                best = cue;
            }
        }
        return best;
    }
}
