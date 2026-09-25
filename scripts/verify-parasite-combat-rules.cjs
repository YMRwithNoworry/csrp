const fs = require("node:fs");
const path = require("node:path");

// Verifies the shared EntityParasiteBase combat rules restored on the 1.21.1 line:
// per-tier damage cap with RAGE, armor-bypassing minimum damage scaled by VIRA, food stealing,
// poison-to-healing, fear on heavy hits, kill healing, status immunity and the fire multiplier.
// Usage: node scripts/verify-parasite-combat-rules.cjs

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
const effects = read("src/main/java/alku/csrp/entity/ParasiteCombatEffects.java");
const status = read("src/main/java/alku/csrp/event/StatusEffectEvents.java");
const primitive = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const buglin = read("src/main/java/alku/csrp/entity/BuglinEntity.java");

// config surface: the original SRPConfig per-tier numbers plus the shared genes
for (const [pattern, message] of [
  [/"infected;2;0\.5;0\.1"/, "infected tier row (cap 2 / min 0.5) is missing"],
  [/"feral;3;0\.75;0\.5"/, "feral tier row (cap 3 / min 0.75) is missing"],
  [/"hijacked;5;1\.3;0\.1"/, "hijacked tier row (cap 5 / min 1.3) is missing"],
  [/"assimara;5;1\.1;0\.5"/, "assimara tier row (cap 5 / min 1.1) is missing"],
  [/"primitive;6;2\.0;0\.5"/, "primitive tier row (cap 6 / min 2.0) is missing"],
  [/"adapted;9;4\.0;0\.5"/, "adapted tier row (cap 9 / min 4.0) is missing"],
  [/"pure;13;7\.0;0\.5"/, "pure tier row (cap 13 / min 7.0) is missing"],
  [/"nexus_siii;14;0\.0;0\.5"/, "nexus stage III tier row (cap 14) is missing"],
  [/defineInRange\("parasitePoisonHealing", 2\.5D/, "parasitePoisonHealing (genePoisonHealing) is missing"],
  [/defineInRange\("parasiteFoodTheftChance", 0\.1D/, "parasiteFoodTheftChance (foodRott) is missing"],
  [/defineInRange\("parasiteFireMultiplier", 4\.0D/, "parasiteFireMultiplier (firemultyplier) is missing"],
  [/public static List<\? extends String> parasiteCombatTable\(\)/, "parasiteCombatTable accessor is missing"],
  [/private static boolean validCombatTableEntry\(Object value\)/, "combat table validator is missing"]
]) expect(config, pattern, message);

// event-driven rules: defense, offense, kill heal
for (const [pattern, message] of [
  [/public static void applyDefenseRules\(LivingIncomingDamageEvent event\)/,
    "damage-cap/poison defense handler is missing"],
  [/public static void applyAttackRules\(LivingDamageEvent\.Post event\)/,
    "minimum-damage/food-steal/fear handler is missing"],
  [/public static void applyKillHeal\(LivingDeathEvent event\)/,
    "kill-heal handler is missing"],
  [/maximumHealth \/ tier\.damageCap\(\) \+ maximumHealth % tier\.damageCap\(\) \* 0\.5F/,
    "damage cap formula must match maxHealth/cap + maxHealth%cap*0.5"],
  [/new MobEffectInstance\(ModMobEffects\.RAGE, 200, 1, false, false\)/,
    "reaching the damage cap must grant RAGE 200/1"],
  [/DamageTypeTags\.IS_FIRE\)[\s\S]{0,160}?return;/, "the damage cap must skip fire damage"],
  [/convertPoisonToHealing/, "poison-to-healing conversion is missing"],
  [/DamageTypes\.MAGIC/, "poison conversion must key on magic damage"],
  [/parasite\.hasEffect\(MobEffects\.POISON\)/, "poison conversion must require the poison effect"],
  [/FEAR_DAMAGE_THRESHOLD = 8\.0F/, "fear threshold (damage above 8) is missing"],
  [/ParasiteCombatEffects\.applyFearFromDamage/, "fear application is missing"],
  [/ParasiteCombatEffects\.applyMinimumDamage\(parasite, victim, tier\.minimumDamage\(\)\)/,
    "minimum melee damage is not applied per tier"],
  [/ParasiteCombatEffects\.stealFood\(parasite, victim, tier\.foodSteal\(\)/,
    "food stealing is not applied per tier"]
]) expect(rules, pattern, message);

// tier resolution follows the entity id families the way InfectionMechanics does
for (const tier of ["infected", "feral", "hijacked", "assimara", "adapted", "primitive",
  "nexus_si", "nexus_sii", "nexus_siii", "nexus_siv", "ancient", "pure", "preeminent"]) {
  expect(rules, new RegExp(`"${tier}"`), `tier ${tier} is not resolvable from an entity id`);
}
expect(rules, /path\.startsWith\("sim_"\)/, "sim_* must resolve to the infected tier");
expect(rules, /path\.startsWith\("fer_"\)/, "fer_* must resolve to the feral tier");
expect(rules, /case "marauder" -> "assimara"/, "pure-tier ids must map to the legacy tiers");

// shared helpers
for (const [pattern, message] of [
  [/public static void applyMinimumDamage\(LivingEntity attacker, LivingEntity target, float base\)/,
    "applyMinimumDamage helper is missing"],
  [/base \* \(viral == null \? 1\.0F : viral\.getAmplifier\(\) \+ 2\)/,
    "minimum damage must scale with the victim's VIRA amplifier"],
  [/public static void stealFood\(LivingEntity attacker, LivingEntity victim, float exhaustion, float theftChance\)/,
    "stealFood helper is missing"],
  [/player\.causeFoodExhaustion\(exhaustion\)/, "food stealing must add player exhaustion"],
  [/stack\.has\(DataComponents\.FOOD\)/, "food stealing must look for food components"],
  [/new ItemStack\(ModItems\.ASSIMILATED_FLESH\.get\(\)\)/,
    "stolen food must drop as assimilated flesh (the original infected_drop)"],
  [/public static void healOnKill\(LivingEntity killer, LivingEntity victim\)/,
    "healOnKill helper is missing"],
  [/EvolutionSystem\.generationProfile\(serverLevel\)\.mobHealing\(\)/,
    "kill healing must use the generation profile mobHealing gene"]
]) expect(effects, pattern, message);

// legacy canBeAffected: parasites never take their own status effects
expect(status, /public static void preventParasiteStatusApplication\(MobEffectEvent\.Applicable event\)/,
  "parasite status immunity handler is missing");
for (const effect of ["COTH", "VIRAL", "CORROSION", "NEEDLER"]) {
  expect(status, new RegExp(`instance\\.is\\(ModMobEffects\\.${effect}\\)`),
    `status immunity does not cover ${effect}`);
}

// fire multiplier + 20% RAGE for the families outside the assimilated wiring
for (const [source, name] of [[primitive, "PrimitiveParasiteEntity"], [buglin, "BuglinEntity"]]) {
  expect(source, /Config\.parasiteFireMultiplier\(\)/, `${name}: fire multiplier is missing`);
  expect(source, /ModMobEffects\.RAGE, 200, 1, false, false/, `${name}: fire RAGE roll is missing`);
  expect(source, /0\.2F/, `${name}: fire RAGE chance must be 20%`);
}

// legacy applyGene: the damage cap and the minimum damage only run when the generation says so
for (const [pattern, message] of [
  [/private static boolean generationAllows\(LivingEntity parasite,/,
    "the applyGene gate helper is missing"],
  [/!generationAllows\(parasite, EvolutionSystem\.GenerationProfile::damageCap\)/,
    "the damage cap must be gated by the geneDamcap flag"],
  [/if \(generationAllows\(parasite, EvolutionSystem\.GenerationProfile::minimumDamage\)\) \{/,
    "the minimum damage must be gated by the geneMindam flag"],
  [/import alku\.csrp\.world\.EvolutionSystem;/, "the EvolutionSystem import is missing"]
]) expect(rules, pattern, message);

// geneSpecialmove also gates the sim_human leap (the goal is a vanilla one, so the check is in canUse)
const simHuman = read("src/main/java/alku/csrp/entity/SimHumanEntity.java");
expect(simHuman, /private boolean generationAllowsSpecialMoves\(\) \{[\s\S]{0,200}?specialMoves\(\)/,
  "sim_human must gate its leap on the geneSpecialmove flag");

// legacy geneBlockSearch / geneSprinting: both gate behaviour that already existed
for (const [pattern, message] of [
  [/protected final boolean blockSearchEnabled\(\) \{[\s\S]{0,200}?\.blockSearch\(\)/,
    "PrimitiveParasiteEntity.blockSearchEnabled() is missing"],
  [/if \(!canBreakBlocks\(\) \|\| !blockSearchEnabled\(\)\) \{/,
    "block breaking must be gated by the geneBlockSearch flag"],
  [/protected final boolean sprintingEnabled\(\) \{[\s\S]{0,200}?\.sprinting\(\)/,
    "PrimitiveParasiteEntity.sprintingEnabled() is missing"]
]) expect(primitive, pattern, message);

const sprintGoal = read("src/main/java/alku/csrp/entity/GeneSprintGoal.java");
for (const [pattern, message] of [
  [/public final class GeneSprintGoal extends Goal/, "the sprint goal is missing"],
  [/SPRINT_MULTIPLIER = 1\.3D/, "the legacy 1.3 sprint multiplier is missing"],
  [/SPRINT_DISTANCE_SQR = 16\.0D/, "the sprint must only apply while the target is far"],
  [/setFlags\(EnumSet\.of\(Flag\.MOVE\)\)/, "the sprint goal must claim the MOVE flag"],
  [/EvolutionSystem\.generationProfile\(serverLevel\)\.sprinting\(\)/,
    "the sprint goal must consult the geneSprinting flag"]
]) expect(sprintGoal, pattern, message);

// the three families register the sprint goal just before their melee goal, same priority
for (const [file, pattern, message] of [
  ["MarauderizedParasiteEntity.java",
    /addGoal\(\d, new GeneMeleeGoal\(this, meleeSpeed(\(\))?, false\)\)/,
    "MarauderizedParasiteEntity must drive melee through the gene-aware goal"],
  ["FeralParasiteEntity.java",
    /addGoal\(2, new GeneMeleeGoal\(this, 1\.5D, false\)\)/,
    "FeralParasiteEntity must drive melee through the gene-aware goal"],
  ["AssimilatedParasiteEntity.java",
    /addGoal\(\d, new GeneMeleeGoal\(this, meleeSpeed(\(\))?, false\)\)/,
    "AssimilatedParasiteEntity must drive melee through the gene-aware goal"],
]) expect(read("src/main/java/alku/csrp/entity/" + file), pattern, message);

// legacy geneAttackSpeed: the interval scales with the generation and the sprint gene folds in
const geneMelee = read("src/main/java/alku/csrp/entity/GeneMeleeGoal.java");
for (const [pattern, message] of [
  [/public final class GeneMeleeGoal extends Goal/, "the gene-aware melee goal is missing"],
  [/BASE_ATTACK_INTERVAL_TICKS = 20/, "the base attack interval is missing"],
  [/attackSpeedMultiplier\(\)/, "the attack cadence must follow the generation"],
  [/Math\.max\(1, Math\.round\(BASE_ATTACK_INTERVAL_TICKS \* multiplier\)\)/,
    "the interval must be scaled by the multiplier"],
  [/setFlags\(EnumSet\.of\(Flag\.MOVE, Flag\.LOOK\)\)/, "the goal must claim move and look"],
  [/mob\.isWithinMeleeAttackRange\(target\) && attackCooldown <= 0/, "attacks must respect the cadence"],
  [/sprinting && mob\.distanceToSqr\(target\) > SPRINT_DISTANCE_SQR/, "the sprint gene must still apply while the target is far"],
]) expect(geneMelee, pattern, message);

// legacy EntityAIJumping: a periodic hop when the target is above the mob
const jumping = read("src/main/java/alku/csrp/entity/JumpAtHigherTargetGoal.java");
for (const [pattern, message] of [
  [/public final class JumpAtHigherTargetGoal extends Goal/, "the jumping goal is missing"],
  [/CHECK_INTERVAL_TICKS = 10/, "the legacy ten tick cadence is missing"],
  [/target\.distanceToSqr\(mob\.getX\(\), target\.getY\(\), mob\.getZ\(\)\) >= VERTICAL_RANGE_SQR/,
    "the legacy squared-distance gate is missing"],
  [/target\.getY\(\) - \(mob\.getY\(\) \+ mob\.getEyeHeight\(\)\) <= REQUIRED_HEIGHT_DIFFERENCE/,
    "the one block height difference gate is missing"],
  [/0\.2D \+ mob\.getBbHeight\(\) \* 0\.15D/, "the legacy hop velocity is missing"]
]) expect(jumping, pattern, message);
expect(read("src/main/java/alku/csrp/entity/LongarmsEntity.java"),
  /addGoal\(5, new JumpAtHigherTargetGoal\(this\)\)/,
  "LongarmsEntity must register EntityAIJumping at priority 5");

if (failures.length) {
  console.error("Parasite combat rules verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Parasite combat rules verification passed.");
