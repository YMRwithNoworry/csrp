const fs = require("fs");
const path = require("path");

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

const blockDir = "src/main/java/alku/csrp/block/";
const modBlocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");

// Every legacy id in this list must be produced by a dedicated SRP class instead of the generic
// `new Block(legacyProperties(key))` fallback in ModBlocks#registerLegacyBlocks.
const dedicated = {
  goreada: ["GoreBlock", "gore"],
  gorefer: ["GoreBlock", "gore"],
  goremar: ["GoreBlock", "gore"],
  gorepri: ["GoreBlock", "gore"],
  gorepur: ["GoreBlock", "gore"],
  goresim: ["GoreBlock", "gore"],
  assimilated_blossom: ["AssimilatedBlossomBlock", "flower"],
  bloodyice: ["BloodyIceBlock", "ice"],
  infestedbush: ["InfestedBushBlock", "bush"],
  parasitebush: ["ParasiteBushBlock", "bush"],
  parasitestain: ["ParasiteStainBlock", "stain"],
  parasiterubble: ["ParasiteRubbleBlock", "rubble"],
  parasiteplank: ["ParasitePlankBlock", "plank"],
  parasitecanister: ["ParasiteCanisterBlock", "canister"],
  infestedore: ["InfestedOreBlock", "ore"],
  harlequinn_grass: ["HarlequinnGrassBlock", "grass"],
  infested_leaves: ["InfestedLeavesBlock", "leaves"],
  infested_leaves_fast: ["InfestedLeavesBlock", "leaves"],
  infested_cactus: ["InfestedCactusBlock", "cactus"]
};

const dedicatedSection = modBlocks.split("legacyDedicatedBlocks()")[1] || "";
for (const [id, [className]] of Object.entries(dedicated)) {
  if (!dedicatedSection.includes(`"${id}"`)) {
    failures.push(`ModBlocks: legacy id ${id} is not wired to ${className}`);
  }
  if (!new RegExp(`new ${className}\\(`).test(dedicatedSection)) {
    failures.push(`ModBlocks: ${className} is never constructed by the legacy factory map`);
  }
}

// The six gore ids share one class and one variant property.
const gore = read(`${blockDir}GoreBlock.java`);
const goreChecks = [
  ['EnumProperty.create("variant", Variant.class)', "gore variant property"],
  ["Variant.SMALL", "gore default variant SMALL (BlockGore.java:46)"],
  ["DECAY_CHANCE_BIG = 45", "gore BIG decay denominator 45 (BlockGore.java:102)"],
  ["DECAY_CHANCE_SMALL = 10", "gore decay denominator 10 (BlockGore.java:99)"],
  ["ModMobEffects.COTH", "gore applies COTH (BlockGore.java:89)"],
  ["ModMobEffects.REPEL", "gore respects the E.P.E.L. immunity (BlockGore.java:88)"],
  ["return List.of();", "gore drops nothing (BlockGore.java:119)"]
];
for (const [needle, label] of goreChecks) {
  if (!gore.includes(needle)) failures.push(`GoreBlock: missing ${label}`);
}

const blossom = read(`${blockDir}AssimilatedBlossomBlock.java`);
for (const [needle, label] of [
  ["extends BushBlock", "assimilated blossom is a BlockBush"],
  ["BlockTags.DIRT", "grass/ground material support (BlockSRPFlower.java:20-23)"],
  ["BlockTags.SAND", "sand material support (BlockSRPFlower.java:20-23)"],
  ["Blocks.CLAY", "clay material support (BlockSRPFlower.java:20-23)"]
]) {
  if (!blossom.includes(needle)) failures.push(`AssimilatedBlossomBlock: missing ${label}`);
}

const ice = read(`${blockDir}BloodyIceBlock.java`);
for (const [needle, label] of [
  ["extends ParasiteSpreadingBlock", "bloody ice is a spreading block"],
  ["super(properties, true)", "bloody ice is an infested spreading block (BlockBloodyIce.java:24)"],
  ["BREAK_FALL_DISTANCE = 5.0F", "bloodyIceBreakFallDistance default 5.0"],
  ["BREAK_DIAMETER = 3", "bloodyIceBreakDiameter default 3"],
  ["public void fallOn(", "break on hard landing hook (BlockBloodyIce.java:67)"],
  ["return List.of();", "bloody ice drops nothing (BlockBloodyIce.java:40)"]
]) {
  if (!ice.includes(needle)) failures.push(`BloodyIceBlock: missing ${label}`);
}
if (!modBlocks.includes(".friction(0.98F)")) {
  failures.push("ModBlocks: bloodyice must keep slipperiness 0.98 (BlockBloodyIce.java:25)");
}

