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

// legacy geneSpecialmove: the shared gate used by skill goals
expect(primitive, /protected final boolean specialMovesEnabled\(\) \{[\s\S]{0,200}?\.specialMoves\(\)/,
  "PrimitiveParasiteEntity.specialMovesEnabled() is missing");

// legacy EntityAISkill: the shared dispatch contract (gate + distance window + cadence)
const skillGoal = read("src/main/java/alku/csrp/entity/ParasiteSkillGoal.java");
for (const [pattern, message] of [
  [/public final class ParasiteSkillGoal extends Goal/, "the skill dispatch goal is missing"],
  [/interface ParasiteSkill \{/, "the doSpecialSkill contract is missing"],
  [/void tick\(\);/, "the skill contract must expose the per-tick step"],
  [/boolean isFinished\(\);/, "the skill contract must expose the legacy getFinished"],
  [/upperDistanceSqr = \(double\) minDistance \* minDistance/, "distances must be squared like the original"],
  [/if \(distance >= upperDistanceSqr \|\| \(lowerDistanceSqr > 0\.0D && distance < lowerDistanceSqr\)\)/, "the legacy distanceC/distanceL window must be honoured"],
  [/parasite\.specialMovesEnabled\(\)/, "the skill must be gated by geneSpecialmove"],
  [/!needVisual \|\| mob\.getSensing\(\)\.hasLineOfSight\(target\)/, "needVisual must require line of sight"],
  [/if \(attackTimer < cooldownTicks\)/, "the legacy cooldown warm-up is missing"]
]) expect(skillGoal, pattern, message);

// legacy EntityAISkill(this, 80, 4, false, 21): LongarmsEntity drives its orb through the contract
const longarmsSource = read("src/main/java/alku/csrp/entity/LongarmsEntity.java");
expect(longarmsSource, /addGoal\(2, new ParasiteSkillGoal\(this, 21, new ScaryOrbSkill\(\), 80, 4, false\)\)/,
  "LongarmsEntity must register the legacy attackID 21 skill at priority 2 with (80, 4, false)");
expect(longarmsSource, /applyScaryOrbEffect\(target, 0\)/,
  "the skill must trigger the existing scary orb effect");
expect(longarmsSource, /implements ParasiteSkillGoal\.ParasiteSkill/,
  "the skill must implement the dispatch contract");

// legacy EntityInfCow:75 EntityAISkill(this, 60, 32, 8, true, 1): the cow charge is gene gated
const assimilated = read("src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java");
for (const [pattern, message] of [
  [/private boolean specialMovesEnabled\(\) \{[\s\S]{0,200}?\.specialMoves\(\)/,
    "the assimilated family needs the geneSpecialmove gate"],
  [/&& specialMovesEnabled\(\)/, "the cow charge must be gated by geneSpecialmove"],
  [/distance >= 64\.0D && distance < 1024\.0D/, "the legacy 8-32 block window is missing"],
  [/chargeCooldown = 60;/, "the legacy 60 tick cooldown is missing"]
]) expect(assimilated, pattern, message);

// legacy finalizeSpawn stat bonus: phase >= threshold scales health, armor and attack
for (const [pattern, message] of [
  [/public static void applyPhaseStatBonus\(FinalizeSpawnEvent event\)/, "the phase stat bonus handler is missing"],
  [/\.evolutionPhase\(\) < Config\.evolutionStatIncreasePhase\(\)/, "the phase threshold must gate the bonus"],
  [/scaleBaseAttribute\(parasite, Attributes\.MAX_HEALTH, multiplier\)/, "max health must be scaled"],
  [/scaleBaseAttribute\(parasite, Attributes\.ARMOR, multiplier\)/, "armor must be scaled"],
  [/scaleBaseAttribute\(parasite, Attributes\.ATTACK_DAMAGE, multiplier\)/, "attack damage must be scaled"]
]) expect(rules, pattern, message);
expect(config, /defineInRange\("evolutionStatIncreasePhase", 10, 0, 100\)/, "the legacy phase threshold (10) is missing");
expect(config, /defineInRange\("evolutionStatIncreaseValue", 0\.07D, 0\.0D, 10\.0D\)/, "the legacy +7% value is missing");

// legacy doLast: SPOT on the target plus waking nearby parasites
for (const [pattern, message] of [
  [/public static void markSpottedTarget\(LivingChangeTargetEvent event\)/, "the doLast handler is missing"],
  [/nearestInfectionPosition\(parasite\.blockPosition\(\)\) == null/, "the infection position gate is missing"],
  [/ModMobEffects\.SPOTTED, SPOT_DURATION_TICKS/, "the legacy 1200 tick SPOT is missing"],
  [/private static void alertOthers\(ServerLevel level, LivingEntity parasite, LivingEntity target\)/,
    "the alertOthers helper is missing"],
  [/ALERT_OTHERS_RADIUS = 7\.0D/, "the legacy 7 block alert radius is missing"]
]) expect(rules, pattern, message);
expect(read("src/main/java/alku/csrp/world/SrpWorldData.java"),
  /public BlockPos nearestInfectionPosition\(BlockPos origin\)/,
  "SrpWorldData.nearestInfectionPosition is missing");

// legacy EntityAISwimmingDiving: dive after a submerged target, otherwise keep stroking
const diving = read("src/main/java/alku/csrp/entity/SwimmingDivingGoal.java");
for (const [pattern, message] of [
  [/public final class SwimmingDivingGoal extends Goal/, "the swimming diving goal is missing"],
  [/DIVE_RANGE_SQR = 25\.0D/, "the legacy squared range gate is missing"],
  [/target\.getY\(\) - mob\.getY\(\) < -DIVE_HEIGHT_DIFFERENCE/, "the diving height gate is missing"],
  [/subtract\(0\.0D, diveMotion, 0\.0D\)/, "the dive must sink by yMotion"],
  [/STROKE_CHANCE = 0\.8F/, "the legacy 80% stroke chance is missing"],
  [/mob\.getJumpControl\(\)\.jump\(\)/, "the stroke must use the jump control"]
]) expect(diving, pattern, message);
for (const file of ["AssimilatedParasiteEntity.java", "FeralParasiteEntity.java", "SimHumanEntity.java"]) {
  if (!/addGoal\(0, new SwimmingDivingGoal\(this, 0\.08D\)\)/.test(read("src/main/java/alku/csrp/entity/" + file))) {
    failures.push(file + " must register the legacy diving task at priority 0 with 0.08");
  }
}

// legacy EntityAIGetFollowers(this, 1, 16): recruit one leaderless parasite nearby
// (EntityInfHuman registers the same task one priority lower)
expect(read("src/main/java/alku/csrp/entity/SimHumanEntity.java"),
  /addGoal\(5, new RecruitFollowersGoal\(this, 16\)\)/,
  "SimHumanEntity must register the legacy recruit task at priority 5");
const recruit = read("src/main/java/alku/csrp/entity/RecruitFollowersGoal.java");
for (const [pattern, message] of [
  [/public final class RecruitFollowersGoal extends Goal/, "the recruit goal is missing"],
  [/CHECK_INTERVAL_TICKS = 20/, "the legacy twenty tick cadence is missing"],
  [/SEARCH_HEIGHT = 2\.0D/, "the legacy search box height is missing"],
  [/ParasiteFollowGoal\.getLeader\(leader\) == null/, "the leaderless gate is missing"],
  [/ParasiteFollowGoal\.setLeader\(candidate, leader\)/, "the recruit must assign the leader"],
  [/leader\.hasLineOfSight\(mob\)/, "line of sight must be required"]
]) expect(recruit, pattern, message);
for (const file of ["AssimilatedParasiteEntity.java", "FeralParasiteEntity.java"]) {
  if (!/addGoal\(6, new RecruitFollowersGoal\(this, 16\)\)/.test(read("src/main/java/alku/csrp/entity/" + file))) {
    failures.push(file + " must register the legacy recruit task at priority 6 with range 16");
  }
}

// legacy EntityAIGetFollowers version 3: steal followers from low ranking leaders
const recruitV3 = read("src/main/java/alku/csrp/entity/RecruitFollowersGoal.java");
expect(recruitV3, /STEAL_LEADER_RANK = 40/, "the legacy version 3 steal ceiling is missing");
expect(recruitV3, /public RecruitFollowersGoal\(Mob leader, int searchRange, int version\)/,
  "the versioned constructor is missing");
expect(recruitV3, /version < 3 \|\| ParasiteFollowGoal\.commandRank\(existing\) > STEAL_LEADER_RANK/,
  "version 3 must gate the steal on the leader rank");

// legacy EntityHiSkeleton:52 tasks.addTask(6, EntityAIGetFollowers(this, 1, 16))
expect(read("src/main/java/alku/csrp/entity/HiSkeletonEntity.java"),
  /addGoal\(6, new RecruitFollowersGoal\(this, 16\)\)/,
  "HiSkeletonEntity must register the legacy recruit task at priority 6");

// legacy EntityAIAttackProjectile(this, 60, 15, 3): charge sixty ticks then three web balls
const variant = read("src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java");
for (const [pattern, message] of [
  [/if \(kind == Kind\.BIGSPIDER\) \{/, "the spider must drive the legacy projectile task"],
  [/private void tickWebBallVolley\(\)/, "the legacy projectile task body is missing"],
  [/WEB_CHARGE_TICKS = 60/, "the legacy 60 tick charge is missing"],
  [/WEB_VOLLEY_INTERVAL_TICKS = 15/, "the legacy 15 tick volley interval is missing"],
  [/WEB_VOLLEY_SHOTS = 3/, "the legacy three shot volley is missing"],
  [/WEB_RANGE_SQR = 4225\.0D/, "the legacy 65 block range gate is missing"],
  [/fireWebBall\(target\);/, "the web ball shot is missing"],
  [/webVolleyShots--;/, "the volley must consume shots"]
]) expect(variant, pattern, message);

// legacy func_75246_d: RAGE doubles the projectile charge rate
expect(read("src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java"),
  /hasEffect\(ModMobEffects\.RAGE\)\) \{\s*\r?\n\s*rangedCooldown\+\+;/,
  "the RAGE charge doubling is missing");

// legacy func_75246_d: the projectile sound plays ten ticks before the shot
for (const [pattern, message] of [
  [/WEB_SOUND_LEAD_TICKS = 10/, "the legacy sound lead time is missing"],
  [/playSound\(ModSounds\.MOB_SHOOT\.get\(\), getSoundVolume\(\), getVoicePitch\(\)\)/,
    "the projectile telegraph sound is missing"],
  [/!webChargeCued && rangedCooldown >= WEB_CHARGE_TICKS - WEB_SOUND_LEAD_TICKS/,
    "the telegraph must fire once at the legacy lead time"]
]) expect(variant, pattern, message);

// legacy SRPConfigMobs per-mob attribute multipliers (default 1.0F in the original)
const mobsConfig = read("src/main/java/alku/csrp/config/MobsConfig.java");
for (const key of ["dorpaHealthMultiplier", "dorpaDamageMultiplier", "dorpaArmorMultiplier",
  "dorpaKDResistanceMultiplier"]) {
  if (!mobsConfig.includes(key)) failures.push(`MobsConfig is missing ${key}`);
}

// legacy SRPConfigMobs per-mob multipliers must actually be read
const spawnMobs = read("src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java");
for (const [pattern, message] of [
  [/boolean dorpa = kind == Kind\.BIGSPIDER;/, "the dorpa multiplier branch is missing"],
  [/kind\.maxHealth \* health/, "max health must use the per-mob multiplier"],
  [/kind\.armor \* armor/, "armor must use the per-mob multiplier"],
  [/kind\.attackDamage \* damage/, "attack damage must use the per-mob multiplier"],
  [/Math\.min\(1\.0D, kind\.knockbackResistance \* knockback\)/, "knockback must use the per-mob multiplier and stay clamped"]
]) expect(spawnMobs, pattern, message);
expect(mobsConfig, /public static double dorpaHealthMultiplier\(\)/, "the dorpa accessors are missing");

// legacy SRPConfigMobs.infcow* multipliers are read by the assimilated cow
const assimilatedSpawn = read("src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java");
for (const [pattern, message] of [
  [/boolean cow = kind == Kind\.COW;/, "the infcow multiplier branch is missing"],
  [/MobsConfig\.infcowHealthMultiplier\(\)/, "the cow health multiplier is not read"],
  [/MobsConfig\.infcowDamageMultiplier\(\)/, "the cow damage multiplier is not read"],
  [/MobsConfig\.infcowArmorMultiplier\(\)/, "the cow armor multiplier is not read"],
  [/MobsConfig\.infcowKnockbackMultiplier\(\)/, "the cow knockback multiplier is not read"]
]) expect(assimilatedSpawn, pattern, message);

// legacy SRPConfigMobs.infsheep* / infwolf* multipliers are read too
for (const [pattern, message] of [
  [/boolean sheep = kind == Kind\.SHEEP;/, "the sheep multiplier branch is missing"],
  [/boolean wolf = kind == Kind\.WOLF;/, "the wolf multiplier branch is missing"],
  [/MobsConfig\.infsheepHealthMultiplier\(\)/, "the sheep health multiplier is not read"],
  [/MobsConfig\.infwolfDamageMultiplier\(\)/, "the wolf damage multiplier is not read"]
]) expect(assimilatedSpawn, pattern, message);
expect(mobsConfig, /public static double infsheepArmorMultiplier\(\)/, "the sheep accessors are missing");
expect(mobsConfig, /public static double infwolfKnockbackMultiplier\(\)/, "the wolf accessors are missing");

if (failures.length) {
  console.error("Parasite combat rules verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Parasite combat rules verification passed.");
