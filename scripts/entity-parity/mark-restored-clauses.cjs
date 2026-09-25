#!/usr/bin/env node
// Mark audit clauses as restored, with the project evidence that closes them.
//
// The raw audits in docs/entity-parity/raw/<mob>.json are the restoration backlog: each clause
// carries a verdict plus original/project evidence. When an implementation batch closes a clause,
// recording the new verdict here keeps the parity matrix an honest measure of the remaining work.
//
// Usage:
//   node scripts/entity-parity/mark-restored-clauses.cjs --list
//   node scripts/entity-parity/mark-restored-clauses.cjs --apply --batch infected-combat-rules
//
// A batch is a list of clause matchers (regex on the clause text), the new verdict, the facets it
// covers and the project evidence to record. Matching is exact enough to be auditable by diff.
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..", "..");
const rawDir = path.join(root, "docs", "entity-parity", "raw");

const BATCHES = {
  "infected-combat-rules": {
    note: "批次：同化体原版通用战斗规则（EntityPInfected / EntityParasiteBase）",
    // Only mobs whose project class actually received the wiring may be marked. The original
    // builds these rules into EntityPInfected, so other parasite families (feral, marauderized,
    // primitives, …) still have to be wired in a follow-up batch before their clauses move.
    projectClasses: ["AssimilatedParasiteEntity", "AssimilatedVariantEntity", "SimHumanEntity"],
    clauses: [
      {
        match: /损害上限|伤害上限 SRPConfig\.infectedCap/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/ParasiteCombatEffects.java",
        detail: "damageAfterIncomingCap：maxHealth/cap + 余数*0.5 上限 + RAGE 200/1，火焰/虚空不设限，基因门控"
      },
      {
        match: /VIRA 病毒叠加放大最小伤害/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/ParasiteCombatEffects.java",
        detail: "applyMinimumMeleeDamage：VIRA 等级 +2 倍率"
      },
      {
        match: /偷取食物 foodSteal/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/ParasiteCombatEffects.java",
        detail: "stealFoodFromPlayer：按 infectedFoodSteal 概率偷取一份食物并以 assimilated_flesh 掉落"
      },
      {
        match: /药水免疫：COTH\/VIRA\/CORRO\/DLER/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/StatusEffectEvents.java",
        detail: "preventParasiteStatusApplication：寄生体拒绝 COTH/VIRA/CORRO/DLER"
      },
      {
        match: /击杀后按 victim 最大生命.*geneMobHealing/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/ParasiteCombatEffects.java",
        detail: "healOnKill：按 generationProfile.mobHealing 基因 × victim 最大生命回血"
      }
    ]
  },
  // 批次 2：把上面的规则事件化并按科分档，一次覆盖全部寄生体科（原版 EntityParasiteBase 全科共享）。
  "all-tier-combat-rules": {
    note: "批次：全科通用战斗规则事件化（EntityParasiteBase，按 SRPConfig 各科分档）",
    projectClasses: [
      "FeralParasiteEntity", "MarauderizedCowEntity", "HiSkeletonEntity", "LongarmsEntity",
      "HostEntity", "BuglinEntity", "NexusParasiteEntity",
      "AssimilatedParasiteEntity", "AssimilatedVariantEntity", "SimHumanEntity"
    ],
    clauses: [
      {
        // Cap clauses only: the gore reaction and the applyGene paragraph stay untouched.
        match: /^(?!.*(EntityGore|基因|attackEntityFromCap))(?=.*(伤害上限|damageCap|进伤上限)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "applyDefenseRules：按 parasiteCombatTable 分科上限（infected 2 / feral 3 / hijacked 5 / assimara 5 / primitive 6 / adapted 9 / pure 13 / nexus 4-20），触顶 RAGE 200/1"
      },
      {
        match: /^(?!.*(基因|skin|血块|spawnShock|EntityWaveShock|charge\(\)))(?=.*(最小伤害|MiniDamage|命中至少)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/ParasiteCombatEffects.java",
        detail: "applyMinimumDamage：按科 minimumDamage 穿甲扣血，并随 VIRA 等级 +2 放大"
      },
      {
        match: /^(?!.*(EntityGore|基因))(?=.*(偷取食物|foodSteal|食物转为|吞噬玩家食物|增加饥饿)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/ParasiteCombatEffects.java",
        detail: "stealFood：按科 foodSteal 增加玩家饥饿度，并按 foodRott 概率把一份食物转成 assimilated_flesh"
      },
      {
        match: /毒伤害治愈|中毒伤害/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "convertPoisonToHealing：magic 来源且 amount==1 且自身中毒时改为治疗 parasitePoisonHealing"
      },
      {
        match: /药水免疫|免疫 COTH/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/StatusEffectEvents.java",
        detail: "preventParasiteStatusApplication：寄生体拒绝 COTH/VIRA/CORRO/DLER"
      },
      {
        // setWait(10) is a separate animation lock and is not implemented yet.
        match: /^(?!.*setWait)(?=.*(击杀后按 victim|击杀回血|击杀后治疗)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "applyKillHeal：击杀按 generationProfile.mobHealing × victim 最大生命回血"
      },
      {
        // Line-of-sight fear variants still need their own pass.
        match: /^(?!.*视线内)(?=.*(FEAR|恐惧)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "applyFear：命中造成 > 8 伤害时按伤害给 FEAR 1..3 级（200~500 tick）"
      },
      {
        match: /^(?!.*skin)(?=.*(火焰伤害乘|火伤倍率)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java",
        detail: "火伤 × parasiteFireMultiplier(4.0) 且 20% 概率 RAGE 200/1（Primitive 系继承；Buglin 单独实现）"
      }
    ]
  },
  // 批次 4：死亡血肉与自爆（原版 spawnGore / attackEntityFromEffects / attackEntityFromCap / selfExplode）
  "death-gore-and-self-explode": {
    note: "批次：死亡血肉与自爆（EntityParasiteBase.spawnGore / EntityPInfected.spawnGore / selfExplode）",
    projectClasses: [
      "FeralParasiteEntity", "MarauderizedCowEntity", "HiSkeletonEntity", "LongarmsEntity",
      "HostEntity", "BuglinEntity", "NexusParasiteEntity",
      "AssimilatedParasiteEntity", "AssimilatedVariantEntity", "SimHumanEntity"
    ],
    clauses: [
      {
        // The original also spawns EntityAta / checks worldMobCap / syncs skin in a few tiers;
        // those clauses keep their verdict.
        match: /^(?!.*(EntityAta|worldMobCap|skin 同步))(?=.*(EntityRemain|spawnGore)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "leaveGore：BIG 血迹方块 + RemainEntity（goal = 20 × parasiteRemainValue）+ 摊铺 flat 血迹 + 3 个 type 1 血肉弹"
      },
      {
        match: /^(?!.*(粒子|客户端|EntityAta|worldMobCap|skin 同步))(?=.*(attackEntityFromEffects|铺 gore|血迹方块)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/registry/ModBlocks.java",
        detail: "ModBlocks.placeGore 按科选 goresim/gorepri/goreada/gorepur/gorefer/goremar，受击 10% 铺 flat 血迹"
      },
      {
        match: /^(?!.*(EntityAta|worldMobCap|skin 同步)).*attackEntityFromCap.*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "spawnGoreBombs：触顶 30% 抛 1 个、死亡抛 3 个 type 1 EntityGore（带随机初速）"
      },
      {
        match: /10% 概率(调用|触发) attackEntityFromEffects/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "GORE_ON_HURT_CHANCE = 0.1F：受击 10% 铺设一块 flat 血迹"
      },
      {
        match: /^(?!.*(dyingBurst|额外召唤))(?=.*selfExplode).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "selfExplode：MOB_EXPLOSION 音效 + ToxicCloud（半径 width×1.5、waitTime 10、时长减半、中毒 300、COTH 3600）；40 tick 引信未做，仍走死亡即爆"
      }
    ]
  },
  // 批次 5a：受击 20% 反击 RAGE（原版 EntityParasiteBase.attackEntityFrom，全科共享）
  "retaliation-rage": {
    note: "批次：受击 20% 反击 RAGE（EntityParasiteBase.attackEntityFrom:796）",
    projectClasses: [
      "FeralParasiteEntity", "MarauderizedCowEntity", "HiSkeletonEntity", "LongarmsEntity",
      "HostEntity", "BuglinEntity", "NexusParasiteEntity",
      "AssimilatedParasiteEntity", "AssimilatedVariantEntity", "SimHumanEntity"
    ],
    clauses: [
      {
        match: /20% 概率(施加|获得) RAGE|所有活体攻击都会施加 20% 概率 RAGE/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "applyDefenseRules：被活体命中时 nextInt(5)==0（20%）且无 RAGE 时给 RAGE 200/1"
      }
    ]
  },
  // 批次 5b：AI 定格 + 击杀再生（原版 EntityAIWait / primitiveRegen，PrimitiveParasiteEntity 全链）
  "wait-and-regen": {
    note: "批次：EntityAIWait 定格与 primitiveRegen 再生（PrimitiveParasiteEntity 链）",
    projectClasses: [
      "LongarmsEntity", "HostEntity", "HiSkeletonEntity", "NexusParasiteEntity",
      "PureParasiteEntity", "PreeminentParasiteEntity", "AncientParasiteEntity",
      "DerivedParasiteEntity", "DeterrentParasiteEntity"
    ],
    clauses: [
      {
        match: /^(?!.*Jumping)(?=.*(EntityAIWait|setWait)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java",
        detail: "WaitGoal（优先级 0、互斥 MOVE/LOOK/JUMP）对应 EntityAIWait；击杀后 setWait(10)"
      },
      {
        match: /primitiveRegen|生命恢复/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java",
        detail: "tickRegeneration：每 20 tick 一次、killcount>1、非着火且受伤时 heal(parasiteRegen)，每 5 次消耗 1 killcount"
      }
    ]
  },
  // 台账修正（无新代码）：PARATE 击杀强化在工程中早已实现，审计（9/23）未发现，逐条核对语义一致后订正。
  "parate-kill-buff-verified": {
    note: "台账修正：PARATE 击杀强化（EntityParasiteBase:1046-1074，parateMuch = 0.5）——实现早已存在，审计陈旧",
    projectClasses: [
      "FeralParasiteEntity", "MarauderizedCowEntity", "HiSkeletonEntity", "LongarmsEntity",
      "HostEntity", "BuglinEntity", "NexusParasiteEntity",
      "AssimilatedParasiteEntity", "AssimilatedVariantEntity", "SimHumanEntity"
    ],
    clauses: [
      {
        match: /击杀后用 PARATE 强化/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/StatusEffectEvents.java",
        detail: "absorbParateAttributes：击杀时若击杀者带 PARATE，则按 0.5×(amp+1)（原版 parateMuch=0.5 × bonuss）把受害者基础生命/护甲/攻击加到自身"
      }
    ]
  },
  // 批次 6：SELFE 自爆引信（原版 dyingBurst / madeRng / getSelfeFlashIntensity，PrimitiveParasiteEntity 链）
  "selfe-fuse": {
    note: "批次：SELFE 自爆引信与闪烁缩放（EntityParasiteBase.dyingBurst / getSelfeFlashIntensity）",
    projectClasses: [
      "LongarmsEntity", "HostEntity", "HiSkeletonEntity", "NexusParasiteEntity",
      "PureParasiteEntity", "PreeminentParasiteEntity", "AncientParasiteEntity",
      "DerivedParasiteEntity", "DeterrentParasiteEntity"
    ],
    clauses: [
      {
        // COLD_L / DISLO15 are separate sync fields and stay missing for now.
        match: /^(?!.*(COLD_L|DISLO15))(?=.*SELFE).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java",
        detail: "SELFE 同步数据（默认 -1）+ getSelfeState；引信期间按 tick 递增供客户端渲染"
      },
      {
        match: /dyingBurst|madeRng/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java",
        detail: "madeRng 首次受击 rand.nextInt(2)（50%，广播 byte 40）；死亡后 tickDeath 持尸 40 tick 引信再 selfExplode"
      },
      {
        match: /getSelfeFlashIntensity|闪烁缩放|闪白/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/client/renderer/PrimitiveParasiteRenderer.java",
        detail: "getSelfeFlashIntensity 按 (fuse + partial)/(fuseTime - 2) 驱动 preRenderCallback 同款膨胀缩放（f1/f2/f3 公式）"
      }
    ]
  },
  // 批次 7：SELFE 引信铺开到同化/野化系（Assimilated* / SimHuman / Feral）
  "selfe-fuse-families": {
    note: "批次：SELFE 引信铺开到 Assimilated/SimHuman/Feral 系（原版 EntityPInfected 全家族共享）",
    projectClasses: [
      "AssimilatedParasiteEntity", "AssimilatedVariantEntity", "SimHumanEntity", "FeralParasiteEntity"
    ],
    clauses: [
      {
        match: /^(?!.*(COLD_L|DISLO15))(?=.*SELFE).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/ParasiteFuseState.java",
        detail: "共享 ParasiteFuseState：SELFE 同步（默认 -1）+ madeRng 首次受击掷骰 + fuseTime 40 引信；各族 defineSynchedData 注册同一 accessor"
      },
      {
        // status 6 is the self-destruct pose, which is not implemented yet.
        match: /^(?!.*status 6)(?=.*(getSelfeFlashIntensity|闪烁缩放|闪白)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/client/renderer/SelfeFuseRender.java",
        detail: "SelfeFuseRender.applySwelling 用原版 f1/f2/f3 公式缩放；Primitive/Assimilated/SimHuman 三个渲染器均已接入"
      }
    ]
  },
  // 批次 8：掠夺化族引信订正——该族经 HijackedParasiteEntity 继承 PrimitiveParasiteEntity，
  // 本就带着 SELFE 引信；本轮只补 TetheredMarauderizedRenderer 的膨胀缩放。
  "selfe-fuse-marauderized": {
    note: "批次：掠夺化族引信（经 HijackedParasiteEntity 继承）+ 束缚型渲染器膨胀",
    projectClasses: [
      "MarauderizedCowEntity", "MarauderizedBearEntity", "MarauderizedSheepEntity",
      "MarauderizedHumanEntity", "MarauderizedVillagerEntity", "MarauderizedEndermanEntity",
      "TetheredMarauderizedEntity"
    ],
    clauses: [
      {
        match: /^(?!.*(COLD_L|DISLO15))(?=.*SELFE).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java",
        detail: "MarauderizedParasiteEntity extends HijackedParasiteEntity extends PrimitiveParasiteEntity，继承 ParasiteFuseState 引信与 SELFE 同步"
      },
      {
        match: /^(?!.*status 6)(?=.*(getSelfeFlashIntensity|闪烁缩放|闪白)).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/client/renderer/TetheredMarauderizedRenderer.java",
        detail: "mar_cow/mar_human/mar_sheep/mar_villager 走 PrimitiveParasiteRenderer（已接），mar_bear/mar_enderman 走 TetheredMarauderizedRenderer（本批接入 SelfeFuseRender）"
      }
    ]
  },
  // 批次 10：水跃能力本体 + geneWaterleap 生成行（原版 EntityAIWaterLeapAtTargetStatus / generationWaterLeap0..5）
  "water-leap-gene": {
    note: "批次：水跃能力与 geneWaterleap 门控（EntityAIWaterLeapAtTargetStatus / generationWaterLeap0..5）",
    projectClasses: ["LongarmsEntity", "AdaptedVariantEntity", "HeedEntity", "PreeminentParasiteEntity",
      "PrimitiveVariantEntity", "PureParasiteEntity", "VisceraEntity"],
    clauses: [
      {
        match: /EntityAIWaterLeapAtTargetStatus/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/WaterLeapAtTargetGoal.java",
        detail: "参数化复刻：瞄准 cooldown tick → 记录目标位置与 0.07 高度补偿 → 起跳（speed*0.9 + 现速*0.3，垂直 0.7）→ 落地按 damageRange 击退/攻击；由 generationProfile.waterLeap 门控"
      }
    ]
  },
  // 批次 12：野化族与 sim_human 的水跃任务（原版 EntityFer*/EntityInfHuman 构造表）
  "water-leap-feral-simhuman": {
    note: "批次：野化族与 sim_human 的水跃任务（EntityFerVillager:53 / EntityInfHuman:119）",
    projectClasses: ["FeralParasiteEntity", "SimHumanEntity"],
    clauses: [
      {
        // handleWater/liquidLeap is a separate mechanism and stays missing.
        match: /^(?!.*handleWater)(?=.*EntityAIWaterLeapAtTargetStatus).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/FeralParasiteEntity.java",
        detail: "野化族按 EntityFer*.java:53 全族在优先级 2 注册 WaterLeapAtTargetGoal(this, 0.7F, 1.5, 20, 0)；sim_human 按 EntityInfHuman:119 同参数注册；两者均以 generationProfile.waterLeap 为门"
      }
    ]
  },
  // 批次 13：handleWater 液体命中突进（原版 EntityParasiteBase:462-491）
  "liquid-leap": {
    note: "批次：handleWater 液体突进（EntityParasiteBase:462）",
    projectClasses: ["FeralParasiteEntity", "PrimitiveParasiteEntity", "LongarmsEntity"],
    clauses: [
      {
        match: /handleWater|liquidLeap/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/LiquidLeap.java",
        detail: "液体命中累积 charge（上限 4，每 20 tick 判定一次），每 tick 消耗一枚并按 geneWaterleap 朝目标突进：潜没时高度 0.1/强度 0.5、出水时 0.3/1.0，水平公式 str*0.8 + 现速*0.2"
      }
    ]
  },
  // 批次 16：EntityAIJumping（原版 EntityParasiteBase:2525）
  "jumping-ai": {
    note: "批次：EntityAIJumping 跳跃 AI（EntityParasiteBase.EntityAIJumping:2525）",
    projectClasses: ["LongarmsEntity"],
    clauses: [
      {
        match: /EntityAIJumping/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/JumpAtHigherTargetGoal.java",
        detail: "每 10 tick 判定：目标高出门眼 1 格以上且平方距离 < 4.0 且在地面 → 停导航 + 起跳（垂直 0.2 + 高*0.15，水平 0.5*0.8 + 现速*0.2）；沿用原版 canUse 内执行并返回 false 的形态；LongarmsEntity 优先级 5 注册"
      }
    ]
  },
  // 批次 22：EntityAISkill 契约接入（原版 EntityAISkill，pri_longarms attackID 21 恐怖球）
  "skill-dispatch": {
    note: "批次：EntityAISkill 契约与 pri_longarms 恐怖球技能（EntityShyco tasks.addTask(2, EntityAISkill(this, 80, 4, false, 21))）",
    projectClasses: ["LongarmsEntity"],
    clauses: [
      {
        match: /EntityAISkill/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/ParasiteSkillGoal.java",
        detail: "共享 ParasiteSkillGoal 复刻契约（geneSpecialmove 门控、距离窗口平方、needVisual、冷却、attackID 派发）；LongarmsEntity 以 (80, 4, false, 21) 注册，技能体复用既有 applyScaryOrbEffect/applyScaryOrbMinimumDamage"
      }
    ]
  },
  // 批次 24：gene 捆绑条款翻转（适用子项已全具备；不可用子项经原版任务表证伪）
  "gene-bundle-complete": {
    note: "批次：gene 捆绑条款完成（min dmg/dmg cap/heal/poison/sprint/attack speed 全具备；waterleap、blockSearch、specialmove 经原版任务表证伪为不适用）",
    projectClasses: ["MarauderizedCowEntity", "FeralParasiteEntity", "SimHumanEntity"],
    clauses: [
      {
        match: /applyGene/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/world/EvolutionSystem.java",
        detail: "适用子项全部实现：最小伤害/伤害上限（ParasiteCombatRules 门控 r6）、击杀治疗与毒伤治疗（既有）、疾跑与攻击速度（GeneMeleeGoal r12-15）。不适用子项经原版证据证伪：EntityAIWaterLeapAtTargetStatus 仅 EntityFer*/EntityInfHuman 有（r9/r12 已实现）、EntityAIBlockLight 在 EntityInf*/EntityFer*/EntitySpe* 全段为 0、EntityAISkill 仅 EntityInfCow 与 EntitySpeBear 有（ORIGINAL_AI_TASKS.md）"
      }
    ]
  },
  // 批次 25：同化族中已完成的其余三只（class 粒度过滤会误带仍缺技能的 sim_cow，故用 mob 过滤）
  "gene-bundle-assimilated": {
    note: "批次：gene 捆绑条款完成（sim_sheep/sim_wolf/sim_squid；水跃、穿墙破块、技能经原版任务表证伪为不适用）",
    mobs: ["sim_sheep", "sim_wolf", "sim_squid"],
    clauses: [
      {
        match: /applyGene/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/world/EvolutionSystem.java",
        detail: "适用子项全部实现：最小伤害/伤害上限（ParasiteCombatRules 门控）、击杀治疗与毒伤治疗（既有）、疾跑与攻击速度（GeneMeleeGoal）。不适用子项：EntityAIWaterLeapAtTargetStatus 与 EntityAIBlockLight 在其原版类均为 0，EntityAISkill 未出现在 ORIGINAL_AI_TASKS.md 的 EntityInfSheep/InfWolf/InfSquid 段落"
      }
    ]
  },
  // 批次 26：sim_bigspider（原版 EntityDorpa）gene 捆绑条款完成
  "gene-bundle-dorpa": {
    note: "批次：gene 捆绑条款完成（sim_bigspider = EntityDorpa；水跃/穿墙/技能经原版任务表证伪为不适用）",
    mobs: ["sim_bigspider"],
    clauses: [
      {
        match: /applyGene/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/world/EvolutionSystem.java",
        detail: "适用子项全部实现（最小伤害/伤害上限/治疗/毒伤治疗/疾跑/攻击速度）；不适用子项：ORIGINAL_AI_TASKS.md:1469 起 EntityDorpa 段落无 EntityAISkill/EntityAIWaterLeapAtTargetStatus/EntityAIBlockLight"
      }
    ]
  },
  // 批次 27：sim_cow（原版 EntityInfCow）gene 捆绑条款完成
  "gene-bundle-sim-cow": {
    note: "批次：gene 捆绑条款完成（sim_cow = EntityInfCow；技能现由 CowChargeGoal 按原版 EntityAISkill 参数与 gene 门驱动）",
    mobs: ["sim_cow"],
    clauses: [
      {
        match: /applyGene/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java",
        detail: "适用子项全部实现：最小伤害/伤害上限（ParasiteCombatRules 门控）、治疗与毒伤治疗（既有）、疾跑与攻击速度（GeneMeleeGoal）、技能（CowChargeGoal 对齐原版 EntityInfCow:75 EntityAISkill(this, 60, 32, 8, true, 1)：8-32 格窗口、60 tick 冷却、geneSpecialmove 门控）。水跃与穿墙破块对该原版类不适用（各 0 处）"
      }
    ]
  },
  // 批次 28：阶段属性加成（原版 finalizeSpawn:1682-1696，+7%）
  "phase-stat-bonus": {
    note: "批次：阶段属性加成（EntityParasiteBase:1682-1696）",
    projectClasses: ["FeralParasiteEntity", "NexusParasiteEntity", "PrimitiveParasiteEntity", "AssimilatedParasiteEntity"],
    clauses: [
      {
        match: /阶段属性加成/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "applyPhaseStatBonus 于 FinalizeSpawnEvent 中实现：phase >= Config.evolutionStatIncreasePhase(10) 时把 MAX_HEALTH/ARMOR/ATTACK_DAMAGE 基础值 ×(1+0.07)，与原版 finalizeSpawn 同点同公式（原版用 evolutionParasiteStatIncrease/Value 配置）"
      }
    ]
  },
  // 批次 29：doLast 的 SPOT 与 alertOthers（原版 EntityParasiteBase:1167-1179）
  "dolast-spot": {
    note: "批次：doLast SPOT + alertOthers（EntityParasiteBase:1167-1179）",
    projectClasses: ["FeralParasiteEntity", "MarauderizedCowEntity", "HiSkeletonEntity", "LongarmsEntity",
      "HostEntity", "BuglinEntity", "NexusParasiteEntity", "AssimilatedParasiteEntity",
      "AssimilatedVariantEntity", "SimHumanEntity", "PrimitiveParasiteEntity"],
    clauses: [
      {
        match: /doLast|alertOthers/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "markSpottedTarget（LivingChangeTargetEvent）：目标确定且 SrpWorldData.nearestInfectionPosition 存在时给目标 SPOTTED 1200 tick，并由 alertOthers 唤醒 7 格内无目标的寄生体；新增 SrpWorldData.nearestInfectionPosition（节点/殖民地最近点）"
      }
    ]
  },
  // 批次 30：EntityAISwimmingDiving（原版 entity/ai/EntityAISwimmingDiving.java）
  "swimming-diving": {
    note: "批次：EntityAISwimmingDiving 潜水任务（yMotion 0.08，优先级 0）",
    projectClasses: ["AssimilatedParasiteEntity", "FeralParasiteEntity", "SimHumanEntity"],
    clauses: [
      {
        match: /EntityAISwimmingDiving/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/SwimmingDivingGoal.java",
        detail: "复刻原版：水中/岩浆中且在目标于液体内、平方距离 <25 且低 1 格以上时按 yMotion 下潜；否则 80% 概率划水（jump）；三族按原版优先级 0 与 0.08 参数注册"
      }
    ]
  },
  // 批次 31：EntityAIGetFollowers（原版 entity/ai/EntityAIGetFollowers.java，version 1 / range 16）
  "recruit-followers": {
    note: "批次：EntityAIGetFollowers 招募跟随（version 1，range 16，优先级 6）",
    projectClasses: ["AssimilatedParasiteEntity", "FeralParasiteEntity"],
    clauses: [
      {
        match: /EntityAIGetFollowers/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/RecruitFollowersGoal.java",
        detail: "复刻原版：每 20 tick、自身无 leader 且无目标时，在 (range,2,range) 盒内找第一个有视线、存活、尚无 leader 的寄生体并令其跟随（ParasiteFollowGoal.setLeader）；同化与野化族按原版优先级 6 / range 16 注册（EntityInfCow:74 等）"
      }
    ]
  },
  // 批次 32：sim_human 的招募任务优先级订正（原版 EntityInfHuman:122 为优先级 5）
  "recruit-followers-simhuman": {
    note: "批次：EntityAIGetFollowers 招募跟随（sim_human 优先级 5 形态，EntityInfHuman:122）",
    mobs: ["sim_human"],
    clauses: [
      {
        match: /EntityAIGetFollowers/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/RecruitFollowersGoal.java",
        detail: "同化族招募任务已实现（RecruitFollowersGoal 复刻 EntityAIGetFollowers version 1/range 16）；sim_human 按原版 EntityInfHuman:122 在优先级 5 注册"
      }
    ]
  },
  // 批次 34：hi_skeleton 的招募任务（原版 EntityHiSkeleton:52）
  "recruit-followers-hiskeleton": {
    note: "批次：EntityAIGetFollowers 招募跟随（hi_skeleton，原版 EntityHiSkeleton:52 优先级 6 / range 16）",
    mobs: ["hi_skeleton"],
    clauses: [
      {
        match: /EntityAIGetFollowers/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/RecruitFollowersGoal.java",
        detail: "RecruitFollowersGoal 复刻 EntityAIGetFollowers version 1/range 16（每 20 tick、自身无 leader 且无目标时招募一个有视线且尚无 leader 的寄生体跟随）；HiSkeletonEntity 按原版优先级 6 注册，其领导模型由 primitive 链继承的 ParasiteFollowGoal 提供"
      }
    ]
  },
  // 批次 36：sim_bigspider 的蛛网弹（条款 2 已由既有 fireWebBall + 60 tick 冷却满足；条款 1 的注册形态仍缺）
  "web-ball-ranged": {
    note: "批次：EntityAIAttackProjectile 远程蛛网弹（每 60 tick 一发）",
    mobs: ["sim_bigspider"],
    clauses: [
      {
        match: /^(?!.*addTask)(?=.*蛛网弹).*$/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java",
        detail: "AssimilatedVariantEntity tick 中 BIGSPIDER 分支：rangedCooldown <= 0 且有目标与视线时 fireWebBall(target) 并置 rangedCooldown = 60，与原版 EntityAIAttackProjectile(this, 60, …) 的冷却一致"
      }
    ]
  },
  // 批次 37：im_bigspider 的 EntityAIAttackProjectile 注册形态（60 tick 蓄力 + 15 tick 间隔 3 连发）
  "web-ball-volley": {
    note: "批次：EntityAIAttackProjectile(this, 60, 15, 3)（原版 func_75246_d：蓄力 60 tick，随后每 15 tick 一发共 3 发，射程 65 格）",
    mobs: ["sim_bigspider"],
    clauses: [
      {
        match: /EntityAIAttackProjectile/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java",
        detail: "tickWebBallVolley：目标存活、65 格内且可见时先蓄力 WEB_CHARGE_TICKS(60)，随后 WEB_VOLLEY_SHOTS(3) 发、每 WEB_VOLLEY_INTERVAL_TICKS(15) 一发 fireWebBall；目标失效/超距/失去视线即重置，与原版 func_75246_d 的 attackTimer/shootingTimes/tickInterval 语义一致"
      }
    ]
  },
  // 批次 43：sim_bigspider（dorpa）的 per-mob 属性倍率接线
  "per-mob-multipliers-dorpa": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.dorpa*，默认 1.0F）",
    mobs: ["sim_bigspider"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java",
        detail: "createAttributes 读取原版 SRPConfigMobs 的 dorpa 四项（health/damage/armor/KDResistance，默认 1.0）并相乘，与既有全局倍率（OriginalConfigEvents）构成「全局 × per-mob」结算；击退抗性按原版上限夹取 1.0"
      }
    ]
  },
  // 批次 44：sim_cow（infcow）的 per-mob 属性倍率接线
  "per-mob-multipliers-infcow": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.infcow*，默认 1.0F）",
    mobs: ["sim_cow"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java",
        detail: "createAttributes 读取原版 SRPConfigMobs 的 infcow 四项（health/damage/armor/KDResistance，默认 1.0）并相乘，与既有全局倍率构成「全局 × per-mob」结算；击退抗性按原版上限夹取 1.0"
      }
    ]
  },
  // 批次 45：sim_sheep / sim_wolf 的 per-mob 属性倍率接线
  "per-mob-multipliers-insheep-wolf": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.infsheep*/infwolf*，默认 1.0F）",
    mobs: ["sim_sheep", "sim_wolf"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java",
        detail: "createAttributes 按 kind 读取原版 SRPConfigMobs 的 infsheep/infwolf 四项（health/damage/armor/KDResistance，默认 1.0）并相乘，与既有全局倍率构成「全局 × per-mob」结算；击退抗性夹取 1.0"
      }
    ]
  },
  // 批次 46：sim_squid 的 per-mob 属性倍率接线
  "per-mob-multipliers-insquid": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.infsquid*，默认 1.0F）",
    mobs: ["sim_squid"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java",
        detail: "createAttributes 的 SQUID 分支读取原版 SRPConfigMobs 的 infsquid 四项（health/damage/armor/KDResistance，默认 1.0）并相乘，与既有全局倍率构成「全局 × per-mob」结算；击退抗性夹取 1.0"
      }
    ]
  },
  // 批次 47：sim_human 的 per-mob 属性倍率接线
  "per-mob-multipliers-infhuman": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.infhuman*，默认 1.0F）",
    mobs: ["sim_human"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/SimHumanEntity.java",
        detail: "SimHumanEntity.createAttributes 读取原版 SRPConfigMobs 的 infhuman 四项（health/damage/armor/KDResistance，默认 1.0）并相乘基础生命 40/攻击 12/护甲 6，与既有全局倍率构成「全局 × per-mob」结算；击退抗性夹取 1.0"
      }
    ]
  },
  // 批次 48：fer_villager 的 per-mob 属性倍率接线
  "per-mob-multipliers-fervillager": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.fervillager*，默认 1.0F）",
    mobs: ["fer_villager"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/FeralParasiteEntity.java",
        detail: "FeralParasiteEntity.createAttributes 在 Kind.VILLAGER 分支读取原版 SRPConfigMobs 的 fervillager 四项（health/damage/armor/KDResistance，默认 1.0）并相乘，与既有全局倍率构成「全局 × per-mob」结算；击退抗性夹取 1.0"
      }
    ]
  },
  // 批次 49：pri_longarms（shyco）的 per-mob 属性倍率接线
  "per-mob-multipliers-shyco": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.shyco*，默认 1.0F）",
    mobs: ["pri_longarms"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/LongarmsEntity.java",
        detail: "LongarmsEntity.createAttributes 读取原版 SRPConfigMobs 的 shyco 四项（health/damage/armor/KDResistance，默认 1.0）并相乘基础生命 45/护甲 9/攻击 15/击退 0.7（夹取 1.0），与既有全局倍率构成「全局 × per-mob」结算"
      }
    ]
  },
  // 批次 50：hi_skeleton（hiskeleton）的 per-mob 属性倍率接线
  "per-mob-multipliers-hiskeleton": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.hiskeleton*，默认 1.0F）",
    mobs: ["hi_skeleton"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/HiSkeletonEntity.java",
        detail: "HiSkeletonEntity.createAttributes 读取原版 SRPConfigMobs 的 hiskeleton 四项（health/damage/armor/KDResistance，默认 1.0）并乘以基础生命 27/护甲 8/攻击 17/击退 0.9（夹取 1.0），与既有全局倍率构成「全局 × per-mob」结算"
      }
    ]
  },
  // 批次 51：mar_cow（marcow）的 per-mob 属性倍率接线
  "per-mob-multipliers-marcow": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.marcow*，默认 1.0F）",
    mobs: ["mar_cow"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/MarauderizedCowEntity.java",
        detail: "MarauderizedCowEntity.createAttributes 读取原版 SRPConfigMobs 的 marcow 四项（health/damage/armor/KDResistance，默认 1.0）并乘基础生命 38/护甲 8/攻击 15/击退 0.8（夹取 1.0），与既有全局倍率构成「全局 × per-mob」结算"
      }
    ]
  },
  // 批次 52：host 的 per-mob 属性倍率接线
  "per-mob-multipliers-host": {
    note: "批次：per-mob 属性倍率接线（原版 SRPConfigMobs.host*，默认 1.0F）",
    mobs: ["host"],
    clauses: [
      {
        match: /per-mob|倍率/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/entity/HostEntity.java",
        detail: "HostEntity.createAttributes 读取原版 SRPConfigMobs 的 host 三项（health/damage/armor，默认 1.0）并乘基础生命 50/护甲 7/攻击 10；击退抗性由 helper 固定为 1.0（已在上限，倍率等价），与既有全局倍率构成「全局 × per-mob」结算"
      }
    ]
  },
  // 批次 100：生成合法性 func_70601_bi（两级光照 + spawnDays + 非和平）
  "spawn-validity-func-70601-bi": {
    note: "批次：生成合法性 func_70601_bi 接线（FinalizeSpawnEvent 上套用）",
    mobs: ["sim_cow", "sim_sheep", "sim_squid", "sim_human", "sim_bigspider", "mar_cow"],
    clauses: [
      {
        match: /func_70601_bi|光照\/难度\/天数/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/event/ParasiteCombatRules.java",
        detail: "已实现原版 func_70601_bi 语义：ParasiteCombatRules.enforceLegacySpawnValidity 在 FinalizeSpawnEvent 上判定，未通过则 setSpawnCancelled(true)（刷怪笼/刷怪蛋/指令豁免）；world/SpawnLightChecks 提供两级光照（isValidLightLevelTwo 的随机门控照抄、isValidLightLevelOne 含 SKY>nextInt(32)、getMaxLocalRawBrightness<=nextInt(8)、getWalkTargetValue>=0）与 canSpawnNaturally（和平难度拒、Config.spawnDays() > getGameTime() 拒、按 phase>=evolutionSpawningIgnoreSunlight || phase==-1&&phaseLightlessMinusOne 选档）；配置键 spawnDays/evolutionSpawningIgnoreSunlight/phaseLightlessMinusOne 已补。已知偏差（文档批次 94）：寄生区以脚下方块为 InfestedBlock 近似、不模拟雷暴临时减光。"
      }
    ]
  },
  // 批次 101：fer_villager 的生成合法性（含 SRPConfig.ignoreL）
  "spawn-validity-ignorel": {
    note: "批次：生成合法性补 ignoreL（useEvolution 关闭时改用宽松档）",
    mobs: ["fer_villager"],
    clauses: [
      {
        match: /ignoreL/,
        verdict: "satisfied",
        evidence: "src/main/java/alku/csrp/world/SpawnLightChecks.java",
        detail: "原版 ignoreL 仅在使用进化阶段关闭时生效（EntityParasiteBase:1568 的 else-if 分支：为真则用宽松档 isValidLightLevelTwo），已按此语义实现：Config 新增 ignoreL 键（默认 false），SpawnLightChecks.canSpawnNaturally 在 Config.useEvolutionPhases() 为假时按 ignoreLightLevel() 选择宽松/严格档；结合既有的 isValidLightLevelOne/Two、spawnDays tick 门槛与非和平判定，该条款要素齐备。"
      }
    ]
  }
};

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

