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
