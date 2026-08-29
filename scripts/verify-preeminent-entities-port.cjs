const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = ["bogle", "carrier_colony", "haunter", "bomber_heavy", "wraith", "succor"];
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
const preeminent = read("src/main/java/alku/csrp/entity/PreeminentParasiteEntity.java");
const succor = read("src/main/java/alku/csrp/entity/FlamEntity.java");

expect(preeminent, /MAX_ADAPTATION_HITS\s*=\s*5/, "Preeminent damage-adaptation cap is missing");
expect(preeminent, /ADAPTATION_PER_HIT\s*=\s*0\.20F/, "Preeminent adaptation reduction is missing");
expect(preeminent, /MAX_LEARNABLE_DAMAGE_SOURCES\s*=\s*20/, "Preeminent learnable-source cap is missing");
expect(preeminent, /DamageTypeTags\.IS_FIRE/, "Preeminent fire weakness is missing");
expect(preeminent, /amount\s*\*\s*4\.0F/, "Preeminent fire damage is not quadrupled");
expect(preeminent, /LegacyProjectileAttackGoal\(60,\s*30,\s*3\)/, "Bogle bombardment cadence is missing");
expect(preeminent, /Mode\.LENCIA_BALL/, "Bogle explosive projectile is missing");
expect(preeminent, /CarrierBuffGoal/, "Colony Carrier support aura is missing");
expect(preeminent, /HaunterHomingBurstGoal/, "Haunter homing burst is missing");
expect(preeminent, /HeavyBomberBombGoal/, "Heavy Bomber payload is missing");
expect(preeminent, /LegacyProjectileAttackGoal\(20,\s*10,\s*4\)/, "Wraith projectile cadence is missing");
expect(preeminent, /Mode\.ELVIA_NADE/, "Wraith nade burst is missing");
expect(preeminent, /trySummonFlam/, "Preeminent Succor support is missing");
expect(preeminent, /ModEntities\.SUCCOR/, "Preeminent Succor entity creation is missing");
expect(succor, /DamageTypeTags\.IS_FIRE/, "Succor fire weakness is missing");
expect(succor, /amount\s*\*\s*4\.0F/, "Succor fire damage is not quadrupled");
expect(succor, /ACTION_EXPLODE\s*=\s*1/, "Succor explosion action is missing");
expect(succor, /ACTION_ORB\s*=\s*2/, "Succor orb action is missing");
expect(succor, /ACTION_TELEPORT\s*=\s*3/, "Succor teleport action is missing");
expect(succor, /completeAction\(\)/, "Succor utility action completion is missing");
expect(succor, /performExplosion\(\)/, "Succor explosion behavior is missing");
expect(succor, /spawnScaryOrb\(/, "Succor orb behavior is missing");
expect(succor, /completeTeleportAction\(/, "Succor teleport behavior is missing");

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
    `src/main/resources/data/csrp/loot_tables/entities/${id}.json`
  ]) read(relative);
}

if (failures.length) {
  console.error("Preeminent entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Preeminent entity port verification passed (${ids.length} entities).`);
