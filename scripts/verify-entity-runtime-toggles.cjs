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

// ---------------------------------------------------------------------------
// 1. SRPConfig.doTileDrops -> RuntimeToggles.parasiteBlockDrops()
//
// out109 facts:
//   entity/ai/misc/EntityParasiteBase.java:2301  destroyBlockPos(blockpos, SRPConfig.doTileDrops)
//   entity/ai/misc/EntityPStationary.java:373    destroyBlockPos(blockpos, SRPConfig.doTileDrops)
//   entity/monster/EntityWaveShock.java:247      destroyBlockPos(blockpos, SRPConfig.doTileDrops)
//   entity/monster/adapted/EntityLumAdapted.java:275
//   entity/monster/primitive/EntityLum.java:275
//   entity/monster/pure/EntityOrch.java:363
//   entity/monster/pure/preeminent/EntityFlam.java:363
//   entity/monster/pure/preeminent/EntityPheon.java:313
// ---------------------------------------------------------------------------
const toggle = read("src/main/java/alku/csrp/config/RuntimeToggles.java");
expect(toggle, /public static boolean parasiteBlockDrops\(\)/,
  "RuntimeToggles.parasiteBlockDrops() is missing");
expect(toggle, /public static boolean mobEvolution\(\)/,
  "RuntimeToggles.mobEvolution() is missing");

const blockDropSites = [
  ["src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java", "EntityParasiteBase/EntityPStationary"],
  ["src/main/java/alku/csrp/entity/PureParasiteEntity.java", "EntityOrch/EntityFlam/EntityPheon"],
  ["src/main/java/alku/csrp/entity/AssimilatedDragonEntity.java", "EntityInfDragonE block sweep"],
  ["src/main/java/alku/csrp/entity/UntamedPriLasherEntity.java", "crude block sweep"]
];
for (const [file, origin] of blockDropSites) {
  const text = read(file);
  expect(text, /destroyBlock\([^)]*RuntimeToggles\.parasiteBlockDrops\(\)/,
    `${file}: block breaking does not honour SRPConfig.doTileDrops (${origin})`);
  expectNo(text, /destroyBlock\([^)]*,\s*true\s*,/,
    `${file}: a destroyBlock call still hard-codes dropBlock=true`);
}

// ---------------------------------------------------------------------------
// 2. ParasiteEventEntity.canSpawnNext -> RuntimeToggles.mobEvolution()
//
// out109 call sites that gate a kill-count driven upgrade:
//   ai/EntityAIFollowBodies.java:65,119   ai/EntityAINexusGrow.java:286,345,404
//   ai/misc/EntityPInfected.java:110      monster/crude/EntityHost.java:197,446
//   monster/deterrent/nexus/EntityVenkrolSIV.java:209
//   monster/inborn/EntityLodo.java:152    monster/inborn/EntityMudo.java:274,297
//   monster/infected/EntityInfPlayer.java:94,114
//   monster/primitive/EntityBano.java:82,149  (+ Canra/Emana/Gim/Hull/Nogla/Ranrac/Shyco)
// ---------------------------------------------------------------------------
const evolutionGates = [
  ["src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java", "EntityBano/Canra/Emana/Gim/Hull/Nogla/Ranrac/Shyco kill-count upgrade"],
  ["src/main/java/alku/csrp/entity/NexusParasiteEntity.java", "EntityAINexusGrow stage upgrade"],
  ["src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java", "EntityPInfected sim -> feral"],
  ["src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java", "EntityPInfected sim -> feral"],
  ["src/main/java/alku/csrp/entity/SimHumanEntity.java", "EntityInfHuman sim -> feral"],
  ["src/main/java/alku/csrp/entity/AssimilatedEndermanEntity.java", "EntityInfEnderman sim -> feral"],
  ["src/main/java/alku/csrp/entity/SimAdventurerEntity.java", "EntityInfPlayer adventurer -> thrall"],
  ["src/main/java/alku/csrp/entity/HostEntity.java", "EntityHost herd upgrade"],
  ["src/main/java/alku/csrp/entity/BuglinEntity.java", "EntityLodo growth upgrade"],
  ["src/main/java/alku/csrp/entity/RupterEntity.java", "EntityMudo mangler upgrade"]
];
for (const [file, origin] of evolutionGates) {
  const text = read(file);
  expect(text, /RuntimeToggles\.mobEvolution\(\)/,
    `${file}: kill-count driven evolution is not gated by canSpawnNext (${origin})`);
  expect(text, /import alku\.csrp\.config\.RuntimeToggles;/,
    `${file}: RuntimeToggles import is missing`);
}

