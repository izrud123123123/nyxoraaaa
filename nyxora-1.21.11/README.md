# NYXORA (Fabric, Minecraft 1.21.11)

Client-side Fabric mod: dark futuristic menu (Right Shift), player and block-break particles,
kill effects, UI sounds, profiles, persistent config.

## Status (honest)

| Part | Written | Compiled | Run in Minecraft |
|---|---|---|---|
| Core: animation, settings, config, profiles | yes | yes (plain javac) | no |
| Core self-test (30 checks) | yes | yes | passes (no Minecraft needed) |
| Menu screen, widgets, sounds, particles, kill effects, entrypoint | yes | **syntax-checked only** | **no** |
| Gradle build / fabric.mod.json | yes | **never run** | no |
| **Custom skies (skybox presets)** | **not written** | - | - |
| **Motion blur** | **not written** | - | - |

The Minecraft-facing code was written without access to the 1.21.11 jars (no network), so API names
are from memory and web notes. Expect a few compile errors on first build; they are listed below.

## Build
Requires JDK 21. `./gradlew build` (add the Gradle wrapper first: `gradle wrapper --gradle-version 9.2.1`).
Versions in `gradle.properties` are unverified; if rejected, take the 1.21.11 set from https://fabricmc.net/develop.
Output jar: `build/libs/`. Put it in `.minecraft/mods/` with Fabric API.

## Core self-test
    mkdir -p build/classes
    javac -d build/classes $(find src/main/java/com/nyxora/client/{animation,settings,config,profiles,util} -name '*.java') src/test/java/com/nyxora/client/CoreSelfTest.java
    java -ea -cp build/classes com.nyxora.client.CoreSelfTest

## Features as implemented
- **Menu**: Right Shift toggles (edge-triggered, no repeat). Sidebar, scrolling, tooltips, toast notifications,
  time-based open/close/page/hover/toggle animations (disable via Interface > Animations), toggles, sliders
  with values, profiles page.
- **Particles**: player-following and block-break bursts using vanilla particle objects with tuned size,
  lifetime and tint; per-second budget; off when master toggle is off or the game's particles are Minimal.
- **Kill effects**: best-effort client detection (see limits). Presets, intensity, duration, color, components.
- **UI sounds**: vanilla sound events at low volume (no custom .ogg yet); one sound per slider drag.
- **Config**: `config/nyxora.properties`, debounced atomic writes, bad entries skipped. **Profiles**:
  `config/nyxora/profiles/`, built-in Default/Performance/Cosmic/Minimal, clipboard import/export.

## Known limitations / differences from the spec
- Sky settings exist and persist but **no sky is rendered yet**. Motion blur settings exist but **nothing renders**.
- Dropdowns are click-to-cycle (no popup list); color pickers are a palette cycler (exact hex via config file).
- No settings search box.
- Particle "glow" is binary (full-bright vanilla types vs lit dust) plus alpha.
- Kill detection can miss kills (victim out of range, health not synced) and can credit a bystander's
  kill shortly after your hit. It never alters combat.

## Likely first-build compile fixes (APIs I could not verify)
`Click`/`KeyInput` screen signatures (1.21.9+ forms), `KeyBinding.Category.create`,
`ClientPlayerBlockBreakEvents.AFTER`, `Entity#getEntityPos` (older: `getPos`),
`ParticleManager#addParticle` return type, `Particle#scale/setMaxAge/setColor/setAlpha`, `DustParticleEffect(int,float)`,
`PositionedSoundInstance.master(RegistryEntry,...)`, `Keyboard#setClipboard/getClipboard`.
