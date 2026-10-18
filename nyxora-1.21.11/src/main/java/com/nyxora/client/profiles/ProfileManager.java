package com.nyxora.client.profiles;

import com.nyxora.client.config.PropertiesCodec;
import com.nyxora.client.settings.NyxoraSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Built-in profiles are override maps applied on top of defaults. User profiles are full
 * snapshots stored one file per profile. Every operation returns a short message for UI feedback.
 */
public final class ProfileManager {
    public static final int MAX_NAME = 32;
    private static final int MAX_IMPORT_CHARS = 64 * 1024;

    private static final Map<String, Map<String, String>> BUILT_IN = new LinkedHashMap<>();
    static {
        BUILT_IN.put("Default", Map.of());
        BUILT_IN.put("Performance", Map.of(
                "sky.enabled", "false", "particles.master", "false", "follow.enabled", "false",
                "break.enabled", "false", "kill.enabled", "false", "blur.enabled", "false",
                "ui.animations", "false"));
        BUILT_IN.put("Cosmic", Map.of(
                "sky.enabled", "true", "sky.preset", "Galaxy", "sky.glow", "true",
                "follow.enabled", "true", "follow.style", "Cosmic Dust",
                "break.enabled", "true", "kill.enabled", "true", "blur.enabled", "false"));
        BUILT_IN.put("Minimal", Map.of(
                "sky.enabled", "false", "follow.enabled", "false", "break.enabled", "false",
                "kill.enabled", "false", "blur.enabled", "false", "ui.sounds", "false"));
    }

    private final NyxoraSettings settings;
    private final Path dir;
    private String active = "Default";

    public ProfileManager(NyxoraSettings settings, Path profileDir) {
        this.settings = settings;
        this.dir = profileDir;
    }

    public String active() { return active; }

    public List<String> names() {
        List<String> out = new ArrayList<>(BUILT_IN.keySet());
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                s.map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".properties"))
                        .map(n -> n.substring(0, n.length() - ".properties".length()))
                        .filter(n -> !BUILT_IN.containsKey(n))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .forEach(out::add);
            } catch (IOException ignored) { }
        }
        return out;
    }

    public boolean isBuiltIn(String name) { return BUILT_IN.containsKey(name); }

    /** Letters, digits, space, '-' and '_' only; prevents path traversal. Returns null if unusable. */
    public static String sanitizeName(String raw) {
        if (raw == null) return null;
        String n = raw.trim().replaceAll("[^A-Za-z0-9 _-]", "");
        if (n.length() > MAX_NAME) n = n.substring(0, MAX_NAME).trim();
        return n.isEmpty() ? null : n;
    }

    public String load(String name) {
        Map<String, String> overrides;
        if (BUILT_IN.containsKey(name)) {
            overrides = BUILT_IN.get(name);
        } else {
            String clean = sanitizeName(name);
            if (clean == null) return "Invalid profile name";
            try {
                overrides = PropertiesCodec.parse(Files.readString(fileFor(clean), StandardCharsets.UTF_8));
            } catch (IOException e) {
                return "Profile \"" + name + "\" could not be read";
            }
        }
        settings.resetAll();
        PropertiesCodec.apply(overrides, settings.all());
        active = name;
        return "Loaded profile \"" + name + "\"";
    }

    public String saveCurrent(String rawName) {
        String name = sanitizeName(rawName);
        if (name == null) return "Enter a valid profile name";
        if (BUILT_IN.containsKey(name)) return "\"" + name + "\" is built in and cannot be overwritten";
        try {
            Files.createDirectories(dir);
            Files.writeString(fileFor(name), PropertiesCodec.write(settings.all().values(), "NYXORA profile"), StandardCharsets.UTF_8);
            active = name;
            return "Saved profile \"" + name + "\"";
        } catch (IOException e) {
            return "Could not save profile";
        }
    }

    public String delete(String name) {
        if (BUILT_IN.containsKey(name)) return "Built-in profiles cannot be deleted";
        String clean = sanitizeName(name);
        if (clean == null) return "Invalid profile name";
        try {
            Files.deleteIfExists(fileFor(clean));
            if (active.equals(name)) active = "Default";
            return "Deleted profile \"" + name + "\"";
        } catch (IOException e) {
            return "Could not delete profile";
        }
    }

    public String export() {
        return PropertiesCodec.write(settings.all().values(), "NYXORA profile export");
    }

    /** Validates before touching anything: size cap, at least one recognised key. */
    public String importText(String text) {
        if (text == null || text.isBlank()) return "Nothing to import";
        if (text.length() > MAX_IMPORT_CHARS) return "Import is too large";
        Map<String, String> values = PropertiesCodec.parse(text);
        boolean anyKnown = values.keySet().stream().anyMatch(k -> settings.find(k) != null);
        if (!anyKnown) return "Import contains no NYXORA settings";
        int applied = PropertiesCodec.apply(values, settings.all());
        active = "Custom";
        return "Imported " + applied + " settings";
    }

    private Path fileFor(String cleanName) { return dir.resolve(cleanName + ".properties"); }
}
