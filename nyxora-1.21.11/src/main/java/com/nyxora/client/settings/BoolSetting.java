package com.nyxora.client.settings;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String id, String label, String tooltip, Category cat, boolean def) {
        super(id, label, tooltip, cat, def);
    }

    @Override protected Boolean sanitize(Boolean c) { return c; }

    @Override public Boolean parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase();
        if (s.equals("true")) return Boolean.TRUE;
        if (s.equals("false")) return Boolean.FALSE;
        return null;
    }
}
