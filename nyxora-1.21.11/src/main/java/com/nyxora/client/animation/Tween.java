package com.nyxora.client.animation;

/**
 * Time-based tween toward a target. Driven by delta seconds, never by frame count,
 * so it behaves identically at 30 and 300 FPS. Retargeting restarts from the current value.
 */
public final class Tween {
    private final double duration;
    private final Easing easing;
    private double from, to, elapsed, value;

    public Tween(double initial, double durationSeconds, Easing easing) {
        this.duration = Math.max(1e-4, durationSeconds);
        this.easing = easing;
        this.from = this.to = this.value = initial;
        this.elapsed = this.duration;
    }

    public void setTarget(double target) {
        if (target == to) return;
        from = value;
        to = target;
        elapsed = 0;
    }

    /** Jump immediately (used when animations are disabled / reduced-motion). */
    public void snap(double target) {
        from = to = value = target;
        elapsed = duration;
    }

    public void update(double dtSeconds) {
        if (elapsed >= duration) { value = to; return; }
        elapsed = Math.min(duration, elapsed + Math.max(0, dtSeconds));
        value = from + (to - from) * easing.apply(elapsed / duration);
    }

    public double get() { return value; }
    public double target() { return to; }
    public boolean isFinished() { return elapsed >= duration; }
}
