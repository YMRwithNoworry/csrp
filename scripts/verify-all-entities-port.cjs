const fs = require("fs");
const path = require("path");
const { groups, all, behaviorPorts } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (file) => {
  const full = path.join(root, file);
  if (!fs.existsSync(full)) {
    failures.push(`missing ${file}`);
    return "";
  }
  return fs.readFileSync(full, "utf8");
};

if (all.length !== 127) failures.push(`manifest contains ${all.length} IDs instead of 127`);
if (new Set(all).size !== all.length) failures.push("manifest contains duplicate IDs");
for (const [id, originalClass] of [
  ["grunt", "EntityFlog"],
  ["bomber_light", "EntityOmboo"],
  ["monarch", "EntityOrch"],
  ["overseer", "EntityAlafha"],
  ["vigilante", "EntityAnged"]
]) {
  if (behaviorPorts[id]?.originalClass !== originalClass
      || behaviorPorts[id]?.status !== "audited"
      || behaviorPorts[id]?.auditScope !== "entity-specific") {
    failures.push(`${id}: entity-specific behavior audit metadata is missing`);
  }
  if (!fs.existsSync(path.join(root, behaviorPorts[id]?.verifier ?? ""))) {
    failures.push(`${id}: behavior verifier is missing`);
  }
}

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const common = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const creative = read("src/main/java/alku/csrp/Csrp.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");

const escaped = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
const constants = {
  sim_adventurerhead: "SIM_ADVENTURER_HEAD",
  sim_cowhead: "SIM_COW_HEAD",
  sim_dragonehead: "SIM_DRAGON_HEAD",
  sim_endermanhead: "SIM_ENDERMAN_HEAD",
  sim_horsehead: "SIM_HORSE_HEAD",
  sim_humanhead: "SIM_HUMAN_HEAD",
  sim_pighead: "SIM_PIG_HEAD",
  sim_sheephead: "SIM_SHEEP_HEAD",
  sim_villagerhead: "SIM_VILLAGER_HEAD",
  sim_wolfhead: "SIM_WOLF_HEAD"
};
const legacyRendererless = new Set(["carrier_worm", "seeker"]);
for (const id of all) {
  const literal = escaped(id);
  const constant = constants[id] ?? id.toUpperCase();
  if (!new RegExp(`(?:monster\\(|register\\()\\s*"${literal}"`).test(entities)) {
    failures.push(`${id}: entity type is missing`);
  }
  if (!items.includes(`"${id}_spawn_egg"`)) failures.push(`${id}: spawn egg is missing`);
  const itemMatch = items.match(new RegExp(
    `([A-Z0-9_]+)_SPAWN_EGG\\s*=\\s*spawnEgg\\(\\s*"${literal}_spawn_egg"`
  ));
  if (itemMatch && !creative.includes(`ModItems.${itemMatch[1]}_SPAWN_EGG`)) {
    failures.push(`${id}: creative-tab entry is missing`);
  }
  if (!common.includes(`ModEntities.${constant}`)) failures.push(`${id}: attributes are missing`);
  if (!client.includes(`ModEntities.${constant}`)) failures.push(`${id}: renderer is missing`);
  if (!english.includes(`"entity.csrp.${id}"`)) failures.push(`${id}: English name is missing`);
  if (!chinese.includes(`"entity.csrp.${id}"`)) failures.push(`${id}: Chinese name is missing`);

  const resources = [
    `assets/csrp/models/item/${id}_spawn_egg.json`,
    `assets/csrp/textures/item/${id}_spawn_egg.png`,
    `data/csrp/loot_table/entities/${id}.json`
  ];
  if (!legacyRendererless.has(id)) {
    resources.push(
      `assets/csrp/geo/${id}.geo.json`,
      `assets/csrp/animations/${id}.animation.json`,
      `assets/csrp/textures/entity/${id}.png`
    );
  }
  for (const resource of resources) {
    const full = path.join(root, "src/main/resources", resource);
    if (!fs.existsSync(full)) failures.push(`${id}: missing ${resource}`);
  }
}

if (failures.length) {
  console.error(`All-entity port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`All ${all.length} legacy registered creatures passed aggregate verification.`);
console.log(`Groups: ${Object.entries(groups).map(([name, ids]) => `${name}=${ids.length}`).join(", ")}`);
