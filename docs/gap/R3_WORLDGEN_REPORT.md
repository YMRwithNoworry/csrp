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

## 4. 殖民地建筑 B1–B4 / BS1–BS4（批次 2）

全部 8 个类已 1:1 转写，落点 `src/main/java/alku/csrp/world/gen/`。

| 原类 | 26.3 实现 | 关键参数（与原始一致） |
|---|---|---|
| `WorldGenParasiteColonyB1` | `WorldGenParasiteColonyB1` | 地面 12/12/12 红污；塔高 `22+nextInt(3)`、半径夹取 `min(9,+2)`/`max(3,-1)`、`tic=2`、`cool=3`；`he=posss`、`aa=zz`、`bonusH=nextInt(5)`、`posss.above(18+bonusH-zz/2*2)`；1/2 概率 `generateSphere(zz+1, 2, ..., 1, false, 1, 1, 1, 5)`；三条 DNA 螺旋（bone block / flesh stain / bone block，位于 `he.below(4|3|2)`，`aa-2` 半径、2 圈、`11+bonusH` 螺距）。 |
| `WorldGenParasiteColonyB2` | `WorldGenParasiteColonyB2` | 无塔体；`height=22+nextInt(10)`，`spa = height / sec`（**整数除法**，原版如此）；五条螺旋 flesh/sackflesh/bone rubble/bone block(`--kil`)/feeler(`kil+2`, 2 圈)；1/2 概率直接返回，否则 `generateSphere(4, 3, rand, rand.nextInt(20)==0, 4, **true**, 1, 3, 2, ...)` —— 全家族唯一使用 `random=true` 生长标志与 1/20 倒尖的结构。 |
| `WorldGenParasiteColonyB3` | `WorldGenParasiteColonyB3` | 基底 `generateSphere(4,3,...,6,...,2,1,5)`；`above(12)`、半径 8 圆上取点、塔高 20、`xx=min(3)/zz=min(2)`；顶端 `3/3/3` 穹；`above(10)` 后 1/2 直接 `addEntrance(enter, 5)`，否则半径 4 处再起 15 高塔 + `addEntrance(5)`。 |
| `WorldGenParasiteColonyB4` | `WorldGenParasiteColonyB4` | `generateSphere(4,3,...,7,3,1,5)`；两次 `above(16)`，中间在半径 6 圆上再放 `5/3/3` 穹；1/2 概率只挖入口，否则半径 3 再放 `3/3/3(2,2,5)` 后挖入口。**原版可达性**：B4 只被 `BlockColonyStructure` 的 `updateTick` 里 `case 3` 分支引用，而该分支的 `rand.nextInt(3)` 永远取不到 3 → **在 1.10.9 中不可达**（详见 §4.1）。 |
| `WorldGenParasiteColonyBS1` | `WorldGenParasiteColonyBS1` | 地面 12/8/8；半径 4 圆上取点、塔高 8、`min(3)/min(2)`；顶端 `3/3/3` 穹；`above(10)` 后 1/2 返回，否则半径 4 再起 7 高塔 + 穹。 |
| `WorldGenParasiteColonyBS2` | `WorldGenParasiteColonyBS2` | 从同一个 `enter` 出发的三段塔（半径 7/高 28/初始 zz=2、半径 7/高 47/zz=1、半径 2/高 17/zz=1），每段以 `3/3/3` 穹收尾。三段循环体完全相同，移植为单个私有 `tower(...)` 并保留三组参数。 |
| `WorldGenParasiteColonyBS3` | `WorldGenParasiteColonyBS3` | 最小建筑：半径 3 圆上 `generateSphere(2,10,...,3,false,2,1,2)`，半径 6 圆上 `generateSphere(2,10,...,3,false,4,2,4)`。 |
| `WorldGenParasiteColonyBS4` | `WorldGenParasiteColonyBS4` | 塔高 `22+nextInt(3)`、`min(9,+2)/max(3,-1)`、`tic=2`、`cool=3`；`above(18+bonusH-zz/2*2)` 处 1/2 概率 `zz+1 / 2` 肉穹；仅在 `he.below(3)` 挂一条 bone block 螺旋。**原版可达性**：BS4 在 1.10.9 中**没有任何实例化点**（`grep` 全树 0 命中）→ 纯死代码。 |

### 4.1 原版可达性核查（重要）

对 `out109` 全树 grep 各建筑类的实例化点：

