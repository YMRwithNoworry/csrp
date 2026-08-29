const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = ["grunt", "bomber_light", "monarch", "overseer", "seeker", "vigilante", "warden"];
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
const pure = read("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const projectile = read("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");

expect(pure, /MAX_ADAPTATION_HITS\s*=\s*8/, "Pure damage-adaptation cap is missing");
expect(pure, /ADAPTATION_PER_HIT\s*=\s*0\.125F/, "Pure adaptation reduction is missing");
expect(pure, /MAX_LEARNABLE_DAMAGE_SOURCES\s*=\s*12/, "Pure learnable-source cap is missing");
expect(pure, /DamageTypeTags\.IS_FIRE/, "Pure fire weakness is missing");
expect(pure, /LightBomberBombGoal/, "Light Bomber bombardment is missing");
expect(pure, /MonarchWebVolleyGoal/, "Monarch web projectile behavior is missing");
expect(pure, /OverseerVolleyGoal/, "Overseer rapid volley behavior is missing");
expect(pure, /OverseerSummonGoal/, "Overseer summoning behavior is missing");
expect(pure, /SeekerRandomFlightGoal/, "Seeker random-flight behavior is missing");
expect(pure, /tickSeekerScent/, "Seeker Scent behavior is missing");
expect(pure, /VigilanteRangedGoal/, "Vigilante lingering projectile behavior is missing");
expect(pure, /WardenChargeGoal/, "Warden charge behavior is missing");
expect(pure, /WardenShockwaveGoal/, "Warden shockwave behavior is missing");
expect(pure, /trySummonSupport/, "Pure deterrent support behavior is missing");
expect(projectile, /NEEDLE/, "Overseer Needler projectile mode is missing");

for (const id of ids) {
  const constant = id.toUpperCase();
  expect(entities, new RegExp(`monster\\("${id}"`), `${id}: entity registration is missing`);
  expect(items, new RegExp(`"${id}_spawn_egg"`), `${id}: spawn egg is missing`);
  expect(attributes, new RegExp(`ModEntities\\.${constant}`), `${id}: attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id}: renderer is missing`);
  expect(creative, new RegExp(`ModItems\\.${constant}_SPAWN_EGG`), `${id}: creative tab entry is missing`);
  expect(english, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: English translation is missing`);
  expect(chinese, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: Chinese translation is missing`);

  const resources = [
    `src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`,
    `src/main/resources/assets/csrp/textures/item/${id}_spawn_egg.png`,
    `src/main/resources/data/csrp/loot_tables/entities/${id}.json`
  ];
  if (id !== "seeker") {
    resources.push(
      `src/main/resources/assets/csrp/geo/${id}.geo.json`,
      `src/main/resources/assets/csrp/animations/${id}.animation.json`,
      `src/main/resources/assets/csrp/textures/entity/${id}.png`
    );
  }
  for (const relative of resources) read(relative);
}

if (failures.length) {
  console.error("Pure entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Pure entity port verification passed (${ids.length} entities).`);
