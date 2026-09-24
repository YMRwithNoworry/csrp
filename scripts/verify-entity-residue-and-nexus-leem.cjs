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
// 2% chance every 20 ticks, stage 2 or 3, not for the Rooter family;
// refused while more than SRPConfig.nexusLeemCap (5) Rooters exist or the nearest one
// is closer than SRPConfig.nexusLeemDis (32); summons srparasites:rooter_si.
// ---------------------------------------------------------------------------
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
expect(nexus, /private static final int NEXUS_ROOTER_CAP = 5;/, "nexusLeemCap default 5 is missing");
expect(nexus, /private static final double NEXUS_ROOTER_DISTANCE = 32\.0D;/, "nexusLeemDis default 32 is missing");
expect(nexus, /private static final double NEXUS_ROOTER_CHANCE = 0\.02D;/, "the 2% Rooter roll is missing");
expect(nexus, /private static final int NEXUS_ROOTER_INTERVAL = 20;/, "the 20 tick Rooter cadence is missing");
expect(nexus, /private void trySpawnRooterSi\(ServerLevel serverLevel\)/, "trySpawnRooterSi is missing");
expect(nexus, /\(activeKind\.family == Family\.BECKON \|\| activeKind\.family == Family\.DISPATCHER\)/,
  "the Rooter summon is not restricted to the Dispatcher and Beckon families");
expect(nexus, /activeKind\.stage == 2 \|\| activeKind\.stage == 3/,
  "the Rooter summon is not restricted to stage 2 and 3");
expect(nexus, /rooterCount > NEXUS_ROOTER_CAP \|\| nearestDistanceSqr < NEXUS_ROOTER_DISTANCE \* NEXUS_ROOTER_DISTANCE/,
  "the Rooter cap / minimum distance rules are missing");
expect(nexus, /createNexus\(serverLevel, Family\.ROOTER, 1\)/, "the summon does not create a rooter_si");
expect(nexus, /playSound\(ModSounds\.get\("leem\.si"\), 4\.0F, 1\.0F\)/,
  "the LEEMSI cue (volume 4) is missing");
expect(nexus, /private int rooterSummonCooldown = NEXUS_ROOTER_INTERVAL;/,
  "the Rooter summon cadence field is missing");

if (failures.length) {
  console.error(`verify-entity-residue-and-nexus-leem: ${failures.length} failure(s)`);
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-entity-residue-and-nexus-leem: ok");
