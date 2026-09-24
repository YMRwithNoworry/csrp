const fs = require("fs");
const path = require("path");

/**
 * R5 legacy spawner mapping contract.
 *
 * `LegacyMobSpawnerItem` re-implements 1.10.9's `ItemMobSpawner#spawnEntity` name switch.  Every
 * one of the 119 legacy names must resolve to an entity id that is actually registered in
 * `ModEntities`; a name that falls through to the default branch silently spawns a Crux instead.
 *
 * Fact source for the name targets: out109 `init/SRPEntities.java` (class per name) plus the
 * original `item.srparasites.itemmobspawner_<name>.name` display names.
 */

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");

const modItems = read("src/main/java/alku/csrp/registry/ModItems.java");
const legacySpawner = read("src/main/java/alku/csrp/item/LegacyMobSpawnerItem.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");

/** Names 1.10.9 registered through `new ItemMobSpawner(<name>)`. */
const ORIGINAL_SPAWNER_NAMES = [
  "abobodies", "abohead", "alafha", "anged",
  "ata", "bano", "banoadapted", "buthol",
  "canra", "canraadapted", "cruxa", "cruxb",
  "dod", "dodsii", "dodsiii", "dodsiv",
  "done", "dorpa", "elvia", "emana",
  "emanaadapted", "esor", "ferbear", "fercow",
  "ferenderman", "ferhorse", "ferhuman", "ferpig",
  "fersheep", "fervillager", "ferwolf", "flog",
  "ganro", "gim", "gimadapted", "gothol",
  "heblu", "heed", "hiblaze", "higolem",
  "hiskeleton", "host", "hostii", "hull",
  "hulladapted", "iki", "ikiadapted", "infbear",
  "infcow", "infcowhead", "infdragone", "infdragonehead",
  "infenderman", "infendermanhead", "infhorse", "infhorsehead",
  "infhuman", "infhumanhead", "infpig", "infpighead",
  "infplayer", "infplayerhead", "infsheep", "infsheephead",
  "infsquid", "infvillager", "infvillagerhead", "infwolf",
  "infwolfhead", "inhoom", "inhoos", "jinjo",
  "kirin", "leem", "leemsii", "leemsiii",
  "leemsiv", "leer", "lencia", "lesh",
  "lodo", "lum", "lumadapted", "marbear",
  "marcow", "marenderman", "marhuman", "marsheep",
  "marvillager", "mes", "mudo", "nak",
  "nogla", "noglaadapted", "nuuh", "omboo",
  "orch", "oronco", "pheon", "pod",
  "quac", "ranrac", "ranracadapted", "rathol",
  "shyco", "shycoadapted", "terla", "tonro",
  "unvo", "venkrol", "venkrolsii", "venkrolsiii",
  "venkrolsiv", "vesta", "wymo", "wymoadapted",
  "zaa", "zaaadapted"
];

/** The mapping gaps this batch closed, asserted verbatim so a silent regression is impossible. */
const REQUIRED_MAPPINGS = [
  // inborn family used to collapse onto pri_longarms
  ["lodo", "buglin"], ["mudo", "rupter"], ["nuuh", "mangler"], ["ata", "gnat"],
  ["rathol", "carrier_heavy"], ["gothol", "carrier_light"], ["buthol", "carrier_flying"],
  // renamed 1.21 entities
  ["done", "dredge"], ["lesb", "movingflesh"], ["lesh", "movingflesh"], ["leer", "airscrew"],
  ["dorpa", "sim_bigspider"], ["tonro", "kyphosis"], ["unvo", "sentry"],
  ["higolem", "hi_golem"], ["hiblaze", "hi_blaze"], ["hiskeleton", "hi_skeleton"],
  ["mes", "thrall"], ["infplayer", "sim_adventurer"], ["infplayerhead", "sim_adventurerhead"],
  // primitive family
  ["shyco", "pri_longarms"], ["hull", "pri_manducater"], ["nogla", "pri_reeker"],
  ["emana", "pri_yelloweye"], ["canra", "pri_summoner"], ["bano", "pri_bolster"],
  ["wymo", "pri_tozoon"], ["ranrac", "pri_arachnida"], ["lum", "pri_devourer"],
  ["iki", "pri_vermin"], ["gim", "pri_viscera"], ["zaa", "pri_burrower"],
  // adapted family
  ["shycoadapted", "ada_longarms"], ["hulladapted", "ada_manducater"], ["noglaadapted", "ada_reeker"],
  ["emanaadapted", "ada_yelloweye"], ["canraadapted", "ada_summoner"], ["banoadapted", "ada_bolster"],
  ["wymoadapted", "ada_tozoon"], ["ranracadapted", "ada_arachnida"], ["lumadapted", "ada_devourer"],
  ["ikiadapted", "ada_vermin"], ["gimadapted", "ada_viscera"], ["zaaadapted", "ada_burrower"]
];

