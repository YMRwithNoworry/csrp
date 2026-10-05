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
- 静态校验套件：**120 / 120 通过**。
- 运行期验证：`runGameTestServer` → **174 / 174 GameTests 通过**（170 个实体生成冒烟 + 1 个 NBT 往返 + 2 个 Escape 规则 + 1 个酿造配方），服务器完整启动到测试结束。
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

### JEI 配方查看器集成（待办，方案已就绪）

原件 `compatibility/jei/**` 共 5 个类（`SRPJEIPlugin` 140 行、`InfuserFurnaceJEICategory` 61、`InfuserFurnaceJEIRecipe` 29、`SRPBrewingCategory` 48、`SRPBrewingJEIRecipe` 26）。本工程尚未移植。**JEI 已有 NeoForge 26.3 构建**（Modrinth `jei`，`31.9.0.58`，文件名 `jei-26.3-neoforge-31.9.0.58.jar`），因此可以补齐。

已核实的事实（可直接据此实现）：

1. **依赖**：在 `build.gradle` 加 `maven { url "https://api.modrinth.com/maven" }` 与 `compileOnly "maven.modrinth:jei:31.9.0.58"`（若需在 `runClient` 里验证插件发现，再考虑 `runtimeOnly` 或 `localRuntime`）。
2. **26.3 JEI API（已从 jar 反查）**：`mezz.jei.api.IModPlugin`（`getPluginUid()` 返回 `Identifier`；默认方法 `registerCategories(IRecipeCategoryRegistration)`、`registerRecipeCatalysts(IRecipeCatalystRegistration)`、`registerRecipes(IRecipeRegistration)`）；`IRecipeCategory<T>` 必须实现 `getRecipeType()` / `getTitle()` / `getWidth()` / `getHeight()` / `getIcon()` / `setRecipe(IRecipeLayoutBuilder, T, IFocusGroup)`；注册用 `IRecipeCategoryRegistration.addRecipeCategories(...)`、`IRecipeCatalystRegistration.addCraftingStation(IRecipeType<?>, ItemLike...)`、`IRecipeRegistration.addRecipes(IRecipeType<T>, List<T>)`。
3. **注入熔炉配方**：原件 `recipes/SRPInfuserFurnaceRecipeInit.init()` 共 9 条 = 2 条特殊（铁锭 + 死血 → 半有机锭 + 玻璃瓶；侵染沙 + 死血 → 侵染玻璃 + 玻璃瓶）+ 7 条玻璃置换（32× 侵染玻璃 + 16× 原料 → 2× 对应玻璃：熟肉→血玻璃、侵染陶瓦→灰玻璃、波兰皮块→棕玻璃、哈勒斯金块→丑角玻璃、冰→裹尸玻璃、残渣块→阴郁玻璃、哥特菇→暗影玻璃）。本工程对应实现是 `InfuserFurnaceBlockEntity.GLASS_RECIPES`（7 条玻璃置换的 input→output 映射，private），2 条特殊配方在该类内单独处理——需要暴露一个只读访问器供 JEI 读取。
4. **SRP 酿造配方**：✅ **已实现**（数据包 `data/csrp/recipe/brewing/*.json` 共 7 个文件；校验 `scripts/verify-brewing-recipes.cjs` + GameTest `csrp:brewing_recipes/legacy_srp_brews`）。原件 `potion/Recipe.java` 的 5 个 `IBrewingRecipe` 对应：`alveolar_fluid + 燧石 → 恐惧药水`、`恐惧药水 + 火药 → 喷溅`、`喷溅 + 龙息 → 滞留`、`水/粗制 + 疾病海绵 → 死血`、`水/粗制 + 荆棘浆果 → 荆棘醒酒器`。
   - 关键映射已用官方 **MCPConfig + `mcp_stable-39-1.12` 的 `fields.csv`** 核实：`field_151145_ak` = **FLINT（燧石）**，**不是**地狱疣（原先按 base→potion 用地狱疣的常识推断，查表后纠正）；`field_151016_H` = GUNPOWDER、`field_185157_bK` = DRAGON_BREATH、`field_151068_bn` = POTIONITEM、`field_185155_bH` = SPLASH_POTION、`field_185156_bI` = LINGERING_POTION、`field_151069_bo` = GLASS_BOTTLE、`field_150432_aD` = ICE（与工程 `GLASS_RECIPES` 的 `Items.ICE` 一致 ✓）。
   - 因此 JEI 的酿造分类现在只需**展示**这 7 条已存在的配方，无需再补行为。
