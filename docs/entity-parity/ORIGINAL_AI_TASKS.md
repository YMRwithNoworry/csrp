# 原版生物 AI 任务地面真值（1.10.9）

> 生成时间：2026-09-23T15:41:19.042Z；由 `scripts/entity-parity/extract-original-ai-tasks.cjs` 从反编译源码解析。
> 口径：沿每只生物的原版继承链解析 `field_70714_bg`(tasks) / `field_70715_bh`(targetTasks) 的 `func_75776_a(优先级, 任务)` 注册，
> 含基类构造函数注册；`cond` 表示该注册位于条件块内（阶段/配置门控）。
> 原版未覆盖 `initEntityAI` 或覆盖时调用 `super` 的生物（17 只）另有 vanilla `EntityMob` 基线任务，见 `original-ai-baseline.json`。

- 生物数：127；任务注册总数：1254
- 零注册生物（AI 完全来自 vanilla EntityMob 或能力接口驱动）：无

## 逐生物任务表

### `abo_bodies`（EntityAboBodies）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityAboBodies |  | `EntityAboBodies.java:95` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityAboBodies |  | `EntityAboBodies.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityAboBodies | cond | `EntityAboBodies.java:52` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityAboBodies |  | `EntityAboBodies.java:96` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityAboBodies |  | `EntityAboBodies.java:97` |
| tasks | 8 | `EntityAILookIdle` | EntityAboBodies |  | `EntityAboBodies.java:98` |

### `abo_head`（EntityAboHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityAboHead |  | `EntityAboHead.java:78` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityAboHead |  | `EntityAboHead.java:36` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityAboHead | cond | `EntityAboHead.java:41` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityAboHead |  | `EntityAboHead.java:79` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityAboHead |  | `EntityAboHead.java:80` |
| tasks | 8 | `EntityAILookIdle` | EntityAboHead |  | `EntityAboHead.java:81` |

### `ada_arachnida`（EntityRanracAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityRanracAdapted |  | `EntityRanracAdapted.java:78` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityRanracAdapted |  | `EntityRanracAdapted.java:79` |
| tasks | 2 | `EntityAISkill` | EntityRanracAdapted |  | `EntityRanracAdapted.java:80` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityRanracAdapted |  | `EntityRanracAdapted.java:82` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityRanracAdapted |  | `EntityRanracAdapted.java:83` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityRanracAdapted |  | `EntityRanracAdapted.java:81` |
| tasks | 6 | `EntityAIGetFollowers` | EntityRanracAdapted | cond | `EntityRanracAdapted.java:88` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 9 | `EntityAIBlockResidue` | EntityRanracAdapted | cond | `EntityRanracAdapted.java:85` |

### `ada_bolster`（EntityBanoAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityBanoAdapted |  | `EntityBanoAdapted.java:71` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityBanoAdapted |  | `EntityBanoAdapted.java:72` |
| tasks | 2 | `EntityAISkill` | EntityBanoAdapted |  | `EntityBanoAdapted.java:73` |
| tasks | 2 | `EntityAIAttackMeleeStatusAOE` | EntityBanoAdapted |  | `EntityBanoAdapted.java:74` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIGiveEffectsArea` | EntityBanoAdapted |  | `EntityBanoAdapted.java:75` |
| tasks | 6 | `EntityAIGetFollowers` | EntityBanoAdapted | cond | `EntityBanoAdapted.java:81` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 9 | `EntityAIBlockResidue` | EntityBanoAdapted | cond | `EntityBanoAdapted.java:78` |

### `ada_burrower`（EntityZaaAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 2 | `EntityAIHurtByTarget` | EntityZaaAdapted |  | `EntityZaaAdapted.java:79` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityZaaAdapted |  | `EntityZaaAdapted.java:80` |
| tasks | 1 | `EntityAIFollowBodies` | EntityZaaAdapted |  | `EntityZaaAdapted.java:78` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityZaaAdapted |  | `EntityZaaAdapted.java:81` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `ada_devourer`（EntityLumAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLumAdapted |  | `EntityLumAdapted.java:66` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIAttackMeleeNotGround` | EntityLumAdapted |  | `EntityLumAdapted.java:67` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 6 | `AIMoveRandom` | EntityLumAdapted |  | `EntityLumAdapted.java:68` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `ada_longarms`（EntityShycoAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityShycoAdapted |  | `EntityShycoAdapted.java:86` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityShycoAdapted |  | `EntityShycoAdapted.java:87` |
| tasks | 2 | `EntityAISkill` | EntityShycoAdapted |  | `EntityShycoAdapted.java:88` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityShycoAdapted |  | `EntityShycoAdapted.java:89` |
| tasks | 2 | `EntityAIEvade` | EntityShycoAdapted | cond | `EntityShycoAdapted.java:96` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityShycoAdapted |  | `EntityShycoAdapted.java:90` |
| tasks | 6 | `EntityAIGetFollowers` | EntityShycoAdapted | cond | `EntityShycoAdapted.java:95` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 9 | `EntityAIBlockResidue` | EntityShycoAdapted | cond | `EntityShycoAdapted.java:92` |

### `ada_manducater`（EntityHullAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHullAdapted |  | `EntityHullAdapted.java:130` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityHullAdapted |  | `EntityHullAdapted.java:131` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityHullAdapted |  | `EntityHullAdapted.java:132` |
| tasks | 2 | `EntityAIEvade` | EntityHullAdapted |  | `EntityHullAdapted.java:133` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityHullAdapted |  | `EntityHullAdapted.java:134` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 9 | `EntityAIBlockResidue` | EntityHullAdapted | cond | `EntityHullAdapted.java:136` |

### `ada_reeker`（EntityNoglaAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityNoglaAdapted |  | `EntityNoglaAdapted.java:80` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityNoglaAdapted |  | `EntityNoglaAdapted.java:81` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityNoglaAdapted |  | `EntityNoglaAdapted.java:82` |
| tasks | 2 | `EntityAISkill` | EntityNoglaAdapted |  | `EntityNoglaAdapted.java:84` |
| tasks | 2 | `EntityAIEvade` | EntityNoglaAdapted | cond | `EntityNoglaAdapted.java:90` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityNoglaAdapted |  | `EntityNoglaAdapted.java:83` |
| tasks | 6 | `EntityAIGetFollowers` | EntityNoglaAdapted | cond | `EntityNoglaAdapted.java:89` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 9 | `EntityAIBlockResidue` | EntityNoglaAdapted | cond | `EntityNoglaAdapted.java:86` |

### `ada_summoner`（EntityCanraAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityCanraAdapted |  | `EntityCanraAdapted.java:92` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityCanraAdapted |  | `EntityCanraAdapted.java:93` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityCanraAdapted |  | `EntityCanraAdapted.java:94` |
| tasks | 2 | `EntityAISkill` | EntityCanraAdapted |  | `EntityCanraAdapted.java:95` |
| tasks | 2 | `EntityAIEvade` | EntityCanraAdapted | cond | `EntityCanraAdapted.java:104` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityCanraAdapted |  | `EntityCanraAdapted.java:97` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityCanraAdapted |  | `EntityCanraAdapted.java:98` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityCanraAdapted |  | `EntityCanraAdapted.java:96` |
| tasks | 6 | `EntityAIGetFollowers` | EntityCanraAdapted | cond | `EntityCanraAdapted.java:103` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 9 | `EntityAIBlockResidue` | EntityCanraAdapted | cond | `EntityCanraAdapted.java:100` |

### `ada_tozoon`（EntityWymoAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 2 | `EntityAIHurtByTarget` | EntityWymoAdapted |  | `EntityWymoAdapted.java:84` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityWymoAdapted |  | `EntityWymoAdapted.java:85` |
| tasks | 1 | `EntityAIFollowBodies` | EntityWymoAdapted |  | `EntityWymoAdapted.java:83` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityWymoAdapted |  | `EntityWymoAdapted.java:86` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `ada_vermin`（EntityIkiAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityIkiAdapted |  | `EntityIkiAdapted.java:84` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIFlightLimits` | EntityIkiAdapted | cond | `EntityIkiAdapted.java:61` |
| tasks | 3 | `EntityAIFlightAttack` | EntityIkiAdapted |  | `EntityIkiAdapted.java:85` |
| tasks | 5 | `AIBomb` | EntityIkiAdapted |  | `EntityIkiAdapted.java:86` |
| tasks | 7 | `AIMoveRandom` | EntityIkiAdapted |  | `EntityIkiAdapted.java:87` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `ada_viscera`（EntityGimAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityGimAdapted |  | `EntityGimAdapted.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityGimAdapted |  | `EntityGimAdapted.java:56` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityGimAdapted |  | `EntityGimAdapted.java:57` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityGimAdapted |  | `EntityGimAdapted.java:58` |
| tasks | 6 | `EntityAIGetFollowers` | EntityGimAdapted | cond | `EntityGimAdapted.java:63` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 9 | `EntityAIBlockResidue` | EntityGimAdapted | cond | `EntityGimAdapted.java:60` |

