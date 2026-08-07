const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = ["mar_bear", "mar_cow", "mar_enderman", "mar_human", "mar_sheep", "mar_villager"];
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const exists = (relative) => fs.existsSync(path.join(root, relative));

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const common = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const creative = read("src/main/java/alku/csrp/Csrp.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");

for (const id of ids) {
  const constant = id.toUpperCase();
  if (!entities.includes(`"${id}"`)) failures.push(`${id}: entity registration missing`);
  if (!items.includes(`"${id}_spawn_egg"`)) failures.push(`${id}: spawn egg missing`);
  if (!common.includes(`ModEntities.${constant}`)) failures.push(`${id}: attributes missing`);
  if (!client.includes(`ModEntities.${constant}`)) failures.push(`${id}: renderer missing`);
  if (!creative.includes(`ModItems.${constant}_SPAWN_EGG`)) failures.push(`${id}: creative tab entry missing`);
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

const shared = read("src/main/java/alku/csrp/entity/MarauderizedParasiteEntity.java");
const tethered = read("src/main/java/alku/csrp/entity/TetheredMarauderizedEntity.java");
const bear = read("src/main/java/alku/csrp/entity/MarauderizedBearEntity.java");
const cow = read("src/main/java/alku/csrp/entity/MarauderizedCowEntity.java");
const enderman = read("src/main/java/alku/csrp/entity/MarauderizedEndermanEntity.java");
const human = read("src/main/java/alku/csrp/entity/MarauderizedHumanEntity.java");
const combat = read("src/main/java/alku/csrp/entity/ParasiteCombatEffects.java");
const tetherRenderer = read("src/main/java/alku/csrp/client/renderer/TetheredMarauderizedRenderer.java");

for (const [source, hooks] of [
  [shared, ["MeleeAttackGoal", "meleeSpeed", "PARASITE_STATUS", "STILL_ANI", "startAttackAnimation",
    '"age_controller"', "ParasiteAnimations.isMoving(this, state.isMoving())"]],
  [tethered, ["pullDurationTicks", "tetherDamage", "initialWeaknessAmplifier", "getPullTargetForRendering"]],
  [bear, ["PullVolleyGoal", "PullingBallEntity", "startAttackAnimation"]],
  [cow, ["VOMIT_EVENT", "spawnVomitCloud(this, 4.5D, 3.0F, 100, 300, 20)", "startAttackAnimation"]],
  [combat, ["ModMobEffects.VOMIT", "ModMobEffects.VIRAL", "MobEffects.MOVEMENT_SLOWDOWN",
    "MobEffects.WEAKNESS", "ModMobEffects.CORROSION"]],
  [enderman, ["ParticleTypes.PORTAL", "DamageTypeTags.IS_PROJECTILE", "teleportAwayFromTarget", "pullStrength", "tetherDamage"]],
  [human, ["PounceMountGoal", "startRiding", "MobEffects.BLINDNESS", "MobEffects.HUNGER"]],
  [tetherRenderer, ["RenderType.lightning()", "getPullTargetForRendering", "renderRibbonSegment"]]
]) {
  for (const hook of hooks) {
    if (!source.includes(hook)) failures.push(`behavior hook missing: ${hook}`);
  }
}

if (failures.length) {
  console.error("Marauderized port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Verified ${ids.length} Marauderized entities with behavior, rendering, data, and resource coverage.`);
