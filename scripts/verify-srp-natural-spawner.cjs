const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};
const expectPattern = (source, pattern, message) => expect(pattern.test(source), message);

const spawner = read("src/main/java/alku/csrp/world/SrpWorldParasiteSpawner.java");
const events = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const worldConfig = read("src/main/java/alku/csrp/config/WorldConfig.java");
const common = read("src/main/java/alku/csrp/registry/CommonModEvents.java");

// The dedicated spawner must exist and be ticked once per level tick, like the original
// SRPEventHandlerBus#tickSpawn worldTick hook.
expectPattern(events,
  /tickGeneration\(LevelTickEvent\.Post event\)[\s\S]*?Config\.useEvolutionPhases\(\) && Config\.phaseCustomSpawner\(\)[\s\S]*?SrpWorldParasiteSpawner\.findChunksForSpawning\(level\)/,
  "The dedicated SRP spawner is not driven from the level tick");
expect(!/getGameTime\(\) % 20L == 0L[\s\S]{0,200}SrpWorldParasiteSpawner/.test(events),
  "The dedicated SRP spawner is rate limited to every 20 ticks instead of every tick");

// The original ran its custom spawner while phaseCustomSpawner was on, and only fell back to
// biome-list injection when evolution phases / custom spawning were disabled.
expectPattern(events,
  /replaceNaturalSpawnCandidates[\s\S]*?if \(\(Config\.useEvolutionPhases\(\) && Config\.phaseCustomSpawner\(\)\)[\s\S]*?return;/,
  "Vanilla MONSTER candidate injection is not skipped while the phase spawner is active");

// Original SRPWorldParasiteSpawner#findChunksForSpawningVanilla loop shape.
expectPattern(spawner, /SPAWN_CHUNK_RADIUS = 8/, "Spawner no longer scans an 8 chunk radius");
expectPattern(spawner,
  /boolean border = dx == -SPAWN_CHUNK_RADIUS[\s\S]*?dz == SPAWN_CHUNK_RADIUS/,
  "Spawner no longer excludes the outer chunk ring the original skipped");
expectPattern(spawner, /CLUSTER_ATTEMPTS = 3/, "Spawner lost the original three cluster passes");
expectPattern(spawner,
  /Mth\.ceil\(level\.random\.nextFloat\(\) \* 4\.0F\)/,
  "Spawner no longer uses the original ceil(rand*4) attempt count");
expectPattern(spawner,
  /nextInt\(6\) - level\.random\.nextInt\(6\)/,
  "Spawner lost the original +-6 horizontal scatter");
expectPattern(spawner,
  /hasNearbyAlivePlayer\(centerX, y, centerZ, MIN_SPAWN_DISTANCE\)/,
  "Spawner lost the original 24 block player exclusion");
expectPattern(spawner,
  /worldSpawn\.distToCenterSqr\(centerX, y, centerZ\) < 576\.0D/,
  "Spawner lost the original 576 block world-spawn exclusion");
expectPattern(spawner,
  /level\.getBlockState\(start\)\.isRedstoneConductor\(level, start\)/,
  "Spawner lost the original redstone-conductor rejection");
expectPattern(spawner,
  /packSize >= EventHooks\.getMaxSpawnClusterSize\(mob\)[\s\S]*?continue chunkLoop/,
  "Spawner lost the original per-chunk pack size early exit");

// Table selection must come from SRP tables, not the biome monster list.
expectPattern(spawner,
  /pickSpawnEntry\([\s\S]*?NaturalSpawnTables\.select\(level, pos\)[\s\S]*?WeightedRandom\.getRandomItem/,
  "Spawner does not draw from the weighted SRP phase tables");
expectPattern(spawner,
  /crossDimensionUnlocked\(level, pathOf\(entry\.type\)\)/,
  "Spawner lost the cross-dimension Kirin/Draconite unlock gate");

// Air handling: the original rejected 70 percent and relocated to the nearest far player.
expectPattern(spawner, /AIR_SPAWN_REJECT_PERCENT = 70/, "Air spawn rejection is not the original 70 percent");
expectPattern(spawner,
  /nearestPlayerBeyond[\s\S]*?d > threshold/,
  "Air relocation does not pick the nearest player beyond 24 blocks like the original");
expectPattern(spawner,
  /Math\.min\(base, WorldConfig\.spawnerSkyLimitUp\(\)\)/,
  "Air relocation is not clamped by spawnerSKYLimitUp");

// Caps and cleaner semantics.
expectPattern(worldConfig, /naturalWaterMobCap\(\)/, "Aquatic spawn cap getter is missing");
expectPattern(worldConfig, /naturalAirMobCap\(\)/, "Flying spawn cap getter is missing");
expectPattern(worldConfig, /spawnerSkyLimitUp\(\)/, "Sky spawn limit getter is missing");
expectPattern(worldConfig, /"worldWaterCap", 3/, "worldWaterCap default is not the original 3");
expectPattern(worldConfig, /"worldAirCap", 3/, "worldAirCap default is not the original 3");
expectPattern(worldConfig, /"spawnerSKYLimitUp", 250/, "spawnerSKYLimitUp default is not the original 250");
expectPattern(spawner,
  /WORKER_CAP = 10/,
  "Worker cap is not the original worker > 10 gate");
expectPattern(spawner,
  /isGnatLike\(id\) && exceeds\(gnats, Config\.worldGnatCap\(\)\)/,
  "Gnat/Lice cap is not enforced by the spawner");

// Water spawns drop the liquid-in-hitbox requirement, mirroring the original isNotColliding override.
expectPattern(spawner,
  /if \(!isWaterSpawnType\(type\)\)[\s\S]*?EventHooks\.checkSpawnPosition\(mob, level, MobSpawnType\.NATURAL\)/,
  "Non-aquatic spawns do not use the standard position check");
expectPattern(spawner,
  /level\.isUnobstructed\(mob\)/,
  "Aquatic spawns do not replace checkSpawnObstruction with isUnobstructed");
expect(!spawner.includes("inLiquid && !unobstructed"),
  "Aquatic spawns still run the liquid-in-hitbox obstruction test");

// The spawn sets must stay shared with placement registration so air/water handling cannot drift.
expectPattern(common, /public static final Set<String> WATER_SPAWN_IDS/,
  "Water spawn id set is not exposed for the dedicated spawner");
expectPattern(common, /public static final Set<String> AIR_SPAWN_IDS/,
  "Air spawn id set is not exposed for the dedicated spawner");

if (failures.length) {
  console.error("SRP natural spawner verification failed:");
  failures.forEach((failure) => console.error("- " + failure));
  process.exit(1);
}

console.log("SRP natural spawner verification passed.");
