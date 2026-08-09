const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const commonConfig = read("src/main/java/alku/csrp/Config.java");
const entity = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
const shared = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const projectile = read("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");
const model = read("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/PrimitiveParasiteRenderer.java");
const projectileRenderer = read("src/main/java/alku/csrp/client/renderer/ParasiteProjectileRenderer.java");

expect(registry,
  /"pri_yelloweye", \(type, level\) -> new PrimitiveVariantEntity\(type, level,[\s\S]*?Kind\.YELLOWEYE\)/,
  "registered Primitive Yelloweye does not use the audited implementation");
expect(entity,
  /case YELLOWEYE -> \{[\s\S]*?health = 30\.0D[\s\S]*?armor = 3\.5D[\s\S]*?damage = 3\.5D[\s\S]*?speed = 0\.25D[\s\S]*?knockbackResistance = 0\.20D[\s\S]*?followRange = 24\.0D/,
  "Primitive Yelloweye original attributes are missing");
expect(entity,
  /case YELLOWEYE -> applyConfiguredAttributes\([\s\S]*?MobsConfig\.yelloweyeHealth\(\)[\s\S]*?MobsConfig\.yelloweyeArmor\(\)[\s\S]*?MobsConfig\.yelloweyeNadeDamage\(\)[\s\S]*?MobsConfig\.yelloweyeKnockbackResistance\(\)/,
  "Primitive Yelloweye config is not applied after entity registration");
expect(entity, /xpReward = kind == Kind\.YELLOWEYE \? 30 : 18/,
  "Primitive Yelloweye original experience reward is missing");
expect(entity, /moveControl = new YelloweyeMoveControl\(this\)/,
  "Primitive Yelloweye inertia-based flight control is missing");
expect(entity, /class YelloweyeRandomFlightGoal extends Goal[\s\S]*?random\.nextInt\(7\) == 0/,
  "Primitive Yelloweye random flight cadence is missing");
expect(entity, /case 2 -> origin\.offset\(random\.nextInt\(6\) - 2, random\.nextInt\(7\) - 2/,
  "Primitive Yelloweye long-range approach pattern is missing");
expect(entity, /case 3 -> origin\.offset\(random\.nextInt\(4\) \+ 3, random\.nextInt\(5\) \+ 4/,
  "Primitive Yelloweye close-range reposition pattern is missing");
expect(entity, /YELLOWEYE_WARNING_TICK = 70[\s\S]*?YELLOWEYE_FIRE_TICK = 100/,
  "Primitive Yelloweye original projectile timing is missing");
expect(entity, /yelloweyeAttackTimer \+= hasEffect\(ModMobEffects\.RAGE\) \? 2 : 1/,
  "Rage does not accelerate Primitive Yelloweye shooting");
expect(entity, /rangedShots\+\+[\s\S]*?rangedShots == 4[\s\S]*?boolean acid = rangedShots >= 4/,
  "Primitive Yelloweye fourth-shot Nade cycle is missing");
expect(entity, /Mode mode = acid \? ParasiteProjectileEntity\.Mode\.YELLOWEYE_NADE[\s\S]*?Mode\.YELLOWEYE_SPINE[\s\S]*?createProjectile\(level\(\), mode\)[\s\S]*?configureLegacyFireball\(this, mode/,
  "Primitive Yelloweye does not launch its dedicated original projectiles");
expect(entity, /tickYelloweyeFlightLimits\(\)[\s\S]*?MobsConfig\.yelloweyeMaxFlightHeight\(\)/,
  "Primitive Yelloweye flight-height limiter is missing");

for (const [key, value] of [
  ["primitiveYelloweyeHealthMultiplier", "1.0D"],
  ["primitiveYelloweyeDamageMultiplier", "1.0D"],
  ["primitiveYelloweyeArmorMultiplier", "1.0D"],
  ["primitiveYelloweyeKnockbackResistanceMultiplier", "1.0D"],
  ["primitiveYelloweyePoisonDuration", "3"],
  ["primitiveYelloweyePoisonAmplifier", "1"],
  ["primitiveYelloweyeGearDegrade", "0.04D"],
  ["primitiveYelloweyeFlightHeightLimit", "256"]
]) {
  const escapedValue = value.replaceAll(".", "\\.");
  if (!new RegExp(`"${key}"\\s*,\\s*${escapedValue}`).test(config)) {
    failures.push(`missing Yelloweye config default: ${key}`);
  }
}
expect(config, /primitiveYelloweyeFlightHeightLimit", 256, 0, 256/,
  "Primitive Yelloweye flight-height config range differs from the original");
expect(commonConfig, /primitiveMinimumDamage", 2\.0D/,
  "primitive special-attack minimum damage config is missing");
expect(shared, /applyPrimitiveMinimumDamage\(LivingEntity target\)/,
  "primitive projectile minimum-damage implementation is missing");

expect(entity, /YELLOWEYE_SKIN[\s\S]*?YELLOWEYE_SKIN_HEAVY = 7/,
  "Primitive Yelloweye heavy skin is not synchronized");
expect(entity, /variantSpawnChance\(\)[\s\S]*?alwaysVariantPhase\(\)[\s\S]*?setYelloweyeSkin\(YELLOWEYE_SKIN_HEAVY\)/,
  "Primitive Yelloweye heavy variant spawn rule is missing");
expect(entity, /tag\.putInt\("yelloweye_skin"[\s\S]*?tag\.putInt\("yelloweye_shots"/,
  "Primitive Yelloweye skin or shot cycle is not persisted");
expect(entity, /adjustBlockBreakHardness\(float baseHardness\)[\s\S]*?baseHardness \* 2\.0F/,
  "heavy Primitive Yelloweye does not double its block-breaking hardness");
expect(model, /pri_yelloweye_heavy\.png/,
  "Primitive Yelloweye heavy texture selection is missing");
expect(renderer, /pri_yelloweye_glow\.png[\s\S]*?pri_yelloweye_heavy_glow\.png[\s\S]*?YelloweyeGlowLayer/,
  "Primitive Yelloweye normal/heavy glow layer is missing");

expect(projectile, /ELVIA_NADE,\s*YELLOWEYE_SPINE,\s*YELLOWEYE_NADE/,
  "Yelloweye projectile modes were not appended in a save-compatible order");
expect(projectile, /case ACID, YELLOWEYE_SPINE, YELLOWEYE_NADE,[\s\S]*?-> ParticleTypes\.ITEM_SLIME/,
  "Yelloweye projectiles do not use the original slime trail");
expect(projectile, /impactYelloweyeSpine[\s\S]*?MobEffects\.POISON[\s\S]*?yelloweyeGearDamage\(\)[\s\S]*?applyPrimitiveMinimumDamage/,
  "Yelloweye spine damage, poison, gear damage, or minimum damage is missing");
expect(projectile, /tickYelloweyeNade[\s\S]*?YELLOWEYE_NADE_START_DELAY_TICKS[\s\S]*?YELLOWEYE_NADE_FUSE_TICKS/,
  "Yelloweye Nade delayed expansion sequence is missing");
expect(projectile, /tickYelloweyeNade[\s\S]*?target\.invulnerableTime = 0[\s\S]*?damageSources\(\)\.magic\(\)[\s\S]*?applyPrimitiveMinimumDamage[\s\S]*?YELLOWEYE_NADE_DURATION_TICKS/,
  "Yelloweye Nade continuous magic frame damage is missing");
expect(projectile, /tickAcidNade[\s\S]*?MobEffects\.POISON, 40, 0[\s\S]*?ModMobEffects\.CORROSION, 60, 0/,
  "legacy ACID behavior regressed while adding the Yelloweye Nade");
expect(projectileRenderer, /YELLOWEYE_SPINE -> SPINEBALL_TEXTURE/,
  "Yelloweye spineball texture is not rendered");
expect(projectile, /getMode\(\) == Mode\.YELLOWEYE_SPINE[\s\S]*?return 0\.5F/,
  "Yelloweye spineball render scale differs from the original");
expect(projectileRenderer, /isYelloweyeNadeArmed\(\)[\s\S]*?renderYelloweyeNade/,
  "landed Yelloweye Nade is not rendered as the original expanding object");

for (const asset of [
  "src/main/resources/assets/csrp/textures/entity/pri_yelloweye_heavy.png",
  "src/main/resources/assets/csrp/textures/entity/pri_yelloweye_glow.png",
  "src/main/resources/assets/csrp/textures/entity/pri_yelloweye_heavy_glow.png",
  "src/main/resources/assets/csrp/textures/entity/projectile/spineball.png",
  "src/main/resources/assets/csrp/textures/entity/monster/nade.png"
]) {
  if (!fs.existsSync(path.join(root, asset))) failures.push(`missing Yelloweye asset: ${asset}`);
}

const loot = JSON.parse(read("src/main/resources/data/csrp/loot_table/entities/pri_yelloweye.json"));
const expectedLoot = new Map([
  ["csrp:lurecomponent3", 0.6],
  ["csrp:ada_yelloweye_drop", 0.4],
  ["csrp:bone", 0.04]
]);
for (const pool of loot.pools ?? []) {
  const name = pool.entries?.[0]?.name;
  if (expectedLoot.get(name) === pool.conditions?.[0]?.chance) expectedLoot.delete(name);
}
if (expectedLoot.size) failures.push(`Primitive Yelloweye loot differs from the original: ${[...expectedLoot.keys()].join(", ")}`);
const lurePool = loot.pools?.find((pool) => pool.entries?.[0]?.name === "csrp:lurecomponent3");
const lureCount = lurePool?.entries?.[0]?.functions?.[0]?.count;
if (lureCount?.min !== 1 || lureCount?.max !== 2) failures.push("Primitive Yelloweye lure-component count is not 1-2");

if (failures.length) {
  console.error(`Primitive Yelloweye verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Primitive Yelloweye behavior, projectiles, heavy variant, visuals, and loot verified.");
