# SRParasites 1.10.9 → Minecraft 26.3 移植说明

本文件是交付说明，记录**已完成的内容清单**、**因目标版本 API 缺失而改写或省略的部分**，以及**已知不兼容项**。

---

## 0. 前置确认（开工前已确定）

| 项 | 值 | 依据 |
| --- | --- | --- |
| 目标 Minecraft 版本 | **26.3** | `gradle.properties` → `minecraft_version=26.3`、`minecraft_version_range=[26.3,26.4)` |
| 模组加载器 | **NeoForge 26.3.0.52-beta**（FancyModLoader 12.0.8） | `neo_version=26.3.0.52-beta`、`loader_version_range=[12,)`；构建用 `net.neoforged.moddev` 2.0.147 |
| Java | **25**（GraalVM Community 25.3.4.1） | `java.toolchain.languageVersion = JavaLanguageVersion.of(25)` |
| 原模组 | **SRParasites 1.10.9**（Minecraft 1.12.2 / Forge） | 附件 jar；反编译产物 `_scratch/vf/out109` |
| 差异基线 | SRParasites **1.10.8**（本工程原移植基线） | 反编译产物 `_scratch/vf/out108` |

实现一律按 26.3 的官方 API 与注册体系：`DeferredRegister` / `RegisterEvent` / 数据组件（Data Components）/ 数据包注册表（damage_type、jukebox_song、worldgen、structure、loot_table、recipe、advancement、enchantment、tags）/ `SavedData` / Brigadier / `CustomPacketPayload`，不使用 1.12.2 写法硬套。

---

## 1. 源、反编译基线与合规

- 源 jar **无源码**，先反编译为可读工程作为迁移基础：`D:\code\MC模组\_scratch\vf\out109`（1837 个 `.java`，与 1.10.9 jar 一一对应）。
- **合规**：原 jar 及其反编译产物**不在仓库内、未被上传**。
  - 原 jar 仅存在于 `杂物（别上传）/`，该目录已由 `.gitignore` 规则 `*（别上传）/` 排除（`git check-ignore` 已验证命中）。
  - 反编译树位于仓库**之外**的 `D:\code\MC模组\_scratch`。
  - 仓库内**没有**任何 `.jar`（仅 `gradle/wrapper/gradle-wrapper.jar` 这一标准 wrapper）、没有反编译源码；源码中对上游的引用仅为 javadoc 溯源注释（如 `Port of the 1.12.2 {@code ...block.BlockGore}`）。
- 原 jar **未被覆盖、删除或修改**；未改动原版 Minecraft 与其他模组的任何内容（本模组只注册 `csrp` 命名空间下的内容）。

---

## 2. 交付物

| 交付物 | 位置 |
| --- | --- |
| 完整工程（源码 + 资源 + 构建脚本） | 本仓库（Gradle 工程；Windows 下 `gradlew.bat build`，Niubash 下见 §7） |
| 可加载运行的构建产物 | `build/libs/csrp-1.10.9.jar`（约 134 MB，含全部资源） |
| 移植说明 | 本文件 |
| 各子系统详细报告 | `docs/gap/R2_REPORT.md`、`R2_BLIZZARD_REPORT.md`、`R3_WORLDGEN_REPORT.md`、`R4_BLOCKS_REPORT.md`、`R5_ITEMS_REPORT.md`、`R6_ENTITY_MATRIX.md`、`LANG_REPORT.md` |
| 自动化校验 | `scripts/run-all-verifications.cjs`（147 项）+ `scripts/build-locked.sh`（多 agent 并发构建串行化） |

规模：569 个 Java 文件、88 个方块类、128 个实体文件、44 个物品类、7 个 Mixin、362 个 blockstate、2236 张纹理、1008 个音效、55 个结构 NBT、33 套语言。

---

## 3. 已完成的内容清单

### 3.1 注册系统
- **方块**：全部 id 到位；1.10.9 的 108 个兼容/元数据 id **全部**改用专用类或形状家族（通用 `new Block(...)` 回退 0 个命中）。
- **方块实体**：10 个 `BlockEntityType`，含 1.12.2 兼容 id `legacy_infested_furnace`（`infested_furnace` / `infested_furnace_lit` 是**真实可用**的容器熔炉，非空壳）。
- **物品**：机器化 diff 后原模组 115 个一般物品 id **全部命中**；17 个成就图标统一 `stacksTo(1)`（对齐 `ItemAdvancementIcon` 的 `field_77777_bU = 1`）。
- **实体**：原件 **158 个 id 全部注册**；本工程共 169 个（多出的 11 个是移植实现所需的辅助/拆分实体，见 §5.6）。
- **其他注册**：菜单、粒子、药水、状态效果、音效、唱片（`jukebox_song` 数据包注册表）、伤害类型、附魔、创造模式标签页。

### 3.2 世界生成
- 4 个寄生群系 + 群系装饰（`world/gen/**` 12 个特性类）、结构 NBT 55/55。
- 殖民地基类（原 614 行逐方法转写）+ 核心 + B1-B4 / BS1-BS4、节点保护 1/2/3、陨石撞击、死头巨树与村庄、冷星村庄。
- 运行期已实证：放置节点 → 225 区块 → 四类寄生群系全部落地、36 种寄生体方块生成、零 decoration 失败。

### 3.3 GUI
| 原件界面 | 本工程实现 | 状态 |
| --- | --- | --- |
| `GuiInfuserFurnace` / `ContainerInfuserFurnace` / `TileEntityInfuserFurnace` / `BlockInfuserFurnace` | `client/screen/InfuserFurnaceScreen` + `inventory/InfuserFurnaceMenu` + `block/entity/InfuserFurnaceBlockEntity` + `block/InfuserFurnaceBlock` + `recipe/InfuserFurnaceRecipe(s)` | ✅ 原件纹理、6 槽坐标、双进度、副产物、原版熔炼与朝向方块均已按原件还原（见 §3.5） |
| `GuiConsumedWorkbench` / `ContainerConsumedWorkbench` | 原版 `CraftingTableBlock`（`consumed_workbench` / `infested_workbench`） | ✅ **等价**：原件的容器继承 `ContainerWorkbench`、界面用原版 `crafting_table.png` 与 `container.crafting` 文案、含配方书——即原版合成台界面 |
| `ScannerGui` / `ScannerContainer` | `client/screen/RelayTerminalScreen` + `RelayTerminalMenu` | ✅ **等价**：两者都由 `noderelay` / `relaycontroller` 打开，扫描按钮与模块槽位一致 |
| `GuiGameOverEscape` + `EscapeClientState` | `client/EscapeDeathScreenEvents` + `EscapeClientState` | ✅ 本轮补齐（见 §3.6） |
| `GuiDislodgementReport` / `GuiVectorMapReport` | `relay/RelayScanReportFactory` + 报告物品 | ✅ |
| 世界创建选项 | `client/SrpWorldSettingsScreen` + `SrpWorldPreview` + 入口按钮 | ✅ 本轮按原件结构恢复（见 §3.5） |
| `StarfieldBackground` / `GuiButtonCycle` | 由上述界面内联实现（原件的循环按钮基类） | ✅ 等价 |

### 3.4 配置项
5 个 TOML：`csrp-general.toml` / `csrp-mobs.toml` / `csrp-systems.toml` / `csrp-world.toml` / `csrp-block-conversions.toml`，覆盖原 `SRPConfig` / `SRPConfigMobs` / `SRPConfigSystems` / `SRPConfigWorld` / `SRPConfigBlockConversions` 的行为面（含本轮补的 `*NeededAssimilation` 12 项、`parasiteBlockDrops`、`worldCreationUi`、`escapeEnabled` / `escapeMinDistance` / `escapeMaxDistance`）。

