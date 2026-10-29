const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const source = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/registry/ModItems.java"), "utf8");
const blockSource = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/registry/ModBlocks.java"), "utf8");
const failures = [];
const assets = "src/main/resources/assets/csrp";
const exists = (relative) => fs.existsSync(path.join(root, relative));
const readJson = (relative) => {
  try {
    return JSON.parse(fs.readFileSync(path.join(root, relative), "utf8"));
  } catch (error) {
    failures.push(`${relative} is not valid JSON: ${error.message}`);
    return null;
  }
};

const ids = new Set();
const addMatches = (pattern) => {
  for (const match of source.matchAll(pattern)) ids.add(match[1]);
};
addMatches(/registerSimpleBlockItem\(\s*"([a-z0-9_]+)"/g);
addMatches(/legacyBlockItem\("([a-z0-9_]+)"\)/g);
addMatches(/parasiteLootBlockItem\(\s*"([a-z0-9_]+)"/g);
addMatches(/evolutionLure\(\s*"([a-z0-9_]+)"/g);
addMatches(/ITEMS\.registerItem\(\s*"([a-z0-9_]+)"\s*,\s*properties\s*->\s*new\s+(?:FogNullifierItem|ParasiteBarrierBlockItem)\b/g);

const colorList = blockSource.match(/String\[\]\s+colors\s*=\s*\{([^}]+)\}/);
if (!colorList) {
  failures.push("ModBlocks.registerEscaBulbs() color list could not be found");
} else {
  for (const match of colorList[1].matchAll(/"([a-z0-9_]*)"/g)) {
    ids.add(match[1] ? `esca_bulb_${match[1]}` : "esca_bulb");
  }
}
if (!source.includes("ITEMS.registerSimpleBlockItem(id, block)")) {
  failures.push("ModItems.registerEscaBulbItems() no longer registers each dynamic BlockItem");
}

const expectedParents = {
  dispatchern: "csrp:block/dodn",
  infestedbush: "csrp:block/infestedbush_infected",
  parasitebush: "csrp:block/parasitebush_tendril",
  parasitecanister: "csrp:block/parasitecanister_sac",
  parasiteplank: "csrp:block/parasiteplank_deadhead",
  parasiterubble: "csrp:block/parasiterubble_flesh",
  parasiterubbleslabhalf: "csrp:block/slabbone_bottom",
  parasitesapling: "csrp:item/parasitesapling_tree",
  parasitestain: "csrp:block/parasitestain_dirt",
  parasitestainslabhalf: "csrp:block/slabdirt_bottom"
};

function modelFile(modelId) {
  const [namespace, modelPath] = modelId.includes(":") ? modelId.split(":") : ["minecraft", modelId];
  if (namespace !== "csrp") return null;
  return `${assets}/models/${modelPath}.json`;
}

function verifyModel(id, modelId, seen = new Set()) {
  const model = modelFile(modelId);
  if (!model) return;
  if (!exists(model)) {
    failures.push(`${id} references missing model ${modelId}`);
    return;
  }
  if (seen.has(model)) return;
  seen.add(model);
  const json = readJson(model);
  if (!json) return;
  if (json.parent) verifyModel(id, json.parent, seen);
  for (const [slot, texture] of Object.entries(json.textures || {})) {
    const [namespace, texturePath] = texture.split(":");
    if (namespace === "csrp" && !exists(`${assets}/textures/${texturePath}.png`)) {
      failures.push(`${model} texture ${slot} references missing file ${texture}`);
    }
  }
}

for (const id of [...ids].sort()) {
  const definitionPath = `${assets}/items/${id}.json`;
  const modelPath = `${assets}/models/item/${id}.json`;
  if (!exists(definitionPath)) {
    failures.push(`registered BlockItem ${id} is missing ${definitionPath}`);
    continue;
  }
  if (!exists(modelPath)) {
    failures.push(`registered BlockItem ${id} is missing ${modelPath}`);
    continue;
  }
  const definition = readJson(definitionPath);
  if (!definition || definition.model?.type !== "minecraft:model" || definition.model.model !== `csrp:item/${id}`) {
    failures.push(`${definitionPath} must use minecraft:model pointing to csrp:item/${id}`);
    continue;
  }
  const itemModel = readJson(modelPath);
  if (!itemModel) continue;
  if (expectedParents[id] && itemModel.parent !== expectedParents[id]) {
    failures.push(`${modelPath} must preserve meta-0 model ${expectedParents[id]}`);
  }
  if (itemModel.parent) verifyModel(id, itemModel.parent);
  for (const [slot, texture] of Object.entries(itemModel.textures || {})) {
    const [namespace, texturePath] = texture.split(":");
    if (namespace === "csrp" && !exists(`${assets}/textures/${texturePath}.png`)) {
      failures.push(`${modelPath} texture ${slot} references missing file ${texture}`);
    }
  }
}

if (failures.length) {
  console.error(`Block item definition verification failed (${failures.length}):`);
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(`Block item definition verification passed (${ids.size} registered BlockItems checked).`);
