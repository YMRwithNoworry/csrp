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
const json = (relative) => {
  const text = read(relative);
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (error) {
    failures.push(`${relative}: invalid JSON (${error.message})`);
    return null;
  }
};

const slabs = [
  ["infested_cobblestone_slab", "infested_cobblestone"],
  ["infested_stone_slab", "infestedrubble"],
  ["infested_dirt_slab", "infestedstain"],
  ["infested_stone_brick_slab", "infested_stone_bricks"],
  ["infested_terracotta_slab", "infested_terracotta"],
  ["polished_infested_stone_slab", "infested_stone_polished"],
  ["residue_brick_slab", "residue_bricks"],
  ["infested_sandstone_slab", "inf_ss"],
  ["infested_plank_slab", "infested_planks"]
];
const stairs = [
  ["infested_sandstone_stairs", "inf_ss"],
  ["residue_stairs", "residue_bricks"],
  ["infested_planks_stairs", "infested_planks"],
  ["infested_stone_bricks_stairs", "infested_stone_bricks"],
  ["infested_polished_stone_bricks_stairs", "infested_stone_polished"],
  ["infested_stone_stairs", "infestedrubble"]
];
const walls = [
  ["residue_wall", "residue_bricks"],
  ["infested_plank_wall", "infested_planks"],
  ["polished_infested_stone_wall", "infested_stone_polished"],
  ["infested_stone_brick_wall", "infested_stone_bricks"],
  ["infested_sandstone_wall", "inf_ss"],
  ["infestedrubble_wall", "infestedrubble"],
  ["infestedstain_wall", "infestedstain"]
];
const ids = [...slabs, ...stairs, ...walls].map(([id]) => id);
const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");

for (const id of ids) {
  if (!blocks.includes(`"${id}"`)) failures.push(`${id}: missing block registration`);
  if (!items.includes(`"${id}"`)) failures.push(`${id}: missing block item registration`);
  const loot = json(`src/main/resources/data/csrp/loot_tables/blocks/${id}.json`);
  if (loot?.pools?.[0]?.entries?.[0]?.name !== `csrp:${id}`) failures.push(`${id}: incorrect self-drop`);
  const model = json(`src/main/resources/assets/csrp/models/item/${id}.json`);
  const expectedParent = walls.some(([wall]) => wall === id) ? `csrp:block/${id}_inventory` : `csrp:block/${id}`;
  if (model?.parent !== expectedParent) failures.push(`${id}: incorrect item model parent`);
}

for (const [id, base] of slabs) {
  const state = json(`src/main/resources/assets/csrp/blockstates/${id}.json`);
  if (state?.variants?.["type=bottom"]?.model !== `csrp:block/${id}`) failures.push(`${id}: missing bottom state`);
  if (state?.variants?.["type=top"]?.model !== `csrp:block/${id}_top`) failures.push(`${id}: missing top state`);
  if (state?.variants?.["type=double"]?.model !== `csrp:block/${base}`) failures.push(`${id}: incorrect double state`);
  for (const suffix of ["", "_top"]) json(`src/main/resources/assets/csrp/models/block/${id}${suffix}.json`);
  const loot = json(`src/main/resources/data/csrp/loot_tables/blocks/${id}.json`);
  const countFunction = loot?.pools?.[0]?.entries?.[0]?.functions?.find((entry) => entry.function === "minecraft:set_count");
  if (countFunction?.count !== 2 || countFunction?.conditions?.[0]?.properties?.type !== "double") {
    failures.push(`${id}: double slab must drop two items`);
  }
}
for (const [id] of stairs) {
  const state = json(`src/main/resources/assets/csrp/blockstates/${id}.json`);
  if (Object.keys(state?.variants ?? {}).length !== 40) failures.push(`${id}: expected 40 stair variants`);
  for (const suffix of ["", "_inner", "_outer"]) json(`src/main/resources/assets/csrp/models/block/${id}${suffix}.json`);
}
for (const [id] of walls) {
  const state = json(`src/main/resources/assets/csrp/blockstates/${id}.json`);
  if (state?.multipart?.length !== 9) failures.push(`${id}: expected 9 wall multipart entries`);
  for (const direction of ["north", "east", "south", "west"]) {
    for (const height of ["low", "tall"]) {
      if (!state?.multipart?.some((part) => part.when?.[direction] === height)) {
        failures.push(`${id}: missing ${direction}=${height} wall state`);
      }
    }
  }
  for (const suffix of ["_post", "_side", "_side_tall", "_inventory"]) {
    json(`src/main/resources/assets/csrp/models/block/${id}${suffix}.json`);
  }
}

const recipes = [
  ...slabs.map(([id, input]) => [
    id === "infested_stone_slab" ? "infested_stone_slab_from_infested_rubble"
      : id === "infested_dirt_slab" ? "infested_dirt_slab_from_infested_stain"
        : id === "infested_stone_brick_slab" ? "infested_stone_brick_slab_from_infested_stone_bricks"
          : id === "polished_infested_stone_slab" ? "polished_infested_stone_slab_from_infested_stone_polished"
            : id === "infested_sandstone_slab" ? "infested_sandstone_slab_from_inf_ss"
              : `${id}_from_${input}`,
    input, id, 6
  ]),
  ...stairs.map(([id, input]) => [id === "residue_stairs" ? "residue_stairs_from_residue_bricks" : id, input, id, 4]),
  ...walls.map(([id, input]) => [id === "residue_wall" ? "residue_wall_from_residue_bricks" : id, input, id, 6]),
  ["residue_stairs_from_residue_bricks_mirrored", "residue_bricks", "residue_stairs", 4]
];
for (const [name, input, output, count] of recipes) {
  const recipe = json(`src/main/resources/data/csrp/recipes/${name}.json`);
  if (recipe?.key?.["#"]?.item !== `csrp:${input}`) failures.push(`${name}: incorrect input`);
  if (recipe?.result?.item !== `csrp:${output}` || recipe?.result?.count !== count) {
    failures.push(`${name}: incorrect output`);
  }
}

for (const [kind, group] of [["slabs", slabs], ["stairs", stairs], ["walls", walls]]) {
  for (const registry of ["block", "item"]) {
    const tag = json(`src/main/resources/data/minecraft/tags/${registry}/${kind}.json`);
    for (const [id] of group) {
      if (!tag?.values?.includes(`csrp:${id}`)) failures.push(`${id}: missing ${registry} ${kind} tag`);
    }
  }
}

const wallClass = read("src/main/java/alku/csrp/block/InfestedWallBlock.java");
for (const expected of ["scheduleCheck(level, pos, 10)", "touchesInfestation(level, pos)",
  "BlockInfestation.infestAround(level, pos, 1)", "level.scheduleTick(pos, this, 20)"]) {
  if (!wallClass.includes(expected)) failures.push(`InfestedWallBlock missing behavior: ${expected}`);
}
const stairClass = read("src/main/java/alku/csrp/block/InfestedStairBlock.java");
if (!stairClass.includes("BlockInfestation.spread(level, pos, 0, random)")) {
  failures.push("InfestedStairBlock missing legacy stage-zero spread");
}

const en = json("src/main/resources/assets/csrp/lang/en_us.json");
const zh = json("src/main/resources/assets/csrp/lang/zh_cn.json");
for (const id of ids) {
  if (!en?.[`block.csrp.${id}`]) failures.push(`${id}: missing English translation`);
  if (!zh?.[`block.csrp.${id}`]) failures.push(`${id}: missing Chinese translation`);
}

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log("Infested shape blocks port verification passed.");