### 3.5 本轮（R2–R6 + 收尾）新增
- **R2 1.10.9 增量**：刷怪清理阈值 4→6 / 2→3 + 50 tick 冷却；`doTileDrops` / `doMobEvolution` 运行时开关；`/srparasites` 三个缺失子命令（`toggle_dotiledrops` / `toggle_domobevolution` / `readconfigurationfile`）；12 项同化门槛 + 世界级同化计数持久化；暴风雪客户端渲染与星型同步差集；碎裂地形（默认关闭）。
- **R3 世界生成**：见 §3.2。
- **R4 方块保真化**：108 个 id 全部专用实现（36 个新方块类 + 形状家族）。
- **R5 物品补齐**：20 个 id（唱片、报告、弓镰部件、图标、3 个刷怪蛋）+ `LegacyMobSpawnerItem` 32 处实体映射修正。
- **R6 实体/AI**：73 个 AI 类逐条判定（已实现 65 / 不一致 3 / **缺失 0** / N/A 5）；补齐同化门槛、SoundEater 潜行、Venkrol 龙卷、NexusProtection 触发、BlockLight / BlockResidue / CircleGroup。
- **语言**：33 套 `.lang` → 26.3 JSON；`en_us` 2332/2332、`zh_cn` 2140/2140 全覆盖。
- **世界创建选项**：恢复原件的「*SRP World Options...* 入口按钮 → 专用 *SRP World Settings* 界面（5 个选项 + 动画世界预览 + Done）」，替换此前被硬塞进原版创建世界界面底部、会被原版页脚盖住的 5 个按钮；补 `GeneralConfig.worldCreationUi`（原 `SRPConfig.worldGIU`）。
- **纹理缺失修复**：Kirin 光束/牵引线此前引用 26.3 已迁移的 vanilla 路径（`textures/entity/guardian_beam.png` → 现为 `textures/entity/guardian/guardian_beam.png`），会渲染成缺失纹理；`OrbBoom` 与投射物默认纹理引用了从未随包的 `scary_orb.png`。新增 `scripts/verify-texture-references.cjs` 全量校验（含对照 26.3 客户端 jar 校验 vanilla 路径）。
- **实体渲染器覆盖修复**：26.3 对未注册渲染器的实体类型**没有兜底**（`EntityRenderDispatcher.getRenderer` 是普通查表，`extractEntity` 直接解引用结果），而两个移植自有的不可见伤害载体实体 `crux_block_damage` / `haunter_damage` 会被 `addFreshEntity` 同步到客户端——一旦渲染即崩溃。已按同类不可见实体的既有做法注册 `NoopRenderer`，现 **169/169 实体 id 均有渲染器**（`scripts/verify-entity-renderers.cjs` 断言）。
- **酿造配方恢复（玩法缺口）**：原件 `potion/Recipe.java` 用 5 个 `IBrewingRecipe` 注册了恐惧药水三级链、死血、荆棘醒酒器；NeoForge 26.3 移除了 `IBrewingRecipe` 与 `RegisterBrewingRecipesEvent`，本工程此前只留下一个空的 `ThornshadeBrewingEvents` NEEDS-DESIGN 占位类，**配方实际不可用**。现按 26.3 的数据驱动方式恢复为 `data/csrp/recipe/brewing/*.json` 共 7 个文件（恐惧药水链 3 条 + 死血 water/awkward 2 条 + 醒酒器 water/awkward 2 条），删除空壳类；校验 `scripts/verify-brewing-recipes.cjs` + GameTest `csrp:brewing_recipes/legacy_srp_brews`（经真实 `RecipeManager` 加载并匹配）。
  - **字段映射经官方映射表核实**：从 `MinecraftForge/MCPConfig` 的 `joined.tsrg` 与 Forge maven 的 `mcp_stable-39-1.12.zip`（内含 `fields.csv`）查得 —— `field_151145_ak` = **FLINT（燧石）**、`field_151016_H` = GUNPOWDER、`field_185157_bK` = DRAGON_BREATH、`field_151068_bn` = POTIONITEM、`field_185155_bH` = SPLASH_POTION、`field_185156_bI` = LINGERING_POTION、`field_151069_bo` = GLASS_BOTTLE、`field_150432_aD` = ICE。其中燧石一项**纠正了此前的常识推断**（曾误判为地狱疣），ICE 一项同时证实了工程注入熔炉玻璃置换映射（现为 `recipe/InfuserFurnaceRecipes`）的正确性。
- **注入熔炉保真化**：此前是简化重写（4 槽、原版熔炉纹理、单进度、无副产物、无原版熔炼、非朝向方块）。现按原件 `TileEntityInfuserFurnace` / `ContainerInfuserFurnace` / `GuiInfuserFurnace` / `BlockInfuserFurnace` 还原：**6 槽**（熔炼输入/燃料/熔炼输出/注入试剂/注入产物/副产物）坐标与原件纹理上的槽位框一一对应、**两个独立 200 tick 计数器**共用一个燃料缓冲、注入产出**主产物 + 玻璃瓶副产物**、玻璃置换产出 **2 个**、**同时执行原版熔炼**、GUI 改用原件 `infuser_furnace.png`（此前该纹理在仓库里却无人引用）、方块恢复 `FACING` 四变体。配方表独立为 `recipe/InfuserFurnaceRecipe(s)`（9 条，镜像原件 `SRPInfuserFurnaceRecipeInit`）。
- **移除了一处臆造玩法（保真修正）**：本工程此前新增了「寄生体攻击时同化附近龙蛋」的机制（`DragonEggAssimilationEntity` + 渲染器 + 进度 `csrp:again` + 语言键，共 30 个文件、38 处调用点）。经核对，**原件 1.10.9 与 1.10.8 都完全没有该玩法**（`EntityInfDragonE` 无任何 egg 引用，`EntityParasiteBase` 的攻击逻辑也无方块转化），其引入提交标题即为「添加龙蛋同化末影龙事件」。为遵守「不要添加原模组没有的新生物、新玩法或新系统」，该功能已完整移除。同时清除了 `data/csrp/advancement/csrp/` 下 **39 个陈旧重复进度**（会以 `csrp:csrp/*` 重复注册，其中 24 个原件根本没有）；现进度集与原件**恰好一致（40 个）**，由 `scripts/verify-advancements.cjs` 断言。
- **`/harlequin_here` 与极端降雪系统**：harlequin_here 按原件（权限 2、`[radiusChunks=1]`、改写 (2r+1)² 个区块的群系并推送客户端）实现，26.3 以 `ChunkAccess#fillBiomesFromNoise` + `ChunkMap#resendBiomesForChunks` 表达（原版 `FillBiomeCommand.fill` 有 32768 方块体积上限，原件无此限制，故刻意不用）。原件的极端降雪系统（`ExtremeSnowData`/`ExtremeSnowServer`/`ExtremeSnowNetwork`/`ClientExtremeSnow`）已按 parity 移植并接线，但**其唯一开关 `/srp_extremesnow` 在原件中从未注册**（全树唯一的 `setEnabled` 调用点就在该命令内部，且原件语言文件无任何相关条目），因此该状态在原版永远是关闭的——本工程同样**不注册**该命令，避免给玩家一个原版没有的管理命令。详见 §4.1 与 `PORT_BACKLOG.md`。
- **JEI 配方查看器集成**：原件 `compatibility/jei/**` 此前未移植。现补齐 `compat/jei/SrpJeiPlugin` + `InfuserFurnaceJeiCategory`（注入熔炉分类、催化剂、隐藏 16 个成就图标），JEI 以 `compileOnly` 引入，不装 JEI 也能构建运行；酿造那 7 条配方由 JEI 内置酿造分类自行展示。详见 §4.1。
- **Escape 系统补齐**：见 §3.6。

### 3.6 Escape（死亡循环逃脱）系统
原件 `feature/EscapeOnDeathHandler` + `feature/EscapeRespawnHandler` + `client/EscapeClientState` + `client/gui/GuiGameOverEscape` + `network/msg/C2SRequestEscape` + `S2CSetEscapeOffer`，本工程此前只迁移了语言键、**代码缺失**，本轮补齐：