const spreading = read(`${blockDir}ParasiteSpreadingBlock.java`);
for (const [needle, label] of [
  ["isAreaLoaded(pos, 3)", "area-loaded guard (BlockParasiteSpreading.java:31)"],
  ["BlockInfestation.spread", "canInfestBlock branch"],
  ["InfestationSpreadLimiter.Type.BIOME", "biome stain branch"]
]) {
  if (!spreading.includes(needle)) failures.push(`ParasiteSpreadingBlock: missing ${label}`);
}

const bushBase = read(`${blockDir}SrpBushBlock.java`);
for (const [needle, label] of [
  ['BooleanProperty.create("node")', "NODE property (BlockInfestedBush.java:9)"],
  ['BooleanProperty.create("end")', "END property (BlockInfestedBush.java:10)"],
  ["Block.box(1.6D, 0.0D, 1.6D, 14.4D, 12.8D, 14.4D)", "TALL_GRASS_AABB shape"],
  ["Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D)", "REED_AABB shape"],
  ['"bloodyice".equals(path)', "bloody ice is not a valid bush support"],
  ['"ashen_glass".equals(path)', "ashen glass is not a valid bush support"],
  ["Items.SHEARS", "shears still harvest the bush (IShearable)"]
]) {
  if (!bushBase.includes(needle)) failures.push(`SrpBushBlock: missing ${label}`);
}

const infestedBush = read(`${blockDir}InfestedBushBlock.java`);
for (const [needle, label] of [
  ["INFECTED", "infected variant"],
  ["GRASS1", "grass1 variant"],
  ["FLOWER1", "flower1 variant"],
  ["SPINE", "spine variant"],
  ["VINE", "vine variant"],
  ["ARC", "arc variant"],
  ["isClimber", "spine/vine climbing support"]
]) {
  if (!infestedBush.includes(needle)) failures.push(`InfestedBushBlock: missing ${label}`);
}

const parasiteBush = read(`${blockDir}ParasiteBushBlock.java`);
for (const [needle, label] of [
  ["TENDRIL", "tendril variant"],
  ["BINE", "bine variant"],
  ["POP", "pop variant"],
  ["EYE", "eye variant"],
  ["TOOH", "tooh variant"],
  ["hasSrpCeilingSupport", "tendril/bine ceiling support (BlockParasiteBush.java:130-141)"],
  ["destroyBlock(pos, true)", "unsupported parasite bush drops (BlockParasiteBush.java:217)"]
]) {
  if (!parasiteBush.includes(needle)) failures.push(`ParasiteBushBlock: missing ${label}`);
}

const stain = read(`${blockDir}ParasiteStainBlock.java`);
for (const variant of ["DIRT", "MUD", "FLESH", "FEELER", "SPORE", "RED", "SACKFLESH"]) {
  if (!stain.includes(`${variant}("`)) failures.push(`ParasiteStainBlock: missing variant ${variant}`);
}
if (!stain.includes("SoundType.MUD") || !stain.includes("SoundType.GRAVEL") || !stain.includes("SoundType.GRASS")) {
  failures.push("ParasiteStainBlock: per-variant sound mapping missing (BlockParasiteStain.java:28-40)");
}
if (!stain.includes("Variant.DIRT")) failures.push("ParasiteStainBlock: default variant must be DIRT");

const rubble = read(`${blockDir}ParasiteRubbleBlock.java`);
for (const variant of ["WEATHB", "WEATHBS", "WEATHBC", "WEATHBCS", "WEATHFS", "WEATHFSS", "BONE", "METAL", "OBSIDIAN", "FUNGUS", "STONEDEBRIS", "BRICKS", "WOOD", "STONE", "FLESH"]) {
  if (!rubble.includes(`${variant}("`)) failures.push(`ParasiteRubbleBlock: missing variant ${variant}`);
}
if (!rubble.includes("Blocks.SNOW")) failures.push("ParasiteRubbleBlock: snow swap missing (BlockParasiteRubble.java:74-117)");

