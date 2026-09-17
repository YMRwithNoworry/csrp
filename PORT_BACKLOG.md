# csrp-26.3 移植待办（对齐原模组 SRParasites 1.10.9）

目标：把原模组（1.12.2 Forge，本任务附件 `[逃逸：寄生体] SRParasites-1.10.9.jar`）100% 移植到 **MC 26.3 / NeoForge 26.3.0.1-beta**（本工程）。

## 参照源

| 参照 | 路径 | 用途 |
| --- | --- | --- |
| 原模组 1.10.9（目标版本） | 附件 jar；反编译产物 `D:\code\MC模组\_scratch\vf\out109` | 事实来源 |
| 原模组 1.10.8（本工程移植基线） | `D:\code\模组反编译器\杂物\[逃逸：寄生体] SRParasites-1.10.8.jar`；反编译产物 `_scratch\vf\out108` | 差异基线 |
| 1.10.8 源码（已反编译） | `D:\code\模组反编译器\decompiled\[逃逸：寄生体] SRParasites-1.10.8` | 行为核对 |
| 兄弟分支（Forge 1.20.1，行为契约全绿） | `D:\code\MC模组\_scratch\ref1201`（提交 `0e2d7b2f` 的快照） | 行为实现参照 |
| 校验套件 | `scripts/run-all-verifications.cjs`（92 项） | 完成度度量 |

## 当前基线（本轮开始时）

- `gradlew build`：**成功**（GraalVM 25 / Gradle 9.2.1）。
- 校验套件：**33 / 92 通过**（59 项失败）。
- 资源：结构 NBT 55/55、音效 1008/1008、纹理对齐（本轮补入 1.10.9 的 11 张纹理、1 个音效、4 个 NBT）。

## 批次

### B1 校验契约对齐 — 纯系实体（进行中）
`verify-grunt/monarch/overseer/vigilante/warden/seeker-port`：判定"断言漂移"还是"真实缺口"，真实缺口从 1.20.1 快照/原模组补实现。

### B2 校验契约对齐 — 原始系实体（进行中）
`verify-primitive-*`、`verify-vermin-entities-port`、`verify-longarms-*`（10 项）。

### B3 校验契约对齐 — 客户端/模型/资源（进行中）
`verify-entity-animation-contracts`、`-extracted-entity-resources`、`-marauder`、`-buglin`、`-rupter`、`-spawn-egg-textures`、`-fog`、`-kirin-effects`、`-assimilated-feral-entities`（9 项）。

### B4 校验契约对齐 — 适应/粗制/载体/劫持系（待做）
`verify-adapted-arachnida/-adapted-tozoon/-burrower-entities/-carrier-entities/-crude-entities/-dredge/-airscrew-tether-effect/-gnat-lice/-hijacked-and-enderman/-marauderized`（10 项）。

### B5 校验契约对齐 — 世界与系统（待做）
`verify-original-bomb-behavior/-original-wave-behavior/-original-source-behavior/-projectile-entity-ids/-relay-tower/-parasitic-growth/-overlast-evolution-hud/-title-credits/-original-sounds/-coth-application/-damage-adaptation/-assimilation-wand/-assimilated-disguise/-evolution-lures/-equipment-items/-sim-adventurer/-preeminent-*/-infested-*-blocks/-parasite-trap-blocks/-alveoli-blocks`（约 24 项）。

### B6 1.10.9 版本增量（待做）
1.10.9 相对 1.10.8 的新增内容（本工程目前**完全没有**）：
- 4 个新方块：`deadhead_grass_short`、`deadhead_grass_tall`、`snow_short_grass`、`snow_tall_grass`（纹理已补入，缺注册/模型/语言/掉落表）
- `SnowGrassHandler`（积雪草转换）、`SRPBlizzardDerivedHandler` + `ExtremeSnowServer/Data` + `CommandExtremeSnow`（极寒暴风雪）
- `WorldGenDeadheadTreeStructure`（死头树 NBT 结构放置，`deadhead_tree_large_1..4.nbt` 已补入）
- `SRPColdStarTreeHandler`（蘑菇树/叠层树生成）
- `SRPFracturedTerrainHandler`（碎裂地形世界生成）+ 世界创建设置项
- `SRPColdVillageWallGenerator`（冷星村庄城墙）
- 世界设置 GUI：`fractured`、`mushroom_trees` 开关及 12 条提示文本（1.10.9 新增 18 条语言键）
- 移除：`parasitebush_frostg`、`parasitebush_frostgt`

### B7 世界生成特性（两个分支都缺，最大内容缺口）
原模组 `world/gen/**` 约 6800 行从未移植：`WorldGenCustomStructures`、`WorldGenStructure`、`HarlequinRockBushGen`、26 个 `WorldGenParasite*` 特性（树/细树/灌木/高花/十花/球/大球/脊柱/口器/节点核心/殖民地 B1-B4/BS1-BS4/核心/基底/节点保护 1-3/陨石撞击），以及 `BiomeParasiteDecorator` 的每生物群系概率表与 `BiomeParasite{Base,Boils,Demen,Harlequin,Shrouded}` 参数。
现状：55 个结构 NBT 已就位，但**没有任何代码放置 harlequin/dh_village/beckon 系列**。

### B8 方块保真化（待做）
`ModBlocks.registerLegacyBlocks()` 目前有 108 个"按名字猜形状"的占位方块；其中约 60 个在 1.12.2 原模组里有真实实现（灌木 `parasitebush_*`/`infestedbush_*`、矿石 `infestedore_*`、血肉 `gore*`、`evolutionlure_one..ten`、`parasiterubble*slab/stairs`、`parasitestain*slab/stairs`、`parasitetrunk_*`、`parasitesapling_*` 等），需按反编译源码实现。

### B9 物品补齐（待做）
原模组有、本工程缺的 40 个物品 id：`bow_core/grip/lowerlimb/string/upperlimb`、`scythe_back/blade/core/handle/head`、`discone/disctwo`、`relay_report/scan_report/vector_report`、`ada_burrower_drop`、`itemtab`、4 个缺失刷怪蛋（`flam/soo/tenn/worker`）、以及若干进度图标物品。

### B10 行为逐实体核对（长期）
原模组 73 个专用 AI 类；B1-B5 的校验契约覆盖其中一部分，剩余实体需以反编译源码为基准逐条 diff（`registerGoals`、技能、状态机、NBT、配置项）。

## 提交规范

每批次：`gradlew build`（GraalVM 25，`JAVA_HOME=D:\MC\jdk\graalvm-community-25.3.4.1+1.1`）必须 `BUILD SUCCESSFUL`，`node scripts/run-all-verifications.cjs` 不得新增失败项，然后提交（`-（类别）说明`）并推送到 `origin/port/neoforge-26.3`。