- 被寄生体致死且**一分钟内超过 5 次** → 置 `srp_offer_escape` 并下发 `escape_offer` 包；
- 客户端死亡界面出现「DeathLoop Prevention |200-300 blocks」按钮；
- 点击 → `escape_request` 包置 `srp_escape_pending` 并重生；
- 重生时在 `escapeMinDistance`(200) ~ `escapeMaxDistance`(300) 范围内随机寻找安全落点（要求脚下可站立且非仙人掌、脚部与头部均为空气且非液体，最多 24 次尝试）并传送。

### 3.7 资源
结构 NBT 55/55、音效 1008/1008、纹理 2236 张（与原件 1:1，`textures/blocks/` 按 26.3 规范改为 `textures/block/`）、33 套语言、全部 blockstate / 模型 / 物品定义。**未新增任何写实渲染或网页式 UI**；新增界面沿用原件 16×16 像素纹理与既有调色板（世界预览面板直接使用原件 `textures/gui/worldsettings/*.png`）。

### 3.8 收尾轮：harlequin 命令补全、进度语言键归一、映射表核实

- **`/harlequin_convert`**（权限 2，原件 `network/CommandHarlequinConvert` 176 行 + `util/convert/HarlequinBlockConverter` 164 行）：斑块播种 + 逐列向下转换两遍，参数默认值 16/1/32/18/3/5、上界 256/128/256/256/64/64、30% `lipoma_mass` 副产、`max(5, top-depth)` 深度限制全部按原件。新增 `world/gen/HarlequinBlockConverter` 与 `command/HarlequinConvertCommand`。
- **`/harlequin_scatter`**（权限 2，原件 `network/CommandHarlequinScatter` 225 行）：六种类型、`tries`/`radius` 默认 32/32 与 1..4096 / 8..512 上界、四类模板清单（20 个，`data/csrp/structure/` 下全部存在）、地面/顶面规则、四种平坦度阈值（5/3、3/3、6/3、10/4）与 `both`/`all` 的组合数量全部按原件。新增 `command/HarlequinScatterCommand`。至此原件 `CommonProxy#registerServerCommand` 的 18 条命令全部核对完毕。
- **1.12.2 方块映射不再靠推断**：用官方 MCP stable_39 的 `fields.csv` 逐个查表确认原件比较的 SRG 名（`field_150354_m`=SAND、`field_150322_A`=SANDSTONE、`field_180395_cM`=RED_SANDSTONE、`field_150344_f`=PLANKS、`field_150347_e`=COBBLESTONE、`field_150341_Y`=MOSSY_COBBLESTONE、`field_150348_b`=STONE、`field_150349_c`=GRASS、`field_150346_d`=DIRT、`field_150355_j`/`field_150358_i`=WATER/FLOWING_WATER、`field_150353_l`/`field_150356_k`=LAVA/FLOWING_LAVA）。据此修正 `world/gen/HarlequinRockBushGen` 三处偏差：地面由「红沙/砂砾」改回**砂岩/红砂岩**、高度图由 `MOTION_BLOCKING_NO_LEAVES` 改回原件的 `MOTION_BLOCKING`、冰不再被当作坏顶面。
- **原件的 `maybePlaceHair` 是死代码**（证据：`Block#canSustainPlant` 的 Forge 默认值为 `false`，1.10.9 全树对该方法的引用只有转换器自己这一处），故 `harlequin_convert` 在原版从不长出发丝/硬毛。本工程**按原样保留这道关闭的闸门**（`canSustain` 恒 `false`），未改成 `canSurvive`——否则会凭空长出原件没有的植物。GameTest 断言转换后的草上方仍是空气。
- **进度（advancement）语言键归一**：原件有 4 个进度文件写的是单数 `advancement.<ns>.<id>.title/.desc`（1.12.2 的笔误），工程的转换器（`scripts/convert-lang-109.cjs`）把它们归一为 `advancements.csrp.<id>.title/.description`。本轮把数据文件也改成同一套键（`cosmic_structural_failure`、`hellfire_chemical_warfare`、`sepeku`、`thornshade_self_destruct`，`data/` 与 `assets/` 两份），并清掉 67 个**原模组没有、也没有任何数据/代码引用**的臆造别名键（`advancements.csrp.<语义名>.desc` 等）。现每个语言文件的 `advancement(s).csrp.*` 键集合 = 原件 `advancement(s).srparasites.*` 经同一规则映射后的集合（`en_us` 82 键、`hr_hr` 78、`ko_kr` 80、`zh_cn` 82 …），既无缺键也无多余键；`en_us` 2911 键、`zh_cn` 2927 键。
- **寄生群系雾（原件雾系统的两个分支全部到位）**：原件 `SRPEventHandlerBus` 的 `FogDensity` / `FogColors` 两个钩子：
  - **血液分支**：玩家浸入 `SRPBlocks.PARASITEBLOOD` 时 `EXP + density 1.0F`、雾色 `(0.1, 0.6, 0.2)`。`SRPBlocks.PARASITEBLOOD` 在原件里是流体 **Material**（不是方块），故 26.3 用 `LocalPlayer#isInFluidType(csrp:deadblood)` 判定。
  - **群系分支**：服务端逐玩家把雾浓度以 `4.5E-4F`/tick 推向 `biomeFogDensity`（默认 `0.06F`），离开寄生群系与节点影响范围后以 `8.0E-4F`/tick 退雾，颜色取当前寄生群系的 `biomeOne..FourFog*`（Shrouded/Boils/Harlequin/Demen，0..255 ÷ 255）；客户端在 `fog > biomeFogDensity × 0.2` 时换色，两个分支都受**夜视**豁免（血液分支不受）。
  - 为此补齐的前置：`WorldConfig` 的 `biomeFogDensity` + 12 个雾色 + `nodeTwoTime`/`nodeThreeTime`（10/40）+ `nodeRangeEffectsOne/Two/Three`（400/1600/8000）与 `nodeRangeSpread*`（200/800/4000）；`SrpWorldData#convertDayToAgeNode` / `getDistanceEffectByAge` / `getDistanceSpreadByAge` / `nearestHeartAge` / `nearestHeartType`（含原件「覆盖范围内**最远**的节点胜出」这一怪癖）；`SrpCoreSystems.nodeStage` 改为走可配置阈值；`ParasiteBiomeDecorator#parasiteBiomeIndex` / `nodeBiomeIndex`（`SRPReference#getBiomeFromInt` 只把类型 3 映射到 Harlequin、其余映射到 Shrouded）。新增 `world/ParasiteBiomeAmbienceServer` + `network/BiomeFogPayload`（`csrp:biome_fog`，只在浓度仍在变化时发送，与原件一致）。
  - 26.3 改写点：`EntityViewRenderEvent.FogDensity` 已不存在，指数密度按工程既有换算（`ClientExtremeSnow.density()`：`density = sqrt(ln 50) / 能见度`）折算成 `ViewportEvent.RenderFog` 的线性 near/far；原件对所有雾模式都覆盖，故不加 `FogType` 过滤；客户端的 `fog` 静态量在换世界时清零（对应原件 `onWorldLoad(Unload)`）。
  - **按原件保留的怪癖**：原件把 `fog` 记在 `SRPEventHandlerBus` 的 **static** 字段上，服务端全体玩家（与所有维度）共用同一个累加值，最后一个被 tick 的玩家决定所有人看到的浓度。本工程按原样保留（改掉就偏离原件行为），已在代码注释与本节写明。
  - 校验：`scripts/verify-parasite-fog.cjs`（客户端两分支）+ `scripts/verify-biome-fog.cjs`（配置默认值、节点查询、服务端推进、包注册）+ 2 个 GameTest（`biome_fog/node_influence`、`biome_fog/node_stage_thresholds`）。