const batchName = arg("batch", "infected-combat-rules");
const batch = BATCHES[batchName];
if (!batch) {
  console.error(`unknown batch: ${batchName}`);
  process.exit(2);
}

const apply = process.argv.includes("--apply");
const files = fs.readdirSync(rawDir).filter((f) => f.endsWith(".json")).sort();
let changedFiles = 0;
let changedClauses = 0;

for (const file of files) {
  const full = path.join(rawDir, file);
  const data = JSON.parse(fs.readFileSync(full, "utf8"));
  if (batch.mobs && !batch.mobs.includes(data.id)) {
    console.log(`[skip] ${data.id}: not in this batch mob list`);
    continue;
  }
  if (batch.projectClasses && !batch.projectClasses.some((c) => (data.projectClass ?? "").startsWith(c))) {
    console.log(`[skip] ${data.id} (${data.projectClass}): not touched by this batch`);
    continue;
  }
  let touched = false;
  for (const facet of data.facets ?? []) {
    for (const clause of facet.clauses ?? []) {
      if (clause.verdict === "satisfied" || clause.verdict === "na") continue;
      const rule = batch.clauses.find((r) => r.match.test(clause.clause ?? ""));
      if (!rule) continue;
      console.log(`${apply ? "[apply] " : "[dry] "} ${data.id} [${facet.name}] ${clause.clause}`);
      console.log(`         ${clause.verdict} -> ${rule.verdict}  (${rule.evidence})`);
      clause.verdict = rule.verdict;
      clause.note = `${rule.detail}；${batch.note}`;
      clause.evidence = { ...(clause.evidence ?? {}), project: rule.evidence };
      touched = true;
      changedClauses++;
    }
  }
  if (touched) {
    changedFiles++;
    if (apply) fs.writeFileSync(full, `${JSON.stringify(data, null, 2)}\n`, "utf8");
  }
}

console.log(`\n${apply ? "applied" : "would change"}: ${changedClauses} clause(s) in ${changedFiles} file(s)`);
if (!apply) console.log("re-run with --apply to write the audits");
