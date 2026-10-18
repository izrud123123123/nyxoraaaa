package com.nyxora.client.settings;

/** Numeric slider setting. Values are clamped to [min,max] and snapped to {@code step}. */
public final class FloatSetting extends Setting<Float> {
    private final float min, max, step;
    private final String unit;

    public FloatSetting(String id, String label, String tooltip, Category cat,
                        float def, float min, float max, float step, String unit) {
        super(id, label, tooltip, cat, Math.max(min, Math.min(max, def)));
        this.min = min; this.max = max; this.step = step; this.unit = unit;
    }

    @Override protected Float sanitize(Float c) {
        float v = c;
        if (Float.isNaN(v) || Float.isInfinite(v)) return defaultValue();
        if (step > 0) v = min + Math.round((v - min) / step) * step;
        // Round away float drift from snapping (1.5000001 -> 1.5) so saved files stay clean and equality works.
        v = Math.round(v * 10000f) / 10000f;
        return Math.max(min, Math.min(max, v));
    }

    @Override public Float parse(String raw) {
        try { return raw == null ? null : Float.valueOf(raw.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    public float min() { return min; }
    public float max() { return max; }
    public float step() { return step; }

    /** Position of the current value on the slider track, 0..1. */
    public float normalized() { return max == min ? 0 : (get() - min) / (max - min); }

    /** Human-readable value for the slider label, e.g. "1.25x" or "40%". */
    public String display() {
        float v = get();
        String num = step >= 1 ? String.valueOf(Math.round(v)) : String.format("%.2f", v);
        return num + unit;
    }
}
