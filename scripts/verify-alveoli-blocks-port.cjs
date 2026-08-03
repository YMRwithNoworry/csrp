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

const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const alveoli = read("src/main/java/alku/csrp/block/AlveoliBlock.java");
const sick = read("src/main/java/alku/csrp/block/SickAlveoliBlock.java");
const growth = read("src/main/java/alku/csrp/block/AlveoliGrowthBlock.java");
const fluid = read("src/main/java/alku/csrp/item/AlveolarFluidItem.java");
const alveoliItem = read("src/main/java/alku/csrp/item/AlveoliItem.java");
const ids = ["alveoli", "sick_alveoli", "alveoli_growth", "solid_alveoli_block", "hair_follicle_block"];

for (const id of ids) {
  if (!blocks.includes(`"${id}"`)) failures.push(`${id}: block registration is missing`);
  if (!items.includes(`"${id}"`)) failures.push(`${id}: block item registration is missing`);
  parseJson(`src/main/resources/assets/csrp/blockstates/${id}.json`);
  parseJson(`src/main/resources/assets/csrp/models/item/${id}.json`);
  const loot = parseJson(`src/main/resources/data/csrp/loot_table/blocks/${id}.json`);
  if (loot?.pools?.[0]?.entries?.[0]?.name !== `csrp:${id}`) {
    failures.push(`${id}: self-drop loot entry is missing`);
  }
}
for (const expected of [
  'BooleanProperty.create("active")',
  'BooleanProperty.create("depleted")',
  "BRONCHIAL_SEARCH_RADIUS = 6",
  "RECOVERY_TICKS = 1_200",
  "Items.GLASS_BOTTLE",
  "ModItems.ALVEOLAR_FLUID",
  'contains("hair_follicle")'
]) {
  if (!alveoli.includes(expected)) failures.push(`Alveoli behavior missing: ${expected}`);
}
if (!sick.includes("MobEffects.HUNGER, 40, 0")) failures.push("Sick Alveoli must apply Hunger for 40 ticks");
for (const expected of [
  "Block.box(3.2D, 12.8D, 3.2D, 12.8D, 16.0D, 12.8D)",
  "pos.above()",
  "Direction.DOWN",
  "Shapes.empty()",
  "Blocks.AIR.defaultBlockState()"
]) {
  if (!growth.includes(expected)) failures.push(`Alveoli Growth behavior missing: ${expected}`);
}
for (const expected of [
  "MobEffects.NIGHT_VISION, EFFECT_DURATION_TICKS, 0",
  "MobEffects.MOVEMENT_SPEED, EFFECT_DURATION_TICKS, 0",
  "ModMobEffects.VIRAL, EFFECT_DURATION_TICKS, 2",
  "Items.GLASS_BOTTLE",
  "return 32",
  "UseAnim.DRINK"
]) {
  if (!fluid.includes(expected)) failures.push(`Alveolar Fluid behavior missing: ${expected}`);
}
for (const expected of ["return 32", "UseAnim.EAT", "stack.shrink(1)", "tooltip.csrp.alveoligrowth"]) {
  if (!alveoliItem.includes(expected)) failures.push(`Alveoli item behavior missing: ${expected}`);
}

const state = parseJson("src/main/resources/assets/csrp/blockstates/alveoli.json");
for (const active of [true, false]) {
  for (const depleted of [true, false]) {
    const key = `active=${active},depleted=${depleted}`;
    const expected = active ? "csrp:block/alveoli_active" : "csrp:block/alveoli_inactive";
    if (state?.variants?.[key]?.model !== expected) failures.push(`alveoli: incorrect model for ${key}`);
  }
}
for (const model of ["alveoli_active", "alveoli_inactive", "alveoli_growth", "sick_alveoli", "solid_alveoli_block", "hair_follicle_block"]) {
  parseJson(`src/main/resources/assets/csrp/models/block/${model}.json`);
}

const unpack = parseJson("src/main/resources/data/csrp/recipe/alveoli_from_solid_alveoli_block.json");
if (unpack?.ingredients?.[0]?.item !== "csrp:solid_alveoli_block"
    || unpack?.result?.id !== "csrp:alveoli" || unpack?.result?.count !== 2) {
  failures.push("solid alveoli unpacking recipe is incorrect");
}
const pack = parseJson("src/main/resources/data/csrp/recipe/solid_alveoli_block.json");
if (pack?.ingredients?.length !== 2 || pack.ingredients.some((entry) => entry.item !== "csrp:alveoli")
    || pack?.result?.id !== "csrp:solid_alveoli_block") {
  failures.push("solid alveoli packing recipe is incorrect");
}

const en = parseJson("src/main/resources/assets/csrp/lang/en_us.json");
const zh = parseJson("src/main/resources/assets/csrp/lang/zh_cn.json");
for (const id of ids) {
  if (!en?.[`block.csrp.${id}`]) failures.push(`${id}: English translation is missing`);
  if (!zh?.[`block.csrp.${id}`]) failures.push(`${id}: Chinese translation is missing`);
}
for (const key of ["item.csrp.alveolar_fluid", "tooltip.csrp.alveoligrowth"]) {
  if (!en?.[key] || !zh?.[key]) failures.push(`${key}: translation is missing`);
}

const textures = [
  ["block", "alveoli_block.png"],
  ["block", "sick_alveoli_block.png"],
  ["block", "alveoli_growth.png"],
  ["block", "solid_alveoli_block.png"],
  ["block", "hair_follicle.png"],
  ["block", "hair_follicle_side.png"],
  ["item", "alveolar_fluid.png"],
  ["item", "alveoli.png"]
];
for (const [folder, name] of textures) {
  const relative = `src/main/resources/assets/csrp/textures/${folder}/${name}`;
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
console.log("Alveoli block family port verification passed.");
