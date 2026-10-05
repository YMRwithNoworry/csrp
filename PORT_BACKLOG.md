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

- `gradlew clean build`：**成功**（GraalVM 25.3.4.1 / Gradle 9.2.1），产物 `build/libs/csrp-1.10.9.jar`。
- 静态校验套件：**127 / 127 通过**。
- 运行期验证：`runGameTestServer` → **182 / 182 GameTests 通过**（169 个实体生成冒烟 + 1 个 NBT 往返 + 2 个 Escape 规则 + 1 个酿造配方 + 3 个注入熔炉 + 2 个失真开关 + 2 个复仇抓钩 + 2 个 harlequin），服务器完整启动到测试结束。
- 规模：560+ 个 Java 文件、88 个方块类、170 个实体 id（原件 158 个 **全部覆盖**，另有 12 个自有辅助实体）、33 套语言（en_us 2978 键 / zh_cn 2994 键）。
- 并发构建保护：`scripts/build-locked.sh`（多 agent 共享工作区时串行化 Gradle）。
- 本轮由 `runGameTestServer` 抓出并修复的运行期加载失败：方块状态定义读取 `super()` 前未赋值的实例字段（`LegacyVariantSlabBlock` / `LegacyRelayBlock`，改为工厂预发布）、`JukeboxSong` 是数据包注册表故 `discone` / `disctwo` 需要 `data/csrp/jukebox_song/*.json`（见 commit `d7f7ca5c`）。

## 已完成（本轮 R2–R6）

- **R2 1.10.9 增量**：刷怪清理阈值 4→6 / 2→3 + 50 tick 冷却、`doTileDrops` / `doMobEvolution` 运行时开关、`/srparasites` 三个缺失子命令（`toggle_dotiledrops` / `toggle_domobevolution` / `readconfigurationfile`）、12 项 `*NeededAssimilation` 门槛与世界级同化计数持久化、暴风雪客户端渲染与星型同步差集（含 6 项 26.3 不适用项的替代方案）、碎裂地形（默认关闭）。报告：`docs/gap/R2_REPORT.md`、`docs/gap/R2_BLIZZARD_REPORT.md`。
- **R3 世界生成**：殖民地基类（原 614 行逐方法转写）+ Core + B1-B4 / BS1-BS4、NexusProtection 1/2/3、陨石撞击逐行移植。报告：`docs/gap/R3_WORLDGEN_REPORT.md`。
- **R4 方块保真化**：108 个占位方块 → **0 个 id 落到通用 `new Block(...)` 回退**（Lead 独立复核：从 `registerLegacyBlocks` 的 `String[] ids` 取出 108 个 id，与 `LEGACY_DEDICATED_BLOCKS` 的工厂键（含 5 个数组族 + gore 内联数组循环）比对，未命中 0 个；源码里仍保留该 `else` 分支作为防御性兜底，另有 `legacyStateBlock` 的匿名子类属合法的状态属性路径）。36 个专用方块类 + 形状家族。报告：`docs/gap/R4_BLOCKS_REPORT.md`。
- **R5 物品补齐**：机器化 diff 后补齐 20 个 id（唱片、报告、弓镰部件、图标、3 个刷怪蛋）；另修 `LegacyMobSpawnerItem` 32 处实体映射缺失、17 个成就图标统一 `stacksTo(1)`。报告：`docs/gap/R5_ITEMS_REPORT.md`。
- **R6 实体/AI**：73 个 AI 类逐条判定（已实现 65 / 不一致 3 / 缺失 0 / N/A 5）；**实体 id 级 100% 覆盖**（原件 158 全部命中）；同化门槛接线、SoundEater 潜行、Venkrol 龙卷、NexusProtection 触发、BlockLight / BlockResidue / CircleGroup。报告：`docs/gap/R6_ENTITY_MATRIX.md`。
- **语言**：33 套 `.lang` → 26.3 JSON，`en_us` 2332/2332、`zh_cn` 2140/2140 全覆盖；剔除 6 个会覆盖原版字幕的 vanilla 键。报告：`docs/gap/LANG_REPORT.md`。
- **世界创建选项（UI 保真修复）**：原模组的世界选项是「创建世界界面上的 *SRP World Options...* 按钮 → 专用 *SRP World Settings* 界面（5 个选项 + 世界预览面板 + Done）」，由 `SRPConfig.worldGIU` 开关（`client/gui/SRPWorldCreateButtons.java` + `GuiSRPWorldSettings.java` + `GuiSRPWorldPreview.java`）。此前本工程把 5 个 CycleButton 直接硬塞进原版创建世界界面的固定 y 偏移（`height-52 … height-148`），会被原版页脚/标签内容盖住而看不到。现按原件恢复：新增 `client/SrpWorldSettingsScreen.java`（专用界面，原版布局 leftX / topY+0,48,72,96,120 + 160×122 预览 + Done）、`client/SrpWorldPreview.java`（星空 + 行星系统，固定种子 923847、70 星、8 张既有纹理）、`SrpDifficultyScreenEvents` 改为页脚行内单个入口按钮（按位置定位原版页脚，不引用任何 vanilla 语言键），并补 `GeneralConfig.worldCreationUi`（原 `worldGIU`）与 `gui.csrp.worldsettings.star.warm` 标签。校验：`scripts/verify-world-settings-ui.cjs`。
- **资源**：结构 NBT 55/55、音效 1008/1008、纹理对齐。

