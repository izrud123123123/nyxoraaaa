package com.nyxora.client.util;

/** ARGB integer helpers (0xAARRGGBB). */
public final class ColorUtil {
    private ColorUtil() {}

    public static int argb(int a, int r, int g, int b) {
        return (clamp8(a) << 24) | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b);
    }

    public static int withAlpha(int color, float alpha) {
        return (clamp8(Math.round(alpha * 255f)) << 24) | (color & 0xFFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int al = Math.round((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return argb(al, r, g, bl);
    }

    /** Parses "#RRGGBB", "RRGGBB" or "#AARRGGBB"; returns null when malformed. */
    public static Integer parse(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6 && s.length() != 8) return null;
        try {
            long v = Long.parseLong(s, 16);
            return s.length() == 6 ? (int) (0xFF000000L | v) : (int) v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String format(int argb) {
        return String.format("#%08X", argb);
    }

    private static int clamp8(int v) { return Math.max(0, Math.min(255, v)); }
}
