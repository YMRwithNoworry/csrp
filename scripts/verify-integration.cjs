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

function refute(content, pattern, description) {
    if (pattern.test(content)) failures.push(description);
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

// Original ItemAdvancementIcon sets stack size 1 for every icon (field_77777_bU = 1).
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
expect(items, /private static DeferredItem<Item> advancementIcon\(String id\)/,
        "ModItems has no advancementIcon() helper for stack-size-1 icons");
expect(items, /new Item\.Properties\(\)\.stacksTo\(1\)/,
        "advancementIcon() does not cap the stack size at one");
const looseIcons = items.match(/simple\("[a-z_]+_icon"\)/g) || [];
if (looseIcons.length > 0) {
    failures.push("advancement icons still registered with the default stack size: " + looseIcons.join(", "));
}
expect(items, /SELF_DESTRUCT_ICON = advancementIcon\("self_destruct_icon"\)/,
        "self_destruct_icon is not registered through advancementIcon()");

// 1.10.9 registers the mob id draconite against the class EntityHeblu, so the legacy spawner
// name heblu must resolve to draconite and not to the unrelated wraith.
const spawner = read("src/main/java/alku/csrp/item/LegacyMobSpawnerItem.java");
expect(spawner, /case "heblu" -> "draconite";/,
        "the heblu legacy spawner does not resolve to draconite");
refute(spawner, /case "heblu" -> "wraith";/,
        "the heblu legacy spawner still resolves to wraith");
const blizzard = read("src/main/java/alku/csrp/world/star/SRPBlizzardDerivedHandler.java");
expect(blizzard, /Csrp\.MODID, "draconite"\)\)/,
        "SRPBlizzardDerivedHandler does not resolve csrp:draconite");
refute(blizzard, /Csrp\.MODID, "heblu"\)\)/,
        "SRPBlizzardDerivedHandler still looks up the non-existent csrp:heblu id");

// Runtime regressions caught by runGameTestServer: a block whose createBlockStateDefinition reads a
// constructor-assigned instance field leaves its properties out of the state definition, which aborts
// block registration at load time. Both classes must publish the value before super() runs.
const variantSlab = read("src/main/java/alku/csrp/block/LegacyVariantSlabBlock.java");
expect(variantSlab, /PENDING_VARIANT\.set\(variant\)/,
        "LegacyVariantSlabBlock does not publish the variant before the superclass constructor runs");
expect(variantSlab, /EnumProperty<\?> variant = PENDING_VARIANT\.get\(\);/,
        "LegacyVariantSlabBlock reads the variant from an instance field inside createBlockStateDefinition");
expect(variantSlab, /builder\.add\(variant\)/,
        "LegacyVariantSlabBlock no longer adds the variant property");
const relay = read("src/main/java/alku/csrp/block/LegacyRelayBlock.java");
expect(relay, /PENDING_LIT_STATE\.set\(hasLitState\)/,
        "LegacyRelayBlock does not publish hasLitState before the superclass constructor runs");
expect(relay, /Boolean\.TRUE\.equals\(PENDING_LIT_STATE\.get\(\)\)/,
        "LegacyRelayBlock reads hasLitState from an instance field inside createBlockStateDefinition");
refute(read("src/main/java/alku/csrp/registry/ModBlocks.java"), /new LegacyRelayBlock\(/,
        "ModBlocks still constructs LegacyRelayBlock directly instead of through its factory");
refute(read("src/main/java/alku/csrp/registry/ModBlocks.java"), /new LegacyVariantSlabBlock\(/,
        "ModBlocks still constructs LegacyVariantSlabBlock directly instead of through its factory");

// JukeboxSong is a datapack registry in 26.3: a code-side DeferredRegister entry alone leaves
// "Missing element ResourceKey[minecraft:jukebox_song / csrp:<id>]" during item initialisation.
for (const disc of ["discone", "disctwo", "discthree"]) {
    const relative = "src/main/resources/data/csrp/jukebox_song/" + disc + ".json";
    const absolute = path.join(root, relative);
    if (!fs.existsSync(absolute)) {
        failures.push("missing jukebox song datapack entry " + relative);
        continue;
    }
    const entry = JSON.parse(fs.readFileSync(absolute, "utf8"));
    if (entry.sound_event !== "csrp:srparasites." + disc) {
        failures.push(relative + " points at the wrong sound event: " + entry.sound_event);
    }
    if (entry.description === undefined || entry.description.translate !== "jukebox_song.csrp." + disc) {
        failures.push(relative + " has the wrong description key");
    }
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("integration contract ok");
