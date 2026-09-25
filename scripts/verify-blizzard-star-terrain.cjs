const fs = require("node:fs");

const read = (path) => fs.readFileSync(path, "utf8");
const failures = [];
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};

const genLayer = read("src/main/java/alku/csrp/world/star/GenLayerSRPDynamicStar.java");
const syncHandler = read("src/main/java/alku/csrp/world/star/SRPStarTypeSyncHandler.java");
const derived = read("src/main/java/alku/csrp/world/star/SRPBlizzardDerivedHandler.java");
const fractured = read("src/main/java/alku/csrp/world/star/SRPFracturedTerrainHandler.java");
const worldData = read("src/main/java/alku/csrp/world/SrpWorldData.java");
const coldSelection = read("src/main/java/alku/csrp/world/SrpColdStarSelection.java");
const createScreen = read("src/main/java/alku/csrp/client/SrpDifficultyScreenEvents.java");

// --- GenLayerSRPDynamicStar: no GenLayer in 26.3 -> BiomeSource dispatch -----------------
expect(!/import\s+net\.minecraft\.world\.gen\.layer\.GenLayer/.test(genLayer),
  "GenLayerSRPDynamicStar still imports the removed 1.12.2 GenLayer");
for (const token of ["BiomeSource select(SrpStarType starType", "case COLD -> cold",
  "case WARM -> warm", "case NORMAL -> original", "SrpWorldData.get(overworld).starType()"])
  expect(genLayer.includes(token), `GenLayerSRPDynamicStar is missing the 26.3 dispatch: ${token}`);

// --- SRPStarTypeSyncHandler: login / respawn / dimension --------------------------------
for (const token of ["PlayerEvent.PlayerLoggedInEvent", "PlayerEvent.PlayerRespawnEvent",
  "PlayerEvent.PlayerChangedDimensionEvent", "serverPlayer.level().getServer().overworld()",
  "MsgSyncStarType.send(serverPlayer, SrpWorldData.get(overworld).starType())"])
  expect(syncHandler.includes(token), `SRPStarTypeSyncHandler is missing original behaviour: ${token}`);

// --- SRPBlizzardDerivedHandler: 100-block Heblu/Kirin scan ------------------------------
for (const token of ["RANGE = 100.0D", "RANGE_SQ = RANGE * RANGE", "CHECK_INTERVAL = 10",
  "player.tickCount % CHECK_INTERVAL != 0", "PlayerTickEvent.Post",
  "MsgSyncBlizzardReverse.send(player, reverse)", "PlayerEvent.PlayerLoggedOutEvent",
  "LAST_STATE.remove(event.getEntity().getUUID())", "KirinEntity.class",
  "player.distanceToSqr(kirin) <= RANGE_SQ", "SrpStarType.COLD", "Level.OVERWORLD",
  "csrp", "heblu", "player.getBoundingBox().inflate(RANGE)"])
  expect(derived.includes(token), `SRPBlizzardDerivedHandler is missing original behaviour: ${token}`);
expect(derived.includes("GenLayerSRPDynamicStar.activeGenerationStarType(level)"),
  "SRPBlizzardDerivedHandler must use the shared generation-time star source");

// --- SRPFracturedTerrainHandler: hook + default-off gate --------------------------------
// 1.12.2 API names may only survive in the provenance javadoc, never in code.
const fracturedCode = fractured
  .replace(/\/\*[\s\S]*?\*\//g, "")
  .replace(/^\s*\/\/.*$/gm, "");
for (const token of ["ChunkEvent.Load", "event.isNewChunk()", "EventPriority.HIGHEST",
  "level.dimension() != Level.OVERWORLD", "SrpWorldData.get(level)",
  "data.fracturedTerrainEnabled()", "SrpStarType.COLD"])
  expect(fractured.includes(token), `SRPFracturedTerrainHandler is missing original gate: ${token}`);
expect(!fracturedCode.includes("PopulateChunkEvent"),
  "SRPFracturedTerrainHandler still uses the removed 1.12.2 PopulateChunkEvent");
// The world-creation toggle must stay opt-in.  The selector itself now lives in the dedicated
// SRP world-settings screen (1.10.9 GuiSRPWorldSettings), so the default is asserted there.
expect(worldData.includes("starType == SrpStarType.COLD && Boolean.TRUE.equals(fracturedTerrain)"),
  "SrpWorldData#fracturedTerrainEnabled no longer requires an explicit opt-in");
expect(createScreen.includes("SrpMeteorMode fracturedTerrain = SrpMeteorMode.OFF;"),
  "the create-world fractured-terrain selector no longer defaults to off");
expect(createScreen.includes("cold && state.fracturedTerrain.enabled()"),
  "the create-world fractured-terrain choice is no longer gated behind a cold star");
expect(coldSelection.includes("consumeFractured()"),
  "SrpColdStarSelection no longer carries the create-world fractured-terrain choice");

// --- SRPFracturedTerrainHandler: original generator constants ---------------------------
for (const token of ["PLATE_SIZE = 96", "PLATE_JITTER = 28", "CRACK_WIDTH = 2.25D",
  "COLLISION_WIDTH = 4.75D", "MIN_RAVINE_DEPTH = 20", "MAX_RAVINE_DEPTH = 52",
  "MIN_COLLISION_RISE = 7", "MAX_COLLISION_RISE = 19", "MIN_PLATE_OFFSET = -12",
  "MAX_PLATE_OFFSET = 16", "Long.rotateLeft(sample.secondaryHash(), 21)",
  "9172280023384029625L", "-7046029254386353131L", "-4658895280553007687L",
  "-7723592293110705685L", "-3335678366873096957L", "-2643881736870682267L",
  "341873128712L", "132897987541L", "value(boundaryHash, 100) < 36",
  "value(boundaryHash >>> 43, 100) < 38", "value(plateHash >>> 31, 100) < 16",
  "ridgeNoise < 0.28D", "value(hash >>> 51, 2) == 0"])
  expect(fractured.includes(token), `SRPFracturedTerrainHandler lost original generator tuning: ${token}`);

// --- SRPFracturedTerrainHandler: 26.3 world access --------------------------------------
for (const token of ["chunk.markUnsaved()", "getLightEngine()", ".checkBlock(",
  "Heightmap.Types.WORLD_SURFACE"])
  expect(fractured.includes(token), `SRPFracturedTerrainHandler is missing 26.3 chunk handling: ${token}`);
for (const token of ["WRITE_FLAGS = 0", "chunk.setBlockState(", "chunk.getBlockState(",
  "BlockTags.DIRT", "BlockTags.SAND", "state.hasBlockEntity()", "state.liquid()",
  "Blocks.BEDROCK"])
  expect(fractured.includes(token), `SRPFracturedTerrainHandler is missing 26.3 block access: ${token}`);
// 1.12.2 Material / obfuscated names must only survive in the provenance javadoc, never in code.
for (const token of ["PopulateChunkEvent", "Material.", "field_150", "func_", "IBlockState",
  "getEntityBoundingBox"])
  expect(!fracturedCode.includes(token),
    `SRPFracturedTerrainHandler still uses 1.12.2 API in code: ${token}`);

if (failures.length) {
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
console.log("Verified dynamic-star dispatch, star type sync, derived-blizzard reversal and the default-off fractured terrain generator.");
