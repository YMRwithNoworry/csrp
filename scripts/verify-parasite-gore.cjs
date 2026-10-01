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
const remains = read("src/main/java/alku/csrp/entity/RemainEntity.java");

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

// Entity remains deliberately replace legacy terrain-changing gore blocks.
for (const [pattern, message] of [
  [/public static RemainEntity spawn\(ServerLevel level/,
    "the server-side remains spawn helper is missing"],
  [/case "infected", "sim" -> 0/, "infected remains appearance is missing"],
  [/case "primitive", "pri" -> 1/, "primitive remains appearance is missing"],
  [/case "adapted", "ada" -> 2/, "adapted remains appearance is missing"],
  [/case "pure"/, "pure remains appearance is missing"],
  [/case "feral", "fer" -> 4/, "feral remains appearance is missing"],
  [/case "assimara", "mar" -> 5/, "assimara remains appearance is missing"],
  [/case "small" -> 1;[\s\S]*?case "big" -> 2;/, "small/big remains variants are missing"]
]) expect(remains, pattern, message);
if (/placeGore|\.setBlock(?:AndUpdate)?\(/.test(rules)) {
  failures.push("combat gore must spawn entities without placing blocks");
}

// behaviour side: hurt gore, death gore, Remain, gore bombs and the self-explode cloud
for (const [pattern, message] of [
  [/public static void applyDeathGore\(LivingDeathEvent event\)/,
    "the parasite death-gore handler is missing"],
  [/GORE_ON_HURT_CHANCE = 0\.1F/, "the 10% gore-on-hurt roll is missing"],
  [/Config\.parasiteGoreEnabled\(\)/, "gore placement must be gated by the config"],
  [/RemainEntity\.spawn\(level, parasite\.getX\(\), parasite\.getY\(\), parasite\.getZ\(\), tier, "big"\)/,
    "death gore must create a visible big remains entity"],
  [/RemainEntity\.spawn\(goreLevel,[\s\S]*?"flat"\)/,
    "hurt gore must create a flat remains entity"],
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
