# R6 实体行为逐条核对矩阵（task-5）

> 生成方式：`out109`（SRParasites 1.10.9 / 1.12.2 Forge 反编译产物）逐文件比对
> `src/main/java/alku/csrp/entity/**` + `registry/ModEntities.java`。
> 事实来源只读目录：`D:\code\MC模组\_scratch\vf\out109\com\dhanantry\scapeandrunparasites\entity\**`。

状态取值：`已实现`（有语义等价实现，给出证据） / `不一致`（有对应物但语义有差异） / `缺失`（无对应实现） / `N/A`（26.3 无对应物，或 1.10.9 里是死代码）。

## 0. 统计

| 维度 | 数量 |
|---|---|
| out109 `entity/` java 文件总数 | 248 |
| out109 `entity/monster/**` java 文件 | 137 |
| out109 `entity/ai/*.java` | 39 |
| out109 `entity/ai/misc/*.java` | 34 |
| out109 AI 类合计（本矩阵逐条核对） | 73 |
| out109 `init/SRPEntities.java` 注册的实体 id | 158 |
| 本工程 `ModEntities.java` 注册的实体 id | 157 |
| 本工程 `entity/**` java 文件 | 125 |

AI 判定分布：

| 状态 | 条数 |
|---|---|
| 已实现 | 58 |
| 不一致 | 5 |
| 缺失 | 5 |
| N/A | 5 |

## 1. 实体 id 级 diff（原 158 vs 本工程 157）

### 1.1 原模组有、本工程无（13 个，全部为投射物）

这 13 个 id 在本工程被**有意合并**进单一实体 `csrp:parasite_projectile`（`entity/ParasiteProjectileEntity.java` 的 `Mode` 枚举）：

| 原 id | 原类 | 合并去向 |
|---|---|---|
| `ancientball` | `EntityProjectileAncientball` | Mode.ANCIENT_BALL |
| `ballball` | `EntityProjectileAngedball` | Mode.ANGED_BALL |
| `ballmall` | `EntityProjectileLenciaBall` | Mode.LENCIA_BALL |
| `balltall` | `EntityProjectileElviaBall` | Mode.ELVIA_BALL |
| `biomassball` | `EntityProjectileBiomass` | Mode.BIOMASS_BALL |
| `heblu_light` | `EntityProjectileHebluLight` | Mode.HEBLU_LIGHT |
| `meteor` | `EntityMeteor` | Mode.METEOR（另有独立 meteor_satellite） |
| `missile` | `EntityProjectileDragonE` | Mode.DRAGON_MISSILE |
| `nadeball` | `EntityProjectileNade` | Mode.NADE（NadeEntity.Kind.ACID） |
| `salivaball` | `EntityProjectileAlafhaBall` | Mode.ALAFHA_BALL |
| `salivaeff` | `EntityProjectileEffects` | Mode.EFFECTS |
| `spineball` | `EntityProjectileSpineball` | Mode.SPINEBALL |
| `webball` | `EntityProjectileWebball` | Mode.WEBBALL（webKind 0..2 对应三种网） |

### 1.2 本工程有、原模组无（12 个，均为本工程新增的辅助/拆分 id）

| 本工程 id | 本工程实现类 | 说明 |
|---|---|---|
| `crux_block_damage` | `undefined` | CruxA 投掷方块伤害的独立伤害源实体 |
| `dragon_egg_assimilation` | `undefined` | 龙蛋同化辅助实体 |
| `haunter_damage` | `undefined` | Haunter 伤害载体 |
| `haunter_homing` | `undefined` | Haunter 追踪弹（原 EntityProjectileHomming 的拆分） |
| `marauder_tendril` | `MarauderTendrilEntity` | Marauder 触手部件 |
| `meteor_satellite` | `undefined` | 陨星子体 |
| `parasite_projectile` | `undefined` | 22 个原投射物类的合并实体 |
| `pulling_ball` | `undefined` | 原 EntityProjectilePullball 的独立化 |
| `scary_orb` | `undefined` | 原 EntityOrbScary 的命名对齐 |
| `shockwave` | `undefined` | waveshock 的 legacy 别名 |
| `sim_dragonhead` | `AssimilatedDragonHeadEntity` | sim_dragonehead 的命名对齐 |
| `warden_waveshock` | `undefined` | Warden 冲击波独立实体 |

### 1.3 结论

- 实体 **id 层无真正缺失**：13 个缺口全部是有意的投射物合并，且都有明确的 `Mode` 去向。
- 本工程多出 12 个 id，均为部件/伤害载体/命名对齐，不影响原 id 的可用性。
- `registry/ModEntities.java` 无需为 R6 增删 id。

## 2. 实体类矩阵（out109 `entity/monster/**`，137 个文件）

映射规则：out109 类 → `init/SRPEntities.java` 里的注册 id → 本工程 `ModEntities.java` 里该 id 的实现类。
本工程采用「少数大类 + `Kind` 枚举」的参数化架构，因此多个原类会指向同一个本工程类，这是**有意的合并**，不是缺失。

