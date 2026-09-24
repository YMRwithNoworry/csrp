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

// ---------------------------------------------------------------------------------------------
// WorldGenParasiteColonyBase — the shared shell vocabulary of the colony family
// ---------------------------------------------------------------------------------------------

const base = read("src/main/java/alku/csrp/world/gen/WorldGenParasiteColonyBase.java");

expectPattern(base, /class WorldGenParasiteColonyBase/, "WorldGenParasiteColonyBase is missing");
expectPattern(base, /public abstract boolean generate\(ServerLevel level, RandomSource random, BlockPos pos\)/,
  "the base does not expose the func_180709_b entry point");
expectPattern(base, /protected int type;/, "the original stage field is missing");
expectPattern(base, /floor = ParasiteGenContext\.STAIN_DIRT/, "floor default is not the dirt stain");
expectPattern(base, /tacle = ParasiteGenContext\.STAIN_FEELER/, "tacle default is not the feeler stain");
expectPattern(base, /wall = ParasiteGenContext\.DENSE_WALL/, "wall default is not the dense rubble wall");
expectPattern(base, /floorColony = ParasiteGenContext\.RUBBLE_FLESH/, "floorColony default is not flesh rubble");

// getDirectionRoot: 0 north, 1 south, 2 west, 3 east (func_177964_d/177965_g/177970_e/177985_f)
expectPattern(base,
  /getDirectionRoot\(BlockPos center, int direction, int times\)[\s\S]{0,220}case 0 -> center\.north\(times\);[\s\S]{0,120}case 1 -> center\.south\(times\);[\s\S]{0,120}case 2 -> center\.west\(times\);[\s\S]{0,120}default -> center\.east\(times\);/,
  "getDirectionRoot does not use the original north/south/west/east ordering");

// directionToGrow: 0 north, 1 east, 2 south (default), 3 west
expectPattern(base,
  /directionToGrow\(BlockPos atm, int choice, boolean sideCurse\)[\s\S]{0,420}case 0 -> atm\.north\(\);[\s\S]{0,120}case 1 -> atm\.east\(\);[\s\S]{0,120}case 3 -> atm\.west\(\);[\s\S]{0,120}default -> atm\.south\(\);/,
  "directionToGrow does not use the original east/south/west ordering");

// placeColumn walks up on extraChance == 1.0 and down otherwise, placing one block past the end
expectPattern(base, /if \(extraChance == 1\.0D\) \{\s*newPos = newPos\.above\(\);\s*\} else \{\s*newPos = newPos\.below\(\);\s*\}/,
  "placeColumn does not honour the original extraChance direction switch");
