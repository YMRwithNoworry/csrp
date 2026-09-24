# R2 —— 1.10.9 版本增量差集（26.3 移植报告）

参照源：`D:\code\MC模组\_scratch\vf\out109`（SRParasites 1.10.9 反编译，1.12.2 Forge）
基线：`_scratch\vf\out108`（1.10.8，本工程原移植基线）
本工程：`D:\code\MC模组\csrp-26.3`（MC 26.3 / NeoForge 26.3.0.1-beta）

## 1. 本轮由 Lead 落地的增量

| 项 | 原模组证据 | 本工程实现 | 提交 |
| --- | --- | --- | --- |
| 刷怪清理阈值 4→6 / 2→3 | `init/SRPSpawning.java:438`（`* 6`）、`:457`（`* 3`）；1.10.8 为 `* 4` / `* 2` | `world/EvolutionEvents.java`：`MOB_CLEANER_TRIGGER_MULTIPLIER = 6`、`MOB_CLEANER_STOP_MULTIPLIER = 3`，并把"删到阈值以下即停"改为"删到 3×cap 以下即停" | `c7da59a0` |
| 清理冷却 50 tick | `init/SRPSpawning.java:443`（`mobClearCooldown = 50`） | `MOB_CLEANER_COOLDOWN_TICKS = 50` + `mobCleanerCooldown` 计数（原工程完全缺失） | `c7da59a0` |
| `doTileDrops` 运行时开关 | `util/config/SRPConfig.java:137,731`（默认 true） | `config/GeneralConfig.java` 新增 `parasiteBlockDrops`；`config/RuntimeToggles.java` 提供覆盖语义 | `c7da59a0` |
| `doMobEvolution` 运行时开关 | `util/ParasiteEventEntity.java:127`（`canSpawnNext`，瞬态，重启复位） | `config/RuntimeToggles.mobEvolution()`（瞬态，默认 true，与原始语义一致） | `c7da59a0` |
| `/srparasites toggle_dotiledrops` | `network/SRPCommandRoot.java:143-148` | `command/SrpCommands.java` 子命令 | `c7da59a0` |
| `/srparasites toggle_domobevolution` | `network/SRPCommandRoot.java:150-155` | 同上 | `c7da59a0` |
| `/srparasites readconfigurationfile` | `network/SRPCommandRoot.java:115-141` | 同上，用 `ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, FMLPaths.CONFIGDIR.get())` 重读全部 CSRP 配置 | `c7da59a0` |
| 12 项 `*NeededAssimilation` 门槛 | `util/config/SRPConfigMobs.java`（`*CanSpawnAssimilatedNat`，默认 2/9/5/-1/4/3/2/4/6/3/-1/6） | `config/MobsConfig.java` 12 个 `srparasites:sim_*` / `hi_golem` 条目 + `neededAssimilation(String)` | `21dd8dcd` |
| 世界级同化计数持久化 | `world/SRPSaveData.java:1357,1361`（`assimCounts`），存档键 `srpassimilatedtotalid*` / `srpassimilatedtotalidtimes*` | `world/SrpWorldData.java`：`assimilationCounts` + `assimilationCount()` / `addAssimilationCount()`，存档键 `assimilation_counts` | `21dd8dcd` |
| 26.3 API 修复 | —— | `Entity.hurtMarked` 在 26.3 已改名为 `Entity.syncVelocity`（`block/InfestedCactusBlock.java`） | `21dd8dcd` |

新增校验：`scripts/verify-r2-increment.cjs`（覆盖上表全部契约）。

## 2. 逐项核对结论（R2 清单）

### 2.1 已确认**本工程早已覆盖**，无需改动

