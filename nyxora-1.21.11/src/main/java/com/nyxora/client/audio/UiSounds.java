package com.nyxora.client.audio;

import com.nyxora.client.settings.NyxoraSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Subtle UI feedback using vanilla sounds (no custom .ogg assets are shipped yet).
 * Plays through the UI/master channel at low volume; every call is failure-safe.
 * API NOTE (unverified on 1.21.11): PositionedSoundInstance.master(RegistryEntry, pitch, volume).
 */
public final class UiSounds {
    public enum Cue { CLICK, TOGGLE_ON, TOGGLE_OFF, OPEN, CLOSE, CATEGORY, SLIDER_RELEASE }

    private final NyxoraSettings settings;

    public UiSounds(NyxoraSettings settings) { this.settings = settings; }

    public void play(Cue cue) {
        if (!settings.soundsEnabled.get()) return;
        float volume = settings.soundsVolume.get() * 0.6f;
        if (volume <= 0f) return;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getSoundManager() == null) return;
            RegistryEntry<SoundEvent> event = switch (cue) {
                case OPEN, CLOSE -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
                case TOGGLE_ON, TOGGLE_OFF, SLIDER_RELEASE -> SoundEvents.BLOCK_COPPER_BULB_TURN_ON;
                case CATEGORY -> SoundEvents.UI_BUTTON_CLICK;
                default -> SoundEvents.UI_BUTTON_CLICK;
            };
            float pitch = switch (cue) {
                case OPEN, TOGGLE_ON -> 1.5f;
                case CLOSE, TOGGLE_OFF -> 1.1f;
                case CATEGORY -> 1.3f;
                case SLIDER_RELEASE -> 1.7f;
                default -> 1.2f;
            };
            mc.getSoundManager().play(PositionedSoundInstance.master(event, pitch, volume));
        } catch (RuntimeException ignored) {
            // Missing/renamed sound must never crash the client.
        }
    }
}
