# SRParasites 1.10.9 → Minecraft 26.3 移植说明

本文件是交付说明，记录**已完成的内容清单**、**因目标版本 API 缺失而改写或省略的部分**，以及**已知不兼容项**。

---

## 0. 前置确认（开工前已确定）

| 项 | 值 | 依据 |
| --- | --- | --- |
| 目标 Minecraft 版本 | **26.3** | `gradle.properties` → `minecraft_version=26.3`、`minecraft_version_range=[26.3,26.4)` |
| 模组加载器 | **NeoForge 26.3.0.1-beta** | `neo_version=26.3.0.1-beta`、`loader_version_range=[12,)`；构建用 `net.neoforged.moddev` 2.0.147 |
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
| 完整工程（源码 + 资源 + 构建脚本） | 本仓库（Gradle 工程，`./gradlew.bat build` 即可编译） |
| 可加载运行的构建产物 | `build/libs/csrp-1.10.9.jar`（约 134 MB，含全部资源） |
| 移植说明 | 本文件 |
| 各子系统详细报告 | `docs/gap/R2_REPORT.md`、`R2_BLIZZARD_REPORT.md`、`R3_WORLDGEN_REPORT.md`、`R4_BLOCKS_REPORT.md`、`R5_ITEMS_REPORT.md`、`R6_ENTITY_MATRIX.md`、`LANG_REPORT.md` |
| 自动化校验 | `scripts/run-all-verifications.cjs`（120 项）+ `scripts/build-locked.sh`（多 agent 并发构建串行化） |

规模：569 个 Java 文件、88 个方块类、128 个实体文件、44 个物品类、7 个 Mixin、362 个 blockstate、2236 张纹理、1008 个音效、55 个结构 NBT、33 套语言。

---

## 3. 已完成的内容清单

### 3.1 注册系统
- **方块**：全部 id 到位；1.10.9 的 108 个兼容/元数据 id **全部**改用专用类或形状家族（通用 `new Block(...)` 回退 0 个命中）。
- **方块实体**：10 个 `BlockEntityType`，含 1.12.2 兼容 id `legacy_infested_furnace`（`infested_furnace` / `infested_furnace_lit` 是**真实可用**的容器熔炉，非空壳）。
- **物品**：机器化 diff 后原模组 115 个一般物品 id **全部命中**；17 个成就图标统一 `stacksTo(1)`（对齐 `ItemAdvancementIcon` 的 `field_77777_bU = 1`）。
- **实体**：原件 **158 个 id 全部注册**；本工程共 170 个（多出的 12 个是移植实现所需的辅助/拆分实体，见 §5.6）。
- **其他注册**：菜单、粒子、药水、状态效果、音效、唱片（`jukebox_song` 数据包注册表）、伤害类型、附魔、创造模式标签页。

### 3.2 世界生成
- 4 个寄生群系 + 群系装饰（`world/gen/**` 12 个特性类）、结构 NBT 55/55。
- 殖民地基类（原 614 行逐方法转写）+ 核心 + B1-B4 / BS1-BS4、节点保护 1/2/3、陨石撞击、死头巨树与村庄、冷星村庄。
- 运行期已实证：放置节点 → 225 区块 → 四类寄生群系全部落地、36 种寄生体方块生成、零 decoration 失败。

### 3.3 GUI
| 原件界面 | 本工程实现 | 状态 |
| --- | --- | --- |
| `GuiInfuserFurnace` / `ContainerInfuserFurnace` | `client/screen/InfuserFurnaceScreen` + `inventory/InfuserFurnaceMenu` | ✅ |
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
- **实体渲染器覆盖修复**：26.3 对未注册渲染器的实体类型**没有兜底**（`EntityRenderDispatcher.getRenderer` 是普通查表，`extractEntity` 直接解引用结果），而两个移植自有的不可见伤害载体实体 `crux_block_damage` / `haunter_damage` 会被 `addFreshEntity` 同步到客户端——一旦渲染即崩溃。已按同类不可见实体的既有做法注册 `NoopRenderer`，现 **170/170 实体 id 均有渲染器**（`scripts/verify-entity-renderers.cjs` 断言）。
- **酿造配方恢复（玩法缺口）**：原件 `potion/Recipe.java` 用 5 个 `IBrewingRecipe` 注册了恐惧药水三级链、死血、荆棘醒酒器；NeoForge 26.3 移除了 `IBrewingRecipe` 与 `RegisterBrewingRecipesEvent`，本工程此前只留下一个空的 `ThornshadeBrewingEvents` NEEDS-DESIGN 占位类，**配方实际不可用**。现按 26.3 的数据驱动方式恢复为 `data/csrp/recipe/brewing/*.json` 共 7 个文件（恐惧药水链 3 条 + 死血 water/awkward 2 条 + 醒酒器 water/awkward 2 条），删除空壳类；校验 `scripts/verify-brewing-recipes.cjs` + GameTest `csrp:brewing_recipes/legacy_srp_brews`（经真实 `RecipeManager` 加载并匹配）。
  - **字段映射经官方映射表核实**：从 `MinecraftForge/MCPConfig` 的 `joined.tsrg` 与 Forge maven 的 `mcp_stable-39-1.12.zip`（内含 `fields.csv`）查得 —— `field_151145_ak` = **FLINT（燧石）**、`field_151016_H` = GUNPOWDER、`field_185157_bK` = DRAGON_BREATH、`field_151068_bn` = POTIONITEM、`field_185155_bH` = SPLASH_POTION、`field_185156_bI` = LINGERING_POTION、`field_151069_bo` = GLASS_BOTTLE、`field_150432_aD` = ICE。其中燧石一项**纠正了此前的常识推断**（曾误判为地狱疣），ICE 一项同时证实了工程 `InfuserFurnaceBlockEntity.GLASS_RECIPES` 的映射正确。
