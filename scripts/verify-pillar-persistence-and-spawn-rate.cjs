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
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};
const reject = (text, pattern, message) => {
  if (pattern.test(text)) failures.push(message);
};

const config = read("src/main/java/alku/csrp/config/WorldConfig.java");
const tables = read("src/main/java/alku/csrp/world/NaturalSpawnTables.java");
const events = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
const transformation = read("src/main/java/alku/csrp/entity/ParasiteTransformation.java");

// Beckons, Dispatchers and Rooters are world structures: nothing in the mod may delete them.
expect(nexus, /public boolean isWorldStructure\(\)[\s\S]*?activeKind\(\)\.isRooterBall\(\)/,
  "Nexus world-structure marker is missing");
expect(nexus, /spawned\.canGrow = false;/,
  "Beckons summoned by another Beckon are not marked as non-growing");
expect(nexus, /if \(!kind\.isRooterBall\(\)\) \{\s*setPersistenceRequired\(\);\s*\}/,
  "Pillars are not marked persistent when they are constructed");
expect(nexus, /if \(!activeKind\(\)\.isRooterBall\(\)\) \{\s*setPersistenceRequired\(\);\s*\}/,
  "Pillars loaded from older worlds are not re-marked persistent");
// Mob checks the peaceful difficulty before PersistenceRequired, so persistence alone
// is not enough to keep a pillar alive.
expect(nexus, /protected boolean shouldDespawnInPeaceful\(\) \{\s*return activeKind\(\)\.isRooterBall\(\);\s*\}/,
  "Pillars are still deleted when the world switches to peaceful");

// The original devolution wand deletes parasites without a predecessor; a stage one
// pillar must survive that branch instead of vanishing.
expect(transformation, /source instanceof NexusParasiteEntity nexus && nexus\.isWorldStructure\(\)\) \{\s*return false;\s*\}/,
  "The devolution wand still removes a stage one Beckon");
reject(transformation, /if \(targetType == null\) \{\s*source\.discard\(\);/,
  "Devolve still discards parasites without checking for a world structure");
reject(nexus, /TEMPORARY_BECKON_LIFETIME|temporaryLifetimeTicks|makeTemporaryBeckon/,
  "Nexus entities still self-destruct through a temporary lifetime");
reject(nexus, /nexus_temporary_lifetime/,
  "Nexus still saves the removed temporary lifetime");

expect(events, /instanceof Parasite\s*&& !isNexusWorldStructure\(living\)/,
  "The mob cleaner still collects Beckons, Dispatchers and Rooters");
expect(events, /private static boolean isNexusWorldStructure\(Entity entity\)[\s\S]*?nexus\.isWorldStructure\(\)/,
  "The mob cleaner has no Nexus world-structure guard");
expect(events, /if \(!\(entity instanceof Parasite\) \|\| isNexusWorldStructure\(entity\)\) \{\s*continue;\s*\}[\s\S]*?\+\+count >= cap/,
  "The natural spawn cap still counts Beckons, Dispatchers and Rooters");

// The mod's parasites appear slightly more often through a configurable weight multiplier.
expect(config, /defineInRange\("naturalSpawnWeightMultiplier", 1\.25D, 0\.0D, 10\.0D\)/,
  "naturalSpawnWeightMultiplier is not defined with a 1.25 default");
expect(config, /public static double naturalSpawnWeightMultiplier\(\)/,
  "The naturalSpawnWeightMultiplier getter is missing");
expect(tables, /WorldConfig\.naturalSpawnWeightMultiplier\(\)/,
  "The natural spawn tables never read the weight multiplier");
expect(tables, /return scaledWeights\(ubiquitous\);/,
  "Ubiquitous development tables are not weight scaled");
expect(tables, /return scaledWeights\(phaseEntries\(phase\)\);/,
  "Phase tables are not weight scaled");
expect(tables, /new MobSpawnSettings\.SpawnerData\(group\.type\(\),\s*scaledWeight\(/,
  "Scaled spawn entries are not rebuilt through the multiplier");
expect(tables, /BASE_GROUPS\.put\(entry, new SpawnGroup\(type, weight, minCount, maxCount\)\)/,
  "Original spawn weights are not recorded for scaling");

// The discrete tables must keep the original SRP 1.10.8 values.
expect(tables, /spawn\("buglin", 2, 6, 30\)/, "Phase 0 Buglin weight is no longer original");
expect(tables, /spawn\("pri_devourer", 1, 2, 1\)/, "Phase -1 Devourer weight is no longer original");
expect(tables, /spawn\("grunt", 6, 10, 40\)/, "UD4 Grunt weight is no longer original");
reject(tables, /spawn\("buglin", 2, 6, 38\)/, "Scaled weights leaked into the discrete tables");

if (failures.length) {
  console.error("Pillar persistence and spawn rate verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log("Pillar persistence and spawn rate verification passed.");