expectPattern(base, /while \(current < atm \+ in\) \{[\s\S]{0,320}placeBlock\(level, newPos, state\);\s*return newPos;/,
  "placeColumn does not place the final block past the end");

// generateCircle: the exact original gate and the 1/60 rim roll with the 1/4 blood and the loot tiers
expectPattern(base,
  /if \(!\(existing\.isAir\(\) \|\| ParasiteGenContext\.isModBlock\(existing\) \|\| flagAir\)\s*\|\| rand\.nextInt\(incomplete\) == 0\) \{\s*continue;\s*\}/,
  "generateCircle lost the air-or-mod-block gate or the incomplete roll");
expectPattern(base, /rim && rand\.nextInt\(60\) == 0/, "generateCircle lost the 1/60 rim roll");
expectPattern(base, /if \(rand\.nextInt\(4\) == 0\) \{[\s\S]{0,200}ParasiteGenContext\.DEAD_BLOOD/,
  "generateCircle lost the 1/4 dead blood rim outcome");
expectPattern(base, /rand\.nextInt\(10\) == 0\) \{\s*ParasiteGenContext\.placeLoot\(level, target, LOOT_RARE, rand\);/,
  "generateCircle lost the 1/10 rare loot roll");
expectPattern(base, /rand\.nextInt\(4\) == 0\) \{\s*ParasiteGenContext\.placeLoot\(level, target, LOOT_UNCOMMON, rand\);/,
  "generateCircle lost the 1/4 uncommon loot roll");
expectPattern(base, /ParasiteGenContext\.placeLoot\(level, target, LOOT_COMMON, rand\);/, "generateCircle lost the common loot fill");
expectPattern(base, /boolean rim = x == radiusX \|\| z == radiusZ \|\| x == -radiusX \|\| z == -radiusZ[\s\S]{0,200}x - 1 == -radiusX \|\| z - 1 == -radiusZ;/,
  "the rim band of generateCircle is not the original two-block-thick border");
expectPattern(base, /if \(pos\.getY\(\) <= 2 \|\| pos\.getY\(\) >= 240\) \{\s*return false;/, "generateCircle lost the y keep-out band");

// generateSphere: inner taper, outer crown and tip section
for (const call of [
  /generateCircle\(state1, state2, level, rand, posss, xx, zz, 1, incomplete, 6\)/,
  /generateCircle\(state3, state3, level, rand, posss, xx - ticc, zz - ticc, 1, 50000, 6\)/,
  /generateCircle\(state1, state2, level, rand, posss, xx, zz, 1, incomplete, 0\)/,
  /generateCircle\(state3, state3, level, rand, posss, xx - 2, zz - 2, 1, 50000, 0\)/,
  /incomplete, invertedTip \? 9 : 0\)/
]) {
  expectPattern(base, call, `generateSphere is missing a circle call: ${call}`);
}
expectPattern(base, /if \(rand\.nextBoolean\(\) && random\) \{\s*xx \+= 2;/, "generateSphere lost the inner xx growth roll");
expectPattern(base, /if \(rand\.nextBoolean\(\) && random\) \{\s*zz \+= 2;/, "generateSphere lost the inner zz growth roll");
expectPattern(base, /if \(invertedTip\) \{\s*heightAbove = \(int\) \(heightAbove \* 0\.5D\);/, "generateSphere lost the inverted-tip halving");
expectPattern(base, /int ticc = 2;/, "generateSphere lost the ticc offset of two");
expectPattern(base, /generateCircle\(state3, state3, level, rand, posss, 1, 50000, 6\)|50000/, "generateSphere lost the 1/50000 inner roll");

// DNA helix and the entrance helper
expectPattern(base, /double tStep = 0\.1D;/, "generateDNAHelix lost the 0.1 angle step");
expectPattern(base, /int x2 = pos\.getX\(\) \+ \(int\) Math\.round\(radius \* Math\.cos\(t \+ Math\.PI\)\);/,
  "generateDNAHelix lost the antiparallel second strand");
expectPattern(base, /int direction = rand\.nextInt\(4\);\s*int offset = 3;/, "addEntrance lost the random direction or the three-block lead-in");
expectPattern(base, /generateFilledVerticalDisk\(level, position\.above\(\), 3, direction != 1 && direction != 3\);/,
  "addEntrance lost the disk carve or its axis choice");
expectPattern(base, /if \(!fill\) \{\s*addFloorSpace\(level, position\);\s*\}/, "addFloor lost the unfilled 3x3 space punch");
expectPattern(base, /!ParasiteGenContext\.isModBlock\(ParasiteGenContext\.get\(level, atm\)\)/s, "addFloor does not stop at mod material");
expectPattern(base, /while \(range > 0 && isFloorReplaceable\(ParasiteGenContext\.get\(level, filler\)\)\)/,
  "genFloorFloor lost the fill gate or the 15-block depth");
expectPattern(base, /genFloorFloor\(level, atm\.below\(\), 15, fill\);/, "addFloor does not use the 15-block floor depth");
expectPattern(base, /if \(!ParasiteGenContext\.isModBlock\(current\) && !current\.isAir\(\)\)/, "replaceCircleGround lost the mod-block/air skip");

// ---------------------------------------------------------------------------------------------
// ParasiteGenContext additions
// ---------------------------------------------------------------------------------------------

const context = read("src/main/java/alku/csrp/world/gen/ParasiteGenContext.java");

expectPattern(context, /public static final BlockState RUBBLE_BONE = rubble\("bone"\);/, "RUBBLE_BONE is missing");
expectPattern(context, /public static final BlockState RUBBLE_BRICKS = rubble\("bricks"\);/, "RUBBLE_BRICKS is missing");
expectPattern(context, /public static final BlockState RUBBLE_FLESH = rubble\("flesh"\);/, "RUBBLE_FLESH is missing");
expectPattern(context, /public static final BlockState RUBBLE_FUNGUS = rubble\("fungus"\);/, "RUBBLE_FUNGUS is missing");
expectPattern(context, /private static BlockState rubble\(String variant\) \{\s*return variantState\(ModBlocks\.legacyBlock\("parasiterubble"\)\.get\(\), variant\);/,
  "the rubble palette does not resolve csrp:parasiterubble variants");
expectPattern(context, /public static final BlockState DENSE_WALL = ModBlocks\.PARASITERUBBLEDENSE\.get\(\)\.defaultBlockState\(\);/,
  "DENSE_WALL does not map to csrp:parasiterubbledense");
expectPattern(context, /public static final BlockState FOG = ModBlocks\.legacyBlock\("parasitefog"\)\.get\(\)\.defaultBlockState\(\);/,
  "FOG does not map to csrp:parasitefog");
expectPattern(context, /public static final BlockState BONE_BLOCK = Blocks\.BONE_BLOCK\.defaultBlockState\(\);/,
  "the field_189880_di mapping to Blocks.BONE_BLOCK is missing");
expectPattern(context, /public static final BlockState BUSH_BINE = variantState\(ModBlocks\.legacyBlock\("parasitebush"\)\.get\(\), "bine"\);/,
  "the placeVine bine state is missing");
expectPattern(context, /public static boolean isModBlock\(BlockState state\) \{\s*return BuiltInRegistries\.BLOCK\.getKey\(state\.getBlock\(\)\)\.getNamespace\(\)\.equals\(Csrp\.MODID\);/,
  "isModBlock does not reproduce the instanceof BlockBase test");
expectPattern(context, /public static void placeLoot\(ServerLevel level, BlockPos pos, ParasiteLootBlock\.Tier tier,\s*RandomSource random\)/,
  "the colony placeLoot helper is missing");
expectPattern(context, /case COMMON -> ModBlocks\.PARASITE_LOOT_COMMON\.get\(\);[\s\S]{0,200}case UNCOMMON -> ModBlocks\.PARASITE_LOOT_UNCOMMON\.get\(\);[\s\S]{0,200}case RARE -> ModBlocks\.PARASITE_LOOT_RARE\.get\(\);/,
  "placeLoot does not map the three original config loot lists onto the three loot tumors");
expectPattern(context, /loot\.generateLoot\(tier, random\);/, "placeLoot does not roll the tumor contents");

// sideCurse: i * 10 is the right-hand diagonal, i * 10 + 1 the left-hand one
const sideCurse = context.match(/public static BlockPos sideCurse\(BlockPos pos, int choice\) \{[\s\S]*?\n    \}/);
expect(sideCurse !== null, "ParasiteGenContext#sideCurse is missing");
if (sideCurse) {
  const text = sideCurse[0];
  const table = [
    ["case 0 -> pos.north().east();", "0 must walk north-east"],
    ["case 1 -> pos.north().west();", "1 must walk north-west"],
    ["case 10 -> pos.east().south();", "10 must walk south-east (the original func_177974_f then func_177968_d)"],
    ["case 11 -> pos.east().north();", "11 must walk north-east"],
    ["case 20 -> pos.south().west();", "20 must walk south-west"],
    ["case 21 -> pos.south().east();", "21 must walk south-east"],
    ["case 30 -> pos.west().north();", "30 must walk north-west"],
    ["default -> pos.west().south();", "31 must walk south-west"]
  ];
  for (const [line, message] of table) {
    expect(text.includes(line), `sideCurse: ${message}`);
  }
}

// The two BlockPos orderings must stay distinct: getDirectionRoot(0) is north, horizontal(0) is north,
// but getDirectionRoot(1) is south while directionToGrow(1) is east.
expectPattern(context, /case 1 -> pos\.east\(\);/, "ParasiteGenContext#horizontal lost its directionToGrow ordering");

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log("worldgen colony base: ok");
