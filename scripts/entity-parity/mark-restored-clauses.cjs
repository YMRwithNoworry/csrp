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
  if (batch.projectClasses && !batch.projectClasses.includes(data.projectClass)) {
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
