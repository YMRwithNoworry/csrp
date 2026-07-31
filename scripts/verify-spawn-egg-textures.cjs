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
const texturedEggSource = readText("src/main/java/alku/csrp/item/TexturedSpawnEggItem.java");
const sharedModelPath = "src/main/resources/assets/csrp/models/item/spawn_egg.json";
const sharedModelText = readText(sharedModelPath);
const spawnEggIds = [...new Set([...itemSource.matchAll(/"([a-z0-9_]+_spawn_egg)"/g)]
  .map((match) => match[1]))].sort();

if (!spawnEggIds.length) failures.push("no registered spawn eggs found");

if (itemSource.includes("new SpawnEggItem(")) {
  failures.push("spawn eggs must use TexturedSpawnEggItem to avoid vanilla color multiplication");
}
if (!/int getColor\(int tintIndex\)[\s\S]*?return 0xFFFFFF;/.test(texturedEggSource)) {
  failures.push("TexturedSpawnEggItem must return a white tint for custom full-color textures");
}

if (sharedModelText) {
  let sharedModel;
  try {
    sharedModel = JSON.parse(sharedModelText);
  } catch (error) {
    failures.push(`shared spawn-egg model has invalid JSON: ${error.message}`);
  }
  if (sharedModel) {
    if (sharedModel.parent !== "minecraft:item/generated") {
      failures.push("shared spawn-egg model must inherit minecraft:item/generated");
    }
    if (sharedModel.gui_light !== "front") {
      failures.push("shared spawn-egg model must use front GUI light to preserve custom texture brightness");
    }
    if (sharedModel.ambientocclusion !== false) {
      failures.push("shared spawn-egg model must disable ambient occlusion in the item GUI");
    }
  }
}

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

  if (model.parent !== "csrp:item/spawn_egg") {
    failures.push(`${id} must inherit the front-lit csrp:item/spawn_egg model`);
  }
  if (model.textures?.layer0 !== `csrp:item/${id}`) {
    failures.push(`${id} does not bind its custom layer0 texture`);
  }
  const textureFile = path.join(root, texturePath);
  if (!isPng(textureFile)) {
    failures.push(`${id} is missing a valid custom PNG texture`);
    continue;
  }

  const texture = fs.readFileSync(textureFile);
  if (texture.length <= 100) {
    failures.push(`${id} texture is still a generated placeholder`);
  }
  const width = texture.readUInt32BE(16);
  const height = texture.readUInt32BE(20);
  if (width !== 16 || height !== 16) {
    failures.push(`${id} texture must be 16x16, found ${width}x${height}`);
  }
  if (!texture.includes(Buffer.from("IDAT"))) {
    failures.push(`${id} texture has no PNG image data`);
  }
}

if (failures.length) {
  console.error("Spawn egg texture verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Spawn egg texture verification passed (${spawnEggIds.length} custom egg textures).`);
