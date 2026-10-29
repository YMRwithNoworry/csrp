const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const itemDefinitionPath = path.join(root, "src/main/resources/assets/csrp/items/deadblood_bucket.json");
const oldModelPath = path.join(root, "src/main/resources/assets/csrp/models/item/deadblood_bucket.json");
const itemDefinition = JSON.parse(fs.readFileSync(itemDefinitionPath, "utf8"));
const model = itemDefinition.model;

if (model?.type !== "neoforge:fluid_container") {
    throw new Error("Dead Blood Bucket must use the registered NeoForge fluid_container item model");
}
if (model.fluid !== "csrp:deadblood") {
    throw new Error("Dead Blood Bucket model points to the wrong fluid");
}
if (model.textures?.base !== "minecraft:item/bucket"
        || model.textures?.fluid !== "csrp:block/dead_blood_still") {
    throw new Error("Dead Blood Bucket model uses unexpected base or fluid textures");
}
if (fs.existsSync(oldModelPath)) {
    throw new Error("Obsolete legacy Dead Blood Bucket geometry model still exists");
}

console.log("Dead Blood Bucket item-model contract ok");
