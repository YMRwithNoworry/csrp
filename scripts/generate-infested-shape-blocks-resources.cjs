const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const writeJson = (relative, value) => {
  const file = path.join(root, relative);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);
};
const readJson = (relative) => JSON.parse(fs.readFileSync(path.join(root, relative), "utf8"));
const addTagValues = (relative, ids) => {
  const file = path.join(root, relative);
  const tag = fs.existsSync(file) ? readJson(relative) : { replace: false, values: [] };
  tag.values = [...new Set([...tag.values, ...ids.map((id) => `csrp:${id}`)])];
  writeJson(relative, tag);
};

const slabs = [
  ["infested_cobblestone_slab", "infested_cobblestone", "infested_cobblestone"],
  ["infested_stone_slab", "infestedrubble", "infestedrubble"],
  ["infested_dirt_slab", "infestedstain", "infestedstain"],
  ["infested_stone_brick_slab", "infested_stone_bricks", "infested_stone_bricks"],
  ["infested_terracotta_slab", "infested_terracotta", "infested_terracotta"],
  ["polished_infested_stone_slab", "infested_stone_polished", "infested_stone_polished"],
  ["residue_brick_slab", "residue_bricks", "residue_bricks"],
  ["infested_sandstone_slab", "inf_ss", "inf_ss"],
  ["infested_plank_slab", "infested_planks", "infested_planks"]
];
const stairs = [
  ["infested_sandstone_stairs", "inf_ss", "inf_ss"],
  ["residue_stairs", "residue_bricks", "residue_bricks"],
  ["infested_planks_stairs", "infested_planks", "infested_planks"],
  ["infested_stone_bricks_stairs", "infested_stone_bricks", "infested_stone_bricks"],
  ["infested_polished_stone_bricks_stairs", "infested_stone_polished", "infested_stone_polished"],
  ["infested_stone_stairs", "infestedrubble", "infestedrubble"]
];
const walls = [
  ["residue_wall", "residue_bricks", "residue_bricks"],
  ["infested_plank_wall", "infested_planks", "infested_planks"],
  ["polished_infested_stone_wall", "infested_stone_polished", "infested_stone_polished"],
  ["infested_stone_brick_wall", "infested_stone_bricks", "infested_stone_bricks"],
  ["infested_sandstone_wall", "inf_ss", "inf_ss"],
  ["infestedrubble_wall", "infestedrubble", "infestedrubble"],
  ["infestedstain_wall", "infestedstain", "infestedstain"]
];

const textureSet = (texture) => texture === "inf_ss"
  ? { bottom: "csrp:block/inf_ss_down", top: "csrp:block/inf_ss_top", side: "csrp:block/inf_ss" }
  : { bottom: `csrp:block/${texture}`, top: `csrp:block/${texture}`, side: `csrp:block/${texture}` };
const blockModel = (parent, textures) => ({ parent: `minecraft:block/${parent}`, textures });
const selfDrop = (id) => ({
  type: "minecraft:block",
  pools: [{
    bonus_rolls: 0,
    conditions: [{ condition: "minecraft:survives_explosion" }],
    entries: [{ type: "minecraft:item", name: `csrp:${id}` }],
    rolls: 1
  }]
});
const slabDrop = (id) => ({
  type: "minecraft:block",
  pools: [{
    bonus_rolls: 0,
    entries: [{
      type: "minecraft:item",
      functions: [{
        add: false,
        conditions: [{
          block: `csrp:${id}`,
          condition: "minecraft:block_state_property",
          properties: { type: "double" }
        }],
        count: 2,
        function: "minecraft:set_count"
      }, { function: "minecraft:explosion_decay" }],
      name: `csrp:${id}`
    }],
    rolls: 1
  }],
  random_sequence: `csrp:blocks/${id}`
});

for (const [id, baseModel, texture] of slabs) {
  const textures = textureSet(texture);
  writeJson(`src/main/resources/assets/csrp/blockstates/${id}.json`, {
    variants: {
      "type=bottom": { model: `csrp:block/${id}` },
      "type=double": { model: `csrp:block/${baseModel}` },
      "type=top": { model: `csrp:block/${id}_top` }
    }
  });
  writeJson(`src/main/resources/assets/csrp/models/block/${id}.json`, blockModel("slab", textures));
  writeJson(`src/main/resources/assets/csrp/models/block/${id}_top.json`, blockModel("slab_top", textures));
  writeJson(`src/main/resources/assets/csrp/models/item/${id}.json`, { parent: `csrp:block/${id}` });
  writeJson(`src/main/resources/data/csrp/loot_table/blocks/${id}.json`, slabDrop(id));
}

