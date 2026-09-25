# 生物还原缺口聚类（补齐批次依据）

> 生成时间：2026-09-23T15:06:17.819Z；来源：`docs/entity-parity/raw/*.json`（8 只生物）。
> 「系统性缺口」= 被 ≥3 只生物共享的 missing/partial 条款，这些应作为共用系统一次性补齐；其余作为逐生物条目。

## 系统性缺口

### ai

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| tasks.addTask(5, EntityAIWanderStatus(1.0, 120, 0.001, true)) | 8 | 8/10 | `buglin` `pri_longarms` `sim_bigspider` `sim_cow` `sim_human` |
| 继承 EntityMob：tasks.addTask(7, EntityAIWander) | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 继承 EntityMob：tasks.addTask(8, EntityAIWatchClosest(EntityPlayer, 8)) | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| EntityParasiteBase 构造：tasks.addTask(0, EntityAIWait) | 6 | 6/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| EntityParasiteBase 构造：tasks.addTask(5, EntityAIWanderStatus(1.0, 120, 0.001F, true)) | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| EntityPInfected 构造：target 4 NearestAttackableTargetStatus(EntityPlayer, sneakPen 0.8, inviPen 0.7) | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| EntityPInfected 构造：SRPConfig.mobattacking 时 target 4 EntityLiving（排除水栖/动物/村民+黑名单） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |

### animation

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| 受击/自爆闪白（getSelfeFlashIntensity(fuseTime=40) 驱动缩放脉动） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 控制器注册：ModelSRP.func_78087_a 每帧姿势计算（含 underground 起跳/埋地） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |

### attributes

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| 跟随范围 SRPConfig.infectedFollow = 16 | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 全局/单体倍率 SRPAttributes 初始化时全局倍率 × per-mob 倍率 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 阶段属性加成 evolutionParasiteStatIncrease（phase ≥ 阈值时生命/护甲/攻击 ×(1+v)） | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 基因加成 applyGene（最小伤害/伤害上限/穿墙/疾跑/水跃/技能/攻击速度） | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 经验 field_70728_aV = SRPAttributes.XP_INFECTED = 8 | 5 | 0/5 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_wolf` |
| 移动速度 0.2 | 4 | 0/4 | `sim_cow` `sim_sheep` `sim_squid` `sim_wolf` |

### behaviors

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| 近战附加最小伤害 attackEntityAsMobMinimum(MiniDamage = SRPConfig.infectedMinDamage 0.5) | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 命中玩家时偷取食物 foodSteal = 0.1 并转化为 infected_drop | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 命中时按 cothSpread 概率施加 COTH（SRPConfigSystems.cothInfected = 0.1） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 每 5 tick 的 COTH 光环 InfectNearby（SRPConfigSystems.cothAura = 3 格，4800 tick） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 死亡时按 50% 概率（madeRng）进入自爆流程 dyingBurst(fuseTime = 40) | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| selfExplode：生成 EntityToxicCloud（半径 width×1.5、waitTime 10、duration/2、中毒 300、COTH 3600） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| selfExplode：播放 SRPSounds.MOBEXPLOTION | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| attackEntityFromEffects：受击时按 paraGore 概率铺设 goreSim 地面方块 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| attackEntityFromCap：伤害上限触发时抛出 3 个 EntityGore 炸弹 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 受击时 10% 概率调用 attackEntityFromEffects(1,1) | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 落水时 liquidLeap 跃向目标（geneWaterleap） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| placeNidus：killcount ≥ 10 且概率下生成 Nidus 弹 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| doLast：发现感染点后给目标 SPOT 效果并 alertOthers | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| EntityAISwimmingDiving(0.08)：潜水调整 | 3 | 3/0 | `sim_cow` `sim_sheep` `sim_wolf` |
| 融化（EntityCanMelt）→ EntityLesh（73 tick） | 3 | 0/3 | `sim_cow` `sim_sheep` `sim_wolf` |

### damage_and_effects

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| 伤害上限 SRPConfig.infectedCap = 2（超过即削减并施加 RAGE 200/1） | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 药水免疫：COTH/VIRA/CORRO/DLER 不可被施加（func_70687_e） | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| VIRA 病毒叠加放大最小伤害 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 击杀后按 victim 最大生命 × geneMobHealing(3.0) 回血 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 击杀后用 PARATE 强化生命/护甲/伤害 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| thisMelting / EntityAIInfectedSearch 接入（killcount > primitiveKills(10) 且 levelCreated ≥ deveMergeUse(1)） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |

### model_texture

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| preRenderCallback：按引信强度缩放模型（1±0.4/0.1） | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 渲染阴影半径 0.5F | 5 | 0/5 | `sim_cow` `sim_human` `sim_sheep` `sim_squid` `sim_wolf` |

### registration

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| tracker(64, 3, true)：追踪距离 64 格 / 更新间隔 3 tick | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 刷怪蛋颜色 8611072/16711900（全部 sim_* 共用同一颜色） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 刷怪蛋开关 SRPConfig.vanillaEggs | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 眼高 1.75 | 3 | 0/3 | `sim_bigspider` `sim_cow` `sim_human` |

### spawning

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| 光照/难度/天数规则 func_70601_bi（SRPConfig.spawnDays、isValidLightLevelOne/Two） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| 同化生成数据门控 getIDSpawn/canSpawnByIDData（每个生物一个阈值） | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| SRPSpawning.addSpawn(0, undefined.class, 1, 1, biome, 10, <enabled>) | 5 | 0/5 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_wolf` |
| 生成群组 min/max 数量 | 5 | 0/5 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_wolf` |
| 生成权重（SRPConfigMobs 配置项） | 5 | 0/5 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_wolf` |

### sync_data

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| DataManager.register SPECIAL (byte)：parasiteStatus 状态机 0..6 | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| DataManager.register SELFE (int)：自爆引信状态 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| DataManager.register SKIN (byte)：贴图/变体（1..120） | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| DataManager.register COLD_L (boolean)：寒冷环境标记 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| DataManager.register DISLO15 (boolean)：击杀计数/禁用状态 | 6 | 6/0 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |
| EntityInfHuman/EntityInfCow/EntityInfSheep/EntityInfWolf register HEIGH (float) 融化高度 | 6 | 0/6 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_squid` |

### loot

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| 经验 8（XP_INFECTED） | 5 | 0/5 | `sim_bigspider` `sim_cow` `sim_human` `sim_sheep` `sim_wolf` |
| 主掉落 srparasites:assimilated_flesh;80;2;false | 4 | 0/4 | `sim_cow` `sim_human` `sim_sheep` `sim_wolf` |
| 副掉落 srparasites:lurecomponent2;10;1;true | 4 | 4/0 | `sim_cow` `sim_human` `sim_sheep` `sim_wolf` |

### sounds

| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |
| --- | ---: | --- | --- |
| 融化音 SRPSounds.INFECTED_MELT（每 20 tick） | 4 | 0/4 | `sim_cow` `sim_human` `sim_sheep` `sim_wolf` |

## 逐生物缺口（未被共享的条款）

共 155 条，明细见各生物 raw JSON 与 `PARITY_MATRIX.md`。
