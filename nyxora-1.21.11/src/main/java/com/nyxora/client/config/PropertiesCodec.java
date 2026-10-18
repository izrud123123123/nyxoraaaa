package com.nyxora.client.config;

import com.nyxora.client.settings.Setting;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny dependency-free "key=value" codec shared by the main config, profiles and import/export.
 * Malformed lines and unknown keys are skipped, never fatal.
 */
public final class PropertiesCodec {
    private PropertiesCodec() {}

    public static Map<String, String> parse(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String line : text.split("\\R")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            int eq = t.indexOf('=');
            if (eq <= 0) continue;
            out.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
        }
        return out;
    }

    public static String write(Collection<Setting<?>> settings, String header) {
        StringBuilder sb = new StringBuilder("# ").append(header).append('\n');
        for (Setting<?> s : settings) sb.append(s.id()).append('=').append(s.serialize()).append('\n');
        return sb.toString();
    }

    /** Applies recognised, valid entries. Returns how many settings were updated successfully. */
    public static int apply(Map<String, String> values, Map<String, Setting<?>> known) {
        int applied = 0;
        for (Map.Entry<String, String> e : values.entrySet()) {
            Setting<?> s = known.get(e.getKey());
            if (s != null && s.setFromString(e.getValue())) applied++;
        }
        return applied;
    }
}