const ore = read(`${blockDir}InfestedOreBlock.java`);
for (const variant of ["CO", "DIA", "EME", "GOL", "IRO", "LAP", "RED", "UN"]) {
  if (!ore.includes(`${variant}("`)) failures.push(`InfestedOreBlock: missing variant ${variant}`);
}

const canister = read(`${blockDir}ParasiteCanisterBlock.java`);
for (const variant of ["SAC", "CYST", "LUMP", "BAG"]) {
  if (!canister.includes(`${variant}("`)) failures.push(`ParasiteCanisterBlock: missing variant ${variant}`);
}
if (!canister.includes("Blocks.AIR.defaultBlockState()")) {
  failures.push("ParasiteCanisterBlock: CYST decay tick missing (BlockParasiteCanister.java:36-43)");
}
if (!canister.includes("SAC_DROP_CHANCE = 0.05D")) {
  failures.push("ParasiteCanisterBlock: SAC 5% drop chance missing (BlockParasiteCanister.java:52)");
}

const plank = read(`${blockDir}ParasitePlankBlock.java`);
for (const variant of ["DEADHEAD", "DEADHEADS"]) {
  if (!plank.includes(`${variant}("`)) failures.push(`ParasitePlankBlock: missing variant ${variant}`);
}

const leaves = read(`${blockDir}InfestedLeavesBlock.java`);
for (const [needle, label] of [
  ['IntegerProperty.create("decay_age", 0, 5)', "decay_age 0..5 (BlockLeafLike.java:6)"],
  ["HEAL_DIVISOR = 6", "1/6 healing divisor (BlockLeafLike.java:50)"],
  ["DECAY_DIVISOR = 12", "1/12 decay divisor (BlockLeafLike.java:57)"],
  ["BASE_FALSE_APPLE_CHANCE = 40", "false apple base chance 40 (BlockLeafLike.java:7,89)"],
  ["Enchantments.FORTUNE", "fortune scaling (BlockLeafLike.java:89)"],
  ['"srparasites".equals(id.getNamespace())', "touchingAnySRP namespace scan"]
]) {
  if (!leaves.includes(needle)) failures.push(`InfestedLeavesBlock: missing ${label}`);
}

const cactus = read(`${blockDir}InfestedCactusBlock.java`);
for (const [needle, label] of [
  ["extends CactusBlock", "infested cactus is a cactus"],
  ["PUSH_COOLDOWN_TICKS = 8L", "8-tick push cooldown (BlockParasiteCactus.java:11)"],
  ["PUSH_HORIZONTAL = 0.35D", "PUSH_H (BlockParasiteCactus.java:9)"],
  ["PUSH_VERTICAL = 0.08D", "PUSH_Y (BlockParasiteCactus.java:10)"],
  ['"srp_pcactus_last_push"', "per-player cooldown NBT key (BlockParasiteCactus.java:7)"],
  ["BlockTags.SAND", "sand support (BlockParasiteCactus.java:78-81)"],
  ["ModBlocks.INFESTED_SAND", "infested sand support (BlockParasiteCactus.java:80)"]
]) {
  if (!cactus.includes(needle)) failures.push(`InfestedCactusBlock: missing ${label}`);
}

const harlequinn = read(`${blockDir}HarlequinnGrassBlock.java`);
for (const [needle, label] of [
  ["extends ParasiteSpreadingBlock", "harlequinn grass is a spreading block"],
  ["super(properties, false)", "harlequinn grass is not an infested spreading block"],
  ["ModBlocks.LOCS_BLOCK", "snow converts harlequinn grass into locs block (BlockParasiteSpreading.java:36-40)"]
]) {
  if (!harlequinn.includes(needle)) failures.push(`HarlequinnGrassBlock: missing ${label}`);
}

