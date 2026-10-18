package com.nyxora.client;

import com.nyxora.client.audio.UiSounds;
import com.nyxora.client.config.ConfigManager;
import com.nyxora.client.effects.KillEffects;
import com.nyxora.client.particles.ParticleController;
import com.nyxora.client.profiles.ProfileManager;
import com.nyxora.client.settings.NyxoraSettings;
import com.nyxora.client.ui.NyxoraScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.event.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

/**
 * Client entry point: wires settings, config, profiles, sounds, particles and kill effects together.
 * Fabric API names used here are UNVERIFIED against the 1.21.11 jars (no network in the authoring sandbox):
 * KeyBinding.Category.create, ClientPlayerBlockBreakEvents.AFTER, AttackEntityCallback.
 */
public final class NyxoraClient implements ClientModInitializer {
    public static final String MOD_ID = "nyxora";

    private NyxoraSettings settings;
    private ConfigManager config;
    private ProfileManager profiles;
    private UiSounds sounds;
    private ParticleController particles;
    private KillEffects kills;
    private KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        settings = new NyxoraSettings();
        config = new ConfigManager(configDir.resolve("nyxora.properties"), settings);
        config.load();
        profiles = new ProfileManager(settings, configDir.resolve("nyxora").resolve("profiles"));
        sounds = new UiSounds(settings);
        particles = new ParticleController(settings);
        kills = new KillEffects(settings, particles);

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nyxora.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> config.saveNow());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { particles.reset(); kills.reset(); });
        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> {
            if (player == MinecraftClient.getInstance().player) particles.onBlockBroken(MinecraftClient.getInstance(), pos);
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && player == MinecraftClient.getInstance().player) kills.onAttack(entity);
            return ActionResult.PASS; // never alter combat
        });
    }

    private void onTick(MinecraftClient mc) {
        config.tick(System.currentTimeMillis());
        // wasPressed() is edge-triggered: holding the key does not re-open the menu.
        while (openKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new NyxoraScreen(settings, config, profiles, sounds));
        }
        particles.tick(mc);
        kills.tick(mc);
    }
}
