const fs = require("fs");
const path = require("path");
const { all } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const animationKeys = new Set();
const shortKeys = new Set([
  "abo_head", "marauder_tendril", "marauder", "movingflesh", "pri_summoner",
  "sim_cow", "sim_cowhead", "sim_pig", "inf_sheep", "inf_sheep_head", "inf_villager"
]);

function resolvedAnimationKey(id, requestedAction) {
  const resourceId = id === "sim_dragonhead" ? "sim_dragonehead"
    : id === "dispatcher_tentacle" ? "dispatcherten"
      : id === "crux_incomplete" ? "crux" : id;
  let action = ["run", "fly"].includes(requestedAction) ? "walk"
    : ["spawn", "throw", "smash", "swipe", "melee_attack", "ranged_attack", "burst"].includes(requestedAction)
      ? "attack"
      : requestedAction === "func_78087_a.getDigging" ? "get_dig_model.get_digging_1"
        : requestedAction === "animation" ? "idle" : requestedAction;

  if (action === "attack" && shortKeys.has(resourceId) && !["abo_head", "marauder"].includes(resourceId)) {
    return "walk";
  }
  if (action === "attack") {
    action = {
      pri_arachnida: "walk.get_parasite_status_2",
      pri_manducater: "idle.get_parasite_status_1",
      pri_reeker: "idle.get_parasite_status_1",
      sim_dragone: "walk.get_parasite_status_2",
      dispatcher_sii: "idle"
    }[resourceId] || action;
  }
  if (resourceId === "marauder" && action.includes("get_parasite_status")) action = "skill";
  else if (resourceId === "pri_summoner" && action === "summon") action = "run";
  else if (resourceId === "sim_cow" && action === "idle.get_parasite_status_3.get_still_ani_1") action = "idle";
  else if (resourceId === "sim_cow" && action === "walk.get_parasite_status_3") action = "run";
  else if (resourceId === "ada_arachnida" && action === "idle.get_parasite_status_11") action = "idle.get_parasite_status_3";
  else if (resourceId === "ada_summoner" && action === "idle.get_parasite_status_100") action = "idle.get_parasite_status_25";
  else if (resourceId === "ada_manducater" && action === "idle.get_parasite_status_10") action = "idle.get_parasite_status_3";
  else if (resourceId === "ada_manducater" && action === "idle.get_parasite_status_25") action = "walk.get_parasite_status_2";
  return shortKeys.has(resourceId) ? action : `animation.${resourceId}.${action}`;
}