5. **催化剂**：注入熔炉方块 → `csrp:infuser_furnace` 分类；酿造台 → `csrp:srp_brewing` 分类。
6. **隐藏 16 个成就图标物品**：原件把它们从 JEI 物品列表里移除（`dark_days_icon`、`adapted_icon`、`primitive_icon`、`crude_icon`、`pure_icon`、`hunt_season_icon`、`guerilla_icon`、`ecstasy_icon`、`enemy_of_enemy_icon`、`fog_nullifier_icon`、`self_destruct_icon`、`potion_columbus_icon`、`potion_stolas_icon`、`hellfire_chemical_warfare_icon`、`cosmic_structural_failure_icon`、`roots_icon`）。26.3 JEI 用 `IJeiRuntime` / `IJeiFeatures` 或 `jei.hide` 配置实现隐藏，需按实际 API 选择。
### 注入熔炉保真化（待办，原件数据已全部核实）

工程当前的 `InfuserFurnaceBlockEntity` / `InfuserFurnaceMenu` / `InfuserFurnaceScreen` 是**简化重写**，与原件差异较大，需要重构：

| 项 | 原件（1.10.9） | 工程现状 | 处理 |
| --- | --- | --- | --- |
| GUI 背景 | `srparasites:textures/gui/infuser_furnace.png`（176×166 区域，256×256 图集） | **原版** `textures/gui/container/furnace.png` | 改用 `csrp:textures/gui/infuser_furnace.png`（**该纹理已在仓库中，但无人引用**） |
| 槽位 | **6 个**：0 熔炼输入 (56,17)、1 燃料 (56,53)、2 原版熔炼输出 (140,17)、3 注入输入 (30,35)、4 注入产物 (140,53)、5 副产物 (116,35) | **4 个**：(44,17)、(62,17)、(56,53)、(116,35) | 按原件坐标恢复 6 槽 |
| 熔炼 | 同时执行**原版熔炉配方**（`FurnaceRecipes`，输入槽 0 → 输出槽 2） | 无 | 补回 |
| 注入消耗 | `doInfuse`：槽 0 与槽 3 各 `shrink(1)`（**各 1 个**，配方里的 32/16 只是 JEI 展示量，1.12.2 的 `Ingredient.apply` 不校验数量） | 槽 0/1 各 shrink(1) | 改为槽 0/3 |
| 注入产出 | `mergeInto(4, r.infusedOut)` + `mergeInto(5, r.bottleOut)`——**主产物 + 副产物** | 只有 1 个输出槽、无副产物 | 补回副产物槽 |
| 玻璃置换产物数量 | **2 个**（`addGlassSwap(y16, x32)` 里 `x32 = new ItemStack(BloodyGlass, 2)` 作为 `infusedOut`） | 1 个 | 改为 2 个 |
| 进度条绘制 | 箭头 `blit(x+80, y+35, 176, 14, w, 16)`；火焰 `blit(x+56, y+36+12-k, 176, 12-k, 14, k+1)` | 原版熔炉坐标 (79,34) / (56,36) | 按原件 u/v 与坐标绘制 |

原件配方全表（`recipes/SRPInfuserFurnaceRecipeInit.init()`，9 条）：

1. 铁锭 + 死血 → 半有机锭 ×1 + **玻璃瓶 ×1**（副产物）
2. `infestedsand` + 死血 → 侵染玻璃 ×1 + **玻璃瓶 ×1**（副产物）
3. ~9. 32× 侵染玻璃 + 16× 原料 → **2×** 对应玻璃（熟肉→血玻璃、侵染陶瓦→灰玻璃、波兰皮块→棕玻璃、哈勒斯金块→丑角玻璃、冰→裹尸玻璃、残渣块→阴郁玻璃、哥特菇→暗影玻璃），无副产物

已完成的部分：输入项已由原版 `SAND` 修正为原件要求的 `csrp:infestedsand`（`BlockFallingInfestedStain("infestedsand")`）。

同时暴露一份只读配方表（输入 1 / 输入 2 / 主产物 / 副产物）供 JEI 注入熔炉分类使用（见上一节）。
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
