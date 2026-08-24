const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const originalRoot = "D:\\code\\模组反编译器\\decompiled\\[逃逸：寄生体] SRParasites-1.10.8\\assets\\srparasites";
const failures = [];

const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const exists = (relative) => fs.existsSync(path.join(root, relative));

const itemResources = {
  greek_fire: "greek_fire",
  discthree: "disc3",
  mobility_armor_helmet: "mobility_armor_helmet",
  mobility_armor_chestpiece: "mobility_armor_chestpiece",
  mobility_armor_leggings: "mobility_armor_leggings",
  mobility_armor_boots: "mobility_armor_boots"
};

for (const [id, texture] of Object.entries(itemResources)) {
  const modelPath = `src/main/resources/assets/csrp/models/item/${id}.json`;
  const texturePath = `src/main/resources/assets/csrp/textures/item/${texture}.png`;
  if (!exists(modelPath)) {
    failures.push(`missing model: ${id}`);
    continue;
  }
  const model = JSON.parse(read(modelPath));
  if (model.parent !== "minecraft:item/generated") failures.push(`${id}: invalid parent`);
  if (model.textures?.layer0 !== `csrp:item/${texture}`) failures.push(`${id}: invalid texture`);
  if (!exists(texturePath)) failures.push(`missing texture: ${texture}`);
}

const bucket = JSON.parse(read("src/main/resources/assets/csrp/models/item/deadblood_bucket.json"));
if (bucket.parent !== "forge:item/default") failures.push("deadblood bucket must use Forge model parent");
if (bucket.loader !== "forge:fluid_container") failures.push("deadblood bucket must use Forge fluid loader");
if (bucket.fluid !== "csrp:deadblood") failures.push("deadblood bucket fluid id mismatch");
if (bucket.textures?.fluid !== "csrp:block/dead_blood_still") {
  failures.push("deadblood bucket fluid texture mismatch");
}

for (const layer of ["mobility_layer_1.png", "mobility_layer_2.png"]) {
  if (!exists(`src/main/resources/assets/csrp/textures/models/armor/${layer}`)) {
    failures.push(`missing worn armor texture: ${layer}`);
  }
}

const originalFiles = {
  "textures/item/greek_fire.png": "textures/items/greek_fire.png",
  "textures/item/disc3.png": "textures/items/disc3.png",
  "textures/item/mobility_armor_helmet.png": "textures/items/mobility_armor_helmet.png",
  "textures/item/mobility_armor_chestpiece.png": "textures/items/mobility_armor_chestpiece.png",
  "textures/item/mobility_armor_leggings.png": "textures/items/mobility_armor_leggings.png",
  "textures/item/mobility_armor_boots.png": "textures/items/mobility_armor_boots.png",
  "textures/models/armor/mobility_layer_1.png": "textures/models/armor/mobility_armor.png",
  "textures/models/armor/mobility_layer_2.png": "textures/models/armor/mobility_armor.png"
};

for (const [current, original] of Object.entries(originalFiles)) {
  const currentBytes = fs.readFileSync(path.join(root, "src/main/resources/assets/csrp", current));
  const originalBytes = fs.readFileSync(path.join(originalRoot, original));
  if (!currentBytes.equals(originalBytes)) failures.push(`${current}: differs from original asset`);
}

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}

console.log("Missing equipment/item models and original textures verified.");
