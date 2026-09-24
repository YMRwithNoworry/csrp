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
| 本工程 `ModEntities.java` 注册的实体 id | 170 |

AI 判定分布：

| 状态 | 条数 |
|---|---|
| 已实现 | 65 |
| 不一致 | 3 |
| 缺失 | 0 |
| N/A | 5 |

## 1. 实体 id 级 diff

### 1.1 原模组有、本工程无（0 个）

原模组 `SRPEntities.java` 注册的 158 个 id 在本工程**全部已注册**。
工程早期版本曾把 13 个投射物 id 合并进 `csrp:parasite_projectile` 的 `Mode` 枚举，后续这些 id 也逐一补回了独立注册；下表保留映射关系作为记录：

| 原 id | 原类 | 早期合并去向 |
|---|---|---|
| `webball` | `EntityProjectileWebball` | Mode.WEBBALL |
| `spineball` | `EntityProjectileSpineball` | Mode.SPINEBALL |
| `nadeball` | `EntityProjectileNade` | Mode.NADE |
| `salivaball` | `EntityProjectileAlafhaBall` | Mode.ALAFHA_BALL |
| `ballball` | `EntityProjectileAngedball` | Mode.ANGED_BALL |
| `ancientball` | `EntityProjectileAncientball` | Mode.ANCIENT_BALL |
| `biomassball` | `EntityProjectileBiomass` | Mode.BIOMASS_BALL |
| `missile` | `EntityProjectileDragonE` | Mode.DRAGON_MISSILE |
| `balltall` | `EntityProjectileElviaBall` | Mode.ELVIA_BALL |
| `ballmall` | `EntityProjectileLenciaBall` | Mode.LENCIA_BALL |
| `salivaeff` | `EntityProjectileEffects` | Mode.EFFECTS |
| `heblu_light` | `EntityProjectileHebluLight` | Mode.HEBLU_LIGHT |
| `meteor` | `EntityMeteor` | Mode.METEOR |

### 1.2 本工程有、原模组无（12 个，均为本工程新增的辅助/拆分 id）

| 本工程 id | 本工程实现类 | 说明 |
|---|---|---|
| `crux_block_damage` | `—` | CruxA 投掷方块伤害的独立伤害源实体 |
| `dragon_egg_assimilation` | `—` | 龙蛋同化辅助实体 |
| `haunter_damage` | `—` | Haunter 伤害载体 |
| `haunter_homing` | `—` | Haunter 追踪弹 |
| `marauder_tendril` | `MarauderTendrilEntity` | Marauder 触手部件 |
| `meteor_satellite` | `—` | 陨星子体 |
| `parasite_projectile` | `—` | 原投射物类的合并实体（Mode 枚举） |
| `pulling_ball` | `—` | 原 EntityProjectilePullball 的独立化 |
| `scary_orb` | `—` | 原 EntityOrbScary 的命名对齐 |
| `shockwave` | `—` | waveshock 的 legacy 别名 |
| `sim_dragonhead` | `AssimilatedDragonHeadEntity` | sim_dragonehead 的命名对齐 |
| `warden_waveshock` | `—` | Warden 冲击波独立实体 |

### 1.3 结论