| 建筑 | 实例化点 | 是否可达 |
|---|---|---|
| B1 | `BlockColonyStructure#updateTick`（stage1 default）、`BlockColonyOutpost#updateTick`（stage1 末支）、`BlockColonyStructure` 死亡 `case 100` | ✅ |
| B2 | `BlockColonyStructure#updateTick` case2、`BlockColonyOutpost#updateTick` 中支、死亡 `case 100` | ✅ |
| B3 | `BlockColonyStructure#updateTick` case1、`BlockColonyOutpost`（stage1 与放置时 case 2） | ✅ |
| B4 | `BlockColonyStructure#updateTick` 的 **`case 3`（`nextInt(3)` 不可达）**、死亡 `case 100` | ❌ 不可达 |
| BS1 | `BlockColonyStructure#updateTick` stage2 case1、`BlockColonyOutpost#updateTick` stage2（两个分支都建 BS1） | ✅ |
| BS2 | `BlockColonyStructure#updateTick` stage2 default | ✅ |
| BS3 | `BlockColonyStructure#updateTick` stage2 case2 | ✅ |
| BS4 | **无** | ❌ 不可达 |

另：`BlockColonyStructure#func_180639_a`（右键）里的 `switch (i)` 只用 `int i = 101;` 进入，
因此 `case 100`（围着核心在东西南北 20 格外各建一座 B1/B2/B3/B4）与 `case 101`（空体，只声明 `int var26 = 20`）
**全部是死分支**；`i = -2/1/2/3/5/7..15` 亦不可达。

**移植决策**：`ColonyStructureGenerator.generateBuilding` 按原版 `updateTick` 的**可达**派发表逐字实现
（stage1：`nextInt(3)` → 1→B3、2→B2、3→B4（不可达但原样保留）、default→B1；stage2：1→BS1、2→BS3、default→BS2），
同时提供 `ColonyStructureGenerator.ColonyBuilding` 枚举 + `place(...)` 显式入口，让 B4/BS4 不被"静默丢弃"而又不伪造"原版会自动生成它们"的行为。

### 4.2 `BlockColonyOutpost` 的 `makePillar` 门槛

`BlockColonyOutpost#updateTick` 在建造前会先跑 `makePillar(pos, 23, 4)`（stage1）或 `makePillar(pos, 13, 3)`（stage2）：
从方块上方开始向上数，遇到 `ParasiteStain` 就继续、否则消耗 `blocks` 并把 10 格半径内的
非本模组方块（硬度 ≤ 7）清成空气，最终在脚下写 flesh stain；
`totalCheck` 用尽则返回 true 允许建造。

该逻辑属于 `block/**`（`BlockColonyOutpost` 的移植点，不在本任务写入范围），因此**未移植**，
在此登记为"待接线"项（见 §6），并给出契约：`boolean makePillar(ServerLevel level, BlockPos pos, int totalCheck, int blocks)`。

### 4.3 `WorldGenParasiteColonyBS1` 的两个死方法

`placeWallsBottom(World, BlockPos, int, Random, IBlockState, int)` 与
`placeWallsTopIn(World, BlockPos, int, boolean, Random, int, IBlockState)` 在 1.10.9 中**没有任何调用点**
（包括它们所在的类自身），是遗留死代码，故**不移植**（其墙柱/藤蔓原语已由基类的
`placeColumn`/`directionToGrow`/`addVines` 覆盖）。校验脚本 `verify-worldgen-colony-buildings.cjs`
断言 BS1 不会把这些死方法"悄悄长回来"。

---

## 5. 陨石撞击 `WorldGenParasiteMeteorCrash`（批次 3）

**原类**：`world/gen/feature/WorldGenParasiteMeteorCrash.java`（270 行，`extends WorldGenParasiteColonyBase`）
**26.3**：`src/main/java/alku/csrp/world/gen/WorldGenParasiteMeteorCrash.java`（逐行移植）
**稳定门面**：`alku.csrp.world.MeteorCrashFeature#generate(level, random, pos, type)` —— 保持 `MeteorEntity` /
`ParasiteProjectileEntity` 既有调用签名不变，内部只做 `new WorldGenParasiteMeteorCrash(type).generate(...)`。

### 5.1 逐段比对