### `ada_yelloweye`（EntityEmanaAdapted）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityEmanaAdapted |  | `EntityEmanaAdapted.java:75` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted |  | `EntityPAdapted.java:42` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAdapted | cond | `EntityPAdapted.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 1 | `EntityAIAttackProjectile` | EntityEmanaAdapted |  | `EntityEmanaAdapted.java:79` |
| tasks | 2 | `EntityAIAttackMeleeNotGround` | EntityEmanaAdapted |  | `EntityEmanaAdapted.java:80` |
| tasks | 2 | `EntityAISkill` | EntityPAdapted | cond | `EntityPAdapted.java:50` |
| tasks | 3 | `EntityAIFlightLimits` | EntityEmanaAdapted | cond | `EntityEmanaAdapted.java:59` |
| tasks | 3 | `EntityAIFlightAttack` | EntityEmanaAdapted |  | `EntityEmanaAdapted.java:76` |
| tasks | 4 | `AIChargeAttack` | EntityEmanaAdapted |  | `EntityEmanaAdapted.java:77` |
| tasks | 6 | `AIMoveRandom` | EntityEmanaAdapted |  | `EntityEmanaAdapted.java:78` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `airscrew`（EntityLeer）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLeer |  | `EntityLeer.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAISkill` | EntityLeer |  | `EntityLeer.java:79` |
| tasks | 3 | `EntityAIFlightLimits` | EntityLeer | cond | `EntityLeer.java:63` |
| tasks | 3 | `EntityAIFlightAttack` | EntityLeer |  | `EntityLeer.java:78` |
| tasks | 4 | `EntityAIFlightLimits` | EntityLeer |  | `EntityLeer.java:81` |
| tasks | 6 | `AIMoveRandom` | EntityLeer |  | `EntityLeer.java:80` |

### `anc_dreadnaut`（EntityOronco）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityOronco |  | `EntityOronco.java:86` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAncient |  | `EntityPAncient.java:23` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAncient | cond | `EntityPAncient.java:31` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `AIMoveRandom` | EntityOronco |  | `EntityOronco.java:85` |
| tasks | 4 | `EntityAIAncientSummon` | EntityOronco |  | `EntityOronco.java:88` |
| tasks | 4 | `EntityAIFlightLimits` | EntityOronco |  | `EntityOronco.java:92` |
| tasks | 5 | `EntityAIAttackProjectile` | EntityOronco |  | `EntityOronco.java:87` |
| tasks | 5 | `EntityAIFlightAttack` | EntityOronco |  | `EntityOronco.java:93` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `anc_dreadnaut_ten`（EntityOroncoTen）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |

### `anc_overlord`（EntityTerla）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityTerla |  | `EntityTerla.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAncient |  | `EntityPAncient.java:23` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPAncient | cond | `EntityPAncient.java:31` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityTerla |  | `EntityTerla.java:59` |
| tasks | 2 | `EntityAIAttackMeleeStatusAOE` | EntityTerla |  | `EntityTerla.java:61` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityTerla |  | `EntityTerla.java:62` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityTerla |  | `EntityTerla.java:60` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `anc_pod`（EntityDropPod）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |

### `architect`（EntityTenn）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityTenn |  | `EntityTenn.java:69` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent |  | `EntityPPreeminent.java:48` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent | cond | `EntityPPreeminent.java:56` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIFlightLimits` | EntityTenn | cond | `EntityTenn.java:56` |
| tasks | 3 | `EntityAIFlightAttack` | EntityTenn |  | `EntityTenn.java:70` |
| tasks | 6 | `AIMoveRandom` | EntityTenn |  | `EntityTenn.java:71` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `beckon_si`（EntityVenkrol）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityVenkrol |  | `EntityVenkrol.java:61` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIBlockInfest` | EntityVenkrol | cond | `EntityVenkrol.java:39` |
| tasks | 4 | `EntityAINexusGrow` | EntityVenkrol |  | `EntityVenkrol.java:62` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `beckon_sii`（EntityVenkrolSII）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityVenkrolSII |  | `EntityVenkrolSII.java:60` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIBlockInfest` | EntityVenkrolSII | cond | `EntityVenkrolSII.java:38` |
| tasks | 4 | `EntityAINexusGrow` | EntityVenkrolSII |  | `EntityVenkrolSII.java:61` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `beckon_siii`（EntityVenkrolSIII）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityVenkrolSIII |  | `EntityVenkrolSIII.java:60` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIBlockInfest` | EntityVenkrolSIII | cond | `EntityVenkrolSIII.java:38` |
| tasks | 4 | `EntityAINexusGrow` | EntityVenkrolSIII |  | `EntityVenkrolSIII.java:61` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `beckon_siv`（EntityVenkrolSIV）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityVenkrolSIV |  | `EntityVenkrolSIV.java:85` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `bogle`（EntityLencia）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLencia |  | `EntityLencia.java:65` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent |  | `EntityPPreeminent.java:48` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent | cond | `EntityPPreeminent.java:56` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIFlightAttack` | EntityLencia |  | `EntityLencia.java:66` |
| tasks | 4 | `AIChargeAttack` | EntityLencia |  | `EntityLencia.java:68` |
| tasks | 4 | `EntityAIFlightLimits` | EntityLencia |  | `EntityLencia.java:71` |
| tasks | 4 | `EntityAIFlightLimits` | EntityLencia |  | `EntityLencia.java:72` |
| tasks | 5 | `EntityAIAttackProjectile` | EntityLencia |  | `EntityLencia.java:67` |
| tasks | 6 | `AIMoveRandom` | EntityLencia |  | `EntityLencia.java:69` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityLencia |  | `EntityLencia.java:70` |

### `bomber_heavy`（EntityJinjo）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityJinjo |  | `EntityJinjo.java:63` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent |  | `EntityPPreeminent.java:48` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent | cond | `EntityPPreeminent.java:56` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIFlightLimits` | EntityJinjo | cond | `EntityJinjo.java:48` |
| tasks | 3 | `EntityAIFlightAttack` | EntityJinjo |  | `EntityJinjo.java:64` |
| tasks | 4 | `AIChargeAttack` | EntityJinjo |  | `EntityJinjo.java:65` |
| tasks | 4 | `EntityAIFlightLimits` | EntityJinjo |  | `EntityJinjo.java:69` |
| tasks | 5 | `AIBomb` | EntityJinjo |  | `EntityJinjo.java:67` |
| tasks | 6 | `AIMoveRandom` | EntityJinjo |  | `EntityJinjo.java:66` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityJinjo |  | `EntityJinjo.java:68` |

### `bomber_light`（EntityOmboo）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityOmboo |  | `EntityOmboo.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure |  | `EntityPPure.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure | cond | `EntityPPure.java:65` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAISkill` | EntityPPure | cond | `EntityPPure.java:61` |
| tasks | 3 | `EntityAIFlightLimits` | EntityOmboo | cond | `EntityOmboo.java:45` |
| tasks | 3 | `EntityAIFlightAttack` | EntityOmboo |  | `EntityOmboo.java:59` |
| tasks | 4 | `AIChargeAttack` | EntityOmboo |  | `EntityOmboo.java:60` |
| tasks | 4 | `EntityAIFlightLimits` | EntityOmboo |  | `EntityOmboo.java:63` |
| tasks | 4 | `EntityAIFlightLimits` | EntityOmboo |  | `EntityOmboo.java:64` |
| tasks | 5 | `AIBomb` | EntityOmboo |  | `EntityOmboo.java:62` |
| tasks | 6 | `AIMoveRandom` | EntityOmboo |  | `EntityOmboo.java:61` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `buglin`（EntityLodo）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLodo |  | `EntityLodo.java:53` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityLodo |  | `EntityLodo.java:54` |
| tasks | 3 | `EntityAIAvoidEntity` | EntityLodo |  | `EntityLodo.java:55` |

