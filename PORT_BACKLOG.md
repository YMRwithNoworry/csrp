# csrp-26.3 移植待办（对齐原模组 SRParasites 1.10.9）

目标：把原模组（1.12.2 Forge，本任务附件 `[逃逸：寄生体] SRParasites-1.10.9.jar`）完整移植到 **MC 26.3 / NeoForge 26.3.0.1-beta**（本工程）。

## 参照源

| 参照 | 路径 | 用途 |
| --- | --- | --- |
| 原模组 1.10.9（目标版本） | 附件 jar；反编译产物 `D:\code\MC模组\_scratch\vf\out109` | 事实来源 |
| 原模组 1.10.8（本工程移植基线） | `D:\code\模组反编译器\杂物\[逃逸：寄生体] SRParasites-1.10.8.jar`；反编译产物 `_scratch\vf\out108` | 差异基线 |
| 兄弟分支（Forge 1.20.1） | `D:\code\MC模组\_scratch\ref1201`（提交 `0e2d7b2f` 快照） | 行为实现参照 |
| 校验套件 | `scripts/run-all-verifications.cjs`（111 项） | 完成度度量 |

## 当前基线

- `gradlew build`：**成功**（GraalVM 25.3.4.1 / Gradle 9.2.1）。
- 校验套件：**111 / 111 通过**。
- 规模：560 个 Java 文件、88 个方块类、128 个实体文件、33 套语言（en_us 2978 键 / zh_cn 2994 键）。
- 并发构建保护：`scripts/build-locked.sh`（多 agent 共享工作区时串行化 Gradle）。

## 已完成（本轮 R2–R6）

- **R2 1.10.9 增量**：刷怪清理阈值 4→6 / 2→3 + 50 tick 冷却、`doTileDrops` / `doMobEvolution` 运行时开关、`/srparasites` 三个缺失子命令（`toggle_dotiledrops` / `toggle_domobevolution` / `readconfigurationfile`）、12 项 `*NeededAssimilation` 门槛与世界级同化计数持久化、暴风雪客户端渲染与星型同步差集（含 6 项 26.3 不适用项的替代方案）、碎裂地形（默认关闭）。报告：`docs/gap/R2_REPORT.md`、`docs/gap/R2_BLIZZARD_REPORT.md`。
- **R3 世界生成**：殖民地基类（原 614 行逐方法转写）+ Core + B1-B4 / BS1-BS4、NexusProtection 1/2/3、陨石撞击逐行移植。报告：`docs/gap/R3_WORLDGEN_REPORT.md`。
- **R4 方块保真化**：108 个占位方块 → **0 个通用 `new Block(...)` 回退**，36 个专用方块类 + 形状家族。报告：`docs/gap/R4_BLOCKS_REPORT.md`。
- **R5 物品补齐**：机器化 diff 后补齐 20 个 id（唱片、报告、弓镰部件、图标、3 个刷怪蛋）；另修 `LegacyMobSpawnerItem` 32 处实体映射缺失。报告：`docs/gap/R5_ITEMS_REPORT.md`。
- **R6 实体/AI**：158→157 id 级 diff、73 个 AI 类逐条判定、同化门槛接线、SoundEater 潜行、Venkrol 龙卷、NexusProtection 触发。报告：`docs/gap/R6_ENTITY_MATRIX.md`。
- **语言**：33 套 `.lang` → 26.3 JSON，`en_us` 2332/2332、`zh_cn` 2140/2140 全覆盖；剔除 6 个会覆盖原版字幕的 vanilla 键。报告：`docs/gap/LANG_REPORT.md`。
- **资源**：结构 NBT 55/55、音效 1008/1008、纹理对齐。

## 待办

### R6 剩余 AI 组（entity-ai 批次 3，进行中）
G7 `EntityAINexusGrow` 的 spawnLeem、G6 `EntityAIBlockResidue` 覆盖其余 gene 8 的 adapted Kind、G2 `EntityAIFollowBodies`、G3 `EntityAICircleGroup`、G1 `EntityAIBlockLight`。

### 已核实的"backlog 误报"（不再作为待办）
- `BlockParasiteBush.THORN`：out109 的 `BlockParasiteBush.EnumType` 无 THORN、无荆棘伤害；thorn 行为属 `BlockThornshade` + `ThornshadeThornsEvents`（已实现）。证据见 `docs/gap/R4_BLOCKS_REPORT.md` §3.1。
- `ada_burrower_drop`：1.10.9 的 `SRPItems.java` 未注册该 id，仅存在于 lang 与掉落表引用（`SRPConfigMobs.java:74,81`）；注册它会让原模组本就不生效的掉落条目生效，属改变行为，故不注册。证据见 `docs/gap/R5_ITEMS_REPORT.md`。

### 已知差异（有意保留，需在后续版本复核）
- `util/config/SRPConfig*` 的 1709 个 1.12.2 键名按 26.3 惯例重构为 camelCase，不做机械重命名；以"配置项是否对应真实行为"为准逐项补缺。
- `relay_report` 等文案保留原 `%s` 占位符（1.12.2 格式，26.3 不填充）。
- `self_destruct_icon` 按原类 `ItemAdvancementIcon` 为 `stacksTo(1)`，与既有 15 个 `*_icon` 的 64 不一致（既有项差异）。
- `noderelay` / `relaycontroller` / `relay_controller_dummy` / `dermoid_cyst` / `parasite_barrier` 的 1.12.2 tile entity 层未复刻：本工程既有 relay/cyst 体系（`RelayTerminalBlockEntity` / `ParasiticCystBlockEntity`）语义已覆盖，且原 `TileEntityRelayController`（1043 行）与既有实现耦合到不同方块，重复注册会产生半可用方块。`infested_furnace` / `infested_furnace_lit` **已接线**到 `InfuserFurnaceBlockEntity`（`ModBlockEntities.LEGACY_INFESTED_FURNACE`），是真实可用的容器熔炉。
- 原版 1.10.9 的 `B4` / `BS4` 建筑不可达（仅通过显式 API 暴露，不伪造自动投放）；`EntityAIEvadeTP` / `EntityAINexusNest` / `AISoundEaterStalk` / `AIDisableBeaconIki` 为死代码。证据见 `docs/gap/R3_WORLDGEN_REPORT.md` §4.1 与 `docs/gap/R6_ENTITY_MATRIX.md`。

### 运行期验证
- `runGameTestServer` 实体冒烟（每个实体创建/tick/NBT 往返）+ 服务器启动到 Done 已通过。
- 世界生成运行时验证（R1，前一轮已实证）：节点 → 225 区块 → 四类寄生群系与 36 种寄生体方块落地，零 decoration 失败。

## 提交规范

每批次：`gradlew build`（`JAVA_HOME=D:\MC\jdk\graalvm-community-25.3.4.1+1.1`）必须 `BUILD SUCCESSFUL`，`node scripts/run-all-verifications.cjs` 不得新增失败项，然后**路径限定提交**（`git commit -m "-（类别）说明" -- <路径>`，避免共享索引误带他人文件）并推送到 `origin/port/neoforge-26.3`。
