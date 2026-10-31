const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = [
  "beckon_si", "beckon_sii", "beckon_siii", "beckon_siv",
  "dispatcher_si", "dispatcher_sii", "dispatcher_siii", "dispatcher_siv",
  "rooter_si", "rooter_sii", "rooter_siii", "rooter_siv", "rooterball",
  "abo_bodies", "abo_head"
];
const failures = [];
const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
};
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const creative = read("src/main/java/alku/csrp/Csrp.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
const mobsConfig = read("src/main/java/alku/csrp/config/MobsConfig.java");
const abomination = read("src/main/java/alku/csrp/entity/AbominationEntity.java");

for (const feature of [
  "BECKON_SIV", "DISPATCHER_SIV", "ROOTER_SIV", "ROOTERBALL", "DamageTypeTags.IS_FIRE",
  "spawnBombVolley", "spawnPodVolley", "spawnRootmassCysts", "rootmassCystsInRange",
  "rooterCystRange", "rooterCystLimit", "summonBeckonParasites",
  "summonDispatcherDefenses", "applyRooterSupport", "createStormVortex", "evolve"
]) {
  if (!nexus.includes(feature)) failures.push(`Nexus behavior is missing ${feature}`);
}
expect(nexus, /float sharedDamage = amount \/ cysts\.size\(\);/,
  "Rootmass Cyst damage is not split across nearby cysts");
expect(nexus, /else \{\s*spawnRootmassCysts\(MobsConfig\.rooterCystLimit\(activeKind\.stage\)\);\s*\}/,
  "Rooter does not create Rootmass Cysts before taking an unprotected hit");
for (const [stage, range, effectRange, cooldown, limit] of [
  ["SI", 16, 20, 12, 3], ["SII", 32, 30, 10, 4],
  ["SIII", 48, 40, 8, 5], ["SIV", 128, 56, 6, 6]
]) {
  for (const [suffix, value] of [["RANGE", range], ["EFFECT_RANGE", effectRange],
    ["COOLDOWN", cooldown], ["LIMIT", limit]]) {
    expect(mobsConfig, new RegExp(`ROOTER_${stage}_${suffix} = intValue\\([\\s\\S]*?", ${value},`),
      `Rooter stage ${stage} has the wrong ${suffix} default`);
  }
}
expect(nexus, /rooterCycleTicks == 5[\s\S]*?supportCooldown--[\s\S]*?rooterCycleTicks == 10[\s\S]*?MobsConfig\.rooterSupportCooldown/,
  "Rooter support does not use the original cycle and cooldown ordering");
expect(nexus, /MobsConfig\.rooterEffectRange\(stage\)[\s\S]*?rooterArea\(range, range\)/,
  "Rooter support does not use its configured effect range");
expect(nexus, /EvolutionSystem\.applyRooterGenes\(ally\)/,
  "Rooter support does not apply the original genes");
for (const feature of ["Kind.BODIES", "FastMeleeAttackGoal", "DamageTypeTags.IS_FIRE", "applyBodiesSupport"]) {
  if (!abomination.includes(feature)) failures.push(`Abomination behavior is missing ${feature}`);
}
expect(abomination, /MAX_HEALTH, 17\.0D/, "Abomination health is missing");
expect(abomination, /ARMOR, 10\.0D/, "Abomination armor is missing");
expect(abomination, /ATTACK_DAMAGE, 9\.0D/, "Abomination attack damage is missing");
for (const [stage, range] of [[1, 16], [2, 32], [3, 48], [4, 128]]) {
  expect(mobsConfig,
    new RegExp(`NEXUS_STAGE${stage}_FOLLOW = followRange\\([\\s\\S]*?nexusStage${stage}FollowRange", ${range}\\.0D\\)`),
    `Nexus stage ${stage} follow-range default does not match the original`);
}
expect(nexus, /FOLLOW_RANGE, kind\.defaultFollowRange/,
  "Nexus entity attribute registration reads configuration before it is loaded");
expect(nexus, /MobsConfig\.nexusGrowthDelayTicks\(kind, random\)/,
  "Nexus growth delay is not initialized from the configured stage range");
expect(nexus, /supportsOrdinaryGrowth\(\) \&\& growthDelayTicks > 0/,
  "ordinary Nexus growth is not gated by the stage's growth behavior");
expect(nexus, /!activeKind\.supportsOrdinaryGrowth\(\) \|\| !RuntimeToggles\.mobEvolution\(\)/,
  "evolve() does not explicitly reject Nexus stages without ordinary growth");
expect(nexus, /ROOTER_SI\(Family\.ROOTER, 1, 25\.0D, 4\.0D, 2\.5D/,
  "Rooter stage I attributes do not reuse the original Venkrol stage I values");
expect(nexus, /ROOTER_SII\(Family\.ROOTER, 2, 60\.0D, 8\.0D, 6\.0D/,
  "Rooter stage II attributes do not reuse the original Venkrol stage II values");
expect(nexus, /ROOTER_SIII\(Family\.ROOTER, 3, 110\.0D, 16\.0D, 13\.0D/,
  "Rooter stage III attributes do not reuse the original Venkrol stage III values");
expect(nexus, /ROOTER_SIV\(Family\.ROOTER, 4, 220\.0D, 25\.0D, 20\.0D/,
  "Rooter stage IV attributes do not reuse the original Venkrol stage IV values");
expect(mobsConfig, /nexusGrowthDelayTicks\(NexusParasiteEntity\.Kind kind, net\.minecraft\.util\.RandomSource random\)[\s\S]*?BECKON_SI, DISPATCHER_SI, ROOTER_SI, ROOTER_SII, ROOTER_SIII[\s\S]*?BECKON_SII, DISPATCHER_SII[\s\S]*?BECKON_SIII, DISPATCHER_SIII[\s\S]*?BECKON_SIV, DISPATCHER_SIV, ROOTER_SIV, ROOTERBALL/,
  "Nexus growth-time mapping does not match the original families and stages");
expect(mobsConfig, /random\.nextInt\(maximumSeconds - minimumSeconds \+ 1\)[\s\S]*?return seconds \* 20/,
  "Nexus growth seconds are not sampled with the inclusive original range and converted to ticks");
for (const [key, value] of [["nexusMinGrowTime", 240], ["nexusMaxGrowTime", 300],
  ["nexussiiMinGrowTime", 300], ["nexussiiMaxGrowTime", 360],
  ["nexussiiiMinGrowTime", 600], ["nexussiiiMaxGrowTime", 1200]]) {
  expect(mobsConfig, new RegExp(`"${key}", ${value}`), `${key} does not preserve the original default`);
}
expect(nexus, /ROOTERBALL\(Family\.ROOTERBALL,[^\n]*, 16\.0D\)/,
  "Rooterball follow-range default does not match the original");
const originalConfigEvents = read("src/main/java/alku/csrp/config/OriginalConfigEvents.java");
expect(originalConfigEvents, /double followRange = MobsConfig\.followRange\(entity\);[\s\S]*?follow\.setBaseValue\(followRange\)/,
  "Configured Nexus follow ranges are not applied after entity creation");
expect(mobsConfig, /entity instanceof NexusParasiteEntity nexus\) return nexusFollowRange\(nexus\.getKind\(\)\)/,
  "Nexus follow-range overrides are not applied when an entity joins the server");
for (const [stage, kind] of [[1, "SI"], [2, "SII"], [3, "SIII"], [4, "SIV"]]) {
  expect(mobsConfig, new RegExp(`case DISPATCHER_${kind} -> NEXUS_STAGE${stage}_FOLLOW\\.get\\(\\) \\* DISPATCHER_${kind}_FOLLOW_MULTIPLIER\\.get\\(\\)`),
    `Dispatcher stage ${kind} does not multiply its configured Nexus follow range`);
  expect(mobsConfig, new RegExp(`DISPATCHER_${kind}_FOLLOW_MULTIPLIER = intValue\\([\\s\\S]*?dispatcher${kind}FollowRangeMultiplier", 2, 1, 100`),
    `Dispatcher stage ${kind} follow-range multiplier does not preserve its original default`);
}

for (const id of ids) {
  const constant = id.toUpperCase();
  expect(entities, new RegExp(`monster\\("${id}"`), `${id}: entity registration is missing`);
  expect(items, new RegExp(`"${id}_spawn_egg"`), `${id}: spawn egg is missing`);
  expect(attributes, new RegExp(`ModEntities\\.${constant}`), `${id}: attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id}: renderer is missing`);
  expect(creative, new RegExp(`ModItems\\.${constant}_SPAWN_EGG`), `${id}: creative entry is missing`);
  expect(english, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: English translation is missing`);
  expect(chinese, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: Chinese translation is missing`);

  for (const relative of [
    `src/main/resources/assets/csrp/geo/${id}.geo.json`,
    `src/main/resources/assets/csrp/animations/${id}.animation.json`,
    `src/main/resources/assets/csrp/textures/entity/${id}.png`,
    `src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`,
    `src/main/resources/assets/csrp/textures/item/${id}_spawn_egg.png`,
    `src/main/resources/data/csrp/loot_table/entities/${id}.json`
  ]) {
    read(relative);
  }
}

if (failures.length) {
  console.error("Nexus and aberrant entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Nexus and aberrant entity port verification passed (${ids.length} entities).`);
