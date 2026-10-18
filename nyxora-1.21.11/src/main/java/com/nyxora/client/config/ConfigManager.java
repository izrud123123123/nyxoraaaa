package com.nyxora.client.config;

import com.nyxora.client.settings.NyxoraSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads and saves the main config. Writes are debounced (markDirty + tick) so dragging a slider
 * never hits the disk every frame, and atomic (temp file + move) so a crash can't corrupt it.
 */
public final class ConfigManager {
    private static final Logger LOG = Logger.getLogger("NYXORA");
    private static final long DEBOUNCE_MS = 750;
    private static final long MAX_BYTES = 256 * 1024;

    private final Path file;
    private final NyxoraSettings settings;
    private boolean dirty;
    private long dirtySince;

    public ConfigManager(Path file, NyxoraSettings settings) {
        this.file = file;
        this.settings = settings;
        settings.all().values().forEach(s -> s.addListener(v -> markDirty(System.currentTimeMillis())));
    }

    /** Missing file = defaults. Corrupt file = keep defaults for bad keys, keep a .bak copy. */
    public void load() {
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > MAX_BYTES) throw new IOException("config too large");
            String text = Files.readString(file, StandardCharsets.UTF_8);
            int applied = PropertiesCodec.apply(PropertiesCodec.parse(text), settings.all());
            if (applied == 0 && !text.isBlank()) backup();
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Could not read NYXORA config, using defaults", e);
            backup();
        } finally {
            dirty = false; // loading must not trigger a rewrite
        }
    }

    public void markDirty(long nowMs) {
        if (!dirty) dirtySince = nowMs;
        dirty = true;
    }

    /** Call once per client tick. Saves only when changes have settled. */
    public void tick(long nowMs) {
        if (dirty && nowMs - dirtySince >= DEBOUNCE_MS) saveNow();
    }

    public void saveNow() {
        dirty = false;
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, PropertiesCodec.write(settings.all().values(), "NYXORA config"), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save NYXORA config", e);
        }
    }

    private void backup() {
        try {
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) { }
    }

    public boolean isDirty() { return dirty; }
}
