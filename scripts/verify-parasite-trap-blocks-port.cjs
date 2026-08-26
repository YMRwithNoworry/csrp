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
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (error) {
    failures.push(`${relative}: invalid JSON (${error.message})`);
    return null;
  }
};

const trap = read("src/main/java/alku/csrp/block/ParasiteTrapBlock.java");
const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const damageTypes = read("src/main/java/alku/csrp/registry/ModDamageTypes.java");

expect(blocks, /"biomass_block"/, "Biomass Block registration is missing");
expect(blocks, /"parasitemouth"/, "Maw registration is missing");
expect(blocks, /"parasiterubble_stonedebris"/, "Hivestone Debris registration is missing");
expect(items, /BIOMASS_BLOCK/, "Biomass Block item registration is missing");
expect(items, /PARASITE_MOUTH/, "Maw item registration is missing");
expect(items, /HIVESTONE_DEBRIS/, "Hivestone Debris item registration is missing");
expect(trap, /BIOMASS\(20,/, "Biomass cooldown is not 20 ticks");
expect(trap, /MAW\(10,/, "Maw cooldown is not 10 ticks");
expect(trap, /living\.hurt\([^;]+1\.0F\)/s, "trap damage is not one point");
expect(trap, /CORROSION_DURATION_TICKS\s*=\s*100/, "Corrosion duration is not five seconds");
expect(trap, /VIRAL_DURATION_TICKS\s*=\s*200/, "Viral duration is not ten seconds");
expect(trap, /VIRAL_AMPLIFIER\s*=\s*1/, "Viral base amplifier is not Viral II");
expect(trap, /BIOMASS_COTH_DURATION_TICKS\s*=\s*1_000/, "Biomass COTH duration is not 50 seconds");
expect(trap, /BIOMASS_COTH_AMPLIFIER\s*=\s*3/, "Biomass COTH amplifier is not IV");
expect(trap, /EffectStacking\.apply\(living, ModMobEffects\.VIRAL/,
  "trap blocks do not use legacy Viral stacking");
expect(trap, /living instanceof Parasite/, "parasites are not immune to trap blocks");
expect(trap, /player\.getAbilities\(\)\.instabuild/, "creative players are not immune to trap blocks");
expect(trap, /isStandingOnTop/, "Maw does not require direct top contact");
expect(damageTypes, /PARASITE_MOUTH = key\("parasite_mouth"\)/,
  "Maw damage type key is missing");

const jsonFiles = [
  "src/main/resources/assets/csrp/blockstates/biomass_block.json",
  "src/main/resources/assets/csrp/blockstates/parasitemouth.json",
  "src/main/resources/assets/csrp/blockstates/parasiterubble_stonedebris.json",
  "src/main/resources/assets/csrp/models/block/biomass_block.json",
  "src/main/resources/assets/csrp/models/block/parasitemouth.json",
  "src/main/resources/assets/csrp/models/block/parasiterubble_stonedebris.json",
  "src/main/resources/assets/csrp/models/item/biomass_block.json",
  "src/main/resources/assets/csrp/models/item/parasitemouth.json",
  "src/main/resources/assets/csrp/models/item/parasiterubble_stonedebris.json",
  "src/main/resources/data/csrp/loot_table/blocks/biomass_block.json",
  "src/main/resources/data/csrp/loot_table/blocks/parasitemouth.json",
  "src/main/resources/data/csrp/loot_table/blocks/parasiterubble_stonedebris.json",
  "src/main/resources/data/csrp/recipes/biomass_block.json",
  "src/main/resources/data/csrp/recipes/hive_scrap_from_hivestone_debris.json",
  "src/main/resources/data/csrp/tags/item/lure_components.json",
  "src/main/resources/data/csrp/damage_type/biomass.json",
  "src/main/resources/data/csrp/damage_type/parasite_mouth.json",
  "src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json",
  "src/main/resources/data/minecraft/tags/block/needs_stone_tool.json",
  "src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json"
];
for (const file of jsonFiles) parseJson(file);

const recipe = parseJson("src/main/resources/data/csrp/recipes/biomass_block.json");
if (recipe?.result?.count !== 4) failures.push("Biomass recipe does not produce four blocks");
if (recipe?.key?.L?.tag !== "csrp:lure_components") {
  failures.push("Biomass recipe does not accept the lure component tag");
}
const smelting = parseJson("src/main/resources/data/csrp/recipes/hive_scrap_from_hivestone_debris.json");
if (smelting?.experience !== 0.1 || smelting?.result?.id !== "csrp:hive_scrap") {
  failures.push("Hivestone Debris smelting output or experience is incorrect");
}
const armorTag = parseJson("src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json");
for (const id of ["csrp:biomass", "csrp:parasite_mouth"]) {
  if (!armorTag?.values?.includes(id)) failures.push(`${id} does not bypass armor`);
}

for (const relative of [
  "src/main/resources/assets/csrp/textures/block/biomass_block.png",
  "src/main/resources/assets/csrp/textures/block/parasitemouth.png",
  "src/main/resources/assets/csrp/textures/block/parasitemouth_side.png",
  "src/main/resources/assets/csrp/textures/block/parasitestain_flesh.png",
  "src/main/resources/assets/csrp/textures/block/hivestone_debris.png",
  "src/main/resources/assets/csrp/textures/block/hivestone_debris_side.png"
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
console.log("Parasite trap blocks and Hivestone Debris port verification passed.");
