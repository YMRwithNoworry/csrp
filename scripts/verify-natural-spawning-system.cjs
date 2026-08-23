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
for (const development of ["UD_TWO", "UD_THREE", "UD_FOUR"]) {
  expect(tables.includes(development), `Ubiquitous development table ${development} is missing`);
}

expectPattern(tables,
  /spawn\("buglin", 2, 6, 30\)/,
  "Phase 0 Buglin group or weight is not original");
expectPattern(tables,
  /spawn\("pri_devourer", 1, 2, 1\)/,
  "Phase -1 Primitive Devourer group or weight is not original");
expectPattern(tables,
  /spawn\("grunt", 3, 6, 30\)[\s\S]*?spawn\("grunt", 6, 10, 40\)/,
  "UD4 dual Grunt entries are not original");
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
  /usesUbiquitousTable\(level\)[\s\S]*?getGameTime\(\)[\s\S]*?sample < UBIQUITOUS_TABLE_CHANCE/,
  "UD 50 percent selection is not stable within a game tick");
expect(!tables.includes("level.random.nextDouble()"),
  "UD selection still rerolls whenever spawn candidates are queried");
expectPattern(tables,
  /canSpawnNaturally\([\s\S]*?contains\(select\(level, pos\), path\)[\s\S]*?crossDimensionUnlocked/,
  "Position checks do not validate the same selected phase or UD table");

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
  /(?:IN_AIR|NO_RESTRICTIONS)[\s\S]*?isWithinBounds\(pos\)[\s\S]*?isEmptyBlock\(pos\.below\(\)\)[\s\S]*?isEmptyBlock\(pos\)[\s\S]*?isEmptyBlock\(pos\.above\(\)\)/,
  "Air placement does not require an in-border three-block air column");
expectPattern(common,
  /NaturalSpawnTables\.allSpawnTypes\(\)[\s\S]*?WATER_SPAWN_IDS\.contains\(id\)[\s\S]*?(?:SpawnPlacementTypes\.IN_WATER|SpawnPlacements\.Type\.IN_WATER)[\s\S]*?AIR_SPAWN_IDS\.contains\(id\) \? (?:IN_AIR|SpawnPlacements\.Type\.NO_RESTRICTIONS) : (?:SpawnPlacementTypes\.ON_GROUND|SpawnPlacements\.Type\.ON_GROUND)/,
  "Natural spawn types are not registered across water, air and ground placements");
expectPattern(common,
  /Monster\.checkMonsterSpawnRules[\s\S]*?(?:RegisterSpawnPlacementsEvent\.Operation\.REPLACE|SpawnPlacementRegisterEvent\.Operation\.REPLACE)/,
  "Natural spawn placements do not retain normal monster rules");

if (failures.length) {
  console.error("Natural spawning verification failed:");
  failures.forEach((failure) => console.error("- " + failure));
  process.exit(1);
}

console.log("Natural spawning verification passed.");
