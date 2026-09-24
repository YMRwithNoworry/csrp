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

if (failures.length) {
  console.error("verify-block-dedicated-classes: FAILED");
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-block-dedicated-classes: ok (19 legacy ids now use dedicated SRP block classes)");
