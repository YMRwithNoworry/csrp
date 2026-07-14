# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Minecraft 1.21.1 mod built for NeoForge 21.1 with Java 21. Production Java code lives under `src/main/java/alku/csrp`; keep the `alku.csrp` package aligned with `mod_group_id`. Static assets belong in `src/main/resources/assets/csrp`, while `src/main/templates/META-INF/neoforge.mods.toml` is expanded during the build. Data-generator output goes to `src/generated/resources` and is included in the main resource set. Gradle wrapper files and version properties remain at the repository root.

## Build, Test, and Development Commands

Use the checked-in wrapper so every contributor uses the same Gradle version:

- `.\gradlew.bat build` compiles Java, processes resources, and creates the mod JAR in `build/libs`.
- `.\gradlew.bat runClient` starts a development client.
- `.\gradlew.bat runServer` starts a headless development server.
- `.\gradlew.bat runData` regenerates data into `src/generated/resources`.
- `.\gradlew.bat gameTestServer` runs registered NeoForge GameTests; it fails when none exist.
- `.\gradlew.bat clean` removes generated build output.

## Coding Style & Naming Conventions

Use four-space indentation and standard Java conventions: `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Keep registry IDs, asset names, and translation keys lowercase with underscores, for example `csrp:example_item`. Prefer NeoForge deferred registers and separate client-only code with `Dist.CLIENT`. No formatter is configured, so match nearby code and keep imports organized.

## Testing Guidelines

The repository currently has no automated test sources or coverage threshold. Add unit tests under `src/test/java` for isolated logic and NeoForge GameTests for gameplay behavior. Name tests after observable behavior, such as `infectedBlockSpreadsToAdjacentBlock`. Run `build` before every contribution and `gameTestServer` whenever GameTests are present.

## Commit & Pull Request Guidelines

This repository has no prior Git history. Use concise, imperative commit subjects in the project convention, such as `-（功能）添加寄生方块注册`. Keep each commit focused. Pull requests should explain behavior changes, list verification commands, link related issues, and include screenshots only for visible client changes. Never commit `.gradle`, `build`, `run`, IDE metadata, credentials, or local agent configuration.