### `carrier_colony`（EntityVesta）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityVesta |  | `EntityVesta.java:52` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent |  | `EntityPPreeminent.java:48` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent | cond | `EntityPPreeminent.java:56` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityVesta |  | `EntityVesta.java:53` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityVesta |  | `EntityVesta.java:54` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityVesta |  | `EntityVesta.java:55` |
| tasks | 3 | `EntityAIGiveEffectsArea` | EntityVesta |  | `EntityVesta.java:57` |
| tasks | 6 | `EntityAIGetFollowers` | EntityVesta |  | `EntityVesta.java:56` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `carrier_flying`（EntityButhol）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityButhol |  | `EntityButhol.java:105` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityButhol |  | `EntityButhol.java:57` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityButhol | cond | `EntityButhol.java:62` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIAttackSwell` | EntityButhol |  | `EntityButhol.java:106` |
| tasks | 3 | `EntityAIFlightAttack` | EntityButhol |  | `EntityButhol.java:107` |
| tasks | 4 | `AIChargeAttack` | EntityButhol |  | `EntityButhol.java:108` |
| tasks | 6 | `AIMoveRandom` | EntityButhol |  | `EntityButhol.java:109` |
| tasks | 8 | `EntityAILookIdle` | EntityButhol |  | `EntityButhol.java:110` |

### `carrier_heavy`（EntityRathol）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityRathol |  | `EntityRathol.java:86` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityRathol |  | `EntityRathol.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityRathol | cond | `EntityRathol.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 1 | `EntityAISwimming` | EntityRathol |  | `EntityRathol.java:87` |
| tasks | 2 | `EntityAIAttackSwell` | EntityRathol |  | `EntityRathol.java:88` |
| tasks | 4 | `EntityAIAttackMelee` | EntityRathol |  | `EntityRathol.java:89` |
| tasks | 6 | `EntityAILookIdle` | EntityRathol |  | `EntityRathol.java:90` |

### `carrier_light`（EntityGothol）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityGothol |  | `EntityGothol.java:86` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityGothol |  | `EntityGothol.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityGothol | cond | `EntityGothol.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 1 | `EntityAISwimming` | EntityGothol |  | `EntityGothol.java:87` |
| tasks | 2 | `EntityAIAttackSwell` | EntityGothol |  | `EntityGothol.java:88` |
| tasks | 4 | `EntityAIAttackMelee` | EntityGothol |  | `EntityGothol.java:89` |
| tasks | 6 | `EntityAILookIdle` | EntityGothol |  | `EntityGothol.java:90` |

### `carrier_worm`（EntityQuac）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 2 | `EntityAIHurtByTarget` | EntityQuac |  | `EntityQuac.java:130` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityQuac |  | `EntityQuac.java:71` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityQuac | cond | `EntityQuac.java:79` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityQuac |  | `EntityQuac.java:131` |
| tasks | 1 | `EntityAIFollowBodies` | EntityQuac |  | `EntityQuac.java:129` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityQuac |  | `EntityQuac.java:132` |

### `crux`（EntityCruxA）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityCruxA |  | `EntityCruxA.java:115` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityCruxA |  | `EntityCruxA.java:73` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityCruxA | cond | `EntityCruxA.java:79` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPCrude |  | `EntityPCrude.java:11` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityCruxA |  | `EntityCruxA.java:116` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityCruxA |  | `EntityCruxA.java:117` |
| tasks | 2 | `EntityAISkill` | EntityCruxA |  | `EntityCruxA.java:118` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityCruxA |  | `EntityCruxA.java:119` |

### `crux_incomplete`（EntityCruxB）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityCruxB |  | `EntityCruxB.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityCruxB | cond | `EntityCruxB.java:53` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityCruxB |  | `EntityCruxB.java:94` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityCruxB |  | `EntityCruxB.java:96` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityCruxB |  | `EntityCruxB.java:97` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityCruxB |  | `EntityCruxB.java:98` |
| tasks | 8 | `EntityAILookIdle` | EntityCruxB |  | `EntityCruxB.java:95` |

### `dispatcher_si`（EntityDod）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityDod |  | `EntityDod.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIDodAttack` | EntityDod |  | `EntityDod.java:48` |
| tasks | 4 | `EntityAINexusGrow` | EntityDod |  | `EntityDod.java:47` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `dispatcher_sii`（EntityDodSII）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityDodSII |  | `EntityDodSII.java:49` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIDodAttack` | EntityDodSII |  | `EntityDodSII.java:51` |
| tasks | 4 | `EntityAINexusGrow` | EntityDodSII |  | `EntityDodSII.java:50` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `dispatcher_siii`（EntityDodSIII）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityDodSIII |  | `EntityDodSIII.java:49` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIDodAttack` | EntityDodSIII |  | `EntityDodSIII.java:51` |
| tasks | 4 | `EntityAINexusGrow` | EntityDodSIII |  | `EntityDodSIII.java:50` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `dispatcher_siv`（EntityDodSIV）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityDodSIV |  | `EntityDodSIV.java:63` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIDodAttack` | EntityDodSIV |  | `EntityDodSIV.java:64` |
| tasks | 4 | `EntityAIAncientSummon` | EntityDodSIV |  | `EntityDodSIV.java:65` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `dispatcherten`（EntityDodT）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityDodT |  | `EntityDodT.java:53` |

### `draconite`（EntityHeblu）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHeblu |  | `EntityHeblu.java:107` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPDerived |  | `EntityPDerived.java:17` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPDerived | cond | `EntityPDerived.java:25` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityHeblu |  | `EntityHeblu.java:108` |
| tasks | 2 | `EntityAISkill` | EntityPCosmical |  | `EntityPCosmical.java:62` |
| tasks | 2 | `EntityAISkill` | EntityPCosmical |  | `EntityPCosmical.java:63` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityHeblu |  | `EntityHeblu.java:109` |
| tasks | 3 | `EntityAIFlightAttack` | EntityHeblu |  | `EntityHeblu.java:112` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityHeblu |  | `EntityHeblu.java:114` |
| tasks | 5 | `AIMoveRandom` | EntityHeblu |  | `EntityHeblu.java:110` |
| tasks | 6 | `AIFireballAttack` | EntityHeblu |  | `EntityHeblu.java:111` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityHeblu |  | `EntityHeblu.java:113` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `dredge`（EntityDone）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityDone |  | `EntityDone.java:122` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityDone |  | `EntityDone.java:62` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityDone | cond | `EntityDone.java:70` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityDone |  | `EntityDone.java:123` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityDone |  | `EntityDone.java:124` |
| tasks | 6 | `EntityAIGetFollowers` | EntityDone |  | `EntityDone.java:126` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityDone |  | `EntityDone.java:125` |

### `fer_bear`（EntityFerBear）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerBear |  | `EntityFerBear.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerBear |  | `EntityFerBear.java:52` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerBear |  | `EntityFerBear.java:53` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerBear |  | `EntityFerBear.java:54` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerBear |  | `EntityFerBear.java:56` |
| tasks | 8 | `EntityAILookIdle` | EntityFerBear |  | `EntityFerBear.java:55` |

### `fer_cow`（EntityFerCow）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerCow |  | `EntityFerCow.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerCow |  | `EntityFerCow.java:52` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerCow |  | `EntityFerCow.java:53` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerCow |  | `EntityFerCow.java:54` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerCow |  | `EntityFerCow.java:56` |
| tasks | 8 | `EntityAILookIdle` | EntityFerCow |  | `EntityFerCow.java:55` |

### `fer_enderman`（EntityFerEnderman）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerEnderman |  | `EntityFerEnderman.java:94` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerEnderman |  | `EntityFerEnderman.java:95` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerEnderman |  | `EntityFerEnderman.java:96` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerEnderman |  | `EntityFerEnderman.java:97` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerEnderman |  | `EntityFerEnderman.java:99` |
| tasks | 8 | `EntityAILookIdle` | EntityFerEnderman |  | `EntityFerEnderman.java:98` |

### `fer_horse`（EntityFerHorse）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerHorse |  | `EntityFerHorse.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerHorse |  | `EntityFerHorse.java:52` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerHorse |  | `EntityFerHorse.java:53` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerHorse |  | `EntityFerHorse.java:54` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerHorse |  | `EntityFerHorse.java:56` |
| tasks | 8 | `EntityAILookIdle` | EntityFerHorse |  | `EntityFerHorse.java:55` |

### `fer_human`（EntityFerHuman）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerHuman |  | `EntityFerHuman.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerHuman |  | `EntityFerHuman.java:52` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerHuman |  | `EntityFerHuman.java:53` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerHuman |  | `EntityFerHuman.java:54` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerHuman |  | `EntityFerHuman.java:56` |
| tasks | 8 | `EntityAILookIdle` | EntityFerHuman |  | `EntityFerHuman.java:55` |

### `fer_pig`（EntityFerPig）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerPig |  | `EntityFerPig.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerPig |  | `EntityFerPig.java:52` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerPig |  | `EntityFerPig.java:53` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerPig |  | `EntityFerPig.java:54` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerPig |  | `EntityFerPig.java:56` |
| tasks | 8 | `EntityAILookIdle` | EntityFerPig |  | `EntityFerPig.java:55` |

