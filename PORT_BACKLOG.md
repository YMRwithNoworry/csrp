# csrp-26.3 移植待办（对齐原模组 SRParasites 1.10.9）

目标：把原模组（1.12.2 Forge，本任务附件 `[逃逸：寄生体] SRParasites-1.10.9.jar`）100% 移植到 **MC 26.3 / NeoForge 26.3.0.1-beta**（本工程）。

## 参照源

| 参照 | 路径 | 用途 |
| --- | --- | --- |
| 原模组 1.10.9（目标版本） | 附件 jar；反编译产物 `D:\code\MC模组\_scratch\vf\out109` | 事实来源 |
| 原模组 1.10.8（本工程移植基线） | `D:\code\模组反编译器\杂物\[逃逸：寄生体] SRParasites-1.10.8.jar`；反编译产物 `_scratch\vf\out108` | 差异基线 |
| 兄弟分支（Forge 1.20.1） | `D:\code\MC模组\_scratch\ref1201`（提交 `0e2d7b2f` 快照） | 行为实现参照 |
| 校验套件 | `scripts/run-all-verifications.cjs`（92 项） | 完成度度量 |

## 当前基线

- `gradlew build`：**成功**（GraalVM 25.3.4.1 / Gradle 9.2.1）。
- 校验套件：**92 / 92 通过**（提交 `7b4dccd0`）。
- 资源：结构 NBT 55/55、音效 1008/1008、纹理对齐、语言 37 套；1.10.9 新增的 11 张纹理 / 1 个音效 / 4 个死头树 NBT 已并入。
- 世界生成：寄生体植被与群系装饰已移植（提交 `6639074f`），**尚未在运行时验证**。

## 已完成

- **校验契约对齐 33→92**：27 个脚本按 26.3 API 重写断言（不降级），修掉 4 处 CRLF 匹配失效，补 4 处真实行为缺口（鞭毛虫拉扯减益、召唤者冷却 200/160、生物质逐轴生长、伤害适应受击染色），修 9 个台阶方块模型、陨石投射物 id、sounds.json 事件键。
- **1.10.9 冷星内容**：5 个方块（死头藤短/长、雪草短/双高、积雪草）、`DeadheadTreeGen`、`SnowGrassEvents`、`SrpColdStarSelection` + `SrpWorldData` 的 fractured/mushroom 持久化、18 条语言键。
- **生物群系装饰**：`world/gen/**`（12 个特性类）、`ParasiteBiomeDecorator`、`ChunkDecorationQueue`、`SrpWorldData.decorated_chunk_regions`。
- **结构模板修复**：6 个 NBT 的 `srparasites:` 命名空间改为 `csrp:`（原先 meteor / 死头巨树 / 死头村庄铁匠铺模板整体无法加载）。

## 待办

### R1 世界生成的运行时验证（最高优先）
装饰链路的运行时行为**未经证实**：用 `/csrp` 无法切换星型（只在创建世界的界面里选），手工写入的 `world/dimensions/minecraft/overworld/data/csrp/csrp_world_data.dat`（star_type=warm）在服务器启动后被覆盖回 `normal`，因此从未产生寄生群系区块。需要：
- 查明 SavedData 覆盖原因（`data_version`/`initialized`/写入时机），或
- 写一个 GameTest 直接调用 `ParasiteBiomeDecorator.decorate(level, chunk, surfaceOf(chunk))` 并在 WARM 星型下跑 `runServer` 生成新区块；
- 验证要点：`run/logs/latest.log` 无 `Parasite biome decoration failed`；区块调色板出现 `csrp:parasitetrunk` / `csrp:parasitebush` 等；无 TPS 塌陷。

### R2 1.10.9 版本增量剩余
- 暴风雪客户端渲染：`client/weather/**`（Blizzard client/events/direction/fog/renderer、SoundBlizzardReverse）、`MixinEntityRendererBlizzard`、`MixinRenderGlobalBlizzardSky`、`AccessorShaderGroup`、`StarWorldShaderManager` 差集 —— 1.12.2 的 ShaderGroup/EntityRenderer 钩子在 26.3 无对应，需按本工程既有的 shader/fog 通道重做。
- `SRPFracturedTerrainHandler`（碎裂地形，389 行）——世界创建设置项，默认关闭。
- `SRPBlizzardDerivedHandler`、`GenLayerSRPDynamicStar`、`SRPStarTypeSyncHandler`、`MsgSyncStarType`、`MsgSyncBlizzardReverse`。
- `client/gui/GuiSRPWorldSettings` 差集、`util/config/SRPConfig*`、`item/ItemMobSpawner`（zaaadapted/wymoadapted 条目）、`world/SRPSaveData`、`network/SRPCommandEvolution`、`proxy/CommonProxy`、`init/SRPSpawning`（4→6 / 2→3 上限）、`ParasiteSummon` 陨石门槛、`BlockDeadheadLeaves` 距离 7 衰减、`BlockParasiteTrunk.canSustainLeaves`、`BlockParasiteBush.THORN`、`BlockEvolutionLure` luredValueNine/Ten。

### R3 世界生成剩余特性
殖民地建筑 B1-B4 / BS1-BS4 / 核心 / 基底、节点保护 1-3、陨石撞击（`WorldGenParasiteMeteorCrash` 已部分由 `MeteorCrashFeature` 覆盖，需比对）。

### R4 方块保真化
`ModBlocks.registerLegacyBlocks()` 仍有 108 个占位方块：
- **42 个在原模组里有专用类**（`assimilated_blossom`、`bloodyice`、`colonyoutpost`、`dispatchern`、`gore*`×6、`harlequinn_grass`、`hirsute_hair`、`infested_cactus`、`infested_leaves(_fast)`、`infestedbush`、`infestedore`、`lipoma_mass`、`noderelay`、`parasitebush`、`parasitecanister(+bag_wall)`、`parasitefog`、`parasiteplank(+deadhead_wall)`、`parasiterubble`、`parasitesapling`、`parasitestain(+flesh_wall)`、`parasitetendril`、`potted_*`、`relay_controller_dummy`、`relaycontroller`、`tresses_hair`、`*_wall` 家族、`*_fence`、`*_pot`），按 `out109/block/*.java` 实现。
- **66 个是 1.12.2 元数据变体**（`*_slab_double`、`*slabhalf`、`*stairs`、`infested_furnace_lit` 等），需要按变体语义实现或明确保留占位。

### R5 物品补齐
原模组有、本工程缺的 40 个物品 id：`bow_core/grip/lowerlimb/string/upperlimb`、`scythe_back/blade/core/handle/head`、`discone/disctwo`、`relay_report/scan_report/vector_report`、`ada_burrower_drop`、`itemtab`、4 个缺失刷怪蛋（`flam/soo/tenn/worker`）、若干进度图标物品。

### R6 行为逐实体核对
92 项校验覆盖到实体的主要契约；剩余需以反编译源码逐条 diff（73 个专用 AI 类、玩家交互、存档兼容）。

## 提交规范

每批次：`gradlew build`（`JAVA_HOME=D:\MC\jdk\graalvm-community-25.3.4.1+1.1`）必须 `BUILD SUCCESSFUL`，`node scripts/run-all-verifications.cjs` 不得新增失败项，然后提交（`-（类别）说明`）并推送到 `origin/port/neoforge-26.3`。
