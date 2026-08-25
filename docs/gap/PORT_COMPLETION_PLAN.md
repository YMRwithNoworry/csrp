# SRParasites 1.10.8 完整移植主计划

> 生成日期：2026-08-22。由四份领域差距报告（实体AI / 注册内容 / 客户端 / 世界系统）汇总而成。
> 参考源码：`D:\code\模组反编译器\decompiled\[逃逸：寄生体] SRParasites-1.10.8\com\dhanantry\scapeandrunparasites\`
> 总体结论：**实体/投射物/音效/状态效果/药水/图鉴/进化/难度/星穹/增援/脱落/感染净化等核心系统已基本完整**，缺口集中在：残骸方块体系、木系建材、世界生成结构、极寒/陨石/逃脱系统、客户端氛围层（体积雾/黑天幕/粒子）、JEI、配置面。

## 一、实体与 AI（完成度 ~97%）

- [x] `kirin_slash` 投射物实体（`EntityProjectileKirinSlash`，Kirin 闪烁斩击）+ 音效 `kirin.projectile_charge/impact/summon` —— 2026-08-22 完成：KirinSlashEntity（延迟/生长/淡出三阶段、沿线命中非寄生生物）+ 程序化光刃渲染器 + 麒麟 performBlink 发出斩击与三音效
- [ ] sim_human 的 `EntityAICircleGroup` 绕行 AI
- [ ] `EntityAIBlockLight` 光源破坏 AI（原版 geneMod(7) 配置门控，一并接配置）
- [ ] Canister 方块+方块实体（`TileEntityCanister` 容器）
- [ ] 通用化重复 Goal：SwimmingDiving/EvasiveDash/WaterLeap 在 5+ 实体中重复（重构项，非移植）
- [ ] Nexus 家族 SII/SIII/SIV 数值逐档比对原版
- 死代码不移植：AIEvadeTP、AINexusNest、AIAttackMeleeRanged、AISoundEaterStalk、AIDisableBeaconIki（原版 1.10.8 均无引用）

## 二、注册内容（物品/方块/配方）

### 方块——残骸体系（最大缺口，波及大量衍生）
- [x] `parasiterubble` 主方块 6 变种：bone/flesh/stone/weathb/weathbc/weathfs —— 2026-08-22（原版其余 wood/bricks/metal/obsidian/fungus 变体无独立贴图，待补）
- [x] `parasiterubbledense` 4 变种：wall/biome/colony/heart
- [x] `parasitestain` 4 变种：flesh/dirt/mud/feeler
- [x] `parasitetrunk` 3 变种：tree/ball/plant；`parasitethin` 2 变种；`parasitesapling` 3 变种
- [ ] 残骸系楼梯 ~21、台阶 2、墙 ~12（依赖已完成的主方块，下一批）
- [ ] `infestedrubble`（已有）；烧炼 `infested_cobblestone→infestedrubble` 已完成 2026-08-22

### 方块——木系建材闭环
- [x] 门 6：goth_door、brusewood_door、consumed_door、infested_door、flesh_door、cooked_flesh_door（含门物品）——2026-08-22，原版 1.10.8 六套全部覆盖
- [x] 活板门 6：goth/brusewood/consumed/infested/flesh/cooked_flesh——2026-08-22
- [x] 栅栏 5：goth/infested/consumed/flesh/deadhead（harleskinn/brusewood 无对应木板贴图，cooked_flesh_fence 已有）——2026-08-22
- [ ] 楼梯 10+、台阶 14（主方块已就绪，下一批）
- [x] `goth_stem`——2026-08-22
- [x] 工作台 2：infested_workbench、consumed_workbench（CraftingTableBlock，原版三面贴图）——2026-08-22

### 方块——功能/殖民地/装饰
- [x] `infested_workbench`、`consumed_workbench`（合成枢纽）——2026-08-22
- [ ] `colonyoutpost`、`relaycontroller`(+dummy)、`noderelay`（TileEntity）、`parasitecanister`
- [x] ~~消失→囊肿机制~~ 2026-08-22 完成：`canisteractive` 活体囊肿（消化物品+进化点，吃空自灭）+ `MobDespawnMixin` 超距消失时调度柱回收（storeBefDes）/落地囊肿（spawnCyst）；持久化临时方案已移除
- [ ] `assimilated_blossom` + 花盆 4（infested_pot/consumed_pot/2×potted_blossom）
- [ ] `parasitebush`、`parasitetendril`（藤蔓）、`infestedbush`、`infested_cactus`、`infested_leaves`
- [ ] `hirsute_hair`、`tresses_hair`、`lipoma_mass`、`harlequinn_grass`、`bloodyice`、gore 6 块（goresim/gorepri/goreada/gorepur/gorefer/goremar）

### 物品
- [x] `mobility_armor` 4 件套（ItemMobilityArmor，机动护甲）——2026-08-22 完成，材质 armor_mobility 数值还原，护甲层用香草皮革贴图
- [x] `greek_fire`（希腊火）——2026-08-22 完成，BFS 燃烧连片灌木（上限 12000）；目标灌木暂为 infestremain/residue_plants，灌木/血肉方块移植后需扩充 `GreekFireItem.isTargetBush`
- [x] `discthree` 唱片（音效事件已注册）——2026-08-22 完成，JukeboxSong 注册表 + 音符盒可播放
- [ ] `lurecomponent7-10`（低危，可并入诱饵迭代）

### 配方
- [x] 烧炼 4 条：bloody_rod→木棍、bloody_bone→骨粉、bloody_iron_ingot→铁锭、infested_cobblestone→infestedrubble —— 2026-08-22 全部完成

## 三、客户端

- [ ] 世界体积雾 SRPFogHandler/FogManager（现仅 2D 屏幕遮罩，氛围差距最大）
- [ ] BlackSky 黑天幕着色器（black_sky_darkness.fsh + BlackSkyClientEvents）
- [ ] SkyFlashRenderer 天空闪光（贴图已有）
- [ ] 补齐 8 种粒子：spore/rhappy/een/flash/dot/wind/rage/blood（现 5/13）
- [ ] fx 组：暴风雪 ClientExtremeSnow/ParticleBlizzard、感染落叶 ParticleInfestedLeaf、复仇粒子 ParticleVengeance
- [ ] GuiGameOverEscape 死亡界面 + 扭曲 HUD 三件套（DistortedGuiNewChat/SubtitleOverlay/DistortionGuiSwap）
- [ ] SOURCE / CLOUD_TOXIC 恢复可见渲染（原版 RenderSource/RenderTCloud）
- [ ] GuiVectorMapReport / GuiDislodgementReport 报告界面（服务端数据源已有）
- [ ] 护甲渲染层（SRPLayerBipedArmor / MobilityArmor 第一人称，接 HumanoidArmorLayer）
- [ ] 其余 overlay：AssimilatedPumpkin、DeadBlood 三件套、ClientQlipShake、ItemHighlight、GuiConsumedWorkbench、GuiSRPConfigEdit/View

## 四、世界生成与系统

- [ ] **极寒积雪系统**（ExtremeSnowServer/Data，整体缺失，寒星核心）
- [ ] **陨石坠落事件+撞击坑**（SRPConfigWorld.meteor* + MeteorCrash）
- [ ] **寄生植被生成器**：Tree/TreeThin/Ball/BigBall/Spine/高花/十瓣花（感染区无树，观感差距大）
- [ ] 寒星村庄补全：blacksmith/church/farm/中型房×2/小屋×2+战利品表
- [ ] 结构化 NodeCore（453 行）/ Mouth（305 行）/ NexusProtection1~3
- [ ] 逃脱系统（C2SRequestEscape/S2CSetEscapeOffer + WorldConfig.escape*）
- [ ] JEI 插件（InfuserFurnace + 酿造配方展示）
- [ ] 网络包补全：EvoPhase 双向、BiomeChangeBatch、VengeanceFX、GuiDistortion
- [ ] 配置扩充：逐生物属性、merge/status_effects 细项、celestial 概率外置
- [x] 寄生生物群系等效（biome tag + 迷雾密度/专属刷怪表）——2026-08-22 部分完成：4 个数据驱动群系 `srp_boils/demen/harlequinn/shrouded`（worldgen/biome JSON + 专属刷怪表），`ParasiteBiomeGenerator.applyParasiteBiome` 在感染 stage≥2 时按区块连片替换；仍缺迷雾密度/群系音效/地表特征
- 2026-08-22 其他完成：调度柱每 200t 感染脚下方块（原版 EntityAIBlockInfest）；`InfestationFlora` 感染成功后 1/12 顶部寄染草、1/5 悬垂草脉（原版 generateInfestationFeatures）；调度柱触须（dispatcherten/seizer/sentry/kyphosis）按原版 EntityPStationary 每 tick 水平回滚锚定，修复乱甩
- [ ] SRPExplosion 独立爆炸类（现由实体替代，按需）

## 五、已确认完整（无需再动）

实体注册 158/159、投射物 22/22、状态效果 37/37、药水 33/33 超集、音效 428/431、流体、图鉴（条目数超原版）、进化/难度/星穹之夜/增援/脱落/感染净化/存档、武器/工具/hijacked 物品、Overlast 食物超集、刷怪蛋全量等价。

## 移植批次建议

1. **批次1（快赢）**：烧炼配方、discthree、greek_fire、mobility_armor、kirin_slash+音效
2. **批次2**：残骸主方块 9+3+4+3 变种与衍生楼梯/台阶/墙（配合 ColonyStructureGenerator 换用新方块）
3. **批次3**：木系门/活板门/栅栏/工作台
4. **批次4**：极寒积雪 + 陨石 + 寄生植被生成
5. **批次5**：客户端氛围（体积雾、黑天幕、粒子、fx）
6. **批次6**：村庄/NodeCore/Mouth 结构、逃脱、JEI、网络包、配置扩充
