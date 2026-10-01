const fs = require("node:fs");
const path = require("node:path");

// Pins the two "parasite remains" regressions seen in game:
//   1. thin legacy remains registered as occluding full cubes -> neighbours cull against them
//      and the remains render see-through;
//   2. legacy 1.12-era model parents (block/door_bottom, block/glass_pane_post_ends, ...) that
//      do not exist in 1.21.1 -> "Failed to load model".
// Usage: node scripts/verify-remain-block-rendering.cjs

const root = path.resolve(__dirname, "..");
const failures = [];

function read(relative) {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
}

function expect(source, pattern, message) {
  if (!pattern.test(source)) failures.push(message);
}

const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");

// --- 1. non-cube legacy blocks must opt out of occlusion ---
expect(blocks, /LEGACY_THIN_BLOCKS/, "the non-occluding legacy block set is missing");
for (const id of ["goreada", "gorefer", "goremar", "gorepri", "gorepur", "goresim",
                  "infestedbush", "parasitebush", "lipoma_mass", "hirsute_hair",
                  "parasitesapling", "parasitecanister", "infested_pot", "consumed_pot"]) {
  expect(blocks, new RegExp(`"${id}"`), `${id} is not marked as a non-occluding legacy block`);
}
expect(blocks, /legacyProperties\(String id\)/, "legacyProperties(id) helper is missing");
expect(blocks, /LEGACY_THIN_BLOCKS\.contains\(id\) \? properties\.noOcclusion\(\) : properties/,
  "thin legacy blocks must be built with noOcclusion()");
expect(blocks, /new Block\(legacyProperties\(id\)\)/,
  "legacy block construction must go through legacyProperties(id)");
expect(blocks, /new Block\(legacyProperties\(id\)\) \{\s*@Override\s*protected void createBlockStateDefinition/,
  "legacy state blocks must go through legacyProperties(id)");

// --- 2. gore must not leave an invisible full-cube collision/support shape ---
const gore = read("src/main/java/alku/csrp/block/GoreBlock.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
expect(blocks, /GORE_BLOCK_BY_TIER\.containsValue\(id\)[\s\S]*?new GoreBlock\(legacyProperties\(id\)\)[\s\S]*?builder\.add\(GORE_VARIANT\)/,
  "all six gore families must use GoreBlock and preserve their variant property");
expect(gore, /super\(properties\.noCollission\(\)\.noOcclusion\(\)\)/,
  "gore must not have invisible collision or occlude neighbouring blocks");
expect(gore, /Block\.box\(1\.6D, 0\.0D, 1\.6D, 14\.4D, 12\.8D, 14\.4D\)/,
  "gore selection bounds must match original BlockGore, not a full cube");
expect(gore, /getShape\([\s\S]*?return SHAPE;/,
  "gore must actually use its non-full selection shape");
expect(gore, /onPlace\([\s\S]*?RemainsMigration\.convert\(serverLevel, pos, state\)/,
  "legacy gore placement must immediately convert to an entity");
expect(client, /ModEntities\.REMAIN\.get\(\), RemainRenderer::new/,
  "remains must have their own visible entity renderer");
for (const family of ["sim", "pri", "ada", "pure", "fer", "mar"]) {
  for (const variant of ["flat", "small", "big"]) {
    const relative = `src/main/resources/assets/csrp/models/block/gore_${family}_${variant}.json`;
    const model = JSON.parse(read(relative));
    if (!["cutout", "minecraft:cutout"].includes(model.render_type)) {
      failures.push(`${relative}: gore model must discard transparent pixels`);
    }
    if (model.parent !== "csrp:block/gore_base") {
      failures.push(`${relative}: gore model must retain its original geometry`);
    }
  }
}

// --- 3. no model may reference a parent that does not exist in 1.21.1 ---
const modelRoot = path.join(root, "src/main/resources/assets/csrp/models");
const vanillaRoot = path.join(root, ".vanilla-assets/assets/minecraft/models");
const deadParents = [
  "block/door_bottom", "block/door_top", "block/door_bottom_rh", "block/door_top_rh",
  "minecraft:block/door_bottom", "minecraft:block/door_top",
  "minecraft:block/door_bottom_rh", "minecraft:block/door_top_rh",
  "block/glass_pane_post_ends",
];

function walk(dir) {
  const out = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...walk(full));
    else if (entry.name.endsWith(".json")) out.push(full);
  }
  return out;
}

for (const file of walk(modelRoot)) {
  let model;
  try {
    model = JSON.parse(fs.readFileSync(file, "utf8"));
  } catch (error) {
    failures.push(`${path.relative(root, file)}: invalid JSON (${error.message})`);
    continue;
  }
  const parent = model.parent;
  if (!parent) continue;
  const relative = path.relative(root, file);
  if (deadParents.includes(parent)) {
    failures.push(`${relative}: parent '${parent}' does not exist in 1.21.1`);
    continue;
  }
  const [namespace, id] = parent.includes(":") ? parent.split(":", 2) : ["minecraft", parent];
  if (namespace === "minecraft") {
    if (id.startsWith("builtin/")) continue;
    if (!fs.existsSync(path.join(vanillaRoot, `${id}.json`))) {
      failures.push(`${relative}: missing vanilla parent '${parent}'`);
    }
  } else if (namespace === "csrp") {
    if (!fs.existsSync(path.join(modelRoot, `${id}.json`))) {
      failures.push(`${relative}: missing csrp parent '${parent}'`);
    }
  }
}

// --- 3. the remains blocks must have readable names, not raw translation keys ---
for (const language of ["zh_cn.json", "en_us.json"]) {
  const lang = read(`src/main/resources/assets/csrp/lang/${language}`);
  let parsed;
  try {
    parsed = JSON.parse(lang);
  } catch (error) {
    failures.push(`${language}: invalid JSON (${error.message})`);
    continue;
  }
  for (const id of ["goresim_flat", "gorepri_flat", "goreada_flat",
                    "gorepur_flat", "gorefer_flat", "goremar_flat"]) {
    if (!parsed[`block.csrp.${id}`]) {
      failures.push(`${language}: block.csrp.${id} has no localized name`);
    }
  }
}

if (failures.length) {
  console.error("Remain block rendering verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Remain block rendering verification passed (non-occluding remains, valid parents).\n");
