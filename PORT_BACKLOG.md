# csrp 1.20.1 Forge — 移植待办（批次清单）

目标：把 SRParasites（原模组，1.12.2 Forge，本任务附件为 1.10.9）100% 移植到 **MC 1.20.1 / Forge 47.4.23**。

三个可用的参照源：

| 参照 | 路径 | 用途 |
| --- | --- | --- |
| 目标工程 | `D:\code\MC模组\csrp-1.20.1-forge` | 本工程（`alku.csrp`，Forge 1.20.1） |
| 捐赠分支 | `D:\code\MC模组\csrp-26.3` | 同一血统、内容更靠前（NeoForge 26.3）；资源与注册可直接搬，需按 API/数据格式降级 |
| 原模组反编译源码 | `D:\code\模组反编译器\decompiled\[逃逸：寄生体] SRParasites-1.10.8` | 行为与内容的事实来源（1.10.8；附件 1.10.9 差异需另行 diff） |
| 原模组资源（geo/动画提取） | `D:\code\MC模组\srp生物模型和动画提取\提取结果` | 124 个实体的 GeckoLib geo + 动画（AST 转写） |
| 差距报告 | `GAP_ANALYSIS.md`（由 `node scripts/mkgap.py` 生成） | 机器统计的清单 |

批次状态：`[x]` 已完成并提交、`[~]` 进行中、`[ ]` 待做。

## 已完成

- `[x]` **资源同步**（`scripts/sync-donor-assets.cjs`）：从捐赠分支补入 2295 个文件——模型 934、方块状态 192、纹理 1068、语言 33、结构 NBT 41、geo/动画 18、着色器 7、音效 2。提交 `09d5d113`。

## 待做批次（按价值排序）

### 1. `[~]` 方块保真化（65 个占位方块 → 真实实现）
现状：`ModBlocks.registerLegacyBlocks()` 用 `legacyBlock(id)` 按名字猜形状，共 174 个近似占位块；捐赠分支已把其中 65 个做成真实实现（真正的门/活板门/栅栏/楼梯/台阶/墙/柱，以及 rubble/stain/trunk 家族的真实多状态）。
交付：`ModBlocks` 专用注册 + 方块物品 + `block.csrp.*` 中英翻译 + 战利品表；从占位数组移除。

### 2. `[ ]` 活体武器与护甲全家族（约 45 个物品）
捐赠分支有、本工程完全没有的内容：`scythe/cleaver/maul/lance/sword/axe/bow` 的普通与 `_sentient` 两档（`LivingWeaponItem` / `LivingBowItem` / `LivingMaulItem` / `LivingArmorItem`：`helm/chest/pants/boots[_sentient]`），以及 `weapon_*` 历史别名。
原模组独有、捐赠分支也缺：`bow_core/bow_grip/bow_lowerlimb/bow_string/bow_upperlimb`、`scythe_back/scythe_blade/scythe_core/scythe_handle/scythe_head`、`discone/disctwo`、`ada_burrower_drop`、`relay_report/scan_report/vector_report`、`itemtab`。
交付：物品类 + 注册 + 中英翻译 + 模型/纹理（已同步）+ 1.20.1 配方 + 相关进度。

### 3. `[ ]` 原模组世界生成（约 6800 行，最大缺口）
两个分支都没有移植：原模组的生物群系装饰与结构生成。
- `world/gen/structure/WorldGenStructure`（NBT 模板放置）
- `world/gen/WorldGenCustomStructures`（按区块与生物群系投结构）
- `world/gen/HarlequinRockBushGen`
- `world/gen/feature/*`：`WorldGenParasiteTree`、`TreeThin`、`Bush`、`TallFlower`、`TenFlower`、`Ball`、`BigBall`、`Spine`、`Mouth`、`NodeCore`、`ColonyBase/ColonyCore`、`ColonyB1..B4`、`ColonyBS1..BS4`、`NexusProtection1..3`、`MeteorCrash`
- `world/biome/BiomeParasiteDecorator` 的每生物群系概率表 + `BiomeParasite{Base,Boils,Demen,Harlequin,Shrouded}` 的装饰参数
现状：本工程只有 104 行的 `ParasiteBiomeGenerator` 程序化近似（`placeVegetation`/`placeBloodPool`）与程序化殖民地生成器。51 个结构 NBT 已就位（`data/csrp/structures/`），但**没有任何代码引用 harlequin/dh_village/beckon 系列**。
接入点：本工程已有 `ChunkEvent.Load` 钩子（`StarBiomeGenerationEvents.convertNewChunk`），新特性可挂在同一代。

### 4. `[ ]` 数据包内容补齐
- 配方：本工程 199，捐赠 329（差 130）；需按 1.20.1 schema 转换（`{"item":...}`、`"result": {"item":...}`、`forge:` 条件）
- 进度：本工程 43，捐赠 83（差 40）；1.20.1 用 `data/csrp/advancement/`，需转换 `icon`/`criteria` 结构
- 战利品表：本工程 272，捐赠 315
- 语言：已同步 33 个语种，但与捐赠语义键仍差约 200（依批次 1/2 补）
- `data/csrp/neoforge/biome_modifier/`（43 个）在 **Forge 1.20.1 下不会被读取**——Forge 用 `data/csrp/forge/biome_modifier/`。当前自然刷怪由 `NaturalSpawnTables` + `CommonModEvents` 程序化实现，需决定是转换这些 JSON 还是删除，注意避免双重刷怪。

