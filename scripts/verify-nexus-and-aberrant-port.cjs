const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = [
  "beckon_si", "beckon_sii", "beckon_siii", "beckon_siv",
  "dispatcher_si", "dispatcher_sii", "dispatcher_siii", "dispatcher_siv",
  "rooter_si", "rooter_sii", "rooter_siii", "rooter_siv", "rooterball",
  "abo_bodies", "abo_head"
];
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
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
const abomination = read("src/main/java/alku/csrp/entity/AbominationEntity.java");

for (const feature of [
  "BECKON_SIV", "DISPATCHER_SIV", "ROOTER_SIV", "ROOTERBALL", "DamageTypeTags.IS_FIRE",
  "spawnBombVolley", "spawnPodVolley", "spawnRootmassCysts", "rootmassCystsInRange",
  "rootmassCystRange", "rootmassCystSpawnLimit", "summonBeckonParasites",
  "summonDispatcherDefenses", "applyRooterSupport", "createStormVortex", "evolve"
]) {
  if (!nexus.includes(feature)) failures.push(`Nexus behavior is missing ${feature}`);
}
expect(nexus, /float sharedDamage = amount \/ cysts\.size\(\);/,
  "Rootmass Cyst damage is not split across nearby cysts");
expect(nexus, /else \{\s*spawnRootmassCysts\(activeKind\.rootmassCystSpawnLimit\(\)\);\s*\}/,
  "Rooter does not create Rootmass Cysts before taking an unprotected hit");
for (const [stage, range, limit] of [["SI", 16, 3], ["SII", 32, 4], ["SIII", 48, 5], ["SIV", 128, 6]]) {
  expect(nexus, new RegExp(`ROOTER_${stage} -> ${range}`),
    `Rooter stage ${stage} is missing its Rootmass Cyst range`);
  expect(nexus, new RegExp(`ROOTER_${stage} -> ${limit}`),
    `Rooter stage ${stage} is missing its Rootmass Cyst spawn limit`);
}
for (const feature of ["Kind.BODIES", "FastMeleeAttackGoal", "DamageTypeTags.IS_FIRE", "applyBodiesSupport"]) {
  if (!abomination.includes(feature)) failures.push(`Abomination behavior is missing ${feature}`);
}
expect(abomination, /MAX_HEALTH, 17\.0D/, "Abomination health is missing");
expect(abomination, /ARMOR, 10\.0D/, "Abomination armor is missing");
expect(abomination, /ATTACK_DAMAGE, 9\.0D/, "Abomination attack damage is missing");

for (const id of ids) {
  const constant = id.toUpperCase();
  expect(entities, new RegExp(`monster\\("${id}"`), `${id}: entity registration is missing`);
  expect(items, new RegExp(`"${id}_spawn_egg"`), `${id}: spawn egg is missing`);
  expect(attributes, new RegExp(`ModEntities\\.${constant}`), `${id}: attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id}: renderer is missing`);
  expect(creative, new RegExp(`ModItems\\.${constant}_SPAWN_EGG`), `${id}: creative entry is missing`);
  expect(english, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: English translation is missing`);
  expect(chinese, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: Chinese translation is missing`);

  for (const relative of [
    `src/main/resources/assets/csrp/geo/${id}.geo.json`,
    `src/main/resources/assets/csrp/animations/${id}.animation.json`,
    `src/main/resources/assets/csrp/textures/entity/${id}.png`,
    `src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`,
    `src/main/resources/assets/csrp/textures/item/${id}_spawn_egg.png`,
    `src/main/resources/data/csrp/loot_table/entities/${id}.json`
  ]) {
    read(relative);
  }
}

if (failures.length) {
  console.error("Nexus and aberrant entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Nexus and aberrant entity port verification passed (${ids.length} entities).`);
