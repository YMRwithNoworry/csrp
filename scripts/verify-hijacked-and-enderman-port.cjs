const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = ["fer_enderman", "hi_blaze", "hi_golem", "hi_skeleton"];
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const exists = (relative) => fs.existsSync(path.join(root, relative));

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");

for (const id of ids) {
  const constant = id.toUpperCase();
  if (!entities.includes(`"${id}"`)) failures.push(`${id}: entity registration missing`);
  if (!items.includes(`"${id}_spawn_egg"`)) failures.push(`${id}: spawn egg missing`);
  if (!client.includes(`ModEntities.${constant}`)) failures.push(`${id}: renderer missing`);
  if (!english.includes(`"entity.csrp.${id}"`)) failures.push(`${id}: English name missing`);
  if (!chinese.includes(`"entity.csrp.${id}"`)) failures.push(`${id}: Chinese name missing`);
  for (const relative of [
    `src/main/resources/assets/csrp/geo/${id}.geo.json`,
    `src/main/resources/assets/csrp/animations/${id}.animation.json`,
    `src/main/resources/assets/csrp/textures/entity/${id}.png`,
    `src/main/resources/assets/csrp/textures/item/${id}_spawn_egg.png`,
    `src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`,
    `src/main/resources/data/csrp/loot_table/entities/${id}.json`
  ]) {
    if (!exists(relative)) failures.push(`${id}: missing ${relative}`);
  }
}

const feralEnderman = read("src/main/java/alku/csrp/entity/FeralEndermanEntity.java");
const hiBlaze = read("src/main/java/alku/csrp/entity/HiBlazeEntity.java");
const hiGolem = read("src/main/java/alku/csrp/entity/HiGolemEntity.java");
const hiSkeleton = read("src/main/java/alku/csrp/entity/HiSkeletonEntity.java");
const airscrew = read("src/main/java/alku/csrp/entity/AirscrewEntity.java");
const airscrewRenderer = read("src/main/java/alku/csrp/client/renderer/AirscrewRenderer.java");
for (const [source, checks] of [
  [feralEnderman, ["teleportAllyToTarget", "teleportAwayFromTarget", "instanceof Parasite"]],
  [hiBlaze, ["SpineBurstGoal", "illuminateNearbyParasites", "Mode.SPINE"]],
  [hiGolem, ["GolemChargeGoal", "MOVEMENT_SLOWDOWN", "WEAKNESS"]],
  [hiSkeleton, ["SkeletonRangedGoal", "Mode.SPINE"]],
  [airscrew, ["PULL_TARGET_IDS", "syncPullTargets", "spawnPullTethers", "ParticleTypes.CRIT",
    "getPullTargetsForRendering"]],
  [airscrewRenderer, ["renderTether", "RenderType.lightning()", "getPullTargetsForRendering"]]
]) {
  for (const check of checks) {
    if (!source.includes(check)) failures.push(`behavior hook missing: ${check}`);
  }
}

if (!client.includes("AirscrewRenderer::new")) {
  failures.push("airscrew: tether renderer missing");
}

if (failures.length) {
  console.error("Hijacked and feral port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Verified ${ids.length} feral and hijacked entities with behavior and resource coverage.`);