| out109 文件 | 原类 | 注册 id | 本工程实现 |
|---|---|---|---|
| `monster/EntityBiomass.java` | `EntityBiomass` | `biomass` | `BiomassEntity` |
| `monster/EntityTendril.java` | `EntityTendril` | `tendril` | — |
| `monster/EntityWave.java` | `EntityWave` | `wave` | — |
| `monster/EntityWaveShock.java` | `EntityWaveShock` | `waveshock` | — |
| `monster/abomination/EntityAboBodies.java` | `EntityAboBodies` | `abo_bodies` | `AbominationEntity` |
| `monster/abomination/EntityAboHead.java` | `EntityAboHead` | `abo_head` | `AbominationEntity` |
| `monster/adapted/EntityBanoAdapted.java` | `EntityBanoAdapted` | `ada_bolster` | `AdaptedVariantEntity` |
| `monster/adapted/EntityCanraAdapted.java` | `EntityCanraAdapted` | `ada_summoner` | `AdaptedVariantEntity` |
| `monster/adapted/EntityEmanaAdapted.java` | `EntityEmanaAdapted` | `ada_yelloweye` | `AdaptedVariantEntity` |
| `monster/adapted/EntityGimAdapted.java` | `EntityGimAdapted` | `ada_viscera` | `AdaptedVariantEntity` |
| `monster/adapted/EntityHullAdapted.java` | `EntityHullAdapted` | `ada_manducater` | `AdaptedVariantEntity` |
| `monster/adapted/EntityIkiAdapted.java` | `EntityIkiAdapted` | `ada_vermin` | `AdaptedVariantEntity` |
| `monster/adapted/EntityLumAdapted.java` | `EntityLumAdapted` | `ada_devourer` | `AdaptedVariantEntity` |
| `monster/adapted/EntityNoglaAdapted.java` | `EntityNoglaAdapted` | `ada_reeker` | `AdaptedVariantEntity` |
| `monster/adapted/EntityRanracAdapted.java` | `EntityRanracAdapted` | `ada_arachnida` | `AdaptedVariantEntity` |
| `monster/adapted/EntityShycoAdapted.java` | `EntityShycoAdapted` | `ada_longarms` | `AdaptedVariantEntity` |
| `monster/adapted/EntityWymoAdapted.java` | `EntityWymoAdapted` | `ada_tozoon` | `AdaptedVariantEntity` |
| `monster/adapted/EntityZaaAdapted.java` | `EntityZaaAdapted` | `ada_burrower` | `AdaptedVariantEntity` |
| `monster/ancient/EntityDharma.java` | `EntityDharma` | **未注册（死类）** | — |
| `monster/ancient/EntityOronco.java` | `EntityOronco` | `anc_dreadnaut` | `AncientParasiteEntity` |
| `monster/ancient/EntityOroncoTen.java` | `EntityOroncoTen` | `anc_dreadnaut_ten` | `DreadnautTentacleEntity` |
| `monster/ancient/EntityTerla.java` | `EntityTerla` | `anc_overlord` | `AncientParasiteEntity` |
| `monster/awakened/EntityOroncoAW.java` | `EntityOroncoAW` | **未注册（死类）** | — |
| `monster/crude/EntityCruxA.java` | `EntityCruxA` | `crux` | `CruxEntity` |
| `monster/crude/EntityCruxB.java` | `EntityCruxB` | `crux_incomplete` | `IncompleteCruxEntity` |
| `monster/crude/EntityDone.java` | `EntityDone` | `dredge` | `DredgeEntity` |
| `monster/crude/EntityHeed.java` | `EntityHeed` | `heed` | `HeedEntity` |
| `monster/crude/EntityHost.java` | `EntityHost` | `host` | `HostEntity` |
| `monster/crude/EntityHostII.java` | `EntityHostII` | `hostii` | `HostIIEntity` |
| `monster/crude/EntityInhooM.java` | `EntityInhooM` | `incompleteform_medium` | `IncompleteFormMediumEntity` |
| `monster/crude/EntityInhooS.java` | `EntityInhooS` | `incompleteform_small` | `IncompleteFormSmallEntity` |
| `monster/crude/EntityLeer.java` | `EntityLeer` | `airscrew` | `AirscrewEntity` |
| `monster/crude/EntityLesh.java` | `EntityLesh` | `movingflesh` | `MovingFleshEntity` |
| `monster/crude/EntityMes.java` | `EntityMes` | `thrall` | `ThrallEntity` |
| `monster/crude/EntityQuac.java` | `EntityQuac` | `carrier_worm` | `CarrierWormEntity` |
| `monster/derived/EntityHeblu.java` | `EntityHeblu` | `draconite` | `DraconiteEntity` |
| `monster/derived/EntityKirin.java` | `EntityKirin` | `kirin` | `KirinEntity` |
| `monster/deterrent/EntityDodT.java` | `EntityDodT` | `dispatcherten` | `DeterrentParasiteEntity` |
| `monster/deterrent/EntityLeemB.java` | `EntityLeemB` | `rooterball` | `NexusParasiteEntity` |
| `monster/deterrent/EntityNak.java` | `EntityNak` | `seizer` | `DeterrentParasiteEntity` |
| `monster/deterrent/EntityRof.java` | `EntityRof` | `worm` | `DeterrentParasiteEntity` |
| `monster/deterrent/EntityTonro.java` | `EntityTonro` | `kyphosis` | `DeterrentParasiteEntity` |
| `monster/deterrent/EntityUnvo.java` | `EntityUnvo` | `sentry` | `DeterrentParasiteEntity` |
| `monster/deterrent/nexus/EntityDod.java` | `EntityDod` | `dispatcher_si` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityDodSII.java` | `EntityDodSII` | `dispatcher_sii` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityDodSIII.java` | `EntityDodSIII` | `dispatcher_siii` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityDodSIV.java` | `EntityDodSIV` | `dispatcher_siv` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityLeem.java` | `EntityLeem` | `rooter_si` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityLeemSII.java` | `EntityLeemSII` | `rooter_sii` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityLeemSIII.java` | `EntityLeemSIII` | `rooter_siii` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityLeemSIV.java` | `EntityLeemSIV` | `rooter_siv` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityVenkrol.java` | `EntityVenkrol` | `beckon_si` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityVenkrolSII.java` | `EntityVenkrolSII` | `beckon_sii` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityVenkrolSIII.java` | `EntityVenkrolSIII` | `beckon_siii` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityVenkrolSIV.java` | `EntityVenkrolSIV` | `beckon_siv` | `NexusParasiteEntity` |
| `monster/deterrent/nexus/EntityVenkrolSV.java` | `EntityVenkrolSV` | **未注册（死类）** | — |
| `monster/feral/EntityFerBear.java` | `EntityFerBear` | `fer_bear` | `FeralParasiteEntity` |
| `monster/feral/EntityFerCow.java` | `EntityFerCow` | `fer_cow` | `FeralParasiteEntity` |
| `monster/feral/EntityFerEnderman.java` | `EntityFerEnderman` | `fer_enderman` | `FeralEndermanEntity` |
| `monster/feral/EntityFerHorse.java` | `EntityFerHorse` | `fer_horse` | `FeralParasiteEntity` |
| `monster/feral/EntityFerHuman.java` | `EntityFerHuman` | `fer_human` | `FeralParasiteEntity` |
| `monster/feral/EntityFerPig.java` | `EntityFerPig` | `fer_pig` | `FeralParasiteEntity` |
| `monster/feral/EntityFerSheep.java` | `EntityFerSheep` | `fer_sheep` | `FeralParasiteEntity` |
| `monster/feral/EntityFerVillager.java` | `EntityFerVillager` | `fer_villager` | `FeralParasiteEntity` |
| `monster/feral/EntityFerWolf.java` | `EntityFerWolf` | `fer_wolf` | `FeralParasiteEntity` |
| `monster/focused/EntityBanoFocused.java` | `EntityBanoFocused` | **未注册（死类）** | — |
| `monster/focused/EntityShycoFocused.java` | `EntityShycoFocused` | **未注册（死类）** | — |
| `monster/hijacked/EntityHiBlaze.java` | `EntityHiBlaze` | `hi_blaze` | `HiBlazeEntity` |
| `monster/hijacked/EntityHiGolem.java` | `EntityHiGolem` | `hi_golem` | `HiGolemEntity` |
| `monster/hijacked/EntityHiSkeleton.java` | `EntityHiSkeleton` | `hi_skeleton` | `HiSkeletonEntity` |
| `monster/inborn/EntityAta.java` | `EntityAta` | `gnat` | `GnatEntity` |
| `monster/inborn/EntityButhol.java` | `EntityButhol` | `carrier_flying` | `CarrierFlyingEntity` |
| `monster/inborn/EntityGothol.java` | `EntityGothol` | `carrier_light` | `CarrierLightEntity` |
| `monster/inborn/EntityKol.java` | `EntityKol` | `worker` | `WorkerEntity` |
| `monster/inborn/EntityLodo.java` | `EntityLodo` | `buglin` | `BuglinEntity` |
| `monster/inborn/EntityMor.java` | `EntityMor` | **未注册（死类）** | — |
| `monster/inborn/EntityMudo.java` | `EntityMudo` | `rupter` | `RupterEntity` |
| `monster/inborn/EntityNuuh.java` | `EntityNuuh` | `mangler` | `ManglerEntity` |
| `monster/inborn/EntityRathol.java` | `EntityRathol` | `carrier_heavy` | `CarrierHeavyEntity` |
| `monster/inborn/EntityViin.java` | `EntityViin` | `lice` | `LiceEntity` |
| `monster/infected/EntityDorpa.java` | `EntityDorpa` | `sim_bigspider` | `AssimilatedVariantEntity` |
| `monster/infected/EntityInfBear.java` | `EntityInfBear` | `sim_bear` | `AssimilatedParasiteEntity` |
| `monster/infected/EntityInfCow.java` | `EntityInfCow` | `sim_cow` | `AssimilatedParasiteEntity` |
| `monster/infected/EntityInfDragonE.java` | `EntityInfDragonE` | `sim_dragone` | `AssimilatedDragonEntity` |
| `monster/infected/EntityInfEnderman.java` | `EntityInfEnderman` | `sim_enderman` | `AssimilatedEndermanEntity` |
| `monster/infected/EntityInfHorse.java` | `EntityInfHorse` | `sim_horse` | `AssimilatedVariantEntity` |
| `monster/infected/EntityInfHuman.java` | `EntityInfHuman` | `sim_human` | `SimHumanEntity` |
| `monster/infected/EntityInfPig.java` | `EntityInfPig` | `sim_pig` | `AssimilatedParasiteEntity` |
| `monster/infected/EntityInfPlayer.java` | `EntityInfPlayer` | `sim_adventurer` | `SimAdventurerEntity` |
| `monster/infected/EntityInfSheep.java` | `EntityInfSheep` | `sim_sheep` | `AssimilatedParasiteEntity` |
| `monster/infected/EntityInfSquid.java` | `EntityInfSquid` | `sim_squid` | `AssimilatedParasiteEntity` |
| `monster/infected/EntityInfVillager.java` | `EntityInfVillager` | `sim_villager` | `AssimilatedVariantEntity` |
| `monster/infected/EntityInfWolf.java` | `EntityInfWolf` | `sim_wolf` | `AssimilatedParasiteEntity` |
| `monster/infected/head/EntityInfCowHead.java` | `EntityInfCowHead` | `sim_cowhead` | `AssimilatedHeadEntity` |
| `monster/infected/head/EntityInfDragonEHead.java` | `EntityInfDragonEHead` | `sim_dragonehead` | `AssimilatedDragonHeadEntity` |
| `monster/infected/head/EntityInfEndermanHead.java` | `EntityInfEndermanHead` | `sim_endermanhead` | `AssimilatedHeadEntity` |
| `monster/infected/head/EntityInfHorseHead.java` | `EntityInfHorseHead` | `sim_horsehead` | `AssimilatedHeadEntity` |
| `monster/infected/head/EntityInfHumanHead.java` | `EntityInfHumanHead` | `sim_humanhead` | `AssimilatedHeadEntity` |
| `monster/infected/head/EntityInfPigHead.java` | `EntityInfPigHead` | `sim_pighead` | `AssimilatedHeadEntity` |
| `monster/infected/head/EntityInfPlayerHead.java` | `EntityInfPlayerHead` | `sim_adventurerhead` | `SimAdventurerHeadEntity` |
| `monster/infected/head/EntityInfSheepHead.java` | `EntityInfSheepHead` | `sim_sheephead` | `AssimilatedHeadEntity` |
| `monster/infected/head/EntityInfVillagerHead.java` | `EntityInfVillagerHead` | `sim_villagerhead` | `AssimilatedHeadEntity` |
| `monster/infected/head/EntityInfWolfHead.java` | `EntityInfWolfHead` | `sim_wolfhead` | `AssimilatedHeadEntity` |
| `monster/infected/special/EntitySpeBear.java` | `EntitySpeBear` | `mar_bear` | `MarauderizedBearEntity` |
| `monster/infected/special/EntitySpeCow.java` | `EntitySpeCow` | `mar_cow` | `MarauderizedCowEntity` |
| `monster/infected/special/EntitySpeEnderman.java` | `EntitySpeEnderman` | `mar_enderman` | `MarauderizedEndermanEntity` |
| `monster/infected/special/EntitySpeHuman.java` | `EntitySpeHuman` | `mar_human` | `MarauderizedHumanEntity` |
| `monster/infected/special/EntitySpeSheep.java` | `EntitySpeSheep` | `mar_sheep` | `MarauderizedSheepEntity` |
| `monster/infected/special/EntitySpeVillager.java` | `EntitySpeVillager` | `mar_villager` | `MarauderizedVillagerEntity` |
| `monster/primitive/EntityBano.java` | `EntityBano` | `pri_bolster` | `PrimitiveVariantEntity` |
| `monster/primitive/EntityCanra.java` | `EntityCanra` | `pri_summoner` | `SummonerEntity` |
| `monster/primitive/EntityEmana.java` | `EntityEmana` | `pri_yelloweye` | `PrimitiveVariantEntity` |
| `monster/primitive/EntityGim.java` | `EntityGim` | `pri_viscera` | `VisceraEntity` |
| `monster/primitive/EntityHull.java` | `EntityHull` | `pri_manducater` | `PrimitiveVariantEntity` |
| `monster/primitive/EntityIki.java` | `EntityIki` | `pri_vermin` | `VerminEntity` |
| `monster/primitive/EntityLum.java` | `EntityLum` | `pri_devourer` | `PrimitiveVariantEntity` |
| `monster/primitive/EntityNogla.java` | `EntityNogla` | `pri_reeker` | `PrimitiveVariantEntity` |
| `monster/primitive/EntityRanrac.java` | `EntityRanrac` | `pri_arachnida` | `PrimitiveVariantEntity` |
| `monster/primitive/EntityShyco.java` | `EntityShyco` | `pri_longarms` | `LongarmsEntity` |
| `monster/primitive/EntityWymo.java` | `EntityWymo` | `pri_tozoon` | `PrimitiveVariantEntity` |
| `monster/primitive/EntityZaa.java` | `EntityZaa` | `pri_burrower` | `PrimitiveVariantEntity` |
| `monster/pure/EntityAlafha.java` | `EntityAlafha` | `overseer` | `PureParasiteEntity` |
| `monster/pure/EntityAnged.java` | `EntityAnged` | `vigilante` | `PureParasiteEntity` |
| `monster/pure/EntityEsor.java` | `EntityEsor` | `marauder` | `MarauderEntity` |
| `monster/pure/EntityFlog.java` | `EntityFlog` | `grunt` | `PureParasiteEntity` |
| `monster/pure/EntityGanro.java` | `EntityGanro` | `warden` | `PureParasiteEntity` |
| `monster/pure/EntityOmboo.java` | `EntityOmboo` | `bomber_light` | `PureParasiteEntity` |
| `monster/pure/EntityOrch.java` | `EntityOrch` | `monarch` | `PureParasiteEntity` |
| `monster/pure/EntityRond.java` | `EntityRond` | **未注册（死类）** | — |
| `monster/pure/EntitySoo.java` | `EntitySoo` | `seeker` | `PureParasiteEntity` |
| `monster/pure/preeminent/EntityElvia.java` | `EntityElvia` | `wraith` | `PreeminentParasiteEntity` |
| `monster/pure/preeminent/EntityFlam.java` | `EntityFlam` | `succor` | `FlamEntity` |
| `monster/pure/preeminent/EntityJinjo.java` | `EntityJinjo` | `bomber_heavy` | `PreeminentParasiteEntity` |
| `monster/pure/preeminent/EntityLencia.java` | `EntityLencia` | `bogle` | `PreeminentParasiteEntity` |
| `monster/pure/preeminent/EntityPheon.java` | `EntityPheon` | `haunter` | `PreeminentParasiteEntity` |
| `monster/pure/preeminent/EntityTenn.java` | `EntityTenn` | `architect` | `ArchitectEntity` |
| `monster/pure/preeminent/EntityVesta.java` | `EntityVesta` | `carrier_colony` | `PreeminentParasiteEntity` |

其中 **7 个原类在 1.10.9 里既未注册也未被任何代码引用（死类）**：

`EntityDharma`、`EntityOroncoAW`、`EntityVenkrolSV`、`EntityMor`、`EntityRond`、`EntityBanoFocused`、`EntityShycoFocused`

这 7 个类在本工程**有意不移植**，理由：无注册 id、无生成途径、无引用点，移植后不可达。
连带 `EntityPFocused`（只被 `EntityBanoFocused`/`EntityShycoFocused` 继承）也一并记为 N/A。

## 3. AI 类矩阵（out109 `entity/ai/**`，73 个文件逐条核对）

| # | out109 文件 | 行数 | 核心语义 | 状态 | 本工程证据 / 理由 |
|---|---|---|---|---|---|
| 1 | `ai/AIDisableBeaconIki.java` | 416 | Iki 搬运并停用信标 | **N/A** | out109 全树无 `new AIDisableBeaconIki`（唯一引用是自身文件）<br>死代码：1.10.9 未把它挂到任何实体上，移植时同样不挂。26.3 若要做等价行为，用 `MoveToBlockGoal` + `BlockEntity` 检测，但无事实来源可依。 |
| 2 | `ai/EntityAIAncientSummon.java` | 81 | 古生代实体召唤 ancientpod | **已实现** | entity/AncientParasiteEntity.java:597 `DreadPodGoal`；entity/AncientPodEntity.java<br>原类挂在 EntityOronco / EntityDodSIV 上，本工程分别落在 AncientParasiteEntity 的 ORONCO Kind 与 NexusParasiteEntity 的 DISPATCHER_SIV。 |
| 3 | `ai/EntityAIAttackMeleeNotGround.java` | 93 | 空中近战（目标上方 off 格） | **已实现** | entity/ArchitectEntity.java:378 `FlightAttackGoal`；entity/LiceEntity.java:247；entity/AdaptedVariantEntity.java:3022 `VerminFlightAttackGoal`<br>原类用 `getMoveHelper().setMoveTo(target.y+off)` 做空中贴近；本工程 FlightAttackGoal 用 `getMoveControl().setWantedPosition(target.y+eyeHeight/2)` 等价。 |
| 4 | `ai/EntityAIAttackMeleeRangeSwitch.java` | 52 | geneSpecialmove 下近战/远程切换 | **已实现** | entity/PureParasiteEntity.java `VigilanteRangeSwitchGoal`；entity/PreeminentParasiteEntity.java `HaunterMeleeRangeSwitchGoal`<br>原类 `parent.getGeneMod(5)` 是 geneSpecialmove（EntityParasiteBase:604，默认 true），本工程对应各 Kind 的 rangeSwitch 分支。 |
| 5 | `ai/EntityAIAttackMeleeRanged.java` | 68 | 近战+保持远程站位 | **已实现** | entity/PreeminentParasiteEntity.java `HaunterRangedPositionGoal` |
| 6 | `ai/EntityAIAttackMeleeStatus.java` | 204 | 带 status/攻击冷却动画的核心近战 | **已实现** | 40 个实体类用 `MeleeAttackGoal`（AbominationEntity/AdaptedVariantEntity/PureParasiteEntity/…）；status 由 `setParasiteStatus`+`ParasiteAnimations` 承担<br>原类里的 `getAttackCooldownAni/setAttackCooldownAni` 在 26.3 由 `Mob.attackAnim` + `ParasiteAnimations` 的 status 轨道替代。 |
| 7 | `ai/EntityAIAttackMeleeStatusAOE.java` | 40 | 范围近战（矩形 AOE） | **已实现** | entity/PureParasiteEntity.java `MonarchAreaMeleeGoal`/`WardenAreaMeleeGoal`；entity/PreeminentParasiteEntity.java `HaunterMeleeAoeGoal`/`GruntAreaMeleeGoal`；entity/LongarmsEntity.java；entity/CruxEntity.java |
| 8 | `ai/EntityAIAttackProjectile.java` | 109 | EntityCanShoot 投射物攻击 | **已实现** | entity/PreeminentParasiteEntity.java `LegacyProjectileAttackGoal`；entity/ParasiteProjectileEntity.java<br>22 个原投射物类合并为 parasite_projectile 的 Mode 枚举，攻击 AI 用 LegacyProjectileAttackGoal 发射。 |
| 9 | `ai/EntityAIAttackRangedStatus.java` | 136 | 带 status 的远程攻击（初速 0.08） | **已实现** | entity/ParasiteProjectileEntity.java:790 `fireProjectile`；entity/PureParasiteEntity.java `YelloweyeRangedGoal`/`VigilanteRangedGoal`；entity/DeterrentParasiteEntity.java `SentrySpineGoal` |
| 10 | `ai/EntityAIAttackSwell.java` | 49 | 膨胀自爆（Carrier） | **已实现** | entity/CarrierEntity.java:417 `SwellGoal` |
| 11 | `ai/EntityAIAttackVenkrol.java` | 143 | Venkrol 近战（attackInterval=20） | **已实现** | entity/NexusParasiteEntity.java:163 `tick()` 家族能力 + `spawnBombVolley`/`spawnPodVolley`；BECKON family 的 status 切换在 tick():172<br>原类是本工程 BECKON 家族的近战/技能循环；本工程改为 tick 驱动的家族能力（间隔由 `Kind.summonCooldown` 控制），语义等价但结构不同。 |
| 12 | `ai/EntityAIAvoidEntityStatus.java` | 96 | 躲避指定实体类 | **已实现** | entity/AssimilatedHeadEntity.java；entity/BuglinEntity.java；entity/MovingFleshEntity.java（`AvoidEntity` 目标） |
| 13 | `ai/EntityAIAvoidOrAttack.java` | 77 | 按距离躲避或攻击 | **已实现** | entity/RupterEntity.java:1 文件内 `AvoidOrAttack` 分支 |
| 14 | `ai/EntityAIBlockInfest.java` | 39 | 周期性感染脚下方块 | **已实现** | entity/NexusParasiteEntity.java:222（BECKON，间隔 `max(20, 100-stage*15)`）与 :229（DISPATCHER，200t）；block/BlockInfestation.infestAround<br>原类间隔见 EntityVenkrol/SII/SIII；本工程按家族分别定节奏。 |
| 15 | `ai/EntityAIBlockLight.java` | 211 | gene 7 寄生虫破坏光源方块 | **缺失** | 无对应实现（grep `BlockLight` 在 src/main/java 无命中）<br>原类由 EntityPMalleable:92 以 `(this, 20, 5)` 挂在优先级 7；`getGeneMod(7)`=geneBlocksearch（EntityPMalleable:110，默认 true），所以对 malleable 寄生虫是**活行为**。规格：无攻击目标且 mobGriefing 时每 40t 扫描 20×4×20，取 `getLightEmission()>=5` 或发光材质方块，距离>5 走 `getNavigation().moveTo(pos,1.1)`，距离<=5 按 `destroySpeed*10` 进度破坏，破坏后 `ticks+=30`、同一方块 240t 冷却。→ 待补（第 4 批）。 |
| 16 | `ai/EntityAIBlockResidue.java` | 84 | gene 8 寄生虫把周围方块转成残留物 | **不一致** | entity/AdaptedVariantEntity.java:1424-1470 `spreadBolsterResidue`/`placeBolsterResidue`（仅 BOLSTER 一个 Kind，冷却 600+rand(601)）<br>原类挂在 9 个类上（EntityBanoAdapted/CanraAdapted/GimAdapted/HullAdapted/NoglaAdapted/RanracAdapted/ShycoAdapted + 2 个死类 Focused），触发条件 `无目标 && !isInWater && geneResidue`，节奏 160/0.2≈800t，status 25，半径=构造参数 range，把方块换成 infested residue。本工程只覆盖 bolster，其余 6 个 adapted Kind 缺。→ 待补（第 4 批）。 |
| 17 | `ai/EntityAIDiveBomb.java` | 250 | 俯冲轰炸（升空 20 / 步长 0.6 / 120t） | **已实现** | entity/PrimitiveVariantEntity.java:2213 `RicardoDiveBombGoal`<br>常量逐一对应：ASCEND_HEIGHT=20.0、ASCEND_STEP_MAX=0.6、MAX_ASCEND_TICKS=120、俯冲初速 2.8、加速度 0.35*diveTicks。 |
| 18 | `ai/EntityAIDodAttack.java` | 513 | Dod/Dispatcher 存储寄生虫、爆弹、破方块 | **不一致** | entity/NexusParasiteEntity.java:482 `storeNearbyParasite`、:770 `spawnBombVolley`、:780 `spawnPodVolley`、:862 `breakBlocksTowardsTarget`、:808 `spawnRootmassCysts`<br>本工程按家族拆分实现，未逐条复刻原类的 `mobattackingBlackList`/`worldMobCap` 过滤与 513 行里的分阶段脚本。差异点：原类的黑名单配置过滤（SRPConfig.mobattackingBlackList / …White）在本工程未接入。→ 记为不一致，低优先。 |
| 19 | `ai/EntityAIEvade.java` | 120 | 受击后跳跃闪避 | **已实现** | entity/AdaptedVariantEntity.java `EvadeGoal`；entity/PrimitiveVariantEntity.java；entity/VisceraEntity.java<br>原类 `getGeneMod(5)`=geneSpecialmove 门控；本工程按 Kind 挂载。 |
| 20 | `ai/EntityAIEvadeDash.java` | 91 | 冲刺闪避 | **已实现** | entity/ManglerEntity.java `EvasiveDashGoal`；entity/PureParasiteEntity.java `MonarchEvasiveDashGoal`/`GruntEvasiveDashGoal`；entity/PreeminentParasiteEntity.java `HaunterEvadeDashGoal` |
| 21 | `ai/EntityAIEvadeTP.java` | 98 | 闪现闪避 | **N/A** | out109 全树无 `new EntityAIEvadeTP`（唯一引用是自身文件）<br>死代码：1.10.9 未挂载。本工程已有 `KirinBlinkGoal`（EntityAIKirinBlink 的活实现）承担瞬移语义，不再单独补。 |
| 22 | `ai/EntityAIFlightAttack.java` | 143 | 飞行近战攻击 | **已实现** | entity/ArchitectEntity.java:378 `FlightAttackGoal`；entity/LiceEntity.java:247；entity/CarrierFlyingEntity.java:135 `FlyingCombatGoal` |
| 23 | `ai/EntityAIFlightLimits.java` | 62 | 飞行高度上下限（±0.04/刻） | **已实现** | entity/VerminEntity.java:409 `FlightHeightLimitGoal`；entity/PureParasiteEntity.java:2545 `OmbooFlightLimitsGoal`、:2821 `OverseerFlightLimitGoal`<br>原类 `howMuchNeg/howMuchPos` 数空气格决定压升/压降；本工程三个 Goal 分别对应不同家族。 |
| 24 | `ai/EntityAIFollowBodies.java` | 190 | Wymo/Zaa/Quac 跟随母体 + 掘地传送 | **缺失** | 无对应实现（grep `FollowBodies` 无命中）<br>原类挂在 pri_tozoon(EntityWymo)、ada_tozoon(EntityWymoAdapted)、pri_burrower(EntityZaa)、ada_burrower(EntityZaaAdapted)、carrier_worm(EntityQuac) 上。规格：跟随 `getFollowing()` 的 UUID，距离>`push` 时每刻加 0.19 速度分量；`dis>9` 直接 snap；掘地态 speed=0、status=-5、`getBodyLength()+2` 刻后调用 teleportDigging（10 格、4 次尝试）；击杀数 > evolutionNeed 且 mobEvolution 时调用 spawnNext。→ 待补（第 4 批）。 |
| 25 | `ai/EntityAIGetFollowers.java` | 96 | 招募跟随者 | **已实现** | entity/PrimitiveVariantEntity.java `RecruitFollowersGoal`；entity/HeedEntity.java；entity/DredgeEntity.java；entity/AdaptedVariantEntity.java `ReekerRecruitFollowersGoal` |
| 26 | `ai/EntityAIGiveEffectsArea.java` | 56 | 范围给状态效果 | **已实现** | entity/CarrierEntity.java `CarrierBuffGoal`；entity/AssimilatedHeadEntity.java:325 `HeadCothCloudGoal`；entity/AdaptedVariantEntity.java `BolsterSupportGoal` |
| 27 | `ai/EntityAIInfectedSearch.java` | 122 | 搜索被感染体以融解合体 | **已实现** | entity/AssimilatedMeltSystem.java:16 文档明写 "Shared EntityAIInfectedSearch and EntityCanMelt behavior"；`tryStartGroup`<br>原类挂在 EntityPInfected 上；本工程由 AssimilatedMeltSystem 统一承担（KILL_THRESHOLD=10、需 3 个同族、最低 phase 1）。 |
| 28 | `ai/EntityAIKirinBlink.java` | 283 | Kirin 蓄力闪现（60t 蓄力 / 200t 冷却 / 16 格起） | **已实现** | entity/KirinEntity.java `KirinBlinkGoal`<br>常量对应：`distanceToSqr(target) <= 256.0D` 即原类 MIN_FAR_DIST_SQ=256.0；BLINK_CHARGE_TICKS 对应 CHARGE_TIME=60；冷却 200t。 |
| 29 | `ai/EntityAINearestAttackableTargetStatus.java` | 306 | 带 gene 2 视线豁免的目标选择 | **已实现** | 32 个实体类使用 `NearestAttackableTargetGoal`<br>原类 `getGeneMod(2)`=geneLookwall（穿透视线）；本工程用 26.3 的 `NearestAttackableTargetGoal` 构造参数（mustSee / 目标谓词）表达同一语义。 |
| 30 | `ai/EntityAINexusGrow.java` | 456 | 节点成长（阶段升级 + 召唤 rooter_si） | **不一致** | entity/NexusParasiteEntity.java:189-199（成长计时 + phase 门槛 + `evolve()`:891）、:229（DISPATCHER 感染）、:847（BECKON 风暴）、:252（首次殖民地）<br>已实现阶段升级（每阶段需要 `SrpWorldData.evolutionPhase() >= stage+2`，mobEvolution 门控已加）。缺：原类 `spawnLeem()` 的 2% 概率召唤 `srparasites:rooter_si`，受 `SRPConfig.nexusLeemCap` 与 `nexusLeemDis` 限制，以及 `SRPConfigSystems.evolutionNests/deveNestsUse` 与 `maximumStageList` 的维度上限锁。→ 待补（第 4 批）。 |
| 31 | `ai/EntityAINexusNest.java` | 21 | 节点巢穴 | **N/A** | out109 全树无 `new EntityAINexusNest`（唯一引用是自身文件）<br>死代码：1.10.9 未挂载；本工程用 `NexusParasiteEntity.placeNestFog`:503 承担巢穴雾语义。 |
| 32 | `ai/EntityAIParasiteFollow.java` | 84 | 跟随领袖寄生虫 | **已实现** | entity/ParasiteFollowGoal.java（27 个实体类使用） |
| 33 | `ai/EntityAISkill.java` | 92 | 技能跳跃 | **已实现** | entity/ManglerEntity.java:432 `SkillLeapGoal`；entity/PureParasiteEntity.java `MonarchSkillLeapGoal`/`GruntSkillLeapGoal`/`RageSkillGoal`；entity/PrimitiveVariantEntity.java<br>原类 `getGeneMod(5)` 门控。 |
| 34 | `ai/EntityAISoundEaterStalk.java` | 141 | SoundEater 潜行 | **缺失** | 无对应实现（grep `SoundEater`/`stalk` 在 src/main/java 无命中）<br>**注意：这个 AI 类本身是死代码**（out109 全树无 `new EntityAISoundEaterStalk`），真正的活行为写在 `EntityInfHuman` 里（skin==111）并被 `events/SoundEaterBlockSoundHandler`、`SoundEaterArrowImpactHandler` 驱动。本工程 `SimHumanEntity` 没有 skin 系统也没有声音记忆，所以**活行为缺失**。规格见下方 SoundEaterSoundHelper 行。→ 待补（第 3 批）。 |
| 35 | `ai/EntityAISwimmingDiving.java` | 47 | 水中下潜 | **已实现** | entity/HeedEntity.java、entity/ManglerEntity.java、entity/GnatEntity.java 等 7 个文件的 `SwimmingDivingGoal` |
| 36 | `ai/EntityAIVenkrolSummon.java` | 451 | Venkrol 按 mob 表召唤 + 上限 | **不一致** | entity/NexusParasiteEntity.java `summonBeckonParasites`/`summonDispatcherDefenses`（由 `performFamilyAbility` 调度，见 :163 tick 与家族 switch）<br>已实现召唤本体与冷却（`Kind.summonCooldown`）。缺：原类依赖 `SRPAttributes.VENKROL_MOBTABLEG`（地面表）/`…A`（空中表）+ `SRPConfigMobs` 的 useEvolution/rsIgnoreCooldownAtSpawn 开关，并做 `getTotalParasites()`/`getActualParasites()` 上限校验。本工程用 `SummonCapacityOwner` 的容量模型替代，上限语义等价但 mob 表未逐条复刻。→ 记为不一致。 |
| 37 | `ai/EntityAIWanderStatus.java` | 107 | 带 status 的游荡 | **已实现** | 24 个实体类的 `RandomMoveGoal` / `WaterAvoidingRandomStrollGoal` |
| 38 | `ai/EntityAIWaterLeapAtTargetStatus.java` | 99 | 朝目标水中跃出 | **已实现** | entity/AdaptedVariantEntity.java `ArachnidaWaterLeapGoal`/`WaterPursuitLeapGoal`；entity/HeedEntity.java `WaterLeapGoal`；entity/PreeminentParasiteEntity.java；entity/ManglerEntity.java<br>原类挂在 32 个类上；本工程按 Kind 分散实现。 |
| 39 | `ai/SoundEaterSoundHelper.java` | 20 | 广播方块破坏/放置声给 SoundEater | **缺失** | 无对应实现（`SimHumanEntity.java` 无 `soundMemory`/`notifyHeardSound`）<br>活行为事实来源：`EntityInfHuman:151-188` + `events/SoundEaterBlockSoundHandler` + `SoundEaterArrowImpactHandler`。规格：spawn 时 1% 概率 skin=111，FOLLOW_RANGE=12、速度=0.32；每刻 tickSoundMemory；无记忆时扫 16×4×16 的非旁观/非创造/存活玩家，loudness=`walkedThisTick`（疾跑 ×3），取最大者 `notifyHeardSound(pos,60)` 并 setTarget；无记忆且无 revenge 目标时清目标；方块破坏广播半径 16/寿命 100，放置半径 12/寿命 80。→ 待补（第 3 批）。 |
| 40 | `ai/misc/EntityAICircleGroup.java` | 311 | sim_human 围绕目标绕圈编队 | **缺失** | 无对应实现（grep `CircleGroup` 无命中）<br>原类只被 `EntityInfHuman` 使用（sim_human）。本工程 `SimHumanEntity.java:115-122` 只有 Float/Leap/Melee/Stroll/ParasiteFollow/RandomLookAround。→ 待补（第 4 批）。 |
| 41 | `ai/misc/EntityBodyParts.java` | 9 | 多部件受击接口 | **已实现** | entity/AssimilatedDragonHeadEntity.java、entity/AssimilatedHeadEntity.java、entity/SimAdventurerHeadEntity.java、entity/AbominationEntity.java、entity/DreadnautTentacleEntity.java、entity/MarauderTendrilEntity.java<br>26.3 用独立的 head/part 实体 + `getRootVehicle`/`startRiding` 或直接的 owner 引用替代 1.12.2 的 `attackEntityBodyFrom`。 |
| 42 | `ai/misc/EntityCanClimb.java` | 5 | 可攀爬标记 | **已实现** | 14 个实体类含 `Climb` 分支（AdaLonglegEntity/AdaScuttlerEntity/AdaptedVariantEntity/…）<br>26.3 无 `isOnLadder` 覆写的等价标记接口，本工程用 Kind 分支直接实现攀爬位移。 |
| 43 | `ai/misc/EntityCanColony.java` | 5 | 殖民地（仅内部生成） | **已实现** | entity/PureParasiteEntity.java `BuildColonyGoal`；block/ColonyHeartBlock.java、block/ColonyStructureBlock.java<br>原接口只有一个 `onlySpawnInside()`；本工程由 colony 方块 + BuildColonyGoal 承担。 |
| 44 | `ai/misc/EntityCanFly.java` | 4 | 可飞行标记（空接口） | **已实现** | entity/CarrierFlyingEntity.java、entity/AirscrewEntity.java、entity/ArchitectEntity.java、entity/LiceEntity.java + 各 `Flight*Goal`/`Flying*Goal`<br>空标记接口在 26.3 无意义；本工程用 `setNoGravity` + `MoveControl` 覆写 + 飞行 Goal 表达。 |
| 45 | `ai/misc/EntityCanHaveBodies.java` | 62 | 多节身体 API（跟随/掘地/身体数） | **已实现** | entity/AdaptedVariantEntity.java（`ARACHNIDA_SKIN` 与 body 轨道动画、:1506 `tendril.setSkin`）、entity/AncientParasiteEntity.java、entity/AssimilatedDragonEntity.java:458<br>本工程用动画/模型的多节轨道 + 独立 tendril 实体实现；缺的是 EntityAIFollowBodies 的掘地传送（见上）。 |
| 46 | `ai/misc/EntityCanMelt.java` | 23 | 融解接口 | **已实现** | entity/MeltableAssimilated.java；entity/AssimilatedMeltSystem.java；`SimHumanEntity implements MeltableAssimilated`<br>原接口的 `getTHeigh/setaSize/getSelfeFlashIntensity2` 在 26.3 由 `getMeltRenderScale(partialTick)` + `EntityDimensions` 承担。 |
| 47 | `ai/misc/EntityCanPullMobs.java` | 21 | 拉扯牵引接口 | **已实现** | entity/PullingBallOwner.java；entity/PullingBallEntity.java；`PullGoal`/`ReekerPullGoal`/`ArachnidaPullSkillGoal`/`WebPullGoal`<br>原接口的 `setPullingMobEffects/getAcceleration` 在 26.3 由 PullingBall 实体 + `push()` 承担。 |
| 48 | `ai/misc/EntityCanShoot.java` | 9 | 投射物工厂接口 | **已实现** | entity/ParasiteProjectileEntity.java（`Mode` 枚举）；`getProj` → `fireProjectile`<br>22 个原投射物类合并为 1 个实体 + Mode。 |
| 49 | `ai/misc/EntityCanSpawn.java` | 7 | 自然生成门控（按 ID 数据） | **不一致** | registry/CommonModEvents.java、world/EvolutionEvents.java、config/WorldConfig.java:92 `dimensionAllowsNaturalSpawning`<br>本工程用世界演化阶段 + 维度白名单做生成门控，原类的 `canSpawnByIDData()`/`getIDSpawn()` 是按实体自身存的数据决定能否生成（供 ParasiteEventEntity 使用）。语义近似但接口不同。→ 记为不一致。 |
| 50 | `ai/misc/EntityCanSummon.java` | 19 | 召唤容量/冷却接口 | **已实现** | entity/SummonCapacityOwner.java；entity/SummonCapacityTracker.java；`AdaptedVariantEntity`/`BiomassEntity`/`ParasiteProjectileEntity` 实现<br>原接口 `addID/checkID/getIDList/getPointList/getTotalParasites/getActualParasites` 是 int 数组 + 点数的容量模型；本工程改为 `Map<UUID,cost>` 的 `SummonCapacityTracker`，容量语义等价且带 NBT 持久化与自动 prune。 |
| 51 | `ai/misc/EntityCanSwim.java` | 4 | 可潜地标记（空接口） | **已实现** | `DevourerRandomSwimGoal`/`SwimmingDivingGoal` 等 11 个文件<br>空标记；本工程用 Goal 直接表达。 |
| 52 | `ai/misc/EntityCanVectors.java` | 4 | 空标记接口（无方法） | **N/A** | out109 中该接口无任何方法、也无 `instanceof EntityCanVectors` 分支<br>纯占位接口，1.10.9 未承载任何行为，26.3 无需对应物。 |
| 53 | `ai/misc/EntityCutomAttack.java` | 7 | 范围攻击接口 | **已实现** | entity/LongarmsEntity.java、entity/CruxEntity.java、entity/AdaptedVariantEntity.java 的 AOE 攻击分支<br>原接口 `attackEntityAsMobAOE`；本工程由各 AreaMelee/Aoe Goal 承担。 |
| 54 | `ai/misc/EntityPAdapted.java` | 349 | adapted 分类基类（12 个 ada_*） | **已实现** | entity/AdaptedVariantEntity.java<br>Kind 枚举参数化。 |
| 55 | `ai/misc/EntityPAncient.java` | 106 | ancient 分类基类（Oronco/Terla/Dharma/OroncoAW） | **已实现** | entity/AncientParasiteEntity.java<br>EntityDharma / EntityOroncoAW 在 1.10.9 未注册（死类），本工程不移植。 |
| 56 | `ai/misc/EntityPAssimara.java` | 24 | assimilated 特殊体基类（spe_*） | **已实现** | entity/AssimilatedVariantEntity.java |
| 57 | `ai/misc/EntityPBeckon.java` | 58 | Beckon 分类基类（venkrol 系） | **已实现** | entity/NexusParasiteEntity.java（Family.BECKON）<br>EntityVenkrolSV 在 1.10.9 未注册（死类）。 |
| 58 | `ai/misc/EntityPCosmical.java` | 632 | 派生体基类（Heblu/Kirin） | **已实现** | entity/DerivedParasiteEntity.java；entity/DraconiteEntity.java；entity/KirinEntity.java |
| 59 | `ai/misc/EntityPCrude.java` | 30 | crude 分类基类（CruxA/CruxB/Heed/Inhoo/Host/Lesh/…） | **已实现** | entity/CruxEntity.java、entity/HeedEntity.java、entity/HostEntity.java、entity/HostIIEntity.java、entity/AbstractHostEntity.java |
| 60 | `ai/misc/EntityPDerived.java` | 81 | derived 分类基类 | **已实现** | entity/DerivedParasiteEntity.java |
| 61 | `ai/misc/EntityPDispatcher.java` | 328 | Dispatcher 分类基类（dod/leem 系） | **已实现** | entity/NexusParasiteEntity.java（Family.DISPATCHER） |
| 62 | `ai/misc/EntityPFeral.java` | 345 | feral 分类基类（fer_*） | **已实现** | entity/FeralParasiteEntity.java；entity/FeralEndermanEntity.java |
| 63 | `ai/misc/EntityPFocused.java` | 81 | focused 分类基类 | **N/A** | EntityPFocused 只被 EntityBanoFocused / EntityShycoFocused 继承，而这两个类在 1.10.9 未注册也未引用（死类）<br>死代码：无 id、无生成途径。本工程不移植。 |
| 64 | `ai/misc/EntityPHijacked.java` | 124 | hijacked 分类基类（hi_*） | **已实现** | entity/HiBlazeEntity.java、entity/HiGolemEntity.java、entity/HiSkeletonEntity.java |
| 65 | `ai/misc/EntityPInfected.java` | 494 | infected/assimilated 分类基类（sim_*/head） | **已实现** | entity/AssimilatedParasiteEntity.java、entity/AssimilatedVariantEntity.java、entity/AssimilatedHeadEntity.java、entity/SimHumanEntity.java、entity/SimAdventurerEntity.java<br>mobEvolution 门控已接入。 |
| 66 | `ai/misc/EntityPMalleable.java` | 755 | 所有可变寄生虫的公共基类 | **已实现** | entity/PrimitiveParasiteEntity.java（本工程公共基类）+ 各变体类<br>差异：原基类的 gene 系统（geneMindam/geneDamcap/geneLookwall/geneSprinting/geneWaterleap/geneSpecialmove/geneAdaptation/geneBlocksearch/geneResidue/geneOrbbox）在本工程**没有对应物**——本工程直接用 Kind 分支决定行为，不再有 SRPConfigSystems.generationUse 随机基因。这是结构性差异，见下方「遗留项」。 |
| 67 | `ai/misc/EntityPPreeminent.java` | 383 | preeminent 分类基类（Elvia/Flam/Jinjo/Lencia/Pheon/Tenn/Vesta） | **已实现** | entity/PreeminentParasiteEntity.java |
| 68 | `ai/misc/EntityPPrimitive.java` | 334 | primitive 分类基类（12 个 pri_*） | **已实现** | entity/PrimitiveVariantEntity.java |
| 69 | `ai/misc/EntityPPure.java` | 535 | pure 分类基类（Alafha/Anged/Esor/Flog/Ganro/Omboo/Orch/Rond/Soo） | **已实现** | entity/PureParasiteEntity.java<br>EntityRond 在 1.10.9 未注册（死类）。 |
| 70 | `ai/misc/EntityPRooter.java` | 164 | Rooter 分类基类（leem 系） | **已实现** | entity/NexusParasiteEntity.java（Family.ROOTER）；`spawnRootmassCysts`:808、`applyRooterSupport`:829 |
| 71 | `ai/misc/EntityPStationary.java` | 397 | stationary 分类基类（deterrent 系） | **已实现** | entity/DeterrentParasiteEntity.java |
| 72 | `ai/misc/EntityPStationaryArchitect.java` | 345 | architect 分类基类（Ten/Kol） | **已实现** | entity/ArchitectEntity.java；entity/WorkerEntity.java |
| 73 | `ai/misc/EntityParasiteBase.java` | 2585 | 全部寄生虫的根基类 | **已实现** | entity/PrimitiveParasiteEntity.java（822 行，本工程公共基类）<br>差异见「遗留项」：gene 系统、`canChangeVariant`、skin 120 特殊态、`skillBreakBlocks` 的 `doTileDrops` 分支（本工程已用 RuntimeToggles 接线）等未 1:1 复刻。 |

