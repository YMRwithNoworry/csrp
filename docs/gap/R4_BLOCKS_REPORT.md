# R4 — 方块保真化报告（108 个 legacy id）

任务：task-1 / R4「108 个占位方块的专用实现」。
分支 `port/neoforge-26.3`，目标 Minecraft 26.3 / NeoForge 26.3.0.1-beta / Java 25。
事实来源（只读）：`D:\code\MC模组\_scratch\vf\out109\com\dhanantry\scapeandrunparasites\`（SRParasites 1.10.9 反编译产物）。

## 0. 结论摘要

| 项目 | 结果 |
| --- | --- |
| legacy id 总数 | 108 |
| 仍走 `new Block(legacyProperties(key))` 通用回退的 id | **0** |
| 新增专用方块类 | 36 个（`src/main/java/alku/csrp/block/**`） |
| 新增断言脚本 | `scripts/verify-block-dedicated-classes.cjs`、`scripts/verify-block-r2-backlog.cjs` |
| `gradlew build -x test` | BUILD SUCCESSFUL |
| `node scripts/run-all-verifications.cjs` | 108 / 107 通过；唯一失败项 `verify-blizzard-star-terrain.cjs` 属其他 teammate 范围（我方两个脚本全过） |

第一个提交 `27c489e9` 因共享索引误带了其他 teammate 已暂存的文件（99 个，内容未丢失，已向 Lead 报备）；此后改为 `git commit -- <路径>` 限定提交（`da24bcb3`、`6ba58e8b`、`047c266f`）。

## 1. 移植方式总览

1.12.2 的元数据（metadata int）在 26.3 映射为 BlockState 属性；原类里的 `getActualState` 映射为 `updateShape`，`onBlockActivated` 映射为 `useItemOn`，`onEntityWalk`/`entityInside` 映射为 `stepOn`/`entityInside`，`getLightValue` 映射为 `Properties#lightLevel`，`field_149765_K`（滑度）映射为 `Properties#friction`。

原类 → 26.3 类对应：

| 原类（out109） | 26.3 实现 |
| --- | --- |
| `BlockGore` | `GoreBlock` |
| `BlockBloodyIce` | `BloodyIceBlock`（extends `ParasiteSpreadingBlock`） |
| `BlockParasiteSpreading` | `ParasiteSpreadingBlock` |
| `BlockSRPFlower` | `AssimilatedBlossomBlock` |
| `BlockInfestedBush` | `InfestedBushBlock`（extends `SrpBushBlock`） |
| `BlockParasiteBush` | `ParasiteBushBlock`（extends `SrpBushBlock`） |
| `BlockParasiteStain` | `ParasiteStainBlock` |
| `BlockParasiteRubble` | `ParasiteRubbleBlock` |
| `BlockParasitePlank` | `ParasitePlankBlock` |
| `BlockParasiteCanister` | `ParasiteCanisterBlock` |
| `BlockInfestedOre` | `InfestedOreBlock` |
| `BlockParasiteSapling` | `ParasiteSaplingBlock` |
| `BlockLeafLike` | `InfestedLeavesBlock` |
| `BlockParasiteCactus` | `InfestedCactusBlock` |
| `BlockHirsuteHair` | `HirsuteHairBlock` |
| `BlockTressesHair` | `TressesHairBlock` |
| `BlockLipomaMass` | `LipomaMassBlock` |
| `BlockVineBase` | `ParasiteTendrilBlock` |
| `BlockInfestedRemain` | `InfestedRemainBlock` |
| `BlockParasiteFog` | `ParasiteFogBlock` |
| `BlockParasiteBarrier` | `ParasiteBarrierBlock` |
| `BlockDermoidCyst` | `DermoidCystBlock` |
| `BlockDod` | `DispatcherNBlock` |
| `BlockEpitomeInfestationWarpDiffuser` | `EpitomeDiffuserBlock` |
| `BlockColonyCore`（colonyoutpost） | `ColonyOutpostBlock` |
| `BlockInfestedFurnace` | `InfestedFurnaceBlock` |
| `BlockRelayController` / `BlockRelay` / `BlockNodeRelay` | `LegacyRelayBlock`(+/`.Dummy`/`.Node`) |
| `BlockSlabBase` / `BlockSlabRubble` / `BlockSlabStain` / `BlockHarleskinnSlab` | `LegacySlabBlock` / `LegacyVariantSlabBlock` |
| `BlockStairBase` / `BlockHarleskinnStairs` | `LegacyStairBlock` |
| `BlockWallBase` | `LegacyWallBlock` |
| `BlockHarleskinnFence` | `LegacyFenceBlock` |
| `BlockPottedSRPFlower` | `PottedSrpBlock` |
| `BlockParasiteTrunk` | `ParasiteTrunkBlock` |

## 2. 逐 id 对照（108/108）

证据列给出「原类文件:行」→「26.3 文件:关键行」。缩写：`MB` = `src/main/java/alku/csrp/registry/ModBlocks.java`，`B/` = `src/main/java/alku/csrp/block/`。

### 2.1 有专用行为的 id（42）

| id | 原类（out109） | 26.3 实现 | 移植方式 / 证据 |
| --- | --- | --- | --- |
| `assimilated_blossom` | `block/BlockSRPFlower.java:17-27` | `B/AssimilatedBlossomBlock.java:25-38` | `BlockBush` + `mayPlaceOn` = GRASS/GROUND/SAND/CLAY 材质 → `BlockTags.DIRT`/`SAND`/`GRAVEL`/`CLAY`/`FARMLAND`。注册 `MB:1085`。 |
| `bloodyice` | `block/BlockBloodyIce.java:23-94` | `B/BloodyIceBlock.java:44-88` | infested=true 的扩散块；滑度 0.98 → `Properties#friction`（`MB:1034`）；`GLASS` 音效；无掉落（`BlockBloodyIce.java:40`）；`onFallenUpon` → `fallOn` 重写（`B/BloodyIceBlock.java:61-70`），配置默认值 `5.0/3/true`（`util/config/SRPConfigWorld.java:26,32,35`）。 |
| `colonyoutpost` | `init/SRPBlocks.java:486`（`BlockColonyCore`，30.0F / 1200.0F） | `B/ColonyOutpostBlock.java:31-63` | active 0..3 + `ColonyStructureGenerator.generateBuilding` 调度；与已移植 `ColonyStructureBlock` 同构（该类为 `final`，故复制其 tick 契约）。注册 `MB:1245`。 |
| `dispatchern` | `block/BlockDod.java:7-72` | `B/DispatcherNBlock.java:41-100` | AABB 1..15（`:7,47`）；反向推击 0.3/+0.05 与 20 刻冷却后的 1.2/+0.25 重击 + 4.0F 伤害 + 20 刻 DOD 烟雾效果（`:51-73`）；NBT 键 `srp_dod_last_hit`（`:9`）。 |
| `goreada` | `block/BlockGore.java` | `B/GoreBlock.java:44-155` | 6 个 id 共用；variant flat/small/big（默认 SMALL，`:46`）；`TALL_GRASS_AABB`（`:38`）；衰变 1/10、BIG 1/45（`:99-104`）；`entityInside` 施加 COTH（`:80-95`）；无掉落（`:119`）。注册 `MB:1080-1082`。 |
| `gorefer` | 同上 | 同上 | 同上。 |
| `goremar` | 同上 | 同上 | 同上。 |
| `gorepri` | 同上 | 同上 | 同上。 |
| `gorepur` | 同上 | 同上 | 同上。 |
| `goresim` | 同上 | 同上 | 同上。 |
| `harlequinn_grass` | `block/BlockParasiteSpreading.java:36-40` | `B/HarlequinnGrassBlock.java:27-42` | infested=false 的扩散块；上方积雪 → 变为 `locs_block`（`modBlocks.LOCS_BLOCK`）。注册 `MB:1113`。 |
| `hirsute_hair` | `block/BlockHirsuteHair.java:18-57` | `B/HirsuteHairBlock.java:22-38` | `BushBlock` + 支撑必须为 `srparasites` 命名空间（`:46-57`）。 |
| `infested_cactus` | `block/BlockParasiteCactus.java:7-119` | `B/InfestedCactusBlock.java:42-118` | 沙/红沙/`infestedsand`/自身支撑（`:76-89`）；8 刻推击冷却 + 0.35/0.08（`:9-11`）；寄生虫免伤（`:70-72`）；`hasImpulse`→`Entity#syncVelocity`（26.3 字段，见 `Entity.java:284`）。 |
| `infested_leaves` | `block/BlockLeafLike.java:6-105` | `B/InfestedLeavesBlock.java:54-131` | `decay_age` 0..5；邻接 SRP 块时 1/6 恢复、否则 1/12 衰变，满级销毁（`:45-64`）；掉落仅假苹果，概率 `1/(40-8*fortune)`（`:7,88-94`）。 |
| `infested_leaves_fast` | 同上 | 同上（`fast=true`） | 1.10.9 两个 id 都是 `new BlockLeafLike(name)`（`init/SRPBlocks.java:629-630`），类无差异；`fast` 仅记录注册 id。 |
| `infestedbush` | `block/BlockInfestedBush.java:5-33` | `B/InfestedBushBlock.java:31-109` + `B/SrpBushBlock.java` | variant infected/grass1/flower1/spine/vine/arc（默认 INFECTED，`:29-33`）；END/NODE；spine/vine 用 `REED_AABB` 且可叠（`:31,315-317`）；支撑须 SRP 且排除 bloodyice/ashen_glass（`:53-69`）；无掉落（`:257`）；剪刀可采（`:379-388`）。 |
| `infestedore` | `block/BlockInfestedOre.java:12-25,76-79` | `B/InfestedOreBlock.java:20-64` | variant co/dia/eme/gol/iro/lap/red/un（默认 CO，`:25`）；原 `updateTick`/`onEntityWalk` 为空实现 → 无扩散；`blockinfest.*` 音效。 |
| `infestedremain` | `block/BlockInfestedRemain.java:5-64,92+` | `B/InfestedRemainBlock.java:43-158` | `source` 0..1 + `infested_base`；AABB 高 0.125（`:5`）；滑度 0.52（`:23`）；非潜行实体水平速度 ×0.84（`:45`）；仅铲子掉落（`:56`）；无支撑时清除 + 1/2 扩散。 |
| `lipoma_mass` | `block/BlockLipomaMass.java:18-62` | `B/LipomaMassBlock.java:21-52` | 唯一「向下悬挂」的植物：上方须为 SRP 块且下表面实心（`:37`）；失去支撑 `destroyBlock(pos, true)`（`:40-44`）。 |
| `noderelay` | `block/BlockNodeRelay.java` + `tileentity/TileEntityNodeRelay.java` | `B/LegacyRelayBlock.Node`（`B/LegacyRelayBlock.java:71-75`） | 无 BlockState；见 §4 遗留项（tile entity 注册）。 |
| `parasite_barrier` | `block/BlockParasiteBarrier.java:18-96` | `B/ParasiteBarrierBlock.java:22-40` | 硬度 -1、抗爆 6,000,000（`:20`）；无碰撞箱（`:76`）；不可被实体/爆炸破坏（`:90-96`）。 |
| `parasitebush` | `block/BlockParasiteBush.java:8-11,49-113,300-320` | `B/ParasiteBushBlock.java:40-148` + `B/SrpBushBlock.java` | variant 取资源包超集（见 §3.1）；tendril/bine 可挂天花板、可叠（`:51-53,130-141`）；END/NODE 由空气邻居推导（`:300-320`）；失去支撑 `destroyBlock(pos,true)`（`:217`）。 |
| `parasitecanister` | `block/BlockParasiteCanister.java:10-58` | `B/ParasiteCanisterBlock.java:33-118` | variant sac/cyst/lump/bag（默认 SAC，`:25`）；仅 CYST 用 0.55 高箱体（`:10,19`）；CYST 下方为空气时消失（`:36-43`）；SAC 5% 掉 `itemlurecomponent6`（`:44-58`）。 |
| `parasitefog` | `block/BlockParasiteFog.java:9-146` | `B/ParasiteFogBlock.java:29-108` | `air` 0..2（`:9`，资源包正是 `air=0/1/2`）；0→1 推进、2 时把 5³ 内雾气全设为 2 后自毁（`:78-124`）；玻璃瓶交互换 `FOG_BOTTLE`（`:125-146`）。 |
| `parasiteplank` | `block/BlockParasitePlank.java:12-25,71-84` | `B/ParasitePlankBlock.java:17-55` | variant deadhead/deadheads（默认 DEADHEAD，`:25`）；`SoundType.WOOD`、斧类工具；`randomDisplayTick` 粒子属客户端渲染层。 |
| `parasiterubble` | `block/BlockParasiteRubble.java:18-117` | `B/ParasiteRubbleBlock.java:26-115` | 15 个 variant（默认 BONE，`:23`）；`getActualState` 的积雪换装（weathb/weathbc/weathfs ↔ *S，`:74-117`）→ `updateShape`；infested=false 扩散。 |
| `parasitesapling` | `block/BlockParasiteSapling.java:9-30` | `B/ParasiteSaplingBlock.java:36-128` | `stage` 0..1 + variant tree/treethin/flowertall/consumed/deadhead/infested（默认 TREE，`:17`）；光照 ≥9 且 1/7 概率生长（`:26-27`）；stage0 只推进、stage1 交给世界生成器（`:32-39`，结构放置归 world-gen teammate）。 |
| `parasitestain` | `block/BlockParasiteStain.java:12-40` | `B/ParasiteStainBlock.java:25-81` | variant dirt/mud/flesh/feeler/spore/red/sackflesh（默认 DIRT，`:25`）；每变体音效（DIRT→GRAVEL、MUD→MUD、SPORE→GRASS，其余 FLESH，`:28-40`）；infested=false 扩散。 |
| `parasitetendril` | `block/BlockVineBase.java:17-23` | `B/ParasiteTendrilBlock.java:17-20` | `BlockVineBase` 只是 `BlockVine` + 注册名/硬度 0.5/随机刻，26.3 `VineBlock` 已具备 `north/east/south/west/up`。 |
| `potted_assimilated_blossom` | `block/BlockPottedSRPFlower.java:10-55` | `B/PottedSrpBlock.java:18-33` | 箱体 AABB(0.3125,0,0.3125,0.6875,0.375,0.6875)（`:14`）；硬度 0、`SoundType.STONE`；只掉自己（`:44`）。 |
| `potted_consumed_assimilated_blossom` | 同上 | 同上 | 同 id 家族（`init/SRPBlocks.java:514-517`）。 |
| `relay_controller_dummy` | `block/BlockRelay.java` | `B/LegacyRelayBlock.Dummy`（`B/LegacyRelayBlock.java:66-71`） | 无 BlockState；见 §4。 |
| `relaycontroller` | `block/BlockRelayController.java` + `tileentity/TileEntityRelayController.java` | `B/LegacyRelayBlock`（`B/LegacyRelayBlock.java:31-64`） | facing（资源包仅 4 个朝向）+ lit；见 §4。 |
| `tresses_hair` | `block/BlockTressesHair.java:17-51` | `B/TressesHairBlock.java:16-26` | `BlockDoublePlant`（`half=lower/upper`）；支撑须 SRP 命名空间（`:40-51`）。 |
| `parasitestain_flesh_wall` 等 6 个 `*_wall` | `block/BlockWallBase.java:23-51` | `B/LegacyWallBlock.java:27-66` | `BlockWall` + tickRandom；邻接侵染块时 10 刻复查 + `beckonInfestation`（→ `BlockInfestation.infestAround(...,1)`）与 20 刻续调（`:44-51`）。 |
| `bruisewood_fence`, `harleskinn_fence` | `block/BlockHarleskinnFence.java:16-55` | `B/LegacyFenceBlock.java:24-59` | 同上的墙/栅栏族侵染 tick；硬度 2.0 + FLESH 音效。 |
| `consumed_pot`, `infested_pot` | `block/BlockPottedSRPFlower.java` | `B/PottedSrpBlock.java` | 见上。 |

### 2.2 1.12.2 元数据变体 id（66）

这些 id 在原模组里由同一族类的 `getMetaFromState`/`getStateFromMeta` + 独立注册项产生，26.3 语义如下：

| id 组 | 数量 | 原类 | 26.3 实现 | 变体语义 |
| --- | --- | --- | --- | --- |
| `*_slab` / `*_slab_double` / `*slabhalf` / `*slabdouble` | 44 | `slabs/BlockSlabBase.java`、`slabs/BlockSlabRubble.java`、`slabs/BlockSlabStain.java`、`BlockHarleskinnSlab.java:17-19` | `B/LegacySlabBlock.java`、`B/LegacyVariantSlabBlock.java`（`MB:1297-1310`） | 1.12.2 用「HALF 元数据 + isDouble 子类」两套类表达 half/double；26.3 统一为 `SlabBlock` 的 `type=bottom/top/double`，**双台阶即 `type=double` 的完整方块**，与全部 `*_slab*.json` 一致。`parasiterubbleslab*`/`parasitestainslab*` 额外保留 `variant` 属性（`BlockSlabRubble.java:2`、`BlockSlabStain.java:2`）。 |
| `*_stairs` / `*stairs` | 16 | `block/BlockStairBase.java:16-22`、`block/BlockHarleskinnStairs.java` | `B/LegacyStairBlock.java`（`MB:1313-1319`） | `BlockStairs` 的 `facing/half/shape`；原类的 `setToolStats` 在 26.3 由标签工具决定，故省略。 |
| 6 个 `*_wall` | 6 | `block/BlockWallBase.java` | `B/LegacyWallBlock.java` | 见 §2.1。 |
| 2 个 fence | 2 | `block/BlockHarleskinnFence.java` | `B/LegacyFenceBlock.java` | 见 §2.1。 |
| `consumed_pot`、`infested_pot` | 2 | `block/BlockPottedSRPFlower.java` | `B/PottedSrpBlock.java` | 见 §2.1。 |
| `infested_furnace_lit` | 1 | `block/BlockInfestedFurnace.java:29-54` | `B/InfestedFurnaceBlock.java`（同一类注册两次，`MB:1235-1243`） | `lit=true` 时 `lightLevel = 14`（`:53`），`facing` 由放置者决定（`:122-131`）。 |

## 3. 与原模组的差异（有意为之 / 已核实）

### 3.1 `parasitebush` 的 thorn/frost 变体不是 1.10.9 行为（backlog 误报）

- 事实：`out109/block/BlockParasiteBush.java:555-560` 的 `EnumType` **只有** `TENDRIL, BINE, POP, EYE, TOOH`；全仓 `grep -rn "THORN" out109` 命中的是 `BlockThornshade`、`util/handlers/ThornshadeThornsHandler.java`、`SRPPotions.THORNSHADE_THORNS_E`，与 `parasitebush` 无关。
- 而本仓 `assets/csrp/blockstates/parasitebush.json` 含 `thorn/thorndead/thorndormat/thorntwo/thorntwos/frostg/frostgt/decanter/decanterempty` —— 这些来自更新版本的资源包，不是 1.10.9 的行为。
- 处理：`ParasiteBushBlock` 的 variant 保留资源包超集（否则资源与 `ParasiteGenContext` 的按名查表会失效），并对 1.10.9 实际定义的 5 个常量实现元数据行为；超集常量仅作装饰变体。
- thorn（荆棘反伤）行为在本工程由 `B/ThornshadeBlock.java` + `event/ThornshadeThornsEvents.java` 实现，未受影响。

### 3.2 配置项缺位

`config/**` 不在 task-1 写入范围内，以下原模组配置项改用 1.12.2 默认值：

| 原配置 | 默认值 | 使用处 |
| --- | --- | --- |
| `bloodyIceBreakOnHardLanding` / `bloodyIceBreakFallDistance` / `bloodyIceBreakDiameter` | `true` / `5.0` / `3`（`util/config/SRPConfigWorld.java:26,32,35`） | `B/BloodyIceBlock.java:47-52` |
| `bushClimbingEnabled` | `true` | `B/ParasiteBushBlock.java:75-78`（`isClimbable`） |
| `rsBlockParticleS` | `0.05`（`SRPConfigSystems.java:44`） | 客户端粒子，属渲染层 |

### 3.3 已由其他 teammate 覆盖 / 归其范围

- 树木、大型花、陨石等结构放置：`BlockParasiteSapling.generateTree` 的实际结构生成在 `alku.csrp.world.gen.*`（world-gen teammate）。
- `InfestedLeavesBlock` 的假苹果掉落沿用 `ModItems.FALSE_APPLE`；`ParasiteCanisterBlock` 的 `itemlurecomponent6` 沿用 `ModItems.LURECOMPONENT6`。

## 4. 遗留问题（需 Lead 决策 / 后续批次）

1. **`registry/ModBlockEntities.java` 不在 task-1 写入范围**，因此以下 4 个 id 只完成了方块层保真（BlockState/属性/光照/掉落），其 tile entity 行为尚未接回：
   - `dermoid_cyst`：`TileEntityDermoidCyst` 容器（`BlockDermoidCyst.java:34-42`）。
   - `infested_furnace` / `infested_furnace_lit`：`TileEntityInfestedFurnace` 三格熔炼 + `ContainerFurnace` GUI + 分面物品槽 + `setLitState` 换状态保物品（`BlockInfestedFurnace.java:35-60`）。本工程已有 `InfuserFurnaceBlockEntity` 可直接复用，仅缺一条 `BLOCK_ENTITIES.register(...)` + `Set.of(...)`。
   - `noderelay`、`relaycontroller`、`relay_controller_dummy`：`TileEntityNodeRelay` / `TileEntityRelayController`（`TileEntityRelayController.java` 1043 行）。本工程已有 `RelayTerminalBlockEntity` 与 `NodeLampBlock` 承载同类逻辑。
   - `parasite_barrier`：`TileEntityParasiteBarrier` 的可配置区块半径（`BlockParasiteBarrier.java:38-70`）。
   建议：授权在 `registry/ModBlockEntities.java` 增加对应条目（每项 2-3 行），随后把上述 6 个 id 的 `EntityBlock` 接上已有 BE 实现。
2. `BlockParasiteBush.isLadder`（`BlockParasiteBush.java:262-276`）依赖 NeoForge 的攀爬钩子；本批以 `ParasiteBushBlock.isClimbable(BlockState)` 暴露判定，尚未绑定到实体攀爬事件。
3. `LegacySlabBlock`/`LegacyWallBlock`/`LegacyFenceBlock` 的侵染 tick 使用 `BlockInfestation.infestAround(...,1)` 对应原 `beckonInfestation`；原实现还带 `SRPConfigSystems` 的进化点结算，配置项缺位故未接。

## 5. R2 backlog 处理结果

| R2 项 | 结论 | 证据 |
| --- | --- | --- |
| 1) `BlockDeadheadLeaves` 距离 7 衰减 | **已实现**。原类 `BlockDeadheadLeaves extends Block`，自建 7 格 BFS（`decayDistance = 7`，`BlockDeadheadLeaves.java:28-60`），种子为响应 `canSustainLeaves` 的方块。26.3 用 `LeavesBlock.DISTANCE` 表达同一语义：`B/DeadheadLeavesBlock.java:31-84` 在 `randomTick` 前重算 7 格内到 SRP 树干/原木的距离，再走原生衰减判定。 | `B/DeadheadLeavesBlock.java:24-84`；断言 `scripts/verify-block-r2-backlog.cjs` |
| 2) `BlockParasiteTrunk.canSustainLeaves` | **已实现（等价形式）**。原类 `canSustainLeaves` 恒为 `true`（`BlockParasiteTrunk.java:123-125`）。26.3 已移除 `Block#canSustainLeaves`（改由 `minecraft:logs` 标签驱动），故新增 `B/ParasiteTrunkBlock.java:26-45`：`sustainsDeadheadLeaves(BlockState)` = 自身或 `BlockTags.LOGS`，并由树叶在衰减扫描中识别；`parasitetrunk` 已改注册为 `ParasiteTrunkBlock`（`MB:192`）。 | `B/ParasiteTrunkBlock.java:26-45` |
| 3) `BlockParasiteBush.THORN` | **backlog 误报**，见 §3.1。未凭名字造行为。 | `out109/block/BlockParasiteBush.java:555-560`（EnumType 取值）；本仓 `B/ThornshadeBlock.java`、`event/ThornshadeThornsEvents.java` |
| 4) `luredValueNine` / `luredValueTen` | **已具备且数值一致**：原配置 `luredValueNine = 1200`，`luredValueNineCool = 50000000`（`SRPConfigSystems.java:754-755`）；`luredValueTen = 1200`，`luredValueTenCool = 72000000`（`:817-818`）。本工程 `EvolutionLureBlock.Tier` 的 `NINE("nine", 1_200, 50_000_000, 7)`、`TEN("ten", 1_200, 72_000_000, 8)` 完全对应。 | `B/EvolutionLureBlock.java:176-177` |
| 5) `infested_furnace` / `infested_furnace_lit` 熔炉语义 | **方块层完成**（facing + lit + lit 时光照 14 + `setLitState` 状态切换 + 放置朝向）；**tile entity 层受阻于写入范围**，见 §4.1。 | `B/InfestedFurnaceBlock.java:34-66`；`out109/block/BlockInfestedFurnace.java:29-131`；`out109/tileentity/TileEntityInfestedFurnace.java` |

## 6. 验证

```
JAVA_HOME=D:/MC/jdk/graalvm-community-25.3.4.1+1.1 ./gradlew.bat build -x test --console=plain   # BUILD SUCCESSFUL
node scripts/run-all-verifications.cjs                                                           # {"total":108,"passed":107,"failed":1}
node scripts/verify-block-dedicated-classes.cjs                                                  # ok (all 108 legacy ids use dedicated SRP block classes)
node scripts/verify-block-r2-backlog.cjs                                                         # ok
```

唯一失败项 `verify-blizzard-star-terrain.cjs` 属 blizzard 的地形生成范围（失败信息指向 `SRPFracturedTerrainHandler`），与本任务无关；基线 92/92 未出现回归。

提交：`27c489e9`、`da24bcb3`、`6ba58e8b`、`047c266f`（均未 push）。
