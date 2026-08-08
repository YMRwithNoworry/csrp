const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const parseJson = (relative) => JSON.parse(read(relative));
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const entity = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
const burrowing = read("src/main/java/alku/csrp/entity/BurrowingVariantEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");

expect(registry, /"pri_tozoon"[\s\S]*?0\.978F, 1\.2F/, "Tozoon registry dimensions are wrong");
expect(entity, /case TOZOON -> \{[\s\S]*?health = 45\.0D[\s\S]*?armor = 9\.0D[\s\S]*?damage = 15\.0D[\s\S]*?speed = 0\.26D[\s\S]*?knockbackResistance = 1\.0D[\s\S]*?followRange = 24\.0D/, "Primitive Tozoon attributes are incomplete");
expect(entity, /case TOZOON -> applyConfiguredAttributes\([\s\S]*?MobsConfig\.tozoonHealth\(\)[\s\S]*?MobsConfig\.tozoonArmor\(\)[\s\S]*?MobsConfig\.tozoonDamage\(\)[\s\S]*?MobsConfig\.tozoonKnockbackResistance\(\)/,
  "Primitive Tozoon config is not applied after entity registration");
expect(entity, /goalSelector\.addGoal\(2, new TozoonAoeAttackGoal\(\)\)/, "Tozoon does not use the dedicated AOE attack goal");
expect(entity, /TozoonAoeAttackGoal[\s\S]*?ATTACK_INTERVAL_TICKS = 10[\s\S]*?ATTACK_DISTANCE_SQR = 9\.0D[\s\S]*?getNavigation\(\)\.moveTo\(target, 1\.3D\)/, "Tozoon AOE goal timing or movement is wrong");
expect(entity, /new AABB\(target\.getX\(\), target\.getY\(\), target\.getZ\(\)[\s\S]*?inflate\(1\.5D\)/, "Tozoon attack AABB is missing");
expect(entity, /ModSounds\.MOB_SWIPE\.get\(\)/, "Tozoon attack sound is missing");
expect(entity, /triggerAnim\("attack_controller", "get_attack_timer"\)/, "Tozoon attack animation trigger is missing");
if (/case TOZOON -> target\.addEffect\(new MobEffectInstance\(MobEffects\.MOVEMENT_SLOWDOWN/.test(entity)) {
  failures.push("Primitive Tozoon still applies the fabricated slowness effect");
}

expect(entity, /protected void bodyPartEffect\(\)[\s\S]*?new AABB\(blockPosition\(\)\)\.inflate\(3\.0D\)/,
  "Tozoon body segment AOE is missing");
expect(entity, /protected double bodyFollowDistance\(\)[\s\S]*?case TOZOON -> 1\.7D/,
  "Tozoon body follow distance is wrong");
expect(burrowing, /previous\.hurt\(source, amount \* 0\.5F\)/,
  "burrowing body damage does not propagate to the predecessor");
expect(burrowing, /entityData\.get\(BODY_ATTACK_TICKS\) > 0[\s\S]*?entityData\.set\(BODY_ATTACK_TICKS, entityData\.get\(BODY_ATTACK_TICKS\) - 1/,
  "burrowing body attack timers are not ticked independently");
if (/entityData\.set\(BODY_ATTACK_TICKS, previous\.entityData\.get\(BODY_ATTACK_TICKS\)\)/.test(burrowing)) {
  failures.push("burrowing body attack timers still mirror the predecessor");
}
expect(burrowing, /protected boolean canBreakBlocks\(\)[\s\S]*?getBodyNumber\(\) == 0/,
  "burrowing body segments can still break blocks");

for (const [key, value] of [
  ["primitiveTozoonHealthMultiplier", "1.0D"],
  ["primitiveTozoonDamageMultiplier", "1.0D"],
  ["primitiveTozoonArmorMultiplier", "1.0D"],
  ["primitiveTozoonKnockbackResistanceMultiplier", "1.0D"]
]) {
  if (!new RegExp(`"${key}"\\s*,\\s*${value.replaceAll(".", "\\.")}`).test(config)) {
    failures.push(`missing Primitive Tozoon config default: ${key}`);
  }
}

for (const relative of [
  "src/main/resources/data/csrp/loot_table/entities/pri_tozoon.json",
  "src/main/resources/assets/csrp/compendium/drops/pri_tozoon.json"
]) {
  const table = parseJson(relative);
  if (!Array.isArray(table.pools) || table.pools.length !== 2) {
    failures.push(`${relative}: expected the two resolvable original drop pools`);
    continue;
  }
  const [lure, bone] = table.pools;
  if (lure.conditions?.[0]?.condition !== "minecraft:random_chance"
      || lure.conditions[0].chance !== 0.4
      || lure.entries?.[0]?.name !== "csrp:lurecomponent3"
      || lure.entries[0].functions?.[0]?.count?.min !== 1
      || lure.entries[0].functions?.[0]?.count?.max !== 2) {
    failures.push(`${relative}: lurecomponent3 pool is not the original 40% 1-2 drop`);
  }
  if (bone.conditions?.length !== 1
      || bone.conditions[0]?.condition !== "minecraft:random_chance"
      || bone.conditions[0]?.chance !== 0.1
      || bone.entries?.[0]?.name !== "csrp:bone") {
    failures.push(`${relative}: bone pool is not the original independent 10% drop`);
  }
  if (JSON.stringify(table).includes("minecraft:killed_by_player")) {
    failures.push(`${relative}: legacy independent drops were incorrectly gated behind a player kill`);
  }
  if (JSON.stringify(table).includes("ada_tozoon_drop")) {
    failures.push(`${relative}: unresolved legacy ada_tozoon_drop still invalidates the modern loot table`);
  }
}

if (failures.length) {
  console.error(`Primitive Tozoon verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Primitive Tozoon original behavior is wired and verified.");
