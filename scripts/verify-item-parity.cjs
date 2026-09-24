const fs = require("fs");
const path = require("path");

/**
 * R5 item parity contract.
 *
 * Fact source: SRParasites 1.10.9 (1.12.2) `com/dhanantry/scapeandrunparasites/init/SRPItems.java`
 * and `assets/srparasites/lang/en_us.lang`.  ORIGINAL_ITEM_IDS below is the complete set of
 * registry names the original `init()` handed to an Item constructor (115 ids); the 119 legacy
 * `ItemMobSpawner` names live in verify-item-legacy-spawner-mapping.cjs.
 */

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const exists = (relative) => fs.existsSync(path.join(root, relative));

const ORIGINAL_ITEM_IDS = [
  "ada_arachnida_drop", "ada_bolster_drop", "ada_devourer_drop", "ada_longarms_drop",
  "ada_manducater_drop", "ada_reeker_drop", "ada_summoner_drop", "ada_vermin_drop",
  "ada_viscera_drop", "ada_yelloweye_drop", "adapted_icon", "alveolar_fluid",
  "alveoligrowth", "assimilated_flesh", "axe", "axe_sentient",
  "beckon_drop", "bloody_bone", "bloody_iron_ingot", "bloody_rod",
  "bone", "boots", "boots_sentient", "bough",
  "bow", "bow_sentient", "chest", "chest_sentient",
  "cleaver", "cleaver_sentient", "colonycompass", "cosmic_structural_failure_icon",
  "crude_icon", "dark_days_icon", "deadblood_fluid", "discthree",
  "dislodgement_report", "dispatcher_drop", "dried_tendons", "ecstasy_icon",
  "enemy_of_enemy_icon", "evclock", "false_apple", "fishlin",
  "fog_nullifier_icon", "greek_fire", "guerilla_icon", "hardened_bone_handle",
  "hellfire_chemical_warfare_icon", "helm", "helm_sentient", "hijacked_drop",
  "hijacked_iron_axe", "hijacked_iron_boots", "hijacked_iron_chestpiece", "hijacked_iron_helmet",
  "hijacked_iron_hoe", "hijacked_iron_leggings", "hijacked_iron_pickaxe", "hijacked_iron_shovel",
  "hijacked_iron_sword", "hive_scrap", "hunt_season_icon", "infectious_blade_fragment",
  "infested_bonemeal", "itemassimilate", "itemdevolve", "itemevolve",
  "itemtab", "itemthrow", "itemvariant", "lance",
  "lance_sentient", "levelclock", "living_core", "lurecomponent1",
  "lurecomponent10", "lurecomponent2", "lurecomponent3", "lurecomponent4",
  "lurecomponent5", "lurecomponent6", "lurecomponent7", "lurecomponent8",
  "lurecomponent9", "maul", "maul_sentient", "mobility_armor_boots",
  "mobility_armor_chestpiece", "mobility_armor_helmet", "mobility_armor_leggings", "module_base",
  "nodecompass", "organ_synth", "origincompass", "pants",
  "pants_sentient", "phase_report", "potion_columbus_icon", "potion_stolas_icon",
  "primitive_icon", "pure_icon", "roots_icon", "scythe",
  "scythe_sentient", "self_destruct_icon", "semiorganic_ingot", "shrimp",
  "sword", "sword_sentient", "the_sign_charm", "tissue_spike",
  "vector_map", "venkrol_boots", "vile_shell"
];

/** Ids that only exist in the original language file / config, never as a 1.10.9 Item registry entry. */
const LANG_ONLY_UPSTREAM = [
  "discone", "disctwo", "relay_report", "scan_report", "vector_report",
  "bow_core", "bow_grip", "bow_lowerlimb", "bow_string", "bow_upperlimb",
  "scythe_back", "scythe_blade", "scythe_core", "scythe_handle", "scythe_head"
];

/** Spawn-egg ids added under the project's `<name>_spawn_egg` convention (original lang: itemmobspawner_*). */
const ADDED_SPAWN_EGGS = [
  ["flam_spawn_egg", "ModEntities.SUCCOR"],
  ["soo_spawn_egg", "ModEntities.SEEKER"],
  ["tenn_spawn_egg", "ModEntities.ARCHITECT"]
];

