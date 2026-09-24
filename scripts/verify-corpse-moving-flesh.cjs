const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];

function read(relativePath) {
  const file = path.join(root, relativePath);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relativePath}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
}

function expect(source, pattern, message) {
  if (!pattern.test(source)) failures.push(message);
}

const events = read("src/main/java/alku/csrp/infection/InfectionEvents.java");
const flesh = read("src/main/java/alku/csrp/entity/MovingFleshEntity.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");

// 1. A parasite kill must leave Living Flesh on the corpse without claiming the death, so the
//    victim keeps dropping its loot and experience.
expect(events, /@SubscribeEvent\(priority = EventPriority\.LOWEST\)/,
  "corpse flesh handler must run after the COTH conversions (EventPriority.LOWEST is missing)");
const handlerMatch = events.match(
  /leaveMovingFleshOnParasiteKill\(LivingDeathEvent event\)\s*\{([\s\S]*?)\n    \}/);
if (!handlerMatch) {
  failures.push("leaveMovingFleshOnParasiteKill(LivingDeathEvent) handler was not found");
} else {
  const body = handlerMatch[1];
  expect(body, /event\.isCanceled\(\)/, "corpse handler must skip deaths a conversion already claimed");
  expect(body, /event\.getSource\(\)\.getEntity\(\) instanceof Parasite/,
    "corpse handler must require a parasite killer");
  expect(body, /corpse instanceof Parasite/, "corpse handler must ignore parasite corpses");
  expect(body, /corpse instanceof Player/, "corpse handler must ignore player deaths");
  expect(body, /MovingFleshEntity\.spawnFromCorpse\(serverLevel, corpse\)/,
    "corpse handler must spawn the Living Flesh from the corpse");
  if (/setCanceled/.test(body)) {
    failures.push("corpse handler must not cancel the death, or the victim loses its drops");
  }
}

// 2. Two Living Flesh masses must seek each other out and melt into a merge-pool parasite.
expect(flesh, /REQUIRED_MERGES = 2/, "two Living Flesh masses must be enough to fuse");
expect(flesh, /class MergeMovingFleshGoal/, "Living Flesh no longer seeks a merge partner");
expect(flesh, /navigation\.snapTo\(target, 1\.1D\)|navigation\.moveTo\(target, 1\.1D\)/,
  "Living Flesh merge goal no longer walks towards its partner");
expect(flesh,
  /public static MovingFleshEntity spawnFromCorpse\(ServerLevel serverLevel, LivingEntity corpse\)/,
  "MovingFleshEntity.spawnFromCorpse factory is missing");
expect(flesh, /serverLevel\.addFreshEntity\(flesh\)/,
  "spawnFromCorpse does not register the new Living Flesh in the level");
expect(flesh, /createMergedParasite\(serverLevel\)/,
  "the fused Living Flesh does not create the merge-pool parasite");
expect(flesh, /MobsConfig\.mergeSystemMobList\(\)/, "the merge pool config is no longer used");
expect(flesh, /MobsConfig\.mergeSystemMobHealth\(\)/, "the merge result health config is no longer used");

// 3. The default merge pool must cover every tier with entity ids that actually exist.
const poolMatch = config.match(
  /"mergeSystemMobList",\s*List\.of\(([\s\S]*?)\),\s*\n\s*"Moving Flesh merge table/);
if (!poolMatch) {
  failures.push("Moving Flesh merge table default was not found in MobsConfig");
} else {
  const pool = [...poolMatch[1].matchAll(/"(?:srparasites|csrp):([a-z0-9_]+);\d+"/g)].map((m) => m[1]);
  if (pool.length < 50) {
    failures.push(`merge pool only lists ${pool.length} mobs; every parasite tier is expected`);
  }
  const registered = new Set(
    [...entities.matchAll(/\bmonster\(\s*"([a-z0-9_]+)"/g)].map((m) => m[1]));
  for (const id of pool) {
    if (!registered.has(id)) {
      failures.push(`merge pool entry ${id} is not a registered entity type`);
    }
  }
  const tierMembers = {
    Crude: ["airscrew", "crux", "crux_incomplete", "dredge", "heed", "host", "hostii",
      "incompleteform_small", "incompleteform_medium", "thrall"],
    Feral: ["fer_bear", "fer_cow", "fer_enderman", "fer_horse", "fer_human", "fer_pig",
      "fer_sheep", "fer_villager", "fer_wolf"],
    Assimara: ["mar_bear", "mar_cow", "mar_enderman", "mar_human", "mar_sheep", "mar_villager"],
    Hijacked: ["hi_blaze", "hi_golem", "hi_skeleton"],
    Primitive: ["pri_arachnida", "pri_bolster", "pri_burrower", "pri_devourer", "pri_longarms",
      "pri_manducater", "pri_reeker", "pri_summoner", "pri_tozoon", "pri_vermin", "pri_viscera",
      "pri_yelloweye"],
    Adapted: ["ada_arachnida", "ada_bolster", "ada_burrower", "ada_devourer", "ada_longarms",
      "ada_manducater", "ada_reeker", "ada_summoner", "ada_tozoon", "ada_vermin", "ada_viscera",
      "ada_yelloweye"],
    Pure: ["bomber_light", "grunt", "marauder", "monarch", "overseer", "vigilante", "warden"]
  };
  for (const [tier, members] of Object.entries(tierMembers)) {
    const missing = members.filter((id) => !pool.includes(id));
    if (missing.length) {
      failures.push(`${tier} tier is incomplete in the merge pool: missing ${missing.join(", ")}`);
    }
  }
}

if (failures.length) {
  console.error("Corpse Living Flesh verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Corpse Living Flesh verification passed.");
