const fs = require("node:fs");
const path = require("node:path");

// The last two of the three 1.10.9 harlequin commands (CommonProxy#registerServerCommand:253-255
// registers Here, Convert, Scatter in that order) plus the block converter they share.
//
// Every number below is copied from the decompiled 1.10.9 sources:
//   network/CommandHarlequinConvert.java, network/CommandHarlequinScatter.java,
//   util/convert/HarlequinBlockConverter.java, world/gen/HarlequinRockBushGen.java
//
// The 1.12.2 block ids those sources compare against are obfuscated; the SRG → MCP names used here
// come from the official MCP stable_39 mapping table (fields.csv), so the port's block sets can be
// re-checked without the mappings file:
//   field_150354_m = SAND            field_150322_A = SANDSTONE     field_180395_cM = RED_SANDSTONE
//   field_150349_c = GRASS           field_150346_d = DIRT          field_150348_b  = STONE
//   field_150344_f = PLANKS          field_150347_e = COBBLESTONE   field_150341_Y  = MOSSY_COBBLESTONE
//   field_150355_j = WATER           field_150358_i = FLOWING_WATER field_150353_l  = LAVA
//   field_150356_k = FLOWING_LAVA
// (FLOWING_WATER/FLOWING_LAVA are the same blocks as WATER/LAVA in 26.3; PLANKS is the
// minecraft:planks tag because 1.12.2 kept all six plank variants in one block.)
const root = path.resolve(__dirname, "..");
const failures = [];

function read(relativePath) {
    const file = path.join(root, relativePath);
    if (!fs.existsSync(file)) {
        failures.push(`missing ${relativePath}`);
        return "";
    }
    return fs.readFileSync(file, "utf8");
}

function expect(content, pattern, description) {
    if (!pattern.test(content)) failures.push(description);
}

function refute(content, pattern, description) {
    if (pattern.test(content)) failures.push(description);
}

const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");

// --- /harlequin_convert ----------------------------------------------------------------------------
const convert = read("src/main/java/alku/csrp/command/HarlequinConvertCommand.java");
expect(convert, /NAME = "harlequin_convert"/, "the command name changed");
expect(convert, /Commands\.hasPermission\(Commands\.LEVEL_GAMEMASTERS\)/,
        "harlequin_convert is no longer permission level 2");
expect(convert, /"radius", "sampleChance", "depth", "blotchChance", "blotchMinDiam", "blotchMaxDiam"/,
        "the six positional arguments changed");
expect(convert, /DEFAULTS = \{16, 1, 32, 18, 3, 5\}/, "the argument defaults changed");
expect(convert, /MINIMUMS = \{1, 1, 1, 1, 1, 1\}/, "the argument lower bounds changed");
expect(convert, /MAXIMUMS = \{256, 128, 256, 256, 64, 64\}/, "the argument upper bounds changed");
expect(convert, /blotchChance <= 1 \|\| random\.nextInt\(Math\.max\(2, blotchChance\)\) == 0/,
        "the blotch seeding roll changed");
expect(convert, /minDiameter \+ random\.nextInt\(maxDiameter - minDiameter \+ 1\)/,
        "the blotch diameter roll changed");
expect(convert, /random\.nextInt\(Math\.max\(1, sampleChance\)\) != 0/, "the sampling roll changed");
expect(convert, /y >= Math\.max\(5, yTop - depth\)/, "the column depth limit changed");
expect(convert, /replacedWith != config\.alveoli\(\) && random\.nextInt\(100\) < 30/,
        "the 30% lipoma_mass placement changed");
expect(convert, /Heightmap\.Types\.MOTION_BLOCKING/, "the heightmap is no longer MOTION_BLOCKING");
expect(convert, /"Harlequin convert: changed ~"/, "the original's report line is gone");
expect(convert, /ModBlocks\.legacyBlock\("harlequinn_grass"\)/,
        "the palette no longer uses the legacy harlequinn_grass id");
expect(convert, /ModBlocks\.legacyBlock\("lipoma_mass"\)/, "the palette no longer uses lipoma_mass");
expect(convert, /ModBlocks\.legacyBlock\("tresses_hair"\)/, "the palette no longer uses tresses_hair");
expect(convert, /ModBlocks\.legacyBlock\("hirsute_hair"\)/, "the palette no longer uses hirsute_hair");
expect(commands, /dispatcher\.register\(HarlequinConvertCommand\.command\(\)\)/,
        "harlequin_convert is not registered");

