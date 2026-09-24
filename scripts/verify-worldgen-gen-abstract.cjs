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

const context = read("src/main/java/alku/csrp/world/gen/ParasiteGenContext.java");
const placer = read("src/main/java/alku/csrp/world/StructurePlacer.java");
const glob = (pattern) => {
  const results = [];
  const walk = (directory) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const full = path.join(directory, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (pattern.test(entry.name)) results.push(full);
    }
  };
  walk(path.join(root, "src/main/java/alku/csrp"));
  return results;
};

// ---------------------------------------------------------------------------------------------
// WorldGenParasiteGenAbstract / WorldGenParasiteTreeAbstract semantics
// ---------------------------------------------------------------------------------------------

// canGrowInto is vanilla WorldGenAbstractTree#func_150523_a plus the parasite bush, so it must
// carry the sapling and vine members and both log ids, and must not carry podzol.
expectPattern(context, /public static boolean canGrowInto\(BlockState state\)/,
  "ParasiteGenContext#canGrowInto is missing");
const growInto = context.match(/public static boolean canGrowInto\(BlockState state\) \{[\s\S]*?\n    \}/);
expect(growInto !== null, "cannot read the canGrowInto body");
if (growInto) {
  const body = growInto[0];
  for (const [needle, message] of [
    ["state.isAir()", "canGrowInto must accept air (Material.AIR)"],
    ["block instanceof LeavesBlock", "canGrowInto must accept leaves (Material.LEAVES)"],
    ["block == Blocks.GRASS_BLOCK", "canGrowInto must accept Blocks.GRASS (field_150349_c)"],
    ["block == Blocks.DIRT", "canGrowInto must accept Blocks.DIRT (field_150346_d)"],
    ["block == Blocks.OAK_LOG", "canGrowInto must accept Blocks.LOG (field_150364_r)"],
    ["block == Blocks.SPRUCE_LOG", "canGrowInto must accept every Blocks.LOG variant"],
    ["block == Blocks.BIRCH_LOG", "canGrowInto must accept every Blocks.LOG variant"],
    ["block == Blocks.JUNGLE_LOG", "canGrowInto must accept every Blocks.LOG variant"],
    ["block == Blocks.ACACIA_LOG", "canGrowInto must accept Blocks.LOG2 (acacia, field_150363_s)"],
    ["block == Blocks.DARK_OAK_LOG", "canGrowInto must accept Blocks.LOG2 (dark oak, field_150363_s)"],
    ["state.is(BlockTags.SAPLINGS)", "canGrowInto must accept saplings (field_150345_g)"],
    ["block == Blocks.VINE", "canGrowInto must accept vines (field_150395_bd)"],
    ['ModBlocks.legacyBlock("parasitebush")', "canGrowInto must accept the parasite bush"]
  ]) {
    expect(body.includes(needle), message);
  }
  expect(!/PODZOL/.test(body), "canGrowInto still maps the sapling case to podzol");
}