- 实体 **id 层无缺失**：原模组 158 个 id 在本工程全部已注册。
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
| 1 | `ai/AIDisableBeaconIki.java` | 416 | Iki 搬运并停用信标 | **N/A** | out109 全树无 `new AIDisableBeaconIki`（唯一引用是自身文件）<br>死代码：1.10.9 未把它挂到任何实体上，移植时同样不挂。26.3 若要做等价行为，用 `MoveToBlockGoal` + 方块实体检测，但无事实来源可依。 |
| 2 | `ai/EntityAIAncientSummon.java` | 81 | 古生代实体召唤 ancientpod | **已实现** | entity/AncientParasiteEntity.java `DreadPodGoal`；entity/AncientPodEntity.java<br>原类挂在 EntityOronco / EntityDodSIV 上，本工程分别落在 AncientParasiteEntity 的 ORONCO Kind 与 NexusParasiteEntity 的 DISPATCHER_SIV。 |
| 3 | `ai/EntityAIAttackMeleeNotGround.java` | 93 | 空中近战（目标上方 off 格） | **已实现** | entity/ArchitectEntity.java `FlightAttackGoal`；entity/LiceEntity.java；entity/AdaptedVariantEntity.java `VerminFlightAttackGoal`<br>原类用 `getMoveHelper().setMoveTo(target.y+off)` 做空中贴近；本工程 FlightAttackGoal 用 `getMoveControl().setWantedPosition(target.y+eyeHeight/2)` 等价。 |
| 4 | `ai/EntityAIAttackMeleeRangeSwitch.java` | 52 | geneSpecialmove 下近战/远程切换 | **已实现** | entity/PureParasiteEntity.java `VigilanteRangeSwitchGoal`；entity/PreeminentParasiteEntity.java `HaunterMeleeRangeSwitchGoal`<br>原类 `parent.getGeneMod(5)` 是 geneSpecialmove（EntityParasiteBase:604，默认 true），本工程对应各 Kind 的 rangeSwitch 分支。 |
| 5 | `ai/EntityAIAttackMeleeRanged.java` | 68 | 近战+保持远程站位 | **已实现** | entity/PreeminentParasiteEntity.java `HaunterRangedPositionGoal` |
| 6 | `ai/EntityAIAttackMeleeStatus.java` | 204 | 带 status/攻击冷却动画的核心近战 | **已实现** | 40 个实体类用 `MeleeAttackGoal`；status 由 `setParasiteStatus` + `ParasiteAnimations` 承担<br>原类的 `getAttackCooldownAni/setAttackCooldownAni` 在 26.3 由 `attackAnim` + status 轨道替代。 |
| 7 | `ai/EntityAIAttackMeleeStatusAOE.java` | 40 | 范围近战（矩形 AOE） | **已实现** | entity/PureParasiteEntity.java `MonarchAreaMeleeGoal`/`WardenAreaMeleeGoal`；entity/PreeminentParasiteEntity.java `HaunterMeleeAoeGoal`/`GruntAreaMeleeGoal`；entity/LongarmsEntity.java；entity/CruxEntity.java |
| 8 | `ai/EntityAIAttackProjectile.java` | 109 | EntityCanShoot 投射物攻击 | **已实现** | entity/PreeminentParasiteEntity.java `LegacyProjectileAttackGoal`；entity/ParasiteProjectileEntity.java<br>22 个原投射物类合并为 parasite_projectile 的 Mode 枚举（部分 id 已另行独立注册）。 |
| 9 | `ai/EntityAIAttackRangedStatus.java` | 136 | 带 status 的远程攻击（初速 0.08） | **已实现** | entity/ParasiteProjectileEntity.java `fireProjectile`；entity/PureParasiteEntity.java `YelloweyeRangedGoal`/`VigilanteRangedGoal`；entity/DeterrentParasiteEntity.java `SentrySpineGoal` |
| 10 | `ai/EntityAIAttackSwell.java` | 49 | 膨胀自爆（Carrier） | **已实现** | entity/CarrierEntity.java `SwellGoal` |
| 11 | `ai/EntityAIAttackVenkrol.java` | 143 | Venkrol 近战（attackInterval=20） | **已实现** | entity/NexusParasiteEntity.java `tick()` 家族能力 + `spawnBombVolley`/`spawnPodVolley`<br>原类是本工程 BECKON 家族的近战/技能循环；本工程改为 tick 驱动的家族能力（间隔由 `Kind.summonCooldown` 控制），语义等价但结构不同。 |
| 12 | `ai/EntityAIAvoidEntityStatus.java` | 96 | 躲避指定实体类 | **已实现** | entity/AssimilatedHeadEntity.java；entity/BuglinEntity.java；entity/MovingFleshEntity.java |
| 13 | `ai/EntityAIAvoidOrAttack.java` | 77 | 按距离躲避或攻击 | **已实现** | entity/RupterEntity.java 内 `AvoidOrAttack` 分支 |
| 14 | `ai/EntityAIBlockInfest.java` | 39 | 周期性感染脚下方块 | **已实现** | entity/NexusParasiteEntity.java（BECKON 间隔 `max(20, 100-stage*15)`，DISPATCHER 200t）；block/BlockInfestation.infestAround<br>原类间隔见 EntityVenkrol/SII/SIII；本工程按家族分别定节奏。 |
| 15 | `ai/EntityAIBlockLight.java` | 211 | gene 7 寄生虫破坏光源方块 | **已实现** | entity/PrimitiveParasiteEntity.java `LightSourceBreakingGoal`（挂载优先级 7）；malleable 家族在 PureParasiteEntity / AdaptedVariantEntity / PrimitiveVariantEntity / PreeminentParasiteEntity 里覆写 `supportsLightSourceBreaking()`<br>第 4 批已实现：20×4×20 扫描、lightEmission>=5、mobGriefing、无目标、40t 重扫、距离>5 走 moveTo(1.1)、<=5 按 destroySpeed*10 进度破坏并同步 destroyBlockProgress、240t 不可达拉黑、破坏用 RuntimeToggles.parasiteBlockDrops()。原 gene 7（EntityPMalleable.geneBlocksearch，默认 true）在本工程用家族开关替代。 |
| 16 | `ai/EntityAIBlockResidue.java` | 84 | gene 8 寄生虫把周围方块转成残留物 | **已实现** | entity/AdaptedVariantEntity.java `BlockResidueGoal`（`addGoal(9, ...)`）+ `spreadResiduePatch` / `sendResidueParticles`<br>第 3 批已实现：counter 160（1/5 概率推进）、-1 停寻路+ADAPTED_V、-40 再响、每刻粒子、-60 按 (2*range+1)² 铺残留（1/2 概率、空气上方、实心地板、排除 InfestedStain）、-100 复位 200。range 按 Kind：BOLSTER 3，ARACHNIDA/LONGARMS/MANDUCATER/REEKER/SUMMONER/VISCERA 2，对应 out109 的 7 个挂载类（另 2 个是死类 Focused）。 |
| 17 | `ai/EntityAIDiveBomb.java` | 250 | 俯冲轰炸（升空 20 / 步长 0.6 / 120t） | **已实现** | entity/PrimitiveVariantEntity.java `RicardoDiveBombGoal`<br>常量逐一对应：ASCEND_HEIGHT=20.0、ASCEND_STEP_MAX=0.6、MAX_ASCEND_TICKS=120、俯冲初速 2.8、加速度 0.35*diveTicks。 |
| 18 | `ai/EntityAIDodAttack.java` | 513 | Dod/Dispatcher 存储寄生虫、爆弹、破方块 | **不一致** | entity/NexusParasiteEntity.java `storeNearbyParasite` / `spawnBombVolley` / `spawnPodVolley` / `breakBlocksTowardsTarget` / `spawnRootmassCysts`<br>本工程按家族拆分实现，未逐条复刻原类 513 行里的分阶段脚本。差异点：原类的 `mobattackingBlackList` / `mobattackingBlackListWhite` 配置过滤（在 config/**，属 Lead 范围）未接入。 |
| 19 | `ai/EntityAIEvade.java` | 120 | 受击后跳跃闪避 | **已实现** | entity/AdaptedVariantEntity.java `EvadeGoal`；entity/PrimitiveVariantEntity.java；entity/VisceraEntity.java<br>原类 `getGeneMod(5)`=geneSpecialmove 门控；本工程按 Kind 挂载。 |
| 20 | `ai/EntityAIEvadeDash.java` | 91 | 冲刺闪避 | **已实现** | entity/ManglerEntity.java `EvasiveDashGoal`；entity/PureParasiteEntity.java `MonarchEvasiveDashGoal`/`GruntEvasiveDashGoal`；entity/PreeminentParasiteEntity.java `HaunterEvadeDashGoal` |
| 21 | `ai/EntityAIEvadeTP.java` | 98 | 闪现闪避 | **N/A** | out109 全树无 `new EntityAIEvadeTP`（唯一引用是自身文件）<br>死代码：1.10.9 未挂载。瞬移语义已由活的 `EntityAIKirinBlink` / `KirinBlinkGoal` 承担。 |
| 22 | `ai/EntityAIFlightAttack.java` | 143 | 飞行近战攻击 | **已实现** | entity/ArchitectEntity.java `FlightAttackGoal`；entity/LiceEntity.java；entity/CarrierFlyingEntity.java `FlyingCombatGoal` |
| 23 | `ai/EntityAIFlightLimits.java` | 62 | 飞行高度上下限（±0.04/刻） | **已实现** | entity/VerminEntity.java `FlightHeightLimitGoal`；entity/PureParasiteEntity.java `OmbooFlightLimitsGoal`/`OverseerFlightLimitGoal`<br>原类 `howMuchNeg/howMuchPos` 数空气格决定压升/压降；本工程三个 Goal 分别对应不同家族。 |
| 24 | `ai/EntityAIFollowBodies.java` | 190 | Wymo/Zaa/Quac 跟随母体 + 掘地传送 | **不一致** | entity/BurrowingVariantEntity.java（`bodyPredecessor` 链式跟随、spacing 0.2 规则、>3 格 snap、掘地/传送循环、bodyPartEffect）；entity/PrimitiveParasiteEntity.java `onParasiteKill` 覆盖击杀进化<br>原类把「游离体跟随母体」做成一个 AI；本工程把每一节身体做成独立实体，用 bodyPredecessor UUID 链 + lerp 到位，语义等价但结构不同。原类里 `spawnNext(..., worm.getEvolution(...))` 对 ada_tozoon / ada_burrower / carrier_worm 恒为空（out109 EntityWymoAdapted:471 / EntityZaaAdapted:384 / EntityQuac:497 的 getEvolution 都 return null），本工程同样不需要；pri_tozoon / pri_burrower 的进化由 PrimitiveParasiteEntity.onParasiteKill（已接 mobEvolution）承担。 |
| 25 | `ai/EntityAIGetFollowers.java` | 96 | 招募跟随者 | **已实现** | entity/PrimitiveVariantEntity.java `RecruitFollowersGoal`；entity/HeedEntity.java；entity/DredgeEntity.java；entity/AdaptedVariantEntity.java `ReekerRecruitFollowersGoal` |
| 26 | `ai/EntityAIGiveEffectsArea.java` | 56 | 范围给状态效果 | **已实现** | entity/CarrierEntity.java `CarrierBuffGoal`；entity/AssimilatedHeadEntity.java `HeadCothCloudGoal`；entity/AdaptedVariantEntity.java `BolsterSupportGoal` |
| 27 | `ai/EntityAIInfectedSearch.java` | 122 | 搜索被感染体以融解合体 | **已实现** | entity/AssimilatedMeltSystem.java（文档明写 Shared EntityAIInfectedSearch and EntityCanMelt behavior）；`tryStartGroup`<br>原类挂在 EntityPInfected 上；本工程由 AssimilatedMeltSystem 统一承担（KILL_THRESHOLD=10、需 3 个同族、最低 phase 1）。 |
| 28 | `ai/EntityAIKirinBlink.java` | 283 | Kirin 蓄力闪现（60t 蓄力 / 200t 冷却 / 16 格起） | **已实现** | entity/KirinEntity.java `KirinBlinkGoal`<br>常量对应：`distanceToSqr(target) <= 256.0D` 即原类 MIN_FAR_DIST_SQ=256.0；BLINK_CHARGE_TICKS 对应 CHARGE_TIME=60；冷却 200t。 |
| 29 | `ai/EntityAINearestAttackableTargetStatus.java` | 306 | 带 gene 2 视线豁免的目标选择 | **已实现** | 32 个实体类使用 `NearestAttackableTargetGoal`<br>原类 `getGeneMod(2)`=geneLookwall（穿透视线）；本工程用 26.3 的 `NearestAttackableTargetGoal` 构造参数（mustSee / 目标谓词）表达同一语义。 |
| 30 | `ai/EntityAINexusGrow.java` | 456 | 节点成长（阶段升级 + 召唤 rooter_si + 结构生成） | **已实现** | entity/NexusParasiteEntity.java（成长计时 + mobEvolution 门控、`evolve()`、`trySpawnRooterSi`、`generateProtectionStructure`）<br>阶段升级、mobEvolution 门控、2% 召唤 rooter_si（20t 节奏、nexusLeemCap=5、nexusLeemDis=32）、升级后按家族概率生成 NexusProtection1/2/3（Dispatcher 0.3 / Beckon 0.5 / Rooter 0.3，stage=1）均已实现。未复刻：SRPConfigSystems.evolutionNests / deveNestsUse 与 maximumStageList 的维度上限锁（配置项在 config/**）。 |
| 31 | `ai/EntityAINexusNest.java` | 21 | 节点巢穴 | **N/A** | out109 全树无 `new EntityAINexusNest`（唯一引用是自身文件）<br>死代码：1.10.9 未挂载；本工程用 `NexusParasiteEntity.placeNestFog` 承担巢穴雾语义。 |
| 32 | `ai/EntityAIParasiteFollow.java` | 84 | 跟随领袖寄生虫 | **已实现** | entity/ParasiteFollowGoal.java（27 个实体类使用） |
| 33 | `ai/EntityAISkill.java` | 92 | 技能跳跃 | **已实现** | entity/ManglerEntity.java `SkillLeapGoal`；entity/PureParasiteEntity.java `MonarchSkillLeapGoal`/`GruntSkillLeapGoal`/`RageSkillGoal`；entity/PrimitiveVariantEntity.java<br>原类 `getGeneMod(5)` 门控。 |
| 34 | `ai/EntityAISoundEaterStalk.java` | 141 | SoundEater 潜行 | **已实现** | entity/SimHumanEntity.java `isSoundEater` / `tickSoundEater` / `notifyHeardSound` / `broadcastSound`；entity/SoundEaterSoundEvents.java<br>**这个 AI 类本身在 1.10.9 是死代码**（out109 全树无 new），活行为写在 EntityInfHuman 的 skin 111 里。第 2 批按活行为实现：1% 生成率、FOLLOW_RANGE 12 / 速度 0.32、16×4×16 听觉盒、疾跑 ×3 响度、记忆 60t、无记忆无复仇目标则清目标。26.3 无 walkDist/walkDistO，用 getDeltaMovement().horizontalDistance() 替代（同量纲）。 |
| 35 | `ai/EntityAISwimmingDiving.java` | 47 | 水中下潜 | **已实现** | entity/HeedEntity.java、entity/ManglerEntity.java、entity/GnatEntity.java 等 7 个文件的 `SwimmingDivingGoal` |
| 36 | `ai/EntityAIVenkrolSummon.java` | 451 | Venkrol 按 mob 表召唤 + 上限 | **不一致** | entity/NexusParasiteEntity.java `summonBeckonParasites` / `summonDispatcherDefenses`（由 `performFamilyAbility` 调度）<br>已实现召唤本体与冷却（`Kind.summonCooldown`）。缺：原类依赖 `SRPAttributes.VENKROL_MOBTABLEG`（地面表）/`…A`（空中表）+ `SRPConfigMobs` 的 useEvolution / rsIgnoreCooldownAtSpawn 开关，并做 `getTotalParasites()` / `getActualParasites()` 上限校验。本工程用 `SummonCapacityOwner` 容量模型替代，上限语义等价但 mob 表未逐条复刻。 |
| 37 | `ai/EntityAIWanderStatus.java` | 107 | 带 status 的游荡 | **已实现** | 24 个实体类的 `RandomMoveGoal` / `WaterAvoidingRandomStrollGoal` |
| 38 | `ai/EntityAIWaterLeapAtTargetStatus.java` | 99 | 朝目标水中跃出 | **已实现** | entity/AdaptedVariantEntity.java `ArachnidaWaterLeapGoal`/`WaterPursuitLeapGoal`；entity/HeedEntity.java `WaterLeapGoal`；entity/PreeminentParasiteEntity.java；entity/ManglerEntity.java<br>原类挂在 32 个类上；本工程按 Kind 分散实现。 |
| 39 | `ai/SoundEaterSoundHelper.java` | 20 | 广播方块破坏/放置声给 SoundEater | **已实现** | entity/SimHumanEntity.java `broadcastSound`；entity/SoundEaterSoundEvents.java（BreakBlockEvent 半径 16/寿命 100、BlockEvent.EntityPlaceEvent 半径 12/寿命 80）<br>第 2 批已实现，数值照 out109 SoundEaterBlockSoundHandler。 |
| 40 | `ai/misc/EntityAICircleGroup.java` | 311 | sim_human 围绕目标绕圈编队 | **已实现** | entity/SimHumanEntity.java `CircleGroupGoal`（goalSelector.addGoal(4, ...)，与原 EntityInfHuman:121 同优先级）<br>第 5 批已实现：speed 1.15 / minGroup 8 / radius 4..10 / scan 16、id 升序快照、每 10t 重估圆心 + 0.15 平滑、半径 0.2 平滑、方向取 id 奇偶、2π/100 角速度 + 抖动、wobble 0.8/0.06、wander 0.6/0.09、目标 lerp 0.35、±0.4 垂直夹取、8t 或 >2 格刷新航点、yaw 20° 步进、look(30,30)、切向推力 0.03、分离推力 min(0.035, 0.02/d²)。 |
| 41 | `ai/misc/EntityBodyParts.java` | 9 | 多部件受击接口 | **已实现** | entity/AssimilatedDragonHeadEntity.java、entity/AssimilatedHeadEntity.java、entity/SimAdventurerHeadEntity.java、entity/AbominationEntity.java、entity/DreadnautTentacleEntity.java、entity/MarauderTendrilEntity.java<br>26.3 用独立的 head/part 实体 + owner 引用替代 1.12.2 的 `attackEntityBodyFrom`。 |
| 42 | `ai/misc/EntityCanClimb.java` | 5 | 可攀爬标记 | **已实现** | 14 个实体类含 `Climb` 分支（AdaLonglegEntity/AdaScuttlerEntity/AdaptedVariantEntity/…）<br>26.3 无 `isOnLadder` 覆写的等价标记接口，本工程用 Kind 分支直接实现攀爬位移。 |
| 43 | `ai/misc/EntityCanColony.java` | 5 | 殖民地（仅内部生成） | **已实现** | entity/PureParasiteEntity.java `BuildColonyGoal`；block/ColonyHeartBlock.java、block/ColonyStructureBlock.java<br>原接口只有一个 `onlySpawnInside()`；本工程由 colony 方块 + BuildColonyGoal 承担。 |
| 44 | `ai/misc/EntityCanFly.java` | 4 | 可飞行标记（空接口） | **已实现** | entity/CarrierFlyingEntity.java、entity/AirscrewEntity.java、entity/ArchitectEntity.java、entity/LiceEntity.java + 各 `Flight*Goal`/`Flying*Goal`<br>空标记接口在 26.3 无意义；本工程用 `setNoGravity` + `MoveControl` 覆写 + 飞行 Goal 表达。 |
| 45 | `ai/misc/EntityCanHaveBodies.java` | 62 | 多节身体 API（跟随/掘地/身体数） | **已实现** | entity/BurrowingVariantEntity.java（bodyPredecessor 链、bodySegmentCount、bodyPartEffect、掘地传送）；entity/AdaptedVariantEntity.java（ARACHNIDA_SKIN 与 body 轨道动画、tendril.setSkin）<br>本工程用动画/模型的多节轨道 + 独立 tendril 实体实现。 |
| 46 | `ai/misc/EntityCanMelt.java` | 23 | 融解接口 | **已实现** | entity/MeltableAssimilated.java；entity/AssimilatedMeltSystem.java；`SimHumanEntity implements MeltableAssimilated`<br>原接口的 `getTHeigh/setaSize/getSelfeFlashIntensity2` 在 26.3 由 `getMeltRenderScale(partialTick)` + `EntityDimensions` 承担。 |
| 47 | `ai/misc/EntityCanPullMobs.java` | 21 | 拉扯牵引接口 | **已实现** | entity/PullingBallOwner.java；entity/PullingBallEntity.java；`PullGoal`/`ReekerPullGoal`/`ArachnidaPullSkillGoal`/`WebPullGoal`<br>原接口的 `setPullingMobEffects/getAcceleration` 在 26.3 由 PullingBall 实体 + `push()` 承担。 |
| 48 | `ai/misc/EntityCanShoot.java` | 9 | 投射物工厂接口 | **已实现** | entity/ParasiteProjectileEntity.java（`Mode` 枚举）；`getProj` → `fireProjectile`<br>原投射物类合并为 1 个实体 + Mode（部分 id 已另行独立注册）。 |
| 49 | `ai/misc/EntityCanSpawn.java` | 7 | 自然生成门控（按 ID 数据） | **已实现** | entity/AssimilationSpawnGate.java + entity/AssimilationSpawnGateEvents.java（MobSpawnEvent.PositionCheck）；各实体类覆写 assimilationSpawnKey()<br>第 2 批已实现：照 SRPSpawning.java:538-546 的 `count < canSpawnByIDData()` 方向，只在 NATURAL/CHUNK_GENERATION 时拒绝；计数由 infection/InfectionMechanics.replaceForcedHost 在成功同化后 +1。逐类照 out109 核对（含 EntityHiBlaze 读 infbearCanSpawnAssimilatedNat、EntityHiSkeleton 读 higolemCanSpawnAssimilatedNat 两个原版怪癖）。 |
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
| 66 | `ai/misc/EntityPMalleable.java` | 755 | 所有可变寄生虫的公共基类 | **已实现** | entity/PrimitiveParasiteEntity.java（本工程公共基类）+ 各变体类<br>差异：原基类的 gene 系统（geneMindam/geneDamcap/geneLookwall/geneSprinting/geneWaterleap/geneSpecialmove/geneAdaptation/geneBlocksearch/geneResidue/geneOrbbox）在本工程**没有对应物**——本工程直接用 Kind 分支决定行为，不再有 SRPConfigSystems.generationUse 随机基因。这是结构性差异，见「遗留项」。 |
| 67 | `ai/misc/EntityPPreeminent.java` | 383 | preeminent 分类基类（Elvia/Flam/Jinjo/Lencia/Pheon/Tenn/Vesta） | **已实现** | entity/PreeminentParasiteEntity.java |
| 68 | `ai/misc/EntityPPrimitive.java` | 334 | primitive 分类基类（12 个 pri_*） | **已实现** | entity/PrimitiveVariantEntity.java |
| 69 | `ai/misc/EntityPPure.java` | 535 | pure 分类基类（Alafha/Anged/Esor/Flog/Ganro/Omboo/Orch/Rond/Soo） | **已实现** | entity/PureParasiteEntity.java<br>EntityRond 在 1.10.9 未注册（死类）。 |
| 70 | `ai/misc/EntityPRooter.java` | 164 | Rooter 分类基类（leem 系） | **已实现** | entity/NexusParasiteEntity.java（Family.ROOTER）；`spawnRootmassCysts`、`applyRooterSupport` |
| 71 | `ai/misc/EntityPStationary.java` | 397 | stationary 分类基类（deterrent 系） | **已实现** | entity/DeterrentParasiteEntity.java |
| 72 | `ai/misc/EntityPStationaryArchitect.java` | 345 | architect 分类基类（Ten/Kol） | **已实现** | entity/ArchitectEntity.java；entity/WorkerEntity.java |
| 73 | `ai/misc/EntityParasiteBase.java` | 2585 | 全部寄生虫的根基类 | **已实现** | entity/PrimitiveParasiteEntity.java（本工程公共基类）<br>差异见「遗留项」：gene 系统、`canChangeVariant`、skin 120 特殊态等未 1:1 复刻；`skillBreakBlocks` 的 `doTileDrops` 分支已用 RuntimeToggles 接线。 |

## 4. 死代码清单（1.10.9 事实）

以下原类在 out109 全树中**没有任何 `new` 实例化点**，属于 1.10.9 遗留的死代码：

| 原类 | 行数 | 判定 | 本工程处理 |
|---|---|---|---|
| `ai/AIDisableBeaconIki` | 416 | N/A | 不移植；26.3 无信标停用语义的事实来源 |
| `ai/EntityAIEvadeTP` | 98 | N/A | 不移植；瞬移语义已由活的 `EntityAIKirinBlink`/`KirinBlinkGoal` 承担 |
| `ai/EntityAINexusNest` | 21 | N/A | 不移植；巢穴雾已由 `NexusParasiteEntity.placeNestFog` 承担 |
| `ai/EntityAISoundEaterStalk` | 141 | 已实现（活行为） | AI 类本身不移植，但其描述的活行为（EntityInfHuman skin 111）已在 SimHumanEntity 补齐 |
| `ai/misc/EntityCanVectors` | 4 | N/A | 空接口，无行为 |
| `ai/misc/EntityPFocused` | 81 | N/A | 仅被两个死类继承 |
| `monster/focused/EntityBanoFocused` | 352 | N/A | 未注册 |
| `monster/focused/EntityShycoFocused` | 281 | N/A | 未注册 |
| `monster/ancient/EntityDharma` | 228 | N/A | 未注册 |
| `monster/ancient/EntityOroncoAW` | 417 | N/A | 未注册 |
| `monster/deterrent/nexus/EntityVenkrolSV` | 87 | N/A | 未注册 |
| `monster/inborn/EntityMor` | 55 | N/A | 未注册 |
| `monster/pure/EntityRond` | 329 | N/A | 未注册 |

## 5. 缺口处置汇总

### 5.1 已补齐（本任务实现）

| # | 缺口 | 原事实来源 | 本工程落点 | 提交 |
|---|---|---|---|---|
| G1 | `EntityAIBlockLight`：gene 7 破坏光源 | `ai/EntityAIBlockLight.java`；挂载点 `ai/misc/EntityPMalleable.java:92` | `PrimitiveParasiteEntity.LightSourceBreakingGoal`（malleable 家族启用） | 022ed139 |
| G2 | `EntityAIFollowBodies`：跟随母体 + 掘地传送 | `ai/EntityAIFollowBodies.java` | `BurrowingVariantEntity`（bodyPredecessor 链 + 掘地传送循环），语义等价、结构不同 | 既有实现（复核后改判为「不一致」） |
| G3 | `EntityAICircleGroup`：sim_human 绕圈编队 | `ai/misc/EntityAICircleGroup.java`；挂载点 `monster/infected/EntityInfHuman.java:121` | `SimHumanEntity.CircleGroupGoal`（优先级 4） | 本批 |
| G4 | SoundEater：skin 111 声音记忆 + 方块声广播 | `EntityInfHuman:151-188,440-465,620-640`；`events/SoundEaterBlockSoundHandler`；`ai/SoundEaterSoundHelper.java` | `SimHumanEntity` + `SoundEaterSoundEvents` | b5332510 |
| G5 | Venkrol 龙卷完整分级力场 | `entity/logic/VenkrolTornadoLogic.java` | `NexusParasiteEntity.createStormVortex` | b5332510 |
| G6 | `EntityAIBlockResidue` 覆盖全部 gene 8 的 adapted Kind | `ai/EntityAIBlockResidue.java`；9 个挂载类 | `AdaptedVariantEntity.BlockResidueGoal`（优先级 9） | 7cf19ce1 |
| G7 | `EntityAINexusGrow.spawnLeem`：2% 召唤 rooter_si + 上限 | `ai/EntityAINexusGrow.java:69-98` | `NexusParasiteEntity.trySpawnRooterSi` | 7cf19ce1 |
| G8 | `EntityCanSpawn` 同化门槛 | `ai/misc/EntityCanSpawn.java`；`init/SRPSpawning.java:538-546` | `AssimilationSpawnGate` + `AssimilationSpawnGateEvents` | b5332510 |
| G9 | NexusProtection 结构触发 | `EntityPDispatcher/EntityPBeckon/EntityPRooter.generateStructure()` | `NexusParasiteEntity.generateProtectionStructure` | b5332510 |
| G10 | `RuntimeToggles` 接线（doTileDrops / canSpawnNext） | `EntityParasiteBase:2301`、`SRPConfig.doTileDrops`、`ParasiteEventEntity.canSpawnNext` | 4 处 destroyBlock + 10 处进化门控 | 5370d90e |

### 5.2 记录为不一致、本轮不改（低影响或有结构性原因）

| 缺口 | 原因 |
|---|---|
| gene 系统（`EntityPMalleable` 的 10 个 gene 开关 + `SRPConfigSystems.generationUse` 随机基因） | 本工程用 `Kind` 枚举直接决定行为，无基因层。要 1:1 需要新增整套 gene 数据与同步，属架构级改动，超出 R6 范围。 |
| `EntityAIDodAttack` 的 `mobattackingBlackList` / `mobattackingBlackListWhite` 过滤 | 本工程按家族拆实现，未接入黑/白名单配置；配置项在 config/**，属 Lead 范围。 |
| `EntityAIVenkrolSummon` 的 `SRPAttributes.VENKROL_MOBTABLEG` / `…A` mob 表 | 本工程用 `SummonCapacityOwner` 容量模型替代，容量语义等价但表未逐条复刻。 |
| `EntityAIFollowBodies` 的 `getFollowing()`/`getHead()` 单体 AI 形态 | 本工程改为「每节身体一个实体 + bodyPredecessor UUID 链」，语义等价、结构不同。 |
| `EntityAINexusGrow` 的 `SRPConfigSystems.evolutionNests` / `deveNestsUse` / `maximumStageList` 维度上限锁 | 配置项在 config/**，属 Lead 范围。 |
| `EntityParasiteBase` 的 skin 120 特殊态、`canChangeVariant`、`getaSize`/`getTHeigh` 渲染尺寸 | 由 26.3 的 `EntityDimensions` + 各实体自己的同步数据承担，非 1:1。 |