// ---------------------------------------------------------------------------------------------
// Full coverage: every one of the 108 legacy ids must resolve to a dedicated factory, so none of
// them can fall through to the generic `new Block(legacyProperties(key))` placeholder.
// ---------------------------------------------------------------------------------------------
const idsSection = (modBlocks.split("String[] ids = {")[1] || "").split("};")[0];
const legacyIds = [...idsSection.matchAll(/"([a-z_0-9]+)"/g)].map((m) => m[1]);
const dedicatedSection2 = modBlocks.split("legacyDedicatedBlocks()")[1] || "";
const covered = new Set([...dedicatedSection2.matchAll(/dedicated\.put\("([a-z_0-9]+)"/g)].map((m) => m[1]));
// The gore family is registered from a loop over a literal array inside the factory method.
for (const id of ["goreada", "gorefer", "goremar", "gorepri", "gorepur", "goresim"]) {
  if (dedicatedSection2.includes(`"${id}"`)) {
    covered.add(id);
  }
}
for (const arrayName of ["LEGACY_SLAB_IDS", "LEGACY_STAIR_IDS", "LEGACY_WALL_IDS", "LEGACY_FENCE_IDS", "LEGACY_POT_IDS"]) {
  const body = (modBlocks.split(`String[] ${arrayName} = {`)[1] || "").split("};")[0];
  for (const match of body.matchAll(/"([a-z_0-9]+)"/g)) {
    covered.add(match[1]);
  }
}
if (legacyIds.length !== 108) {
  failures.push(`ModBlocks: expected 108 legacy ids, found ${legacyIds.length}`);
}
for (const id of legacyIds) {
  if (!covered.has(id)) {
    failures.push(`ModBlocks: legacy id ${id} still falls back to the generic placeholder block`);
  }
}

// The shape families must reproduce the original slab/stair/wall/fence/pot state contracts.
const slab = read(`${blockDir}LegacySlabBlock.java`);
for (const [needle, label] of [
  ["extends SlabBlock", "slabs are vanilla SlabBlock shaped (BlockSlabBase.java:1)"],
  ["BlockInfestation.infestAround", "slab/wall beckon tick (BlockWallBase.java:44-51)"]
]) {
  if (!slab.includes(needle)) failures.push(`LegacySlabBlock: missing ${label}`);
}
const variantSlab = read(`${blockDir}LegacyVariantSlabBlock.java`);
if (!variantSlab.includes("builder.add(variant)")) {
  failures.push("LegacyVariantSlabBlock: variant property must be part of the state definition");
}
const wall = read(`${blockDir}LegacyWallBlock.java`);
if (!wall.includes("extends WallBlock")) failures.push("LegacyWallBlock: must extend WallBlock");
const fence = read(`${blockDir}LegacyFenceBlock.java`);
if (!fence.includes("extends FenceBlock")) failures.push("LegacyFenceBlock: must extend FenceBlock");
const stair = read(`${blockDir}LegacyStairBlock.java`);
if (!stair.includes("extends StairBlock")) failures.push("LegacyStairBlock: must extend StairBlock");
const pot = read(`${blockDir}PottedSrpBlock.java`);
if (!pot.includes("Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D)")) {
  failures.push("PottedSrpBlock: pot AABB missing (BlockPottedSRPFlower.java:14)");
}

// Machines and remaining flora.
const fog = read(`${blockDir}ParasiteFogBlock.java`);
for (const [needle, label] of [
  ['IntegerProperty.create("air", 0, 2)', "air 0..2 metadata (BlockParasiteFog.java:9)"],
  ["EXPAND_RADIUS = 2", "BGrange = 2 expansion (BlockParasiteFog.java:87)"],
  ["Items.GLASS_BOTTLE", "glass bottle interaction (BlockParasiteFog.java:127)"]
]) {
  if (!fog.includes(needle)) failures.push(`ParasiteFogBlock: missing ${label}`);
}

const furnace = read(`${blockDir}InfestedFurnaceBlock.java`);
for (const [needle, label] of [
  ["LIT_LIGHT_LEVEL = 14", "lit light level 14 (BlockInfestedFurnace.java:53)"],
  ["setLitState", "setLitState helper (BlockInfestedFurnace.java:35)"]
]) {
  if (!furnace.includes(needle)) failures.push(`InfestedFurnaceBlock: missing ${label}`);
}
if (!modBlocks.includes("InfestedFurnaceBlock.LIT_LIGHT_LEVEL")) {
  failures.push("ModBlocks: infested furnace ids must emit light 14 while lit");
}

const barrier = read(`${blockDir}ParasiteBarrierBlock.java`);
for (const [needle, label] of [
  ["Shapes.empty()", "no collision box (BlockParasiteBarrier.java:76)"],
  ["6_000_000.0F", "resistance 6000000 (BlockParasiteBarrier.java:20)"]
]) {
  if (!barrier.includes(needle)) failures.push(`ParasiteBarrierBlock: missing ${label}`);
}

const diffuser = read(`${blockDir}EpitomeDiffuserBlock.java`);
for (const [needle, label] of [
  ["MAX_BLOCKS_PER_TICK = 8192", "8192 blocks/tick (BlockEpitomeInfestationWarpDiffuser.java:8)"],
  ["MAX_BLOCKS_TOTAL = 2_000_000", "2000000 block cap (BlockEpitomeInfestationWarpDiffuser.java:9)"],
  ["RADIUS = 256", "256 block radius (BlockEpitomeInfestationWarpDiffuser.java:7)"]
]) {
  if (!diffuser.includes(needle)) failures.push(`EpitomeDiffuserBlock: missing ${label}`);
}

const dod = read(`${blockDir}DispatcherNBlock.java`);
for (const [needle, label] of [
  ["HIT_DAMAGE = 4.0F", "4.0F dispatch damage (BlockDod.java:66)"],
  ["HIT_COOLDOWN_TICKS = 20L", "20 tick hit cooldown (BlockDod.java:60)"],
  ['"srp_dod_last_hit"', "hit cooldown NBT key (BlockDod.java:9)"]
]) {
  if (!dod.includes(needle)) failures.push(`DispatcherNBlock: missing ${label}`);
}

const sapling = read(`${blockDir}ParasiteSaplingBlock.java`);
for (const [needle, label] of [
  ['IntegerProperty.create("stage", 0, 1)', "stage 0..1 (BlockParasiteSapling.java:10)"],
  ["GROWTH_DIVISOR = 7", "1/7 growth chance (BlockParasiteSapling.java:27)"],
  ["MIN_GROWTH_LIGHT = 9", "light >= 9 gate (BlockParasiteSapling.java:26)"]
]) {
  if (!sapling.includes(needle)) failures.push(`ParasiteSaplingBlock: missing ${label}`);
}

const remain = read(`${blockDir}InfestedRemainBlock.java`);
for (const [needle, label] of [
  ['IntegerProperty.create("source", 0, 1)', "source 0..1 (BlockInfestedRemain.java:5)"],
  ['BooleanProperty.create("infested_base")', "infested_base flag (BlockInfestedRemain.java:6)"],
  ["WALK_DAMPING = 0.84D", "0.84 walk damping (BlockInfestedRemain.java:45)"],
  ["Items.IRON_SHOVEL", "shovel-only harvest (BlockInfestedRemain.java:56)"]
]) {
  if (!remain.includes(needle)) failures.push(`InfestedRemainBlock: missing ${label}`);
}

for (const [file, needle, label] of [
  ["HirsuteHairBlock.java", "extends BushBlock", "hirsute hair is a bush"],
  ["TressesHairBlock.java", "extends DoublePlantBlock", "tresses hair is a double plant"],
  ["LipomaMassBlock.java", "Direction.DOWN", "lipoma mass hangs from an SRP ceiling (BlockLipomaMass.java:37)"],
  ["ParasiteTendrilBlock.java", "extends VineBlock", "tendril is a vine (BlockVineBase.java:1)"],
  ["ColonyOutpostBlock.java", "ACTIVE", "colony outpost keeps the active stage"],
  ["ColonyOutpostBlock.java", "ColonyStructureGenerator.generateBuilding", "colony outpost builds through the ported generator"],
  ["DermoidCystBlock.java", "extends HorizontalDirectionalBlock", "dermoid cyst is horizontally facing"],
  ["LegacyRelayBlock.java", "LIT", "relay controller keeps its lit flag"]
]) {
  if (!read(blockDir + file).includes(needle)) failures.push(`${file}: missing ${label}`);
}

if (failures.length) {
  console.error("verify-block-dedicated-classes: FAILED");
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log(`verify-block-dedicated-classes: ok (all ${legacyIds.length} legacy ids use dedicated SRP block classes)`);
