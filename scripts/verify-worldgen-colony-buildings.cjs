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

const dir = "src/main/java/alku/csrp/world/gen/";
const bodies = {};
for (const name of ["B1", "B2", "B3", "B4", "BS1", "BS2", "BS3", "BS4"]) {
  bodies[name] = read(`${dir}WorldGenParasiteColony${name}.java`);
}

// ---------------------------------------------------------------------------------------------
// Every building: shape of the class and the shared ground preparation
// ---------------------------------------------------------------------------------------------

for (const [name, source] of Object.entries(bodies)) {
  expectPattern(source, new RegExp(`class WorldGenParasiteColony${name} extends WorldGenParasiteColonyBase`),
    `WorldGenParasiteColony${name} does not extend the colony base`);
  expectPattern(source, new RegExp(`public WorldGenParasiteColony${name}\\(int stage\\)`),
    `WorldGenParasiteColony${name} lost its stage constructor`);
  expectPattern(source, /public boolean generate\(ServerLevel level, RandomSource rand, BlockPos posss\)/,
    `WorldGenParasiteColony${name} does not expose generate(ServerLevel, RandomSource, BlockPos)`);
  expectPattern(source, /replaceCircleGround\(level, posss\.below\(\), 12, ParasiteGenContext\.STAIN_RED\);/,
    `WorldGenParasiteColony${name} lost its radius-12 red ground disc`);
}

// B1..B4 use the 12/12/12 disc triple, BS1..BS4 the 12/8/8 one
for (const name of ["B1", "B2", "B3", "B4"]) {
  expectPattern(bodies[name], /replaceCircleGround\(level, posss\.below\(2\), 12, ParasiteGenContext\.STAIN_RED\);\s*replaceCircleGround\(level, posss\.below\(3\), 12, ParasiteGenContext\.STAIN_RED\);/,
    `WorldGenParasiteColony${name} does not use the 12/12/12 ground disc triple`);
}
for (const name of ["BS1", "BS2", "BS3", "BS4"]) {
  expectPattern(bodies[name], /replaceCircleGround\(level, posss\.below\(2\), 8, ParasiteGenContext\.STAIN_RED\);\s*replaceCircleGround\(level, posss\.below\(3\), 8, ParasiteGenContext\.STAIN_RED\);/,
    `WorldGenParasiteColony${name} does not use the 12/8/8 ground disc triple`);
}

// ---------------------------------------------------------------------------------------------
// Per-building body
// ---------------------------------------------------------------------------------------------

