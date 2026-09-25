# 生物还原矩阵（SRParasites 1.10.9 → csrp）

> 生成时间：2026-09-25T05:39:39.387Z；方法见 `docs/entity-parity/AUDIT_PROTOCOL.md`，逐生物明细见 `docs/entity-parity/raw/<id>.json`。
> 判定：✅ 全部条款满足；🟠 有部分实现但无缺失；❌ 存在缺失；· 未审计。

- 注册生物总数：**127**；已审计：**17**；未审计：**110**
- 条款总计：满足 903 / 部分 443 / 缺失 302（不计入 71 条不适用）
- **加权完成度：68.2%**（partial 计 0.5）

## 分面完成度

| 面 | 满足 | 部分 | 缺失 | 完成度 |
| --- | ---: | ---: | ---: | ---: |
| 注册 `registration` | 87 | 48 | 37 | 64.5% |
| 属性 `attributes` | 125 | 36 | 5 | 86.1% |
| AI `ai` | 105 | 96 | 37 | 64.3% |
| 行为 `behaviors` | 197 | 83 | 74 | 67.4% |
| 伤害/效果 `damage_and_effects` | 126 | 16 | 20 | 82.7% |
| 同步数据 `sync_data` | 33 | 39 | 39 | 47.3% |
| 动画 `animation` | 65 | 16 | 8 | 82% |
| 模型/贴图 `model_texture` | 56 | 6 | 20 | 72% |
| 音效 `sounds` | 42 | 29 | 23 | 60.1% |
| 生成 `spawning` | 56 | 42 | 20 | 65.3% |
| 掉落 `loot` | 11 | 32 | 19 | 43.5% |

## 分组完成度

| 分组 | 已审计/应有 | 满足 | 部分 | 缺失 | 完成度 |
| --- | ---: | ---: | ---: | ---: | ---: |
| crude | 0/4 | 0 | 0 | 0 | 0% |
| primitive | 0/8 | 0 | 0 | 0 | 0% |
| adapted | 0/12 | 0 | 0 | 0 | 0% |
| pure_and_preeminent | 0/19 | 0 | 0 | 0 | 0% |
| ancient | 0/4 | 0 | 0 | 0 | 0% |
| nexus_and_aberrant | 1/15 | 43 | 38 | 32 | 54.9% |
| hijacked_and_feral | 2/12 | 111 | 56 | 59 | 61.5% |
| early_lifecycle | 1/10 | 42 | 25 | 18 | 64.1% |
| marauderized | 1/7 | 58 | 24 | 22 | 67.3% |
| current | 2/13 | 99 | 42 | 34 | 68.6% |
| assimilated | 10/23 | 550 | 258 | 137 | 71.9% |

## 逐生物矩阵

