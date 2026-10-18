package com.nyxora.client.animation;

/** Produces clamped delta times so a long stall (world load, alt-tab) never causes a visual jump. */
public final class AnimationClock {
    private static final double MAX_DT = 0.1;
    private long lastNanos = -1;

    public double tick(long nowNanos) {
        if (lastNanos < 0) { lastNanos = nowNanos; return 0; }
        double dt = (nowNanos - lastNanos) / 1_000_000_000.0;
        lastNanos = nowNanos;
        return Math.max(0, Math.min(MAX_DT, dt));
    }

    /** Call when the owning screen closes so the first frame after reopening has dt = 0. */
    public void reset() { lastNanos = -1; }
}