### 5. `[ ]` 剩余占位方块（109 个）
捐赠分支同样仍是占位实现的部分，必须回到反编译源码实现：`infestedbush_*`、`parasitebush_*`、`infestedore_*`、`gore*_{big,flat,small}`、`evolutionlure_one..ten`、`parasiterubble*slabhalf/double`、`parasitestain*slab/stairs`、`parasitetrunk_{ball,plant,tree}*`、`parasitesapling_*`、`parasitic_colony_core_slab*` 等。

### 6. `[ ]` 实体表现层收尾
9 个实体（`buglin`、`rupter`、`marauder`、`pri_longarms/summoner/vermin/viscera/arachnida/bolster`）走 Citadel/Tabula Java 模型（比 GeckoLib 提取更忠实），而 `scripts/verify-*.cjs` 仍断言旧的 GeckoLib 契约；需要把校验脚本改写到当前架构（不得降低校验强度）。

### 7. `[ ]` 行为逐实体核对
原模组 73 个专用 AI 类；本工程已覆盖注册与部分行为，但"行为忠实度"尚无逐实体系统核对（捐赠分支只有 8 个实体做过 entity-specific 审计）。需要以反编译源码为基准，逐实体 diff `registerGoals`、技能、状态机、NBT。

### 8. `[ ]` 1.10.8 → 1.10.9 差异
本工程按 1.10.8 移植（`mod_version=1.10.8`），任务附件是 1.10.9。需要把 1.10.9 jar 反编译并与 1.10.8 反编译树做 diff，把差异逐条并入。

## 提交规范

每次批次完成：`mc_gradle build` 必须 `BUILD SUCCESSFUL`，`node scripts/run-all-verifications.cjs` 不得出现新的失败项，然后提交（`-（类别）说明`）并推送到 `origin/port-1.20.1-forge`。

### 3b. `[~]` 结构 NBT 的「属性合法性」缺口（2026-09-18 发现，阻塞第 3 批次）

`StructureTemplate.load` → `BlockStateParser.parseForBlock` 对 palette 里**属性名不存在**的条目会抛
`IllegalArgumentException`，结构**静默不生成**（只有一条日志）。已确认并修复的部分：

- `[x]` `csrp:infestedbush`（被 10 个结构引用）—— `MeteorStructureLoader.BLOCK_RENAMES` 之前只
  处理 `srparasites:` 前缀，而捐赠同步来的 NBT 已是 `csrp:` 前缀，改名表一条都没生效。已修
  （`rewriteId` 现在同时处理两种前缀，`csrp:infestedbush → csrp:residue_plants`）。
- `[x]` `csrp:parasitestain_feeler` —— 方块根本没注册（原 `BlockParasiteStain` 的 7 个变体，
  本工程按变体拆成独立方块时只注册了 flesh/dirt）。已补注册 `feeler`/`mud`/`sackflesh`
  + BlockItem + 中英译名（译名取自原版 `en_us.lang`/`zh_cn.lang`：Compressed Arteries 交汇动脉管 /
  Visceral Mud 内脏烂泥 / Sacked Flesh Block 肉囊块）。顺带校正了 dirt/flesh 的占位译名
  （"Parasite Stain (Dirt)" → "Hivesoil"，与同家族 `*stairs` 一致）。
- `[x]` 全部 55 个结构的 palette **方块 id** 均可解析（`scripts/verify-1.10.9-port.py` 的 `[6]`
  检查常驻守卫）。

**仍未验证/待做**（下一步必须先解决，否则第 3 批次的结构即使接了触发点也不会出现）：

- `[ ]` **属性合法性**：上述检查只覆盖方块 id，未覆盖属性名。已知风险点：
  `tresses_hair`（legacy 占位方块，实际是普通 `Block`，无 `facing`/`half`）、
  `parasitethin`（`ParasiteThinBlock` 的 6 个连接布尔量）、各类 `*_wall`（
  `legacyBlock` 返回 `WallBlock`，属性应为 `east/west/north/south/up`，与 NBT 对得上）、
  `*slabhalf`/`*slabdouble`/`*stairs`（`SlabBlock` 用 `type`，`StairBlock` 用 `facing/half/shape`）。
  `deadblood` 的 `level` 已确认为合法（`LiquidBlock.LEVEL` 的序列化名就是 `level`，
  与 vanilla `water.json` 同样不在 blockstate 里声明）。
- `[ ]` **权威验证手段**：静态源码解析无法可靠判定（继承链 + legacy 占位方块是工厂生成的）。
  可用的两条路：① 给 `ModBlocks` 里每个 legacy 方块补一个显式 `createBlockStateDefinition`
  断言测试；② 让服务端在 `ServerStartedEvent` 里遍历结构 NBT 并逐个 `StructureTemplate.load`，
  把失败项打进日志（推荐，最接近真实加载路径）。

### 3c. `[~]` 球体特性（2026-09-18 完成首块）

`[x]` `WorldGenParasiteBall` + `WorldGenParasiteBigBall` → `world/ParasiteBallPlacer.java`，
由 `StarBiomeGenerationEvents` 的冷星分支驱动（`ball.nbt` 7×10×7 / `ballbig.nbt` 13×21×13）。
常数额字搬运（净空 12 高 × 半径 4、锚点 (3,0,3)/(6,0,6)、细枝跳过率 0.5/0.15、侧枝率 0.2）。
`[ ]` 尚未在真实世界目视确认（需要冷星世界）。
`[ ]` 其余 25 个 `world/gen/feature/*` 仍缺（spine/mouth/nodecore/colony 家族/nexus protection/
meteor crash/tree/treeThin/bush/tallFlower/tenFlower…），且 harlequin/beckon/dh_village 需要先决定
是否补自定义生物群系（原模组用 5 个 SRP 生物群系驱动装饰，本工程完全没有）。
