const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const exists = (relative) => fs.existsSync(path.join(root, relative));
const sha256 = (relative) => crypto.createHash("sha256").update(fs.readFileSync(path.join(root, relative))).digest("hex");

const entity = read("src/main/java/alku/csrp/entity/MarauderEntity.java");
const tendril = read("src/main/java/alku/csrp/entity/MarauderTendrilEntity.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const sounds = read("src/main/java/alku/csrp/registry/ModSounds.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const creative = read("src/main/java/alku/csrp/Csrp.java");
const model = read("src/main/java/alku/csrp/client/model/MarauderModel.java");
const tendrilModel = read("src/main/java/alku/csrp/client/model/MarauderTendrilModel.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");
const soundsJson = read("src/main/resources/assets/csrp/sounds.json");
const matrix = read("docs/ENTITY_PORTING_MATRIX.md");

for (const [source, hooks] of [
  [entity, ["TENDRIL_HEALTH_FRACTION", "SMASH_DURATION_TICKS = 100", "ensureAttachedTendrils", "trySummonSupportTendril", "hurtTendril", "ModMobEffects.RAGE"]],
  [tendril, ["Mode.ATTACHED", "Mode.DETACHED", "Mode.TELEPORT", "Mode.SNARE", "updateAttachedPosition", "tickSnareSupport"]],
  [model, ["geo/marauder.geo.json", "marauder_hardened.png", "taclejointLA0", "taclejointRA0"]],
  [tendrilModel, ["geo/marauder_tendril.geo.json", "marauder_tendril.animation.json"]]
]) {
  for (const hook of hooks) {
    if (!source.includes(hook)) failures.push(`missing behavior or model hook: ${hook}`);
  }
}

for (const [source, hook, description] of [
  [entities, "\"marauder\"", "Marauder entity registration"],
  [entities, "\"marauder_tendril\"", "Marauder tendril registration"],
  [items, "\"marauder_spawn_egg\"", "Marauder spawn egg"],
  [sounds, "MARAUDER_LIVING", "Marauder living sound registration"],
  [attributes, "ModEntities.MARAUDER", "Marauder attributes"],
  [attributes, "ModEntities.MARAUDER_TENDRIL", "Marauder tendril attributes"],
  [client, "MarauderRenderer::new", "Marauder renderer"],
  [client, "MarauderTendrilRenderer::new", "Marauder tendril renderer"],
  [creative, "ModItems.MARAUDER_SPAWN_EGG", "Marauder creative-tab entry"],
  [english, "\"entity.csrp.marauder\"", "Marauder English name"],
  [chinese, "\"entity.csrp.marauder\"", "Marauder Chinese name"],
  [soundsJson, "\"marauder.living\"", "Marauder sounds.json entry"],
  [matrix, "Progress: **49 / 119**", "Marauder matrix progress"]
]) {
  if (!source.includes(hook)) failures.push(`missing ${description}`);
}

const jsonResources = [
  "src/main/resources/assets/csrp/geo/marauder.geo.json",
  "src/main/resources/assets/csrp/geo/marauder_tendril.geo.json",
  "src/main/resources/assets/csrp/animations/marauder.animation.json",
  "src/main/resources/assets/csrp/animations/marauder_tendril.animation.json",
  "src/main/resources/assets/csrp/models/item/marauder_spawn_egg.json",
  "src/main/resources/data/csrp/loot_table/entities/marauder.json"
];
for (const resource of jsonResources) {
  if (!exists(resource)) {
    failures.push(`missing ${resource}`);
    continue;
  }
  try {
    JSON.parse(read(resource));
  } catch (error) {
    failures.push(`invalid JSON ${resource}: ${error.message}`);
  }
}

const marauderGeo = JSON.parse(read("src/main/resources/assets/csrp/geo/marauder.geo.json"));
const marauderBones = new Set(marauderGeo["minecraft:geometry"][0].bones.map((bone) => bone.name));
for (const bone of ["mainbody", "jointLL", "jointRL", "jointLA1", "jointRA1", "taclejointLA0", "taclejointRA0"]) {
  if (!marauderBones.has(bone)) failures.push(`Marauder geometry is missing legacy bone ${bone}`);
}

const tendrilGeo = JSON.parse(read("src/main/resources/assets/csrp/geo/marauder_tendril.geo.json"));
if (tendrilGeo["minecraft:geometry"][0].description.identifier !== "geometry.marauder_tendril") {
  failures.push("Marauder tendril geometry identifier is wrong");
}

const animation = JSON.parse(read("src/main/resources/assets/csrp/animations/marauder.animation.json"));
for (const clip of ["idle", "walk", "smash", "swipe"]) {
  if (!animation.animations[clip]) failures.push(`Marauder animation clip is missing: ${clip}`);
}
const tendrilAnimation = JSON.parse(read("src/main/resources/assets/csrp/animations/marauder_tendril.animation.json"));
for (const clip of ["idle", "walk"]) {
  if (!tendrilAnimation.animations[clip]) failures.push(`Marauder tendril animation clip is missing: ${clip}`);
}

const pngResources = [
  "src/main/resources/assets/csrp/textures/entity/marauder.png",
  "src/main/resources/assets/csrp/textures/entity/marauder_hardened.png",
  "src/main/resources/assets/csrp/textures/entity/marauder_tendril.png",
  "src/main/resources/assets/csrp/textures/item/marauder_spawn_egg.png"
];
for (const resource of pngResources) {
  if (!exists(resource) || fs.readFileSync(path.join(root, resource)).subarray(0, 8).toString("hex") !== "89504e470d0a1a0a") {
    failures.push(`missing or invalid PNG ${resource}`);
  }
}

const sourceTextureHashes = {
  "src/main/resources/assets/csrp/textures/entity/marauder.png": "e757962cc9c567fdffd73da202d65294c3a7a8b89824b662fd6854915c25c2ee",
  "src/main/resources/assets/csrp/textures/entity/marauder_hardened.png": "f0b56dccfc9906f989e93157ddd52f04d5db334e9fbe6f825b90511b2d088655",
  "src/main/resources/assets/csrp/textures/entity/marauder_tendril.png": "ccc38247bf48039e8f5f9a741a1011ef4d7cb99c895ed8aab35004903ad7efff",
  "src/main/resources/assets/csrp/textures/item/marauder_spawn_egg.png": "1d0ff28e10d7a0b0a791372ed7629aa6161500ca636a34ae6e42b4349d2b712d"
};
for (const [resource, expected] of Object.entries(sourceTextureHashes)) {
  if (exists(resource) && sha256(resource) !== expected) failures.push(`legacy texture changed: ${resource}`);
}

for (const sound of ["living1", "living2", "living3", "hurt1", "hurt2", "hurt3", "death"]) {
  const resource = `src/main/resources/assets/csrp/sounds/mob/pure/marauder/${sound}.ogg`;
  if (!exists(resource) || fs.readFileSync(path.join(root, resource)).subarray(0, 4).toString() !== "OggS") {
    failures.push(`missing or invalid Marauder sound ${sound}`);
  }
}

const loot = JSON.parse(read("src/main/resources/data/csrp/loot_table/entities/marauder.json"));
if (!JSON.stringify(loot).includes("csrp:lurecomponent5") || !JSON.stringify(loot).includes("0.8")) {
  failures.push("Marauder legacy lurecomponent5 80% drop is missing");
}

if (failures.length) {
  console.error("Marauder port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Verified Marauder legacy behavior hooks, source resources, GeckoLib assets, sounds, loot, and registrations.");
