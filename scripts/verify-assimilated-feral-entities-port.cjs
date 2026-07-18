const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const ids = [
  "sim_bear", "sim_cow", "sim_pig", "sim_sheep", "sim_wolf", "sim_squid",
  "fer_bear", "fer_cow", "fer_pig", "fer_sheep", "fer_wolf"
];
const read = (file) => {
  const full = path.join(root, file);
  if (!fs.existsSync(full)) {
    failures.push(`missing ${file}`);
    return "";
  }
  return fs.readFileSync(full, "utf8");
};
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const events = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const mod = read("src/main/java/alku/csrp/Csrp.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");
const assimilated = read("src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java");
const feral = read("src/main/java/alku/csrp/entity/FeralParasiteEntity.java");
const model = read("src/main/java/alku/csrp/client/model/AssimilatedParasiteModel.java");
const squidBreathingTag = read("src/main/resources/data/minecraft/tags/entity_type/can_breathe_under_water.json");

expect(assimilated, /FERAL_KILL_THRESHOLD\s*=\s*60/, "Feral transformation threshold is missing");
expect(assimilated, /parasiteKills\s*>\s*FERAL_KILL_THRESHOLD/, "Feral conversion must retain the legacy strict threshold");
expect(assimilated, /DamageTypeTags\.IS_FIRE.*amount \* 4\.0F/, "Assimilated fire weakness is missing");
expect(assimilated, /COTH_DURATION_TICKS\s*=\s*4_800/, "Assimilated COTH duration is missing");
expect(assimilated, /CowChargeGoal/, "Assimilated Cow charge is missing");
expect(assimilated, /PREPARE_TICKS\s*=\s*40/, "Assimilated Cow charge wind-up is missing");
expect(assimilated, /RandomSwimmingGoal/, "Assimilated Squid water movement is missing");
expect(assimilated, /getBoundingBox\(\)\.inflate\(1\.75D\)/, "Assimilated Squid area attack is missing");
expect(squidBreathingTag, /"csrp:sim_squid"/, "Assimilated Squid water-breathing tag is missing");
expect(assimilated, /rollSheepTextureVariant/, "Assimilated Sheep variants are missing");
expect(assimilated, /setTamedWolfTexture\(random\.nextInt\(100\) == 0\)/,
  "Assimilated Wolf tamed texture chance is missing");
expect(feral, /REGEN_AMOUNT\s*=\s*3\.0F/, "Feral recovery amount is missing");
expect(feral, /REGEN_KILL_INTERVAL\s*=\s*10/, "Feral recovery interval is missing");
expect(feral, /DamageTypeTags\.IS_FIRE.*amount \* 4\.0F/, "Feral fire weakness is missing");
expect(model, /getTextureResource\(AssimilatedParasiteEntity/, "Assimilated dynamic texture model is missing");

for (const id of ids) {
  const constant = id.toUpperCase();
  expect(entities, new RegExp(`monster\\("${id}"`), `${id}: entity type is missing`);
  expect(items, new RegExp(`"${id}_spawn_egg"`), `${id}: spawn egg is missing`);
  expect(events, new RegExp(`ModEntities\\.${constant}`), `${id}: attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id}: renderer is missing`);
  expect(mod, new RegExp(`${constant}_SPAWN_EGG`), `${id}: spawn egg is absent from the spawn-egg tab`);
  expect(english, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: English name is missing`);
  expect(chinese, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: Chinese name is missing`);

  for (const resource of [
    `geo/${id}.geo.json`, `animations/${id}.animation.json`, `textures/entity/${id}.png`,
    `models/item/${id}_spawn_egg.json`, `textures/item/${id}_spawn_egg.png`
  ]) read(`src/main/resources/assets/csrp/${resource}`);
  read(`src/main/resources/data/csrp/loot_table/entities/${id}.json`);

  const itemModel = read(`src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`);
  expect(itemModel, new RegExp(`csrp:item/${id}_spawn_egg`), `${id}: custom spawn egg texture is not wired`);

  const geometryText = read(`src/main/resources/assets/csrp/geo/${id}.geo.json`);
  const animationText = read(`src/main/resources/assets/csrp/animations/${id}.animation.json`);
  if (geometryText && animationText) {
    const geometry = JSON.parse(geometryText);
    const animations = JSON.parse(animationText);
    const bones = new Set(geometry["minecraft:geometry"][0].bones.map((bone) => bone.name));
    for (const [animationName, animation] of Object.entries(animations.animations)) {
      for (const bone of Object.keys(animation.bones ?? {})) {
        if (!bones.has(bone)) failures.push(`${id}/${animationName} references missing bone ${bone}`);
      }
    }
  }
}

for (const texture of ["sim_sheep_grey.png", "sim_sheep_black.png", "sim_wolf_tamed.png"]) {
  read(`src/main/resources/assets/csrp/textures/entity/${texture}`);
}

if (failures.length) {
  console.error("Assimilated and Feral entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Assimilated and Feral entity port verification passed (11 entities).");
