const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const parseJson = (relative) => JSON.parse(read(relative));
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const primitive = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const burrowing = read("src/main/java/alku/csrp/entity/BurrowingVariantEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");

expect(registry, /"pri_burrower"[\s\S]*?1\.0F, 0\.25F, 0\.25F/,
  "Primitive Burrower dimensions or eye height differ from EntityZaa");
expect(registry, /"ada_burrower"[\s\S]*?1\.321F, 1\.2F, 1\.0F/,
  "Adapted Burrower dimensions or eye height differ from EntityZaaAdapted");

expect(primitive, /case BURROWER -> \{[\s\S]*?health = 45\.0D[\s\S]*?armor = 9\.0D[\s\S]*?damage = 15\.0D[\s\S]*?speed = 0\.26D[\s\S]*?knockbackResistance = 0\.70D[\s\S]*?followRange = 24\.0D/,
  "Primitive Burrower attributes do not match EntityZaa");
expect(primitive, /case BURROWER -> applyConfiguredAttributes\([\s\S]*?MobsConfig\.burrowerHealth\(\)[\s\S]*?MobsConfig\.burrowerArmor\(\)[\s\S]*?MobsConfig\.burrowerDamage\(\)[\s\S]*?MobsConfig\.burrowerKnockbackResistance\(\)/,
  "Primitive Burrower config is not applied after entity registration");
expect(adapted, /case BURROWER -> \{[\s\S]*?health = 115\.0D[\s\S]*?armor = 24\.0D[\s\S]*?damage = 45\.0D[\s\S]*?speed = 0\.32D[\s\S]*?knockbackResistance = 1\.0D[\s\S]*?followRange = 32\.0D/,
  "Adapted Burrower does not preserve EntityZaaAdapted's WYMO attribute-copy behavior");
expect(adapted, /case BURROWER, TOZOON -> applyConfiguredAttributes\([\s\S]*?MobsConfig\.adaptedTozoonHealth\(\)[\s\S]*?MobsConfig\.adaptedTozoonArmor\(\)[\s\S]*?MobsConfig\.adaptedTozoonDamage\(\)/,
  "Adapted Burrower config is not applied after entity registration");

for (const [key, value] of [
  ["primitiveBurrowerHealthMultiplier", "1.0D"],
  ["primitiveBurrowerDamageMultiplier", "1.0D"],
  ["primitiveBurrowerArmorMultiplier", "1.0D"],
  ["primitiveBurrowerKnockbackResistanceMultiplier", "1.0D"],
  ["adaptedBurrowerAdditionalHealth", "50.0D"],
  ["adaptedBurrowerAdditionalDamage", "12.0D"],
  ["adaptedBurrowerAdditionalArmor", "7.0D"],
  ["adaptedBurrowerAdditionalKnockbackResistance", "0.3D"]
]) {
  if (!new RegExp(`"${key}"\\s*,\\s*${value.replaceAll(".", "\\.")}`).test(config)) {
    failures.push(`missing original Burrower config default: ${key}`);
  }
}
expect(config, /burrowerHealth\(\)[\s\S]*?45\.0D \* BURROWER_HEALTH_MULTIPLIER/,
  "Primitive Burrower health is not connected to its original multiplier");
expect(config, /burrowerDamage\(\)[\s\S]*?15\.0D \* BURROWER_DAMAGE_MULTIPLIER/,
  "Primitive Burrower damage is not connected to its original multiplier");
expect(config, /burrowerArmor\(\)[\s\S]*?9\.0D \* BURROWER_ARMOR_MULTIPLIER/,
  "Primitive Burrower armor is not connected to its original multiplier");
if (/case BURROWER -> \{[\s\S]*?ADAPTED_BURROWER_ADDITIONAL_/.test(adapted)) {
  failures.push("Adapted Burrower incorrectly uses config values ignored by SRP 1.10.7");
}

for (const [source, label] of [[primitive, "Primitive"], [adapted, "Adapted"]]) {
  expect(source, /kind == Kind\.BURROWER \|\| kind == Kind\.TOZOON[\s\S]*?setPathfindingMalus\(PathType\.WATER, -1\.0F\)/,
    `${label} Burrower water path malus is missing`);
  expect(source, /kind == Kind\.BURROWER \|\| kind == Kind\.TOZOON[\s\S]*?attributes\.add\(Attributes\.STEP_HEIGHT, 1\.0D\)/,
    `${label} Burrower one-block step height is missing`);
  expect(source, /case BURROWER -> \{[\s\S]*?createBurrowMovementGoal\(\)[\s\S]*?new BurrowerMeleeGoal\(\)/,
    `${label} Burrower goals do not use the original digging and melee behavior`);
  expect(source, /class BurrowerMeleeGoal extends MeleeAttackGoal[\s\S]*?1\.30D, false[\s\S]*?getTicksUntilNextAttack\(\)[\s\S]*?return 10/,
    `${label} Burrower melee speed or ten-tick attack interval is wrong`);
}

expect(primitive, /bodyFollowDistance\(\)[\s\S]*?case BURROWER -> 1\.75D/,
  "Primitive Burrower body follow distance is not 1.75 blocks");
expect(adapted, /bodyFollowDistance\(\)[\s\S]*?Kind\.BURROWER \|\| kind == Kind\.TOZOON \? 1\.9D/,
  "Adapted Burrower body follow distance is not 1.9 blocks");
expect(primitive, /bodySegmentCount\(\)[\s\S]*?Kind\.BURROWER \|\| kind == Kind\.TOZOON \? 2 : 0/,
  "Primitive Burrower does not create two body segments");
expect(adapted, /bodySegmentCount\(\)[\s\S]*?Kind\.BURROWER \|\| kind == Kind\.TOZOON \? 4 : 0/,
  "Adapted Burrower does not create four body segments");
expect(primitive, /burrowSkillCooldownTicks\(\)[\s\S]*?Kind\.BURROWER \? 140 : 200/,
  "Primitive Burrower digging cooldown is not 140 ticks");
expect(adapted, /burrowSkillCooldownTicks\(\)[\s\S]*?Kind\.BURROWER \? 80 : 140/,
  "Adapted Burrower digging cooldown is not 80 ticks");
expect(adapted, /bodyPartEffect\(\)[\s\S]*?activeKind\(\) != Kind\.TOZOON/,
  "Adapted Burrower incorrectly gained a body-segment effect");

expect(burrowing, /previous\.hurt\(source, amount \* 0\.5F\)/,
  "Burrower body damage does not propagate 50 percent to its predecessor");
expect(burrowing, /protected boolean canBreakBlocks\(\)[\s\S]*?getBodyNumber\(\) == 0/,
  "Burrower body segments can break blocks");
expect(burrowing, /public void setTarget\(LivingEntity target\)[\s\S]*?getBodyNumber\(\) == 0/,
  "Burrower body segments can acquire targets");
expect(burrowing, /killedEntity\(ServerLevel level, LivingEntity victim\)[\s\S]*?head\.killedEntity\(level, victim\)/,
  "Burrower body kills are not credited to the head");

const dropSpecs = [
  {
    id: "pri_burrower",
    lure: "csrp:lurecomponent3",
    lureChance: 0.6,
    lureMin: 1,
    lureMax: 2,
    boneChance: 0.1,
    boneMin: 1,
    boneMax: 1
  },
  {
    id: "ada_burrower",
    lure: "csrp:lurecomponent4",
    lureChance: 0.8,
    lureMin: 1,
    lureMax: 1,
    boneChance: 0.2,
    boneMin: 1,
    boneMax: 2
  }
];
for (const spec of dropSpecs) {
  for (const relative of [
    `src/main/resources/data/csrp/loot_table/entities/${spec.id}.json`,
    `src/main/resources/assets/csrp/compendium/drops/${spec.id}.json`
  ]) {
    const table = parseJson(relative);
    if (!Array.isArray(table.pools) || table.pools.length !== 2) {
      failures.push(`${relative}: expected two effective original drop pools`);
      continue;
    }
    const [lure, bone] = table.pools;
    const lureCount = lure.entries?.[0]?.functions?.[0]?.count;
    const actualLureMin = lureCount?.min ?? 1;
    const actualLureMax = lureCount?.max ?? 1;
    if (lure.conditions?.length !== 1
        || lure.conditions[0]?.condition !== "minecraft:random_chance"
        || lure.conditions[0]?.chance !== spec.lureChance
        || lure.entries?.[0]?.name !== spec.lure
        || actualLureMin !== spec.lureMin || actualLureMax !== spec.lureMax) {
      failures.push(`${relative}: lure-component candidate pool differs from SRP 1.10.7`);
    }
    const boneCount = bone.entries?.[0]?.functions?.[0]?.count;
    const actualBoneMin = boneCount?.min ?? 1;
    const actualBoneMax = boneCount?.max ?? 1;
    if (bone.conditions?.length !== 1
        || bone.conditions[0]?.condition !== "minecraft:random_chance"
        || bone.conditions[0]?.chance !== spec.boneChance
        || bone.entries?.[0]?.name !== "csrp:bone"
        || actualBoneMin !== spec.boneMin || actualBoneMax !== spec.boneMax) {
      failures.push(`${relative}: independent bone pool differs from SRP 1.10.7`);
    }
    const serialized = JSON.stringify(table);
    if (serialized.includes("minecraft:killed_by_player")) {
      failures.push(`${relative}: legacy independent drops were incorrectly gated behind a player kill`);
    }
    if (serialized.includes("ada_burrower_drop")) {
      failures.push(`${relative}: unresolved legacy ada_burrower_drop invalidates the modern loot table`);
    }
  }
}
if (items.includes('simple("ada_burrower_drop")')) {
  failures.push("fabricated ada_burrower_drop item was registered even though SRP 1.10.7 has no such item");
}

if (failures.length) {
  console.error(`Burrower verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Primitive and Adapted Burrower behavior matches the SRP 1.10.7 implementation.");
