# TheTrueOneAbove — Forge 1.20.1

A Forge mod for Minecraft 1.20.1. Type `let there be light` in chat (any capitalization) to:

- Transform the speaker into **TheTrueOneAbove** with a visible name tag.
- Grant invulnerability, strength, speed, jump boost, regeneration, fire resistance, night vision and glowing.
- Remove all loaded **non-player entities** from every loaded dimension (including mobs, dropped items, projectiles and vehicles). Players are explicitly excluded.
- Show every connected client a brief full-screen white flash.
- Give the transformed player a white particle aura.

## Build

Requirements: Java 17 and internet access for Gradle/Forge dependency downloads.

1. Extract this project.
2. Run `gradle build` from the project folder (Gradle must be installed).
3. The mod JAR is created in `build/libs/`.
4. Put the JAR in the `mods` folder of a Forge 1.20.1 installation.

## Android launcher notes

This is a normal Java Forge mod, not an Android-specific port. It can work on Android only when the launcher supports **Minecraft 1.20.1 Forge 47.x**, Java 17, and the required Forge networking/client rendering APIs. Android launchers vary, so compatibility cannot be guaranteed. Install it on the integrated single-player game, or on both the client and server for multiplayer. The chat-triggered entity removal runs on the logical server; a client-only install on a remote server cannot perform it.

## Trigger

`let there be light`, `LET THERE BE LIGHT`, and any other capitalization all work. The phrase must be the complete chat message.

## Safety / gameplay note

The effect removes all non-player entities in all currently loaded dimensions. It does not delete blocks, items, or players. It is intentionally a one-way transformation for the current player-data lifetime; use a backup if testing in an important world.

## Time stop

Say any message containing `the first and the last` (e.g. `I am the first and the last`) to stop time for 10 seconds. All non-player entities (mobs, items, projectiles, TNT, minecarts) freeze in place; you and other players keep moving. Projectiles you fire and vehicles you ride are not frozen. Clients see an impact frame, a grey tint and a countdown.

## Impact frame

`let there be light` now opens with a flickering black/white anime-style impact frame with speed lines and a screen shake before the white fade.
