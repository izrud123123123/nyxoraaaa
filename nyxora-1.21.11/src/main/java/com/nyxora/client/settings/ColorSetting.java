package com.nyxora.client.settings;

import com.nyxora.client.util.ColorUtil;

/** ARGB color setting stored as #AARRGGBB. */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String id, String label, String tooltip, Category cat, int argb) {
        super(id, label, tooltip, cat, argb);
    }

    @Override protected Integer sanitize(Integer c) { return c; }
    @Override public Integer parse(String raw) { return ColorUtil.parse(raw); }
    @Override public String serialize() { return ColorUtil.format(get()); }
}