/** Ids the original mod references but never registers; registering them would change behaviour. */
const INTENTIONALLY_ABSENT = ["ada_burrower_drop"];

const modItems = read("src/main/java/alku/csrp/registry/ModItems.java");
const legacySpawner = read("src/main/java/alku/csrp/item/LegacyMobSpawnerItem.java");
const lang = JSON.parse(read("src/main/resources/assets/csrp/lang/_pending/items.json"));
const entitySource = read("src/main/java/alku/csrp/registry/ModEntities.java");

// ---------------------------------------------------------------- 1. registration machine diff
for (const id of ORIGINAL_ITEM_IDS) {
  if (modItems.includes(`"${id}"`)) continue;
  // lurecomponent1..10 are produced by the "lurecomponent" + version helper.
  if (/^lurecomponent\d+$/.test(id) && modItems.includes('"lurecomponent" + version')) continue;
  failures.push(`original item id is not registered in ModItems.java: ${id}`);
}

for (const id of LANG_ONLY_UPSTREAM) {
  if (!modItems.includes(`"${id}"`)) {
    failures.push(`language-file-only upstream id is not registered: ${id}`);
  }
}

for (const id of INTENTIONALLY_ABSENT) {
  if (modItems.includes(`"${id}"`)) {
    failures.push(`${id} must stay unregistered: SRP 1.10.9 never registers it`);
  }
}

// ---------------------------------------------------------------- 2. per-item behaviour
const expectations = [
  // 1.10.9 ItemBase("itemtab", 1, (byte)7) / ItemAdvancementIcon("self_destruct_icon")
  [`simple("itemtab", new Item.Properties().stacksTo(1))`, "itemtab must be a single-stack item"],
  [`simple("self_destruct_icon", new Item.Properties().stacksTo(1))`, "self_destruct_icon must stack to 1"],
  // ItemDiscRecord + JukeboxSong (1.12.2 ItemRecord equivalent)
  ['"discone", Item::new', "discone must be registered as a record item"],
  ['"disctwo", Item::new', "disctwo must be registered as a record item"],
  ["jukeboxPlayable(DISC_ONE_KEY)", "discone must declare its jukebox song"],
  ["jukeboxPlayable(DISC_TWO_KEY)", "disctwo must declare its jukebox song"],
  ['JUKEBOX_SONGS.register("discone"', "discone jukebox song must be registered"],
  ['JUKEBOX_SONGS.register("disctwo"', "disctwo jukebox song must be registered"],
  ['discSound("srparasites.discone")', "discone jukebox song must reuse the upstream sound event"],
  ['discSound("srparasites.disctwo")', "disctwo jukebox song must reuse the upstream sound event"],
  // reports
  ['"relay_report"', "relay_report must be registered"],
  ['"scan_report"', "scan_report must be registered"],
  ['"vector_report"', "vector_report must be registered"],
  ["new RelayReportItem(RelayReportItem.Type.VECTOR, properties)", "vector_report must open the vector report"],
  // living weapon parts
  ['simple("bow_core")', "bow_core must be registered"],
  ['simple("bow_grip")', "bow_grip must be registered"],
  ['simple("bow_lowerlimb")', "bow_lowerlimb must be registered"],
  ['simple("bow_string")', "bow_string must be registered"],
  ['simple("bow_upperlimb")', "bow_upperlimb must be registered"],
  ['simple("scythe_back")', "scythe_back must be registered"],
  ['simple("scythe_blade")', "scythe_blade must be registered"],
  ['simple("scythe_core")', "scythe_core must be registered"],
  ['simple("scythe_handle")', "scythe_handle must be registered"],
  ['simple("scythe_head")', "scythe_head must be registered"]
];

for (const [needle, message] of expectations) {
  if (!modItems.includes(needle)) failures.push(`${message} (missing: ${needle})`);
}

for (const [id, entity] of ADDED_SPAWN_EGGS) {
  if (!modItems.includes(`"${id}", ${entity}`)) {
    failures.push(`${id} must be a spawn egg for ${entity}`);
  }
  if (!(`item.csrp.${id}` in lang)) {
    failures.push(`${id} is missing its pending language entry`);
  }
}

if (modItems.includes("new SpawnEggItem(")) {
  failures.push("spawn eggs must use TexturedSpawnEggItem, not vanilla SpawnEggItem");
}

