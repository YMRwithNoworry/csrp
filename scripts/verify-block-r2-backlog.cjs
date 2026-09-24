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

// ---------------------------------------------------------------------------------------------
// R2-1 / R2-2: BlockDeadheadLeaves seven-block decay + BlockParasiteTrunk.canSustainLeaves
// ---------------------------------------------------------------------------------------------
const originalLeaves = "D:/code/MC模组/_scratch/vf/out109/com/dhanantry/scapeandrunparasites/block/BlockDeadheadLeaves.java";
const originalTrunk = "D:/code/MC模组/_scratch/vf/out109/com/dhanantry/scapeandrunparasites/block/BlockParasiteTrunk.java";

const leaves = read(`${blockDir}DeadheadLeavesBlock.java`);
for (const [needle, label] of [
  ["DECAY_DISTANCE = 7", "seven-block decay distance (BlockDeadheadLeaves.java:28)"],
  ["distanceToSustainSupport", "decay scan driven by canSustainLeaves peers"],
  ["LeavesBlock", "leaves still use the vanilla DISTANCE machinery"]
]) {
  if (!leaves.includes(needle)) failures.push(`DeadheadLeavesBlock: missing ${label}`);
}

const trunk = read(`${blockDir}ParasiteTrunkBlock.java`);
for (const [needle, label] of [
  ["extends RotatedPillarBlock", "trunk is a rotated pillar (BlockParasiteTrunk.java:32)"],
  ["sustainsDeadheadLeaves", "modern replacement for canSustainLeaves (BlockParasiteTrunk.java:123)"],
  ["BlockTags.LOGS", "vanilla logs still sustain the leaves"]
]) {
  if (!trunk.includes(needle)) failures.push(`ParasiteTrunkBlock: missing ${label}`);
}

const modBlocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
if (!modBlocks.includes("new ParasiteTrunkBlock(")) {
  failures.push("ModBlocks: parasitetrunk must be registered with ParasiteTrunkBlock");
}

// The cited original facts must still be true in the read-only source of truth.
if (fs.existsSync(originalLeaves)) {
  const text = fs.readFileSync(originalLeaves, "utf8");
  if (!text.includes("int decayDistance = 7")) {
    failures.push("out109 BlockDeadheadLeaves no longer declares decayDistance = 7");
  }
  if (!text.includes("canSustainLeaves")) {
    failures.push("out109 BlockDeadheadLeaves no longer consults canSustainLeaves");
  }
}
if (fs.existsSync(originalTrunk)) {
  const text = fs.readFileSync(originalTrunk, "utf8");
  if (!text.includes("public boolean canSustainLeaves")) {
    failures.push("out109 BlockParasiteTrunk no longer declares canSustainLeaves");
  }
}

// ---------------------------------------------------------------------------------------------
// R2-3: the THORN backlog entry is a mis-attribution.  1.10.9 BlockParasiteBush has no THORN
// constant and no thorn damage; the thorn behaviour belongs to BlockThornshade + the
// THORNSHADE_THORNS potion, which this port already implements.
// ---------------------------------------------------------------------------------------------
const originalBush = "D:/code/MC模组/_scratch/vf/out109/com/dhanantry/scapeandrunparasites/block/BlockParasiteBush.java";
if (fs.existsSync(originalBush)) {
  const text = fs.readFileSync(originalBush, "utf8");
  const enumBody = (text.split("public enum EnumType implements IStringSerializable {")[1] || "").split("}")[0];
  for (const constant of ["TENDRIL", "BINE", "POP", "EYE", "TOOH"]) {
    if (!enumBody.includes(constant)) {
      failures.push(`out109 BlockParasiteBush.EnumType lost constant ${constant}`);
    }
  }
  if (enumBody.includes("THORN")) {
    failures.push("out109 BlockParasiteBush.EnumType now declares THORN: re-check the R2-3 finding");
  }
  if (text.includes("THORN_E") || text.includes("THORNSHADE")) {
    failures.push("out109 BlockParasiteBush now references thorn logic: re-check the R2-3 finding");
  }
}

const portBush = read(`${blockDir}ParasiteBushBlock.java`);
if (!portBush.includes("decorative variants")) {
  failures.push("ParasiteBushBlock: the thorn/frost variants must be documented as asset-only extras");
}
const thornShade = read("src/main/java/alku/csrp/block/ThornshadeBlock.java");
if (thornShade.length === 0) {
  failures.push("ThornshadeBlock missing: thorn behaviour must stay with BlockThornshade");
}
const thornEvents = read("src/main/java/alku/csrp/event/ThornshadeThornsEvents.java");
if (!thornEvents.includes("THORNSHADE_THORNS")) {
  failures.push("ThornshadeThornsEvents: the thorn potion behaviour must stay implemented");
}

// ---------------------------------------------------------------------------------------------
// R2-4: BlockEvolutionLure luredValueNine / luredValueTen
// ---------------------------------------------------------------------------------------------
const originalConfig = "D:/code/MC模组/_scratch/vf/out109/com/dhanantry/scapeandrunparasites/util/config/SRPConfigSystems.java";
const lure = read(`${blockDir}EvolutionLureBlock.java`);
for (const [needle, label] of [
  ['NINE("nine", 1_200, 50_000_000, 7)', "NINE tier = luredValueNine 1200s / luredValueNineCool 50000000"],
  ['TEN("ten", 1_200, 72_000_000, 8)', "TEN tier = luredValueTen 1200s / luredValueTenCool 72000000"]
]) {
  if (!lure.includes(needle)) failures.push(`EvolutionLureBlock: missing ${label}`);
}
if (fs.existsSync(originalConfig)) {
  const text = fs.readFileSync(originalConfig, "utf8");
  for (const [needle, label] of [
    ["luredValueNine = 1200", "out109 luredValueNine default"],
    ["luredValueNineCool = 50000000", "out109 luredValueNineCool default"],
    ["luredValueTen = 1200", "out109 luredValueTen default"],
    ["luredValueTenCool = 72000000", "out109 luredValueTenCool default"]
  ]) {
    if (!text.includes(needle)) failures.push(`out109 SRPConfigSystems lost ${label}`);
  }
}

// ---------------------------------------------------------------------------------------------
// R2-5: infested furnace semantics
// ---------------------------------------------------------------------------------------------
const furnace = read(`${blockDir}InfestedFurnaceBlock.java`);
for (const [needle, label] of [
  ["LIT_LIGHT_LEVEL = 14", "lit furnaces emit light 14 (BlockInfestedFurnace.java:53)"],
  ["FACING", "horizontal facing metadata (BlockInfestedFurnace.java:29)"],
  ["setLitState", "setLitState state swap (BlockInfestedFurnace.java:35)"]
]) {
  if (!furnace.includes(needle)) failures.push(`InfestedFurnaceBlock: missing ${label}`);
}
if (!modBlocks.includes("infested_furnace_lit")) {
  failures.push("ModBlocks: infested_furnace_lit must be registered");
}

if (failures.length) {
  console.error("verify-block-r2-backlog: FAILED");
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-block-r2-backlog: ok (deadhead decay, canSustainLeaves, THORN finding, lure tiers, furnace)");
