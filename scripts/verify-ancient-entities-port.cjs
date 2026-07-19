const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = ["anc_dreadnaut", "anc_overlord"];
const failures = [];
const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
};
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const creative = read("src/main/java/alku/csrp/Csrp.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");
const ancient = read("src/main/java/alku/csrp/entity/AncientParasiteEntity.java");
const projectile = read("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");

expect(ancient, /MAX_ADAPTATION_HITS\s*=\s*10/, "Ancient adaptation hit cap is missing");
expect(ancient, /MAX_LEARNABLE_DAMAGE_SOURCES\s*=\s*5/, "Ancient learnable-source cap is missing");
expect(ancient, /DamageTypeTags\.IS_FIRE/, "Ancient fire weakness is missing");
expect(ancient, /DreadVolleyGoal/, "Dreadnaut Wither volley is missing");
expect(ancient, /DreadPodGoal/, "Dreadnaut pod summon is missing");
expect(ancient, /DreadFlightGoal/, "Dreadnaut swoop is missing");
expect(ancient, /OverlordHomingGoal/, "Overlord homing projectile is missing");
expect(ancient, /triggerAncientDeathBurst/, "Ancient death cloud is missing");
expect(projectile, /WITHER/, "Wither projectile mode is missing");
expect(projectile, /spawnLingeringWitherCloud/, "Wither effect cloud is missing");

for (const id of ids) {
  const constant = id.toUpperCase();
  expect(entities, new RegExp(`monster\\("${id}"`), `${id}: entity registration is missing`);
  expect(items, new RegExp(`"${id}_spawn_egg"`), `${id}: spawn egg is missing`);
  expect(attributes, new RegExp(`ModEntities\\.${constant}`), `${id}: attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id}: renderer is missing`);
  expect(creative, new RegExp(`ModItems\\.${constant}_SPAWN_EGG`), `${id}: creative tab entry is missing`);
  expect(english, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: English translation is missing`);
  expect(chinese, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: Chinese translation is missing`);

  for (const relative of [
    `src/main/resources/assets/csrp/geo/${id}.geo.json`,
    `src/main/resources/assets/csrp/animations/${id}.animation.json`,
    `src/main/resources/assets/csrp/textures/entity/${id}.png`,
    `src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`,
    `src/main/resources/assets/csrp/textures/item/${id}_spawn_egg.png`,
    `src/main/resources/data/csrp/loot_table/entities/${id}.json`
  ]) read(relative);
}

if (failures.length) {
  console.error("Ancient entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Ancient entity port verification passed (${ids.length} entities).`);