- **Escape 死亡循环逃脱系统（本轮补齐）**：原 `feature/EscapeOnDeathHandler` + `EscapeRespawnHandler` + `client/EscapeClientState` + `GuiGameOverEscape` + 两个网络包此前只迁移了语言键、代码缺失。现补齐：寄生体致死一分钟内超过 5 次 → 死亡界面出现逃脱按钮 → 点击置 pending 并重生 → 在 200~300 格内寻找安全落点传送。配置 `escapeEnabled` / `escapeMinDistance` / `escapeMaxDistance`（原 `SRPConfigWorld` EscapeSystem 分节）。校验 `scripts/verify-escape-system.cjs` + 2 个 GameTest。
- **交付说明**：`PORTING_NOTES.md`（已完成清单、因 26.3 API 缺失而改写/省略的部分、已知不兼容项——含旧存档不可迁移的明确说明）。

## 待办

### R6 剩余（低影响，已在矩阵登记）

### R6 剩余（低影响，已在矩阵登记）

全部 73 个 AI 类已逐条判定，无"缺失"项；剩余登记项为：`EntityAIDodAttack` 的黑/白名单过滤、`EntityAIVenkrolSummon` 的 mob 表逐条复刻、`EntityAIFollowBodies` 的架构差异（本工程用 `bodyPredecessor` 实体链，语义等价）、gene 系统（本工程用 Kind 分支替代 `EntityPMalleable` 的 10 个开关）。详见 `docs/gap/R6_ENTITY_MATRIX.md` §5.2。

### JEI 配方查看器集成（✅ 已完成）

已实现 `compat/jei/SrpJeiPlugin`（`@JeiPlugin`）+ `compat/jei/InfuserFurnaceJeiCategory`，JEI 依赖为 `compileOnly 'maven.modrinth:jei:31.9.0.58'`（Modrinth maven），**不装 JEI 也能构建与运行**：

- 注入熔炉分类：槽位按原件 (55,16)/(29,34)/(115,34)/(139,52)，200 tick 动画箭头在 (79,34)，催化剂为 `csrp:infuser_furnace`，配方来自 `recipe/InfuserFurnaceRecipes.all()`。
- 隐藏原件黑名单里的 16 个成就图标（`onRuntimeAvailable` → `removeIngredientsAtRuntime`）。
- **酿造分类不需要插件代码**：7 条配方是真正的 `minecraft:brewing` 配方，JEI 内置酿造分类会自行展示。
- 因 API 差异的改写：JEI 26.3 无 `getBackground()`，分类改为在 `draw()` 中绘制原件 `infuser_furnace.png` 的 (0,0,176,82)；`needsRecipeBorder()` 返回 false 以免叠加边框。
- 保真保留：原件把注入产物放在第一个输出槽 (115,34)、副产物放在第二个 (139,52)，与 GUI 上这两个槽位的实际位置相反——这是原件的展示顺序，已按原样保留而非「顺手修正」。

验证：`scripts/verify-jei-plugin.cjs`（插件注解、UID、分类/催化剂/配方注册、16 个图标 id 与注册表一致、JEI 保持 compileOnly）；产物中 `alku/csrp/compat/jei/SrpJeiPlugin.class` 已打包且带 `Lmezz/jei/api/JeiPlugin;` 注解（JEI 以类文件级扫描发现插件）。**未做运行期目视验证**：JEI 是客户端模组，本环境无法启动图形客户端。

