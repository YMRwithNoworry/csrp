const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};
const expectNo = (text, pattern, message) => {
  if (pattern.test(text)) failures.push(message);
};
const entity = (name) => read(`src/main/java/alku/csrp/entity/${name}.java`);

// ---------------------------------------------------------------------------
// 1. EntityCanSpawn -> AssimilationSpawnGate contract
//
// out109 facts (entity/ai/misc/EntityCanSpawn.java + per-class overrides):
//   EntityPInfected.canSpawnByIDData() -> 0 (subclasses override)
//   EntityPInfected/EntityPHijacked.getIDSpawn() -> getParasiteIDRegister()
//   EntityFerBear.getIDSpawn()=49  canSpawnByIDData=infbearCanSpawnAssimilatedNat
//   EntityInfPlayer.getIDSpawn()=40 canSpawnByIDData=infhumanCanSpawnAssimilatedNat
//   EntityHiBlaze.canSpawnByIDData=infbearCanSpawnAssimilatedNat (copy/paste in the original)
//   EntityHiSkeleton.canSpawnByIDData=higolemCanSpawnAssimilatedNat
//   SRPSpawning.java:538-546 denies spawning while count < canSpawnByIDData()
// ---------------------------------------------------------------------------
const gate = entity("AssimilationSpawnGate");
expect(gate, /public interface AssimilationSpawnGate/, "AssimilationSpawnGate interface is missing");
expect(gate, /default String assimilationSpawnKey\(\)\s*\{\s*return null;/,
  "assimilationSpawnKey must be a default method returning null so partial implementations compile");
expect(gate, /default int canSpawnByIDData\(\)/, "canSpawnByIDData() is missing");
expect(gate, /MobsConfig\.neededAssimilation\(key\)/, "canSpawnByIDData does not read MobsConfig.neededAssimilation");
expect(gate, /default boolean blockedByAssimilationCount\(ServerLevel level\)/,
  "blockedByAssimilationCount(ServerLevel) is missing");
expect(gate, /assimilationCount\(assimilationSpawnKey\(\)\) < threshold/,
  "the gate does not preserve the original `count < canSpawnByIDData()` direction");
expect(gate, /static void recordAssimilation\(ServerLevel level, Entity converted\)/,
  "recordAssimilation helper is missing");

// Per-class keys, taken from the out109 overrides.
const keys = [
  ["AssimilatedParasiteEntity", "getKind().id()", "EntityInfBear/Cow/Pig/Sheep/Wolf/Squid"],
  ["AssimilatedVariantEntity", "getKind().id()", "EntityDorpa/EntitySpeHorse/Human/Villager"],
  ["AssimilatedHeadEntity", "endsWith(\"head\")", "EntityInf*Head"],
  ["AssimilatedDragonEntity", "sim_dragone", "EntityInfDragonE"],
  ["AssimilatedDragonHeadEntity", "sim_dragone", "EntityInfDragonEHead"],
  ["AssimilatedEndermanEntity", "sim_enderman", "EntityInfEnderman"],
  ["SimHumanEntity", "sim_human", "EntityInfHuman"],
  ["SimAdventurerEntity", "sim_human", "EntityInfPlayer"],
  ["SimAdventurerHeadEntity", "sim_human", "EntityInfPlayerHead"],
  ["FeralEndermanEntity", "sim_enderman", "EntityFerEnderman"],
  ["HiGolemEntity", "hi_golem", "EntityHiGolem"],
  ["HiSkeletonEntity", "hi_golem", "EntityHiSkeleton"],
  ["HiBlazeEntity", "sim_bear", "EntityHiBlaze (reads infbearCanSpawnAssimilatedNat in out109)"]
];
for (const [file, expected, origin] of keys) {
  const text = entity(file);
  // FeralEndermanEntity extends FeralParasiteEntity and Hi* extend HijackedParasiteEntity, so the
  // marker may come from the base class; what matters is the key override.
  expect(text, new RegExp(`assimilationSpawnKey\\(\\)\\s*\\{[\\s\\S]{0,160}?${expected.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}`),
    `${file} does not override assimilationSpawnKey() with ${expected} (${origin})`);
}
expect(entity("FeralParasiteEntity"), /implements[\s\S]{0,80}AssimilationSpawnGate/,
  "FeralParasiteEntity does not implement the gate (EntityFer* family)");
expect(entity("HijackedParasiteEntity"), /implements AssimilationSpawnGate/,
  "HijackedParasiteEntity does not implement the gate (EntityHi* family)");
// FeralParasiteEntity maps every Kind, including the two without an id string.
const feral = entity("FeralParasiteEntity");
expect(feral, /implements[\s\S]{0,80}AssimilationSpawnGate/, "FeralParasiteEntity does not implement the gate");
for (const key of ["sim_bear", "sim_cow", "sim_enderman", "sim_horse", "sim_human", "sim_pig",
  "sim_sheep", "sim_villager", "sim_wolf"]) {
  expect(feral, new RegExp(`"${key}"`), `FeralParasiteEntity does not map ${key}`);
}
// Marauderized* never implemented EntityCanSpawn in 1.10.9, so they must stay ungated.
for (const file of ["MarauderizedBearEntity", "MarauderizedCowEntity", "MarauderizedEndermanEntity",
  "MarauderizedHumanEntity", "MarauderizedSheepEntity", "MarauderizedVillagerEntity",
  "MarauderizedParasiteEntity", "TetheredMarauderizedEntity"]) {
  expectNo(entity(file), /assimilationSpawnKey/,
    `${file} must stay ungated: it never implemented EntityCanSpawn in 1.10.9`);
}

// ---------------------------------------------------------------------------
// 2. Natural-spawn hook
// ---------------------------------------------------------------------------
const events = entity("AssimilationSpawnGateEvents");
expect(events, /@EventBusSubscriber\(modid = Csrp\.MODID\)/, "gate events are not on the mod event bus");
expect(events, /MobSpawnEvent\.PositionCheck event/, "gate does not subscribe to MobSpawnEvent.PositionCheck");
expect(events, /EntitySpawnReason\.NATURAL[\s\S]*?EntitySpawnReason\.CHUNK_GENERATION/,
  "gate does not restrict itself to natural / chunk-generation spawns");
expect(events, /AssimilationSpawnGate\.blocksNaturalSpawn\(level, event\.getEntity\(\)\)/,
  "gate does not consult AssimilationSpawnGate.blocksNaturalSpawn");
expect(events, /event\.setResult\(MobSpawnEvent\.PositionCheck\.Result\.FAIL\)/,
  "gate does not deny the spawn");

// ---------------------------------------------------------------------------
// 3. Counter increments on a successful assimilation
// ---------------------------------------------------------------------------
const mechanics = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
expect(mechanics, /AssimilationSpawnGate\.recordAssimilation\(level, converted\)/,
  "the assimilation conversion path does not increment the per-type counter");
expect(mechanics, /import alku\.csrp\.entity\.AssimilationSpawnGate;/,
  "InfectionMechanics does not import AssimilationSpawnGate");
expect(mechanics, /if \(!level\.addFreshEntity\(converted\)\)[\s\S]{0,600}?recordAssimilation/,
  "the counter is incremented before the converted entity is actually added");

// ---------------------------------------------------------------------------
// 4. NexusProtection triggers
//
// out109: EntityPDispatcher.generateStructure() -> NexusProtection1 at blockPosition().below()
//         EntityPBeckon.generateStructure()     -> NexusProtection2 at blockPosition()
//         EntityPRooter.generateStructure()     -> NexusProtection3 at blockPosition()
//         all constructed with stage 1, rolled from EntityAINexusGrow.upgradeV/upgradeD/upgradeL
//         with 0.5 (Beckon) / 0.3 (Dispatcher) / 0.3 (Rooter) and gated on SRPConfig.nexusStructures
// ---------------------------------------------------------------------------
const nexus = entity("NexusParasiteEntity");
expect(nexus, /import alku\.csrp\.world\.gen\.WorldGenParasiteNexusProtection1;/,
  "NexusParasiteEntity does not import NexusProtection1");
expect(nexus, /new WorldGenParasiteNexusProtection1\(1\)\.generate\(serverLevel, random, origin\)/,
  "Dispatcher upgrade does not generate NexusProtection1 with stage 1");
expect(nexus, /new WorldGenParasiteNexusProtection2\(1\)\.generate\(serverLevel, random, origin\)/,
  "Beckon upgrade does not generate NexusProtection2 with stage 1");
expect(nexus, /new WorldGenParasiteNexusProtection3\(1\)\.generate\(serverLevel, random, origin\)/,
  "Rooter upgrade does not generate NexusProtection3 with stage 1");
expect(nexus, /case DISPATCHER -> 0\.3D;/, "Dispatcher structure chance is not the original 0.3");
expect(nexus, /case ROOTER -> 0\.3D;/, "Rooter structure chance is not the original 0.3");
expect(nexus, /case BECKON -> 0\.5D;/, "Beckon structure chance is not the original 0.5");
expect(nexus, /activeKind\.family == Family\.DISPATCHER \? blockPosition\(\)\.below\(\) : blockPosition\(\)/,
  "the Dispatcher structure must be placed at blockPosition().below()");
expect(nexus, /generateProtectionStructure\(serverLevel, activeKind\)[\s\S]{0,120}?discard\(\);/,
  "the structure is not generated on a successful stage upgrade");

if (failures.length) {
  console.error(`verify-entity-assimilation-gate: ${failures.length} failure(s)`);
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-entity-assimilation-gate: ok");