// --- util/convert/HarlequinBlockConverter ----------------------------------------------------------
const converter = read("src/main/java/alku/csrp/world/gen/HarlequinBlockConverter.java");
expect(converter, /state\.is\(BlockTags\.PLANKS\) \|\| state\.is\(Blocks\.COBBLESTONE\) \|\| state\.is\(Blocks\.MOSSY_COBBLESTONE\)/,
        "the planks/cobblestone/mossy-cobblestone family changed");
expect(converter, /state\.is\(Blocks\.GRASS_BLOCK\)/, "the grass branch changed");
expect(converter, /state\.is\(Blocks\.STONE\)/, "the stone branch changed");
expect(converter, /Blocks\.SANDSTONE\) \|\| state\.is\(Blocks\.CHISELED_SANDSTONE\)/,
        "the sandstone family lost its vanilla variants");
expect(converter, /state\.is\(Blocks\.RED_SANDSTONE\) \|\| state\.is\(Blocks\.CHISELED_RED_SANDSTONE\)/,
        "red sandstone (field_180395_cM) is no longer recognised");
expect(converter, /block instanceof LeavesBlock/, "the leaves branch changed");
expect(converter, /state\.is\(Blocks\.WATER\) \|\| state\.is\(Blocks\.LAVA\)/,
        "the air/water/lava rejection changed");
expect(converter, /level\.canSeeSky\(pos\.above\(\)\)/, "isTopSunlit no longer checks the sky above");
expect(converter, /random\.nextInt\(100\) < 30/, "the 30% alveoli_growth roll changed");
expect(converter, /withFacing\(config\.alveoliGrowth\(\)\.defaultBlockState\(\),\s*Direction\.DOWN\)/,
        "the alveoli_growth facing write changed");
expect(converter, /aboveBlock == config\.tresses\(\) \|\| aboveBlock == config\.hirsute\(\)/,
        "the blotch clear of tresses/hirsute above the grass changed");
