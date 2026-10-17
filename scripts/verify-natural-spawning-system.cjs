const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};
const expectPattern = (source, pattern, message) => expect(pattern.test(source), message);

const tables = read("src/main/java/alku/csrp/world/NaturalSpawnTables.java");
const events = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const system = read("src/main/java/alku/csrp/world/EvolutionSystem.java");
const common = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
const blockLanguage = JSON.parse(read("src/main/resources/assets/csrp/lang/zh_cn.json"));

const blockstateDirectory = path.join(root, "src/main/resources/assets/csrp/blockstates");
for (const filename of fs.readdirSync(blockstateDirectory).filter((name) => name.endsWith(".json"))) {
  const id = filename.slice(0, -5);
  expect(Object.hasOwn(blockLanguage, `block.csrp.${id}`), `Block csrp:${id} has no Chinese name`);
}

const spawnIds = [...tables.matchAll(/spawn\("([a-z0-9_]+)"/g)].map((match) => match[1]);
for (const id of new Set(spawnIds)) {
  expect(entities.includes(`"${id}"`), `Natural spawn entity csrp:${id} is not registered`);
}

for (const phase of [
  "PHASE_MINUS_ONE", "PHASE_ZERO", "PHASE_ONE", "PHASE_TWO", "PHASE_THREE",
  "PHASE_FOUR", "PHASE_FIVE", "PHASE_SIX", "PHASE_SEVEN", "PHASE_EIGHT",
  "PHASE_NINE", "PHASE_TEN"
]) {
  expect(tables.includes(phase), `Natural spawn table ${phase} is missing`);
}
expectPattern(tables,
  /spawn\("buglin", 2, 6, 30\)/,
  "Phase 0 Buglin group or weight is not original");
expectPattern(tables,
  /spawn\("pri_devourer", 1, 2, 1\)/,
  "Phase -1 Primitive Devourer group or weight is not original");
expectPattern(tables,
  /PHASE_EIGHT = latePhase\(false, false\)[\s\S]*?PHASE_NINE = latePhase\(true, true\)[\s\S]*?PHASE_TEN = latePhase\(true, false\)/,
  "Late phase Dragon, Architect or preeminent group variants are wrong");

expectPattern(tables,
  /phase == -2 \|\| phase == -1 && !isInsideVector\(level, pos\)/,
  "Phase -2 denial or phase -1 EIV radius gate is missing");
expectPattern(tables,
  /vector\.health\(\) > 0 && vector\.pos\(\)\.distSqr\(pos\) <= radius \* radius/,
  "Natural spawning does not require a living EIV within its radius");
expectPattern(tables,
  /return phaseEntries\(phase\);/,
  "Natural spawning can still replace the local phase table with a global development table");
expect(!tables.includes("ubiquitousEntries") && !tables.includes("usesUbiquitousTable"),
  "Natural spawning still has a cross-dimension table override");
expect(!tables.includes("UD_TWO") && !tables.includes("UD_THREE") && !tables.includes("UD_FOUR"),
  "Cross-stage ubiquitous-development mobs remain in the natural spawn registry");
expectPattern(tables,
  /canSpawnNaturally\([\s\S]*?contains\(select\(level, pos\), path\)[\s\S]*?crossDimensionUnlocked/,
  "Position checks do not validate the selected phase table");

expectPattern(events,
  /LevelEvent\.PotentialSpawns[\s\S]*?MobCategory\.MONSTER[\s\S]*?removeSpawnerData[\s\S]*?NaturalSpawnTables\.select[\s\S]*?addSpawnerData/,
  "PotentialSpawns is not replacing CSRP candidates with the active table");
expectPattern(events,
  /List\.copyOf\(event\.getSpawnerDataList\(\)\)/,
  "PotentialSpawns does not safely preserve non-CSRP spawn candidates");
expect(!events.includes("MobSpawnEvent.PositionCheck.Result.SUCCEED"),
  "Natural parasite checks still bypass placement, collision or light rules");
expectPattern(system,
  /canNaturallySpawn\(String path, int phase\)[\s\S]*?NaturalSpawnTables\.canSpawnAtPhase/,
  "EvolutionSystem does not use the discrete original phase tables");
expectPattern(system,
  /path\.equals\("kirin"\)[\s\S]*?Level\.END[\s\S]*?requiredPhase = 7[\s\S]*?path\.equals\("draconite"\)[\s\S]*?Level\.NETHER[\s\S]*?requiredPhase = 7/,
  "Kirin or Draconite cross-dimension phase 7 unlock is missing");
expectPattern(blocks,
  /LEGACY_NO_COLLISION_BLOCKS = java\.util\.Set\.of\([\s\S]*?"harlequinn_grass"[\s\S]*?"infestedbush"[\s\S]*?"parasitebush"[\s\S]*?"parasitesapling"[\s\S]*?LEGACY_NO_COLLISION_BLOCKS\.contains\(id\)[\s\S]*?properties\.noCollission\(\)/,
  "Legacy grass and bush blocks still have full-block collision");

for (const id of ["sim_squid", "pri_devourer", "ada_devourer"]) {
  expect(common.includes(`"${id}"`), `Water spawn id ${id} is missing`);
}
for (const id of [
  "carrier_flying", "lice", "sim_dragone", "pri_yelloweye", "ada_yelloweye",
  "pri_vermin", "airscrew", "overseer", "bomber_light", "bomber_heavy", "wraith",
  "bogle", "architect", "draconite"
]) {
  expect(common.includes(`"${id}"`), `Air spawn id ${id} is missing`);
}
expectPattern(common,
  /IN_AIR = [\s\S]*?isWithinBounds\(pos\)[\s\S]*?isEmptyBlock\(pos\.below\(\)\)[\s\S]*?isEmptyBlock\(pos\)[\s\S]*?isEmptyBlock\(pos\.above\(\)\)/,
  "Air placement does not require an in-border three-block air column");
expectPattern(common,
  /NaturalSpawnTables\.allSpawnTypes\(\)[\s\S]*?WATER_SPAWN_IDS\.contains\(id\)[\s\S]*?SpawnPlacementTypes\.IN_WATER[\s\S]*?AIR_SPAWN_IDS\.contains\(id\) \? IN_AIR : SpawnPlacementTypes\.ON_GROUND/,
  "Natural spawn types are not registered across water, air and ground placements");
expectPattern(common,
  /Monster\.checkMonsterSpawnRules[\s\S]*?RegisterSpawnPlacementsEvent\.Operation\.REPLACE/,
  "Natural spawn placements do not retain normal monster rules");

if (failures.length) {
  console.error("Natural spawning verification failed:");
  failures.forEach((failure) => console.error("- " + failure));
  process.exit(1);
}

console.log("Natural spawning verification passed.");
