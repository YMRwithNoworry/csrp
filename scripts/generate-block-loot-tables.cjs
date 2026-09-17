/**
 * Generates the missing self-drop loot tables for registered blocks.
 *
 * In the original 1.12.2 mod every block created its own `ItemBlock` in its constructor, so every
 * obtainable block dropped itself. 1.20.1 replaced that implicit behaviour with loot tables, which
 * means a registered block without a loot table drops nothing. This script fills that gap for every
 * block that has an item form in `ModItems` but no loot table yet.
 *
 *   node scripts/generate-block-loot-tables.cjs [--dry]
 */
const fs = require("fs");
const path = require("path");

const DRY = process.argv.includes("--dry");
const ROOT = path.resolve(__dirname, "..");
const LANG = path.join(ROOT, "src/main/resources/assets/csrp/lang/en_us.json");
const BLOCKS = path.join(ROOT, "src/main/java/alku/csrp/registry/ModBlocks.java");
const ITEMS = path.join(ROOT, "src/main/java/alku/csrp/registry/ModItems.java");
const LOOT_DIR = path.join(ROOT, "src/main/resources/data/csrp/loot_tables/blocks");

const lang = JSON.parse(fs.readFileSync(LANG, "utf8"));
const blockSource = fs.readFileSync(BLOCKS, "utf8");
const itemSource = fs.readFileSync(ITEMS, "utf8");

const blockIds = Object.keys(lang)
  .filter((key) => key.startsWith("block.csrp."))
  .map((key) => key.slice("block.csrp.".length))
  .filter((id) => blockSource.includes(`"${id}"`))
  .sort();

const simpleTable = (id) => ({
  type: "minecraft:block",
  pools: [
    {
      bonus_rolls: 0,
      conditions: [{ condition: "minecraft:survives_explosion" }],
      entries: [{ type: "minecraft:item", name: `csrp:${id}` }],
      rolls: 1
    }
  ]
});

const slabTable = (id) => ({
  type: "minecraft:block",
  pools: [
    {
      bonus_rolls: 0,
      entries: [
        {
          type: "minecraft:item",
          functions: [
            {
              add: false,
              conditions: [
                {
                  block: `csrp:${id}`,
                  condition: "minecraft:block_state_property",
                  properties: { type: "double" }
                }
              ],
              count: 2,
              function: "minecraft:set_count"
            },
            { function: "minecraft:explosion_decay" }
          ],
          name: `csrp:${id}`
        }
      ],
      rolls: 1
    }
  ],
  random_sequence: `csrp:blocks/${id}`
});

let written = 0;
const skippedNoItem = [];
const alreadyPresent = [];
for (const id of blockIds) {
  const target = path.join(LOOT_DIR, `${id}.json`);
  if (fs.existsSync(target)) {
    alreadyPresent.push(id);
    continue;
  }
  if (!itemSource.includes(`"${id}"`)) {
    // No item form: the original left such blocks without an ItemBlock, so they must not drop one.
    skippedNoItem.push(id);
    continue;
  }
  const table = id.includes("slab") && !id.endsWith("slab_double") && !id.endsWith("slabdouble")
    ? slabTable(id)
    : simpleTable(id);
  if (!DRY) {
    fs.writeFileSync(target, `${JSON.stringify(table, null, 2)}\n`, "utf8");
  }
  written += 1;
}

console.log(`${DRY ? "[dry] would write" : "wrote"} ${written} loot tables`);
console.log(`already present: ${alreadyPresent.length}`);
console.log(`blocks without an item form (left without a loot table): ${skippedNoItem.length}`);
if (skippedNoItem.length) {
  console.log(`  ${skippedNoItem.join(" ")}`);
}