expectPattern(bodies.B1, /int height = 22 \+ rand\.nextInt\(3\);/, "B1 tower height is not 22 + nextInt(3)");
expectPattern(bodies.B1, /xx = Math\.min\(9, xx \+ 2\);/, "B1 lost the min(9) widening clamp");
expectPattern(bodies.B1, /xx = Math\.max\(3, xx - 1\);/, "B1 lost the max(3) narrowing clamp");
expectPattern(bodies.B1, /int tic = 2;/, "B1 lost the tic = 2 inner disc offset");
expectPattern(bodies.B1, /posss = posss\.above\(18 \+ bonusH - zz \/ 2 \* 2\);/, "B1 lost the pod height formula");
expectPattern(bodies.B1, /generateSphere\(level, posss, zz \+ 1, 2, rand, false, 1, false, 1, 1, 5,\s*ParasiteGenContext\.RUBBLE_FLESH, ParasiteGenContext\.STAIN_FLESH,/,
  "B1 pod is not the zz+1 / 2 flesh pod");
for (const offset of [4, 3, 2]) {
  expectPattern(bodies.B1, new RegExp(`generateDNAHelix\\(ParasiteGenContext\\.(BONE_BLOCK|STAIN_FLESH), level, level\\.getRandom\\(\\), he\\.below\\(${offset}\\), aa - 2, 2,\\s*11 \\+ bonusH\\);`),
    `B1 lost the DNA helix at he.below(${offset})`);
}

expectPattern(bodies.B2, /int height = 22 \+ rand\.nextInt\(10\);/, "B2 helix height is not 22 + nextInt(10)");
expectPattern(bodies.B2, /int kil = 3;\s*int sec = 2;\s*double spa = height \/ sec;/, "B2 lost the integer-divided helix pitch");
expectPattern(bodies.B2, /--kil, sec, spa\);/, "B2 lost the --kil bone-block strand");
expectPattern(bodies.B2, /kil \+= 2;\s*int var14 = 2;/, "B2 lost the doubled feeler strand radius");
expectPattern(bodies.B2, /generateSphere\(level, posss, 4, 3, rand, rand\.nextInt\(20\) == 0, 4, true, 1, 3, 2,\s*ParasiteGenContext\.STAIN_FLESH, ParasiteGenContext\.STAIN_FEELER,/,
  "B2 pod is not the 4/3 sphere with the 1/20 inverted tip and the random growth flag");
expectPattern(bodies.B2, /if \(rand\.nextBoolean\(\)\) \{\s*return true;\s*\}/, "B2 lost the coin flip before its pod");

expectPattern(bodies.B3, /generateSphere\(level, posss, 4, 3, rand, false, 6, false, 2, 1, 5,/, "B3 base pod is not the 4/3/6 sphere");
expectPattern(bodies.B3, /posss = posss\.above\(12\);/, "B3 lost the twelve-block tower offset");
expectPattern(bodies.B3, /int height = 20;/, "B3 tower height is not 20");
expectPattern(bodies.B3, /xx = Math\.min\(3, xx \+ 1\);/, "B3 lost the min(3) clamp");
expectPattern(bodies.B3, /posss = posss\.above\(10\);/, "B3 lost the ten-block offset before the coin flip");
expectPattern(bodies.B3, /addEntrance\(level, rand, enter, 5\);/, "B3 lost the five-step entrance");
expectPattern(bodies.B3, /int var25 = 15;/, "B3 second tower height is not 15");
expectPattern(bodies.B3, /xx = Math\.min\(2, xx \+ 1\);/, "B3 second tower lost the min(2) clamp");

expectPattern(bodies.B4, /generateSphere\(level, posss, 4, 3, rand, false, 7, false, 3, 1, 5,/, "B4 base pod is not the 4/3/7 sphere");
expectPattern(bodies.B4, /posss = posss\.above\(16\);/, "B4 lost the sixteen-block pod offsets");
expectPattern(bodies.B4, /int radius = 6;/, "B4 lost the radius-6 pod jump");
expectPattern(bodies.B4, /generateSphere\(level, posss, 5, 3, rand, false, 3, false, 2, 1, 5,/, "B4 second pod is not the 5/3/3 sphere");
expectPattern(bodies.B4, /int var13 = 3;/, "B4 lost the radius-3 final pod");
expectPattern(bodies.B4, /generateSphere\(level, posss, 3, 3, rand, false, 3, false, 2, 2, 5,/, "B4 final pod is not the 3/3/3 sphere with heightAbove 2");

expectPattern(bodies.BS1, /int radius = 4;/, "BS1 lost the radius-4 tower");
expectPattern(bodies.BS1, /int height = 8;/, "BS1 tower height is not 8");
expectPattern(bodies.BS1, /xx = Math\.min\(3, xx \+ 1\);/, "BS1 lost the min(3) clamp");
expectPattern(bodies.BS1, /posss = posss\.above\(10\);/, "BS1 lost the ten-block offset before the coin flip");
expectPattern(bodies.BS1, /int var24 = 7;/, "BS1 second tower height is not 7");
expectPattern(bodies.BS1, /return true;\s*\}\s*\s*int var22 = 4;/, "BS1 lost the coin-flip early return");

expectPattern(bodies.BS2, /tower\(level, rand, posss, 7, 28, 1, 2, 1, 2, missing\);/, "BS2 lost the 28-high radius-7 tower");
expectPattern(bodies.BS2, /tower\(level, rand, enter, 7, 47, 1, 2, 1, 1, missing\);/, "BS2 lost the 47-high radius-7 tower");
expectPattern(bodies.BS2, /tower\(level, rand, enter, 2, 17, 1, 2, 1, 1, missing\);/, "BS2 lost the 17-high radius-2 tower");
expectPattern(bodies.BS2, /generateCircle\(ParasiteGenContext\.RUBBLE_BONE, ParasiteGenContext\.STAIN_FLESH, level,\s*level\.getRandom\(\), posss, xx, zz, 1, 20000, 6\);/,
  "BS2 tower shell is not bone rubble / flesh stain");
expectPattern(bodies.BS2, /generateSphere\(level, posss, 3, 3, rand, false, 3, false, 2, 1, 5,/, "BS2 tower cap is not the 3/3/3 pod");

expectPattern(bodies.BS3, /int radius = 3;/, "BS3 lost the radius-3 first bulge");
expectPattern(bodies.BS3, /generateSphere\(level, posss, 2, 10, rand, false, 3, false, 2, 1, 2,/, "BS3 first bulge is not the 2/10/3 sphere");
expectPattern(bodies.BS3, /int var12 = 6;/, "BS3 lost the radius-6 second bulge");
expectPattern(bodies.BS3, /generateSphere\(level, posss, 2, 10, rand, false, 3, false, 4, 2, 4,/, "BS3 second bulge is not the 2/10/3 sphere with the 4/2/4 tail");

expectPattern(bodies.BS4, /int height = 22 \+ rand\.nextInt\(3\);/, "BS4 tower height is not 22 + nextInt(3)");
expectPattern(bodies.BS4, /xx = Math\.min\(9, xx \+ 2\);/, "BS4 lost the min(9) widening clamp");
expectPattern(bodies.BS4, /generateSphere\(level, posss, zz \+ 1, 2, rand, false, 1, false, 1, 1, 5,/, "BS4 pod is not the zz+1 / 2 flesh pod");
expectPattern(bodies.BS4, /generateDNAHelix\(ParasiteGenContext\.BONE_BLOCK, level, level\.getRandom\(\), he\.below\(3\), aa - 2, 2,\s*11 \+ bonusH\);/,
  "BS4 lost its single bone-block helix at he.below(3)");

// BS1's two private wall helpers were never called in 1.10.9; the port must not silently grow them.
expect(!/private BlockPos placeWalls(Bottom|TopIn)/.test(bodies.BS1),
  "BS1 grew the dead placeWalls helpers instead of documenting them as unreachable");

// ---------------------------------------------------------------------------------------------
// The dispatcher
// ---------------------------------------------------------------------------------------------

const generator = read("src/main/java/alku/csrp/world/ColonyStructureGenerator.java");

expectPattern(generator, /public static boolean generateBuilding\(ServerLevel level, BlockPos origin, int stage, RandomSource random\)/,
  "generateBuilding lost its ColonyStructureBlock signature");
expectPattern(generator,
  /case 1 -> \{\s*switch \(random\.nextInt\(3\)\) \{\s*case 1 -> building = new WorldGenParasiteColonyB3\(2\);\s*case 2 -> building = new WorldGenParasiteColonyB2\(2\);\s*case 3 -> building = new WorldGenParasiteColonyB4\(2\);\s*default -> building = new WorldGenParasiteColonyB1\(2\);/,
  "the stage-1 table is not the original nextInt(3) B3/B2/B4/B1 switch");
expectPattern(generator,
  /case 2 -> \{\s*switch \(random\.nextInt\(3\)\) \{\s*case 1 -> building = new WorldGenParasiteColonyBS1\(2\);\s*case 2 -> building = new WorldGenParasiteColonyBS3\(2\);\s*default -> building = new WorldGenParasiteColonyBS2\(2\);/,
  "the stage-2 table is not the original nextInt(3) BS1/BS3/BS2 switch");
expectPattern(generator, /default -> \{\s*return false;\s*\}/, "an unknown stage does not report failure");
expectPattern(generator, /building\.generate\(level, random, origin\);/, "the dispatcher does not run the selected building");

for (const name of ["B1", "B2", "B3", "B4", "BS1", "BS2", "BS3", "BS4"]) {
  expectPattern(generator, new RegExp(`case ${name} -> new WorldGenParasiteColony${name}\\(stage\\);`),
    `ColonyBuilding.${name} is not reachable through ColonyBuilding#create`);
}
expectPattern(generator, /B1, B2, B3, B4, BS1, BS2, BS3, BS4;/,
  "the ColonyBuilding enum must list exactly the eight legacy buildings");
expectPattern(generator, /public static void place\(ServerLevel level, BlockPos origin, ColonyBuilding building, int stage,\s*RandomSource random\)/,
  "the explicit building entry point is missing");

// The building dispatch must stay reachable from the block that used to run it.
expectPattern(read("src/main/java/alku/csrp/block/ColonyStructureBlock.java"),
  /ColonyStructureGenerator\.generateBuilding\(level, pos, stage, random\)/,
  "ColonyStructureBlock no longer runs the colony building dispatcher");

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log("worldgen colony buildings: ok");
