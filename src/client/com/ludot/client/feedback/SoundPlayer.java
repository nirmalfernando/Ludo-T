package com.ludot.client.feedback;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;

// every audio call happens on one background thread, so opening a sound line (which can take a moment) never stalls the UI
final class SoundPlayer {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "sound");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Cue, Clip> clips = new EnumMap<>(Cue.class);
    private volatile boolean unavailable;

    // opens every sound ahead of time so the first real cue is not late
    void warmUp() {
        worker.execute(() -> {
            for (Cue cue : Cue.values()) {
                if (!unavailable) {
                    clipFor(cue);
                }
            }
        });
    }

    void play(Cue cue, double volume) {
        if (!unavailable) {
            worker.execute(() -> playNow(cue, volume));
        }
    }

    private void playNow(Cue cue, double volume) {
        Clip clip = clipFor(cue);
        if (clip == null) {
            return;
        }
        try {
            setVolume(clip, volume);
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (RuntimeException e) {
            unavailable = true;
        }
    }

    private Clip clipFor(Cue cue) {
        Clip existing = clips.get(cue);
        if (existing != null) {
            return existing;
        }
        try {
            short[] samples = SoundSynth.render(cue);
            byte[] bytes = new byte[samples.length * 2];
            for (int i = 0; i < samples.length; i++) { // little-endian 16-bit
                bytes[2 * i] = (byte) samples[i];
                bytes[2 * i + 1] = (byte) (samples[i] >> 8);
            }
            Clip clip = AudioSystem.getClip();
            clip.open(new AudioFormat(SoundSynth.SAMPLE_RATE, 16, 1, true, false), bytes, 0, bytes.length);
            clips.put(cue, clip);
            return clip;
        } catch (LineUnavailableException | RuntimeException e) {
            unavailable = true; // no audio device, or the system refused: stay quiet from now on
            return null;
        }
    }

    private static void setVolume(Clip clip, double volume) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            double decibels = 20 * Math.log10(Math.max(volume, 0.001));
            gain.setValue((float) Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
        }
    }
}