| 原版语句 | 移植 | 结论 |
|---|---|---|
| `world.getTopSolidOrLiquidBlock(posss).down()` | `MeteorImpactUtil.topSolidOrLiquid(level, posss).below()` | 一致。 |
| `type != 5`：`rand.nextInt(9)` 选 1 个 fragment 模板 | `switch (rand.nextInt(9))` + `FRAGMENTS[]` | 一致；9 个模板全部存在。 |
| `origin = surface.add(2,2,2)` + `generateInPosition(..., -2,-2,-2)` | `StructurePlacer.place(level, id, impactCenter)` | 两次偏移相互抵消 → 模板落在撞击面本身，一致。1.12.2 `Template#addBlocksToWorld` **不接收 Random**，26.3 `StructureTemplate#placeInWorld` 只把 `RandomSource` 递给 `StructureProcessor`（本处 settings 无 processor），故模板投放**不消耗随机数**，后续火焰投点与原版同一随机流。 |
| 火：`18 + rand.nextInt(18)` 次、半径 10、`dx²+dz² ≤ r²` | 同 | 一致。 |
| 火：`top.y > 5`、`isBlockLoaded`、下方材质 ∉ {空气, 水, 岩浆}、上方为空气 | 同（`Fluids.WATER/LAVA`、`level.isLoaded`） | 一致。 |
| `world.setBlockState(firePos, FIRE, 2)` | `ParasiteGenContext.setBlock(...)`（flag 2） | 一致。 |
| 主陨石：`enter = impactCenter.down(10)`，20 层 `replaceCircleGround(enter.up(i), type*7, RED)` | 同 | 一致（`type*7 = 35`）。 |
| `posss = impactCenter.down(rad*rad)`；`minCenterY = rad*16+6` | 同 | 一致（86）。 |
| `generateSphere(posss, rad*16, rad*16, rand, false, 1, false, 1, 1, 5, AIR, AIR, AIR, 2)` | 同（直接用基类 `generateSphere`） | **本批次修正**：旧 `MeteorCrashFeature` 用自写的 `carveAirSphere`/`carveCircleAir` 复刻，可见效果相同（`flagAir` 使 rim 分支失效、仅剩 `rand.nextInt(incomplete)` 判定），但丢掉了原版每层两次被丢弃的 `rand.nextBoolean()` 与 `generateCircle` 的椭圆归一化路径。现改为直接调用基类原语，与原文逐字对应。 |
| `yaw = rand.nextFloat()*360F`、`dirX=-sin`、`dirZ=cos`、`steepness=0.25F+rand.nextFloat()*0.75F`、`dirY=-steepness` | 同 | 一致。 |
| `carveAngledTunnel(world, surface.down(3), 10, 30, dirX, dirY, dirZ)` | 同 | 一致。 |
| `baseR = rad*8`、`baseDepth = (int)(baseR*(0.4F+rand.nextFloat()*0.2F))`、`openNeeded+3`、`depthDrivenR = (int)(adjustedDepth*1.6F)`、`placeY = max(bottomY+1, 6)` | 同 | 一致。 |
| `clearVegetationInArea(surface, adjustedR*2, y-depth-12, y+50)` | 同 | 一致。 |
| `carveCraterBowl / scorchRings / spawnEjecta / microCraters` | 同（`MeteorImpactUtil`） | 一致（既有工具类逐函数核对无缺项）。 |
| 死血池：`poolR = max(4, adjustedR/6)`、`skipR = 10`、`bottomY2 = max(6, y-depth+1)`、池高 4、坐标为空气则写 DeadBlood | 同 | 一致。 |
| `half=22, fix=2`；`meteorPos = structPos.up(14).add(-24,0,-24)`；投放 `meteor` 模板 | 同（`csrp:meteor`，文件存在） | 一致。 |
| 战利品三循环 `-11..11`；玻璃类跳过；`iron/gold/diamond_block` → rare/uncommon 概率 10/4、7/3、4/2，写对应 tier 肿瘤并在其上方写 DeadBlood | 同（`ParasiteGenContext.placeLoot` + `Tier`） | 一致。玻璃判定：原版排除 `BlockGlass`、`BlockPane`(Material.GLASS)、玻璃板/染色玻璃板；26.3 用 `BlockTags.IMPERMEABLE` + `GLASS_PANE` + `StainedGlassPaneBlock`，集合等价（铁栏杆是 Material.IRON 的 pane，原版**不**跳过，该标签也不含它）。 |
| 非玻璃分支写 `ParasiteStain.getStateFromMeta(2)` | `ParasiteGenContext.STAIN_FLESH` | 一致（`StainVariant` 序 DIRT/FEELER/**FLESH**/… → meta 2 = flesh）。 |
| `updateWaterAfterImpact(surface, adjustedR, adjustedDepth)` | 同 | 一致。 |
| `markMainMeteor(...)`（**原版没有调用**） | **已移除** | 原版 `WorldGenMeteorImpactUtil` 的 `markMainMeteor`/`isNearMainMeteor`/`tickPendingStructures`/`scheduleDelayedStructure` **在 1.10.9 全树无任何调用点**（已 grep 验证），属遗留 API。旧 `MeteorCrashFeature` 在撞击末尾多加了一次 `markMainMeteor`；本批次删除以贴合原文，API 本体保留，且因无读取方，删除对行为**零影响**。 |

