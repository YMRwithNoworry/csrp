# R3 世界生成剩余特性报告（task-3）

范围：`D:\code\MC模组\_scratch\vf\out109\com\dhanantry\scapeandrunparasites\world\gen\**`（1.10.9 / 1.12.2 Forge 反编译产物）
→ `D:\code\MC模组\csrp-26.3`（Minecraft 26.3 / NeoForge 26.3.0.1-beta / Java 25）。

本报告逐类给出「原类 → 26.3 实现 → 差异」三列结论，并显式标注**不适用**项与理由。
状态随批次推进更新；截止本批次（批次 1）已完成：基类语义、殖民地核心、三个节点保护。

---

## 0. 平台层面的通用差异（适用于下面所有条目）

| 原（1.12.2） | 26.3 实现 | 说明 |
|---|---|---|
| `net.minecraft.world.World` | `net.minecraft.server.level.ServerLevel` | 世界生成/结构投放全部发生在服务端；`World` 的客户端变体在 26.3 没有对应需求。 |
| `java.util.Random` | `net.minecraft.util.RandomSource` | 原类内部同时使用**传入的 rand** 与 `world.rand` 两套随机源；本移植统一为调用方传入的 `RandomSource`（或 `level.getRandom()`）。**差异**：随机取数次数保持一致，但"由哪一个对象提供"不再区分。 |
| `World#setBlockState(pos, state, 2)` | `ParasiteGenContext.setBlock(level, pos, state)`（内部 `level.setBlock(pos, state, 2)`） | flag 2 完全一致（通知客户端、跳过邻居更新）。新增了 `isLoaded` 与 build-height 保护：1.12.2 的装饰阶段必然在已加载区块内，26.3 的特性可能跨区块边界。 |
| `World#getBlockState` | `ParasiteGenContext.get(level, pos)` | 越界/未加载返回空气，避免区块生成期抛异常。 |
| `IBlockState` | `BlockState` | 无差异。 |
| `BlockPos#north/east/south/west` | `BlockPos#north()/east()/south()/west()` | SRG 同名常量映射见 §2。 |
| `instanceof BlockBase`（本模组所有方块） | `ParasiteGenContext.isModBlock(state)`（命名空间 = `csrp`） | 语义等价：`BlockBase` 是原模组所有方块的基类。 |
| `SRPConfigWorld.blockLootCommon/Uncommon/Rare` 字符串 id 列表 | `ParasiteLootBlock.Tier.COMMON/UNCOMMON/RARE` + `ParasiteLootBlockEntity#generateLoot` | 战利品池已落在方块实体里，且注释明确说明按原 `placeLoot()` 的 1/2 逐槽判定实现。 |
| `net.minecraft.world.gen.feature.WorldGenerator#generate` | 各特性类的 `generate(ServerLevel, RandomSource, BlockPos)` | 抽象方法签名替换；调用点见各节。 |

### 关于 `IWorldGenerator` 与 `WorldGenCustomStructures`

| 原类 | 26.3 结论 |
|---|---|
| `WorldGenCustomStructures implements IWorldGenerator` | **不适用，已由既有架构取代**。其 `generate(Random,int,int,World,...)` 是 1.12.2 Forge 的 `IWorldGenerator` 入口，26.3 无此类（区块装饰走 `BiomeModifier`/`Feature`/`Structure` 数据驱动管线）。本工程把它的两个私有辅助函数语义分别落到：<br>• `generateStructure(...)` 的"随机点 + 地面高度 + 生物群系/概率门槛"→ `ParasiteBiomeDecorator`（既有，R2 之前完成）<br>• `generateInPosition(WorldGenStructure, rand, world, pos, dx, dy, dz)` → `StructurePlacer.place(level, id, pos.offset(dx,dy,dz), random)`，1:1 对应（原实现就是 `new BlockPos(x+dx, y+dy, z+dz)` 后调用生成器）。 |
| `WorldGenStructure`（结构模板投放） | 已由 `alku.csrp.world.StructurePlacer` 承接（既有）。差异：原名硬编码 `srparasites` 命名空间与主世界 `TemplateManager`（`FMLCommonHandler...getMinecraftServerInstance().getWorld(0).getStructureTemplateManager()` 的旧限制），26.3 用 `level.getStructureTemplateManager()` + `csrp:` 命名空间（task 要求，不回退）。`WorldGenStructure.structureName` 是 **static 字段**（原版线程安全隐患），移植版没有复刻该状态，改为逐次传 id。 |

**⚠ 命名空间提醒**：`data/csrp/structure/*.nbt` 已全部改为 `csrp:`，本报告所有模板名均指该目录。

---

## 1. `WorldGenParasiteColonyBase`（殖民地家族基类）

**原类**：`world/gen/feature/WorldGenParasiteColonyBase.java`（614 行，抽象，继承 `WorldGenParasiteGenAbstract`）
**26.3**：`src/main/java/alku/csrp/world/gen/WorldGenParasiteColonyBase.java`

