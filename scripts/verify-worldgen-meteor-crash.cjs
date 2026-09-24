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

const crash = read("src/main/java/alku/csrp/world/gen/WorldGenParasiteMeteorCrash.java");
const util = read("src/main/java/alku/csrp/world/MeteorImpactUtil.java");
const facade = read("src/main/java/alku/csrp/world/MeteorCrashFeature.java");

// ---------------------------------------------------------------------------------------------
// WorldGenParasiteMeteorCrash
// ---------------------------------------------------------------------------------------------

expectPattern(crash, /class WorldGenParasiteMeteorCrash extends WorldGenParasiteColonyBase/,
  "WorldGenParasiteMeteorCrash does not extend the colony base");
expectPattern(crash, /public WorldGenParasiteMeteorCrash\(int stage\)/, "the meteor crash lost its stage constructor");
expectPattern(crash, /this\.type != 5/, "the meteor crash lost the root/fragment switch on type == 5");
expectPattern(crash, /BlockPos impactCenter = MeteorImpactUtil\.topSolidOrLiquid\(level, posss\)\.below\(\);/,
  "the impact centre is not getTopSolidOrLiquidBlock(pos).down()");
expectPattern(crash, /return generateFragment\(level, rand, impactCenter\);/, "the fragment path is missing");
expectPattern(crash, /return generateMainCrash\(level, rand, impactCenter\);/, "the root path is missing");

// Fragment meteor
for (const [index, name] of [
  "meteor_fragment_large1", "meteor_fragment_large2", "meteor_fragment_large3",
  "meteor_fragment_small1", "meteor_fragment_small2", "meteor_fragment_small3",
  "meteor_fragment_small4", "meteor_fragment_small5", "meteor_fragment_small6"
].entries()) {
  expectPattern(crash, new RegExp(`"${name}"`), `the fragment table lost ${name}`);
  exists(`src/main/resources/data/csrp/structure/${name}.nbt`);
  if (index > 0) {
    expectPattern(crash, new RegExp(`case ${index} -> out = FRAGMENTS\\[${index}\\];`),
      `the fragment roll lost case ${index} -> ${name}`);
  }
}
expectPattern(crash, /switch \(rand\.nextInt\(9\)\)/, "the fragment template roll is not nextInt(9)");
expectPattern(crash, /int fires = FIRE_COUNT_BASE \+ rand\.nextInt\(FIRE_COUNT_BASE\);/, "the fire count is not 18 + nextInt(18)");
expectPattern(crash, /private static final int FIRE_RADIUS = 10;/, "the fire radius is not 10");
expectPattern(crash, /if \(dx \* dx \+ dz \* dz > FIRE_RADIUS \* FIRE_RADIUS\) \{\s*continue;\s*\}/,
  "the fire scatter is not clamped to its radius");
expectPattern(crash, /if \(top\.getY\(\) <= 5\) \{\s*continue;\s*\}/, "the fire placement lost the y > 5 guard");
expectPattern(crash, /below\.isAir\(\) \|\| below\.getFluidState\(\)\.is\(Fluids\.WATER\)\s*\|\| below\.getFluidState\(\)\.is\(Fluids\.LAVA\)/,
  "the fire placement lost the air/water/lava material guard");
expectPattern(crash, /ParasiteGenContext\.setBlock\(level, firePos, Blocks\.FIRE\.defaultBlockState\(\)\);/,
  "the fragment meteor does not place fire through the flag-2 writer");
expectPattern(crash, /StructurePlacer\.place\(level, Identifier\.fromNamespaceAndPath\(Csrp\.MODID, out\), impactCenter\);/,
  "the fragment template is not placed at the impact centre in the csrp namespace");

