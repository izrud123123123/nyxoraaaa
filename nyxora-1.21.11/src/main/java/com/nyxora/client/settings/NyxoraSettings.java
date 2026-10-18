package com.nyxora.client.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.nyxora.client.settings.Category.*;

/**
 * Single registry of every setting. Rendering code reads values from here; it never owns state.
 * Adding a feature = adding a field here; UI, config and profiles pick it up automatically.
 */
public final class NyxoraSettings {
    public static final List<String> SKY_PRESETS = List.of(
            "Galaxy", "Milky Way", "Deep Space", "Nebula", "Mars", "Neptune",
            "Moonlit Night", "Starfield", "Cosmic Purple", "Aurora Borealis", "Default");
    private static final Set<String> HAS_STARS = Set.of(
            "Galaxy", "Milky Way", "Deep Space", "Nebula", "Moonlit Night", "Starfield", "Cosmic Purple");
    private static final Set<String> HAS_NEBULA = Set.of(
            "Galaxy", "Milky Way", "Deep Space", "Nebula", "Cosmic Purple", "Neptune");

    private final Map<String, Setting<?>> byId = new LinkedHashMap<>();

    // ---- Sky ----
    public final BoolSetting skyEnabled = add(new BoolSetting("sky.enabled", "Custom Sky", "Replace the vanilla sky with a NYXORA sky.", SKY, false));
    public final ChoiceSetting skyPreset = add(new ChoiceSetting("sky.preset", "Preset", "Which sky to render.", SKY, SKY_PRESETS, "Galaxy"));
    public final FloatSetting skyBrightness = add(new FloatSetting("sky.brightness", "Brightness", "Overall sky brightness.", SKY, 1f, 0.2f, 2f, 0.05f, "x"));
    public final FloatSetting skyOpacity = add(new FloatSetting("sky.opacity", "Opacity", "Blend between vanilla sky and custom sky.", SKY, 1f, 0f, 1f, 0.05f, ""));
    public final FloatSetting skyRotation = add(new FloatSetting("sky.rotation", "Rotation", "Static rotation offset in degrees.", SKY, 0f, 0f, 360f, 1f, "\u00B0"));
    public final FloatSetting skyRotationSpeed = add(new FloatSetting("sky.rotationSpeed", "Rotation Speed", "Degrees per second.", SKY, 0.3f, 0f, 5f, 0.1f, "\u00B0/s"));
    public final FloatSetting skyStarDensity = add(new FloatSetting("sky.starDensity", "Star Density", "Amount of stars.", SKY, 1f, 0f, 2f, 0.05f, "x"));
    public final FloatSetting skyNebulaIntensity = add(new FloatSetting("sky.nebulaIntensity", "Nebula Intensity", "Strength of gas clouds.", SKY, 1f, 0f, 2f, 0.05f, "x"));
    public final ColorSetting skyTint = add(new ColorSetting("sky.tint", "Color Tint", "Multiplies the sky color.", SKY, 0xFFFFFFFF));
    public final BoolSetting skyGlow = add(new BoolSetting("sky.glow", "Atmospheric Glow", "Soft glow near the horizon.", SKY, true));
    public final BoolSetting skyAnimate = add(new BoolSetting("sky.animate", "Animation", "Animate twinkling and drift.", SKY, true));