### `fer_sheep`（EntityFerSheep）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerSheep |  | `EntityFerSheep.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerSheep |  | `EntityFerSheep.java:52` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerSheep |  | `EntityFerSheep.java:53` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerSheep |  | `EntityFerSheep.java:54` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerSheep |  | `EntityFerSheep.java:56` |
| tasks | 8 | `EntityAILookIdle` | EntityFerSheep |  | `EntityFerSheep.java:55` |

### `fer_villager`（EntityFerVillager）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerVillager |  | `EntityFerVillager.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerVillager |  | `EntityFerVillager.java:52` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerVillager |  | `EntityFerVillager.java:53` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerVillager |  | `EntityFerVillager.java:54` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerVillager |  | `EntityFerVillager.java:56` |
| tasks | 8 | `EntityAILookIdle` | EntityFerVillager |  | `EntityFerVillager.java:55` |

### `fer_wolf`（EntityFerWolf）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFerWolf |  | `EntityFerWolf.java:52` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral |  | `EntityPFeral.java:46` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPFeral | cond | `EntityPFeral.java:51` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFerWolf |  | `EntityFerWolf.java:53` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFerWolf |  | `EntityFerWolf.java:54` |
| tasks | 2 | `EntityAIEvade` | EntityPFeral | cond | `EntityPFeral.java:69` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityFerWolf |  | `EntityFerWolf.java:55` |
| tasks | 6 | `EntityAIGetFollowers` | EntityFerWolf |  | `EntityFerWolf.java:57` |
| tasks | 8 | `EntityAILookIdle` | EntityFerWolf |  | `EntityFerWolf.java:56` |

### `gnat`（EntityAta）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityAta |  | `EntityAta.java:86` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityAta |  | `EntityAta.java:49` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityAta | cond | `EntityAta.java:54` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityAta |  | `EntityAta.java:84` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityAta |  | `EntityAta.java:87` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityAta |  | `EntityAta.java:88` |
| tasks | 3 | `EntityAILeapAtTarget` | EntityAta |  | `EntityAta.java:89` |
| tasks | 8 | `EntityAILookIdle` | EntityAta |  | `EntityAta.java:90` |

### `grunt`（EntityFlog）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityFlog |  | `EntityFlog.java:57` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure |  | `EntityPPure.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure | cond | `EntityPPure.java:65` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityFlog |  | `EntityFlog.java:58` |
| tasks | 0 | `EntityAISkill` | EntityFlog |  | `EntityFlog.java:62` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityFlog |  | `EntityFlog.java:59` |
| tasks | 2 | `EntityAIEvadeDash` | EntityFlog |  | `EntityFlog.java:63` |
| tasks | 2 | `EntityAISkill` | EntityPPure | cond | `EntityPPure.java:61` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityFlog |  | `EntityFlog.java:60` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `haunter`（EntityPheon）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityPheon |  | `EntityPheon.java:72` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent |  | `EntityPPreeminent.java:48` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent | cond | `EntityPPreeminent.java:56` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityPheon |  | `EntityPheon.java:73` |
| tasks | 2 | `EntityAIAttackMeleeStatusAOE` | EntityPheon |  | `EntityPheon.java:75` |
| tasks | 2 | `EntityAIEvadeDash` | EntityPheon |  | `EntityPheon.java:79` |
| tasks | 3 | `EntityAIFlightAttack` | EntityPheon |  | `EntityPheon.java:78` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityPheon |  | `EntityPheon.java:76` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityPheon |  | `EntityPheon.java:74` |
| tasks | 6 | `EntityAIAttackProjectile` | EntityPheon |  | `EntityPheon.java:77` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `heed`（EntityHeed）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHeed |  | `EntityHeed.java:83` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityHeed | cond | `EntityHeed.java:49` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPCrude |  | `EntityPCrude.java:11` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityHeed |  | `EntityHeed.java:84` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityHeed |  | `EntityHeed.java:85` |
| tasks | 2 | `EntityAISkill` | EntityHeed |  | `EntityHeed.java:87` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityHeed |  | `EntityHeed.java:86` |
| tasks | 6 | `EntityAIGetFollowers` | EntityHeed |  | `EntityHeed.java:88` |

### `hi_blaze`（EntityHiBlaze）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHiBlaze |  | `EntityHiBlaze.java:68` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPHijacked |  | `EntityPHijacked.java:27` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPHijacked | cond | `EntityPHijacked.java:32` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 1 | `EntityAIAttackProjectile` | EntityHiBlaze |  | `EntityHiBlaze.java:71` |
| tasks | 3 | `EntityAIFlightLimits` | EntityHiBlaze | cond | `EntityHiBlaze.java:52` |
| tasks | 3 | `EntityAIFlightAttack` | EntityHiBlaze |  | `EntityHiBlaze.java:69` |
| tasks | 6 | `AIMoveRandom` | EntityHiBlaze |  | `EntityHiBlaze.java:70` |

### `hi_golem`（EntityHiGolem）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHiGolem |  | `EntityHiGolem.java:73` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPHijacked |  | `EntityPHijacked.java:27` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPHijacked | cond | `EntityPHijacked.java:32` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityHiGolem |  | `EntityHiGolem.java:74` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityHiGolem |  | `EntityHiGolem.java:75` |
| tasks | 2 | `EntityAISkill` | EntityHiGolem |  | `EntityHiGolem.java:79` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityHiGolem |  | `EntityHiGolem.java:76` |
| tasks | 6 | `EntityAIGetFollowers` | EntityHiGolem |  | `EntityHiGolem.java:78` |
| tasks | 8 | `EntityAILookIdle` | EntityHiGolem |  | `EntityHiGolem.java:77` |

### `hi_skeleton`（EntityHiSkeleton）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHiSkeleton |  | `EntityHiSkeleton.java:49` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPHijacked |  | `EntityPHijacked.java:27` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPHijacked | cond | `EntityPHijacked.java:32` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityHiSkeleton |  | `EntityHiSkeleton.java:50` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityHiSkeleton |  | `EntityHiSkeleton.java:54` |
| tasks | 6 | `EntityAIGetFollowers` | EntityHiSkeleton |  | `EntityHiSkeleton.java:52` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityHiSkeleton |  | `EntityHiSkeleton.java:53` |
| tasks | 8 | `EntityAILookIdle` | EntityHiSkeleton |  | `EntityHiSkeleton.java:51` |

### `host`（EntityHost）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHost |  | `EntityHost.java:128` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityHost |  | `EntityHost.java:74` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityHost | cond | `EntityHost.java:82` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAISkill` | EntityHost |  | `EntityHost.java:132` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityHost |  | `EntityHost.java:129` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityHost |  | `EntityHost.java:131` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityHost |  | `EntityHost.java:130` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `hostii`（EntityHostII）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHostII |  | `EntityHostII.java:133` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityHostII |  | `EntityHostII.java:80` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityHostII | cond | `EntityHostII.java:88` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAISkill` | EntityHostII |  | `EntityHostII.java:137` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityHostII |  | `EntityHostII.java:134` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityHostII |  | `EntityHostII.java:136` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityHostII |  | `EntityHostII.java:135` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `incompleteform_medium`（EntityInhooM）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInhooM |  | `EntityInhooM.java:66` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityInhooM | cond | `EntityInhooM.java:36` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPCrude |  | `EntityPCrude.java:11` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityInhooM |  | `EntityInhooM.java:67` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInhooM |  | `EntityInhooM.java:68` |

### `incompleteform_small`（EntityInhooS）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInhooS |  | `EntityInhooS.java:66` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityInhooS | cond | `EntityInhooS.java:36` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPCrude |  | `EntityPCrude.java:11` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityInhooS |  | `EntityInhooS.java:67` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInhooS |  | `EntityInhooS.java:68` |

### `kirin`（EntityKirin）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityKirin |  | `EntityKirin.java:152` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPDerived |  | `EntityPDerived.java:17` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPDerived | cond | `EntityPDerived.java:25` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityKirin |  | `EntityKirin.java:153` |
| tasks | 2 | `EntityAIKirinBlink` | EntityKirin |  | `EntityKirin.java:154` |
| tasks | 2 | `EntityAISkill` | EntityKirin |  | `EntityKirin.java:156` |
| tasks | 2 | `EntityAISkill` | EntityKirin |  | `EntityKirin.java:157` |
| tasks | 2 | `EntityAISkill` | EntityPCosmical |  | `EntityPCosmical.java:62` |
| tasks | 2 | `EntityAISkill` | EntityPCosmical |  | `EntityPCosmical.java:63` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityKirin |  | `EntityKirin.java:155` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `kyphosis`（EntityTonro）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityTonro |  | `EntityTonro.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAISkill` | EntityTonro |  | `EntityTonro.java:58` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityTonro |  | `EntityTonro.java:56` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityTonro |  | `EntityTonro.java:57` |

