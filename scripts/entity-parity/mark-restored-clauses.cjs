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
