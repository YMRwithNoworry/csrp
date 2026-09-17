/**
 * Fills the two gaps left by the placeholder block registry:
 *
 * 1. Translation keys. 104 of the ids that `ModBlocks.registerLegacyBlocks()` registers as
 *    approximations have no `block.csrp.<id>` entry, so they render as raw keys in-game. Names are
 *    recovered from, in order: the 26.3 donor branch, the decompiled 1.10.8 original's
 *    `tile.srparasites.<id>.name` (or one of its variants), a sibling id, or a derived label.
 * 2. Loot tables. A registered block without a loot table drops nothing. The original created an
 *    `ItemBlock` for these blocks, so each one must drop itself.
 *
 *   node scripts/complete-legacy-block-content.cjs [--dry]
 */
const fs = require("fs");
const path = require("path");

const DRY = process.argv.includes("--dry");
const ROOT = path.resolve(__dirname, "..");
const DONOR = path.resolve(ROOT, "..", "csrp-26.3");
// The decompiler output keeps code only, so the original's lang files live in docs/original-lang/.
const ORIGINAL_LANG = path.join(ROOT, "docs", "original-lang");

const BLOCKS = path.join(ROOT, "src/main/java/alku/csrp/registry/ModBlocks.java");
const LANG_DIR = path.join(ROOT, "src/main/resources/assets/csrp/lang");
const LOOT_DIR = path.join(ROOT, "src/main/resources/data/csrp/loot_tables/blocks");

const read = (file) => fs.readFileSync(file, "utf8");
const readJson = (file) => JSON.parse(read(file));
const exists = (file) => fs.existsSync(file);

const parseLegacyLang = (file) => {
  const entries = {};
  if (!exists(file)) {
    return entries;
  }
  for (const line of read(file).split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#") || !trimmed.includes("=")) {
      continue;
    }
    const index = trimmed.indexOf("=");
    entries[trimmed.slice(0, index).trim()] = trimmed.slice(index + 1);
  }
  return entries;
};

const originalEnglish = parseLegacyLang(path.join(ORIGINAL_LANG, "en_us.lang"));
const originalChinese = parseLegacyLang(path.join(ORIGINAL_LANG, "zh_cn.lang"));
const donorEnglish = readJson(path.join(DONOR, "src/main/resources/assets/csrp/lang/en_us.json"));
const donorChinese = readJson(path.join(DONOR, "src/main/resources/assets/csrp/lang/zh_cn.json"));

const blocksSource = read(BLOCKS);
const legacySection = blocksSource.slice(blocksSource.indexOf("registerLegacyBlocks()"));
const legacyIds = [...legacySection.slice(0, legacySection.indexOf("};")).matchAll(/"([a-z0-9_]+)"/g)]
  .map((match) => match[1])
  .filter((id, index, all) => all.indexOf(id) === index)
  .sort();

const titleCase = (id) => id.split("_").map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(" ");

// Names that no source string covers; derived from the closest sibling that does.
const derivedEnglish = {
  cooked_flesh_slab_double: "Cooked Flesh Double Slab",
  flesh_slab_double: "Flesh Double Slab",
  harleskinn_slab_double: "Harleskinn Double Slab",
  infested_plank_slab_double: "Infested Plank Double Slab",
  infestedrubblestairs: "Infested Rubble Stairs",
  infestedstainstairs: "Infested Stain Stairs",
  infestedtrunkstairs: "Infested Trunk Stairs",
  parasitebush: "Parasite Bush",
  parasitecanister: "Parasite Canister",
  parasiteplank: "Parasite Plank",
  parasiterubble: "Parasite Rubble",
  parasiterubbleslabdouble: "Parasite Rubble Double Slab",
  parasiterubbleslabhalf: "Parasite Rubble Slab",
  parasitesapling: "Parasite Sapling",
  parasitestain: "Parasite Stain",
  parasitestainslabhalf: "Parasite Stain Slab",
  relay_controller_dummy: "Relay Controller"
};
const derivedChinese = {
  cooked_flesh_slab_double: "熟肉台阶（双层）",
  flesh_slab_double: "肉块台阶（双层）",
  harleskinn_slab_double: "哈勒皮台阶（双层）",
  infested_plank_slab_double: "感染木板台阶（双层）",
  infestedrubblestairs: "感染残骸楼梯",
  infestedstainstairs: "感染污渍楼梯",
  infestedtrunkstairs: "感染树干楼梯",
  parasitebush: "寄生灌木",
  parasitecanister: "寄生罐",
  parasiteplank: "寄生木板",
  parasiterubble: "寄生残骸",
  parasiterubbleslabdouble: "寄生残骸台阶（双层）",
  parasiterubbleslabhalf: "寄生残骸台阶",
  parasitesapling: "寄生树苗",
  parasitestain: "寄生污渍",
  parasitestainslabhalf: "寄生污渍台阶",
  relay_controller_dummy: "中继控制器"
};