- **节点心跳音（`biomeHeartFreq` + `SRPPacketMovingSound(-1, vol)`）**：原件每 `biomeHeartFreq`（默认 30，0..100）个玩家 tick 重置一次计数器，若玩家处在寄生群系内则取 `nearestHeartAge(pos, spread = true)`（**扩散**半径表）的档位与 `getDistanceSpreadByAge(age, true)` 的平方扩散半径 `totalS`，再用 `isInRangeOfHeart(pos, totalS)` 取到节点的平方距离算出音量 `1 - distSq/totalS` 并发包；客户端 `ClientProxy#playMovingSound(-1, v)` 以 `pitch 1.0F、volume v × biomeHeartVol`（默认 0.4）播放 `srparasites:biome.heart`（本工程 `csrp:biome.heart`——音效文件与 sounds.json 条目原本就在仓库里，却从未被播放过）。
  - 新增 `world/ParasiteBiomeAmbienceServer`（雾与心跳在原件里同属一个 `playerTick`，故同处一个方法）、`network/NodeHeartPayload`（`csrp:node_heart`）、`client/sound/ClientNodeHeart`（`playLocalSound(..., SoundSource.NEUTRAL, v × biomeHeartVol, 1.0F, false)`，对应原件 `PositionedSoundRecord.func_194007_a(BHEART, 1.0F, v × biomeHeartVol)`）。
  - 前置补齐：配置 `biomeHeartFreq`(30) 与 `biomeHeartVol`(0.4)；`SrpWorldData#isInRangeOfHeart`（保留原件「半径先自增 1、取**第一个**命中而非最近」两处怪癖）与 `nearestHeartAge`/`nearestHeartType` 的 `spread` 重载。
  - 按原件保留的怪癖：心跳计数器与雾累加值一样是共享的（原件 `SRPEventHandlerBus.heart`），多人下按玩家人数成比例加快。另：`nodeRangeSpread*` 被配置成 0 时原件会除以 0，本工程在该处直接跳过并注明。
  - 校验：`scripts/verify-node-heart.cjs` + GameTest `biome_fog/node_influence` 中的 `isInRangeOfHeart` 与 spread 重载断言。
- **进化阶段音乐（原件 `SRPPacketMovingSound(1..10)`）**：`EvolutionSystem#announcePhaseChange` 在阶段上升到 1..10 时向该维度每个玩家发原生 `ClientboundSoundPacket(Holder.direct(ModSounds.evolutionPhase(current)), SoundSource.NEUTRAL, x, y, z, 0.75F, 1.0F, seed)`——与原件 `ClientProxy#playMovingSound` 的 `case 1..10`（`pitch 1.0F`、`volume 0.75F`、`SoundCategory.NEUTRAL`）逐参数一致（26.3 用原生声音包替代原件的自定义包）；原件 `warning = 0`（-1→0）与 `-7`（1→0 下降）在客户端 switch 里**没有对应 case**，即不发声，工程的 `advanced && current >= 1` 条件与之一致。本轮修正了此前 `volume 1.0F` / `SoundSource.MASTER` 两处偏差。校验：`scripts/verify-node-heart.cjs`。
- **OST 调度（原件 `SRPEventHandlerBus#soundTwo` / `soundThree` / `getMusicPhase` / `resetSouncTicker`）**：原件客户端唯一会主动播放 OST 的地方，此前完全缺失——10 首阶段曲、`srparasites.biome`、`srparasites.scent` 与对应 sounds.json 条目都在仓库里，却没有任何代码播放它们。现已按原件实现（新增 `client/sound/ClientPhaseMusicScheduler`）：
  - 每 tick 递减 `clientVector`（收到 `vector = true` 的答复时置 200）；`clientVector <= 0` 时整段调度跳过，并在刚跨过 0 的那一 tick 停掉 MUSIC 轨（对应原件 `originActivated && clientVector <= 0` 分支——工程没有 `originActivated` 开关，故始终走原件默认值 true 的分支）。
  - `musicTimer` 到点、无待清理曲目且 `clientCurrentEvoPhase > 0` 时：先 `stop(null, SoundSource.MUSIC)`，再播放当前处境的曲子——寄生群系内 `srparasites.biome`，否则 `srparasites.phaseone..phaseten`（原件 `getMusicPhase` 的 1..10 映射），随后按 `musicPhaseMin..musicPhaseMax`（新增配置，默认 14420..18200）重掷下一次延迟。
  - `ScentMusicSound` 开始时把 `clientScent` 置 5000（对应原件 `ClientProxy#playMovingSound case 102`），scent 曲优先于阶段曲；`clientScent` 归 -1 时停 MUSIC 并置 -10。
  - vanilla 背景音乐在「有寄生曲该播」时由 `PlaySoundEvent` 直接丢弃（原件把流式曲目排队到下一 tick 再移除，26.3 里等价于不让它开始）。
  - 网络：新增 `csrp:request_evolution_phase`（客户端每 20 tick 询问）与 `csrp:evolution_phase_client`（服务端回 `(维度阶段, 是否处于感染向量半径内)`，对应原件 `SRPSaveData#getEvolutionPhase` + `SRPWorldData#nearestInfectionPosition(false, pos) != null`）。
  - 校验：`scripts/verify-phase-music.cjs`。
- **文档与工具**：`AGENTS.md` 的三节过时描述（1.21.1 / NeoForge 21.1 / Java 21 / `gameTestServer`）改为实际的 26.3 / NeoForge 26.3.x-beta / Java 25 / `runGameTestServer`；`scripts/build-locked.sh` 改用 `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain`（本环境 Niubash 无法执行 `gradlew.bat`，会静默挂起）。

### 3.9 R6 遗留项收敛轮：mobAttackingFull、beckon 召唤表、gene 三开关、nexus 生长闸门

R6 实体矩阵 §5.2 登记的 6 条"记录为不一致、本轮不改"中，4 条在本轮补齐（另 2 条为有意的结构性等价，保持不变）：

- **`mobAttackingFull`**（原件 `SRPConfig.mobAttackingFull`，默认 false）：新增 `Config.mobAttackingFull` 与 `entity/MobAttacking`，后者逐字复刻 `ParasiteEventEntity#checkName` 的 `String#contains` 匹配、`checkEntity` 的实体 id 取值，以及 `EntityParasiteBase#canAttackClass` 的 `!mobAttackingFull || !checkName(...)` 判定。`ArchitectEntity` / `DredgeEntity` / `HeedEntity` / `PureParasiteEntity` 4 处此前各自手写的黑名单比较改为统一调用。
  - **按原件保留的冗余**：原件的 `mobAttackingFull` 只在 `canAttackClass` 里生效，而各家族的目标选择谓词本身已经无条件排除黑名单（`!ParasiteEventEntity.checkEntity(...)`），所以该配置对"是否锁定黑名单怪"实际没有额外效果。本工程未"顺手修正"，保留了同一层冗余。
- **`EntityAIVenkrolSummon` 的 mob 表**（原件 `SRPConfigMobs.venkrolmoblist` / `venkrolsiimoblist` / `venkrolsiiimoblist` / `venkrolsivmoblist`）：`MobsConfig` 新增 4 张 beckon 召唤表，逐行复刻原件默认值（含 `ground;air` 前缀、chance、capacity cost），并补 `venkrolCAMinimumV`(4) / `venkrolCAExtraM`(0.5)。`NexusParasiteEntity#summonBeckonParasites` 由硬编码 switch 改为读表：地面/空中按原件 `getY()+3 > target.getY()` 切换，选怪按原件"随机起点 + 最多两圈 + 容量足够才取"的 `spawnBiomassFromBeckon` 流程。
- **`EntityAINexusGrow` 的三个配置闸门**：`Config` 新增 `evolutionNests`(11) / `deveNestsUse`(3) / `maximumStageList`({"1;3"})，并提供 `nexusStageLock(Level)`（含 1.12.2 数字维度 id → 26.3 维度的映射）与 `legacyDimensionId(Level)`；`NexusParasiteEntity` 构造时求 `dimensionStageLock`，`evolve()` 应用原件 `dimLock >= stage+1` 的停长闸门，`trySpawnRooterSi` 应用 `evolutionNests` / `deveNestsUse` 的 nest 解锁闸门。
- **gene 系统缺的 3 个开关**：`EvolutionSystem.GenerationProfile` 此前缺 `lookWalls`(2) / `waterLeap`(4) / `residue`(8)（原件 `SRPConfigSystems.generation*0..5` 共 13 项，工程只有 10 项）。现补齐三张默认表并接入：`lookWalls` → `PrimitiveParasiteEntity` 的目标感知（原件 `shouldCheckSight || !getGeneMod(2)`，因 26.3 的 `mustSee` 在构造期固定，改在逐候选谓词里判定）；`waterLeap` → `DredgeEntity` 的液体跃击；`residue` → `AdaptedVariantEntity.BlockResidueGoal`。