| 清单项 | 结论 | 证据 |
| --- | --- | --- |
| `world/spawner/MobCaps`（`worldMobCap + players × worldMobCapPlusPlayer`） | 已覆盖 | `config/WorldConfig.java:55` `naturalMobCap(ServerLevel)` |
| `network/SRPCommandEvolution` 子命令 | 已覆盖 | `command/SrpCommands.java` `srpevolution` 树含 `set_evolutionloss` / `set_evolutiongaining` / `evolutionlock_*` / `addpoints` / `setphase` / `setcooldown` / `addcooldown`，与 `out109/network/SRPCommandEvolution.java` 一致 |
| `ParasiteSummon` 陨石门槛（`meteorRadius` / `meteorMinRadius`） | 已覆盖 | `Config.meteorRadius()`（120）、`Config.meteorMinimumRadius()`（80）、`world/MeteorInfectionSystem.java:96-118`（1.12.2 语义：`rand.nextInt(max)` + min 夹取 + 有符号偏移） |
| `client/gui/GuiSRPWorldSettings` 差集 | 已覆盖 | `client/SrpDifficultyScreenEvents.java` 在世界创建界面提供 difficulty / star type / meteor / mushroom trees / fractured terrain 五个选择器，与原 `GuiSRPWorldSettings` 的 5 个按钮一一对应（且按 cold star 联动可见性） |
| `world/SRPSaveData` 的 EIV 字段（`dimEIVHealth` / `dimEIVArea`） | 已覆盖（概念映射） | 原模组的 EIV = Emerging Infestation Vector，即本工程的 vector；持久化在 `SrpWorldData.writeVectors` / `readVectors` |
| 阶段 -1 / -2 语义 | 已覆盖 | `world/EvolutionSystem.java:84-87`、`world/NaturalSpawnTables.java:286`、`world/SrpCoreSystems.java:146-155`（`phase == -1` 时 vector 上限 1、生命 ×10、初始 area 2） |

### 2.2 已分派给对应工作流（不在本报告范围内闭环）

| 清单项 | 承接方 | 说明 |
| --- | --- | --- |
| `client/weather/**`、`MixinEntityRendererBlizzard`、`MixinRenderGlobalBlizzardSky`、`AccessorShaderGroup`、`StarWorldShaderManager` | blizzard-r2（task-6） | 1.12.2 的 ShaderGroup/EntityRenderer/RenderGlobal 钩子在 26.3 无对应，按 26.3 渲染事件重做 |
| `SRPBlizzardDerivedHandler`、`GenLayerSRPDynamicStar`、`SRPStarTypeSyncHandler`、`MsgSyncStarType`、`MsgSyncBlizzardReverse`、`SRPFracturedTerrainHandler` | blizzard-r2（task-6） | 同上 |
| `item/ItemMobSpawner`（zaaadapted / wymoadapted 条目） | items-parity（task-2） | 原模组刷怪蛋的额外实体映射 |
| `BlockDeadheadLeaves` 距离 7 衰减、`BlockParasiteTrunk.canSustainLeaves`、`BlockParasiteBush.THORN`、`BlockEvolutionLure.luredValueNine/Ten` | blocks-fidelity（task-1） | 方块行为差集 |
| `EntityCanSpawn.canSpawnByIDData()` 接线（12 类同化门槛）与 `addNumberIDDataSpawn` 计数增加点 | entity-ai（task-5） | Lead 已提供 config/world 侧 API 与原始判定方向（`计数 < 门槛 → 禁止自然刷怪`，门槛 -1 放行） |

### 2.3 未逐键对齐（设计性差异，非缺口）

`util/config/SRPConfig*` 在 1.12.2 使用带空格的 Forge TOML 键名与分节（共 1709 个 `getInt/getBoolean/...` 调用点），本工程已按 26.3 惯例重构为 camelCase 键名 + 分节（`csrp-general.toml` / `csrp-mobs.toml` / `csrp-systems.toml` / `csrp-world.toml` / `csrp-block-conversions.toml`）。

- **不做键名机械重命名**：会破坏既有用户配置与 92 项校验契约。
- **做行为对齐**：本轮以"配置项是否对应真实行为"为准逐项补缺，已补 `*NeededAssimilation`（12 项）与 `doTileDrops`；`MobCaps`、陨石半径等已存在项经核对默认值一致（`worldMobCap` 40/5、`meteorRadius` 120、`meteorMinRadius` 80）。
- 已知仍待逐项核对的残余：`SRPConfigMobs` 的逐怪 health/damage/armor/knockback/followRange 表（本工程 `MobsConfig` 已有对应实现，默认值需逐条抽样核对，见 R6 矩阵）。

## 3. 验证

```
JAVA_HOME=D:/MC/jdk/graalvm-community-25.3.4.1+1.1 ./gradlew.bat build -x test   → BUILD SUCCESSFUL
node scripts/run-all-verifications.cjs                                            → 97 total / 96 passed
```

（`verify-spawn-egg-textures.cjs` 的临时失败属 items-parity 批次 B 在途工作：`flam/soo/tenn_spawn_egg` 已注册、模型待补，不属 R2。）