## 4. 死代码清单（1.10.9 事实）

以下原类在 out109 全树中**没有任何 `new` 实例化点**，属于 1.10.9 遗留的死代码：

| 原类 | 行数 | 判定 | 本工程处理 |
|---|---|---|---|
| `ai/AIDisableBeaconIki` | 416 | N/A | 不移植；26.3 无信标停用语义的事实来源 |
| `ai/EntityAIEvadeTP` | 98 | N/A | 不移植；瞬移语义已由活的 `EntityAIKirinBlink`/`KirinBlinkGoal` 承担 |
| `ai/EntityAINexusNest` | 21 | N/A | 不移植；巢穴雾已由 `NexusParasiteEntity.placeNestFog` 承担 |
| `ai/EntityAISoundEaterStalk` | 141 | 缺失（活行为） | AI 类不移植，但**其描述的活行为（EntityInfHuman skin 111）本工程缺失**，见第 5 批 |
| `ai/misc/EntityCanVectors` | 4 | N/A | 空接口，无行为 |
| `ai/misc/EntityPFocused` | 81 | N/A | 仅被两个死类继承 |
| `monster/focused/EntityBanoFocused` | 352 | N/A | 未注册 |
| `monster/focused/EntityShycoFocused` | 281 | N/A | 未注册 |
| `monster/ancient/EntityDharma` | 228 | N/A | 未注册 |
| `monster/ancient/EntityOroncoAW` | 417 | N/A | 未注册 |
| `monster/deterrent/nexus/EntityVenkrolSV` | 87 | N/A | 未注册 |
| `monster/inborn/EntityMor` | 55 | N/A | 未注册 |
| `monster/pure/EntityRond` | 329 | N/A | 未注册 |