### 注入熔炉保真化（✅ 已完成）

已按原件 `TileEntityInfuserFurnace` / `ContainerInfuserFurnace` / `GuiInfuserFurnace` / `BlockInfuserFurnace` 重构：

- **6 槽**：0 熔炼输入、1 燃料、2 原版熔炼输出、3 注入试剂、4 注入产物、5 副产物（原件的槽位索引与命名）。
- **两个独立 200 tick 计数器**（`cookTimeSmelt` / `cookTimeInfuse`）共用同一燃料缓冲；燃料耗尽时按 `getCraftingRemainder()` 留下容器物品（如岩浆桶→桶）。
- **注入产出主产物 + 玻璃瓶副产物**（铁锭/侵染沙 + 死血），玻璃置换产出 **2 个**；每次消耗输入各 1 个（1.12.2 的 `Ingredient.apply` 不校验数量，配方的 32/16 仅为 JEI 展示量）。
- **同时执行原版熔炼**（槽 0 → 槽 2，经 `RecipeType.SMELTING` 查询）。
- **GUI 改用原件纹理** `csrp:textures/gui/infuser_furnace.png`（此前误用原版熔炉纹理），箭头 (80,35) u=176 v=14、火焰自 (56,36) 向上绘制，与原件 `GuiInfuserFurnace` 的偏移一致。
- **容器槽位坐标按原件**：(56,17) (56,53) (30,35) (140,53) (116,35) (140,17)，与纹理上的槽位框对齐。
- **方块改为朝向方块**：原件 `BlockInfuserFurnace` 有 `FACING`（无 lit），blockstate 4 个 `facing=*` 变体；此前工程是非朝向单变体。
- **配方表**独立为 `recipe/InfuserFurnaceRecipe(s)`（9 条，镜像原件 `SRPInfuserFurnaceRecipeInit`），供方块实体与 JEI 共用。

验证：`scripts/verify-infuser-furnace.cjs` + 3 个 GameTest（`infuser_furnace/infuse_with_byproduct`、`glass_swap_yields_two`、`vanilla_smelting`），驱动真实 `serverTick` 断言副产物、双倍产物、原版熔炼与逐次消耗量。

### 复仇之书抓钩保真化（✅ 已完成）

已按 `docs/gap/VENGEANCE_GRAPPLE_REPORT.md` 的逐行规格重写 `event/BookOfVengeanceEvents` 与 `item/BookOfVengeanceItem`：

- **三阶段状态机**（原件的 WINDUP / PULL / BOUNCE）：蓄力 20 tick、拉近速度 1.8、接触距离 2.2 格或 40 tick 超时。
- **首次冲击**：14.0F、半径 5 的 AoE（盒 ±5/-2..+4/±5，排除玩家），对范围内每个实体施加 `DOD_SMOKE_TRAIL`/`BLEED`/`DEBAR` 各 100 tick，沿玩家视线以 1.9 前 + 0.65 上弹射，玩家水平速度 ×0.15。
- **弹跳 14 tick** 三段速度曲线（`t≥12`：上抬 0.55、水平 ×0.4；`t≥6`：上抬 -0.02、水平 ×0.65；否则以 2.35 再次拉近），每 tick 清零坠落距离。
- **二次冲击**：目标 6.0F + 上抬 0.35；半径 3.5 的缩放 AoE（`10.0F*(1-d/3.5)`）含击退与上抬，最后落雷。
- **触发与冷却**：右键自动取 **32 格视线目标**（射线 + 0.3 命中盒 + 视线校验），冷却 60；潜行右键为击退爆发（半径 5、强度 2.2、衰减 `0.45+0.55*falloff`、上抬 0.4、自身 0.42），冷却 80 存于物品 `srp_kb_next` 标签。
- **丢失视线即断链**并播放熄火音效；补齐 5 个 `VENGEANCE_*` 音效事件与 4 类粒子表现（对应原件 `PacketVengeanceFX` 的 SPARKS / IMPACT_DUST / HEAVY_BLEED / LIGHTNING_EXPL）。

验证：`scripts/verify-vengeance-grapple.cjs`（钉住全部原件数值与结构）+ 2 个 GameTest（5 个音效已注册、击退衰减公式）。**速度与 AoE 手感仍需真机确认**。

### 管理命令：harlequin 家族（部分完成）

原件 `proxy/CommonProxy` 注册的 18 个命令已全部核对；本轮又完成一条：

