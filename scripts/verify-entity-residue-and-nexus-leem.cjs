const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

// ---------------------------------------------------------------------------
// 1. EntityAIBlockResidue (out109 entity/ai/EntityAIBlockResidue.java, 84 lines)
//
// out109 facts:
//   canUse: no target && !isInWater() && getGeneMod(8)
//   counter starts at 160, decremented only on random.nextInt(5) == 0
//   counter == -1  -> setParasiteStatus(25) + navigation stop + ADAPTED_V sound
//   counter == -40 -> ADAPTED_V sound again
//   every tick     -> particleStatus((byte)13)
//   counter == -60 -> for x,z in [-range, range]: air above a solid, non-InfestedStain floor,
//                     random.nextInt(2) == 0 -> set InfestRemain (meta 1)
//   counter == -100 -> setParasiteStatus(0); counter = 200
//   mount ranges: EntityBanoAdapted 3, EntityCanra/Gim/Hull/Nogla/Ranrac/ShycoAdapted 2
// ---------------------------------------------------------------------------
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
expect(adapted, /private final class BlockResidueGoal extends Goal/, "BlockResidueGoal is missing");
expect(adapted, /private static final int START_COUNTER = 160;/, "the residue wind-up is not the original 160");
expect(adapted, /private static final int RESET_COUNTER = 200;/, "the residue reset is not the original 200");
expect(adapted, /private static final int STATUS_TICKS = 25;/, "the residue status length is not the original 25");
expect(adapted, /if \(random\.nextInt\(5\) == 0\) \{\s*counter--;\s*\}/,
  "the residue counter does not advance on the original 1-in-5 cadence");
expect(adapted, /counter == -1[\s\S]{0,200}?getNavigation\(\)\.stop\(\)[\s\S]{0,200}?playSound\(ModSounds\.get\("adapted\.v"\)/,
  "the residue wind-up completion does not stop navigation and play ADAPTED_V");
expect(adapted, /counter == -40[\s\S]{0,120}?playSound\(ModSounds\.get\("adapted\.v"\)/,
  "the second ADAPTED_V cue at -40 is missing");
expect(adapted, /counter == -60[\s\S]{0,120}?spreadResiduePatch\(blockResidueRange\(activeKind\(\)\)\)/,
  "the residue patch is not placed at -60 with the per-kind range");
expect(adapted, /counter == -100[\s\S]{0,120}?counter = RESET_COUNTER;/,
  "the residue loop does not reset at -100");
expect(adapted, /random\.nextInt\(2\) != 0/, "the per-column 1-in-2 residue roll is missing");
expect(adapted, /floor\.is\(ModBlocks\.INFESTED_STAIN\.get\(\)\)/, "InfestedStain floors are not excluded");
expect(adapted, /floor\.isSolidRender\(\)/, "non-solid floors are not excluded");
expect(adapted, /level\(\)\.setBlock\(candidate, ModBlocks\.INFESTED_REMAINS\.get\(\)\.defaultBlockState\(\), 3\)/,
  "the residue patch does not place infested remains");
expect(adapted, /case BOLSTER -> 3;/, "the Bolster residue range is not the original 3");
expect(adapted, /case ARACHNIDA, LONGARMS, MANDUCATER, REEKER, SUMMONER, VISCERA -> 2;/,
  "the remaining adapted residue range is not the original 2");
expect(adapted, /goalSelector\.addGoal\(9, new BlockResidueGoal\(\)\)/,
  "the residue goal is not mounted at the original priority 9");
expect(adapted, /getTarget\(\) == null && !isInWater\(\)/, "the residue goal conditions are not the original ones");

// ---------------------------------------------------------------------------
// 2. EntityAINexusGrow.spawnLeem (out109 entity/ai/EntityAINexusGrow.java:69-98)
//
// The counter resets at 20 and fires below zero. Stage II/III Beckons and Dispatchers
// use the captured creation progress, a 2% roll, configurable Rooter cap/distance,
// and up to two 12..20 block SummonM candidates.
// ---------------------------------------------------------------------------
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
const config = read("src/main/java/alku/csrp/Config.java");
expect(config, /"nexusLeemCap", 5, 0, 50/, "nexusLeemCap default or bounds are missing");
expect(config, /"nexusLeemDis", 32, 0, 50/, "nexusLeemDis default or bounds are missing");
expect(config, /\.define\("rsSky", false\)/, "the original sky requirement default is missing");
expect(config, /\.define\("rsSounds", true\)/, "the original reinforcement sound default is missing");
expect(nexus, /private static final double NEXUS_ROOTER_CHANCE = 0\.02D;/, "the 2% Rooter roll is missing");
expect(nexus, /private static final int NEXUS_ROOTER_INTERVAL = 20;/, "the Rooter counter reset is missing");
expect(nexus, /private void trySpawnRooterSi\(ServerLevel serverLevel\)/, "trySpawnRooterSi is missing");
expect(nexus, /!isNoAi\(\) && \(activeKind\.family == Family\.BECKON \|\| activeKind\.family == Family\.DISPATCHER\)/,
  "the Rooter summon is not restricted to active Dispatcher and Beckon AI");
expect(nexus, /activeKind\.stage == 2 \|\| activeKind\.stage == 3/,
  "the Rooter summon is not restricted to stage 2 and 3");
expect(nexus, /--rooterSummonCooldown < 0/, "the summon counter does not fire below zero");
expect(nexus, /\+\+rooterCount > Config\.nexusRooterCap\(\)/,
  "the strict Rooter cap rule is missing");
expect(nexus, /distanceToSqr\(other\) < minimumDistance \* minimumDistance/,
  "the strict minimum distance rule is missing");
expect(nexus, /attempt < 2[\s\S]*?12 \+ summonRandom\.nextInt\(9\)[\s\S]*?12 \+ summonRandom\.nextInt\(9\)/,
  "the two 12..20 block candidate attempts are missing");
expect(nexus, /createNexus\(serverLevel, Family\.ROOTER, 1\)/, "the summon does not create a rooter_si");
expect(nexus, /serverLevel\.noCollision\(rooter\)[\s\S]*?getEntities\(rooter, rooter\.getBoundingBox\(\)\.inflate\(0\.25D\), Entity::isAlive\)/,
  "the original block and entity collision checks are missing");
expect(nexus, /Config\.reinforcementSky\(\) && !hasDirectSky\(serverLevel/,
  "the direct-sky requirement is missing");
expect(nexus, /Config\.reinforcementSounds\(\)[\s\S]*?activeValue\(SrpWorldData\.get\(serverLevel\), 15\) == 0[\s\S]*?playSound\(ModSounds\.get\("leem\.si"\), 4\.0F, 1\.0F\)/,
  "the LEEMSI cue or its dislodgment and config gates are missing");
expect(nexus, /serverLevel\.addFreshEntity\(rooter\);\s*return true;/,
  "a canceled join must still terminate the summon attempt successfully");

if (failures.length) {
  console.error(`verify-entity-residue-and-nexus-leem: ${failures.length} failure(s)`);
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-entity-residue-and-nexus-leem: ok");
