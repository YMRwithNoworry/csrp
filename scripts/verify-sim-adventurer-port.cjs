const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];

function read(relativePath) {
  const fullPath = path.join(root, relativePath);
  if (!fs.existsSync(fullPath)) {
    failures.push(`missing ${relativePath}`);
    return "";
  }
  return fs.readFileSync(fullPath, "utf8");
}

function expect(text, pattern, message) {
  if (!pattern.test(text)) failures.push(message);
}

function parseJson(relativePath) {
  const text = read(relativePath);
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (error) {
    failures.push(`invalid JSON ${relativePath}: ${error.message}`);
    return null;
  }
}

function validPng(relativePath) {
  const fullPath = path.join(root, relativePath);
  return fs.existsSync(fullPath)
    && fs.readFileSync(fullPath).subarray(0, 8).toString("hex") === "89504e470d0a1a0a";
}

function pngDimensions(relativePath) {
  const fullPath = path.join(root, relativePath);
  if (!validPng(relativePath)) return null;
  const data = fs.readFileSync(fullPath);
  return [data.readUInt32BE(16), data.readUInt32BE(20)];
}

const entityRegistry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const itemRegistry = read("src/main/java/alku/csrp/registry/ModItems.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const creativeTab = read("src/main/java/alku/csrp/Csrp.java");
const sounds = read("src/main/java/alku/csrp/registry/ModSounds.java");
const adventurer = read("src/main/java/alku/csrp/entity/SimAdventurerEntity.java");
const head = read("src/main/java/alku/csrp/entity/SimAdventurerHeadEntity.java");
const flesh = read("src/main/java/alku/csrp/entity/MovingFleshEntity.java");
const assimilated = read("src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java");
const variants = read("src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java");
const human = read("src/main/java/alku/csrp/entity/SimHumanEntity.java");
const meltSystem = read("src/main/java/alku/csrp/entity/AssimilatedMeltSystem.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");
const soundsJson = parseJson("src/main/resources/assets/csrp/sounds.json");

const creatures = [
  ["sim_adventurer", "SIM_ADVENTURER", "SIM_ADVENTURER", 64, 55],
  ["sim_adventurerhead", "SIM_ADVENTURER_HEAD", "SIM_ADVENTURER_HEAD", 64, 40],
  ["movingflesh", "MOVINGFLESH", "MOVING_FLESH", 64, 50]
];
for (const [id, entityConstant, itemConstant, textureWidth, textureHeight] of creatures) {
  expect(entityRegistry, new RegExp(`monster\\(\"${id}\"`), `${id}: entity registration is missing`);
  expect(itemRegistry, new RegExp(`\"${id}_spawn_egg\"`), `${id}: spawn egg registration is missing`);
  expect(attributes, new RegExp(`ModEntities\\.${entityConstant}`), `${id}: attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${entityConstant}`), `${id}: renderer is missing`);
  expect(creativeTab, new RegExp(`ModItems\\.${itemConstant}_SPAWN_EGG`), `${id}: creative-tab entry is missing`);
  expect(english, new RegExp(`\"entity\\.csrp\\.${id}\"`), `${id}: English name is missing`);
  expect(chinese, new RegExp(`\"entity\\.csrp\\.${id}\"`), `${id}: Chinese name is missing`);

  const jsonResources = [
    `src/main/resources/assets/csrp/geo/${id}.geo.json`,
    `src/main/resources/assets/csrp/animations/${id}.animation.json`,
    `src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`,
    `src/main/resources/data/csrp/loot_tables/entities/${id}.json`
  ];
  jsonResources.forEach(parseJson);
  if (!validPng(`src/main/resources/assets/csrp/textures/entity/${id}.png`)) {
    failures.push(`${id}: entity texture is missing or invalid`);
  }
  if (!validPng(`src/main/resources/assets/csrp/textures/item/${id}_spawn_egg.png`)) {
    failures.push(`${id}: custom spawn egg texture is missing or invalid`);
  }

  const geometry = parseJson(`src/main/resources/assets/csrp/geo/${id}.geo.json`);
  const animation = parseJson(`src/main/resources/assets/csrp/animations/${id}.animation.json`);
  if (geometry && animation) {
    const model = geometry["minecraft:geometry"][0];
    const description = model.description;
    if (description.texture_width !== textureWidth || description.texture_height !== textureHeight) {
      failures.push(`${id}: geometry texture size must be ${textureWidth}x${textureHeight}`);
    }
    const entityTexture = pngDimensions(`src/main/resources/assets/csrp/textures/entity/${id}.png`);
    if (!entityTexture || entityTexture[0] !== textureWidth || entityTexture[1] !== textureHeight) {
      failures.push(`${id}: entity texture size must be ${textureWidth}x${textureHeight}`);
    }
    const bones = new Set(model.bones.map((bone) => bone.name));
    for (const [clip, clipData] of Object.entries(animation.animations)) {
      for (const bone of Object.keys(clipData.bones ?? {})) {
        if (!bones.has(bone)) failures.push(`${id}/${clip} references missing bone ${bone}`);
      }
    }
  }

  const itemModel = parseJson(`src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`);
  if (itemModel?.textures?.layer0 !== `csrp:item/${id}_spawn_egg`) {
    failures.push(`${id}: spawn egg does not bind its custom texture`);
  }
}

