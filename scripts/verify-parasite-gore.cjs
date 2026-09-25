const fs = require("node:fs");
const path = require("node:path");

// Verifies the legacy death-gore / self-explode restoration (EntityParasiteBase.spawnGore,
// EntityPInfected.spawnGore, selfExplode) on the 1.21.1 line.
// Usage: node scripts/verify-parasite-gore.cjs

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

const config = read("src/main/java/alku/csrp/Config.java");
const rules = read("src/main/java/alku/csrp/event/ParasiteCombatRules.java");
const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");

// config surface (SRPConfig.paraGore / infectedRemainValue, dyingBurst chance)
for (const [pattern, message] of [
  [/define\("parasiteGore", true\)/, "parasiteGore (legacy paraGore) config is missing"],
  [/defineInRange\("parasiteRemainValue", 10, 1, 50000\)/,
    "parasiteRemainValue (legacy infectedRemainValue = 10) config is missing"],
  [/defineInRange\("parasiteSelfExplodeChance", 0\.5D, 0\.0D, 1\.0D\)/,
    "parasiteSelfExplodeChance (legacy 50% dyingBurst) config is missing"],
  [/public static boolean parasiteGoreEnabled\(\)/, "parasiteGoreEnabled accessor is missing"],
  [/public static int parasiteRemainValue\(\)/, "parasiteRemainValue accessor is missing"],
  [/public static double parasiteSelfExplodeChance\(\)/,
    "parasiteSelfExplodeChance accessor is missing"]
]) expect(config, pattern, message);

// block side: a public helper that resolves the per-tier legacy gore block and its variant
for (const [pattern, message] of [
  [/public static boolean placeGore\(Level level, BlockPos pos, String tier, boolean big\)/,
    "ModBlocks.placeGore helper is missing"],
  [/"infected", "goresim", "primitive", "gorepri", "adapted", "goreada"/,
    "the per-tier gore block map is incomplete"],
  [/"pure", "gorepur", "feral", "gorefer", "assimara", "goremar"/,
    "the per-tier gore block map is missing pure/feral/assimara entries"],
  [/canBeReplaced\(\)/, "placeGore must not overwrite a solid block"],
  [/big \? GoreVariant\.BIG : GoreVariant\.FLAT/, "placeGore must honour the big/flat variant"]
]) expect(blocks, pattern, message);

// behaviour side: hurt gore, death gore, Remain, gore bombs and the self-explode cloud
for (const [pattern, message] of [
  [/public static void applyDeathGore\(LivingDeathEvent event\)/,
    "the parasite death-gore handler is missing"],
  [/GORE_ON_HURT_CHANCE = 0\.1F/, "the 10% gore-on-hurt roll is missing"],
  [/Config\.parasiteGoreEnabled\(\)/, "gore placement must be gated by the config"],
  [/ModBlocks\.placeGore\(level, pos, tier, true\)/, "spawnGore must place a big gore block"],
  [/ModEntities\.REMAIN\.get\(\)\.create\(level\)/, "spawnGore must create a Remain"],
  [/remain\.setGoal\(20 \* Config\.parasiteRemainValue\(\)\)/,
    "the Remain goal must be 20 * remain value ticks"],
  [/remain\.setParasite\(BuiltInRegistries\.ENTITY_TYPE\.getKey\(parasite\.getType\(\)\)\.toString\(\)\)/,
    "the Remain must remember the parasite it came from"],
  [/private static void spawnGoreBombs\(ServerLevel level, LivingEntity parasite, int count\)/,
    "attackEntityFromCap gore bombs are missing"],
  [/bomb\.setType\(\(byte\) 1\)/, "gore bombs must use the original type 1"],
  [/ModSounds\.MOB_EXPLOSION\.get\(\)/, "selfExplode must play MOB_EXPLOTION"],
  [/MobEffects\.POISON, 300, 0/, "selfExplode cloud must apply poison 300"],
  [/ModMobEffects\.COTH, 3600, 0, false, true/,
    "selfExplode cloud must apply COTH 3600 with the project's visible-icon convention"],
  [/cloud\.setRadius\(parasite\.getBbWidth\(\) \* 1\.5F\)/,
    "selfExplode cloud radius must be width * 1.5"],
  [/cloud\.setWaitTime\(10\)/, "selfExplode cloud must wait 10 ticks"]
]) expect(rules, pattern, message);

if (failures.length) {
  console.error("Parasite gore verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Parasite gore verification passed.");