const rotations = {
  east: { bottom: [270, 0], top: [0, 90] },
  north: { bottom: [180, 270], top: [270, 0] },
  south: { bottom: [0, 90], top: [90, 180] },
  west: { bottom: [90, 180], top: [180, 270] }
};
const straightRotation = { east: 0, north: 270, south: 90, west: 180 };
const stairVariants = (id) => {
  const variants = {};
  for (const facing of Object.keys(rotations)) {
    for (const half of ["bottom", "top"]) {
      for (const shape of ["inner_left", "inner_right", "outer_left", "outer_right", "straight"]) {
        const modelSuffix = shape.startsWith("inner") ? "_inner" : shape.startsWith("outer") ? "_outer" : "";
        const side = shape.endsWith("right") ? 1 : 0;
        const y = shape === "straight" ? straightRotation[facing] : rotations[facing][half][side];
        const apply = { model: `csrp:block/${id}${modelSuffix}` };
        if (half === "top") apply.x = 180;
        if (y !== 0) apply.y = y;
        if (half === "top" || y !== 0) apply.uvlock = true;
        variants[`facing=${facing},half=${half},shape=${shape}`] = apply;
      }
    }
  }
  return { variants };
};

for (const [id, , texture] of stairs) {
  const textures = textureSet(texture);
  writeJson(`src/main/resources/assets/csrp/blockstates/${id}.json`, stairVariants(id));
  writeJson(`src/main/resources/assets/csrp/models/block/${id}.json`, blockModel("stairs", textures));
  writeJson(`src/main/resources/assets/csrp/models/block/${id}_inner.json`, blockModel("inner_stairs", textures));
  writeJson(`src/main/resources/assets/csrp/models/block/${id}_outer.json`, blockModel("outer_stairs", textures));
  writeJson(`src/main/resources/assets/csrp/models/item/${id}.json`, { parent: `csrp:block/${id}` });
  writeJson(`src/main/resources/data/csrp/loot_table/blocks/${id}.json`, selfDrop(id));
}

const wallState = (id) => {
  const multipart = [{ apply: { model: `csrp:block/${id}_post` }, when: { up: "true" } }];
  const directions = [["north", 0], ["east", 90], ["south", 180], ["west", 270]];
  for (const [direction, y] of directions) {
    const apply = { model: `csrp:block/${id}_side`, uvlock: true };
    if (y) apply.y = y;
    multipart.push({ apply, when: { [direction]: "low" } });
  }
  for (const [direction, y] of directions) {
    const apply = { model: `csrp:block/${id}_side_tall`, uvlock: true };
    if (y) apply.y = y;
    multipart.push({ apply, when: { [direction]: "tall" } });
  }
  return { multipart };
};

for (const [id, , texture] of walls) {
  const wallTexture = { wall: `csrp:block/${texture}` };
  writeJson(`src/main/resources/assets/csrp/blockstates/${id}.json`, wallState(id));
  writeJson(`src/main/resources/assets/csrp/models/block/${id}_post.json`, blockModel("template_wall_post", wallTexture));
  writeJson(`src/main/resources/assets/csrp/models/block/${id}_side.json`, blockModel("template_wall_side", wallTexture));
  writeJson(`src/main/resources/assets/csrp/models/block/${id}_side_tall.json`, blockModel("template_wall_side_tall", wallTexture));
  writeJson(`src/main/resources/assets/csrp/models/block/${id}_inventory.json`, blockModel("wall_inventory", wallTexture));
  writeJson(`src/main/resources/assets/csrp/models/item/${id}.json`, { parent: `csrp:block/${id}_inventory` });
  writeJson(`src/main/resources/data/csrp/loot_table/blocks/${id}.json`, selfDrop(id));
}

const shapedRecipe = (pattern, input, output, count) => ({
  type: "minecraft:crafting_shaped",
  category: "building",
  pattern,
  key: { "#": { item: `csrp:${input}` } },
  result: { id: `csrp:${output}`, count }
});
const slabRecipeNames = {
  infested_cobblestone_slab: "infested_cobblestone_slab_from_infested_cobblestone",
  infested_stone_slab: "infested_stone_slab_from_infested_rubble",
  infested_dirt_slab: "infested_dirt_slab_from_infested_stain",
  infested_stone_brick_slab: "infested_stone_brick_slab_from_infested_stone_bricks",
  infested_terracotta_slab: "infested_terracotta_slab_from_infested_terracotta",
  polished_infested_stone_slab: "polished_infested_stone_slab_from_infested_stone_polished",
  residue_brick_slab: "residue_brick_slab_from_residue_bricks",
  infested_sandstone_slab: "infested_sandstone_slab_from_inf_ss",
  infested_plank_slab: "infested_plank_slab_from_infested_planks"
};
for (const [id, input] of slabs) {
  writeJson(`src/main/resources/data/csrp/recipe/${slabRecipeNames[id]}.json`, shapedRecipe(["###"], input, id, 6));
}
for (const [id, input] of stairs) {
  const name = id === "residue_stairs" ? "residue_stairs_from_residue_bricks" : id;
  writeJson(`src/main/resources/data/csrp/recipe/${name}.json`, shapedRecipe(["#  ", "## ", "###"], input, id, 4));
}
writeJson("src/main/resources/data/csrp/recipe/residue_stairs_from_residue_bricks_mirrored.json",
  shapedRecipe(["  #", " ##", "###"], "residue_bricks", "residue_stairs", 4));
