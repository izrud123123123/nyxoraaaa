package com.nyxora.client;

import com.nyxora.client.animation.*;
import com.nyxora.client.config.*;
import com.nyxora.client.profiles.ProfileManager;
import com.nyxora.client.settings.*;
import com.nyxora.client.util.ColorUtil;

import java.nio.file.*;

/** Plain-Java self test (no Minecraft needed): run with `java -ea`. */
public class CoreSelfTest {
    static int checks = 0;
    static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError("FAILED: " + what);
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("nyxora-test");

        // defaults + clamping
        NyxoraSettings s = new NyxoraSettings();
        check(!s.skyEnabled.get(), "sky off by default");
        s.skyBrightness.set(99f);
        check(s.skyBrightness.get() == 2f, "slider clamps to max");
        s.skyBrightness.set(Float.NaN);
        check(s.skyBrightness.get() == 1f, "NaN falls back to default");
        check(!s.skyPreset.setFromString("NotAPreset"), "unknown choice rejected");
        check(s.skyPreset.setFromString("nebula") && s.skyPreset.get().equals("Nebula"), "choice parse is case-insensitive");

        // visibility rules
        s.skyPreset.set("Default");
        check(!s.skyTint.isVisible(), "tint hidden on Default sky");
        s.skyPreset.set("Mars");
        check(s.skyTint.isVisible() && !s.skyStarDensity.isVisible(), "Mars: tint yes, stars no");

        // persistence round trip
        Path cfg = tmp.resolve("nyxora.properties");
        ConfigManager cm = new ConfigManager(cfg, s);
        s.skyEnabled.set(true);
        s.skyPreset.set("Neptune");
        s.skyTint.set(0xFF112233);
        cm.saveNow();
        NyxoraSettings s2 = new NyxoraSettings();
        new ConfigManager(cfg, s2).load();
        check(s2.skyEnabled.get() && s2.skyPreset.get().equals("Neptune") && s2.skyTint.get() == 0xFF112233, "round trip");

        // debounce: one dirty mark, no write before 750ms
        Files.deleteIfExists(cfg);
        cm.markDirty(1000);
        cm.tick(1200);
        check(!Files.exists(cfg), "no write before debounce");
        cm.tick(1800);
        check(Files.exists(cfg) && !cm.isDirty(), "write after debounce");

        // malformed + partial file
        Path bad = tmp.resolve("bad.properties");
        Files.writeString(bad, "garbage line\nsky.enabled=maybe\nsky.brightness=1.5\nunknown.key=1\n=novalue\n");
        NyxoraSettings s3 = new NyxoraSettings();
        new ConfigManager(bad, s3).load();
        check(!s3.skyEnabled.get() && s3.skyBrightness.get() == 1.5f, "bad entries skipped, good ones kept");
        Path junk = tmp.resolve("junk.properties");
        Files.writeString(junk, "\u0000\u0001 not a config at all");
        new ConfigManager(junk, new NyxoraSettings()).load();
        check(Files.exists(tmp.resolve("junk.properties.bak")), "unreadable config backed up");

        // profiles
        ProfileManager pm = new ProfileManager(s, tmp.resolve("profiles"));
        check(pm.load("Cosmic").contains("Loaded") && s.skyEnabled.get() && s.skyPreset.get().equals("Galaxy"), "Cosmic built-in");
        check(pm.load("Performance").contains("Loaded") && !s.skyEnabled.get(), "Performance built-in");
        s.blurEnabled.set(true);
        check(pm.saveCurrent("My ../Setup!").contains("Saved"), "save custom profile");
        check(Files.exists(tmp.resolve("profiles").resolve("My Setup.properties")), "name sanitized, no traversal");
        pm.load("Minimal");
        check(!s.blurEnabled.get(), "Minimal clears blur");
        check(pm.load("My Setup").contains("Loaded") && s.blurEnabled.get(), "custom profile reloads");
        check(pm.saveCurrent("Default").contains("built in"), "cannot overwrite built-in");
        check(pm.names().contains("My Setup"), "listed");
        check(pm.importText("hello").contains("no NYXORA"), "import rejects junk");
        check(pm.importText(pm.export()).contains("Imported"), "export/import round trip");
        check(pm.delete("My Setup").contains("Deleted") && !pm.names().contains("My Setup"), "delete");

        // animation
        for (Easing e : Easing.values()) check(Math.abs(e.apply(0)) < 1e-9 && Math.abs(e.apply(1) - 1) < 1e-9 && Double.isFinite(e.apply(5)), "easing endpoints " + e);
        Tween a = new Tween(0, 0.2, Easing.EASE_OUT_CUBIC), b = new Tween(0, 0.2, Easing.EASE_OUT_CUBIC);
        a.setTarget(1); b.setTarget(1);
        for (int i = 0; i < 13; i++) a.update(1 / 60.0);      // 60 fps
        for (int i = 0; i < 25; i++) b.update(1 / 120.0);     // 120 fps
        check(a.isFinished() && b.isFinished() && Math.abs(a.get() - b.get()) < 1e-9, "tween frame-rate independent");
        AnimationClock c = new AnimationClock();
        check(c.tick(0) == 0 && c.tick(5_000_000_000L) == 0.1, "clock clamps stalls");

        check(ColorUtil.parse("#8B5CF6") == 0xFF8B5CF6 && ColorUtil.parse("zz") == null, "color parse");
        System.out.println("All " + checks + " checks passed");
    }
}