校验：`scripts/verify-mob-attacking-and-beckon-tables.cjs`、`scripts/verify-generation-genes-and-nexus-gates.cjs`（静态校验总数 132 → **134**）；`runGameTestServer` 仍 **187 / 187 通过**，`build` 产出 `build/libs/csrp-1.10.9.jar`。

### 3.10 寄生体掉落表回归原件（1.20.1 简化版 → 1.10.9 配置驱动）

原件 1.10.9 **不使用原版掉落表**：所有寄生体的掉落都由 `util/handlers/SRPEventHandlerBus#setLoot`（out109 1583-2175 行）在 `LivingDropsEvent` 里按 `SRPConfigMobs.<x>Loot` 字符串数组自己掷骰。本工程早期直接继承了 1.20.1 兄弟分支的数据驱动掉落表（`ref1201/src/main/resources/data/csrp/loot_tables`），其中不少条目与 1.10.9 配置不符：

- `sim_human` / `sim_cow` / `sim_sheep` / `sim_wolf` / `sim_pig` / `sim_villager` / `sim_horse` / `sim_bear` 等缺少 `lurecomponent2`（以及 `false_apple` / `fishlin`）；
- `rupter` 等本应掉 `lurecomponent1` 的怪改掉了别名物品 `rupter_viscera`；
- `pri_longarms` / `pri_manducater` / `pri_reeker` / `pri_summoner` / `pri_bolster` / `pri_arachnida` 等本应同时掉 `ada_*_drop` 与 `bone`，工程只掉了 lurecomponent；
- 多张表还多了 `killed_by_player` 限制（原件对任何击杀都掉落）。

现按原件恢复：

- 新增 `world/ParasiteLootTables`：把 `setLoot` switch 引用的 **98 个** `SRPConfigMobs.*Loot` 数组逐字转写成 `entry -> (item, chance, count, always)`，按注册实体路径索引（**105 个**实体；`getParasiteIDRegister()` 通过 `extends` 链解析，因此像 `ada_burrower`（id 57）会正确继承 `shycoLoot`，而 `pri_burrower`（id 318）在原件 switch 里没有 case、无掉落）。
- 新增 `event/ParasiteLootEvents`：`LivingDropsEvent`（`HIGHEST` 优先级，先于其他掉落追加者）里复刻原件 `loot()` 算法——`always=true` 逐条 `rand.nextInt(100) <= chance-1` 后掉 `rand.nextInt(count)+1`；`always=false` 的条目先等概率抽一条再掷骰；同时保留原件 `doMobLoot`、`DEBAR`、dislodgment 代码 18 三个闸门。
- **命名空间改写**：原件用 `srparasites:`，本工程为 `csrp:`，`resolve()` 在查表前改写；查不到的 id（未注册的 `ada_burrower_drop`、附属模组的 `srpmeshi:*`）与原件的 `Item.func_111206_d` 返回 `null` 一致，不掉落。
- 因为 `setLoot` 是寄生体掉落的唯一来源，处理器会先清空数据驱动表的结果再按配置掷骰；数据驱动 JSON 保留作为其他未覆盖实体的来源。

校验：`scripts/verify-parasite-loot-tables.cjs` + GameTest `csrp:parasite_loot/legacy_roll`（用固定种子真实跑 `ParasiteLootEvents.roll`，断言 rupter 只掉 `lurecomponent1` 且数量 1-2、sim_human 两种结果都能出现且数量为 1、ada_burrower 继承三条 shycoLoot 且不掉未注册的 `ada_burrower_drop`、pri_burrower 无表）。静态校验 134 → **135**，GameTest 186 → **187**。

### 3.11 随机起源（EIV）创建补齐

原件的 `SRPEventHandlerBus#worldTick` 有一个每日一次的闸门：`counerW` 每 tick 自增，超过 `SRPConfig.dayTickValue`（24000）后清空 `worldsChecked` 并置 `counerW = -150`，接下来 150 tick 内对每个维度调用 `EIVUtil.createRandomOrigin(world, originCreatingDistanceMin, originCreatingDistanceMax)`（out109 `util/EIVUtil.java:12-72`）。工程此前只有陨石撞击与 `/srparasites` 命令能产生 vector，缺少这个周期性创建。现补齐：

- `WorldConfig` 新增原件 `SRPConfigWorld` 的 7 项：`originActivated`(true) / `originCreatingRand`(10) / `originCreatingRandZero`(3) / `originCreatingDistanceMin`(1000) / `originCreatingDistanceMax`(10000) / `originHealth`(350) / `originRadius`(200)。
- `SrpCoreSystems#createRandomOrigin` 逐行复刻原件：已有 vector 时 1/`originCreatingRand`、首个时 1/`originCreatingRandZero` 每日掷一次；随机取一名玩家、随机角度、距离在 min..max 之间；位置为 `player + (dx, 64, dz)`；最后走 `placeVector(level, pos, originHealth, originRadius)`。
- 挂在 `SrpCoreSystems#advanceDailyState`（每 24000 tick 一次，与原件每日一次语义一致）。
- 原件创建后的 `originNewMess` / `originNewOutbreakMess` 公告默认都是空字符串（`SRPConfigWorld.java:625-626`），空消息不发声，与原件一致。

校验：`scripts/verify-origin-creation.cjs`。静态校验 135 → **136**，GameTest 仍 **187 / 187**。

### 3.12 作物抑制与寄生群系治疗惩罚回归原件

对照 `SRPEventHandlerBus#cropGrow` 与 `#entityHeal`（out109 377-408 / 606-653）发现两处缺失，现补齐：

- **作物生长抑制**：原件先取 `nearestHeartAge(pos, false, 0)`，当 `nodeAge` 在 1..3 且 `rand < nodeCropStopNodeX` 时拒绝生长；只有节点没拦下才走相位表（`cropGrowStunnedOne..Eight`，9 元素数组，因此只有相位 1..8 生效，`cropGrowStunnedNine/Ten` 是死配置）。工程此前只有相位表，且表尾多了相位 9/10 = 1.0（原件不抑制）。现补 `WorldConfig.nodeCropStopNodeOne/Two/Three`(0.3/0.6/1.0) + `nodeCropStopChance(int)`，`EvolutionEvents#slowCropGrowth` 先节点后相位，并把 `EvolutionSystem.CROP_BLOCK_CHANCE` 的相位 9/10 改回 0.0。
- **寄生群系治疗惩罚**：原件在玩家/生物处于 `BiomeParasiteBase` 时按 `canBiomeStillExistType` 选 `biomeOne..FourHealPenalty`（默认均 0.5）缩放治疗量；玩家不做名单检查，其他生物命中 `biomeHealPenaltyBlackList`（默认 `minecraft:villager_golem`，非白名单）时 **直接 return，连后续的进化禁疗也一起跳过**；随后 `phase >= evolutionNoParasiteHealing`（默认 9）时再乘 `evolutionNoParasiteHealingValue`（0.5）。工程此前只硬编码了相位 ≥ 9 的 0.5 倍。现补 `WorldConfig.biomeOne..FourHealPenalty` + `biomeHealPenaltyBlackList` / `biomeHealPenaltyBlackListInverted` + `biomeHealPenalty(int)` / `biomeHealPenaltyExempt(String)`（`listed != inverted`，复刻 `checkName`），`Config.evolutionNoParasiteHealing`(9) / `evolutionNoParasiteHealingValue`(0.5)，`EvolutionEvents#halveNonParasiteHealing` 按原件顺序重写。

校验：`scripts/verify-crop-and-heal-penalty.cjs`。静态校验 136 → **137**，GameTest 仍 **187 / 187**。