// Root meteor
expectPattern(crash, /BlockPos enter = impactCenter\.below\(10\);/, "the root meteor lost the ten-block enter offset");
expectPattern(crash, /for \(int i = 0; i < 20; i\+\+\) \{\s*replaceCircleGround\(level, enter\.above\(i\), this\.type \* 7, ParasiteGenContext\.STAIN_RED\);/,
  "the root meteor ground discs are not 20 layers of type * 7 red stain");
expectPattern(crash, /BlockPos posss = impactCenter\.below\(rad \+ rad\);/, "the crater centre is not impactCentre.below(rad + rad)");
expectPattern(crash, /int minCenterY = rad \* 16 \+ 6;/, "the minimum crater centre y is not rad * 16 + 6");
expectPattern(crash,
  /generateSphere\(level, posss, rad \* 16, rad \* 16, rand, false, 1, false, 1, 1, 5,\s*ParasiteGenContext\.AIR, ParasiteGenContext\.AIR, ParasiteGenContext\.AIR, 2\);/,
  "the root meteor air carve is not generateSphere(rad*16, rad*16, ..., 1, false, 1, 1, 5, AIR, AIR, AIR, 2)");
expectPattern(crash, /float yaw = rand\.nextFloat\(\) \* 360\.0F;/, "the tunnel yaw is not nextFloat() * 360");
expectPattern(crash, /double dirX = -Mth\.sin\(yaw \* \(float\) \(Math\.PI \/ 180\.0D\)\);/, "dirX is not -sin(yaw)");
expectPattern(crash, /double dirZ = Mth\.cos\(yaw \* \(float\) \(Math\.PI \/ 180\.0D\)\);/, "dirZ is not cos(yaw)");
expectPattern(crash, /float steepness = 0\.25F \+ rand\.nextFloat\(\) \* 0\.75F;/, "steepness is not 0.25 + nextFloat() * 0.75");
expectPattern(crash, /double dirY = -steepness;/, "dirY is not -steepness");
expectPattern(crash, /MeteorImpactUtil\.carveAngledTunnel\(\s*level, tunnelStart, 10, 30, dirX, dirY, dirZ\);/,
  "the angled tunnel is not the radius-10 length-30 tunnel from craterSurface.below(3)");
expectPattern(crash, /int baseR = rad \* 8;/, "baseR is not rad * 8");
expectPattern(crash, /int baseDepth = \(int\) \(baseR \* \(0\.4F \+ rand\.nextFloat\(\) \* 0\.2F\)\);/, "baseDepth is not 0.4 + nextFloat() * 0.2");
expectPattern(crash, /adjustedDepth = openNeeded \+ 3;/, "the tunnel-driven depth is not openNeeded + 3");
expectPattern(crash, /int depthDrivenR = \(int\) \(adjustedDepth \* 1\.6F\);/, "the depth-driven radius is not adjustedDepth * 1.6");
expectPattern(crash, /int placeY = Math\.max\(bottomY \+ 1, 6\);/, "placeY is not max(bottomY + 1, 6)");
expectPattern(crash, /MeteorImpactUtil\.clearVegetationInArea\(level, craterSurface, adjustedR \* 2,\s*craterSurface\.getY\(\) - adjustedDepth - 12, craterSurface\.getY\(\) \+ 50\);/,
  "the vegetation clear box is not (adjustedR * 2, y - depth - 12, y + 50)");
for (const call of ["carveCraterBowl", "scorchRings", "spawnEjecta", "microCraters"]) {
  expectPattern(crash, new RegExp(`MeteorImpactUtil\\.${call}\\(level, rand, craterSurface, adjustedR`),
    `the root meteor does not run MeteorImpactUtil#${call}`);
}
expectPattern(crash, /int poolR = Math\.max\(4, adjustedR \/ 6\);/, "the dead blood pool radius is not max(4, adjustedR / 6)");
expectPattern(crash, /int skipR = 10;/, "the dead blood pool skip radius is not 10");
expectPattern(crash, /int poolHeight = 4;/, "the dead blood pool height is not 4");
expectPattern(crash, /ParasiteGenContext\.setBlock\(level, p, ParasiteGenContext\.DEAD_BLOOD\);/, "the pool is not filled with dead blood");
expectPattern(crash, /int half = 22;\s*int fix = 2;/, "the meteor template offsets are not half = 22 / fix = 2");
expectPattern(crash, /BlockPos meteorPos = structPos\.above\(14\)\.offset\(-half - fix, 0, -half - fix\);/,
  "the meteor satellite position is not structPos.above(14).offset(-24, 0, -24)");
expectPattern(crash, /StructurePlacer\.place\(level, Identifier\.fromNamespaceAndPath\(Csrp\.MODID, "meteor"\), meteorPos\);/,
  "the meteor satellite template is not csrp:meteor");
exists("src/main/resources/data/csrp/structure/meteor.nbt");
expectPattern(crash, /private static final int LOOT_BG_RANGE = 11;/, "the loot scan radius is not 11");
expectPattern(crash, /if \(block != Blocks\.IRON_BLOCK && block != Blocks\.GOLD_BLOCK\s*&& block != Blocks\.DIAMOND_BLOCK\)/,
  "the loot scan does not target iron/gold/diamond blocks");
expectPattern(crash, /int rollRare = 10;\s*int rollUncommon = 4;\s*if \(block == Blocks\.GOLD_BLOCK\) \{\s*rollRare = 7;\s*rollUncommon = 3;\s*\} else if \(block == Blocks\.DIAMOND_BLOCK\) \{\s*rollRare = 4;\s*rollUncommon = 2;/,
  "the per-block rare/uncommon rolls are not 10/4, 7/3, 4/2");
expectPattern(crash, /ParasiteGenContext\.placeLoot\(level, blockpos, LOOT_RARE, rand\);/, "the loot scan lost the rare tumor");
expectPattern(crash, /ParasiteGenContext\.placeLoot\(level, blockpos, LOOT_UNCOMMON, rand\);/, "the loot scan lost the uncommon tumor");
expectPattern(crash, /ParasiteGenContext\.placeLoot\(level, blockpos, LOOT_COMMON, rand\);/, "the loot scan lost the common tumor");
expectPattern(crash, /ParasiteGenContext\.setBlock\(level, blockpos\.above\(\), ParasiteGenContext\.DEAD_BLOOD\);/,
  "the loot scan does not mark the block above every tumor with dead blood");
expectPattern(crash, /ParasiteGenContext\.setBlock\(level, blockpos, ParasiteGenContext\.STAIN_FLESH\);/,
  "the glass branch does not write the flesh stain (the original's ParasiteStain meta 2)");
expectPattern(crash, /MeteorImpactUtil\.updateWaterAfterImpact\(level, craterSurface, adjustedR, adjustedDepth\);/,
  "the root meteor does not re-trigger the water updates after the impact");

// The original crash path never marked a "main meteor" — nothing read that set either.
expect(!/MeteorImpactUtil\.markMainMeteor\(/.test(crash), "the meteor crash calls markMainMeteor, which the original crash path never did");
expect(!/markMainMeteor/.test(facade), "the meteor facade calls markMainMeteor, which the original crash path never did");
expectPattern(util, /public static void markMainMeteor\(ServerLevel level, BlockPos center\)/,
  "markMainMeteor must stay available on the util even though nothing calls it");
expectPattern(util, /public static boolean isNearMainMeteor\(ServerLevel level, BlockPos pos, int minDist\)/,
  "isNearMainMeteor must stay available on the util even though nothing calls it");

// ---------------------------------------------------------------------------------------------
// The facade the projectile entities call
// ---------------------------------------------------------------------------------------------

expectPattern(facade, /public static void generate\(ServerLevel level, RandomSource random, BlockPos pos, int type\)/,
  "MeteorCrashFeature lost the signature MeteorEntity calls");
expectPattern(facade, /new WorldGenParasiteMeteorCrash\(type\)\.generate\(level, random, pos\);/,
  "MeteorCrashFeature does not delegate to the ported crash feature");
expect(!/carveAirSphere|carveCircleAir|replaceCircleGround|isGlassLike/.test(facade),
  "MeteorCrashFeature still carries a second, hand-rolled implementation of the crash");

// The entities must keep reaching the crash through the stable facade.
expectPattern(read("src/main/java/alku/csrp/entity/MeteorEntity.java"),
  /MeteorCrashFeature\.generate\(serverLevel, RandomSource\.create\(\), blockPosition\(\),/,
  "MeteorEntity no longer runs the meteor crash on impact");
expectPattern(read("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java"),
  /MeteorCrashFeature\.generate\(serverLevel, serverLevel\.getRandom\(\), hit, rootMeteor \? 5 : 1\);/,
  "ParasiteProjectileEntity no longer runs the meteor crash with the root/fragment type");

// The impact utility that the crash relies on stays complete.
for (const method of [
  "topSolidOrLiquid", "carveCraterBowl", "scorchRings", "spawnEjecta", "microCraters",
  "carveAngledTunnel", "clearVegetationInArea", "updateWaterAfterImpact", "tickPendingStructures",
  "scheduleDelayedStructure"
]) {
  expectPattern(util, new RegExp(`public static [A-Za-z<>, ]+ ${method}\\(`), `MeteorImpactUtil lost ${method}`);
}

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log("worldgen meteor crash: ok");
