const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const entity = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
const shared = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const model = read("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");

expect(registry,
  /"pri_devourer", \(type, level\) -> new PrimitiveVariantEntity\(type, level,[\s\S]*?Kind\.DEVOURER\)/,
  "registered Primitive Devourer does not use the audited implementation");
expect(entity,
  /case DEVOURER -> \{[\s\S]*?health = 60\.0D[\s\S]*?armor = 4\.0D[\s\S]*?damage = 20\.0D[\s\S]*?speed = 0\.0D[\s\S]*?knockbackResistance = 1\.0D[\s\S]*?followRange = 24\.0D/,
  "Primitive Devourer original attributes are missing");
expect(entity,
  /case DEVOURER -> applyConfiguredAttributes\([\s\S]*?MobsConfig\.devourerHealth\(\)[\s\S]*?MobsConfig\.devourerArmor\(\)[\s\S]*?MobsConfig\.devourerDamage\(\)[\s\S]*?MobsConfig\.devourerKnockbackResistance\(\)/,
  "Primitive Devourer config is not applied after entity registration");
expect(entity, /kind == Kind\.DEVOURER[\s\S]*?xpReward = 1 \+ random\.nextInt\(3\)/,
  "Primitive Devourer original 1-3 experience reward is missing");

expect(entity, /moveControl = new DevourerMoveControl\(this\)/,
  "Primitive Devourer original inertia move control is missing");
expect(entity, /class DevourerMoveControl extends MoveControl[\s\S]*?getDeltaMovement\(\)\.scale\(0\.5D\)[\s\S]*?0\.05D \* speedModifier/,
  "Primitive Devourer inertia or arrival damping differs from the original");
expect(entity, /case DEVOURER -> \{[\s\S]*?new DevourerAttackGoal\(\)[\s\S]*?new DevourerRandomSwimGoal\(\)/,
  "Primitive Devourer dedicated attack or random-swim goal is missing");
if (/new TryFindWaterGoal|new RandomSwimmingGoal|new DevourerMeleeGoal/.test(entity)) {
  failures.push("Primitive Devourer still uses generic pathing that can intentionally walk ashore");
}
expect(entity, /usesDefaultMovementGoals\(\)[\s\S]*?activeKind\(\) != Kind\.DEVOURER/,
  "Primitive Devourer still inherits ground wander/follow goals");
expect(shared, /if \(usesDefaultMovementGoals\(\)\)[\s\S]*?WaterAvoidingRandomStrollGoal/,
  "shared movement-goal gate is not applied");
expect(entity, /class DevourerRandomSwimGoal extends Goal[\s\S]*?random\.nextInt\(7\) == 0[\s\S]*?attempt < 3[\s\S]*?FluidTags\.WATER[\s\S]*?0\.19D/,
  "Primitive Devourer original water-only random movement is missing");
expect(entity, /case 2 -> origin\.offset\(random\.nextInt\(6\) - 2, random\.nextInt\(7\) - 2/,
  "Primitive Devourer long-range swim pattern differs from the original");
expect(entity, /case 3 -> origin\.offset\(random\.nextInt\(4\) \+ 3, random\.nextInt\(5\) \+ 4/,
  "Primitive Devourer close-range swim pattern differs from the original");

expect(entity, /class DevourerAttackGoal extends Goal[\s\S]*?TRACKING_FACTOR = 0\.85D[\s\S]*?SWIM_ACCELERATION = 0\.08D/,
  "Primitive Devourer original tracking and swim acceleration are missing");
expect(entity, /target\.getY\(\) >= getY\(\) \+ 3\.0D \? 0\.52D : -0\.2D[\s\S]*?target\.getY\(\) >= getY\(\) \+ 1\.0D[\s\S]*?verticalMotion -= 0\.2D/,
  "Primitive Devourer original vertical attack steering is missing");
expect(entity, /ATTACK_DISTANCE_SQR = 16\.0D[\s\S]*?attackCooldown = 20[\s\S]*?doHurtTarget\(target\)/,
  "Primitive Devourer original melee reach or cadence is missing");
expect(entity, /case DEVOURER -> target\.setDeltaMovement\(target\.getDeltaMovement\(\)\.add\(0\.0D, -0\.5645D, 0\.0D\)\)/,
  "Primitive Devourer attack no longer drags its victim downward");

expect(entity, /setNoGravity\(inWater\)[\s\S]*?setAirSupply\(getMaxAirSupply\(\)\)[\s\S]*?getAirSupply\(\) <= -20[\s\S]*?damageSources\(\)\.drown\(\), 2\.0F/,
  "Primitive Devourer original aquatic gravity or suffocation damage is missing");
expect(entity, /increaseAirSupply\(int airSupply\)[\s\S]*?Kind\.DEVOURER \? airSupply - 1/,
  "Primitive Devourer does not consume one air point per land tick");
expect(entity, /decreaseAirSupply\(int airSupply\)[\s\S]*?Kind\.DEVOURER \? getMaxAirSupply\(\)/,
  "Primitive Devourer does not refill air while submerged");

expect(entity, /DEVOURER_SKIN[\s\S]*?DEVOURER_SKIN_HEAVY = 7/,
  "Primitive Devourer heavy skin is not synchronized");
expect(entity, /activeKind\(\) == Kind\.DEVOURER[\s\S]*?variantSpawnChance\(\)[\s\S]*?setDevourerSkin\(DEVOURER_SKIN_HEAVY\)/,
  "Primitive Devourer heavy variant spawn rule is missing");
expect(entity, /tag\.putInt\("devourer_skin"[\s\S]*?setDevourerSkin\(tag\.getInt\("devourer_skin"\)\)/,
  "Primitive Devourer heavy skin is not persisted");
expect(entity, /getDevourerSkin\(\) == DEVOURER_SKIN_HEAVY[\s\S]*?baseHardness \* 2\.0F/,
  "heavy Primitive Devourer does not double block-breaking hardness");
expect(model, /pri_devourer_heavy\.png[\s\S]*?isPrimitiveDevourer\(\)[\s\S]*?getDevourerSkin\(\) == 7/,
  "Primitive Devourer heavy texture selection is missing");

for (const [key, value] of [
  ["primitiveDevourerHealthMultiplier", "1.0D"],
  ["primitiveDevourerDamageMultiplier", "1.0D"],
  ["primitiveDevourerArmorMultiplier", "1.0D"],
  ["primitiveDevourerKnockbackResistanceMultiplier", "1.0D"]
]) {
  const escapedValue = value.replaceAll(".", "\\.");
  if (!new RegExp(`"${key}"\\s*,\\s*${escapedValue}`).test(config)) {
    failures.push(`missing Primitive Devourer config default: ${key}`);
  }
}
expect(config, /"devourerWaterPlacement", true/,
  "Primitive Devourer original water-placement config is missing");
expect(shared, /devourerWaterPlacement\(\)[\s\S]*?"pri_devourer"[\s\S]*?Blocks\.WATER\.defaultBlockState\(\)/,
  "Devourer block breaking does not restore water as in the original");

const texturePath = path.join(root, "src/main/resources/assets/csrp/textures/entity/pri_devourer_heavy.png");
if (!fs.existsSync(texturePath)) {
  failures.push("Primitive Devourer original heavy texture is missing");
} else {
  const digest = crypto.createHash("sha256").update(fs.readFileSync(texturePath)).digest("hex");
  if (digest !== "a2d32b6c4a8260021333e5eb15216fbeb7d8f97f4bd56152cbb3f512cf5ea36b") {
    failures.push("Primitive Devourer heavy texture differs from the original lumh.png");
  }
}

const loot = JSON.parse(read("src/main/resources/data/csrp/loot_tables/entities/pri_devourer.json"));
const expectedLoot = new Map([
  ["csrp:ada_devourer_drop", {chance: 0.4, max: 2, player: true}],
  ["csrp:lurecomponent3", {chance: 0.4, max: 2, player: false}],
  ["csrp:bone", {chance: 0.1, max: 1, player: true}]
]);
for (const pool of loot.pools ?? []) {
  const entry = pool.entries?.[0];
  const expected = expectedLoot.get(entry?.name);
  if (!expected) continue;
  const chance = pool.conditions?.find((condition) => condition.condition === "minecraft:random_chance")?.chance;
  const requiresPlayer = pool.conditions?.some((condition) => condition.condition === "minecraft:killed_by_player") ?? false;
  const count = entry.functions?.find((fn) => fn.function === "minecraft:set_count")?.count;
  const maximum = count?.max ?? 1;
  if (chance === expected.chance && maximum === expected.max && requiresPlayer === expected.player) {
    expectedLoot.delete(entry.name);
  }
}
if (expectedLoot.size) {
  failures.push(`Primitive Devourer loot differs from the original: ${[...expectedLoot.keys()].join(", ")}`);
}

if (failures.length) {
  console.error(`Primitive Devourer verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Primitive Devourer aquatic AI, combat, suffocation, heavy variant, config, water replacement, and loot verified.");