### 3.13 生物生成时的 COTH 补齐

原件的 `SRPEventHandlerBus#onEntitySpawn`（out109 1089-1135）+ `#writeCOTHTag` + `#setCOTH`（1137-1443）会给新生成的生物施加 Call of the Hive：

1. 若 `SRPConfigWorld.nodesActivated && SRPConfigSystems.cothActive && nearestHeartAge(pos, false, 0) != -1`，直接给 COTH（3600 tick, amp 0）——该分支对所有非玩家 `EntityLivingBase` 生效；
2. 否则，无 `srpcothimmunity` 标签的非寄生生物会写标签并按 `setCOTH(in, phase)` 掷骰：`mobSpawningCOTHChanceOne..Eight`，相位 1..5 为 0.0、6/7/8 为 0.2/0.4/0.8，而 `Nine/Ten` 虽然存在但 switch 没有 case（相位 9/10 不施加）。

工程此前只在攻击路径调 `InfectionMechanics.applyCothEffect`，没有生成路径。现补：

- `Config` 新增 `cothActive`(true) 与 `mobSpawningCothChanceSix/Seven/Eight`(0.2/0.4/0.8) + `mobSpawningCothChance(int phase)`（只有相位 6/7/8 非 0，其余 0.0）；`WorldConfig` 新增 `nodesActivated`(true) / `coloniesActivated`(true)（原件 `SRPConfigWorld` 开关，此前工程完全没有）。
- 新增 `event/CothSpawnEvents`：`EntityJoinLevelEvent` 上按原件顺序（先节点后相位），免疫判定复用已有的 `InfectionMechanics.isCothImmune`，`srpcothimmunity` 标签用等价的 persistent-data 标记代替（原标签只用于防重复处理）。

校验：`scripts/verify-coth-spawn.cjs`。静态校验 137 → **138**，GameTest 仍 **187 / 187**。

### 3.14 死血对相邻水/岩浆/海绵的转换

原件 `SRPEventHandlerBus#onNeighborNotify`（out109 472-570）在方块变化时把死血周围的方块转成寄生方块：

- 变化的是水且邻接死血 → 水变 `parasitestain` 元数据 1（`MUD`）；
- 变化的是岩浆且邻接死血 → 岩浆变 `parasiterubble` 元数据 7（`OBSIDIAN`）；
- 变化的是海绵且邻接死血 → 海绵变 Diseased Sponge；
- 变化的就是死血 → 相邻的水/岩浆/海绵全部按上述规则转换。

工程此前只有 `DiseasedSpongeEvents#onSpongePlaced`（只处理“放海绵”一种情况），没有水/岩浆转换，也没有全方向的邻居通知处理。现新增 `event/DeadBloodConversionEvents`（`BlockEvent.NeighborNotifyEvent`，逐分支复刻原件，保留原件 `srpSoakGuard` 静态防重入，死血改用 `DeadBloodFluid` 流体类型判定）。

校验：`scripts/verify-deadblood-conversion.cjs`。静态校验 138 → **139**，GameTest 仍 **187 / 187**。

### 3.15 玩家死亡转化回归原件（COTH 等级驱动）

原件 `SRPEventHandlerBus#playerDeath`（out109 2297-2336）是原版唯一的玩家死亡转化入口，触发条件是玩家带着 COTH 死亡（不要求被寄生体杀死）：

- `cothActive && infadventurerEnabled && infadventurerSpawnBy && player.hasEffect(COTH_E)`；
- COTH 等级 ≥ 2 → 生成 `EntityInfPlayer`（`sim_adventurer`），继承 **头/腿/脚** 三个装备槽（原件的 `func_184201_a` 只搬了 HEAD/LEGS/FEET），并命名与显示玩家名；
- COTH 等级 == 1 → 生成 `EntityInhooM`（`incompleteform_medium`），同样命名与显示。

工程此前的 `convertKilledPlayer` 是另一套设计：要求 `attacker instanceof Parasite` 且过 COTH 概率掷骰，且永远只生成 Adventurer，没有 COTH I 的 Incomplete Form 分支，也不继承原件的三个槽。现按原件重写该方法（仍是 `LivingDeathEvent` → `convertTerminalCothHost` → `convertKilledPlayer` 同一调用点，`convertKilledHost` 本身已排除玩家，故不会双重转化），并同步更新 `scripts/verify-infection-conversion-port.cjs` 的断言。

校验：静态校验仍 **139 / 139**，GameTest 仍 **187 / 187**。

### 3.16 对齐运行时 NeoForge 26.3.0.52-beta（FancyModLoader 配置类型重命名）

用户在 `D:\MC\.minecraft\versions\26.3模组开发测试` 运行 `csrp-1.10.9.jar` 时在 Bootstrap 阶段崩溃：

```
java.lang.NoSuchFieldError: Class net.neoforged.fml.config.ModConfig$Type does not have member field
  'net.neoforged.fml.config.ModConfig$Type COMMON'
```

原因是 FancyModLoader 在 12.0.x 内部重命名了配置类型枚举：`loader 12.0.0`（NeoForge 26.3.0.1-beta ~ 26.3.0.16-beta）为 `COMMON / CLIENT / SERVER / STARTUP`，而 `loader 12.0.8`（NeoForge 26.3.0.42-beta 起）改为 `LOCAL / CLIENT / SYNCED / STARTUP`。语义承接关系为 `COMMON → LOCAL`（两端都加载、不跨端同步，默认后缀由 `-common` 变 `-local`）、`SERVER → SYNCED`（服务端定义、连接时同步给客户端）。工程此前编译目标是 26.3.0.1-beta，`Csrp` 与 `SrpCommands` 的字节码里是 `getstatic ModConfig$Type.COMMON`，因此在只提供 `LOCAL` 的运行时上解析失败。

处理：`gradle.properties` 的 `neo_version` 提到 **26.3.0.52-beta**（与用户实例一致，loader 12.0.8），`Csrp` 的 5 处 `modContainer.registerConfig` 与 `SrpCommands#readConfigurationFile` 的 `ConfigTracker.INSTANCE.loadConfigs` 改用 `ModConfig.Type.LOCAL`，并同步 `scripts/verify-r2-increment.cjs` 的断言。工程给 5 个配置文件都显式传了文件名（`csrp-general.toml`、`csrp-mobs.toml`、`csrp-systems.toml`、`csrp-world.toml`、`csrp-block-conversions.toml`），FML 只对默认文件名套用类型后缀，所以磁盘上既有配置文件的名字与内容不受影响。26.3.0.52-beta 相对 26.3.0.1-beta 的其余 API 变更没有影响本工程：切换后 `compileJava` 零错误。

验证：`build` 通过；`node scripts/run-all-verifications.cjs` **144 / 144**；`runGameTestServer` **187 / 187**；`javap` 复核产物 jar，`alku/csrp/Csrp.class` 与 `alku/csrp/command/SrpCommands.class` 只引用 `ModConfig$Type.LOCAL`，`COMMON` 残留计数为 0。

---

## 4. 因目标版本 API 缺失而改写或省略的部分