| id | 原版类 | 工程类 | 注册 | 属性 | AI | 行为 | 伤害/效果 | 同步数据 | 动画 | 模型/贴图 | 音效 | 生成 | 掉落 | 完成度 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| `sim_bear` | EntityInfBear | AssimilatedParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_enderman` | EntityInfEnderman | AssimilatedEndermanEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_dragone` | EntityInfDragonE | AssimilatedDragonEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_sheephead` | EntityInfSheepHead | AssimilatedHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_wolfhead` | EntityInfWolfHead | AssimilatedHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_cowhead` | EntityInfCowHead | AssimilatedHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_pighead` | EntityInfPigHead | AssimilatedHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_villagerhead` | EntityInfVillagerHead | AssimilatedHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_horsehead` | EntityInfHorseHead | AssimilatedHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_humanhead` | EntityInfHumanHead | AssimilatedHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_endermanhead` | EntityInfEndermanHead | AssimilatedHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_dragonehead` | EntityInfDragonEHead | AssimilatedDragonHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sim_adventurerhead` | EntityInfPlayerHead | SimAdventurerHeadEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `mar_enderman` | EntitySpeEnderman | MarauderizedEndermanEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `mar_villager` | EntitySpeVillager | MarauderizedVillagerEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `mar_human` | EntitySpeHuman | MarauderizedHumanEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `mar_sheep` | EntitySpeSheep | MarauderizedSheepEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `mar_bear` | EntitySpeBear | MarauderizedBearEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `fer_bear` | EntityFerBear | FeralParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `fer_cow` | EntityFerCow | FeralParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `fer_enderman` | EntityFerEnderman | FeralEndermanEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `fer_horse` | EntityFerHorse | FeralParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `fer_human` | EntityFerHuman | FeralParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `fer_pig` | EntityFerPig | FeralParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `fer_sheep` | EntityFerSheep | FeralParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `fer_wolf` | EntityFerWolf | FeralParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `abo_bodies` | EntityAboBodies | AbominationEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `abo_head` | EntityAboHead | AbominationEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `hi_blaze` | EntityHiBlaze | HiBlazeEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `hi_golem` | EntityHiGolem | HiGolemEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `carrier_heavy` | EntityRathol | CarrierHeavyEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `carrier_light` | EntityGothol | CarrierLightEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `carrier_flying` | EntityButhol | CarrierFlyingEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `rupter` | EntityMudo | RupterEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `movingflesh` | EntityLesh | MovingFleshEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `worker` | EntityKol | WorkerEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `mangler` | EntityNuuh | ManglerEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `gnat` | EntityAta | GnatEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `lice` | EntityViin | LiceEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `beckon_si` | EntityVenkrol | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `beckon_sii` | EntityVenkrolSII | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `beckon_siv` | EntityVenkrolSIV | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `dispatcherten` | EntityDodT | DeterrentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `dispatcher_si` | EntityDod | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `dispatcher_sii` | EntityDodSII | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `dispatcher_siii` | EntityDodSIII | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `dispatcher_siv` | EntityDodSIV | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `rooterball` | EntityLeemB | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `rooter_si` | EntityLeem | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `rooter_sii` | EntityLeemSII | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `rooter_siii` | EntityLeemSIII | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `rooter_siv` | EntityLeemSIV | NexusParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `kyphosis` | EntityTonro | DeterrentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `sentry` | EntityUnvo | DeterrentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `seizer` | EntityNak | DeterrentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `worm` | EntityRof | DeterrentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `incompleteform_small` | EntityInhooS | IncompleteFormSmallEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `incompleteform_medium` | EntityInhooM | IncompleteFormMediumEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `hostii` | EntityHostII | HostIIEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `heed` | EntityHeed | HeedEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `crux` | EntityCruxA | CruxEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `crux_incomplete` | EntityCruxB | IncompleteCruxEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `thrall` | EntityMes | ThrallEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `dredge` | EntityDone | DredgeEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `airscrew` | EntityLeer | AirscrewEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `carrier_worm` | EntityQuac | CarrierWormEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_manducater` | EntityHull | PrimitiveVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_reeker` | EntityNogla | PrimitiveVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_yelloweye` | EntityEmana | PrimitiveVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_summoner` | EntityCanra | SummonerEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_bolster` | EntityBano | PrimitiveVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_tozoon` | EntityWymo | PrimitiveVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_arachnida` | EntityRanrac | PrimitiveVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_devourer` | EntityLum | PrimitiveVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_vermin` | EntityIki | VerminEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_viscera` | EntityGim | VisceraEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `pri_burrower` | EntityZaa | PrimitiveVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_longarms` | EntityShycoAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_manducater` | EntityHullAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_reeker` | EntityNoglaAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_yelloweye` | EntityEmanaAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_summoner` | EntityCanraAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_bolster` | EntityBanoAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_tozoon` | EntityWymoAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_arachnida` | EntityRanracAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_devourer` | EntityLumAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_vermin` | EntityIkiAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_viscera` | EntityGimAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `ada_burrower` | EntityZaaAdapted | AdaptedVariantEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `overseer` | EntityAlafha | PureParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `vigilante` | EntityAnged | PureParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `warden` | EntityGanro | PureParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `bomber_light` | EntityOmboo | PureParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `marauder` | EntityEsor | MarauderEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `monarch` | EntityOrch | PureParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `grunt` | EntityFlog | PureParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `bomber_heavy` | EntityJinjo | PreeminentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `wraith` | EntityElvia | PreeminentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `bogle` | EntityLencia | PreeminentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `haunter` | EntityPheon | PreeminentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `carrier_colony` | EntityVesta | PreeminentParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `succor` | EntityFlam | FlamEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `seeker` | EntitySoo | PureParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `architect` | EntityTenn | ArchitectEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `anc_dreadnaut` | EntityOronco | AncientParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `anc_overlord` | EntityTerla | AncientParasiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `anc_pod` | EntityDropPod | AncientPodEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `anc_dreadnaut_ten` | EntityOroncoTen | DreadnautTentacleEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `kirin` | EntityKirin | KirinEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `draconite` | EntityHeblu | DraconiteEntity | · | · | · | · | · | · | · | · | · | · | · | 未审计 |
| `beckon_siii` | EntityVenkrolSIII | NexusParasiteEntity | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 🟠 | ❌ | 54.9% |
| `hi_skeleton` | EntityHiSkeleton | HiSkeletonEntity | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | ❌ | 🟠 | ❌ | ❌ | ❌ | 61.4% |
| `fer_villager` | EntityFerVillager | FeralParasiteEntity | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 61.6% |
| `host` | EntityHost | HostEntity | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 🟠 | 🟠 | 64.1% |
| `sim_pig` | EntityInfPig | AssimilatedParasiteEntity | ❌ | 🟠 | ❌ | ❌ | · | · | · | · | ❌ | ❌ | ❌ | 67% |
| `mar_cow` | EntitySpeCow | MarauderizedCowEntity | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 67.3% |
| `buglin` | EntityLodo | BuglinEntity | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | 68.6% |
| `pri_longarms` | EntityShyco | LongarmsEntity | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 🟠 | ❌ | 68.6% |
| `sim_horse` | EntityInfHorse | AssimilatedVariantEntity | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | 🟠 | ✅ | ❌ | ❌ | ❌ | 69.7% |
| `sim_human` | EntityInfHuman | SimHumanEntity | ❌ | ✅ | ❌ | ❌ | 🟠 | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | 70.9% |
| `sim_adventurer` | EntityInfPlayer | SimAdventurerEntity | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | 🟠 | 70.9% |
| `sim_squid` | EntityInfSquid | AssimilatedParasiteEntity | ❌ | 🟠 | ❌ | ❌ | 🟠 | ❌ | 🟠 | ❌ | ❌ | ❌ | 🟠 | 71% |
| `sim_villager` | EntityInfVillager | AssimilatedVariantEntity | ❌ | 🟠 | ❌ | ❌ | ✅ | ❌ | 🟠 | 🟠 | ❌ | ❌ | ❌ | 71.7% |
| `sim_wolf` | EntityInfWolf | AssimilatedParasiteEntity | ❌ | 🟠 | ❌ | ❌ | 🟠 | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | 72.4% |
| `sim_cow` | EntityInfCow | AssimilatedParasiteEntity | ❌ | 🟠 | ❌ | ❌ | 🟠 | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | 73.1% |
| `sim_bigspider` | EntityDorpa | AssimilatedVariantEntity | ❌ | ✅ | ❌ | ❌ | 🟠 | ❌ | 🟠 | ❌ | ❌ | ❌ | 🟠 | 74% |
| `sim_sheep` | EntityInfSheep | AssimilatedParasiteEntity | ❌ | 🟠 | ❌ | ❌ | 🟠 | ❌ | 🟠 | ❌ | ❌ | ❌ | ❌ | 75.2% |

## 缺口清单（按完成度升序）

### `beckon_siii`（EntityVenkrolSIII → NexusParasiteEntity，54.9%）
- 眼高 4.9 vs 26.3 默认 4.34、追踪范围 8 vs 64、刷怪蛋颜色与 vanillaEggs 不一致，无数字寄生虫 id(19) 与 per-mob 属性/成长乘数
- AI 缺失：EntityAIWait、EntityAINexusGrow 的群系暂停与 spawnLeem 召唤、EntityAIBlockLight 的光源挖掘语义；召唤 AI 缺 limit 连发/byte 12/邻柱锁定/extraM，召唤表退化为四选一、半径 4 vs 32、冷却 40t vs 280t、配额 8 vs 12
- 行为缺失：钻地/onlyPeek/setBuried/retreat 状态机、顶端粒子、LEVITATION 过滤、实体挤压与鱼钩、itemEvolve/itembase 交互、generateStructure 保护结构、死亡时 ParasiteFog/InfestedStain 升阶、死亡转化 SII、RS 临时柱自伤死亡
- 伤害侧缺失：status 0 时 0.4 减伤、damageCap(14)+RAGE、COTH/VIRA/CORRO/DLER 免疫、毒治愈、PIVOT 转移、CYST_EATING 档与适应粒子；并额外把 nexus 纳入 primitive COTH 传播
- 同步缺失：SKIN/SELFE/COLD_L/DISLO15 与 byte 12/50/51/52 广播；冰冻变体贴图与 LayerGlowing/受击染色层
- 掉落与经验：80%/0-9 个 → 20%/1 个；经验 110 → 64；缺阶段经验门控、死亡钩子与死亡粒子

### `hi_skeleton`（EntityHiSkeleton → HiSkeletonEntity，61.4%）
- 缺少 per-mob 启用开关（hiskeletonEnabled）与 marvillager 专属属性乘数；FOLLOW_RANGE 48 而非 hijackedFollow(24)、XP 30 而非 11
- AI 缺失 EntityAIGetFollowers、EntityAIAttackMeleeRangeSwitch、EntityAIWait、EntityAIJumping；索敌缺少 sneak/invisible 惩罚与 mobattacking 黑名单
- 缺 hijacked skin 变体（setSkin(1)）与 SKIN/COLD_L/DISLO15 同步参数、数字寄生虫 id 303、type 11
- 缺伤害上限 hijackedCap(5)、最小伤害 hijackedMinDamage(1.3)、foodSteal、fearPlayer(FEAR)、oneMindDeathValue
- 缺火焰伤害乘数(×4)与 20% RAGE、毒伤害治愈、效果免疫覆写、血块表现、载具碰撞免疫
- 缺 EntityCanSpawn 计数门控、进化锁/殖民地锁、spawnDays 门控与 phaseCreated 注入；无 SpawnPlacement 注册

### `fer_villager`（EntityFerVillager → FeralParasiteEntity，61.6%）
- 自爆死亡链完全缺失：madeRng 50% → status 6 引信 → dyingBurst/selfExplode（MOBEXPLOSION 音效、ToxicCloud 中毒/COTH、spawnGore 血迹 BIG + EntityRemain(240) + EntityAta + 3 个 EntityGore）以及渲染器引信缩放，fer_villager 的死亡表现与原版差异最大
- 受击/命中反馈缺失：feralMult 0.3 的 EntityGore 炸弹、10% goreFer 血迹铺陈、最小伤害 0.75（+VIRA 放大）、伤害上限 feralCap 3 与 RAGE、攻击冷却 attackCooldownAni=100、偷袭食物与 infected_drop
- AI 缺口：EntityAIEvade 闪避、EntityAIWaterLeapAtTargetStatus 水跃、EntityAIJumping 越障、EntityAIWaterLeapAtTargetStatus/EntityAIWaterLeap 与 EntityAISwimmingDiving 潜水、EntityAIGetFollowers 招募、EntityAIParasiteFollow 反被语义相反（原版 Fer 明确移除）
- 属性/配置缺口：经验 16→10、followRange 20→32、attackSpeedT=14 缺失、per-mob × 全局属性倍率缺失、阶段属性加成与基因加成缺失、scentHPMultiplier 1.5F 未接入
- 同步/NBT 与状态表达缺口：SELFE/SKIN/COLD_L/DISLO15 等 5 项数据参数缺失，parasiteStatus 只能表达 0..2 并被 clamp 到 3，缺少 6/10 等引信与水跃状态
- 生成与掉落规则缺口：无 spawnDays/寄生群系亮度放宽、无 getIDSpawn 27 同化配额与 id 锁、默认掉落表与原版（默认空表 + chance/looting 语义）不一致，额外掉 csrp:bone 1-3；脚步声与音高未还原

### `host`（EntityHost → HostEntity，64.1%）
- 潜地状态下的受伤免疫/击退门控与动态碰撞箱未实现
- 进伤上限（primitiveCap=6）、火伤倍率 4.0、药水免疫、中毒转治疗均缺失
- 最小伤害 2.0 与偷取饱食度 0.5 的近战附加伤害未接入
- 钻地传送、脚下 InfestRemain、地面粒子等伴随表现缺失
- AI：wait/jumpT/AOE 范围切换 GOAL 缺失，跟随 GOAL 未按原版移除
- 经验 20 < 原版 30，且缺少阶段加成与 per-mob 开关

### `sim_pig`（EntityInfPig → AssimilatedParasiteEntity，67%）
- （无缺口摘要，见 raw JSON）

### `mar_cow`（EntitySpeCow → MarauderizedCowEntity，67.3%）
- 眼高 1.3 未实现（用默认值）
- EntityAIWait（呕吐后 60 tick 僵直）与 EntityAIJumping、EntityAISwimmingDiving、EntityAIAttackMeleeRangeSwitch 四个 goal 缺失
- 同化配额门控（getIDSpawn/canSpawnByIDData）与 SRPConfigMobs.marcowEnabled 开关缺失
- 伤害上限 assimaraCap=5、药水免疫、VIRA 最小伤害联动、击杀回血、PARATE 强化、最小伤害攻击全部缺失
- 死亡自爆链（50% madeRng → 40 tick 引信 → 毒云 + gore + MOBEXPLOTION 爆炸音）仅保留 Buglin 召唤
- COTH 命中传播与 3 格 COTH 光环缺失

### `buglin`（EntityLodo → BuglinEntity，68.6%）
- 缺少 per-mob 启用开关（SRPConfigMobs.lodoEnabled）与 lodo 专属属性乘数
- AI 缺失 EntityMob 继承的玩家索敌与近战、EntityAIJumping、EntityAIWait、EntityAIWatchClosest
- 缺少 parasite status/SKIN/SELFE 等同步数据与数字寄生虫 id
- 毒伤害治愈、火焰乘数、效果免疫、RAGE、载具免疫等基类伤害规则未移植
- 冰冻变体贴图（slodo）与脚步静音音效缺失
- 经验值 1 而非 XP_LiTTLE(4)；缺少死亡钩子（spawnCyst/spawnBeckon/leaveScent）

### `pri_longarms`（EntityShyco → LongarmsEntity，68.6%）
- 碰撞箱 1.0x3.0 vs 0.6x3.2、眼高默认 2.55 vs 2.7、追踪范围 8 vs 64、刷怪蛋颜色不一致
- 缺少 per-mob 启用开关（shycoEnabled）与 shyco 专属属性乘数，无数字寄生虫 id
- AI 缺失：水中跃击技能、EntityAIGetFollowers 招募、恐怖球技能(EntityAISkill id 21)、EntityAIBlockLight、EntityAIWait、EntityAIJumping、潜行/隐身索敌惩罚
- 冲击波波体伤害为 1.0x（原版 0.3x）、每目标仅命中一次、寿命与破坏硬度不匹配；AOE 缺少“命中寄生虫清除目标”分支
- 变体皮肤 5/6/7 与冰冻变体 120 全链路缺失（同步数据、finalizeSpawn、贴图、粒子、附加效果）
- 伤害侧缺失：单次伤害上限(damageCap=6)+RAGE、生命恢复、吞噬食物、击杀治疗、FEAR、毒治愈、效果免疫、PIVOT 转移

### `sim_horse`（EntityInfHorse → AssimilatedVariantEntity，69.7%）
- （无缺口摘要，见 raw JSON）

### `sim_human`（EntityInfHuman → SimHumanEntity，70.9%）
- （无缺口摘要，见 raw JSON）

### `sim_adventurer`（EntityInfPlayer → SimAdventurerEntity，70.9%）
- （无缺口摘要，见 raw JSON）

### `sim_squid`（EntityInfSquid → AssimilatedParasiteEntity，71%）
- （无缺口摘要，见 raw JSON）

### `sim_villager`（EntityInfVillager → AssimilatedVariantEntity，71.7%）
- （无缺口摘要，见 raw JSON）

### `sim_wolf`（EntityInfWolf → AssimilatedParasiteEntity，72.4%）
- （无缺口摘要，见 raw JSON）

### `sim_cow`（EntityInfCow → AssimilatedParasiteEntity，73.1%）
- （无缺口摘要，见 raw JSON）

### `sim_bigspider`（EntityDorpa → AssimilatedVariantEntity，74%）
- （无缺口摘要，见 raw JSON）

### `sim_sheep`（EntityInfSheep → AssimilatedParasiteEntity，75.2%）
- （无缺口摘要，见 raw JSON）
