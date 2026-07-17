const fs = require("fs");
const path = require("path");
const { groups, all } = require("./entity-port-manifest.cjs");

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

if (all.length !== 119) failures.push(`manifest contains ${all.length} IDs instead of 119`);
if (new Set(all).size !== all.length) failures.push("manifest contains duplicate IDs");

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");

const escaped = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
for (const id of all) {
  const literal = escaped(id);
  const constant = id.toUpperCase();
  if (!new RegExp(`(?:monster\\(|register\\()\\s*"${literal}"`).test(entities)) {
    failures.push(`${id}: entity type is missing`);
  }
  if (!items.includes(`"${id}_spawn_egg"`)) failures.push(`${id}: spawn egg is missing`);
  if (!client.includes(`ModEntities.${constant}`)) failures.push(`${id}: renderer is missing`);
  if (!english.includes(`"entity.csrp.${id}"`)) failures.push(`${id}: English name is missing`);
  if (!chinese.includes(`"entity.csrp.${id}"`)) failures.push(`${id}: Chinese name is missing`);

  for (const resource of [
    `assets/csrp/geo/${id}.geo.json`,
    `assets/csrp/animations/${id}.animation.json`,
    `assets/csrp/textures/entity/${id}.png`,
    `assets/csrp/models/item/${id}_spawn_egg.json`,
    `data/csrp/loot_table/entities/${id}.json`
  ]) {
    const full = path.join(root, "src/main/resources", resource);
    if (!fs.existsSync(full)) failures.push(`${id}: missing ${resource}`);
  }
}

if (failures.length) {
  console.error(`All-entity port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`All ${all.length} legacy bestiary entities passed aggregate verification.`);
console.log(`Groups: ${Object.entries(groups).map(([name, ids]) => `${name}=${ids.length}`).join(", ")}`);