| 1.12.2 原件 | 26.3 状况 | 处理方式 |
| --- | --- | --- |
| `MixinEntityRendererBlizzard`、`MixinRenderGlobalBlizzardSky` | `EntityRenderer` / `RenderGlobal` 已重构 | 拆为 26.3 渲染事件：`SubmitCustomGeometryEvent`（雪条几何）、`ViewportEvent.RenderFog`（雾数值）、`ViewportEvent.ComputeFogColor`（地平线洗色） |
| `AccessorShaderGroup`、`StarWorldShaderManager`、`BlackSkyShaderManager` | 26.3 无 `ShaderGroup` / GLSL post 管线，也无 OptiFine | **整体作废**；暗日等效果由工程既有 `CelestialSkyRenderer` 通道承担 |
| `IWorldGenerator` / `GenLayerSRP*` | 26.3 无 GenLayer / IWorldGenerator | 改为 `BiomeSource` 派发 + 数据包 worldgen（`data/csrp/worldgen/**`）；星型来源在生成期解析 |
| Forge Capability（`IBestiaryProgress`、`IItemHandler` 等） | Capability 被数据组件 / 直接接口取代 | 图鉴进度改为玩家持久化数据 + 同步包；容器改为 `Container` / `ItemStackHandler` 语义 |
| `ICommand` / `ICommandSender` / `IGuiHandler` | Brigadier / `MenuProvider` | 命令树用 Brigadier 重写；界面用 `MenuType` + `MenuProvider` 注册 |
| `NBTTagCompound` 手写序列化 | 数据组件 / `Codec` / `ValueInput`-`ValueOutput` | 存档用 `SavedData` + `CompoundTag`；实体用 `addAdditionalSaveData(ValueOutput)` |
| 1.12.2 `Material` / `BlockState` 元数据 | `Material` 已移除；元数据变体改为 BlockState 属性 | 方块行为改用 `BlockState` 属性（如 `decay_age`、`variant`、`type`、`air`）表达 |
| 原版「More World Options...」按钮 | 26.3 创建世界界面改为 `HeaderAndFooterLayout` + 标签页，无该按钮 | 入口按钮改为**按位置定位页脚行**（不引用任何 vanilla 语言键），左对齐避开居中的原版按钮 |
| 原版雨雪抑制 | 需要 `CustomWeatherEffectRenderer` + 逐群系环境属性；26.3 的 `BiomeModifiers` **没有**环境属性修饰器，也无 `ModifyBiomeEvent` | **保留差异**：未做侵入式覆盖 vanilla 群系 JSON；现以 `ComputeFogColor` 洗色缓解。API 路径已记录在 `docs/gap/R2_BLIZZARD_REPORT.md` §5 |
| 死亡界面替换（`GuiOpenEvent` + 子类） | `DeathScreen` 的退出按钮列表为私有 | 改为在 `ScreenEvent.Init.Post` 中给已打开的 `DeathScreen` **追加**同一按钮（位置算法与原件一致：最低按钮下方 6px），保留原版死亡文案 |
| `@GameTest` 注解式测试 API | 26.3 改为数据驱动注册（`RegisterGameTestsEvent`） | 测试改为 `GameTestInstance` 子类注册 |

**判定为死代码、有意不移植**（有证据）：`EntityAIEvadeTP`、`EntityAINexusNest`、`AISoundEaterStalk`、`AIDisableBeaconIki`（原件从未实例化）；7 个 monster 类（Dharma / OroncoAW / VenkrolSV / Mor / Rond / BanoFocused / ShycoFocused）原件既未注册也零引用；`WorldGenParasiteColonyB4` / `BS4` 不可达（仅通过显式 API 暴露，不伪造自动投放）；`GuiSRPConfigEdit` / `GuiSRPConfigView` 及其配置按钮（常量与语言键存在但从未加入界面）。以上均**未凭空造行为**。

### 4.1 原件其他包的处理

| 原件 | 处理 | 说明 |
| --- | --- | --- |
| `api/SRPBestiaryApi`（3 个方法） | ✅ **已移植** | `alku.csrp.api.SrpBestiaryApi`；原件读 `IBestiaryProgress` capability，26.3 无 capability 体系，改为读本模组已持久化的图鉴进度（`CompendiumSavedData.progress(uuid).celestials()`），语义与返回不可变集合的行为一致 |
| `phase/EvoPhase`、`EvoPhases` | ✅ 已覆盖 | 进化阶段定义由本工程的进化系统（`EvolutionSystem` / `EvolutionTierCatalog` / `SrpWorldData.evolution_phase`）承担，含阶段 -2..10 |
| `proxy/CommonProxy`、`ClientProxy` | ✅ 已覆盖 | 1.12.2 的代理模式由 NeoForge 事件订阅（`@EventBusSubscriber` + `Dist.CLIENT`）取代 |
| `util/CommandExtremeSnow` + `world/ExtremeSnowData` / `ExtremeSnowServer` / `network/ExtremeSnowNetwork` / `client/fx/ClientExtremeSnow` | ⚠️ **系统按 parity 移植并接线，命令刻意不注册** | 该命令在原件中**从未被 `registerServerCommand` 注册**，且语言文件无任何相关条目，所以原版 1.10.9 的 `ExtremeSnowData.enabled` 永远是 false、`ExtremeSnowServer` 永远不落雪——接线齐全但不可达。本工程保留同样的休眠状态（存档键 `srp_extreme_snow`，字段与默认值同原件），但把命令留在 dispatcher 之外，并由校验脚本断言 |
| `compatibility/ModCompatibility` | ❌ **不适用** | 它只检测并警告两个 **1.12.2 专属**模组（ReachFix、JustEnoughIDs）的冲突，这两个模组在 26.3 不存在；`warnAddonDeprecatedFunction` 是给 1.12.2 附属模组作者的弃用提示。无对应物可移植 |
| `compatibility/jei/**`（`SRPJEIPlugin`、`InfuserFurnaceJEICategory/Recipe`、`SRPBrewingCategory/Recipe`） | ✅ **已移植**（`compat/jei/SrpJeiPlugin` + `InfuserFurnaceJeiCategory`） | 注入熔炉分类按原件槽位（55,16）(29,34)(115,34)(139,52) 与 200 tick 箭头（79,34）还原，催化剂为注入熔炉方块，并隐藏原件那 16 个成就图标。**两处因 API 差异的改写**：① JEI 26.3 移除了 `getBackground()`，分类改为在 `draw()` 里绘制原件 `infuser_furnace.png` 的 (0,0,176,82) 区域（JEI 的绘制顺序是「JEI 背景 → 分类 draw → 槽位 → 控件 → drawable」，`draw()` 是唯一能画在槽位下方的钩子）；② 酿造一半**无需插件代码**——那 7 条配方是真正的 `minecraft:brewing` 配方，JEI 内置的酿造分类会自行展示（1.12.2 时 JEI 不显示 Forge 酿造配方，才需要自建分类）。JEI 为 `compileOnly`，不装 JEI 时模组照常构建与运行 |

---

## 5. 已知不兼容项

### 5.1 旧存档（最重要）
**1.12.2 的存档无法在 26.3 加载**，因此原模组的存档数据也无法沿用——这不是本移植的取舍，而是 Minecraft 自身跨 14 个大版本的格式差异（区块格式、方块/物品/实体注册表 id、NBT 结构、生物群系与世界生成全部改变）。具体不可迁移的数据：

- 原 `SRPSaveData` 的 `srp*` 键（`srpparasitepoints` / `srpparasiteevolution` / `srpgeneration` / `srpevolutioncodes` / `srpeivhealth` 等）**不会被读取**；
- 原 `SRPWorldData` / `SRPStarWorldData` / `CelestialNightData` / `ExtremeSnowData` 的节点、殖民地、向量、星型、天体、极寒数据同样不可迁移；
- 1.12.2 的方块 **metadata 变体**（66 个兼容 id）在 26.3 由 BlockState 属性表达，旧 metadata 无对应映射；
- 1.12.2 世界中已放置的 SRP 方块、已生成的实体、容器内容物均无法保留。

新存档的 SRP 数据由本工程自己的 `SrpWorldData`（`SavedData`，存档键 `csrp_world_data`，带 `data_version` 字段）持久化，键名为 `evolution_phase` / `evolution_points` / `generation` / `generation_ticks` / `can_gain` / `can_lose` / `cooldown_end` / `srp_difficulty` / `star_type` / `meteor_infection` / `fractured_terrain` / `mushroom_trees` / `nodes` / `colonies` / `vectors` / `dislodgment_codes` / `global_adaptations` / `decorated_chunks` / `assimilation_counts` / `locked_parasites` / `assimilated_endermen` 等。**没有任何静默丢弃**：跨版本无法迁移的数据如上明确列出。

