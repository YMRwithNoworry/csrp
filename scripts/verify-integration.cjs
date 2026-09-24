const fs = require("node:fs");
const path = require("node:path");

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

const blockEntities = read("src/main/java/alku/csrp/registry/ModBlockEntities.java");
const furnace = read("src/main/java/alku/csrp/block/InfestedFurnaceBlock.java");
const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");
const main = read("src/main/java/alku/csrp/Csrp.java");
const properties = read("gradle.properties");

// 1.12.2 compatibility ids infested_furnace / infested_furnace_lit were a real container furnace
// (TileEntityInfestedFurnace); they must not stay inert blocks.
expect(blockEntities, /BLOCK_ENTITIES\.register\("legacy_infested_furnace"/,
        "the legacy_infested_furnace block-entity type is not registered");
expect(blockEntities, /ModBlocks\.legacyBlock\("infested_furnace"\)\.get\(\)/,
        "legacy_infested_furnace is not bound to the infested_furnace block");
expect(blockEntities, /ModBlocks\.legacyBlock\("infested_furnace_lit"\)\.get\(\)/,
        "legacy_infested_furnace is not bound to the infested_furnace_lit block");
expect(furnace, /class InfestedFurnaceBlock extends HorizontalDirectionalBlock implements EntityBlock/,
        "InfestedFurnaceBlock does not implement EntityBlock");
expect(furnace, /public BlockEntity newBlockEntity\(BlockPos pos, BlockState state\) \{\s*return new InfuserFurnaceBlockEntity\(pos, state\);/,
        "InfestedFurnaceBlock does not create an InfuserFurnaceBlockEntity");
expect(furnace, /InfuserFurnaceBlockEntity\.serverTick/,
        "InfestedFurnaceBlock has no server ticker for the furnace entity");
expect(furnace, /player\.openMenu\(furnace\)/,
        "InfestedFurnaceBlock does not open the furnace menu");
expect(furnace, /Block\.UPDATE_CLIENTS \| Block\.UPDATE_NEIGHBORS/,
        "setLitState recreates the block entity instead of preserving the inventory");

// SRPCommandSummonNidus generates the protection structure and reports the boolean result.
expect(commands, /new WorldGenParasiteNexusProtection1\(stage\)\s*\n?\s*\.generate\(level, level\.getRandom\(\), pos\.below\(\)\)/,
        "srp_summon_nidus no longer generates the Nexus protection structure below the target");
expect(commands, /"Generated Nidus\/Nexus protection structure at "/,
        "srp_summon_nidus lost the original success message");
expect(commands, /"Nidus\/Nexus protection structure generation returned false\."/,
        "srp_summon_nidus lost the original failure message");
expect(commands, /Commands\.literal\("entity"\)/,
        "the entity-summon convenience subcommand is missing");

// 1.10.9 spawn eggs recovered by R5 must reach the vanilla spawn-egg tab.
expect(main, /event\.accept\(ModItems\.FLAM_SPAWN_EGG\.get\(\)\)/,
        "flam_spawn_egg is not added to the SPAWN_EGGS tab");
expect(main, /event\.accept\(ModItems\.SOO_SPAWN_EGG\.get\(\)\)/,
        "soo_spawn_egg is not added to the SPAWN_EGGS tab");
expect(main, /event\.accept\(ModItems\.TENN_SPAWN_EGG\.get\(\)\)/,
        "tenn_spawn_egg is not added to the SPAWN_EGGS tab");

// The port tracks the upstream release it is aligned with.
expect(properties, /^mod_version=1\.10\.9$/m,
        "mod_version does not track the upstream 1.10.9 release");
expect(properties, /^mod_authors=.+$/m, "mod_authors is empty");
expect(properties, /^mod_description=.+$/m, "mod_description is empty");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("integration contract ok");