## 5. 缺失/不一致清单与处置

### 5.1 必须补（本任务实现）

| # | 缺口 | 原事实来源 | 本工程落点 | 批次 |
|---|---|---|---|---|
| G1 | `EntityAIBlockLight`：gene 7 破坏光源 | `ai/EntityAIBlockLight.java:1-211`；挂载点 `ai/misc/EntityPMalleable.java:92` | 新增 Goal 挂到 `PrimitiveParasiteEntity` | 4 |
| G2 | `EntityAIFollowBodies`：跟随母体 + 掘地传送 + 击杀进化 | `ai/EntityAIFollowBodies.java:1-190`；挂载点 `monster/primitive/EntityWymo.java`、`EntityZaa.java`、`adapted/EntityWymoAdapted.java`、`EntityZaaAdapted.java`、`carrier_worm/EntityQuac.java` | `PrimitiveVariantEntity`(TOZOON/BURROWER)、`AdaptedVariantEntity`、`CarrierWormEntity` | 4 |
| G3 | `EntityAICircleGroup`：sim_human 绕圈编队 | `ai/misc/EntityAICircleGroup.java:1-311`；挂载点 `monster/infected/EntityInfHuman.java` | `SimHumanEntity` | 4 |
| G4 | SoundEater：skin 111 声音记忆 + 方块声广播 | `monster/infected/EntityInfHuman.java:151-188,440-465,620-640`；`events/SoundEaterBlockSoundHandler`；`events/SoundEaterArrowImpactHandler`；`ai/SoundEaterSoundHelper.java` | `SimHumanEntity` + 事件层 | 3 |
| G5 | Venkrol 龙卷完整分级力场 | `entity/logic/VenkrolTornadoLogic.java:1-172` | `NexusParasiteEntity.createStormVortex` 重写 | 3 |
| G6 | `EntityAIBlockResidue` 覆盖到全部 gene 8 的 adapted Kind | `ai/EntityAIBlockResidue.java:1-84`；9 个挂载类 | `AdaptedVariantEntity` | 4 |
| G7 | `EntityAINexusGrow.spawnLeem`：2% 召唤 rooter_si + 上限 | `ai/EntityAINexusGrow.java:69-98` | `NexusParasiteEntity` | 4 |