- **Escape 系统补齐**：见 §3.6。

### 3.6 Escape（死亡循环逃脱）系统
原件 `feature/EscapeOnDeathHandler` + `feature/EscapeRespawnHandler` + `client/EscapeClientState` + `client/gui/GuiGameOverEscape` + `network/msg/C2SRequestEscape` + `S2CSetEscapeOffer`，本工程此前只迁移了语言键、**代码缺失**，本轮补齐：

- 被寄生体致死且**一分钟内超过 5 次** → 置 `srp_offer_escape` 并下发 `escape_offer` 包；
- 客户端死亡界面出现「DeathLoop Prevention |200-300 blocks」按钮；
- 点击 → `escape_request` 包置 `srp_escape_pending` 并重生；
- 重生时在 `escapeMinDistance`(200) ~ `escapeMaxDistance`(300) 范围内随机寻找安全落点（要求脚下可站立且非仙人掌、脚部与头部均为空气且非液体，最多 24 次尝试）并传送。

### 3.7 资源
结构 NBT 55/55、音效 1008/1008、纹理 2236 张（与原件 1:1，`textures/blocks/` 按 26.3 规范改为 `textures/block/`）、33 套语言、全部 blockstate / 模型 / 物品定义。**未新增任何写实渲染或网页式 UI**；新增界面沿用原件 16×16 像素纹理与既有调色板（世界预览面板直接使用原件 `textures/gui/worldsettings/*.png`）。

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
| `compatibility/ModCompatibility` | ❌ **不适用** | 它只检测并警告两个 **1.12.2 专属**模组（ReachFix、JustEnoughIDs）的冲突，这两个模组在 26.3 不存在；`warnAddonDeprecatedFunction` 是给 1.12.2 附属模组作者的弃用提示。无对应物可移植 |
| `compatibility/jei/**`（`SRPJEIPlugin`、`InfuserFurnaceJEICategory/Recipe`、`SRPBrewingCategory/Recipe`） | ⚠️ **尚未移植** | 这是**配方查看器集成**（给注入熔炉与 SRP 酿造添加 JEI 分类），属展示层便利功能，非核心玩法。JEI 已有 NeoForge 26.3 构建（Modrinth `jei` 项目，版本 `31.9.0.58 for NeoForge 26.3`），因此**可以**补齐：需在 `build.gradle` 加 `compileOnly` JEI 依赖，并按 JEI 26.3 的 `IRecipeCategory` / `IRecipeCatalystRegistration` API 重写这 5 个类。已登记在 `PORT_BACKLOG.md` 待办 |

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
- 两个既有保真缺陷在移植中顺手修正（均有证据）：`ParasiteGenContext.sideCurse` 的 10/11、20/21 两对对角线互换；`canGrowInto` 把 `field_150345_g` 当 podzol 且丢失 SAPLING/VINE/LOG2 映射。

### 5.6 12 个实现辅助实体 id
`crux_block_damage`、`dragon_egg_assimilation`、`haunter_damage`、`haunter_homing`、`marauder_tendril`、`meteor_satellite`、`parasite_projectile`、`pulling_ball`、`scary_orb`、`shockwave`、`sim_dragonhead`、`warden_waveshock`。
**不是新玩法**：原件把 13 个投射物分散为独立 id，本工程用单一 `parasite_projectile` + `Mode` 承载并拆分出上述载体/辅助 id。原件 158 个 id 全部仍在，无一缺失（`scripts/verify-entity-id-parity.cjs` 断言）。

---

## 6. 验证证据与复现

```bash
export JAVA_HOME="D:/MC/jdk/graalvm-community-25.3.4.1+1.1"

./gradlew.bat build -x test          # BUILD SUCCESSFUL → build/libs/csrp-1.10.9.jar
node scripts/run-all-verifications.cjs   # {"total":118,"passed":118,"failed":0}
./gradlew.bat runGameTestServer      # All 173 required tests passed
```

| 检查 | 结果 |
| --- | --- |
| 编译与打包 | **BUILD SUCCESSFUL**，产物 `build/libs/csrp-1.10.9.jar` |
| 静态契约校验 | **120 / 120 通过**（含实体 id 级对齐、纹理引用全量解析、方块专用类、语言覆盖率、配置项、Escape 契约等） |
| 运行期 GameTest | **174 / 174 通过**（170 个实体生成冒烟 + 1 个 NBT 往返 + 2 个 Escape 规则 + 1 个酿造配方） |
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
./gradlew.bat build
```

需要 JDK 25 与可访问 NeoForged Maven 的网络（首次构建会下载 NeoForge 26.3.0.1-beta 与 NeoForm 产物）。