// ---------------------------------------------------------------------------
// 3. docs/gap/R6_ENTITY_MATRIX.md contract
// ---------------------------------------------------------------------------
const matrixPath = path.join(root, "docs/gap/R6_ENTITY_MATRIX.md");
if (!fs.existsSync(matrixPath)) {
  failures.push("docs/gap/R6_ENTITY_MATRIX.md is missing");
} else {
  const matrix = fs.readFileSync(matrixPath, "utf8");
  expect(matrix, /AI 类合计（本矩阵逐条核对） \| 73 \|/, "matrix does not cover all 73 AI classes");
  expect(matrix, /out109 `entity\/monster\/\*\*` java 文件 \| 137 \|/, "matrix does not cover the 137 monster files");
  expect(matrix, /out109 `init\/SRPEntities\.java` 注册的实体 id \| 158 \|/, "matrix misses the original 158 entity ids");
  expect(matrix, /本工程 `ModEntities\.java` 注册的实体 id \| 157 \|/, "matrix misses the 157 ported entity ids");

  // every out109 AI class must have exactly one row
  const aiFiles = [
    "ai/AIDisableBeaconIki.java", "ai/EntityAIAncientSummon.java", "ai/EntityAIAttackMeleeNotGround.java",
    "ai/EntityAIAttackMeleeRangeSwitch.java", "ai/EntityAIAttackMeleeRanged.java", "ai/EntityAIAttackMeleeStatus.java",
    "ai/EntityAIAttackMeleeStatusAOE.java", "ai/EntityAIAttackProjectile.java", "ai/EntityAIAttackRangedStatus.java",
    "ai/EntityAIAttackSwell.java", "ai/EntityAIAttackVenkrol.java", "ai/EntityAIAvoidEntityStatus.java",
    "ai/EntityAIAvoidOrAttack.java", "ai/EntityAIBlockInfest.java", "ai/EntityAIBlockLight.java",
    "ai/EntityAIBlockResidue.java", "ai/EntityAIDiveBomb.java", "ai/EntityAIDodAttack.java",
    "ai/EntityAIEvade.java", "ai/EntityAIEvadeDash.java", "ai/EntityAIEvadeTP.java",
    "ai/EntityAIFlightAttack.java", "ai/EntityAIFlightLimits.java", "ai/EntityAIFollowBodies.java",
    "ai/EntityAIGetFollowers.java", "ai/EntityAIGiveEffectsArea.java", "ai/EntityAIInfectedSearch.java",
    "ai/EntityAIKirinBlink.java", "ai/EntityAINearestAttackableTargetStatus.java", "ai/EntityAINexusGrow.java",
    "ai/EntityAINexusNest.java", "ai/EntityAIParasiteFollow.java", "ai/EntityAISkill.java",
    "ai/EntityAISoundEaterStalk.java", "ai/EntityAISwimmingDiving.java", "ai/EntityAIVenkrolSummon.java",
    "ai/EntityAIWanderStatus.java", "ai/EntityAIWaterLeapAtTargetStatus.java", "ai/SoundEaterSoundHelper.java",
    "ai/misc/EntityAICircleGroup.java", "ai/misc/EntityBodyParts.java", "ai/misc/EntityCanClimb.java",
    "ai/misc/EntityCanColony.java", "ai/misc/EntityCanFly.java", "ai/misc/EntityCanHaveBodies.java",
    "ai/misc/EntityCanMelt.java", "ai/misc/EntityCanPullMobs.java", "ai/misc/EntityCanShoot.java",
    "ai/misc/EntityCanSpawn.java", "ai/misc/EntityCanSummon.java", "ai/misc/EntityCanSwim.java",
    "ai/misc/EntityCanVectors.java", "ai/misc/EntityCutomAttack.java", "ai/misc/EntityPAdapted.java",
    "ai/misc/EntityPAncient.java", "ai/misc/EntityPAssimara.java", "ai/misc/EntityPBeckon.java",
    "ai/misc/EntityPCosmical.java", "ai/misc/EntityPCrude.java", "ai/misc/EntityPDerived.java",
    "ai/misc/EntityPDispatcher.java", "ai/misc/EntityPFeral.java", "ai/misc/EntityPFocused.java",
    "ai/misc/EntityPHijacked.java", "ai/misc/EntityPInfected.java", "ai/misc/EntityPMalleable.java",
    "ai/misc/EntityPPreeminent.java", "ai/misc/EntityPPrimitive.java", "ai/misc/EntityPPure.java",
    "ai/misc/EntityPRooter.java", "ai/misc/EntityPStationary.java", "ai/misc/EntityPStationaryArchitect.java",
    "ai/misc/EntityParasiteBase.java"
  ];
  if (aiFiles.length !== 73) failures.push(`AI contract list has ${aiFiles.length} entries instead of 73`);
  for (const file of aiFiles) {
    if (!matrix.includes(`\`${file}\``)) failures.push(`matrix has no row for ${file}`);
  }

  // dead-code facts must stay documented
  for (const dead of ["AIDisableBeaconIki", "EntityAIEvadeTP", "EntityAINexusNest", "EntityAISoundEaterStalk",
    "EntityCanVectors", "EntityPFocused", "EntityBanoFocused", "EntityShycoFocused", "EntityDharma",
    "EntityOroncoAW", "EntityVenkrolSV", "EntityMor", "EntityRond"]) {
    expect(matrix, new RegExp(`\`?(ai/|ai/misc/|monster/[a-z/]+/)?${dead}\`?`),
      `matrix does not record ${dead} as dead code`);
  }
  expect(matrix, /geneBlocksearch/, "matrix does not explain the EntityAIBlockLight gene 7 finding");
  expect(matrix, /死代码/, "matrix does not mark the dead-code sections");
}

// ---------------------------------------------------------------------------
// 4. entity id level diff must stay at 13 missing / 12 extra
// ---------------------------------------------------------------------------
const modEntities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const ids = new Set();
for (const m of modEntities.matchAll(/(?:ENTITIES\.register|monster)\(\s*"([a-z0-9_]+)"/g)) ids.add(m[1]);
if (ids.size !== 157) failures.push(`ModEntities registers ${ids.size} ids instead of 157`);
for (const merged of ["webball", "spineball", "nadeball", "salivaball", "ballball", "ancientball",
  "biomassball", "missile", "balltall", "ballmall", "salivaeff", "heblu_light", "meteor"]) {
  if (ids.has(merged)) failures.push(`${merged} should stay merged into parasite_projectile`);
}
if (!ids.has("parasite_projectile")) failures.push("parasite_projectile (the merged projectile entity) is missing");

if (failures.length) {
  console.error(`verify-entity-runtime-toggles: ${failures.length} failure(s)`);
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-entity-runtime-toggles: ok");