    // ---- Particles ----
    public final BoolSetting particlesMaster = add(new BoolSetting("particles.master", "Particles", "Master switch for all NYXORA particles.", PARTICLES, true));
    public final IntLimit particleCap = new IntLimit();
    public final BoolSetting followEnabled = add(new BoolSetting("follow.enabled", "Player Particles", "Particles that trail the player.", PARTICLES, false));
    public final ChoiceSetting followStyle = add(new ChoiceSetting("follow.style", "Style", "Look of the particles.", PARTICLES,
            List.of("Glowing Stars", "Purple Sparks", "Blue Particles", "Cosmic Dust", "Glowing Orbs", "Magical Trail", "Soft Energy"), "Cosmic Dust"));
    public final ColorSetting followColor = add(new ColorSetting("follow.color", "Color", "Particle color.", PARTICLES, 0xFF8B5CF6));
    public final FloatSetting followSize = add(new FloatSetting("follow.size", "Size", "Particle size.", PARTICLES, 1f, 0.2f, 3f, 0.1f, "x"));
    public final FloatSetting followDensity = add(new FloatSetting("follow.density", "Density", "How many particles are alive.", PARTICLES, 1f, 0.1f, 3f, 0.1f, "x"));
    public final FloatSetting followRate = add(new FloatSetting("follow.rate", "Spawn Rate", "Particles per second.", PARTICLES, 12f, 1f, 60f, 1f, "/s"));
    public final FloatSetting followLifetime = add(new FloatSetting("follow.lifetime", "Lifetime", "Seconds before a particle fades.", PARTICLES, 1.5f, 0.2f, 5f, 0.1f, "s"));
    public final FloatSetting followSpread = add(new FloatSetting("follow.spread", "Spread", "Radius around the player.", PARTICLES, 0.8f, 0.1f, 3f, 0.1f, " m"));
    public final FloatSetting followSpeed = add(new FloatSetting("follow.speed", "Speed", "Drift speed.", PARTICLES, 0.3f, 0f, 2f, 0.05f, "x"));
    public final FloatSetting followGravity = add(new FloatSetting("follow.gravity", "Gravity", "Negative floats upward.", PARTICLES, 0f, -1f, 1f, 0.05f, ""));
    public final FloatSetting followGlow = add(new FloatSetting("follow.glow", "Glow", "Glow intensity where supported.", PARTICLES, 0.6f, 0f, 1f, 0.05f, ""));
    public final ChoiceSetting followAnimation = add(new ChoiceSetting("follow.animation", "Animation", "Movement pattern.", PARTICLES,
            List.of("Drift", "Orbit", "Rise", "Swirl"), "Drift"));
    public final BoolSetting breakEnabled = add(new BoolSetting("break.enabled", "Block Break Particles", "Extra particles when breaking blocks.", PARTICLES, false));
    public final ChoiceSetting breakStyle = add(new ChoiceSetting("break.style", "Style", "Look of the break effect.", PARTICLES,
            List.of("Purple Sparks", "Blue Fragments", "Glowing Dust", "Small Stars", "Cosmic Fragments", "Custom Debris"), "Purple Sparks"));
    public final ColorSetting breakColor = add(new ColorSetting("break.color", "Color", "Particle color.", PARTICLES, 0xFF7C3AED));
    public final FloatSetting breakAmount = add(new FloatSetting("break.amount", "Amount", "Particles per broken block.", PARTICLES, 12f, 1f, 40f, 1f, ""));
    public final FloatSetting breakSize = add(new FloatSetting("break.size", "Size", "Particle size.", PARTICLES, 1f, 0.2f, 3f, 0.1f, "x"));
    public final FloatSetting breakLifetime = add(new FloatSetting("break.lifetime", "Lifetime", "Seconds.", PARTICLES, 0.8f, 0.2f, 3f, 0.1f, "s"));
    public final FloatSetting breakVelocity = add(new FloatSetting("break.velocity", "Velocity", "Outward speed.", PARTICLES, 0.5f, 0.1f, 2f, 0.05f, "x"));
    public final BoolSetting breakGlow = add(new BoolSetting("break.glow", "Glow", "Use bright particles.", PARTICLES, true));