// The original's hair placement is gated on Block#canSustainPlant, which Forge defaults to false and
// 1.10.9 overrides nowhere, so 1.10.9 never grows hair.  The port must keep that gate shut.
expect(converter, /private static boolean canSustain\(ServerLevel level, BlockPos pos, Block plant\) \{\s*return false;/,
        "canSustain no longer mirrors the original's always-false Block#canSustainPlant");
expect(converter, /if \(roll < 60 && canTresses && level\.isEmptyBlock\(above\.above\(\)\)\)/,
        "the tresses_hair branch changed");
expect(converter, /else if \(roll < 80 && canHirsute\)/, "the hirsute_hair branch changed");
expect(converter, /DoublePlantBlock\.HALF, DoubleBlockHalf\.LOWER/,
        "the double-plant lower half is no longer written");
expect(converter, /DoublePlantBlock\.HALF, DoubleBlockHalf\.UPPER/,
        "the double-plant upper half is no longer written");

// --- /harlequin_scatter ----------------------------------------------------------------------------
const scatter = read("src/main/java/alku/csrp/command/HarlequinScatterCommand.java");
expect(scatter, /NAME = "harlequin_scatter"/, "the command name changed");
expect(scatter, /Commands\.hasPermission\(Commands\.LEVEL_GAMEMASTERS\)/,
        "harlequin_scatter is no longer permission level 2");
expect(scatter, /TYPES = \{"rock", "bush", "tree", "ruin", "both", "all"\}/, "the type list changed");
for (const [array, first, last] of [["ROCKS", "harlequin_rock_01", "harlequin_rock_07"],
    ["BUSHES", "harlequin_bush_00", "harlequin_bush_02"],
    ["TREES", "harlequin_tree_01", "harlequin_tree_07"],
    ["RUINS", "harlequin_ruin_01", "harlequin_ruin_03"]]) {
    const block = new RegExp(`${array} = \\{[^}]*"${first}"[^}]*"${last}"`, "s");
    expect(scatter, block, `the ${array} template list changed`);
}
expect(scatter, /DEFAULT_TRIES = 32/, "the default tries is no longer 32");
expect(scatter, /DEFAULT_RADIUS = 32/, "the default radius is no longer 32");
expect(scatter, /IntegerArgumentType\.integer\(1, 4096\)/, "the tries bounds changed");
expect(scatter, /IntegerArgumentType\.integer\(8, 512\)/, "the radius bounds changed");
expect(scatter, /random\.nextInt\(radius \* 2 \+ 1\) - radius/, "the column roll changed");
expect(scatter, /flatEnough\(level, surf, 5, 3\)/, "the rock 5/3 flatness check changed");
expect(scatter, /flatEnough\(level, s2, 3, 3\)/, "the bush 3/3 flatness check changed");
expect(scatter, /flatEnough\(level, surf, 6, 3\)/, "the tree 6/3 flatness check changed");
expect(scatter, /flatEnough\(level, surf, 10, 4\)/, "the ruin 10/4 flatness check changed");
expect(scatter, /1 \+ random\.nextInt\(3\)/, "the bush cluster count changed");
expect(scatter, /1 \+ random\.nextInt\(2\)/, "the 'both' bush cluster count changed");
expect(scatter, /2 \+ random\.nextInt\(3\)/, "the 'all' bush cluster count changed");
expect(scatter, /random\.nextInt\(7\) - 3/, "the bush offset changed");
expect(scatter, /random\.nextInt\(9\) - 4/, "the tree/bush offset changed");
expect(scatter, /random\.nextInt\(17\) - 8/, "the ruin offset changed");
expect(scatter, /"Harlequin scatter: placed ~"/, "the original's report line is gone");
expect(scatter, /Rotation\.NONE, Rotation\.CLOCKWISE_90, Rotation\.CLOCKWISE_180,\s*Rotation\.COUNTERCLOCKWISE_90/,
        "the rotation table order changed");
expect(scatter, /random\.nextBoolean\(\) \? Mirror\.NONE : Mirror\.FRONT_BACK/, "the mirror roll changed");
expect(scatter, /setIgnoreEntities\(true\)/, "placement no longer ignores entities");
expect(scatter, /Heightmap\.Types\.MOTION_BLOCKING/, "the heightmap is no longer MOTION_BLOCKING");
// The original's ground set is SAND/SANDSTONE/RED_SANDSTONE/GRASS/DIRT (fields.csv above), not the
// red-sand/gravel pair an earlier revision of HarlequinRockBushGen used.
expect(scatter, /state\.is\(Blocks\.SAND\) \|\| state\.is\(Blocks\.SANDSTONE\) \|\| state\.is\(Blocks\.RED_SANDSTONE\)/,
        "the scatter ground set lost sandstone/red sandstone");
refute(scatter, /state\.is\(Blocks\.RED_SAND\)/, "red sand is not original ground");
refute(scatter, /state\.is\(Blocks\.GRAVEL\)/, "gravel is not original ground");
refute(scatter, /Blocks\.ICE/, "ice is not a bad top block in the original");
expect(scatter, /block == Blocks\.WATER \|\| block == Blocks\.LAVA/, "the bad top set changed");
expect(scatter, /block instanceof LeavesBlock/, "leaves are no longer a bad top block");
expect(commands, /dispatcher\.register\(HarlequinScatterCommand\.command\(\)\)/,
        "harlequin_scatter is not registered");

// --- the same corrections in the world generator that shares these rules ---------------------------
const bushGen = read("src/main/java/alku/csrp/world/gen/HarlequinRockBushGen.java");
expect(bushGen, /state\.is\(Blocks\.SAND\) \|\| state\.is\(Blocks\.SANDSTONE\) \|\| state\.is\(Blocks\.RED_SANDSTONE\)/,
        "HarlequinRockBushGen lost the sandstone/red-sandstone ground");
refute(bushGen, /state\.is\(Blocks\.RED_SAND\)/, "HarlequinRockBushGen accepts red sand as ground");
refute(bushGen, /state\.is\(Blocks\.GRAVEL\)/, "HarlequinRockBushGen accepts gravel as ground");
refute(bushGen, /Blocks\.ICE/, "HarlequinRockBushGen rejects ice as a top block");
refute(bushGen, /MOTION_BLOCKING_NO_LEAVES/, "HarlequinRockBushGen went back to the no-leaves heightmap");
expect(bushGen, /Heightmap\.Types\.MOTION_BLOCKING/, "HarlequinRockBushGen lost the MOTION_BLOCKING heightmap");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("harlequin convert + scatter contract ok");
