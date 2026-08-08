const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const parseJson = (relative) => JSON.parse(read(relative));
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const entity = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const burrowing = read("src/main/java/alku/csrp/entity/BurrowingVariantEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");

expect(registry, /"ada_tozoon"[\s\S]*?1\.321F, 1\.2F/,
  "Adapted Tozoon registry dimensions are wrong");
expect(entity, /case TOZOON -> \{[\s\S]*?MobsConfig\.adaptedTozoonHealth\(\)[\s\S]*?MobsConfig\.adaptedTozoonArmor\(\)[\s\S]*?MobsConfig\.adaptedTozoonDamage\(\)[\s\S]*?speed = 0\.32D[\s\S]*?knockbackResistance = 1\.0D[\s\S]*?followRange = 32\.0D/,
  "Adapted Tozoon attributes do not match EntityWymoAdapted");
expect(entity, /kind == Kind\.TOZOON[\s\S]*?attributes\.add\(Attributes\.STEP_HEIGHT, 1\.0D\)/,
  "Adapted Tozoon one-block step height is missing");
expect(entity, /kind == Kind\.TOZOON[\s\S]*?setPathfindingMalus\(PathType\.WATER, -1\.0F\)/,
  "Adapted Tozoon water path malus is wrong");
for (const [key, value] of [
  ["adaptedTozoonAdditionalHealth", "70.0D"],
  ["adaptedTozoonAdditionalDamage", "30.0D"],
  ["adaptedTozoonAdditionalArmor", "15.0D"],
  ["adaptedTozoonAdditionalKnockbackResistance", "0.65D"]
]) {
  if (!new RegExp(`"${key}"\\s*,\\s*${value.replaceAll(".", "\\.")}`).test(config)) {
    failures.push(`missing Adapted Tozoon config default: ${key}`);
  }
}
expect(config, /adaptedTozoonHealth\(\)[\s\S]*?45\.0D \+ ADAPTED_TOZOON_ADDITIONAL_HEALTH[\s\S]*?TOZOON_HEALTH_MULTIPLIER/,
  "Adapted Tozoon health does not combine primitive and adapted values");
expect(config, /adaptedTozoonDamage\(\)[\s\S]*?15\.0D \+ ADAPTED_TOZOON_ADDITIONAL_DAMAGE[\s\S]*?TOZOON_DAMAGE_MULTIPLIER/,
  "Adapted Tozoon damage does not combine primitive and adapted values");
expect(config, /adaptedTozoonArmor\(\)[\s\S]*?9\.0D \+ ADAPTED_TOZOON_ADDITIONAL_ARMOR[\s\S]*?TOZOON_ARMOR_MULTIPLIER/,
  "Adapted Tozoon armor does not combine primitive and adapted values");

expect(entity, /case TOZOON -> \{[\s\S]*?goalSelector\.addGoal\(1, createBurrowMovementGoal\(\)\)[\s\S]*?goalSelector\.addGoal\(2, new TozoonAoeAttackGoal\(\)\)/,
  "Adapted Tozoon does not use its original AOE attack goal");
expect(entity, /TozoonAoeAttackGoal[\s\S]*?ATTACK_INTERVAL_TICKS = 10[\s\S]*?ATTACK_DISTANCE_SQR = 16\.0D[\s\S]*?getNavigation\(\)\.moveTo\(target, 1\.3D\)/,
  "Adapted Tozoon AOE timing, range, or speed is wrong");
expect(entity, /performTozoonAoeAttack\(Entity target\)[\s\S]*?startBodyAttackAnimation\(\)[\s\S]*?ModSounds\.MOB_SWIPE\.get\(\)[\s\S]*?new AABB\(target\.getX\(\), target\.getY\(\), target\.getZ\(\)[\s\S]*?inflate\(1\.5D\)[\s\S]*?super\.doHurtTarget\(nearby\)/,
  "Adapted Tozoon AOE damage implementation is incomplete");
expect(entity, /triggerableAnim\("get_attack_timer", TOZOON_ATTACK\)/,
  "Adapted Tozoon attack controller is missing");
expect(entity, /public boolean doHurtTarget\(Entity entity\)[\s\S]*?activeKind == Kind\.TOZOON[\s\S]*?performTozoonAoeAttack\(entity\)/,
  "Adapted Tozoon direct attacks bypass the AOE implementation");

expect(entity, /bodySegmentCount\(\)[\s\S]*?kind == Kind\.BURROWER \|\| kind == Kind\.TOZOON \? 4 : 0/,
  "Adapted Tozoon does not create four body segments");
expect(entity, /bodyFollowDistance\(\)[\s\S]*?Kind\.BURROWER \|\| kind == Kind\.TOZOON \? 1\.9D/,
  "Adapted Tozoon body follow distance is wrong");
expect(entity, /shouldTriggerBodyPartEffect\(\)[\s\S]*?body >= 1 && body <= 3/,
  "Adapted Tozoon body AOE is not enabled for segments one through three");
expect(entity, /bodyPartEffect\(\)[\s\S]*?new AABB\(blockPosition\(\)\)\.inflate\(5\.0D\)[\s\S]*?performTozoonAoeAttack\(target\)/,
  "Adapted Tozoon body-segment AOE is missing");
expect(burrowing, /shouldTriggerBodyPartEffect\(\) && tickCount % 21 == 10/,
  "body-segment effect cadence is not the original 21-tick cycle");
expect(burrowing, /previous\.hurt\(source, amount \* 0\.5F\)/,
  "Adapted Tozoon body damage does not propagate 50 percent to its predecessor");
expect(burrowing, /bodyBurrowCycles - 1 >= getBodyNumber\(\)[\s\S]*?return false/,
  "burrowed body immunity is not staged by the original body counter");
if (/if \(isFullyBurrowed\(\)\)[\s\S]{0,80}return false/.test(burrowing)) {
  failures.push("burrowing head incorrectly becomes invulnerable while underground");
}
expect(burrowing, /bodyBurrowCycles - 1 >= getBodyNumber\(\) \? 0\.2D : bodyFollowDistance\(\)/,
  "fully sunk body segments do not close to the original 0.2-block follow distance");
expect(burrowing, /protected boolean canBreakBlocks\(\)[\s\S]*?getBodyNumber\(\) == 0/,
  "Adapted Tozoon body segments can still break blocks");
expect(entity, /kind == Kind\.TOZOON && isBodyAttackAnimating\(\)[\s\S]*?BODY_ATTACK\[body\]/,
  "Adapted Tozoon head and body attack animation is not driven by actual attacks");
expect(entity, /burrowSkillCooldownTicks\(\)[\s\S]*?Kind\.BURROWER \? 80 : 140/,
  "Adapted Tozoon digging cooldown is not 140 ticks");
expect(burrowing, /SIGNAL_INTERVAL_TICKS = 21[\s\S]*?bodySegmentCount\(\) \+ 3\) \* SIGNAL_INTERVAL_TICKS - DIVE_TICKS/,
  "burrowing duration is not derived from the original body length and 21-tick signal");

for (const relative of [
  "src/main/resources/data/csrp/loot_table/entities/ada_tozoon.json",
  "src/main/resources/assets/csrp/compendium/drops/ada_tozoon.json"
]) {
  const table = parseJson(relative);
  if (!Array.isArray(table.pools) || table.pools.length !== 2) {
    failures.push(`${relative}: expected the two resolvable original drop pools`);
    continue;
  }
  const [lure, bone] = table.pools;
  const lureCount = lure.entries?.[0]?.functions?.[0]?.count;
  if (lure.conditions?.[0]?.condition !== "minecraft:random_chance"
      || lure.conditions[0].chance !== 0.6
      || lure.entries?.[0]?.name !== "csrp:lurecomponent4"
      || lureCount?.min !== 1 || lureCount?.max !== 3) {
    failures.push(`${relative}: lurecomponent4 pool is not the original 60% 1-3 drop`);
  }
  const boneCount = bone.entries?.[0]?.functions?.[0]?.count;
  if (bone.conditions?.length !== 1
      || bone.conditions[0]?.condition !== "minecraft:random_chance"
      || bone.conditions[0]?.chance !== 0.2
      || bone.entries?.[0]?.name !== "csrp:bone"
      || boneCount?.min !== 1 || boneCount?.max !== 3) {
    failures.push(`${relative}: bone pool is not the original independent 20% 1-3 drop`);
  }
  if (JSON.stringify(table).includes("minecraft:killed_by_player")) {
    failures.push(`${relative}: legacy independent drops were incorrectly gated behind a player kill`);
  }
  if (JSON.stringify(table).includes("ada_tozoon_drop")) {
    failures.push(`${relative}: unresolved legacy ada_tozoon_drop still invalidates the modern loot table`);
  }
}

if (failures.length) {
  console.error(`Adapted Tozoon verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Adapted Tozoon original behavior is wired and verified.");