保留的语义（逐条对照）：

| 原方法 | 移植实现 | 备注 |
|---|---|---|
| `getDirectionRoot(center, direction, times)` | 同名 protected 方法 | 0=N `func_177964_d`、1=S `func_177965_g`、2=W `func_177970_e`、3=E `func_177985_f`。**注意**它和 `directionToGrow` 的编号不同，两者在移植版中保持独立，未合并。 |
| `directionToGrow(atm, choice, sideCurse)` | 同名 protected 方法 | 非 sideCurse：0=N、1=E、2=S(default)、3=W；sideCurse：委托 `ParasiteGenContext.sideCurse`（见 §2 的修复）。 |
| `placeColumn(world, pos, in, rand, extraChance, state)` | 同名 protected 方法 | `extraChance == 1.0` 向上、否则向下；循环外再放一格（原版如此）。 |
| `placeBlock` / `placeVine` / `placeReplacement` | 同名 protected 方法 | `placeVine` = `parasitebush` 的 `bine` 变体（原 `BlockParasiteBush.EnumType.BINE`）。 |
| `addVines(world, pos, rand, longer)` | 同名 protected 方法 | **差异**：原版 `rand.nextInt(chance)` 在 `chance` 递减到 0 时会抛 `IllegalArgumentException`（原调用点全部用 `veins > 0` 门控所以从未触发）；移植版把循环条件写成 `chance > 0`，既保留取数顺序又不会崩服。 |
| `replaceLayer` / `addFloor` / `genFloorFloor` / `addFloorSpace` | 同名 protected 方法 | 蛇形铺地板、15 格向下填充、未填充时打 3×3 洞口，全部照抄。 |
| `addMobSpawner` | 空方法（同原版） | 原版就是空实现，保留以支持逐行转写。 |
| `generateSphere(...)` | 同名 protected 方法（15 参数同序） | 内层锥体 + 外层冠 + 尖端段；`ticc = 2`、`50000`（第二圈固定 1/50000）、`invertedTip ? 9 : 0`、`invertedTip` 时 `heightAbove` 折半、`rand.nextBoolean() && random` 时步进 2 否则 1，全部照抄。 |
| `generateCircle(...)` | 同名 protected 方法 | 门控 `(空气 ‖ 本模组方块 ‖ flagAir) && rand.nextInt(incomplete) != 0`；边界判定 `x==radiusX ‖ z==radiusZ ‖ x==-radiusX ‖ z==-radiusZ` 及其 ±1；rim 处 `nextInt(60)==0` → `nextInt(4)==0` 放 `deadblood`（仅 `!flagAir`）否则 1/10 稀有 / 1/4 罕见 / 其余普通寄生虫战利品；`veins>0` 时挂藤；非 rim 处 `rand.nextBoolean() ? state : state2`。全部照抄。 |
| `generateDNAHelix(...)` | 同名 protected 方法 | `tStep = 0.1`、双链相差 π、`pitch/(2π)*t` 高度。原版在 `(int)Math.round(t/Math.PI) % 2 == 0` 分支里算出的 `midX/midZ` **是死代码**（`new BlockPos(...)` 结果被丢弃），移植版省略该无副作用计算。 |
| `getCirclePoint` / `getCirclePoints` / `generatePillar` | 同名 public 方法 | `generatePillar` 用 `level.getRandom()`（原 `world.rand`）。 |
| `replaceCircleGround` / `generateVerticalCircle` / `generateFilledVerticalDisk` / `addEntrance` | 同名 public 方法 | `replaceCircleGround` 跳过空气与本模组方块；`generateFilledVerticalDisk` 保留 `deadblood`/`parasitebush`；`addEntrance` 保留 `offset=3` 与 `direction != 1 && direction != 3` 的平面选择。 |
| `placeLoot(world, pos, String[], state)` + `loot(rand, drop)` | `ParasiteGenContext.placeLoot(level, pos, Tier, random)` | 原版从配置字符串数组取物品并 1/2 逐槽填充；26.3 由 `ParasiteLootBlockEntity#generateLoot(tier, random)` 提供同语义（该类注释即写明"matching placeLoot() in WorldGenParasiteColonyBase"）。三档映射：Common/Uncommon/Rare。 |
| `type`（stage 构造参数） | `protected int type` | 保留同名同义。 |

新增的边界保护（原版没有、26.3 必需）：`generateCircle` 在 `radiusX <= 0 || radiusZ <= 0` 时直接返回
——原版此时 `x/0 = NaN` 使内层判定恒假，等价于"什么都不做"，但会留下除零风险（`CometCrash`/`Core`
里确实存在 `xx - tic` 为负的调用）。

### 1.1 基类链上的另外两个基类

