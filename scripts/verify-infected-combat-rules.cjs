const fs = require("node:fs");
const path = require("node:path");

// Verifies the legacy infected/assimilated combat rules restored in EntityPInfected /
// EntityParasiteBase are wired on the 1.21.1 line: damage cap with RAGE, armor-bypassing
// minimum melee damage scaled by VIRA, food stealing, kill healing, and status immunity.
// Usage: node scripts/verify-infected-combat-rules.cjs

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
const effects = read("src/main/java/alku/csrp/entity/ParasiteCombatEffects.java");
const parasite = read("src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java");
const variant = read("src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java");
const human = read("src/main/java/alku/csrp/entity/SimHumanEntity.java");
const status = read("src/main/java/alku/csrp/event/StatusEffectEvents.java");

// config surface (legacy SRPConfig.infectedCap / infectedMinDamage / foodSteal / geneMobHealing)
for (const [pattern, message] of [
  [/defineInRange\("infectedDamageCap", 2, 1, 100\)/, "infectedDamageCap config is missing"],
  [/defineInRange\("infectedMinimumDamage", 0\.5D, 0\.0D, 1000\.0D\)/,
    "infectedMinimumDamage config is missing"],
  [/defineInRange\("infectedFoodSteal", 0\.1D, 0\.0D, 1\.0D\)/, "infectedFoodSteal config is missing"],
  [/defineInRange\("infectedKillHeal", 1\.0D, 0\.0D, 100\.0D\)/, "infectedKillHeal config is missing"],
  [/public static int infectedDamageCap\(\)/, "infectedDamageCap accessor is missing"],
  [/public static float infectedMinimumDamage\(\)/, "infectedMinimumDamage accessor is missing"],
  [/public static float infectedFoodSteal\(\)/, "infectedFoodSteal accessor is missing"],
  [/public static float infectedKillHeal\(\)/, "infectedKillHeal accessor is missing"]
]) expect(config, pattern, message);

// damage cap: clamp to maxHealth / cap + maxHealth % cap * 0.5, RAGE 200/1, fire and void bypass
for (const [pattern, message] of [
  [/static float damageAfterIncomingCap\(LivingEntity self, DamageSource source, float amount\)/,
    "damageAfterIncomingCap helper is missing"],
  [/maximumHealth \/ cap \+ maximumHealth % cap \* 0\.5F/, "damage cap formula is wrong"],
  [/ModMobEffects\.RAGE, 200, 1, false, false/, "damage cap must grant RAGE 200/1"],
  [/DamageTypeTags\.IS_FIRE\) \|\| source\.is\(DamageTypes\.FELL_OUT_OF_WORLD\)/,
    "damage cap must skip fire and void damage"],
  [/EvolutionSystem\.generationProfile\(serverLevel\)\.damageCap\(\)/,
    "damage cap must be gene gated by the generation profile"]
]) expect(effects, pattern, message);

// minimum melee damage: VIRA amplifier + 2, armor bypass, generation gated
for (const [pattern, message] of [
  [/static void applyMinimumMeleeDamage\(LivingEntity attacker, LivingEntity target\)/,
    "applyMinimumMeleeDamage helper is missing"],
  [/base \* \(viral == null \? 1\.0F : viral\.getAmplifier\(\) \+ 2\)/,
    "minimum damage must scale with the victim's VIRA amplifier"],
  [/EvolutionSystem\.generationProfile\(serverLevel\)\.minimumDamage\(\)/,
    "minimum damage must be gene gated"],
  [/target\.die\(attacker\.damageSources\(\)\.mobAttack\(attacker\)\)/,
    "minimum damage must kill the victim when it drops to zero health"]
]) expect(effects, pattern, message);

// food stealing drops assimilated flesh (the original infected_drop)
for (const [pattern, message] of [
  [/static void stealFoodFromPlayer\(LivingEntity attacker, LivingEntity target\)/,
    "stealFoodFromPlayer helper is missing"],
  [/stack\.has\(DataComponents\.FOOD\)/, "food stealing must look for food components"],
  [/new ItemStack\(ModItems\.ASSIMILATED_FLESH\.get\(\)\)/,
    "stolen food must drop as assimilated flesh"]
]) expect(effects, pattern, message);

// kill healing uses the geneMobHealing value from the generation profile
for (const [pattern, message] of [
  [/static void healOnKill\(LivingEntity killer, LivingEntity victim\)/, "healOnKill helper is missing"],
  [/EvolutionSystem\.generationProfile\(serverLevel\)\.mobHealing\(\)/,
    "kill healing must use the generation profile mobHealing gene"],
  [/killer\.heal\(victim\.getMaxHealth\(\) \* multiplier \* Config\.infectedKillHeal\(\)\)/,
    "kill healing must scale with the victim's maximum health"]
]) expect(effects, pattern, message);

// the three assimilated classes must share the rules
for (const [source, name] of [[parasite, "AssimilatedParasiteEntity"],
  [variant, "AssimilatedVariantEntity"], [human, "SimHumanEntity"]]) {
  expect(source, /ParasiteCombatEffects\.damageAfterIncomingCap\(this, source, dealt\)/,
    `${name}: incoming damage is not capped`);
  expect(source, /ParasiteCombatEffects\.applyMinimumMeleeDamage\(this, livingTarget\)/,
    `${name}: melee minimum damage is not applied`);
  expect(source, /ParasiteCombatEffects\.stealFoodFromPlayer\(this, livingTarget\)/,
    `${name}: food stealing is not applied`);
  expect(source, /ParasiteCombatEffects\.healOnKill\(this, victim\)/,
    `${name}: kill healing is not applied`);
}

// legacy canBeAffected: parasites never take their own status effects
expect(status, /public static void preventParasiteStatusApplication\(MobEffectEvent\.Applicable event\)/,
  "parasite status immunity handler is missing");
expect(status, /event\.getEntity\(\) instanceof Parasite/,
  "status immunity must apply to parasites only");
for (const effect of ["COTH", "VIRAL", "CORROSION", "NEEDLER"]) {
  expect(status, new RegExp(`instance\\.is\\(ModMobEffects\\.${effect}\\)`),
    `status immunity does not cover ${effect}`);
}
expect(status, /MobEffectEvent\.Applicable\.Result\.DO_NOT_APPLY/,
  "status immunity must refuse the effect");

if (failures.length) {
  console.error("Infected combat rules verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Infected combat rules verification passed.");
