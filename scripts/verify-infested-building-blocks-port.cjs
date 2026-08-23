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
const ids = [
  "infested_stone_bricks",
  "infested_terracotta",
  "infested_stone_polished",
  "residue_bricks",
  "infested_column",
  "inf_ss",
  "inf_ss_chiseled",
  "inf_ss_cut"
];
const stagedIds = ids.filter((id) => id !== "infested_column");

for (const id of ids) {
  if (!blocks.includes(`"${id}"`)) failures.push(`${id}: block registration is missing`);
  if (!items.includes(`"${id}"`)) failures.push(`${id}: block item registration is missing`);
}
if (!blocks.includes("DeferredBlock<RotatedPillarBlock> INFESTED_COLUMN")
    && !blocks.includes("RegistryObject<RotatedPillarBlock> INFESTED_COLUMN")) {
  failures.push("Infested Column is not an axis-aware pillar block");
}
for (const expected of [
  '"infested_stone_bricks", 1.5F, 10.0F',
  '"infested_terracotta", 1.25F, 4.2F',
  '"infested_stone_polished", 1.5F, 10.0F',
  '"residue_bricks", 1.5F, 10.0F',
  '"inf_ss", 0.8F, 4.0F',
  '"inf_ss_chiseled", 0.8F, 4.0F',
  '"inf_ss_cut", 0.8F, 4.0F'
]) {
  if (!blocks.includes(expected)) failures.push(`legacy block properties missing for ${expected}`);
}

for (const id of stagedIds) {
  const statePath = `src/main/resources/assets/csrp/blockstates/${id}.json`;
  const state = parseJson(statePath);
  for (let stage = 0; stage <= 3; stage++) {
    if (state?.variants?.[`stage=${stage}`]?.model !== `csrp:block/${id}`) {
      failures.push(`${id}: stage=${stage} blockstate is missing or points to the wrong model`);
    }
  }
}
const columnState = parseJson("src/main/resources/assets/csrp/blockstates/infested_column.json");
for (const axis of ["x", "y", "z"]) {
  if (columnState?.variants?.[`axis=${axis}`]?.model !== "csrp:block/infested_column") {
    failures.push(`infested_column: axis=${axis} blockstate is missing`);
  }
}

for (const id of ids) {
  parseJson(`src/main/resources/assets/csrp/models/block/${id}.json`);
  const itemModel = parseJson(`src/main/resources/assets/csrp/models/item/${id}.json`);
  if (itemModel?.parent !== `csrp:block/${id}`) failures.push(`${id}: item model parent is incorrect`);
  const loot = parseJson(`src/main/resources/data/csrp/loot_table/blocks/${id}.json`);
  const entry = loot?.pools?.[0]?.entries?.[0];
  if (entry?.name !== `csrp:${id}`) failures.push(`${id}: self-drop loot entry is missing`);
}

const recipes = {
  "inf_ss_cut.json": ["csrp:inf_ss_cut", 4],
  "inf_ss_chiseled.json": ["csrp:inf_ss_chiseled", 4],
  "infested_column_from_polished.json": ["csrp:infested_column", 2],
  "infested_terracotta_from_clay.json": ["csrp:infested_terracotta", 2],
  "residue_bricks_from_residue_block.json": ["csrp:residue_bricks", 4]
};
for (const [file, [id, count]] of Object.entries(recipes)) {
  const recipe = parseJson(`src/main/resources/data/csrp/recipe/${file}`);
  if (recipe?.result?.id !== id || recipe?.result?.count !== count) {
    failures.push(`${file}: output must be ${count} ${id}`);
  }
}
const cutRecipe = parseJson("src/main/resources/data/csrp/recipe/inf_ss_cut.json");
if (cutRecipe?.key?.["#"]?.item !== "csrp:infestedsand") {
  failures.push("inf_ss_cut.json: input must be Infested Sand");
}
const chiseledRecipe = parseJson("src/main/resources/data/csrp/recipe/inf_ss_chiseled.json");
if (chiseledRecipe?.key?.["#"]?.item !== "csrp:inf_ss_cut") {
  failures.push("inf_ss_chiseled.json: input must be Chiseled Cut Sandstone");
}
const columnRecipe = parseJson("src/main/resources/data/csrp/recipe/infested_column_from_polished.json");
if (columnRecipe?.key?.["#"]?.item !== "csrp:infested_stone_polished") {
  failures.push("infested_column_from_polished.json: input must be Polished Infested Stone");
}
const terracottaRecipe = parseJson("src/main/resources/data/csrp/recipe/infested_terracotta_from_clay.json");
const terracottaIngredients = terracottaRecipe?.ingredients?.map((ingredient) => ingredient.item) ?? [];
for (const id of ["minecraft:terracotta", "csrp:infestedstain"]) {
  if (!terracottaIngredients.includes(id)) failures.push(`infested_terracotta_from_clay.json: missing ${id}`);
}
const residueRecipe = parseJson("src/main/resources/data/csrp/recipe/residue_bricks_from_residue_block.json");
if (residueRecipe?.key?.["#"]?.item !== "csrp:residue_block") {
  failures.push("residue_bricks_from_residue_block.json: input must be Residue Block");
}

const pickaxe = parseJson("src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json");
const stoneTool = parseJson("src/main/resources/data/minecraft/tags/block/needs_stone_tool.json");
for (const id of ids) {
  if (!pickaxe?.values?.includes(`csrp:${id}`)) failures.push(`${id}: missing from pickaxe tag`);
}
for (const id of stagedIds) {
  if (!stoneTool?.values?.includes(`csrp:${id}`)) failures.push(`${id}: missing from stone tool tag`);
}

const en = parseJson("src/main/resources/assets/csrp/lang/en_us.json");
const zh = parseJson("src/main/resources/assets/csrp/lang/zh_cn.json");
for (const id of ids) {
  const key = `block.csrp.${id}`;
  if (!en?.[key]) failures.push(`${id}: English translation is missing`);
  if (!zh?.[key]) failures.push(`${id}: Chinese translation is missing`);
}

const textures = [
  "infested_stone_bricks.png",
  "infested_terracotta.png",
  "infested_stone_polished.png",
  "residue_bricks.png",
  "infested_column_side.png",
  "infested_column_top.png",
  "inf_ss.png",
  "inf_ss_top.png",
  "inf_ss_down.png",
  "inf_ss_chiseled.png",
  "inf_ss_cut.png"
];
for (const name of textures) {
  const relative = `src/main/resources/assets/csrp/textures/block/${name}`;
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
console.log("Infested building blocks port verification passed.");
