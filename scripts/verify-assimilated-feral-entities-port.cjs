const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const ids = [
  "sim_bear", "sim_cow", "sim_pig", "sim_sheep", "sim_wolf", "sim_squid",
  "fer_bear", "fer_cow", "fer_horse", "fer_human", "fer_pig", "fer_sheep", "fer_villager",
  "fer_wolf"
];
const remainingAssimilatedIds = [
  "sim_bigspider", "sim_dragone", "sim_dragonhead", "sim_enderman", "sim_endermanhead",
  "sim_horse", "sim_horsehead", "sim_human", "sim_humanhead", "sim_cowhead", "sim_pighead",
  "sim_sheephead", "sim_villager", "sim_villagerhead", "sim_wolfhead"
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
const variants = read("src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java");
const heads = read("src/main/java/alku/csrp/entity/AssimilatedHeadEntity.java");
const enderman = read("src/main/java/alku/csrp/entity/AssimilatedEndermanEntity.java");
const dragon = read("src/main/java/alku/csrp/entity/AssimilatedDragonEntity.java");
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
expect(feral, /MeleeAttackGoal\(this, 1\.5D, false\)/, "Feral legacy melee speed is missing");
expect(feral, /HORSE\(37\.0D, 16\.0D, 3\.0D, 0\.6D, 0\.2775D/,
  "Feral Horse legacy attributes are missing");
expect(feral, /HUMAN\(24\.0D, 15\.0D, 7\.0D, 0\.3D, 0\.26D/,
  "Feral Human legacy attributes are missing");
expect(feral, /VILLAGER\(27\.0D, 17\.0D, 8\.0D, 0\.9D, 0\.26D/,
  "Feral Villager legacy attributes are missing");
expect(entities, /"fer_horse"[\s\S]*?1\.3964844F, 1\.75F/,
  "Feral Horse legacy dimensions are missing");
expect(entities, /"fer_human"[\s\S]*?0\.6F, 1\.95F/,
  "Feral Human legacy dimensions are missing");
expect(entities, /"fer_villager"[\s\S]*?0\.6F, 1\.95F/,
  "Feral Villager legacy dimensions are missing");
for (const id of ["fer_horse", "fer_human", "fer_villager"]) {
  expect(client, new RegExp(`"${id}", 0\\.5F`), `${id}: legacy shadow radius is missing`);
}
expect(model, /getTextureResource\(AssimilatedParasiteEntity/, "Assimilated dynamic texture model is missing");
expect(variants, /HEAD_SPAWN_CHANCE\s*=\s*0\.5F/, "Remaining assimilated head chance is missing");
expect(variants, /parasiteKills\s*>\s*AssimilatedParasiteEntity\.FERAL_KILL_THRESHOLD/,
  "Assimilated horse, human, and villager feral transition is missing");
expect(heads, /IncompleteFormMediumEntity/, "Walking heads must rebuild from medium incomplete forms");
expect(enderman, /TARGET_GRACE_TICKS\s*=\s*80/, "Assimilated Enderman target grace period is missing");
expect(enderman, /teleportAllyToTarget/, "Assimilated Enderman ally teleport is missing");
expect(enderman, /FeralEndermanEntity/, "Assimilated Enderman feral transition is missing");
expect(dragon, /PART_HEALTH\s*=\s*52\.0F/, "Assimilated Dragon detachable-part health is missing");
expect(dragon, /detachHead\(\)/, "Assimilated Dragon head detachment is missing");
expect(dragon, /canFly\(\)/, "Assimilated Dragon wing-dependent flight is missing");
expect(dragon, /isMultipartEntity\(\)/, "Assimilated Dragon multipart hitboxes are missing");

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

for (const id of remainingAssimilatedIds) {
  const constant = {
    sim_dragonhead: "SIM_DRAGON_HEAD",
    sim_endermanhead: "SIM_ENDERMAN_HEAD",
    sim_horsehead: "SIM_HORSE_HEAD",
    sim_humanhead: "SIM_HUMAN_HEAD",
    sim_cowhead: "SIM_COW_HEAD",
    sim_pighead: "SIM_PIG_HEAD",
    sim_sheephead: "SIM_SHEEP_HEAD",
    sim_villagerhead: "SIM_VILLAGER_HEAD",
    sim_wolfhead: "SIM_WOLF_HEAD"
  }[id] ?? id.toUpperCase();
  expect(entities, new RegExp(`monster\\("${id}"`), `${id}: entity type is missing`);
  expect(items, new RegExp(`"${id}_spawn_egg"`), `${id}: spawn egg is missing`);
  expect(events, new RegExp(`ModEntities\\.${constant}`), `${id}: attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id}: renderer is missing`);
  expect(mod, new RegExp(`${id.toUpperCase()}_SPAWN_EGG`), `${id}: spawn egg is absent from the spawn-egg tab`);
  expect(english, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: English name is missing`);
  expect(chinese, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: Chinese name is missing`);
  for (const resource of [
    `geo/${id}.geo.json`, `animations/${id}.animation.json`, `textures/entity/${id}.png`,
    `models/item/${id}_spawn_egg.json`, `textures/item/${id}_spawn_egg.png`
  ]) read(`src/main/resources/assets/csrp/${resource}`);
  read(`src/main/resources/data/csrp/loot_table/entities/${id}.json`);
}

for (const texture of ["sim_sheep_grey.png", "sim_sheep_black.png", "sim_wolf_tamed.png"]) {
  read(`src/main/resources/assets/csrp/textures/entity/${texture}`);
}

if (failures.length) {
  console.error("Assimilated and Feral entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Assimilated and Feral entity port verification passed (${ids.length + remainingAssimilatedIds.length} entities).`);
