package com.nyxora.client.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base class for every configurable value. A setting knows how to validate, serialize and
 * parse itself, so the UI, the config file and the profile system all share one definition.
 */
public abstract class Setting<T> {
    private final String id;
    private final String label;
    private final String tooltip;
    private final Category category;
    private final T defaultValue;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private BooleanSupplier visibleWhen = () -> true;
    private T value;

    protected Setting(String id, String label, String tooltip, Category category, T defaultValue) {
        this.id = id;
        this.label = label;
        this.tooltip = tooltip;
        this.category = category;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /** Clamp / normalize an incoming value. Must never throw. */
    protected abstract T sanitize(T candidate);

    /** Parse a stored string. Return null when it is malformed (the caller keeps the old value). */
    public abstract T parse(String raw);

    public String serialize() { return String.valueOf(value); }

    public final T get() { return value; }

    public final void set(T candidate) {
        if (candidate == null) return;
        T clean = sanitize(candidate);
        if (clean.equals(value)) return;
        value = clean;
        for (Consumer<T> l : listeners) l.accept(clean);
    }

    /** Applies a raw string from disk/import. Returns true when it was accepted. */
    public final boolean setFromString(String raw) {
        T parsed = parse(raw);
        if (parsed == null) return false;
        set(parsed);
        return true;
    }

    public final void reset() { set(defaultValue); }
    public final boolean isDefault() { return value.equals(defaultValue); }

    public final Setting<T> visibleWhen(BooleanSupplier predicate) { this.visibleWhen = predicate; return this; }
    public final boolean isVisible() { return visibleWhen.getAsBoolean(); }
    public final void addListener(Consumer<T> l) { listeners.add(l); }

    public final String id() { return id; }
    public final String label() { return label; }
    public final String tooltip() { return tooltip; }
    public final Category category() { return category; }
    public final T defaultValue() { return defaultValue; }
}
