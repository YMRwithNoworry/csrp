const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
};
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};
const expectPattern = (source, pattern, message) => expect(pattern.test(source), message);
const exists = (relative) => expect(fs.existsSync(path.join(root, relative)), `missing ${relative}`);

// ---------------------------------------------------------------------------------------------
// WorldGenParasiteColonyCore
// ---------------------------------------------------------------------------------------------

const core = read("src/main/java/alku/csrp/world/gen/WorldGenParasiteColonyCore.java");

expectPattern(core, /class WorldGenParasiteColonyCore extends WorldGenParasiteColonyBase/,
  "WorldGenParasiteColonyCore does not extend the colony base");
expectPattern(core, /public WorldGenParasiteColonyCore\(int stage\)/, "the colony core lost its stage constructor");
for (let i = 1; i <= 3; i++) {
  const offset = i === 1 ? "below()" : `below(${i})`;
  expectPattern(core, new RegExp(`replaceCircleGround\\(level, posss\\.${offset.replace(/[()]/g, "\\$&")}, 12, ParasiteGenContext\\.STAIN_RED\\)`),
    `the colony core lost red ground disc #${i} (radius 12)`);
  expectPattern(core, new RegExp(`replaceCircleGround\\(level, posss\\.${offset.replace(/[()]/g, "\\$&")}, 12`),
    `the colony core lost ground disc #${i}`);
}
expectPattern(core, /posss = posss\.below\(7\);/, "the colony core lost the seven-block tower offset");
expectPattern(core, /int height = 12 \+ rand\.nextInt\(3\);/, "the colony core tower height is not 12 + nextInt(3)");
expectPattern(core, /int tic = 6;/, "the colony core lost the tic = 6 inner radius offset");
expectPattern(core, /xx = Math\.min\(8, xx \+ 2\);/, "the colony core tower clamp is not min(8)");
expectPattern(core, /xx = Math\.max\(5, xx - 1\);/, "the colony core tower clamp is not max(5)");
expectPattern(core, /level\.getRandom\(\)\.nextInt\(3\) == 0/, "the colony core lost the 1/3 tower widening roll");
expectPattern(core, /generateCircle\(ParasiteGenContext\.RUBBLE_FLESH, ParasiteGenContext\.STAIN_FLESH, level,\s*level\.getRandom\(\), posss, xx, zz, 1, 20000, 6\);/,
  "the colony core tower shell is not flesh rubble / flesh stain");
expectPattern(core, /generateCircle\(ParasiteGenContext\.STAIN_FLESH, ParasiteGenContext\.BONE_BLOCK, level,\s*level\.getRandom\(\), posss, hx, hz, 1, 20000, 6\);/,
  "the colony core tower inner disc is not flesh stain / bone block");
expectPattern(core, /int bonusH = rand\.nextInt\(5\);/, "the colony core lost the nextInt(5) height bonus");
expectPattern(core, /height = 10 \+ rand\.nextInt\(5\);/, "the colony core helix height is not 10 + nextInt(5)");
expectPattern(core, /int kil = 3;\s*int sec = 2;\s*double spa = height \/ sec;/, "the colony core lost the integer-divided helix pitch");
for (const material of ["STAIN_FLESH", "STAIN_SACKFLESH", "RUBBLE_BONE", "BONE_BLOCK", "STAIN_FEELER"]) {
  expectPattern(core, new RegExp(`generateDNAHelix\\(ParasiteGenContext\\.${material}, level, level\\.getRandom\\(\\), posss`),
    `the colony core lost the ${material} DNA helix`);
}
expectPattern(core, /generateDNAHelix\(ParasiteGenContext\.BONE_BLOCK, level, level\.getRandom\(\), posss\.above\(3\),\s*--kil, sec, spa\);/,
  "the colony core lost the --kil decrement in the helix argument list");
expectPattern(core, /generateDNAHelix\(ParasiteGenContext\.STAIN_FEELER, level, level\.getRandom\(\), posss, kil, var79, spa\);/,
  "the colony core lost the feeler helix at the doubled radius");
expectPattern(core, /generateSphere\(level, posss, 8, 6, rand, false, 6, false, 2, 1, 5,\s*ParasiteGenContext\.RUBBLE_FLESH, ParasiteGenContext\.STAIN_FLESH,\s*ParasiteGenContext\.AIR, missing\);/,
  "the colony core first pod is not the 8/6 flesh pod");
