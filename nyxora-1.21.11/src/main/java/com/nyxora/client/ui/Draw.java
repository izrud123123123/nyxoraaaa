package com.nyxora.client.ui;

import net.minecraft.client.gui.DrawContext;

/** Shared drawing primitives + the NYXORA palette. All colors are 0xAARRGGBB. */
public final class Draw {
    private Draw() {}

    public static final int BG = 0xEE0B0B12, SIDEBAR = 0xF00E0E17, PANEL = 0xCC13131E, ROW = 0x99181824;
    public static final int ACCENT = 0xFF8B5CF6, ACCENT_2 = 0xFF3B82F6, INDIGO = 0xFF6366F1;
    public static final int TEXT = 0xFFE8E8F2, DIM = 0xFF8E8EA6, BORDER = 0x33FFFFFF;
    public static final int[] PALETTE = {0xFF8B5CF6, 0xFF7C3AED, 0xFF3B82F6, 0xFF6366F1, 0xFFA78BFA, 0xFF22D3EE, 0xFFF472B6, 0xFFFFFFFF};

    /** Multiplies the alpha channel of an ARGB color. */
    public static int fade(int argb, double alpha) {
        int a = (int) Math.round(((argb >>> 24) & 255) * Math.max(0, Math.min(1, alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Rounded rectangle built from horizontal strips (cheap, no textures, no shaders). */
    public static void round(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /** 1px rounded outline: draw outer rounded rect in border color, inner in fill color. */
    public static void roundOutlined(DrawContext c, int x, int y, int w, int h, int r, int fill, int border) {
        round(c, x, y, w, h, r, border);
        round(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    /** Restrained glow: a few expanding low-alpha rounded rects behind an element. */
    public static void glow(DrawContext c, int x, int y, int w, int h, int r, int color, double strength) {
        for (int i = 4; i >= 1; i--) {
            round(c, x - i, y - i, w + i * 2, h + i * 2, r + i, fade(color, 0.05 * strength * (5 - i)));
        }
    }
}