### `lice`（EntityViin）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityViin |  | `EntityViin.java:89` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityViin |  | `EntityViin.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityViin | cond | `EntityViin.java:56` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIFlightAttack` | EntityViin |  | `EntityViin.java:88` |
| tasks | 4 | `AIChargeAttack` | EntityViin |  | `EntityViin.java:86` |
| tasks | 6 | `AIMoveRandom` | EntityViin |  | `EntityViin.java:87` |
| tasks | 8 | `EntityAILookIdle` | EntityViin |  | `EntityViin.java:90` |

### `mangler`（EntityNuuh）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityNuuh |  | `EntityNuuh.java:105` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityNuuh |  | `EntityNuuh.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityNuuh | cond | `EntityNuuh.java:58` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityNuuh |  | `EntityNuuh.java:98` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityNuuh |  | `EntityNuuh.java:100` |
| tasks | 2 | `EntityAIEvadeDash` | EntityNuuh |  | `EntityNuuh.java:104` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityNuuh |  | `EntityNuuh.java:101` |
| tasks | 3 | `EntityAILeapAtTarget` | EntityNuuh |  | `EntityNuuh.java:102` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityNuuh |  | `EntityNuuh.java:103` |

### `mar_bear`（EntitySpeBear）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntitySpeBear |  | `EntitySpeBear.java:76` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntitySpeBear |  | `EntitySpeBear.java:77` |
| tasks | 2 | `EntityAISkill` | EntitySpeBear |  | `EntitySpeBear.java:80` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntitySpeBear |  | `EntitySpeBear.java:78` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntitySpeBear |  | `EntitySpeBear.java:81` |
| tasks | 8 | `EntityAILookIdle` | EntitySpeBear |  | `EntitySpeBear.java:79` |

### `mar_cow`（EntitySpeCow）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntitySpeCow |  | `EntitySpeCow.java:67` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntitySpeCow |  | `EntitySpeCow.java:68` |
| tasks | 2 | `EntityAIAttackMeleeStatus` | EntitySpeCow |  | `EntitySpeCow.java:72` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntitySpeCow |  | `EntitySpeCow.java:73` |
| tasks | 6 | `EntityAIGetFollowers` | EntitySpeCow |  | `EntitySpeCow.java:70` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntitySpeCow |  | `EntitySpeCow.java:71` |
| tasks | 8 | `EntityAILookIdle` | EntitySpeCow |  | `EntitySpeCow.java:69` |

### `mar_enderman`（EntitySpeEnderman）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntitySpeEnderman |  | `EntitySpeEnderman.java:90` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntitySpeEnderman |  | `EntitySpeEnderman.java:91` |
| tasks | 2 | `EntityAIEvade` | EntitySpeEnderman |  | `EntitySpeEnderman.java:94` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntitySpeEnderman |  | `EntitySpeEnderman.java:92` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 8 | `EntityAILookIdle` | EntitySpeEnderman |  | `EntitySpeEnderman.java:93` |

### `mar_human`（EntitySpeHuman）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntitySpeHuman |  | `EntitySpeHuman.java:65` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntitySpeHuman |  | `EntitySpeHuman.java:66` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntitySpeHuman |  | `EntitySpeHuman.java:67` |
| tasks | 2 | `EntityAIEvade` | EntitySpeHuman |  | `EntitySpeHuman.java:71` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntitySpeHuman |  | `EntitySpeHuman.java:68` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIGetFollowers` | EntitySpeHuman |  | `EntitySpeHuman.java:70` |
| tasks | 8 | `EntityAILookIdle` | EntitySpeHuman |  | `EntitySpeHuman.java:69` |

### `mar_sheep`（EntitySpeSheep）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntitySpeSheep |  | `EntitySpeSheep.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntitySpeSheep |  | `EntitySpeSheep.java:59` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntitySpeSheep |  | `EntitySpeSheep.java:63` |
| tasks | 6 | `EntityAIGetFollowers` | EntitySpeSheep |  | `EntitySpeSheep.java:61` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntitySpeSheep |  | `EntitySpeSheep.java:62` |
| tasks | 8 | `EntityAILookIdle` | EntitySpeSheep |  | `EntitySpeSheep.java:60` |

### `mar_villager`（EntitySpeVillager）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntitySpeVillager |  | `EntitySpeVillager.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntitySpeVillager |  | `EntitySpeVillager.java:59` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntitySpeVillager |  | `EntitySpeVillager.java:63` |
| tasks | 6 | `EntityAIGetFollowers` | EntitySpeVillager |  | `EntitySpeVillager.java:61` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntitySpeVillager |  | `EntitySpeVillager.java:62` |
| tasks | 8 | `EntityAILookIdle` | EntitySpeVillager |  | `EntitySpeVillager.java:60` |

### `marauder`（EntityEsor）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityEsor |  | `EntityEsor.java:80` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure |  | `EntityPPure.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure | cond | `EntityPPure.java:65` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityEsor |  | `EntityEsor.java:77` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityEsor |  | `EntityEsor.java:81` |
| tasks | 2 | `EntityAISkill` | EntityEsor |  | `EntityEsor.java:79` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityEsor |  | `EntityEsor.java:82` |
| tasks | 2 | `EntityAIEvade` | EntityEsor |  | `EntityEsor.java:84` |
| tasks | 2 | `EntityAISkill` | EntityPPure | cond | `EntityPPure.java:61` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityEsor |  | `EntityEsor.java:83` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `monarch`（EntityOrch）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityOrch |  | `EntityOrch.java:70` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure |  | `EntityPPure.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure | cond | `EntityPPure.java:65` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityOrch |  | `EntityOrch.java:68` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityOrch |  | `EntityOrch.java:71` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityOrch |  | `EntityOrch.java:73` |
| tasks | 2 | `EntityAIEvadeDash` | EntityOrch |  | `EntityOrch.java:75` |
| tasks | 2 | `EntityAISkill` | EntityPPure | cond | `EntityPPure.java:61` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityOrch |  | `EntityOrch.java:74` |
| tasks | 6 | `EntityAIAttackProjectile` | EntityOrch |  | `EntityOrch.java:72` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `movingflesh`（EntityLesh）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 3 | `EntityAINearestAttackableTarget` | EntityLesh |  | `EntityLesh.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityLesh |  | `EntityLesh.java:72` |
| tasks | 1 | `EntityAILeshCombine` | EntityLesh |  | `EntityLesh.java:76` |
| tasks | 2 | `EntityAIAttackMelee` | EntityLesh |  | `EntityLesh.java:73` |
| tasks | 3 | `EntityAIAvoidEntity` | EntityLesh |  | `EntityLesh.java:78` |
| tasks | 6 | `EntityAIParasiteFollow` | EntityLesh |  | `EntityLesh.java:75` |
| tasks | 8 | `EntityAILookIdle` | EntityLesh |  | `EntityLesh.java:74` |

### `overseer`（EntityAlafha）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityAlafha |  | `EntityAlafha.java:79` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure |  | `EntityPPure.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure | cond | `EntityPPure.java:65` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 1 | `EntityAIAttackProjectile` | EntityAlafha |  | `EntityAlafha.java:86` |
| tasks | 2 | `EntityAIAttackMeleeNotGround` | EntityAlafha |  | `EntityAlafha.java:87` |
| tasks | 2 | `EntityAISkill` | EntityPPure | cond | `EntityPPure.java:61` |
| tasks | 3 | `EntityAIFlightLimits` | EntityAlafha | cond | `EntityAlafha.java:65` |
| tasks | 3 | `EntityAIFlightAttack` | EntityAlafha |  | `EntityAlafha.java:80` |
| tasks | 5 | `EntityAIAirVomitSummon` | EntityAlafha |  | `EntityAlafha.java:82` |
| tasks | 6 | `AIMoveRandom` | EntityAlafha |  | `EntityAlafha.java:81` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `pri_arachnida`（EntityRanrac）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityRanrac |  | `EntityRanrac.java:73` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityRanrac |  | `EntityRanrac.java:74` |
| tasks | 2 | `EntityAISkill` | EntityRanrac |  | `EntityRanrac.java:75` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityRanrac |  | `EntityRanrac.java:77` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityRanrac |  | `EntityRanrac.java:78` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityRanrac |  | `EntityRanrac.java:76` |
| tasks | 6 | `EntityAIGetFollowers` | EntityRanrac |  | `EntityRanrac.java:80` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityRanrac |  | `EntityRanrac.java:79` |

