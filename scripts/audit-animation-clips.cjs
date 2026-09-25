#!/usr/bin/env node
// Audits whether the animation clips every parasite asks for actually exist.
//
// Background (user report: "原始召唤兽缺少行走动画，其他生物也有类似的情况"):
// ParasiteAnimations resolves a requested legacy action into a clip name. For a handful of
// entities (usesShortAnimationKeys) it returns a *bare* action such as "func_78087_a.limb_swing",
// while the extracted resources key their clips fully qualified
// ("animation.pri_summoner.func_78087_a.limb_swing"). LegacyAnimationLibrary.findClip only does
// an exact lookup plus a last-segment lookup, so those mobs end up playing no clip at all.
//
// This script mirrors the Java resolution rules and reports, per entity, which requested actions
// have no matching clip under either spelling. Run it before/after changing the resolver:
//   node scripts/audit-animation-clips.cjs
//   node scripts/audit-animation-clips.cjs --entity pri_summoner
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const entityDir = path.join(root, "src/main/java/alku/csrp/entity");
const animationsDir = path.join(root, "src/main/resources/assets/csrp/animations");
const tabulaDir = path.join(root, "src/main/resources/assets/csrp/tabula");

const entityFilter = (() => {
  const i = process.argv.indexOf("--entity");
  return i >= 0 ? process.argv[i + 1] : null;
})();
const verbose = process.argv.includes("--verbose");

// --- mirror of ParasiteAnimations ------------------------------------------------------------
// Mirrors ParasiteAnimations.usesShortAnimationKeys: pri_summoner is NOT short-keyed
// (its clips are fully qualified), which was the reported "no walk animation" bug.
const SHORT_KEY_ENTITIES = ["abo_head", "marauder_tendril",
  "inf_sheep", "inf_sheep_head", "inf_villager"];

function aliasFor(action) {
  switch (action) {
    case "run": case "fly": return "walk";
    case "spawn": case "throw": case "smash": case "swipe": return "attack";
    case "melee_attack": case "ranged_attack": case "burst": return "attack";
    case "func_78087_a.getDigging": return "get_dig_model.get_digging_1";
    case "animation": return "idle";
    default: return action;
  }
}

function resolveAction(entityId, requested) {
  // Mirrors ParasiteAnimations.animationResourceId remaps.
  const resourceId = entityId === "sim_dragonhead" ? "sim_dragonehead"
    : entityId === "dispatcher_tentacle" ? "dispatcherten" : entityId;
  let action = aliasFor(requested);
  if (action === "attack") {
    action = { pri_arachnida: "walk.get_parasite_status_2", pri_manducater: "idle.get_parasite_status_1",
      pri_reeker: "idle.get_parasite_status_1", sim_dragone: "walk.get_parasite_status_2",
      dispatcher_sii: "idle" }[entityId] ?? action;
  }
  if (entityId === "pri_summoner" && action === "summon") action = "run";
  else if (entityId === "ada_arachnida" && action === "idle.get_parasite_status_11") action = "idle.get_parasite_status_3";
  else if (entityId === "ada_summoner" && action === "idle.get_parasite_status_100") action = "idle.get_parasite_status_25";
  else if (entityId === "ada_manducater" && action === "idle.get_parasite_status_10") action = "idle.get_parasite_status_3";
  else if (entityId === "ada_manducater" && action === "idle.get_parasite_status_25") action = "walk.get_parasite_status_2";
  if (SHORT_KEY_ENTITIES.includes(resourceId)) {
    // Mirrors the legacy -> short clip translation in ParasiteAnimations.
    if (action.startsWith("func_78087_a.limb_swing")) return "walk";
    if (action.startsWith("func_78087_a.age_in_ticks")) return "idle";
    if (action.startsWith("get_attack_timer")) return "attack";
    return action;
  }
  return `animation.${resourceId}.${action}`;
}

// --- legacy findClip --------------------------------------------------------------------------
function findClip(keys, name) {
  if (keys.has(name)) return name;
  const sep = name.lastIndexOf(".");
  if (sep >= 0 && keys.has(name.slice(sep + 1))) return name.slice(sep + 1);
  // Mirrors the degradation pass added to LegacyAnimationLibrary.findClip: accept any resource
  // key the request is a suffix of, then progressively drop the request's trailing segments.
  let candidate = name;
  while (candidate.length > 0) {
    if (keys.has(candidate)) return candidate;
    for (const key of keys) {
      if (key.length > candidate.length && key.endsWith(candidate)) return key;
    }
    const dot = candidate.lastIndexOf(".");
    if (dot < 0) break;
    candidate = candidate.slice(0, dot);
  }
  return null;
}

// --- project data -----------------------------------------------------------------------------
function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

/** entity id -> clip key set, from the plain animation resource (mirrors the .tbl contents). */
const clipKeys = new Map();
for (const file of fs.readdirSync(animationsDir)) {
  if (!file.endsWith(".animation.json")) continue;
  const id = file.replace(".animation.json", "");
  const json = readJson(path.join(animationsDir, file));
  clipKeys.set(id, new Set(Object.keys(json.animations ?? {})));
}
for (const file of fs.readdirSync(tabulaDir)) {
  if (!file.endsWith(".tbl")) continue;
  const id = file.replace(".tbl", "");
  if (clipKeys.has(id)) continue;
  clipKeys.set(id, null); // no plain resource: skipped by this audit
}

// entity id -> java class, from ModEntities registrations
const modEntities = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/registry/ModEntities.java"), "utf8");
const classToId = new Map();
for (const m of modEntities.matchAll(/monster\("([a-z0-9_]+)",\s*([A-Za-z0-9_]+)::new/g)) {
  classToId.set(m[2], m[1]);
}
for (const m of modEntities.matchAll(/monster\("([a-z0-9_]+)",\s*\(type, level\) -> new ([A-Za-z0-9_]+)/g)) {
  classToId.set(m[2], m[1]);
}

let totalRequests = 0;
let brokenTotal = 0;
const brokenEntities = [];

for (const file of fs.readdirSync(entityDir)) {
  if (!file.endsWith(".java")) continue;
  const className = file.replace(".java", "");
  const entityId = classToId.get(className);
  if (!entityId || (entityFilter && entityId !== entityFilter)) continue;
  const source = fs.readFileSync(path.join(entityDir, file), "utf8");
  const requests = [...source.matchAll(/ParasiteAnimations\.(?:loop|play)\(this,\s*"([^"]+)"/g)]
    .map((m) => m[1]);
  if (!requests.length) continue;
  const keys = clipKeys.get(entityId);
  if (!keys) continue;
  const broken = [];
  for (const requested of new Set(requests)) {
    totalRequests++;
    const resolved = resolveAction(entityId, requested);
    if (!findClip(keys, resolved)) {
      broken.push(`${requested} -> ${resolved}`);
      brokenTotal++;
    } else if (verbose) {
      console.log(`  ok ${entityId}: ${requested} -> ${resolved}`);
    }
  }
  if (broken.length) {
    brokenEntities.push({ entityId, className, broken, clips: keys.size });
  }
}

brokenEntities.sort((a, b) => b.broken.length - a.broken.length);
for (const entry of brokenEntities) {
  console.log(`${entry.entityId} (${entry.className}, ${entry.clips} clips): ${entry.broken.length} unresolved`);
  for (const line of entry.broken) console.log(`    ${line}`);
}
console.log(`\n${brokenTotal} unresolved of ${totalRequests} requests in ${brokenEntities.length} entities`);
process.exit(brokenTotal === 0 ? 0 : 1);
