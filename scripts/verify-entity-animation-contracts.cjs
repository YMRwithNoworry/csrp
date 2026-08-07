const fs = require("fs");
const path = require("path");
const { all } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const animationKeys = new Set();

for (const id of all) {
  const resourceId = id === "sim_dragonhead" ? "sim_dragonehead" : id;
  const resolvedResourceId = resourceId === "crux_incomplete" ? "crux" : resourceId;
  const file = `src/main/resources/assets/csrp/animations/${resolvedResourceId}.animation.json`;
  const animations = JSON.parse(read(file)).animations;
  Object.keys(animations).forEach((key) => animationKeys.add(key));

  const shortKeys = new Set([
    "abo_head", "marauder_tendril", "marauder", "movingflesh", "pri_summoner",
    "sim_cow", "sim_cowhead", "sim_pig", "inf_sheep", "inf_sheep_head", "inf_villager"
  ]);
  const actionAliases = {
    pri_arachnida: "walk.get_parasite_status_2",
    pri_manducater: "idle.get_parasite_status_1",
    pri_reeker: "idle.get_parasite_status_1",
    sim_dragone: "walk.get_parasite_status_2",
    dispatcher_sii: "idle"
  };
  const baseActions = id === "dispatcher_sii" ? ["idle", "idle", "idle"] : ["idle", "walk", actionAliases[id] || "attack"];
  const expected = shortKeys.has(id)
    ? ["idle", "walk", id === "marauder" ? "attack" : "walk"]
    : baseActions.map((action) => `animation.${resolvedResourceId}.${action}`);
  for (const key of expected) {
    if (!(key in animations)) failures.push(`${id}: missing base animation key ${key}`);
  }
}

const entityDirectory = path.join(root, "src/main/java/alku/csrp/entity");
for (const name of fs.readdirSync(entityDirectory).filter((file) => file.endsWith(".java"))) {
  const source = fs.readFileSync(path.join(entityDirectory, name), "utf8");
  for (const match of source.matchAll(/["'](animation\.[a-z0-9_.]+)["']/g)) {
    if (!animationKeys.has(match[1])) failures.push(`${name}: unknown animation key ${match[1]}`);
  }
}

const helper = read("src/main/java/alku/csrp/entity/ParasiteAnimations.java");
if (!helper.includes('case "func_78087_a.getDigging" -> "get_dig_model.get_digging_1";')) {
  failures.push("burrower digging animation is not mapped to the extracted key");
}

const kirin = read("src/main/java/alku/csrp/entity/KirinEntity.java");
if (kirin.includes("animation.kirin.func_78087_a")) {
  failures.push("Kirin still requests obsolete pre-extraction animation keys");
}

const triggeredFamilies = [
  ["PrimitiveVariantEntity.java", "attack_controller"],
  ["AdaptedVariantEntity.java", "bolster_attack_controller"],
  ["AssimilatedVariantEntity.java", "attack_controller"],
  ["AssimilatedParasiteEntity.java", "attack_controller"],
  ["FeralParasiteEntity.java", "attack_controller"],
  ["PureParasiteEntity.java", "attack_controller"],
  ["PreeminentParasiteEntity.java", "attack_controller"],
  ["AncientParasiteEntity.java", "attack_controller"],
  ["DeterrentParasiteEntity.java", "attack_controller"]
];
for (const [file, controller] of triggeredFamilies) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  if (!source.includes(`triggerableAnim("attack"`)) {
    failures.push(`${file}: attack animation is not registered`);
  }
  if (!source.includes(`triggerAnim("${controller}", "attack")`)) {
    failures.push(`${file}: server attacks do not trigger the client animation`);
  }
}

if (failures.length) {
  console.error(`Entity animation contract verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Verified extracted animation contracts for all ${all.length} legacy bestiary entities.`);
console.log("Verified direct animation keys, burrower/Kirin mappings, and shared-family attack triggers.");