### `pri_bolster`（EntityBano）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityBano |  | `EntityBano.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityBano |  | `EntityBano.java:56` |
| tasks | 2 | `EntityAIAttackMelee` | EntityBano |  | `EntityBano.java:57` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIGiveEffectsArea` | EntityBano |  | `EntityBano.java:58` |
| tasks | 6 | `EntityAIGetFollowers` | EntityBano |  | `EntityBano.java:60` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityBano |  | `EntityBano.java:59` |

### `pri_burrower`（EntityZaa）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 2 | `EntityAIHurtByTarget` | EntityZaa |  | `EntityZaa.java:79` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityZaa |  | `EntityZaa.java:80` |
| tasks | 1 | `EntityAIFollowBodies` | EntityZaa |  | `EntityZaa.java:78` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityZaa |  | `EntityZaa.java:81` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `pri_devourer`（EntityLum）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLum |  | `EntityLum.java:66` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIAttackMeleeNotGround` | EntityLum |  | `EntityLum.java:67` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 6 | `AIMoveRandom` | EntityLum |  | `EntityLum.java:68` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `pri_longarms`（EntityShyco）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityShyco |  | `EntityShyco.java:64` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityShyco |  | `EntityShyco.java:65` |
| tasks | 2 | `EntityAISkill` | EntityShyco |  | `EntityShyco.java:66` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityShyco |  | `EntityShyco.java:67` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityShyco |  | `EntityShyco.java:68` |
| tasks | 6 | `EntityAIGetFollowers` | EntityShyco |  | `EntityShyco.java:70` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityShyco |  | `EntityShyco.java:69` |

### `pri_manducater`（EntityHull）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityHull |  | `EntityHull.java:65` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityHull |  | `EntityHull.java:66` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityHull |  | `EntityHull.java:67` |
| tasks | 2 | `EntityAIEvade` | EntityHull |  | `EntityHull.java:68` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityHull |  | `EntityHull.java:69` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityHull |  | `EntityHull.java:70` |

### `pri_reeker`（EntityNogla）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityNogla |  | `EntityNogla.java:93` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityNogla |  | `EntityNogla.java:94` |
| tasks | 1 | `EntityAIDiveBomb` | EntityNogla |  | `EntityNogla.java:95` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityNogla |  | `EntityNogla.java:96` |
| tasks | 2 | `EntityAISkill` | EntityNogla |  | `EntityNogla.java:99` |
| tasks | 2 | `EntityAIEvade` | EntityNogla |  | `EntityNogla.java:101` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityNogla |  | `EntityNogla.java:97` |
| tasks | 6 | `EntityAIGetFollowers` | EntityNogla |  | `EntityNogla.java:100` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityNogla |  | `EntityNogla.java:98` |

### `pri_summoner`（EntityCanra）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityCanra |  | `EntityCanra.java:77` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityCanra |  | `EntityCanra.java:78` |
| tasks | 2 | `EntityAISkill` | EntityCanra |  | `EntityCanra.java:79` |
| tasks | 2 | `EntityAIEvade` | EntityCanra |  | `EntityCanra.java:85` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityCanra |  | `EntityCanra.java:81` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityCanra |  | `EntityCanra.java:82` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityCanra |  | `EntityCanra.java:80` |
| tasks | 6 | `EntityAIGetFollowers` | EntityCanra |  | `EntityCanra.java:84` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityCanra |  | `EntityCanra.java:83` |

### `pri_tozoon`（EntityWymo）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 2 | `EntityAIHurtByTarget` | EntityWymo |  | `EntityWymo.java:84` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityWymo |  | `EntityWymo.java:85` |
| tasks | 1 | `EntityAIFollowBodies` | EntityWymo |  | `EntityWymo.java:83` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityWymo |  | `EntityWymo.java:86` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `pri_vermin`（EntityIki）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityIki |  | `EntityIki.java:89` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIFlightLimits` | EntityIki | cond | `EntityIki.java:63` |
| tasks | 3 | `EntityAIFlightAttack` | EntityIki |  | `EntityIki.java:90` |
| tasks | 4 | `AIChargeAttack` | EntityIki |  | `EntityIki.java:91` |
| tasks | 5 | `AIBomb` | EntityIki |  | `EntityIki.java:92` |
| tasks | 7 | `AIMoveRandom` | EntityIki |  | `EntityIki.java:93` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `pri_viscera`（EntityGim）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityGim |  | `EntityGim.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityGim |  | `EntityGim.java:48` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityGim |  | `EntityGim.java:49` |
| tasks | 2 | `EntityAIEvade` | EntityGim |  | `EntityGim.java:53` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityGim |  | `EntityGim.java:50` |
| tasks | 6 | `EntityAIGetFollowers` | EntityGim |  | `EntityGim.java:52` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityGim |  | `EntityGim.java:51` |

### `pri_yelloweye`（EntityEmana）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityEmana |  | `EntityEmana.java:71` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive |  | `EntityPPrimitive.java:47` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPrimitive | cond | `EntityPPrimitive.java:59` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 1 | `EntityAIAttackProjectile` | EntityEmana |  | `EntityEmana.java:74` |
| tasks | 2 | `EntityAISkill` | EntityPPrimitive | cond | `EntityPPrimitive.java:55` |
| tasks | 3 | `EntityAIFlightLimits` | EntityEmana | cond | `EntityEmana.java:55` |
| tasks | 3 | `EntityAIFlightAttack` | EntityEmana |  | `EntityEmana.java:72` |
| tasks | 6 | `AIMoveRandom` | EntityEmana |  | `EntityEmana.java:73` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `rooter_si`（EntityLeem）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLeem |  | `EntityLeem.java:51` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 4 | `EntityAINexusGrow` | EntityLeem |  | `EntityLeem.java:52` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `rooter_sii`（EntityLeemSII）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLeemSII |  | `EntityLeemSII.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 4 | `EntityAINexusGrow` | EntityLeemSII |  | `EntityLeemSII.java:56` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `rooter_siii`（EntityLeemSIII）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLeemSIII |  | `EntityLeemSIII.java:61` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 4 | `EntityAINexusGrow` | EntityLeemSIII |  | `EntityLeemSIII.java:62` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `rooter_siv`（EntityLeemSIV）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityLeemSIV |  | `EntityLeemSIV.java:60` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `rooterball`（EntityLeemB）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `rupter`（EntityMudo）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityMudo |  | `EntityMudo.java:189` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityMudo |  | `EntityMudo.java:76` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityMudo | cond | `EntityMudo.java:82` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityMudo | cond | `EntityMudo.java:109` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityMudo |  | `EntityMudo.java:130` |
| targetTasks | 4 | `EntityAINearestAttackableTarget` | EntityMudo | cond | `EntityMudo.java:195` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityMudo |  | `EntityMudo.java:187` |
| tasks | 0 | `EntityAISwimming` | EntityMudo |  | `EntityMudo.java:190` |
| tasks | 2 | `EntityAIMudoInfest` | EntityMudo |  | `EntityMudo.java:153` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityMudo |  | `EntityMudo.java:191` |
| tasks | 3 | `EntityAILeapAtTarget` | EntityMudo |  | `EntityMudo.java:192` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityMudo |  | `EntityMudo.java:154` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityMudo |  | `EntityMudo.java:155` |
| tasks | 8 | `EntityAILookIdle` | EntityMudo |  | `EntityMudo.java:193` |

### `seeker`（EntitySoo）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntitySoo |  | `EntitySoo.java:68` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure |  | `EntityPPure.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure | cond | `EntityPPure.java:65` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAISkill` | EntityPPure | cond | `EntityPPure.java:61` |
| tasks | 3 | `EntityAIFlightLimits` | EntitySoo | cond | `EntitySoo.java:54` |
| tasks | 3 | `EntityAIFlightAttack` | EntitySoo |  | `EntitySoo.java:69` |
| tasks | 6 | `AIMoveRandom` | EntitySoo |  | `EntitySoo.java:70` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `seizer`（EntityNak）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityNak |  | `EntityNak.java:64` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityNak |  | `EntityNak.java:65` |

### `sentry`（EntityUnvo）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityUnvo |  | `EntityUnvo.java:45` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIAttackProjectile` | EntityUnvo |  | `EntityUnvo.java:46` |
| tasks | 3 | `EntityAIAttackMelee` | EntityUnvo |  | `EntityUnvo.java:47` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityUnvo |  | `EntityUnvo.java:48` |

### `sim_adventurer`（EntityInfPlayer）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfPlayer |  | `EntityInfPlayer.java:65` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfPlayer |  | `EntityInfPlayer.java:66` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfPlayer |  | `EntityInfPlayer.java:67` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIGetFollowers` | EntityInfPlayer |  | `EntityInfPlayer.java:69` |
| tasks | 8 | `EntityAILookIdle` | EntityInfPlayer |  | `EntityInfPlayer.java:68` |