    // ---- Combat ----
    public final BoolSetting killEnabled = add(new BoolSetting("kill.enabled", "Kill Effects", "Visuals when you defeat a player (client-side detection, best effort).", COMBAT, false));
    public final ChoiceSetting killPreset = add(new ChoiceSetting("kill.preset", "Preset", "Effect style.", COMBAT,
            List.of("Purple Explosion", "Blue Energy Burst", "Glowing Stars", "Cosmic Particles", "Lightning", "Ring Burst", "Magical Disappear"), "Purple Explosion"));
    public final FloatSetting killIntensity = add(new FloatSetting("kill.intensity", "Intensity", "Particle amount multiplier.", COMBAT, 1f, 0.2f, 3f, 0.1f, "x"));
    public final FloatSetting killDuration = add(new FloatSetting("kill.duration", "Duration", "Effect length in seconds.", COMBAT, 1.2f, 0.3f, 4f, 0.1f, "s"));
    public final ColorSetting killColor = add(new ColorSetting("kill.color", "Color", "Primary color.", COMBAT, 0xFF8B5CF6));
    public final BoolSetting killParts = add(new BoolSetting("kill.part.particles", "Particles Component", "Burst particles.", COMBAT, true));
    public final BoolSetting killRing = add(new BoolSetting("kill.part.ring", "Ring Component", "Expanding ring.", COMBAT, true));
    public final BoolSetting killFlash = add(new BoolSetting("kill.part.flash", "Flash Component", "Brief light flash.", COMBAT, true));

    // ---- Motion ----
    public final BoolSetting blurEnabled = add(new BoolSetting("blur.enabled", "Motion Blur", "Cinematic camera blur.", MOTION, false));
    public final FloatSetting blurIntensity = add(new FloatSetting("blur.intensity", "Intensity", "Blend with previous frame.", MOTION, 0.35f, 0.05f, 0.9f, 0.05f, ""));
    public final ChoiceSetting blurQuality = add(new ChoiceSetting("blur.quality", "Quality", "Higher quality costs more GPU time.", MOTION, List.of("Low", "Medium", "High"), "Medium"));

    // ---- Interface ----
    public final BoolSetting soundsEnabled = add(new BoolSetting("ui.sounds", "UI Sounds", "Click, toggle and menu sounds.", INTERFACE, true));
    public final FloatSetting soundsVolume = add(new FloatSetting("ui.volume", "UI Volume", "Volume of UI sounds.", INTERFACE, 0.5f, 0f, 1f, 0.05f, ""));
    public final BoolSetting animationsEnabled = add(new BoolSetting("ui.animations", "Animations", "Disable for reduced motion.", INTERFACE, true));
    public final BoolSetting tooltipsEnabled = add(new BoolSetting("ui.tooltips", "Tooltips", "Show setting descriptions.", INTERFACE, true));
    public final FloatSetting uiScale = add(new FloatSetting("ui.scale", "Menu Scale", "Scale of the NYXORA menu.", INTERFACE, 1f, 0.6f, 1.6f, 0.05f, "x"));

    public NyxoraSettings() {
        // Only show sliders that apply to the chosen preset (spec: don't show meaningless options).
        skyStarDensity.visibleWhen(() -> HAS_STARS.contains(skyPreset.get()));
        skyNebulaIntensity.visibleWhen(() -> HAS_NEBULA.contains(skyPreset.get()));
        skyTint.visibleWhen(() -> !skyPreset.get().equals("Default"));
        skyGlow.visibleWhen(() -> !skyPreset.get().equals("Default"));
        skyBrightness.visibleWhen(() -> !skyPreset.get().equals("Default"));
    }

    /** Hard cap on live NYXORA particles; not user-exposed as a setting id to keep the config small. */
    public static final class IntLimit { public final int max = 600; }

    private <S extends Setting<?>> S add(S s) {
        if (byId.put(s.id(), s) != null) throw new IllegalStateException("Duplicate setting id " + s.id());
        return s;
    }

    public Map<String, Setting<?>> all() { return Collections.unmodifiableMap(byId); }

    public Setting<?> find(String id) { return byId.get(id); }

    public List<Setting<?>> inCategory(Category c) {
        List<Setting<?>> out = new ArrayList<>();
        for (Setting<?> s : byId.values()) if (s.category() == c) out.add(s);
        return out;
    }

    public void resetAll() { for (Setting<?> s : byId.values()) s.reset(); }
}