### 5.2 注册 id 命名空间
所有方块 / 物品 / 实体 / 方块实体 / 药水 / 状态效果 / 语言键的命名空间由 `srparasites` 改为 **`csrp`**（`mod_id=csrp`）。因此任何引用原 id 的数据包、脚本、其他模组联动都需要改写（本工程内部的配方、战利品表、结构 NBT、进度、标签已全部改写）。

### 5.3 配置键
原 `SRPConfig*` 使用带空格的 Forge TOML 键名与分节（1709 个 `getInt/getBoolean/...` 调用点），本工程按 26.3 惯例重构为 camelCase 键名 + 5 个文件分节。**旧配置文件不能直接沿用**；行为面已逐项核对补缺（详见 `docs/gap/R2_REPORT.md` §2.3）。

### 5.4 语言键格式
1.12.2 的 `.lang` 格式迁移为 26.3 的 `.json`：`tile.srparasites.X.name` → `block.csrp.X`、`item.srparasites.X.name` → `item.csrp.X`、`entity.srparasites.X.name` → `entity.csrp.X`、`mob_effect.srparasites:X` → `effect.csrp.X`。原键 2332 个全部有确定归宿（未映射 0）；其中 1359 个键在当前实现下无消费者（如 `bestiary.*` 改用工程自带的 `assets/csrp/compendium/lang/*.lang`），保留是为了满足"原键 100% 可追溯"。

### 5.5 其他已知差异（有意保留）
- 世界创建界面的星型选择器提供 **3 种**（Normal / Cold / Warm），原件该界面只列 2 种（`pendingStarType = (pendingStarType + 1) % 2`）；但原件的 worldgen 层存在 Warm（`world/star/GenLayerSRPWarmStar.java`、预览纹理 `earth_warm.png`），故保留 3 种。
- `relay_report` 等文案保留原 `%s` 占位符（1.12.2 格式，26.3 不填充）。
- `noderelay` / `relaycontroller` / `relay_controller_dummy` / `dermoid_cyst` / `parasite_barrier` 的 1.12.2 tile entity 层未复刻：工程既有 relay / cyst 体系语义已覆盖，且原 `TileEntityRelayController`（1043 行）与既有实现耦合到不同方块，重复注册会产生半可用方块。
- **复仇之书抓钩已按原件对齐**（此前数值成簇偏差）：现为原件的三阶段（蓄力 20 tick → 拉近 1.8 速度、接触 2.2 格或 40 tick → 首次冲击 **14.0F / 半径 5 的 AoE** + 沿视线弹射 + 对范围内每个实体施加 100 tick 减益 + 玩家水平速度 ×0.15 → 弹跳 14 tick 三段速度曲线 → 二次冲击 **6.0F** + 半径 3.5 的缩放 AoE（最高 10.0F）+ 落雷）。触发方式也改为原件语义（右键自动取 **32 格视线目标**，冷却 60；潜行右键为击退爆发，冷却 80 存于物品 `srp_kb_next` 标签）。同时补齐原件的 5 个 `VENGEANCE_*` 音效事件——**ogg 文件与 sounds.json 条目一直在仓库里，却从未注册音效事件**。规格与逐行对照见 `docs/gap/VENGEANCE_GRAPPLE_REPORT.md`。
- 两个既有保真缺陷在移植中顺手修正（均有证据）：`ParasiteGenContext.sideCurse` 的 10/11、20/21 两对对角线互换；`canGrowInto` 把 `field_150345_g` 当 podzol 且丢失 SAPLING/VINE/LOG2 映射。
- **进度键写法归一（唯一一处"改了原件写法"）**：原件有 4 个进度文件把展示键写成单数 `advancement.srparasites.<id>.title` / `.desc`（同一模组其余 36 个用的是复数 `advancements.*.description`），属原件的笔误。本工程的数据文件与语言文件统一为复数 + `.description`，因此这 4 个进度在 26.3 下的展示键名与原件的字面写法不同（**显示文本完全一致**，仍取原件同一条语言条目）。若要逐字复刻原件键名，改回单数即可，但会让 4 个进度在原件语言表里找不到对应键（1.10.9 的 `.lang` 里同时存在单数与复数两套写法，故原件本身可正常显示）。

### 5.6 11 个实现辅助实体 id
`crux_block_damage`、`haunter_damage`、`haunter_homing`、`marauder_tendril`、`meteor_satellite`、`parasite_projectile`、`pulling_ball`、`scary_orb`、`shockwave`、`sim_dragonhead`、`warden_waveshock`。

> 曾有第 12 个额外 id `dragon_egg_assimilation`，它承载的是**原模组（1.10.9 与 1.10.8）完全没有的「寄生体攻击时同化附近龙蛋」玩法**，并附带臆造进度 `csrp:again` 与语言键。为遵守「不要添加原模组没有的新玩法」，该实体、渲染器、进度与语言键已全部移除（`scripts/verify-advancements.cjs` 断言其不再出现）。
**不是新玩法**：原件把 13 个投射物分散为独立 id，本工程用单一 `parasite_projectile` + `Mode` 承载并拆分出上述载体/辅助 id。原件 158 个 id 全部仍在，无一缺失（`scripts/verify-entity-id-parity.cjs` 断言）。

---

## 6. 验证证据与复现

```bash
export JAVA_HOME="D:/MC/jdk/graalvm-community-25.3.4.1+1.1"
GRADLE=("$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain)

"${GRADLE[@]}" build -x test             # BUILD SUCCESSFUL → build/libs/csrp-1.10.9.jar
node scripts/run-all-verifications.cjs   # {"total":147,"passed":147,"failed":0}
"${GRADLE[@]}" runGameTestServer         # All 187 required tests passed
```

| 检查 | 结果 |
| --- | --- |
| 编译与打包 | **BUILD SUCCESSFUL**，产物 `build/libs/csrp-1.10.9.jar` |
| 静态契约校验 | **147 / 147 通过**（含实体 id 级对齐、纹理引用全量解析、方块专用类、语言覆盖率、配置项、Escape/失真/注入熔炉/JEI/抓钩/harlequin 命令契约等） |
| 运行期 GameTest | **187 / 187 通过**（169 个实体生成冒烟 + 1 个 NBT 往返 + 2 个 Escape 规则 + 1 个酿造配方 + 3 个注入熔炉 + 2 个失真开关 + 2 个复仇抓钩 + 4 个 harlequin + 2 个寄生群系雾 + 1 个掉落表） |
| 服务器启动 | GameTest 服务器完整启动至测试结束，无启动崩溃 |
| 世界生成运行期 | 225 区块实证：四类寄生群系 + 36 种寄生体方块落地，零 decoration 失败 |
| 实体 id 对齐 | 原件 158 个 id **全部命中**（缺失 0） |
| 语言覆盖 | `en_us` 2332/2332、`zh_cn` 2140/2140 |

### 尚未验证的部分（如实说明）
- **未做 `runClient` 图形化目视验证**（本环境无图形界面）：暴风雪观感、世界预览动画、Kirin 光束、Escape 死亡界面按钮的最终呈现需要真机确认。这些均有静态契约与运行期逻辑测试覆盖，但"看起来对不对"未验证。
- 原版雨雪在暴风雪期间仍会渲染（见 §4 对应行）。
- 1.12.2 存档迁移（见 §5.1，属不可迁移，非未验证）。

---

## 7. 复现构建

```bash
git clone https://github.com/YMRwithNoworry/csrp.git
cd csrp && git checkout port/neoforge-26.3
export JAVA_HOME="D:/MC/jdk/graalvm-community-25.3.4.1+1.1"
"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar \
    org.gradle.wrapper.GradleWrapperMain build
```

在 Windows 的 PowerShell / cmd 里也可以用 `.\gradlew.bat build`；在 Niubash（`niu -c`）里 `gradlew.bat` 无法执行（进程挂起且不启动 Gradle 守护进程），必须用上面的 wrapper jar 调用方式，或用等价的 `scripts/build-locked.sh build`。

需要 JDK 25 与可访问 NeoForged Maven 的网络（首次构建会下载 NeoForge 26.3.0.52-beta 与 NeoForm 产物）。