for (const radius of [7, 13]) {
  expectPattern(core, new RegExp(`var80 = ${radius};\\s*theta = rand\\.nextDouble\\(\\) \\* 2\\.0D \\* Math\\.PI;\\s*posss = getCirclePoint\\(posss, var80, theta\\);`),
    `the colony core lost the radius-${radius} pod anchor`);
}
for (const count of [7, 27, 10, 8]) {
  expectPattern(core, new RegExp(`var40 = ${count};`), `the colony core lost the ${count}-step pod loop`);
}
expectPattern(core, /generateSphere\(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,\s*ParasiteGenContext\.DENSE_WALL, ParasiteGenContext\.RUBBLE_BRICKS,\s*ParasiteGenContext\.AIR, missing\);/,
  "the colony core satellite pods are not dense wall / bricks");
expectPattern(core, /posss = posss\.above\(10\);\s*if \(rand\.nextBoolean\(\)\) \{\s*placeCore\(level, enter, 1\);\s*return true;\s*\}/,
  "the colony core lost the final coin flip at the entrance");
expectPattern(core, /BlockState heart = ModBlocks\.COLONYHEART\.get\(\)\.defaultBlockState\(\)\s*\.setValue\(SrpCoreBlock\.ACTIVE, stage\);/,
  "placeCore does not write the active colony heart");
expectPattern(core, /placeBlock\(level, pos, heart\);/, "placeCore does not place the heart at the entrance position");

// ---------------------------------------------------------------------------------------------
// The three nexus protections
// ---------------------------------------------------------------------------------------------

const nexus1 = read("src/main/java/alku/csrp/world/gen/WorldGenParasiteNexusProtection1.java");
const nexus2 = read("src/main/java/alku/csrp/world/gen/WorldGenParasiteNexusProtection2.java");
const nexus3 = read("src/main/java/alku/csrp/world/gen/WorldGenParasiteNexusProtection3.java");

for (const [name, source] of [
  ["WorldGenParasiteNexusProtection1", nexus1],
  ["WorldGenParasiteNexusProtection2", nexus2],
  ["WorldGenParasiteNexusProtection3", nexus3]
]) {
  expectPattern(source, new RegExp(`class ${name} extends WorldGenParasiteColonyBase`),
    `${name} does not extend the colony base`);
  expectPattern(source, /public boolean generate\(ServerLevel level, RandomSource rand, BlockPos posss\)/,
    `${name} does not expose generate(ServerLevel, RandomSource, BlockPos)`);
  expectPattern(source, /this\.wall = ParasiteGenContext\.DENSE_WALL;/,
    `${name} does not default its wall to the dense rubble wall`);
  expectPattern(source, /this\.tacle = ParasiteGenContext\.STAIN_FEELER;/,
    `${name} does not default its tacle to the feeler stain`);
}

expectPattern(nexus1, /generateSphere\(level, posss, 3, 3, rand, false, 6, false, 1, 1, 5,\s*ParasiteGenContext\.STAIN_FLESH, ParasiteGenContext\.STAIN_FEELER,\s*ParasiteGenContext\.FOG, 2\);/,
  "nexus protection 1 lost its 3/3/6/1/1/5 flesh-feeler-fog sphere with the incomplete roll of 2");
expectPattern(nexus1, /generateCircle\(ParasiteGenContext\.STAIN_FEELER, ParasiteGenContext\.STAIN_FLESH, level, rand,\s*posss\.below\(5\), 6, 6, 5, 2, 0\);/,
  "nexus protection 1 lost its 6x6x5 feeler/flesh inner disc");
expectPattern(nexus1, /generateCircle\(ParasiteGenContext\.FOG, ParasiteGenContext\.FOG, level, rand,\s*posss\.below\(5\), 4, 4, 5, 20_000_000, 0\);/,
  "nexus protection 1 lost its 4x4x5 fog inner disc");
expectPattern(nexus1, /replaceCircleGround\(level, posss, 8, ParasiteGenContext\.STAIN_FEELER\);/,
  "nexus protection 1 lost its radius-8 felt ground disc");