### 5.2 记录为不一致、本轮不改（低影响或有结构性原因）

| 缺口 | 原因 |
|---|---|
| gene 系统（`EntityPMalleable` 的 10 个 gene 开关 + `SRPConfigSystems.generationUse` 随机基因） | 本工程用 `Kind` 枚举直接决定行为，无基因层。要 1:1 需要新增整套 gene 数据与同步，属于架构级改动，超出 R6 范围。 |
| `EntityAIDodAttack` 的 `mobattackingBlackList` / `mobattackingBlackListWhite` 过滤 | 本工程按家族拆实现，未接入黑/白名单配置；配置项在 config/** 属于 Lead 范围。 |
| `EntityAIVenkrolSummon` 的 `SRPAttributes.VENKROL_MOBTABLEG` / `…A` mob 表 | 本工程用 `SummonCapacityOwner` 容量模型替代，容量语义等价但表未逐条复刻。 |
| `EntityCanSpawn` 的 `canSpawnByIDData()`/`getIDSpawn()` 接口 | 本工程用世界演化阶段 + 维度白名单门控，语义近似。 |
| `EntityParasiteBase` 的 skin 120 特殊态、`canChangeVariant`、`getaSize/getTHeigh` 渲染尺寸 | 由 26.3 的 `EntityDimensions` + 各实体自己的同步数据承担，非 1:1。 |

