package com.ludot.client.feedback;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SoundSynth {

    public static final float SAMPLE_RATE = 22_050f;

    static final double PEAK = 0.8;

    private static final double TWO_PI = 2 * Math.PI;

    private SoundSynth() {
    }

    public static short[] render(Cue cue) {
        return toPcm(RECIPES.get(cue).get());
    }

    // one recipe per cue; render() fails loudly (and the tests with it) if a new Cue has none
    private static final Map<Cue, Supplier<double[]>> RECIPES = recipes();

    private static Map<Cue, Supplier<double[]>> recipes() {
        Map<Cue, Supplier<double[]>> table = new EnumMap<>(Cue.class);
        table.put(Cue.CLICK, () -> tone(1_300, 28, 0.5, 70));
        table.put(Cue.DICE, SoundSynth::rattle);
        table.put(Cue.STEP, () -> mix(tone(330, 70, 0.55, 38), tone(660, 35, 0.2, 90)));
        table.put(Cue.CAPTURE, () -> mix(sweep(620, 140, 230, 0.6, 9), noise(110, 0.45, 28, 7)));
        table.put(Cue.TELEPORT, () -> mix(sweep(300, 1_400, 260, 0.45, 5), sweep(450, 2_100, 260, 0.2, 5)));
        table.put(Cue.HOME, () -> arpeggio(new double[] {523, 659, 784, 1_047}, 95, 0.5));
        table.put(Cue.YOUR_TURN, () -> concat(tone(784, 110, 0.45, 14), tone(1_047, 160, 0.45, 12)));
        table.put(Cue.WIN, SoundSynth::fanfare);
        table.put(Cue.ERROR, () -> mix(tone(150, 170, 0.5, 9), tone(158, 170, 0.35, 9), tone(300, 170, 0.2, 9)));
        table.put(Cue.JOIN, () -> concat(tone(880, 70, 0.4, 20), tone(1_175, 110, 0.4, 14)));
        return table;
    }

    // five short bursts of soft noise getting weaker, then the die settling with a low knock
    private static double[] rattle() {
        double[][] parts = new double[11][];
        double gain = 0.55;
        for (int i = 0; i < 5; i++) {
            parts[i * 2] = noise(28, gain, 70, 11L + i);
            parts[i * 2 + 1] = silence(22);
            gain *= 0.82;
        }
        parts[10] = tone(210, 90, 0.5, 30);
        return concat(parts);
    }

    private static double[] arpeggio(double[] notes, int noteMs, double gain) {
        double[][] parts = new double[notes.length][];
        for (int i = 0; i < notes.length; i++) {
            parts[i] = tone(notes[i], noteMs, gain, 16);
        }
        return concat(parts);
    }

    // a rising run, then a held major chord
    private static double[] fanfare() {
        double[] run = arpeggio(new double[] {523, 659, 784}, 100, 0.4);
        double[] chord = mix(tone(523, 520, 0.3, 4), tone(659, 520, 0.3, 4), tone(784, 520, 0.3, 4), tone(1_047, 520, 0.25, 4));
        return concat(run, chord);
    }

    // ------------------------------------------------------------------ building blocks

    static double[] tone(double hz, int ms, double gain, double decay) {
        double[] out = new double[samples(ms)];
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double phase = TWO_PI * hz * t;
            out[i] = gain * envelope(i, t, decay) * (Math.sin(phase) + 0.3 * Math.sin(2 * phase));
        }
        return out;
    }

    static double[] sweep(double fromHz, double toHz, int ms, double gain, double decay) {
        double[] out = new double[samples(ms)];
        double phase = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double hz = fromHz + (toHz - fromHz) * i / out.length;
            phase += TWO_PI * hz / SAMPLE_RATE;
            out[i] = gain * envelope(i, t, decay) * Math.sin(phase);
        }
        return out;
    }

    static double[] noise(int ms, double gain, double decay, long seed) {
        double[] out = new double[samples(ms)];
        long state = seed * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L;
        double smooth = 0;
        for (int i = 0; i < out.length; i++) {
            state = state * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L;
            double white = ((state >>> 40) / (double) (1L << 24)) * 2 - 1;
            smooth += 0.35 * (white - smooth);
            out[i] = gain * envelope(i, i / (double) SAMPLE_RATE, decay) * smooth;
        }
        return out;
    }

    static double[] silence(int ms) {
        return new double[samples(ms)];
    }

    static double[] concat(double[]... parts) {
        int length = 0;
        for (double[] part : parts) {
            length += part.length;
        }
        double[] out = new double[length];
        int at = 0;
        for (double[] part : parts) {
            System.arraycopy(part, 0, out, at, part.length);
            at += part.length;
        }
        return out;
    }

    static double[] mix(double[]... parts) {
        int length = 0;
        for (double[] part : parts) {
            length = Math.max(length, part.length);
        }
        double[] out = new double[length];
        for (double[] part : parts) {
            for (int i = 0; i < part.length; i++) {
                out[i] += part[i];
            }
        }
        return out;
    }

    // a 3 ms ramp up (no click at the start) times a fade-out
    private static double envelope(int index, double seconds, double decay) {
        double attack = Math.min(1, index / (0.003 * SAMPLE_RATE));
        return attack * Math.exp(-decay * seconds);
    }

    private static int samples(int ms) {
        return (int) Math.round(ms * SAMPLE_RATE / 1000.0);
    }

    // scales down if needed so the loudest sample sits at PEAK, then rounds to 16-bit
    private static short[] toPcm(double[] wave) {
        double loudest = 0;
        for (double sample : wave) {
            loudest = Math.max(loudest, Math.abs(sample));
        }
        double scale = loudest > PEAK ? PEAK / loudest : 1;
        short[] pcm = new short[wave.length];
        for (int i = 0; i < wave.length; i++) {
            pcm[i] = (short) Math.round(wave[i] * scale * Short.MAX_VALUE);
        }
        return pcm;
    }
}
