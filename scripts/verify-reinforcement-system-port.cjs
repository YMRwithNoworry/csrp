const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
};
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};
const parseJson = (relative) => {
  const text = read(relative);
  if (!text) return;
  try {
    JSON.parse(text);
  } catch (error) {
    failures.push(`${relative}: invalid JSON (${error.message})`);
  }
};

const system = read("src/main/java/alku/csrp/world/ReinforcementSystem.java");
const data = read("src/main/java/alku/csrp/world/SrpWorldData.java");
const carrier = read("src/main/java/alku/csrp/entity/CarrierEntity.java");
const infested = read("src/main/java/alku/csrp/block/InfestedBlock.java");
const residue = read("src/main/java/alku/csrp/block/InfestedResidueBlock.java");
const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");

expect(system, /MAX_BECKONS\s*=\s*5/, "Beckon world cap is not five");
expect(system, /COOLDOWN_TICKS\s*=\s*40/, "reinforcement cooldown is not 40 ticks");
expect(system, /MINIMUM_BECKON_DISTANCE\s*=\s*32\.0D/, "nearby Beckon exclusion distance is not 32 blocks");
expect(system, /5_500, 4_000, 1_000, 500, 400, 300, 200, 150/, "residue phase intervals are incorrect");
expect(system, /0\.04F, 0\.06F, 0\.08F, 0\.10F, 0\.14F, 0\.16F, 0\.18F, 0\.20F/,
  "parasite death phase chances are incorrect");
expect(system, /colonyPoints > 40/, "stage III Colony threshold is missing");
expect(system, /colonyPoints > 20/, "stage II Colony threshold is missing");
expect(data, /reinforcement_cooldown_end/, "reinforcement cooldown is not persisted");
expect(carrier, /ModBlocks\.INFESTED_REMAINS/, "Carrier does not place Infested Residue");
expect(infested, /tryFromInfestedBlock/, "infested blocks do not trigger reinforcement attempts");
expect(residue, /tryFromResidue/, "Infested Residue does not trigger reinforcement attempts");
expect(residue, /multiply\(0\.84D, 1\.0D, 0\.86D\)/, "Infested Residue slowdown is incorrect");
expect(residue, /InfectionMechanics\.applyCoth/, "Infested Residue does not apply COTH");
expect(blocks, /"residue_block"/, "Residue Block registration is missing");
expect(blocks, /"infestremain"/, "Infested Residue registration is missing");
expect(items, /RESIDUE_BLOCK/, "Residue Block item registration is missing");
expect(items, /INFESTED_REMAINS/, "Infested Residue item registration is missing");

for (const relative of [
  "src/main/resources/assets/csrp/blockstates/infestremain.json",
  "src/main/resources/assets/csrp/blockstates/residue_block.json",
  "src/main/resources/assets/csrp/blockstates/residue_plants.json",
  "src/main/resources/assets/csrp/models/block/infestremain.json",
  "src/main/resources/assets/csrp/models/block/residue_block_base.json",
  "src/main/resources/assets/csrp/models/block/residue_plant_cross_2.json",
  "src/main/resources/assets/csrp/models/block/residue_plant_cross_3.json",
  "src/main/resources/assets/csrp/models/item/infestremain.json",
  "src/main/resources/assets/csrp/models/item/residue_block.json",
  "src/main/resources/data/csrp/loot_table/blocks/infestremain.json",
  "src/main/resources/data/csrp/loot_table/blocks/residue_block.json",
  "src/main/resources/data/csrp/loot_table/blocks/residue_plants.json",
  "src/main/resources/data/csrp/recipe/residue_block_from_infestremain.json"
]) parseJson(relative);

for (const relative of [
  "src/main/resources/assets/csrp/textures/block/infestremain.png",
  "src/main/resources/assets/csrp/textures/block/residue_sprout_2.png",
  "src/main/resources/assets/csrp/textures/block/residue_sprout_3.png",
  "src/main/resources/assets/csrp/textures/item/residue_item.png"
]) {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
  } else if (fs.readFileSync(file).subarray(0, 8).toString("hex") !== "89504e470d0a1a0a") {
    failures.push(`${relative}: invalid PNG signature`);
  }
}

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log("Reinforcement system and residue block port verification passed.");