- ✅ **`harlequin_here [radiusChunks=1]`**（权限 2）：把调用者周围 (2r+1)² 个区块的生物群系改为 Harlequin 并推送给客户端，然后回报变更区块数与当前位置的实际群系名。
  - 26.3 改写：原件直接写 `Chunk#getBiomeArray()` 并自建区块群系包；26.3 群系按 4×4×4 段存储，故改用 `ChunkAccess#fillBiomesFromNoise`（即 `/fillbiome` 用的同一原语）+ `ChunkMap#resendBiomesForChunks`。
  - **刻意不用** 原版的 `FillBiomeCommand.fill`：它有 **32768 方块**的体积上限，而整高的区块方块远超该值（原件无此限制）。
  - 验证：GameTest `csrp:harlequin_command/biome_fill` 直接调用命令自身的写入路径并断言地形群系确实变成 Harlequin；`csrp:harlequin_command/registered` 断言命令与群系均已注册。

仍缺 2 条（规格已核实）：

1. **`harlequin_convert [radius=16] [sampleChance=1] [depth=32] [blotchChance=18] [blotchMinDiam=3] [blotchMaxDiam=5]`**（权限 2，原件 176 行）：按 `HarlequinBlockConverter.Config(HarleskinnBlock, HarlequinnGrass, Alveoli, AlveoliGrowth, LipomaMass, TressesHair, HirsuteHair)` 做斑块式地形转换。需先移植 `util/convert/HarlequinBlockConverter`（164 行：`convert` / `maybePlaceHair` / `canSustain` / `placeDoublePlant` 等）。
2. **`harlequin_scatter [...]`**（权限 2，原件 225 行）：在合适地面上散布 Harlequin 结构，结构名清单 `harlequin_rock_01..07`、`harlequin_bush_00..02`、`harlequin_tree_01..07`、`harlequin_ruin_01..03`（工程 `data/csrp/structure/` 下**全部存在**），地面判定接受沙子/红沙/黏土/草/泥土以及 `harlequinn_grass` / `harleskinn_block`。

### 极端降雪（`/srp_extremesnow`）：原件为死代码

**结论：不注册该命令，但保留其驱动系统。** 证据：

- 原件的 `CommandExtremeSnow` **从未出现在 `registerServerCommand` 列表中**，全树唯一的 `ExtremeSnowData#setEnabled` 调用点就在它自己内部；
- 原件语言文件里**没有任何** `srphelp.srp_extremesnow` 条目（对比：3 条 harlequin 命令都有 `srphelp.harlequin_*` 条目）；
- 因此 `ExtremeSnowData.enabled` 在原版 1.10.9 中**永远是 false**，`ExtremeSnowServer` 永远不落雪——系统接线齐全但不可达。

处理方式（与既有「死代码不移植」策略一致，同时保留 parity）：

- 移植并接线了系统本身（与原版一样挂着但休眠）：`world/ExtremeSnowData`（存档键 `srp_extreme_snow`，字段 enabled/intensity/forceAnywhere/windDeg/windSpeed）、`world/ExtremeSnowServer`（每玩家 `12+48×intensity` 次尝试、半径 18、放 1 层雪）、`network/ExtremeSnowStatePayload`、`client/weather/ClientExtremeSnow`（风向向量 `cos/sin(deg)×(0.15+0.55×speed)`、雾密度 `sqrt(ln50)/10`、雾色 75% 拉白、五点露天判定）；暴风雪渲染器额外接入该状态作为触发源。
- `command/ExtremeSnowCommand` 保留为 parity 但**不注册**，由 `scripts/verify-harlequin-and-snow.cjs` 断言它不在 dispatcher 上——注册它等于给玩家一个原版从未有过的管理命令。
- 注：原件的 `ExtremeSnowData` setter **不标 dirty**（改动不会存盘），本工程补了 `setDirty()`。

### 文档一致性（待办）

`AGENTS.md` 的“Project Structure / Build, Test, and Development Commands / Testing Guidelines”三节仍写着 **Minecraft 1.21.1 / NeoForge 21.1 / Java 21**，与工程实际（26.3 / NeoForge 26.3.0.1-beta / Java 25，见 `gradle.properties`）不符；其中 `gradlew gameTestServer` 的实际任务名是 **`runGameTestServer`**。该文件同时被 DSH 的 global-system-prompt 插件托管（会反复注入一段本机配置块），因此本轮未直接改动它，仅在此登记，供人工按实际修订。
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