### 5.2 音效 / 伤害 / 掉落（原类之外，登记核验结论）

这些**不在** `WorldGenParasiteMeteorCrash` 内，而在 1.10.9 `entity/projectile/EntityMeteor.java` 的撞击帧：
伤害 `getDistancePack(pos, mob.pos, SRPConfigWorld.meteorDamage)` → `hurt(DamageSource.field_82729_p, str*450F)`；
`getDistancePack(..., 800) > 0.5` 时 `COTH_E 1200 tick`；`EntityOrbBoom(root ? 40 : 8, 1)`；
根陨石且 `originActivated` 时 `ParasiteEventWorld.placeOriginInWorld(...)`；最后实例化本特性类并在 `type = root ? 5 : 1` 下调用。

对应 26.3 现状（`entity/MeteorEntity.java`，**entity 范围，非本任务写入范围**，仅核验）：
第 162–197 行已具备 `Config.meteorDamageRadius()` → `hurt(fallingBlock, strength*450F)`、
`applyCothEffect(living, this, 1200, 0, false, false)`、`OrbBoomEntity`、`ModSounds.METEOR_IMPACT` 播放，
并以 `RandomSource.create()`（等价原版 `new Random()`，均无种子）调用 `MeteorCrashFeature.generate(..., rootMeteor ? 5 : 1)`。
→ **结论：不缺失，本任务无需改动**；唯一观察项：伤害半径取 `Config.meteorDamageRadius()` 而非原版 `SRPConfigWorld.meteorDamage`（配置项重命名）。

### 5.3 `WorldGenStructure` 的替代

原 `WorldGenStructure`（43 行）只做「按名字取模板 → Mirror/Rotation 均 NONE → `Template#addBlocksToWorld`」加一次
`world.notifyBlockUpdate(pos, state, state, 3)`。`StructurePlacer.place` 完整覆盖（`sendBlockUpdated(origin, state, state, 3)`、
`Mirror.NONE`、`Rotation.NONE`、flag 2），并额外提供 anchor/rotation 重载给 `DeadheadTreeGen`。
**差异**：原版 `WorldGenStructure.structureName` 是 `static` 字段（多线程共享，属原版缺陷），移植版改为逐次传 id；原版
`TemplateManager` 取自 **dimension 0** 的服务器实例（非主世界生成会取错 manager），移植版用 `level.getStructureTemplateManager()`。

### 5.4 `WorldGenParasiteGenAbstract` / `WorldGenParasiteTreeAbstract` / `WorldGenDeadheadTreeStructure` / `WorldGenMeteorImpactUtil`

| 原类 | 结论 |
|---|---|
| `WorldGenParasiteGenAbstract` | 不新增文件：语义已在 `ParasiteGenContext`（`canGrowInto`/`isReplaceable`），被 10 个既有特性类使用；本次仅补 `isModBlock`/`isFullCube` 供殖民地家族使用。 |
| `WorldGenParasiteTreeAbstract` | 同上，`setDirtAt`/`isReplaceable` 已在 `ParasiteGenContext`。 |
| `WorldGenDeadheadTreeStructure` | 既有 `DeadheadTreeGen` 覆盖（4 个锚点、旋转对齐、4 张 `deadhead_tree_large_*.nbt`），本批次无改动。 |
| `WorldGenMeteorImpactUtil` | 既有 `MeteorImpactUtil` 覆盖全部 11 个公开方法；本批次无逻辑改动（仅确认 §5.1 的调用点与参数完全一致）。 |

### 5.5 「不适用」项清单（R3 范围内）