for (const [source, checks] of [
  [adventurer, [
    /MAX_HEALTH, 15\.0D/, /ARMOR, 5\.0D/, /ATTACK_DAMAGE, 9\.0D/,
    /MELT_KILL_THRESHOLD = 10/, /THRALL_KILL_THRESHOLD = 15/,
    /MELT_MIN_HEIGHT = 0\.7F/, /MELT_HEIGHT_PER_TICK = 0\.01F/, /(?:getDefaultDimensions|getDimensions)\(Pose pose\)/,
    /COTH_AURA_RADIUS = 3\.0D/, /DamageTypeTags\.IS_FIRE\) \? amount \* 4\.0F/,
    /EquipmentSlot\.CHEST/, /spawnDeathBurst\(\)/, /SIM_ADVENTURER_HEAD/,
    /new ItemStack\(ModItems\.ASSIMILATED_FLESH/, /3 \+ random\.nextInt\(2\)/,
    /private void freezeMelting\(\)/, /MeltableAssimilated/,
    /AssimilatedMeltSystem\.tryStartGroup\(this, parasiteKills\)/, /finalizeSpawn\(ServerLevelAccessor/,
    /PLAYER_IDENTITY_NAMES/, /random\.nextFloat\(\) < EXPLOSION_CHANCE/,
    /random\.nextFloat\(\) < HEAD_SPAWN_CHANCE/
  ]],
  [head, [
    /MAX_HEALTH, 4\.5D/, /ATTACK_DAMAGE, 2\.7D/, /IncompleteFormMediumEntity/,
    /AvoidEntityGoal/, /shouldFleeInDaylight/, /DamageTypeTags\.IS_FIRE\) \? amount \* 4\.0F/,
    /finalizeSpawn\(ServerLevelAccessor/, /copyIdentity\(adventurer\)/,
    /isPersistenceRequired\(\)/
  ]],
  [flesh, [
    /REQUIRED_MERGES = 4/, /EVOLUTION_DELAY_TICKS = 70/, /AUTO_EVOLUTION_AGE_TICKS = 800/,
    /BASE_WIDTH = 0\.7F/, /BASE_HEIGHT = 0\.5F/, /(?:getDefaultDimensions|getDimensions)\(Pose pose\)/,
    /REGEN_PER_TICK = 0\.007F/, /EVOLUTION_FUSE_INCREMENT = 2/, /MergeMovingFleshGoal/,
    /DamageTypeTags\.IS_FIRE\) \? amount \* 4\.0F/, /mergeContacts/,
    /getRenderScale\(1\.0F\) >= other\.getRenderScale\(1\.0F\)/,
    /finalizeSpawn\(serverLevel, serverLevel\.getCurrentDifficultyAt\(blockPosition\(\)/,
    /isPersistenceRequired\(\)/, /EntityDataAccessor<Integer> MERGE_VALUE/,
    /setMergeValue\(getMergeValue\(\) \+ other\.getMergeValue\(\)\)/,
    /MobsConfig\.mergeSystemMobList\(\)/, /MobsConfig\.mergeSystemRandom\(\)/,
    /MobsConfig\.mergeSystemMobHealth\(\)/, /BuiltInRegistries\.ENTITY_TYPE/,
    /func_78087_a\.age_in_ticks/, /func_78087_a\.limb_swing/
  ]]
]) {
  checks.forEach((check) => expect(source, check, `missing behavior hook ${check}`));
}

for (const check of [
  /EntityDataAccessor<Boolean> MELTING/, /public void melt\(\)/,
  /kind != Kind\.SQUID/, /AssimilatedMeltSystem\.spawnMovingFlesh/,
  /getMeltRenderScale/
]) {
  expect(assimilated, check, `missing assimilated-unit melt hook ${check}`);
}

for (const [source, name] of [[variants, "AssimilatedVariantEntity"], [human, "SimHumanEntity"]]) {
  for (const check of [
    /implements GeoEntity, Parasite, MeltableAssimilated/,
    /EntityDataAccessor<Boolean> MELTING/,
    /AssimilatedMeltSystem\.tryStartGroup/,
    /AssimilatedMeltSystem\.spawnMovingFlesh/,
    /HOST_SKELETON_KILLS = 5/,
    /transformToHost\(level\)/
  ]) expect(source, check, `${name}: missing registered melt/Host hook ${check}`);
}

for (const check of [
  /KILL_THRESHOLD = 10/,
  /REQUIRED_NEARBY_ASSIMILATED = 3/,
  /movingFleshCount >= 1 && movingFleshCount <= 3/,
  /Config\.evolutionPhase\(serverLevel\)/,
  /flesh\.setMergeValue\(mergeValue\)/
]) expect(meltSystem, check, `missing shared assimilated merge hook ${check}`);

for (const sound of [
  "SIM_ADVENTURER_LIVING", "SIM_ADVENTURER_HURT", "SIM_ADVENTURER_DEATH", "SIM_ADVENTURER_MELT",
  "SIM_ADVENTURER_EXPLODE", "SIM_ADVENTURER_HEAD_LIVING", "SIM_ADVENTURER_HEAD_HURT",
  "SIM_ADVENTURER_HEAD_DEATH", "MOVING_FLESH_LIVING", "MOVING_FLESH_HURT", "MOVING_FLESH_DEATH",
  "MOVING_FLESH_EAT", "MOVING_FLESH_GROW", "MOVING_FLESH_PRIMITIVE"
]) expect(sounds, new RegExp(sound), `missing sound registration ${sound}`);

for (const id of [
  "sim_adventurer.living", "sim_adventurer.hurt", "sim_adventurer.death", "sim_adventurer.melt",
  "sim_adventurer.explode", "sim_adventurer_head.living", "sim_adventurer_head.hurt",
  "sim_adventurer_head.death", "moving_flesh.living", "moving_flesh.hurt", "moving_flesh.death",
  "moving_flesh.eat", "moving_flesh.grow", "moving_flesh.primitive"
]) {
  if (!soundsJson?.[id]) failures.push(`missing sounds.json entry ${id}`);
}

const adventurerLoot = parseJson("src/main/resources/data/csrp/loot_tables/entities/sim_adventurer.json");
if (adventurerLoot) {
  const serialized = JSON.stringify(adventurerLoot);
  if (!serialized.includes("csrp:assimilated_flesh") || !serialized.includes("0.4")) {
    failures.push("Assimilated Adventurer 40% Infected Flesh drop is missing");
  }
  if (!serialized.includes("csrp:lurecomponent2") || !serialized.includes("0.05")) {
    failures.push("Assimilated Adventurer 5% Diseased Heart drop is missing");
  }
}
for (const id of ["sim_adventurerhead", "movingflesh"]) {
  const loot = parseJson(`src/main/resources/data/csrp/loot_tables/entities/${id}.json`);
  if (loot && Array.isArray(loot.pools) && loot.pools.length !== 0) {
    failures.push(`${id}: should not gain undocumented loot`);
  }
}

if (failures.length) {
  console.error("Sim Adventurer port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Sim Adventurer, Walking Adventurer Head, and Moving Flesh port verification passed.");