for (const [id, input] of walls) {
  const name = id === "residue_wall" ? "residue_wall_from_residue_bricks" : id;
  writeJson(`src/main/resources/data/csrp/recipe/${name}.json`, shapedRecipe(["###", "###"], input, id, 6));
}

const ids = [...slabs, ...stairs, ...walls].map(([id]) => id);
addTagValues("src/main/resources/data/minecraft/tags/block/slabs.json", slabs.map(([id]) => id));
addTagValues("src/main/resources/data/minecraft/tags/item/slabs.json", slabs.map(([id]) => id));
addTagValues("src/main/resources/data/minecraft/tags/block/stairs.json", stairs.map(([id]) => id));
addTagValues("src/main/resources/data/minecraft/tags/item/stairs.json", stairs.map(([id]) => id));
addTagValues("src/main/resources/data/minecraft/tags/block/walls.json", walls.map(([id]) => id));
addTagValues("src/main/resources/data/minecraft/tags/item/walls.json", walls.map(([id]) => id));

const axe = ["infested_plank_slab", "infested_planks_stairs", "residue_wall", "infested_plank_wall"];
const shovel = ["infested_dirt_slab", "infestedstain_wall"];
const pickaxe = ids.filter((id) => !axe.includes(id) && !shovel.includes(id));
const stoneTool = [
  "infested_cobblestone_slab", "infested_stone_slab", "infested_stone_brick_slab",
  "infested_terracotta_slab", "polished_infested_stone_slab", "residue_brick_slab",
  "infested_sandstone_slab"
];
addTagValues("src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json", pickaxe);
addTagValues("src/main/resources/data/minecraft/tags/block/mineable/axe.json", axe);
addTagValues("src/main/resources/data/minecraft/tags/block/mineable/shovel.json", shovel);
addTagValues("src/main/resources/data/minecraft/tags/block/needs_stone_tool.json", stoneTool);

const enNames = {
  infested_cobblestone_slab: "Infested Cobblestone Slab",
  infested_stone_slab: "Infested Stone Slab",
  infested_dirt_slab: "Infested Dirt Slab",
  infested_stone_brick_slab: "Infested Stone Brick Slab",
  infested_terracotta_slab: "Infested Terracotta Slab",
  polished_infested_stone_slab: "Polished Infested Stone Slab",
  residue_brick_slab: "Residue Brick Slab",
  infested_sandstone_slab: "Infested Sandstone Slab",
  infested_plank_slab: "Infested Plank Slab",
  infested_sandstone_stairs: "Infested Sandstone Stairs",
  residue_stairs: "Residue Brick Stairs",
  infested_planks_stairs: "Infested Planks Stairs",
  infested_stone_bricks_stairs: "Infested Stone Bricks Stairs",
  infested_polished_stone_bricks_stairs: "Infested Polished Stone Bricks Stairs",
  infested_stone_stairs: "Infested Stone Stairs",
  residue_wall: "Residue Wall",
  infested_plank_wall: "Infested Plank Wall",
  polished_infested_stone_wall: "Polished Infested Stone Wall",
  infested_stone_brick_wall: "Infested Stone Brick Wall",
  infested_sandstone_wall: "Infested Sandstone Wall",
  infestedrubble_wall: "Infested Stone Wall",
  infestedstain_wall: "Infested Dirt Wall"
};
const zhNames = {
  infested_cobblestone_slab: "寄染圆石台阶",
  infested_stone_slab: "寄染石头台阶",
  infested_dirt_slab: "寄染泥土台阶",
  infested_stone_brick_slab: "寄染石砖台阶",
  infested_terracotta_slab: "寄染陶瓦台阶",
  polished_infested_stone_slab: "磨制寄染石头台阶",
  residue_brick_slab: "残渣砖台阶",
  infested_sandstone_slab: "寄染砂岩台阶",
  infested_plank_slab: "寄染木台阶",
  infested_sandstone_stairs: "寄染砂岩楼梯",
  residue_stairs: "残渣砖台阶",
  infested_planks_stairs: "寄染木楼梯",
  infested_stone_bricks_stairs: "寄染石砖楼梯",
  infested_polished_stone_bricks_stairs: "磨制寄染石砖楼梯",
  infested_stone_stairs: "寄染石头楼梯",
  residue_wall: "残渣砖墙",
  infested_plank_wall: "寄染木墙",
  polished_infested_stone_wall: "磨制寄染石头墙",
  infested_stone_brick_wall: "寄染石砖墙",
  infested_sandstone_wall: "寄染砂岩墙",
  infestedrubble_wall: "寄染石墙",
  infestedstain_wall: "寄染泥土墙"
};
for (const [lang, names] of [["en_us", enNames], ["zh_cn", zhNames]]) {
  const relative = `src/main/resources/assets/csrp/lang/${lang}.json`;
  const translations = readJson(relative);
  for (const [id, name] of Object.entries(names)) translations[`block.csrp.${id}`] = name;
  writeJson(relative, translations);
}

console.log(`Generated resources for ${ids.length} infested shape blocks.`);