for (const id of all) {
  const resourceId = id === "sim_dragonhead" ? "sim_dragonehead" : id;
  const resolvedResourceId = resourceId === "crux_incomplete" ? "crux" : resourceId;
  const file = `src/main/resources/assets/csrp/animations/${resolvedResourceId}.animation.json`;
  const animations = JSON.parse(read(file)).animations;
  Object.keys(animations).forEach((key) => animationKeys.add(key));

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

const registrations = read("src/main/java/alku/csrp/registry/ModEntities.java");
for (const match of registrations.matchAll(/monster\("([a-z0-9_]+)",\s*(\w+)::new/g)) {
  const [, id, className] = match;
  const source = read(`src/main/java/alku/csrp/entity/${className}.java`);
  const resourceId = id === "sim_dragonhead" ? "sim_dragonehead" : id === "crux_incomplete" ? "crux" : id;
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${resourceId}.animation.json`)).animations;
  for (const request of source.matchAll(/ParasiteAnimations\.(?:loop|play)\(this,\s*"([a-zA-Z0-9_.]+)"/g)) {
    const key = resolvedAnimationKey(id, request[1]);
    if (!(key in animations)) failures.push(`${id}/${className}: unresolved requested animation ${key}`);
  }
}

const sharedVariantActions = {
  pri_arachnida: ["idle", "walk", "run", "attack"],
  pri_bolster: ["idle", "walk", "run", "attack"],
  pri_burrower: ["idle", "func_78087_a.getDigging", "attack", "idle.get_body_number_1",
    "idle.get_body_number_2", "get_dig_model.get_body_number_1.get_digging_1",
    "get_dig_model.get_body_number_2.get_digging_1"],
  pri_devourer: ["idle", "walk", "attack"],
  pri_manducater: ["idle", "walk", "run", "attack"],
  pri_reeker: ["idle", "walk", "attack", "idle.get_parasite_status_3.get_still_ani_1",
    "walk.get_parasite_status_3"],
  pri_tozoon: ["idle", "func_78087_a.getDigging", "attack", "idle.get_body_number_1",
    "idle.get_body_number_2", "get_attack_timer.get_body_number_1", "get_attack_timer.get_body_number_2",
    "get_dig_model.get_body_number_1.get_digging_1", "get_dig_model.get_body_number_2.get_digging_1"],
  pri_yelloweye: ["fly", "attack"],
  ada_arachnida: ["idle", "walk", "attack", "walk.get_parasite_status_1",
    "walk.get_parasite_status_2", "idle.get_parasite_status_3", "idle.get_parasite_status_11"],
  ada_bolster: ["idle", "walk", "attack", "idle.get_parasite_status_3",
    "idle.get_parasite_status_15", "idle.get_parasite_status_25",
    "get_attack_timer.get_parasite_status_15", "get_attack_timer.get_parasite_status_25"],
  ada_burrower: ["idle", "func_78087_a.getDigging", "attack", "idle.get_body_number_1",
    "idle.get_body_number_2", "idle.get_body_number_3", "get_dig_model.get_body_number_1.get_digging_1",
    "get_dig_model.get_body_number_2.get_digging_1", "get_dig_model.get_body_number_3.get_digging_1"],
  ada_devourer: ["idle", "walk", "attack"],
  ada_longarms: ["idle", "walk", "run", "attack"],
  ada_manducater: ["idle", "walk", "run", "attack", "walk.get_parasite_status_1",
    "idle.get_parasite_status_10", "idle.get_parasite_status_25"],
  ada_reeker: ["idle", "walk", "attack", "idle.get_parasite_status_1", "walk.get_parasite_status_2",
    "idle.get_parasite_status_3", "walk.get_parasite_status_3", "idle.get_parasite_status_3.get_still_ani_1"],
  ada_summoner: ["idle", "walk", "run", "attack", "idle.get_parasite_status_10",
    "walk.get_parasite_status_1", "idle.get_parasite_status_100", "idle.get_parasite_status_25"],
  ada_tozoon: ["idle", "func_78087_a.getDigging", "attack", "idle.get_body_number_1",
    "idle.get_body_number_2", "idle.get_body_number_3", "get_attack_timer.get_body_number_1",
    "get_attack_timer.get_body_number_2", "get_attack_timer.get_body_number_3",
    "get_dig_model.get_body_number_1.get_digging_1", "get_dig_model.get_body_number_2.get_digging_1",
    "get_dig_model.get_body_number_3.get_digging_1"],
  ada_vermin: ["idle"],
  ada_viscera: ["idle", "walk", "run", "attack"],
  ada_yelloweye: ["fly", "attack", "idle.get_parasite_status_1"]
  ,sim_bear: ["idle", "walk", "run", "attack"]
  ,sim_cow: ["idle", "walk", "run", "attack", "idle.get_parasite_status_3.get_still_ani_1",
    "walk.get_parasite_status_3"]
  ,sim_pig: ["idle", "walk", "run", "attack"]
  ,sim_sheep: ["idle", "walk", "run", "attack"]
  ,sim_wolf: ["idle", "walk", "run", "attack"]
  ,sim_squid: ["idle", "walk", "run", "attack"]
  ,sim_bigspider: ["idle", "walk", "run", "attack", "idle.get_parasite_status_1",
    "walk.get_parasite_status_1"]
  ,sim_horse: ["idle", "walk", "run", "attack"]
  ,sim_villager: ["idle", "walk", "run", "attack"]
  ,grunt: ["idle", "walk", "run", "attack", "idle.get_parasite_status_10"]
  ,bomber_light: ["idle", "fly", "attack"]
  ,monarch: ["idle", "walk", "run", "attack", "idle.get_parasite_status_10"]
  ,overseer: ["idle", "fly", "attack"]
  ,vigilante: ["idle", "walk", "run", "attack", "idle.get_parasite_status_1",
    "walk.get_parasite_status_1", "idle.get_parasite_status_25"]
  ,warden: ["idle", "walk", "run", "attack", "idle.get_parasite_status_3",
    "walk.get_parasite_status_3", "idle.get_parasite_status_10"]
  ,anc_dreadnaut: ["idle", "walk", "attack", "idle.get_parasite_status_77"]
  ,anc_overlord: ["idle", "walk", "attack"]
  ,dispatcherten: ["idle", "attack"]
  ,kyphosis: ["idle", "attack", "idle.get_parasite_status_3"]
  ,seizer: ["idle", "attack", "idle.get_targeted_entity_1"]
  ,sentry: ["idle", "attack", "idle.get_parasite_status_1", "idle.get_parasite_status_2",
    "idle.get_parasite_status_3"]
  ,worm: ["idle", "attack", "get_attack_timer"]
};
for (const [id, actions] of Object.entries(sharedVariantActions)) {
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${id}.animation.json`)).animations;
  for (const action of actions) {
    const key = resolvedAnimationKey(id, action);
    if (!(key in animations)) failures.push(`${id}/shared variant: unresolved requested animation ${key}`);
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