// ---------------------------------------------------------------- 3. pending language keys
const expectedLangKeys = [
  ...LANG_ONLY_UPSTREAM.map((id) => `item.csrp.${id}`),
  "item.csrp.itemtab", "item.csrp.self_destruct_icon",
  "item.csrp.flam_spawn_egg", "item.csrp.soo_spawn_egg", "item.csrp.tenn_spawn_egg",
  "jukebox_song.csrp.discone", "jukebox_song.csrp.disctwo"
];
for (const key of expectedLangKeys) {
  if (!(key in lang)) failures.push(`lang/_pending/items.json is missing ${key}`);
}
if (lang["item.csrp.itemtab"] !== "§dNULL") {
  failures.push("item.csrp.itemtab must keep the upstream value §dNULL");
}
for (const key of Object.keys(lang)) {
  if (!key.startsWith("item.csrp.") && !key.startsWith("jukebox_song.csrp.")) {
    failures.push(`unexpected pending language key namespace: ${key}`);
  }
}

// ---------------------------------------------------------------- 4. assets: definition + model + texture
const assetIds = [
  ...LANG_ONLY_UPSTREAM,
  "flam_spawn_egg", "soo_spawn_egg", "tenn_spawn_egg"
];
for (const id of assetIds) {
  const definition = `src/main/resources/assets/csrp/items/${id}.json`;
  const model = `src/main/resources/assets/csrp/models/item/${id}.json`;
  if (!exists(definition)) {
    failures.push(`missing item model definition ${definition}`);
    continue;
  }
  if (!exists(model)) {
    failures.push(`missing item model ${model}`);
    continue;
  }
  const definitionJson = JSON.parse(read(definition));
  if (definitionJson.model?.model !== `csrp:item/${id}`) {
    failures.push(`${definition} must point at csrp:item/${id}`);
  }
  const modelJson = JSON.parse(read(model));
  const layer = modelJson.textures?.layer0;
  if (!layer) {
    failures.push(`${model} has no layer0 texture`);
    continue;
  }
  const [namespace, texturePath] = layer.split(":");
  if (namespace === "csrp" && !exists(`src/main/resources/assets/csrp/textures/${texturePath}.png`)) {
    failures.push(`${model} binds the missing texture ${layer}`);
  }
  if (namespace === "minecraft" && id !== "relay_report" && id !== "scan_report") {
    failures.push(`${model} unexpectedly falls back to a vanilla texture: ${layer}`);
  }
}

// Newly added spawn eggs must ship a real 16x16 PNG, like every other csrp egg.
for (const [id] of ADDED_SPAWN_EGGS) {
  const texturePath = path.join(root, `src/main/resources/assets/csrp/textures/item/${id}.png`);
  if (!fs.existsSync(texturePath)) {
    failures.push(`${id} is missing its 16x16 texture`);
    continue;
  }
  const png = fs.readFileSync(texturePath);
  const signature = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  if (!png.subarray(0, signature.length).equals(signature)) {
    failures.push(`${id} texture is not a PNG`);
    continue;
  }
  if (png.readUInt32BE(16) !== 16 || png.readUInt32BE(20) !== 16) {
    failures.push(`${id} texture must be 16x16, found ${png.readUInt32BE(16)}x${png.readUInt32BE(20)}`);
  }
  if (png.length <= 100) failures.push(`${id} texture is still a placeholder`);
}

// ---------------------------------------------------------------- 5. cross-checks
for (const [id, entity] of ADDED_SPAWN_EGGS) {
  const entityId = entity.replace("ModEntities.", "").toLowerCase();
  if (!entitySource.includes(`"${entityId}"`) && !entitySource.includes(`"${entityId.replace("_", "")}"`)) {
    failures.push(`${entity} for ${id} is not registered in ModEntities.java`);
  }
}

if (!legacySpawner.includes('case "flam"') && !modItems.includes('"flam_spawn_egg"')) {
  failures.push("the upstream flam/soo/tenn spawn eggs are missing under both naming schemes");
}

if (failures.length) {
  console.error(`Item parity verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Item parity verification passed (${ORIGINAL_ITEM_IDS.length} upstream ids + ` +
  `${LANG_ONLY_UPSTREAM.length} language-only ids + ${ADDED_SPAWN_EGGS.length} spawn eggs).`);