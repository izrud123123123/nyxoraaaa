package com.nyxora.client.effects;

import com.nyxora.client.particles.ParticleController;
import com.nyxora.client.settings.NyxoraSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Best-effort kill detection, purely client-side:
 *  1. remember the last player the local player attacked (UUID + tick),
 *  2. fire once when that player is seen with health <= 0 within a short window.
 * LIMITATIONS (by design, documented in README): a kill by a bystander right after our hit can look like
 * ours; if the victim is out of render range or the server does not sync health before removal, no
 * effect plays. Each victim UUID triggers at most once per window. Combat mechanics are never touched.
 */
public final class KillEffects {
    private static final int WINDOW_TICKS = 100;      // 5s after our last hit
    private static final int COOLDOWN_TICKS = 40;     // don't retrigger on the same UUID

    private final NyxoraSettings s;
    private final ParticleController particles;
    private final Map<UUID, Integer> lastHit = new HashMap<>();
    private final Map<UUID, Integer> fired = new HashMap<>();
    private int tick;

    public KillEffects(NyxoraSettings settings, ParticleController particles) {
        this.s = settings;
        this.particles = particles;
    }

    /** Hook from AttackEntityCallback (client side). */
    public void onAttack(Entity target) {
        if (target instanceof PlayerEntity) lastHit.put(target.getUuid(), tick);
    }

    public void tick(MinecraftClient mc) {
        tick++;
        if (mc.world == null || lastHit.isEmpty()) return;
        lastHit.entrySet().removeIf(e -> tick - e.getValue() > WINDOW_TICKS);
        fired.entrySet().removeIf(e -> tick - e.getValue() > COOLDOWN_TICKS);
        if (!s.killEnabled.get()) return;
        for (PlayerEntity other : mc.world.getPlayers()) {
            if (!lastHit.containsKey(other.getUuid()) || fired.containsKey(other.getUuid())) continue;
            if (other == mc.player) continue;
            if (other.getHealth() <= 0f || other.isDead()) {
                fired.put(other.getUuid(), tick);
                lastHit.remove(other.getUuid());
                play(mc, other.getEntityPos().add(0, other.getHeight() * 0.5, 0));
            }
        }
    }

    public void play(MinecraftClient mc, Vec3d at) {
        int color = s.killColor.get();
        float k = s.killIntensity.get();
        int life = Math.max(4, (int) (s.killDuration.get() * 20));
        String preset = s.killPreset.get();
        java.util.Random r = new java.util.Random();
        ParticleEffect main = switch (preset) {
            case "Blue Energy Burst" -> ParticleTypes.ELECTRIC_SPARK;
            case "Glowing Stars" -> ParticleTypes.END_ROD;
            case "Cosmic Particles", "Magical Disappear" -> ParticleTypes.ENCHANT;
            case "Lightning" -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.END_ROD;
        };
        if (s.killParts.get()) {
            int n = Math.round(24 * k);
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * Math.PI * 2, up = (r.nextDouble() - 0.3) * 0.25, sp = 0.1 + r.nextDouble() * 0.15;
                if (preset.equals("Lightning")) { a = r.nextDouble() * 0.5; up = 0.3 + r.nextDouble() * 0.4; sp = 0.02; }
                particles.spawn(mc, main, at.x, at.y, at.z, Math.cos(a) * sp * k, up, Math.sin(a) * sp * k, 1f, life, color, 1f);
            }
        }
        if (s.killRing.get() || preset.equals("Ring Burst")) {
            int n = Math.round(32 * k);
            for (int i = 0; i < n; i++) {
                double a = (Math.PI * 2 * i) / n;
                particles.spawn(mc, ParticleTypes.END_ROD, at.x, at.y, at.z, Math.cos(a) * 0.25, 0.0, Math.sin(a) * 0.25, 0.9f, life, color, 1f);
            }
        }
        if (s.killFlash.get()) {
            // Flash = one oversized, very short-lived full-bright particle (avoids version-specific FLASH effect API).
            particles.spawn(mc, ParticleTypes.END_ROD, at.x, at.y, at.z, 0, 0, 0, 4f, 4, 0xFFFFFFFF, 0.9f);
        }
    }

    public void reset() { lastHit.clear(); fired.clear(); }
}
