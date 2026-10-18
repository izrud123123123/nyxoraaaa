package com.nyxora.client.animation;

import java.util.function.DoubleUnaryOperator;

/** Easing curves mapping normalized time [0,1] to normalized progress [0,1]. */
public enum Easing {
    LINEAR(t -> t),
    EASE_OUT_CUBIC(t -> 1 - Math.pow(1 - t, 3)),
    EASE_IN_OUT_CUBIC(t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2),
    EASE_OUT_BACK(t -> {
        final double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    });

    private final DoubleUnaryOperator fn;

    Easing(DoubleUnaryOperator fn) { this.fn = fn; }

    /** Input is clamped, so callers never have to guard against overshoot in time. */
    public double apply(double t) {
        return fn.applyAsDouble(Math.max(0.0, Math.min(1.0, t)));
    }
}
