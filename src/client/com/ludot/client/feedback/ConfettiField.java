package com.ludot.client.feedback;

import java.util.Random;

/**
 * The physics of a confetti burst: pieces start above the top edge, fall with gravity, drift sideways and spin.
 * Pure maths with a seeded generator, so it can be tested; the drawing lives in the view.
 */
public final class ConfettiField {

    private static final double GRAVITY = 520;
    private static final double DRAG = 0.6;
    private static final int COLOURS = 6;

    private final double[] x;
    private final double[] y;
    private final double[] vx;
    private final double[] vy;
    private final double[] angle;
    private final double[] spin;
    private final double[] size;
    private final int[] colour;

    /** @param width how wide the area is; pieces start at random places along the top, staggered in height */
    public ConfettiField(int count, double width, long seed) {
        Random random = new Random(seed);
        x = new double[count];
        y = new double[count];
        vx = new double[count];
        vy = new double[count];
        angle = new double[count];
        spin = new double[count];
        size = new double[count];
        colour = new int[count];
        for (int i = 0; i < count; i++) {
            x[i] = random.nextDouble() * width;
            y[i] = -10 - random.nextDouble() * 300;
            vx[i] = (random.nextDouble() - 0.5) * 120;
            vy[i] = 80 + random.nextDouble() * 140;
            angle[i] = random.nextDouble() * Math.PI * 2;
            spin[i] = (random.nextDouble() - 0.5) * 16;
            size[i] = 6 + random.nextDouble() * 7;
            colour[i] = random.nextInt(COLOURS);
        }
    }

    /** Advances the simulation by {@code seconds}. */
    public void step(double seconds) {
        for (int i = 0; i < x.length; i++) {
            vy[i] += GRAVITY * seconds;
            vx[i] -= vx[i] * DRAG * seconds;
            x[i] += vx[i] * seconds;
            y[i] += vy[i] * seconds;
            angle[i] += spin[i] * seconds;
        }
    }

    /** True once every piece has fallen past the bottom edge. */
    public boolean finished(double height) {
        for (double top : y) {
            if (top < height + 30) {
                return false;
            }
        }
        return true;
    }

    public int count() {
        return x.length;
    }

    public double x(int i) {
        return x[i];
    }

    public double y(int i) {
        return y[i];
    }

    public double angle(int i) {
        return angle[i];
    }

    public double size(int i) {
        return size[i];
    }

    /** Which of the six palette colours piece {@code i} uses. */
    public int colour(int i) {
        return colour[i];
    }
}
