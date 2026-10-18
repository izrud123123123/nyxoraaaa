package com.nyxora.client.settings;

import java.util.List;

/** Dropdown setting over a fixed list of option names. Unknown values fall back to the default. */
public final class ChoiceSetting extends Setting<String> {
    private final List<String> options;

    public ChoiceSetting(String id, String label, String tooltip, Category cat, List<String> options, String def) {
        super(id, label, tooltip, cat, options.contains(def) ? def : options.get(0));
        this.options = List.copyOf(options);
    }

    @Override protected String sanitize(String c) { return options.contains(c) ? c : defaultValue(); }

    @Override public String parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        for (String o : options) if (o.equalsIgnoreCase(s)) return o;
        return null;
    }

    public List<String> options() { return options; }
}