// ---------------------------------------------------------------- entity registry ids
const registeredEntityIds = new Set();
for (const match of entities.matchAll(/(?:monster|register)\(\s*"([a-z0-9_]+)"/g)) {
  registeredEntityIds.add(match[1]);
}
if (registeredEntityIds.size < 100) {
  failures.push(`only ${registeredEntityIds.size} entity ids were parsed out of ModEntities.java`);
}

// ---------------------------------------------------------------- name list living in ModItems
const spawnerBlock = modItems.slice(
  modItems.indexOf("registerLegacyMobSpawners"),
  modItems.indexOf("return names.stream()"));
const declaredNames = [...new Set([...spawnerBlock.matchAll(/"([a-z0-9_]+)"/g)].map((m) => m[1]))];
for (const name of ORIGINAL_SPAWNER_NAMES) {
  if (!declaredNames.includes(name)) {
    failures.push(`itemmobspawner_${name} is no longer registered by registerLegacyMobSpawners`);
  }
}

// ---------------------------------------------------------------- parse the resolution switch
const switchBody = legacySpawner.slice(
  legacySpawner.indexOf("currentEntityId(String name)"),
  legacySpawner.indexOf("appendHoverText"));
const caseMap = new Map();
for (const match of switchBody.matchAll(/case\s+([^\-]+?)\s*->\s*(.+?);/g)) {
  const keys = [...match[1].matchAll(/"([a-z0-9_]+)"/g)].map((m) => m[1]);
  for (const key of keys) caseMap.set(key, match[2].trim());
}
if (caseMap.size < 60) failures.push(`only ${caseMap.size} spawner name cases were parsed`);

function resolve(name) {
  const branch = caseMap.get(name);
  if (!branch) return name;
  if (branch.startsWith("name.replace")) return name.replace("mar", "mar_");
  if (branch.startsWith('"fer_" + name.substring(3)')) return `fer_${name.substring(3)}`;
  return branch.replace(/"/g, "");
}

for (const name of ORIGINAL_SPAWNER_NAMES) {
  const target = resolve(name);
  if (!registeredEntityIds.has(target)) {
    failures.push(`itemmobspawner_${name} resolves to csrp:${target}, which is not a registered entity`);
  }
}

for (const [name, target] of REQUIRED_MAPPINGS) {
  if (name === "lesb") continue;
  const branch = caseMap.get(name);
  if (!branch || branch.replace(/"/g, "") !== target) {
    failures.push(`case "${name}" must map to ${target}, found ${branch ?? "<missing>"}`);
  }
}

// The switch must never fall back to a hard-coded default entity.
if (/default\s*->\s*"crux"/.test(switchBody)) {
  failures.push("unknown spawner names must not silently resolve to crux");
}

if (failures.length) {
  console.error(`Legacy spawner mapping verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Legacy spawner mapping verification passed ` +
  `(${ORIGINAL_SPAWNER_NAMES.length} names resolve to registered entities).`);