| 原类 | 26.3 结论 |
|---|---|
| `WorldGenParasiteGenAbstract`（`canGrowInto` / `isReplaceable`） | **已实现（既有）**，落在 `ParasiteGenContext.canGrowInto` / `ParasiteGenContext.isReplaceable`：空气、树叶、草方块、泥土、四种原木、灰化土、寄生虫灌木。 |
| `WorldGenParasiteTreeAbstract`（`func_150523_a` / `func_175921_a` / `isReplaceable`） | **已实现（既有）**：`canGrowInto` 同一套判定，`setDirtAt` → `ParasiteGenContext.setDirtAt`（草/土之外写 `infested_stain`），`isReplaceable` → `ParasiteGenContext.isReplaceable`（追加 `isWood`）。 |
| `WorldGenDeadheadTreeStructure` | **已实现（既有）**：`alku.csrp.world.DeadheadTreeGen`（1.10.9 的 4 个树锚点 + `StructurePlacer.place(..., anchor, rotation)`）。 |
| `WorldGenMeteorImpactUtil` | **已实现（既有）**：`alku.csrp.world.MeteorImpactUtil`（`carveCraterBowl`/`scorchRings`/`spawnEjecta`/`microCraters`/`carveAngledTunnel`/`clearVegetationInArea`/`updateWaterAfterImpact`/`tickPendingStructures`/`scheduleDelayedStructure`/`markMainMeteor`/`isNearMainMeteor` 全在）。差异见 §3。 |

---

## 2. `ParasiteGenContext` 的两处修正（本次新增）

1. **新增殖民地调色板**（原基类的 `floor/tacle/wall/floorColony` 字段所需）：
   `RUBBLE_BONE/BRICKS/FLESH/FUNGUS/STONE`（`csrp:parasiterubble` 的 variant 值）、
   `DENSE_WALL`（`csrp:parasiterubbledense`，即旧 `ParasiteRubbleDense.EnumType.WALL`）、
   `FOG`（`csrp:parasitefog`）、`DEAD_BLOOD`、`COOKED_FLESH`、`BONE_BLOCK`、`STONE`、`AIR`、`BUSH_BINE`。
2. **修复 `sideCurse` 对角线表（保真缺陷）**：原 `directionToGrow(..., true)` 的 8 个分支按
   `i*10` = 右手对角、`i*10+1` = 左手对角成对出现（0/1 = NE/NW，10/11 = SE/NE，20/21 = SW/SE，
   30/31 = NW/SW）。移植版原先写成 10→NE、11→SE、20→SE、21→SW（10/11 与 20/21 两对互换），
   破坏了该规律。已按 SRG 定义（`func_177978_c`=north、`func_177968_d`=south、`func_177976_e`=west、
   `func_177974_f`=east）修正。
   影响面：`WorldGenParasiteNodeCore`（`i*10`/`i*10+1`）、`WorldGenParasiteSpine`、`WorldGenParasiteTree`、
   `WorldGenParasiteTenFlower`（`dir*10+firrr`）——修正后与原版一致。所有既有校验脚本均未引用 `sideCurse`，
   `run-all-verifications` 无回归。
3. **新增 `isModBlock` / `isFullCube` / `placeLoot`**：分别是 `instanceof BlockBase`、
   `IBlockState#func_185913_b()`（`Block#isFullCube`）、基类 `placeLoot` 的等价物。

### `Blocks.field_189880_di` 的判定依据

原殖民地表壳/螺旋大量使用 `Blocks.field_189880_di` 作为 `state2`（内层交替方块）与 DNA 螺旋材料。
按 1.10 新增方块的注册顺序（magma、nether wart block、red nether brick、bone block、structure void）
与该 SRG 号段对应关系，判定其为 **bone block**，映射 `Blocks.BONE_BLOCK`。
选择理由（旁证）：它在 B2/Core 的螺旋里与 `sackflesh`/`bone rubble`/`feeler` 并列为"骨系"材料，
在 BS2 里作为骨环内圈的第二材料，作为"实心骨色方块"语义自洽。
**风险**：这是全报告唯一一处**推定映射**（缺少 MCP 映射表直接证据）。若日后拿到映射表证伪，
只需改 `ParasiteGenContext.BONE_BLOCK` 一处常量。

---

## 3. 陨石撞击（批次 3，见后续更新）

`WorldGenParasiteMeteorCrash` + `WorldGenMeteorImpactUtil` 的逐行比对结论与差异修正将在批次 3 补齐。

---

## 4. 殖民地建筑 B1–B4 / BS1–BS4（批次 2，见后续更新）

---

## 5. 批次记录

| 批次 | 内容 | 构建 | 校验 | commit |
|---|---|---|---|---|
| 1 | 基类 `WorldGenParasiteColonyBase`、`WorldGenParasiteColonyCore`、`WorldGenParasiteNexusProtection1/2/3`、`ParasiteGenContext` 调色板 + `sideCurse` 修复、`ColonyStructureGenerator.generateCore` 接入 | BUILD SUCCESSFUL | 95/95（92 基线 + 本批 2 个新脚本 + 其他 teammate 1 个） | 见 git log |