| 原类/成员 | 判定 | 理由 |
|---|---|---|
| `WorldGenCustomStructures implements IWorldGenerator` | 不适用 | 1.12.2 Forge `IWorldGenerator` 在 26.3 无对应接口；其 `generate(...)` 本体为空，唯一有语义的 `generateInPosition` 已由 `StructurePlacer.place(level, id, pos.offset(dx,dy,dz), random)` 承接，`generateStructure(...)` 的随机点/地表高度/概率门槛由既有 `ParasiteBiomeDecorator` 承接。 |
| `WorldGenCustomStructures#calculateHeight(Block topBlock)` | 不适用 | 「向下找首个等于指定方块的 y」→ 26.3 用 `Heightmap`/`getHeightmapPos`（既有 `ParasiteGenContext.surface`），语义等价且不逐块加载。 |
| `WorldGenParasiteColonyBS1#placeWallsBottom` / `#placeWallsTopIn` | 不适用（死代码） | 1.10.9 全树无调用点；语义已由基类 `placeColumn`/`directionToGrow`/`addVines` 覆盖。 |
| `WorldGenParasiteColonyB4` / `BS4` 的**自动**生成 | 不适用（原版即不可达） | B4 仅被 `rand.nextInt(3)` 的 `case 3` 引用、BS4 无任何实例化点，见 §4.1；移植为显式 `ColonyBuilding` API，不伪造自动投放。 |
| `BlockColonyStructure#onBlockActivated` 的 `case 100/101` | 不适用（原版即死分支） | 方法内 `int i = 101;` 硬编码，其余 case 永不进入，见 §4.1。 |
| `WorldGenMeteorImpactUtil#markMainMeteor/isNearMainMeteor/tickPendingStructures/scheduleDelayedStructure` | 原版即无调用点 | API 保留于 `MeteorImpactUtil`；撞击流程不再调 `markMainMeteor`（§5.1 末行）。`tickPendingStructures` 由既有 `MeteorInfectionSystem` 每 tick 调用（移植侧新增调度点），因 `scheduleDelayedStructure` 无人调用而恒为空操作。 |
| `GenLayerSRPStar` / `world/star/**` | 不适用（他人范围） | 由 teammate `blizzard-r2` 负责；本任务不触碰 `world/star/**`。 |
| `MobCaps` / `SRPWorldEntitySpawner` / `SRPWorldParasiteSpawner` | 不适用（他人范围） | 由 Lead 负责（`NaturalSpawnTables` / 刷怪上限）；本任务不触碰。 |
| `SRPExplosion` / `CelestialNightData` / `ExtremeSnow*` | 不适用（范围外） | 分别属实体爆炸、天体事件、暴风雪（`blizzard-r2`）范围。 |

---

## 6. 待接线（outside task-3 write scope）

| 项 | 应由谁接 | 契约（已稳定，勿改签名） |
|---|---|---|
| `WorldGenParasiteNexusProtection1` | `entity-ai`：`EntityPDispatcher`（原版 1.10.9 第 278 行） | `new WorldGenParasiteNexusProtection1(1).generate(ServerLevel level, RandomSource random, BlockPos pos)` |
| `WorldGenParasiteNexusProtection2` | `entity-ai`：`EntityPBeckon`（原版第 40 行） | `new WorldGenParasiteNexusProtection2(1).generate(level, random, pos)`（内部投放 `csrp:beckon_2x2_1` … `beckon_5x5_1`） |
| `WorldGenParasiteNexusProtection3` | `entity-ai`：`EntityPRooter`（原版第 142 行） | `new WorldGenParasiteNexusProtection3(1).generate(level, random, pos)` |
| 节点保护（同上三者） | 命令侧：`/srp summon_nidus`（原版 `SRPCommandSummonNidus` 第 50 行） | 同上；原版返回 `boolean generated` |
| `BlockColonyOutpost#makePillar` 门槛 | 由 Lead 指派（`block/**`） | `boolean makePillar(ServerLevel level, BlockPos pos, int totalCheck, int blocks)`，见 §4.2 |

---

## 7. 批次记录

| 批次 | 内容 | 构建 | 校验 | commit |
|---|---|---|---|---|
| 1 | 基类 `WorldGenParasiteColonyBase`、`WorldGenParasiteColonyCore`、`WorldGenParasiteNexusProtection1/2/3`、`ParasiteGenContext` 调色板 + `sideCurse` 修复、`ColonyStructureGenerator.generateCore` 接入 | BUILD SUCCESSFUL | 95/95 | `9c60c449` |
| 2 | `WorldGenParasiteColonyB1/B2/B3/B4`、`BS1/BS2/BS3/BS4`、`ColonyStructureGenerator` 派发表 + `ColonyBuilding` 枚举 | BUILD SUCCESSFUL | 见提交说明 | 见 git log |

