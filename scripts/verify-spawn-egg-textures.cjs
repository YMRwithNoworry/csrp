const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];

function readText(relativePath) {
  const fullPath = path.join(root, relativePath);
  if (!fs.existsSync(fullPath)) {
    failures.push(`missing ${relativePath}`);
    return "";
  }
  return fs.readFileSync(fullPath, "utf8");
}

function isPng(file) {
  const signature = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  return fs.existsSync(file) && fs.readFileSync(file).subarray(0, signature.length).equals(signature);
}

const itemSource = readText("src/main/java/alku/csrp/registry/ModItems.java");
const spawnEggIds = [...new Set([...itemSource.matchAll(/"([a-z0-9_]+_spawn_egg)"/g)]
  .map((match) => match[1]))].sort();

if (!spawnEggIds.length) failures.push("no registered spawn eggs found");

for (const id of spawnEggIds) {
  const modelPath = `src/main/resources/assets/csrp/models/item/${id}.json`;
  const texturePath = `src/main/resources/assets/csrp/textures/item/${id}.png`;
  const modelText = readText(modelPath);
  if (!modelText) continue;

  let model;
  try {
    model = JSON.parse(modelText);
  } catch (error) {
    failures.push(`${id} has invalid item model JSON: ${error.message}`);
    continue;
  }

  if (model.parent !== "minecraft:item/generated") {
    failures.push(`${id} must use minecraft:item/generated instead of a vanilla spawn-egg template`);
  }
  if (model.textures?.layer0 !== `csrp:item/${id}`) {
    failures.push(`${id} does not bind its custom layer0 texture`);
  }
  if (!isPng(path.join(root, texturePath))) {
    failures.push(`${id} is missing a valid custom PNG texture`);
  }
}

if (failures.length) {
  console.error("Spawn egg texture verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Spawn egg texture verification passed (${spawnEggIds.length} custom egg textures).`);