/** Resolves a display name from the donor branch, the original lang files, a variant key, or a derivation. */
const resolve = (id, kind, language, donor, original) => {
  const derived = language === "en" ? derivedEnglish[id] : derivedChinese[id];
  const donorKey = `block.csrp.${id}`;
  if (donor[donorKey]) {
    return { value: donor[donorKey], source: "donor" };
  }
  const prefix = kind === "tile" ? "tile.srparasites." : "item.srparasites.";
  const variants = [".name", "_big.name", "_flat.name", "_small.name", "s.name", "half.name",
    "double.name", "_top.name"];
  for (const suffix of variants) {
    if (language === "en" && original[`${prefix}${id}${suffix}`]) {
      return { value: original[`${prefix}${id}${suffix}`], source: "original" };
    }
    if (language === "zh" && original[`${prefix}${id}${suffix}`]) {
      return { value: original[`${prefix}${id}${suffix}`], source: "original" };
    }
  }
  if (derived) {
    return { value: derived, source: "derived" };
  }
  return { value: titleCase(id), source: "fallback" };
};

const sources = { donor: 0, original: 0, derived: 0, fallback: 0 };
const report = [];
for (const [language, file, donor, original, derived] of [
  ["en", "en_us.json", donorEnglish, originalEnglish, derivedEnglish],
  ["zh", "zh_cn.json", donorChinese, originalChinese, derivedChinese]
]) {
  const full = path.join(LANG_DIR, file);
  const lang = readJson(full);
  const added = [];
  for (const id of legacyIds) {
    const key = `block.csrp.${id}`;
    if (lang[key]) {
      continue;
    }
    if (derived[id]) {
      lang[key] = derived[id];
      sources.derived += 1;
      added.push(`${key} = ${derived[id]}`);
      continue;
    }
    const resolved = resolve(id, "tile", language, donor, original);
    lang[key] = resolved.value;
    sources[resolved.source] += 1;
    if (resolved.source === "fallback") {
      report.push(`${language}: ${key} = ${resolved.value} (fallback)`);
    }
    added.push(`${key} = ${resolved.value}`);
  }
  if (!DRY && added.length) {
    // Preserve the file's existing key order and two-space formatting.
    fs.writeFileSync(full, `${JSON.stringify(lang, null, 2)}\n`, "utf8");
  }
  console.log(`${file}: +${added.length} keys`);
}

console.log(`name sources: donor=${sources.donor} original=${sources.original} `
  + `derived=${sources.derived} fallback=${sources.fallback}`);
if (report.length) {
  console.log(report.join("\n"));
}

// ---------------------------------------------------------------- loot tables

const simpleTable = (id) => ({
  type: "minecraft:block",
  pools: [{
    bonus_rolls: 0,
    conditions: [{ condition: "minecraft:survives_explosion" }],
    entries: [{ type: "minecraft:item", name: `csrp:${id}` }],
    rolls: 1
  }]
});

const slabTable = (id) => ({
  type: "minecraft:block",
  pools: [{
    bonus_rolls: 0,
    entries: [{
      type: "minecraft:item",
      functions: [
        {
          add: false,
          conditions: [{
            block: `csrp:${id}`,
            condition: "minecraft:block_state_property",
            properties: { type: "double" }
          }],
          count: 2,
          function: "minecraft:set_count"
        },
        { function: "minecraft:explosion_decay" }
      ],
      name: `csrp:${id}`
    }],
    rolls: 1
  }],
  random_sequence: `csrp:blocks/${id}`
});

let lootWritten = 0;
for (const id of legacyIds) {
  const target = path.join(LOOT_DIR, `${id}.json`);
  if (exists(target)) {
    continue;
  }
  // `legacyBlock()` builds a SlabBlock for any id containing "slab" and a plain Block otherwise.
  const table = id.includes("slab") ? slabTable(id) : simpleTable(id);
  if (!DRY) {
    fs.writeFileSync(target, `${JSON.stringify(table, null, 2)}\n`, "utf8");
  }
  lootWritten += 1;
}
console.log(`${DRY ? "[dry] would write" : "wrote"} ${lootWritten} legacy loot tables`);
