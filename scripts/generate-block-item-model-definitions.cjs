const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const assets = "src/main/resources/assets/csrp";

// 每个变体方块物品使用稳定的单一库存模型，不依赖 blockstate 的状态排序。
const inventoryModels = {
  parasiterubble_bonestairs: "csrp:block/parasiterubble_bonestairs",
  parasiterubble_bricksstairs: "csrp:block/parasiterubble_bricksstairs",
  parasiterubble_fleshstairs: "csrp:block/parasiterubble_fleshstairs",
  parasiterubble_fungusstairs: "csrp:block/parasiterubble_fungusstairs",
  parasiterubble_metalstairs: "csrp:block/parasiterubble_metalstairs",
  parasiterubble_obsidianstairs: "csrp:block/parasiterubble_obsidianstairs",
  parasiterubble_stonedebrisstairs: "csrp:block/parasiterubble_stonedebrisstairs",
  parasiterubble_stonestairs: "csrp:block/parasiterubble_stonestairs",
  parasiterubble_woodstairs: "csrp:block/parasiterubble_woodstairs",
  parasiterubbledense_biomestairs: "csrp:block/parasiterubbledense_biomestairs",
  parasiterubbledense_colonystairs: "csrp:block/parasiterubbledense_colonystairs",
  parasiterubbledense_wallstairs: "csrp:block/parasiterubbledense_wallstairs",
  parasitetrunk_ballstairs: "csrp:block/parasitetrunk_ballstairs",
  parasitetrunk_plantstairs: "csrp:block/parasitetrunk_plantstairs",
  parasitetrunk_treestairs: "csrp:block/parasitetrunk_treestairs",
  dispatchern: "csrp:block/dodn",
  infestedbush: "csrp:block/infestedbush_infected",
  parasitebush: "csrp:block/parasitebush_tendril",
  parasitecanister: "csrp:block/parasitecanister_sac",
  parasiteplank: "csrp:block/parasiteplank_deadhead",
  parasiterubble: "csrp:block/parasiterubble_flesh",
  parasiterubbleslabhalf: "csrp:block/slabbone_bottom",
  parasitesapling: "csrp:item/parasitesapling_tree",
  parasitestain: "csrp:block/parasitestain_dirt",
  parasitestain_dirtstairs: "csrp:block/parasitestain_dirtstairs",
  parasitestain_feelerstairs: "csrp:block/parasitestain_feelerstairs",
  parasitestain_fleshstairs: "csrp:block/parasitestain_fleshstairs",
  parasitestain_mudstairs: "csrp:block/parasitestain_mudstairs",
  parasitestainslabhalf: "csrp:block/slabdirt_bottom"
};

const existingItemModels = [
  "deadhead_grass_short",
  "deadhead_grass_tall",
  "snow_short_grass",
  "snow_tall_grass"
];
const itemIds = [...Object.keys(inventoryModels), ...existingItemModels];

const writeJsonIfMissingOrIdentical = (relative, value) => {
  const file = path.join(root, relative);
  const content = `${JSON.stringify(value, null, 2)}\n`;
  if (fs.existsSync(file)) {
    if (fs.readFileSync(file, "utf8") !== content) {
      throw new Error(`${relative} already exists with different contents`);
    }
    return;
  }
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content);
};

for (const id of itemIds) {
  writeJsonIfMissingOrIdentical(`${assets}/items/${id}.json`, {
    model: {
      type: "minecraft:model",
      model: `csrp:item/${id}`
    }
  });
}

for (const [id, parent] of Object.entries(inventoryModels)) {
  writeJsonIfMissingOrIdentical(`${assets}/models/item/${id}.json`, { parent });
}

console.log(`Generated ${itemIds.length} block item definitions and ${Object.keys(inventoryModels).length} inventory models.`);
