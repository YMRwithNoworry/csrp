const fs = require("node:fs");
const path = require("node:path");

// Verifies the legacy water-leap restoration: the generationWaterLeap gene row, the gate on every
// existing water-leap goal, and the parameterised EntityAIWaterLeapAtTargetStatus equivalent.
// Usage: node scripts/verify-water-leap-gene.cjs

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

function expect(source, pattern, message) {
  if (!pattern.test(source)) failures.push(message);
}

const evolution = read("src/main/java/alku/csrp/world/EvolutionSystem.java");
const goal = read("src/main/java/alku/csrp/entity/WaterLeapAtTargetGoal.java");
const primitive = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const longarms = read("src/main/java/alku/csrp/entity/LongarmsEntity.java");

// legacy generationWaterLeap0..5 = {false, false, false, true, true, true}
expect(evolution, /GENERATION_WATER_LEAP = \{false, false, false, true, true, true\}/,
  "the generationWaterLeap row is missing or has the wrong defaults");
expect(evolution, /GENERATION_RESIDUE = \{false, false, false, false, true, true\}/,
  "the generationResidue row is missing or has the wrong defaults");
expect(evolution, /boolean waterLeap, boolean residue\)/, "GenerationProfile must expose waterLeap");
expect(evolution, /GENERATION_WATER_LEAP\[generation\],\s*\r?\n\s*GENERATION_RESIDUE\[generation\]\)/,
  "generationProfile(int) must fill the new rows");

// shared gate helper on the parasite base
expect(primitive, /protected final boolean waterLeapEnabled\(\) \{[\s\S]{0,220}?\.waterLeap\(\)/,
  "PrimitiveParasiteEntity.waterLeapEnabled() is missing");

// every existing water-leap goal consults the gene flag
const goalOwners = {
  "AdaptedVariantEntity.java": ["ArachnidaWaterLeapGoal"],
  "HeedEntity.java": ["WaterLeapGoal"],
  "PreeminentParasiteEntity.java": ["CarrierWaterLeapGoal"],
  "PrimitiveVariantEntity.java": ["ManducaterWaterLeapGoal", "ReekerWaterLeapGoal"],
  "PureParasiteEntity.java": ["GruntWaterLeapGoal", "MonarchWaterLeapGoal"],
  "VisceraEntity.java": ["WaterLeapGoal"]
};
for (const [file, goals] of Object.entries(goalOwners)) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  for (const name of goals) {
    const at = source.indexOf(`class ${name} extends Goal`);
    const canUse = at < 0 ? -1 : source.indexOf("public boolean canUse() {", at);
    const gated = canUse >= 0 && source.slice(canUse, canUse + 260).includes("waterLeapEnabled()");
    if (!gated) failures.push(`${file}: ${name} is not gated by the geneWaterleap flag`);
  }
}

// the parameterised legacy task
for (const [pattern, message] of [
  [/public final class WaterLeapAtTargetGoal extends Goal/,
    "the water leap goal is missing"],
  [/public WaterLeapAtTargetGoal\(PrimitiveParasiteEntity leaper, float leapMotionY, double jumpSpeed,\s*\r?\n\s*int cooldown, double damageRange\)/,
    "the goal must keep the legacy constructor shape"],
  [/geneEnabled\.getAsBoolean\(\)/, "the goal must consult the gene flag"],
  [/isInWaterOrBubble\(\) \|\| leaper\.isInLava\(\) \|\| attacking >= 1/,
    "the goal must trigger from water, lava or an ongoing leap"],
  [/targetY = Math\.max\(0\.0D, \(target\.getY\(\) - leaper\.getY\(\)\) \* 0\.07D\)/,
    "the legacy vertical aim bonus (0.07) is missing"],
  [/motion\.x \+ \(dx \/ length \* jumpSpeed \* 0\.9D \+ motion\.x \* 0\.3D\)/,
    "the legacy launch formula (speed * 0.9 + motion * 0.3) is missing"],
  [/startLeapAnimation\(\);/, "the launch must play the leap animation"],
  [/public WaterLeapAtTargetGoal\(Mob leaper, java\.util\.function\.BooleanSupplier geneEnabled,/,
    "families outside the primitive chain need the explicit gene gate constructor"],
  [/leaper\.getNavigation\(\)\.stop\(\)/, "the launch must stop navigation"]
]) expect(goal, pattern, message);

// pri_longarms registers it with the legacy parameters at priority 2
expect(longarms, /goalSelector\.addGoal\(2, new WaterLeapAtTargetGoal\(this, 0\.7F, 1\.5D, 20, 0\.0D\)\)/,
  "LongarmsEntity must register the legacy water leap at priority 2 with (0.7F, 1.5, 20, 0)");

// the feral family and sim_human register the same legacy task
for (const [file, message] of [["FeralParasiteEntity.java", "the feral family"], ["SimHumanEntity.java", "sim_human"]]) {
  const source = read("src/main/java/alku/csrp/entity/" + file);
  if (!/addGoal\(2, new WaterLeapAtTargetGoal\(this, \(\) -> level\(\) instanceof ServerLevel serverLevel[\s\S]{0,120}?\.waterLeap\(\), 0\.7F, 1\.5D, 20, 0\.0D\)\)/.test(source)) {
    failures.push(message + " must register the legacy water leap at priority 2 with (0.7F, 1.5, 20, 0)");
  }
}

// legacy handleWater: liquid hits charge a dash that the geneWaterleap flag releases
const liquid = read("src/main/java/alku/csrp/entity/LiquidLeap.java");
for (const [pattern, message] of [
  [/public final class LiquidLeap/, "the liquid leap component is missing"],
  [/MAX_CHARGES = 4/, "the legacy four charge cap is missing"],
  [/vertical = submerged \? 0\.1D : 0\.3D/, "the legacy 0.1/0.3 launch heights are missing"],
  [/strength = submerged \? 0\.5D : 1\.0D/, "the legacy 0.5/1.0 launch strengths are missing"],
  [/dx \/ length \* strength \* 0\.8D \+ motion\.x \* 0\.2D/, "the legacy dash formula is missing"]
]) expect(liquid, pattern, message);
for (const [file, field, message] of [["PrimitiveParasiteEntity.java", "liquidLeap", "the primitive chain"], ["FeralParasiteEntity.java", "feralLiquidLeap", "the feral family"]]) {
  const source = read("src/main/java/alku/csrp/entity/" + file);
  if (!source.includes(field + ".accumulate(this)") || !source.includes(field + ".spend(this")) {
    failures.push(message + " must drive the liquid leap charges");
  }
}

if (failures.length) {
  console.error("Water leap gene verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Water leap gene verification passed.");
