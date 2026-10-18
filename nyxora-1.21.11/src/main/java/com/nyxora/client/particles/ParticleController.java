package com.nyxora.client.particles;

import com.nyxora.client.settings.NyxoraSettings;
import com.nyxora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Spawns vanilla particle objects (so rendering, culling and cleanup stay vanilla) and then tunes
 * size / lifetime / tint on the returned Particle. A per-second budget caps spawn counts, and
 * nothing spawns when the master switch is off or the game's particle option is "Minimal".
 *
 * UNVERIFIED on 1.21.11: ParticleManager#addParticle returning Particle, Particle#scale/setMaxAge/
 * setColor/setAlpha, DustParticleEffect(int,float).
 * LIMITATION: "glow" is binary (full-bright END_ROD/GLOW types vs. lit dust) plus alpha scaling.
 */
public final class ParticleController {
    private static final double TICK_SECONDS = 0.05;
    private static final int PER_SECOND_CAP = 400;

    private final NyxoraSettings s;
    private final Random rnd = new Random();
    private double followAccumulator;
    private int budget = PER_SECOND_CAP;
    private int budgetTicks;
    private double orbitAngle;

    public ParticleController(NyxoraSettings settings) { this.s = settings; }

    /** Called every client tick (20/s). */
    public void tick(MinecraftClient mc) {
        if (++budgetTicks >= 20) { budgetTicks = 0; budget = PER_SECOND_CAP; }
        if (!s.particlesMaster.get() || !s.followEnabled.get()) { followAccumulator = 0; return; }
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.isPaused()) return;
        if (mc.options.getParticles().getValue() == ParticlesMode.MINIMAL) return;
        if (mc.options.getPerspective().isFirstPerson() && s.followSpread.get() < 0.3f) return;

        followAccumulator += s.followRate.get() * s.followDensity.get() * TICK_SECONDS;
        int count = (int) followAccumulator;
        followAccumulator -= count;
        count = Math.min(count, 8); // never burst a backlog after a lag spike
        orbitAngle += 0.25 * Math.max(0.1f, s.followSpeed.get());

        for (int i = 0; i < count && budget > 0; i++) {
            Vec3d base = p.getEntityPos(); // UNVERIFIED name; older: p.getPos()
            double r = s.followSpread.get() * Math.sqrt(rnd.nextDouble());
            double a = rnd.nextDouble() * Math.PI * 2;
            double x = base.x + Math.cos(a) * r;
            double z = base.z + Math.sin(a) * r;
            double y = base.y + rnd.nextDouble() * p.getHeight();
            double sp = 0.05 * s.followSpeed.get();
            double vx, vy, vz;
            switch (s.followAnimation.get()) {
                case "Orbit" -> { vx = -Math.sin(orbitAngle + a) * sp; vz = Math.cos(orbitAngle + a) * sp; vy = 0; }
                case "Rise" -> { vx = 0; vz = 0; vy = sp; }
                case "Swirl" -> { vx = -Math.sin(orbitAngle + a) * sp; vz = Math.cos(orbitAngle + a) * sp; vy = sp * 0.6; }
                default -> { vx = (rnd.nextDouble() - 0.5) * sp; vy = (rnd.nextDouble() - 0.5) * sp; vz = (rnd.nextDouble() - 0.5) * sp; }
            }
            vy -= s.followGravity.get() * 0.04;
            int color = styleColor(s.followStyle.get(), s.followColor.get());
            boolean glow = s.followGlow.get() >= 0.5f;
            spawn(mc, effectFor(s.followStyle.get(), glow, color, s.followSize.get()), x, y, z, vx, vy, vz,
                    s.followSize.get(), (int) (s.followLifetime.get() * 20), color, 0.4f + 0.6f * s.followGlow.get());
        }
    }

    /** Burst for a broken block at its center. */
    public void onBlockBroken(MinecraftClient mc, BlockPos pos) {
        if (!s.particlesMaster.get() || !s.breakEnabled.get() || mc.world == null) return;
        if (mc.options.getParticles().getValue() == ParticlesMode.MINIMAL) return;
        int color = styleColor(s.breakStyle.get(), s.breakColor.get());
        int n = Math.round(s.breakAmount.get());
        double vel = 0.12 * s.breakVelocity.get();
        for (int i = 0; i < n && budget > 0; i++) {
            double vx = (rnd.nextDouble() - 0.5) * 2 * vel, vy = rnd.nextDouble() * vel * 1.2, vz = (rnd.nextDouble() - 0.5) * 2 * vel;
            spawn(mc, effectFor(s.breakStyle.get(), s.breakGlow.get(), color, s.breakSize.get()),
                    pos.getX() + 0.5 + (rnd.nextDouble() - 0.5) * 0.8, pos.getY() + 0.5 + (rnd.nextDouble() - 0.5) * 0.8,
                    pos.getZ() + 0.5 + (rnd.nextDouble() - 0.5) * 0.8, vx, vy, vz,
                    s.breakSize.get(), (int) (s.breakLifetime.get() * 20), color, 1f);
        }
    }

    /** Shared spawn path used by kill effects too. Returns false if blocked by limits. */
    public boolean spawn(MinecraftClient mc, ParticleEffect effect, double x, double y, double z,
                         double vx, double vy, double vz, float size, int lifeTicks, int argb, float alpha) {
        if (budget <= 0 || mc.world == null) return false;
        budget--;
        Particle particle = mc.particleManager.addParticle(effect, x, y, z, vx, vy, vz);
        if (particle != null) {
            particle.scale(Math.max(0.1f, size));
            particle.setMaxAge(Math.max(2, lifeTicks));
            particle.setColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f);
            particle.setAlpha(Math.max(0f, Math.min(1f, alpha)));
        }
        return true;
    }

    public ParticleEffect effectFor(String style, boolean glow, int argb, float size) {
        if (!glow) return new DustParticleEffect(argb & 0xFFFFFF, Math.max(0.3f, size));
        return switch (style) {
            case "Glowing Stars", "Small Stars" -> ParticleTypes.END_ROD;
            case "Purple Sparks", "Blue Fragments", "Blue Particles" -> ParticleTypes.ELECTRIC_SPARK;
            case "Cosmic Dust", "Glowing Dust" -> ParticleTypes.GLOW;
            case "Glowing Orbs" -> ParticleTypes.GLOW;
            case "Magical Trail", "Cosmic Fragments" -> ParticleTypes.ENCHANT;
            default -> ParticleTypes.END_ROD;
        };
    }

    public static int styleColor(String style, int custom) {
        return switch (style) {
            case "Purple Sparks" -> 0xFF8B5CF6;
            case "Blue Particles", "Blue Fragments" -> 0xFF3B82F6;
            default -> custom;
        };
    }

    /** Clears per-world state; call on disconnect / world change. */
    public void reset() { followAccumulator = 0; budget = PER_SECOND_CAP; budgetTicks = 0; }
}