expectPattern(nexus2, /generateRandomPillar\(level, posss\.below\(1\), 6, 5, pillarBlock\);/, "nexus protection 2 lost its radius-6 pillar");
expectPattern(nexus2, /generateRandomPillar\(level, posss\.below\(1\), 9, 9, pillarBlock\);/, "nexus protection 2 lost its radius-9 pillar");
expectPattern(nexus2, /private static final int FLOOR_RANGE = 7;/, "nexus protection 2 floor scan is not the original range of seven");
expectPattern(nexus2, /basePos = ParasiteGenContext\.floor\(level, basePos, FLOOR_RANGE\);/, "nexus protection 2 does not scan for the floor");
expectPattern(nexus2, /basePos = basePos\.below\(\);/, "nexus protection 2 does not step under the floor before placing");
expectPattern(nexus2, /int outt = level\.getRandom\(\)\.nextInt\(4\) \+ 2;/, "nexus protection 2 beckon size is not 2..5");
expectPattern(nexus2, /case 2 -> out = out \+ "_1";/, "nexus protection 2 lost beckon_2x2_1");
expectPattern(nexus2, /case 3 -> out = out \+ "_" \+ \(level\.getRandom\(\)\.nextInt\(5\) \+ 1\);/, "nexus protection 2 lost the 3x3 suffix table");
expectPattern(nexus2, /case 4 -> out = out \+ "_" \+ \(level\.getRandom\(\)\.nextInt\(2\) \+ 1\);/, "nexus protection 2 lost the 4x4 suffix table");
expectPattern(nexus2, /default -> out = out \+ "_1";/, "nexus protection 2 lost beckon_5x5_1");
expectPattern(nexus2, /StructurePlacer\.place\(level, Identifier\.fromNamespaceAndPath\(Csrp\.MODID, out\), basePos,\s*level\.getRandom\(\)\);/,
  "nexus protection 2 does not place the chosen template in the csrp namespace");
expectPattern(nexus2, /double theta = level\.getRandom\(\)\.nextDouble\(\) \* 2\.0D \* Math\.PI;/,
  "nexus protection 2 lost the random circle angle");

// Every template the original could name must still exist under data/csrp/structure.
for (const template of [
  "beckon_2x2_1", "beckon_3x3_1", "beckon_3x3_2", "beckon_3x3_3", "beckon_3x3_4", "beckon_3x3_5",
  "beckon_4x4_1", "beckon_4x4_2", "beckon_5x5_1"
]) {
  exists(`src/main/resources/data/csrp/structure/${template}.nbt`);
}

expectPattern(nexus3, /private static final int RING_STEPS = 64;/, "nexus protection 3 ring is not 64 steps");
expectPattern(nexus3, /int radius = 8;/, "nexus protection 3 ring radius is not 8");
expectPattern(nexus3, /BlockState pillarBlock = ParasiteGenContext\.RUBBLE_FUNGUS;/, "nexus protection 3 pillars are not fungus rubble");
expectPattern(nexus3, /int min = 1;\s*int max = 5;\s*generatePillar\(level, pos, level\.getRandom\(\)\.nextInt\(max - min \+ 1\) \+ min, pillarBlock,\s*pillarBlock2\);/,
  "nexus protection 3 ring pillars are not 1..5 high fungus/feeler");
expectPattern(nexus3, /while \(radius > 0\) \{\s*for \(BlockPos pos : getCirclePoints\(posss, --radius, steps\)\) \{\s*int min = 7;\s*int max = 14;/,
  "nexus protection 3 lost the pre-decrement fog ring walk (radii 7..0)");
expectPattern(nexus3, /generatePillar\(level, pos, level\.getRandom\(\)\.nextInt\(max - min \+ 1\) \+ min, pillarBlock,\s*pillarBlock\);/,
  "nexus protection 3 fog pillars are not 7..14 high");
expectPattern(nexus3, /generateCircle\(ParasiteGenContext\.STAIN_FEELER, ParasiteGenContext\.STAIN_FLESH, level, rand,\s*posss\.below\(5\), 8, 8, 5, 2, 0\);/,
  "nexus protection 3 lost its 8x8x5 feeler/flesh disc");
expectPattern(nexus3, /generateCircle\(ParasiteGenContext\.FOG, ParasiteGenContext\.FOG, level, rand,\s*posss\.below\(5\), 4, 4, 5, 20_000_000, 0\);/,
  "nexus protection 3 lost its 4x4x5 fog disc");
expectPattern(nexus3, /replaceCircleGround\(level, posss\.below\(\), 8, pillarBlock2\);/,
  "nexus protection 3 lost its radius-8 felt ground disc");

// ---------------------------------------------------------------------------------------------
// Reachability: the core must actually run from the colony system
// ---------------------------------------------------------------------------------------------

const generator = read("src/main/java/alku/csrp/world/ColonyStructureGenerator.java");
expectPattern(generator, /new WorldGenParasiteColonyCore\(1\)\.generate\(level, random, foundation\);/,
  "ColonyStructureGenerator#generateCore does not run the ported colony core");
expectPattern(generator, /return foundation;/, "generateCore does not report the heart position the core writes");
expectPattern(read("src/main/java/alku/csrp/world/SrpCoreSystems.java"), /ColonyStructureGenerator\.generateCore\(level, foundation, level\.getRandom\(\)\)/,
  "placeColony no longer calls generateCore");

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log("worldgen nexus protection and colony core: ok");
