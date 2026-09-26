# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working in this repository.

## Project

CSRP is an unofficial reconstruction of *Scape and Run: Parasites* for Minecraft 1.21.1 on NeoForge 21.1, using Java 21. The repository's goal is to make the port auditable and useful to the original SRP team. When restoring original behavior, consult `docs/entity-parity/` for the clause-level evidence and record implementation decisions in the existing ledger convention. SRP names, designs, and third-party assets are excluded from this repository's MIT grant; see `NOTICE`.

## Build and development commands

Use the checked-in Gradle wrapper and JDK 21 (`D:\MC\jdk` in the configured development environment):

- `./gradlew.bat build` — compile and package `build/libs/csrp-<version>.jar`.
- `./gradlew.bat runClient` — launch the NeoForge development client.
- `./gradlew.bat runServer` — launch the development server.
- `./gradlew.bat runData` — regenerate data into `src/generated/resources` (included in the main resource set).
- `./gradlew.bat gameTestServer` — run NeoForge GameTests; this currently fails if no GameTests are registered.
- `node scripts/run-all-verifications.cjs` — execute every `scripts/verify-*.cjs` static port check. The suite has a known failure baseline; compare against the existing baseline and do not increase failures.

There is no configured Gradle lint task or Java unit-test suite. Run an individual static check with `node scripts/<verification-script>.cjs`. On Windows, use `niu -c` for shell commands where available; the Gradle wrapper is `gradlew.bat`.

## Architecture

- `src/main/java/alku/csrp/Csrp.java` is the `@Mod("csrp")` entry point. It wires NeoForge deferred registers, creative tabs, and common config specs. Registry declarations live under `registry/`; new registry-backed content should follow the existing deferred-register catalogs.
- Gameplay is organized by domain packages rather than a single central engine. `entity/`, `block/`, `item/`, and `effect/` define content; `infection/`, `world/`, and `event/` implement cross-cutting infection, evolution, spawning, colonies, and world progression, largely through NeoForge event subscribers. `world/SrpWorldData.java` persists server/world progression in `SavedData`.
- Client-only renderers, models, screens, particles, sounds, HUD, and client event handlers are under `client/` and domain-specific `*/client/` packages. Keep client references out of common/server-loaded code; use the established `Dist.CLIENT` subscriber pattern.
- Custom play networking uses NeoForge custom payloads (`CustomPacketPayload`/`StreamCodec`), with registrations gathered in `compendium/network/CompendiumPayloads.java`; follow the existing payload and handling patterns for client/server state sync.
- Data-driven resources and client assets are in `src/main/resources`. The `neoforge.mods.toml` source is a template in `src/main/templates/META-INF/` and Gradle expands version properties into generated build resources. Datagen output is under `src/generated/resources` and is intentionally ignored by Git; regenerate it with `runData` when needed.
- `scripts/` contains Node.js static verification checks and resource tooling in `tools/`. The checks encode reconstructed behavior and asset contracts, so use them alongside the parity ledger rather than treating them as ordinary unit tests.

## Repository conventions

Use four-space Java indentation and existing naming/style. Registry IDs, asset names, and translation keys are lowercase with underscores. Keep source packages under `alku.csrp` and the mod ID `csrp` aligned. Do not commit generated build/run output, `.gradle`, or `src/generated` artifacts unless a task explicitly requires generated source changes.

The README is the source of truth for the project's purpose, licensing, issue process, and contribution expectations. There are no Cursor rules or GitHub Copilot instruction files in this repository.