// isReplaceable = air || leaves || isWood || canGrowInto || parasite bush
expectPattern(context,
  /public static boolean isReplaceable\(ServerLevel level, BlockPos pos\) \{[\s\S]*?state\.isAir\(\)[\s\S]{0,120}\|\| state\.getBlock\(\) instanceof LeavesBlock[\s\S]{0,120}\|\| isWoodLike\(level, pos\)[\s\S]{0,120}\|\| canGrowInto\(state\)[\s\S]{0,160}\|\| state\.getBlock\(\) == ModBlocks\.legacyBlock\("parasitebush"\)\.get\(\);/,
  "isReplaceable no longer mirrors WorldGenParasiteTreeAbstract#isReplaceable");

// setDirtAt writes the parasite stain everywhere except dirt
expectPattern(context, /public static void setDirtAt\(ServerLevel level, BlockPos pos\) \{[\s\S]{0,260}setBlock\(level, pos, ModBlocks\.INFESTED_STAIN\.get\(\)\.defaultBlockState\(\)\);/,
  "setDirtAt no longer writes the parasite stain (func_175921_a)");

// isWoodLike must cover the mod trunks the original Block#isWood did through its own blocks
for (const trunk of ["PARASITETRUNK", "PARASITETRUNK_BALL", "PARASITETRUNK_PLANT", "INFESTED_TRUNK"]) {
  expectPattern(context, new RegExp(`Block\\s+block == ModBlocks\\.${trunk}\\.get\\(\\)|block == ModBlocks\\.${trunk}\\.get\\(\\)`),
    `isWoodLike does not treat ModBlocks.${trunk} as wood`);
}

// ---------------------------------------------------------------------------------------------
// WorldGenStructure -> StructurePlacer
// ---------------------------------------------------------------------------------------------

expectPattern(placer, /public static boolean place\(ServerLevel level, Identifier id, BlockPos pos\)/,
  "StructurePlacer lost the WorldGenStructure#generate equivalent");
expectPattern(placer, /public static boolean place\(ServerLevel level, Identifier id, BlockPos pos, RandomSource random\)/,
  "StructurePlacer lost the random-carrying overload the mixins use");
expectPattern(placer, /public static boolean place\(ServerLevel level, Identifier id, BlockPos placementPos, RandomSource random,\s*BlockPos anchor, Rotation rotation\)/,
  "StructurePlacer lost the anchor/rotation overload DeadheadTreeGen needs");
expectPattern(placer, /new StructurePlaceSettings\(\)\s*\.setMirror\(Mirror\.NONE\)\s*\.setRotation\(rotation\)\s*\.setIgnoreEntities\(false\);/,
  "StructurePlacer no longer uses Mirror.NONE with the requested rotation (the original used Mirror.NONE/Rotation.NONE)");
expect(placer.includes("Rotation.NONE"), "StructurePlacer must default to Rotation.NONE");
expectPattern(placer, /level\.sendBlockUpdated\(origin, state, state, 3\);/, "StructurePlacer lost the notifyBlockUpdate(pos, state, state, 3) equivalent");
expectPattern(placer, /template\.placeInWorld\(level, origin, origin, settings, random, 2\);/, "StructurePlacer does not place with flag 2");
expectPattern(placer, /level\.getStructureTemplateManager\(\)\.get\(id\)/, "StructurePlacer no longer resolves the template from the level's own manager");
expect(!/srparasites/.test(placer), "StructurePlacer still references the old srparasites namespace");
expectPattern(placer, /data\/" \+ id\.getNamespace\(\)\s*\+\s*"\/structure\//, "the missing-template diagnostic no longer points at data/<namespace>/structure/");

// Every template id the ported R3 features ask for must exist under csrp.
for (const template of ["ball", "ballbig", "meteor", "meteor_fragment_large1", "meteor_fragment_small6"]) {
  expect(fs.existsSync(path.join(root, `src/main/resources/data/csrp/structure/${template}.nbt`)),
    `missing src/main/resources/data/csrp/structure/${template}.nbt`);
}
const structureFiles = fs.readdirSync(path.join(root, "src/main/resources/data/csrp/structure"));
expect(structureFiles.length > 0, "the csrp structure directory is empty");

// ---------------------------------------------------------------------------------------------
// WorldGenCustomStructures: the 1.12.2 IWorldGenerator entry has no 26.3 equivalent
// ---------------------------------------------------------------------------------------------

const sources = glob(/\.java$/);
const iworldgenerator = sources.filter((file) => /implements\s+IWorldGenerator|net\.minecraftforge\.fml\.common\.IWorldGenerator/.test(fs.readFileSync(file, "utf8")));
expect(iworldgenerator.length === 0,
  `26.3 cannot carry the 1.12.2 IWorldGenerator entry point, but these files still implement it: ${iworldgenerator.join(", ")}`);
for (const file of sources) {
  const text = fs.readFileSync(file, "utf8");
  expect(!/calculateHeight\(World, int, int, Block\)/.test(text),
    `the removed 1.12.2 calculateHeight(World,int,int,Block) helper reappeared in ${path.relative(root, file)}`);
}

// generateInPosition's "place at pos + offset" contract is what the nexus and meteor ports rely on.
expectPattern(placer, /BlockPos origin = placementPos;\s*if \(anchor != null\) \{\s*origin = placementPos\.subtract\(rotateAnchor\(anchor, rotation\)\);\s*\}/,
  "StructurePlacer no longer places the template origin at the requested position");
expectPattern(read("src/main/java/alku/csrp/world/gen/WorldGenParasiteNexusProtection2.java"),
  /StructurePlacer\.place\(level, Identifier\.fromNamespaceAndPath\(Csrp\.MODID, out\), basePos,\s*level\.getRandom\(\)\);/,
  "the nexus protection no longer places through StructurePlacer (the generateInPosition equivalent)");
expectPattern(read("src/main/java/alku/csrp/world/gen/WorldGenParasiteMeteorCrash.java"),
  /StructurePlacer\.place\(level, Identifier\.fromNamespaceAndPath\(Csrp\.MODID, out\), impactCenter\);/,
  "the meteor fragments no longer place through StructurePlacer");

// WorldGenCustomStructures#generateStructure gated on the biome class; the port gates on the
// colony/biome decorator instead, so the SRPConfigWorld colony switches must still be honoured.
expectPattern(read("src/main/java/alku/csrp/block/ColonyStructureBlock.java"),
  /nearestColonyInConstructionRange\(pos\)/, "the colony structure block lost its SRPConfigWorld#coloniesActivated equivalent gate");
expectPattern(read("src/main/java/alku/csrp/world/SrpCoreSystems.java"),
  /Config\.colonyMaximumNumber\(\)[\s\S]{0,200}Config\.colonyMinimumDistance\(\)/,
  "placeColony lost the colony count/distance config gates");

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log("worldgen abstract bases and structure placer: ok");
