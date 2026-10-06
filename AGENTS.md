# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Minecraft 26.3 mod built for NeoForge 26.3.0.1-beta with Java 25. Production Java code lives under `src/main/java/alku/csrp`; keep the `alku.csrp` package aligned with `mod_group_id`. Static assets belong in `src/main/resources/assets/csrp`, while `src/main/templates/META-INF/neoforge.mods.toml` is expanded during the build. Data-generator output goes to `src/generated/resources` and is included in the main resource set. Gradle wrapper files and version properties remain at the repository root.

## Build, Test, and Development Commands

Use the checked-in wrapper so every contributor uses the same Gradle version. `JAVA_HOME` must point at a Java 25 JDK (this machine: `D:\MC\jdk\graalvm-community-25.3.4.1+1.1`):

- `.\gradlew.bat build` compiles Java, processes resources, and creates the mod JAR in `build/libs`.
- `.\gradlew.bat runClient` starts a development client.
- `.\gradlew.bat runServer` starts a headless development server.
- `.\gradlew.bat runData` regenerates data into `src/generated/resources`.
- `.\gradlew.bat runGameTestServer` runs the registered NeoForge GameTests (the build restricts them to `csrp:*`); it fails when none exist.
- `.\gradlew.bat clean` removes generated build output.

Under Niubash (`niu -c`) `gradlew.bat` cannot be executed and hangs, so call the wrapper jar directly —
`"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain <task>` —
or use `scripts/build-locked.sh <task>`, which serialises concurrent Gradle runs and uses that invocation.

## Coding Style & Naming Conventions

Use four-space indentation and standard Java conventions: `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Keep registry IDs, asset names, and translation keys lowercase with underscores, for example `csrp:example_item`. Prefer NeoForge deferred registers and separate client-only code with `Dist.CLIENT`. No formatter is configured, so match nearby code and keep imports organized.

## Testing Guidelines

The repository has no `src/test/java` sources yet; gameplay behaviour is covered by NeoForge GameTests registered from `src/main/java/alku/csrp/gametest` and by the static contract scripts under `scripts/verify-*.cjs` (run all of them with `node scripts/run-all-verifications.cjs`). Add unit tests under `src/test/java` for isolated logic and GameTests for gameplay behaviour. Name tests after observable behaviour, such as `infectedBlockSpreadsToAdjacentBlock`. Run `build` and `run-all-verifications.cjs` before every contribution, and `runGameTestServer` whenever GameTests are present.

## Commit & Pull Request Guidelines

This repository has no prior Git history. Use concise, imperative commit subjects in the project convention, such as `-（功能）添加寄生方块注册`. Keep each commit focused. Pull requests should explain behavior changes, list verification commands, link related issues, and include screenshots only for visible client changes. Never commit `.gradle`, `build`, `run`, IDE metadata, credentials, or local agent configuration.

<!-- BEGIN dsh-global-system-prompt -->
<!--
  Managed by the dsh-global-system-prompt plugin. The rules below are synced from:
  C:\Users\Administrator\.dsh\global-prompt.md
  Edits inside this block are overwritten on the next sync; delete the block,
  markers included, to stop syncing.
-->

Global rules — follow these in every task in this project:

默认使用 Niubash（Bash on Windows，`niu -c`）执行任务。
每次任务结束将项目上传至github远程仓库，若无远程仓库则自己创建。
jdk在这里D:\MC\jdk
中文回答问题

<!-- END dsh-global-system-prompt -->