### `sim_adventurerhead`（EntityInfPlayerHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:62` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:73` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:60` |
| tasks | 0 | `EntityAISwimming` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:63` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:65` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:66` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:67` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:68` |
| tasks | 8 | `EntityAILookIdle` | EntityInfPlayerHead |  | `EntityInfPlayerHead.java:64` |

### `sim_bear`（EntityInfBear）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfBear |  | `EntityInfBear.java:60` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfBear |  | `EntityInfBear.java:61` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfBear |  | `EntityInfBear.java:62` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIGetFollowers` | EntityInfBear |  | `EntityInfBear.java:64` |
| tasks | 8 | `EntityAILookIdle` | EntityInfBear |  | `EntityInfBear.java:63` |

### `sim_bigspider`（EntityDorpa）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityDorpa |  | `EntityDorpa.java:56` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityDorpa |  | `EntityDorpa.java:57` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityDorpa |  | `EntityDorpa.java:58` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIAttackProjectile` | EntityDorpa |  | `EntityDorpa.java:60` |
| tasks | 6 | `EntityAIGetFollowers` | EntityDorpa |  | `EntityDorpa.java:61` |
| tasks | 8 | `EntityAILookIdle` | EntityDorpa |  | `EntityDorpa.java:59` |

### `sim_cow`（EntityInfCow）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfCow |  | `EntityInfCow.java:70` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfCow |  | `EntityInfCow.java:71` |
| tasks | 2 | `EntityAISkill` | EntityInfCow |  | `EntityInfCow.java:75` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfCow |  | `EntityInfCow.java:72` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIGetFollowers` | EntityInfCow |  | `EntityInfCow.java:74` |
| tasks | 8 | `EntityAILookIdle` | EntityInfCow |  | `EntityInfCow.java:73` |

### `sim_cowhead`（EntityInfCowHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfCowHead |  | `EntityInfCowHead.java:63` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfCowHead |  | `EntityInfCowHead.java:86` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfCowHead |  | `EntityInfCowHead.java:60` |
| tasks | 0 | `EntityAISwimming` | EntityInfCowHead |  | `EntityInfCowHead.java:64` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfCowHead |  | `EntityInfCowHead.java:62` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfCowHead |  | `EntityInfCowHead.java:66` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityInfCowHead |  | `EntityInfCowHead.java:67` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityInfCowHead |  | `EntityInfCowHead.java:68` |
| tasks | 8 | `EntityAILookIdle` | EntityInfCowHead |  | `EntityInfCowHead.java:65` |

### `sim_dragone`（EntityInfDragonE）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfDragonE |  | `EntityInfDragonE.java:107` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfDragonE |  | `EntityInfDragonE.java:108` |
| tasks | 2 | `EntityAISkill` | EntityInfDragonE |  | `EntityInfDragonE.java:113` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityInfDragonE |  | `EntityInfDragonE.java:109` |
| tasks | 3 | `EntityAIFlightAttack` | EntityInfDragonE |  | `EntityInfDragonE.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `AIMoveRandom` | EntityInfDragonE |  | `EntityInfDragonE.java:110` |
| tasks | 6 | `AIFireballAttack` | EntityInfDragonE |  | `EntityInfDragonE.java:111` |

### `sim_dragonehead`（EntityInfDragonEHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfDragonEHead |  | `EntityInfDragonEHead.java:50` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfDragonEHead |  | `EntityInfDragonEHead.java:55` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityInfDragonEHead |  | `EntityInfDragonEHead.java:51` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfDragonEHead |  | `EntityInfDragonEHead.java:53` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfDragonEHead |  | `EntityInfDragonEHead.java:54` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `AIFireballAttack` | EntityInfDragonEHead |  | `EntityInfDragonEHead.java:49` |
| tasks | 8 | `EntityAILookIdle` | EntityInfDragonEHead |  | `EntityInfDragonEHead.java:52` |

### `sim_enderman`（EntityInfEnderman）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfEnderman |  | `EntityInfEnderman.java:101` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfEnderman |  | `EntityInfEnderman.java:102` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfEnderman |  | `EntityInfEnderman.java:103` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 8 | `EntityAILookIdle` | EntityInfEnderman |  | `EntityInfEnderman.java:104` |

### `sim_endermanhead`（EntityInfEndermanHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfEndermanHead |  | `EntityInfEndermanHead.java:88` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfEndermanHead |  | `EntityInfEndermanHead.java:93` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfEndermanHead |  | `EntityInfEndermanHead.java:86` |
| tasks | 0 | `EntityAISwimming` | EntityInfEndermanHead |  | `EntityInfEndermanHead.java:89` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfEndermanHead |  | `EntityInfEndermanHead.java:91` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfEndermanHead |  | `EntityInfEndermanHead.java:92` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 8 | `EntityAILookIdle` | EntityInfEndermanHead |  | `EntityInfEndermanHead.java:90` |

### `sim_horse`（EntityInfHorse）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfHorse |  | `EntityInfHorse.java:68` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfHorse |  | `EntityInfHorse.java:69` |
| tasks | 2 | `EntityAIAttackSwell` | EntityInfHorse |  | `EntityInfHorse.java:73` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfHorse |  | `EntityInfHorse.java:70` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIGetFollowers` | EntityInfHorse |  | `EntityInfHorse.java:72` |
| tasks | 8 | `EntityAILookIdle` | EntityInfHorse |  | `EntityInfHorse.java:71` |

### `sim_horsehead`（EntityInfHorseHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfHorseHead |  | `EntityInfHorseHead.java:62` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfHorseHead |  | `EntityInfHorseHead.java:86` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfHorseHead |  | `EntityInfHorseHead.java:60` |
| tasks | 0 | `EntityAISwimming` | EntityInfHorseHead |  | `EntityInfHorseHead.java:63` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfHorseHead |  | `EntityInfHorseHead.java:65` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfHorseHead |  | `EntityInfHorseHead.java:66` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityInfHorseHead |  | `EntityInfHorseHead.java:67` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityInfHorseHead |  | `EntityInfHorseHead.java:68` |
| tasks | 8 | `EntityAILookIdle` | EntityInfHorseHead |  | `EntityInfHorseHead.java:64` |

### `sim_human`（EntityInfHuman）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfHuman |  | `EntityInfHuman.java:116` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfHuman |  | `EntityInfHuman.java:117` |
| tasks | 1 | `EntityAIOpenDoor` | EntityInfHuman |  | `EntityInfHuman.java:118` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityInfHuman |  | `EntityInfHuman.java:119` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfHuman |  | `EntityInfHuman.java:120` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAICircleGroup` | EntityInfHuman |  | `EntityInfHuman.java:121` |
| tasks | 5 | `EntityAIGetFollowers` | EntityInfHuman |  | `EntityInfHuman.java:122` |
| tasks | 6 | `EntityAILookIdle` | EntityInfHuman |  | `EntityInfHuman.java:123` |

### `sim_humanhead`（EntityInfHumanHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfHumanHead |  | `EntityInfHumanHead.java:63` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfHumanHead |  | `EntityInfHumanHead.java:61` |
| tasks | 0 | `EntityAISwimming` | EntityInfHumanHead |  | `EntityInfHumanHead.java:64` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfHumanHead |  | `EntityInfHumanHead.java:66` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfHumanHead |  | `EntityInfHumanHead.java:67` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityInfHumanHead |  | `EntityInfHumanHead.java:68` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityInfHumanHead |  | `EntityInfHumanHead.java:69` |
| tasks | 8 | `EntityAILookIdle` | EntityInfHumanHead |  | `EntityInfHumanHead.java:65` |

### `sim_pig`（EntityInfPig）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfPig |  | `EntityInfPig.java:59` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfPig |  | `EntityInfPig.java:60` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfPig |  | `EntityInfPig.java:61` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIGetFollowers` | EntityInfPig |  | `EntityInfPig.java:63` |
| tasks | 8 | `EntityAILookIdle` | EntityInfPig |  | `EntityInfPig.java:62` |

### `sim_pighead`（EntityInfPigHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfPigHead |  | `EntityInfPigHead.java:62` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfPigHead |  | `EntityInfPigHead.java:86` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfPigHead |  | `EntityInfPigHead.java:60` |
| tasks | 0 | `EntityAISwimming` | EntityInfPigHead |  | `EntityInfPigHead.java:63` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfPigHead |  | `EntityInfPigHead.java:65` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfPigHead |  | `EntityInfPigHead.java:66` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityInfPigHead |  | `EntityInfPigHead.java:67` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityInfPigHead |  | `EntityInfPigHead.java:68` |
| tasks | 8 | `EntityAILookIdle` | EntityInfPigHead |  | `EntityInfPigHead.java:64` |

### `sim_sheep`（EntityInfSheep）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfSheep |  | `EntityInfSheep.java:69` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfSheep |  | `EntityInfSheep.java:70` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfSheep |  | `EntityInfSheep.java:71` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIGetFollowers` | EntityInfSheep |  | `EntityInfSheep.java:73` |
| tasks | 8 | `EntityAILookIdle` | EntityInfSheep |  | `EntityInfSheep.java:72` |

### `sim_sheephead`（EntityInfSheepHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfSheepHead |  | `EntityInfSheepHead.java:62` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfSheepHead |  | `EntityInfSheepHead.java:73` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfSheepHead |  | `EntityInfSheepHead.java:60` |
| tasks | 0 | `EntityAISwimming` | EntityInfSheepHead |  | `EntityInfSheepHead.java:63` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfSheepHead |  | `EntityInfSheepHead.java:65` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfSheepHead |  | `EntityInfSheepHead.java:66` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityInfSheepHead |  | `EntityInfSheepHead.java:67` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityInfSheepHead |  | `EntityInfSheepHead.java:68` |
| tasks | 8 | `EntityAILookIdle` | EntityInfSheepHead |  | `EntityInfSheepHead.java:64` |

### `sim_squid`（EntityInfSquid）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfSquid |  | `EntityInfSquid.java:61` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 2 | `EntityAIAttackMeleeNotGround` | EntityInfSquid |  | `EntityInfSquid.java:62` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `AIMoveRandom` | EntityInfSquid |  | `EntityInfSquid.java:63` |

### `sim_villager`（EntityInfVillager）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfVillager |  | `EntityInfVillager.java:71` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfVillager |  | `EntityInfVillager.java:72` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityInfVillager |  | `EntityInfVillager.java:73` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfVillager |  | `EntityInfVillager.java:74` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 6 | `EntityAIGetFollowers` | EntityInfVillager |  | `EntityInfVillager.java:76` |
| tasks | 8 | `EntityAILookIdle` | EntityInfVillager |  | `EntityInfVillager.java:75` |

### `sim_villagerhead`（EntityInfVillagerHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:64` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:75` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:62` |
| tasks | 0 | `EntityAISwimming` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:65` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:67` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:68` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:69` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:70` |
| tasks | 8 | `EntityAILookIdle` | EntityInfVillagerHead |  | `EntityInfVillagerHead.java:66` |

### `sim_wolf`（EntityInfWolf）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfWolf |  | `EntityInfWolf.java:64` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityInfWolf |  | `EntityInfWolf.java:65` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfWolf |  | `EntityInfWolf.java:66` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAILeapAtTarget` | EntityInfWolf |  | `EntityInfWolf.java:68` |
| tasks | 6 | `EntityAIGetFollowers` | EntityInfWolf |  | `EntityInfWolf.java:69` |
| tasks | 8 | `EntityAILookIdle` | EntityInfWolf |  | `EntityInfWolf.java:67` |

### `sim_wolfhead`（EntityInfWolfHead）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityInfWolfHead |  | `EntityInfWolfHead.java:62` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected |  | `EntityPInfected.java:58` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPInfected | cond | `EntityPInfected.java:63` |
| targetTasks | 5 | `EntityAINearestAttackableTarget` | EntityInfWolfHead |  | `EntityInfWolfHead.java:73` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityInfWolfHead |  | `EntityInfWolfHead.java:60` |
| tasks | 0 | `EntityAISwimming` | EntityInfWolfHead |  | `EntityInfWolfHead.java:63` |
| tasks | 2 | `EntityAILeapAtTarget` | EntityInfWolfHead |  | `EntityInfWolfHead.java:65` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityInfWolfHead |  | `EntityInfWolfHead.java:66` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected | cond | `EntityPInfected.java:112` |
| tasks | 3 | `EntityAIInfectedSearch` | EntityPInfected |  | `EntityPInfected.java:349` |
| tasks | 4 | `EntityAIAvoidOrAttack` | EntityInfWolfHead |  | `EntityInfWolfHead.java:67` |
| tasks | 5 | `EntityAIAvoidEntityStatus` | EntityInfWolfHead |  | `EntityInfWolfHead.java:68` |
| tasks | 8 | `EntityAILookIdle` | EntityInfWolfHead |  | `EntityInfWolfHead.java:64` |

### `succor`（EntityFlam）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent |  | `EntityPPreeminent.java:48` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent | cond | `EntityPPreeminent.java:56` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIFlightLimits` | EntityFlam | cond | `EntityFlam.java:59` |
| tasks | 4 | `AIChargeAttack` | EntityFlam |  | `EntityFlam.java:71` |
| tasks | 6 | `AIMoveRandom` | EntityFlam |  | `EntityFlam.java:70` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `thrall`（EntityMes）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityMes |  | `EntityMes.java:113` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityMes |  | `EntityMes.java:59` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityMes | cond | `EntityMes.java:67` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityMes |  | `EntityMes.java:114` |
| tasks | 3 | `EntityAIAttackMeleeStatus` | EntityMes |  | `EntityMes.java:115` |
| tasks | 6 | `EntityAIGetFollowers` | EntityMes |  | `EntityMes.java:117` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityMes |  | `EntityMes.java:116` |

### `vigilante`（EntityAnged）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityAnged |  | `EntityAnged.java:59` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure |  | `EntityPPure.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure | cond | `EntityPPure.java:65` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityAnged |  | `EntityAnged.java:60` |
| tasks | 2 | `EntityAIAttackMeleeStatus` | EntityAnged |  | `EntityAnged.java:62` |
| tasks | 2 | `EntityAISkill` | EntityPPure | cond | `EntityPPure.java:61` |
| tasks | 4 | `EntityAIAttackRangedStatus` | EntityAnged |  | `EntityAnged.java:63` |
| tasks | 6 | `EntityAIAttackMeleeRangeSwitch` | EntityAnged |  | `EntityAnged.java:61` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `warden`（EntityGanro）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityGanro |  | `EntityGanro.java:82` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure |  | `EntityPPure.java:53` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPure | cond | `EntityPPure.java:65` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISkill` | EntityGanro |  | `EntityGanro.java:79` |
| tasks | 0 | `EntityAISwimmingDiving` | EntityGanro |  | `EntityGanro.java:83` |
| tasks | 2 | `EntityAISkill` | EntityGanro |  | `EntityGanro.java:81` |
| tasks | 2 | `EntityAIWaterLeapAtTargetStatus` | EntityGanro |  | `EntityGanro.java:84` |
| tasks | 2 | `EntityAISkill` | EntityGanro |  | `EntityGanro.java:86` |
| tasks | 2 | `EntityAIEvadeDash` | EntityGanro |  | `EntityGanro.java:87` |
| tasks | 2 | `EntityAISkill` | EntityPPure | cond | `EntityPPure.java:61` |
| tasks | 3 | `EntityAIAttackMeleeStatusAOE` | EntityGanro |  | `EntityGanro.java:85` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `worker`（EntityKol）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityKol |  | `EntityKol.java:53` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 0 | `EntityAISwimming` | EntityKol |  | `EntityKol.java:54` |
| tasks | 8 | `EntityAILookIdle` | EntityKol |  | `EntityKol.java:55` |

### `worm`（EntityRof）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:54` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary | cond | `EntityPStationary.java:55` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPStationary |  | `EntityPStationary.java:77` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |

### `wraith`（EntityElvia）

| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |
| --- | ---: | --- | --- | --- | --- |
| targetTasks | 1 | `EntityAIHurtByTarget` | EntityElvia |  | `EntityElvia.java:68` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent |  | `EntityPPreeminent.java:48` |
| targetTasks | 4 | `EntityAINearestAttackableTargetStatus` | EntityPPreeminent | cond | `EntityPPreeminent.java:56` |
| targetTasks | 5 | `EntityAINearestAttackableTargetStatus` | EntityParasiteBase | cond | `EntityParasiteBase.java:243` |
| tasks | 3 | `EntityAIFlightAttack` | EntityElvia |  | `EntityElvia.java:69` |
| tasks | 4 | `AIChargeAttack` | EntityElvia |  | `EntityElvia.java:71` |
| tasks | 4 | `EntityAIFlightLimits` | EntityElvia |  | `EntityElvia.java:74` |
| tasks | 4 | `EntityAIFlightLimits` | EntityElvia |  | `EntityElvia.java:75` |
| tasks | 5 | `EntityAIAttackProjectile` | EntityElvia |  | `EntityElvia.java:70` |
| tasks | 6 | `AIMoveRandom` | EntityElvia |  | `EntityElvia.java:72` |
| tasks | 7 | `EntityAIBlockLight` | EntityPMalleable | cond | `EntityPMalleable.java:92` |
| tasks | 8 | `EntityAILookIdle` | EntityElvia |  | `EntityElvia.java:73` |
