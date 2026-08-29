const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => {
  const absolute = path.join(root, relative);
  if (!fs.existsSync(absolute)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(absolute, "utf8");
};
const parseJson = (relative) => JSON.parse(read(relative));
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};
const sha256 = (relative) => crypto.createHash("sha256")
  .update(fs.readFileSync(path.join(root, relative))).digest("hex");

const entity = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const owner = read("src/main/java/alku/csrp/entity/PullingBallOwner.java");
const projectile = read("src/main/java/alku/csrp/entity/PullingBallEntity.java");
const configuredOrb = read("src/main/java/alku/csrp/entity/ConfiguredOrbEffects.java");
const primitive = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const model = read("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/PrimitiveParasiteRenderer.java");
const profiles = read("src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java");

expect(entities, /"ada_arachnida"[\s\S]*?1\.901F, 2\.85F, 1\.5F/,
  "Adapted Arachnida dimensions or eye height differ from EntityRanracAdapted");
expect(entity, /case ARACHNIDA -> \{[\s\S]*?health = 80\.0D[\s\S]*?armor = 14\.0D[\s\S]*?damage = 30\.0D[\s\S]*?speed = 0\.33D[\s\S]*?knockbackResistance = 1\.0D[\s\S]*?followRange = 32\.0D/,
  "Adapted Arachnida default attributes differ from the original");
expect(entity, /case ARACHNIDA -> applyConfiguredAttributes\([\s\S]*?MobsConfig\.adaptedArachnidaHealth\(\)[\s\S]*?MobsConfig\.adaptedArachnidaArmor\(\)[\s\S]*?MobsConfig\.adaptedArachnidaDamage\(\)[\s\S]*?MobsConfig\.adaptedArachnidaKnockbackResistance\(\)/,
  "Adapted Arachnida config is not applied after entity registration");
expect(entity, /kind == Kind\.ARACHNIDA \|\| kind == Kind\.BURROWER \|\| kind == Kind\.TOZOON[\s\S]*?Attributes\.STEP_HEIGHT, 1\.0D/,
  "Adapted Arachnida one-block step height is missing");
for (const [method, base, additional] of [
  ["adaptedArachnidaHealth", "35.0D", "ADAPTED_ARACHNIDA_ADDITIONAL_HEALTH"],
  ["adaptedArachnidaDamage", "15.0D", "ADAPTED_ARACHNIDA_ADDITIONAL_DAMAGE"],
  ["adaptedArachnidaArmor", "4.0D", "ADAPTED_ARACHNIDA_ADDITIONAL_ARMOR"],
  ["adaptedArachnidaKnockbackResistance", "0.8D", "ADAPTED_ARACHNIDA_ADDITIONAL_KNOCKBACK"]
]) {
  expect(config, new RegExp(`${method}\\(\\)[\\s\\S]*?${base.replaceAll(".", "\\.")} \\+ ${additional}`),
    `${method} does not combine the original primitive and adapted values`);
}
for (const [key, value] of [
  ["adaptedArachnidaAdditionalHealth", "45.0D"],
  ["adaptedArachnidaAdditionalDamage", "15.0D"],
  ["adaptedArachnidaAdditionalArmor", "10.0D"],
  ["adaptedArachnidaAdditionalKnockbackResistance", "0.2D"]
]) {
  expect(config, new RegExp(`"${key}"\\s*,\\s*${value.replaceAll(".", "\\.")}`),
    `missing Adapted Arachnida config default: ${key}`);
}

expect(entity, /new ArachnidaPart\(this, "abdomen",[\s\S]*?-1\.6F, 1\.5F, 1\.7F, 1\.9F, 2\.0F, 0\.75F\)/,
  "Adapted Arachnida abdomen hitbox differs from the original");
expect(entity, /new ArachnidaPart\(this, "head",[\s\S]*?1\.6F, 1\.3F, 1\.5F, 0\.9F, 0\.9F, 1\.25F\)/,
  "Adapted Arachnida head hitbox differs from the original");
expect(entity, /class ArachnidaPart extends PartEntity[\s\S]*?random\.nextBoolean\(\)[\s\S]*?ModMobEffects\.BLEED, 80, 0[\s\S]*?amount \* damageVulnerability/,
  "Adapted Arachnida multipart damage and self-bleeding behavior are incomplete");
expect(entity, /isMultipartEntity\(\)[\s\S]*?(?:arachnidaParts|bodyParts) != null[\s\S]*?getParts\(\)/,
  "Adapted Arachnida multipart registration is not constructor-safe");

expect(entity, /isArachnidaType\(getType\(\)\)[\s\S]*?new WallClimberNavigation/,
  "Adapted Arachnida wall-climber navigation is missing");
expect(entity, /onClimbable\(\)[\s\S]*?Kind\.ARACHNIDA[\s\S]*?!hasLineOfSight\(target\)[\s\S]*?distanceToSqr\(target\) < 100\.0D[\s\S]*?target\.getY\(\) \+ 1\.0D < getY\(\)[\s\S]*?horizontalCollision/,
  "Adapted Arachnida climb restrictions differ from the original");

for (const [constant, value] of [
  ["ARACHNIDA_SKILL_CHARGE_TICKS", 20],
  ["ARACHNIDA_SKILL_SHOTS", 6],
  ["ARACHNIDA_SKILL_SHOT_INTERVAL", 20],
  ["ARACHNIDA_MAX_PULL_TICKS", 400]
]) {
  expect(entity, new RegExp(`${constant}\\s*=\\s*${value}`), `wrong ${constant}`);
}
expect(entity, /ArachnidaPullSkillGoal[\s\S]*?distance < 900\.0D && distance >= 25\.0D[\s\S]*?hasLineOfSight\(target\)[\s\S]*?setArachnidaStatus\(11\)[\s\S]*?shots == 2[\s\S]*?ModSounds\.get\("attack\.ranrac"\)[\s\S]*?ARACHNIDA_SKILL_SHOT_INTERVAL[\s\S]*?ARACHNIDA_SKILL_SHOTS/,
  "Adapted Arachnida six-shot pull skill is incomplete");
expect(owner, /pullProjectileCaptureRadius\(\)[\s\S]*?return 0\.7D/,
  "shared pull projectile capture-radius contract is missing");
expect(owner, /pullProjectileAccelerationMultiplier\(\)[\s\S]*?return 2\.0D/,
  "shared pull projectile acceleration contract is missing");
expect(owner, /pullProjectileMaxAge\(\)[\s\S]*?return 80/,
  "shared pull projectile lifetime contract is missing");
expect(entity, /pullProjectileCaptureRadius\(\)[\s\S]*?Kind\.ARACHNIDA \? 2\.0D/,
  "Adapted Arachnida projectile capture radius is wrong");
expect(entity, /pullProjectileAccelerationMultiplier\(\)[\s\S]*?Kind\.ARACHNIDA \? 4\.0D/,
  "Adapted Arachnida fifth-tick projectile acceleration is wrong");
expect(entity, /pullProjectileMaxAge\(\)[\s\S]*?Kind\.ARACHNIDA \? 0/,
  "Adapted Arachnida projectile lifetime is wrong");
expect(projectile, /tickCount == 5[\s\S]*?pullProjectileAccelerationMultiplier\(\)/,
  "pull projectile does not apply its owner-specific fifth-tick acceleration");
expect(projectile, /inflate\(captureRadius\)[\s\S]*?owner::isValidPullTarget/,
  "pull projectile does not use its owner-specific capture radius");
expect(projectile, /random\.nextInt\(3\) \+ 1[\s\S]*?SrpWebBlock\.Kind\.THIN/,
  "pull projectile block impacts do not create one to three thin webs");

expect(entity, /captureTarget\(LivingEntity target\)[\s\S]*?target != getTarget\(\)[\s\S]*?MobEffects\.GLOWING, 100, 5[\s\S]*?setArachnidaTarget\(target\.getId\(\)\)/,
  "Adapted Arachnida projectile can capture the wrong target or misses Glowing");
expect(entity, /tickArachnidaTether\(\)[\s\S]*?MOVEMENT_SLOWDOWN, 20, 5[\s\S]*?WEAKNESS, 20, 5[\s\S]*?pull\.normalize\(\)\.scale\(0\.2D\)[\s\S]*?ARACHNIDA_MAX_PULL_TICKS/,
  "Adapted Arachnida sustained tether debuffs, pull strength, or duration are wrong");
expect(entity, /ARACHNIDA_TARGET\) == 0[\s\S]*?!arachnidaCanPull[\s\S]*?arachnidaCanPull = true[\s\S]*?arachnidaPullingTicks = 0/,
  "Adapted Arachnida post-timeout pull lock lasts longer than the original AI tick");
expect(entity, /ArachnidaWaterLeapGoal[\s\S]*?return leaping \|\| isInWaterOrBubble\(\)[\s\S]*?chargeTicks >= 20[\s\S]*?airborneTicks = 1[\s\S]*?airborneTicks == 2 && onGround\(\)[\s\S]*?motion\.x \+ deltaX \/ horizontal \* 1\.35D \+ motion\.x \* 0\.3D[\s\S]*?0\.7D \+ targetYOffset/,
  "Adapted Arachnida water leap does not charge and launch at original velocity");
expect(entity, /ArachnidaMeleeGoal[\s\S]*?distance > 64\.0D \|\| arachnidaAttackAnimationCooldown == 0[\s\S]*?fast \? 2 : 1[\s\S]*?fast \? 1\.3D : 1\.0D/,
  "Adapted Arachnida fast and slow melee states are wrong");
expect(entity, /kind == Kind\.ARACHNIDA[\s\S]*?status == 10 \|\| status == 11[\s\S]*?status == 2[\s\S]*?moving \? state\.setAndContinue\(ARACHNIDA_FAST_MOVE\) : PlayState\.STOP/,
  "Adapted Arachnida static status poses differ from ModelRanracAdapted");
expect(entity, /activeKind\(\) == Kind\.ARACHNIDA[\s\S]*?arachnidaAttackAnimationCooldown = 100[\s\S]*?getArachnidaSkin\(\) == 5[\s\S]*?ModMobEffects\.VIRAL[\s\S]*?getArachnidaSkin\(\) == 6[\s\S]*?ModMobEffects\.BLEED/,
  "Adapted Arachnida melee variants or attack animation cooldown are missing");
expect(entity, /doPush\(Entity entity\)[\s\S]*?Kind\.ARACHNIDA && getArachnidaSkin\(\) == 5[\s\S]*?ModMobEffects\.VIRAL/,
  "virulent Adapted Arachnida contact infection is missing");

expect(entity, /finalizeSpawn[\s\S]*?Kind\.ARACHNIDA[\s\S]*?Config\.variantSpawnChance\(\)[\s\S]*?Config\.alwaysVariantPhase\(\)[\s\S]*?setArachnidaSkin\(5 \+ random\.nextInt\(3\)\)/,
  "Adapted Arachnida original variant selection is missing");
for (const effect of [
  /"0;15;2;minecraft:hunger;0;0"/,
  /"0;35;2;csrp:needler;0;0"/,
  /"0;15;2;minecraft:blindness;0;0"/
]) expect(config, effect, "Adapted Arachnida scary-orb config differs from the original");
expect(entity, /ConfiguredOrbEffects\.apply\(this, target, nearbyEntities, MobsConfig\.adaptedArachnidaOrbEffects\(\)\)/,
  "Adapted Arachnida scary-orb effects are not applied");
expect(configuredOrb, /parts\.length != 6[\s\S]*?wrapAsHolder[\s\S]*?EffectStacking\.apply/,
  "shared six-field scary-orb config parser is incomplete");
expect(primitive, /ConfiguredOrbEffects\.apply\(this, target, nearbyEntities, MobsConfig\.reekerOrbEffects\(\)\)/,
  "Primitive Reeker no longer reuses the shared scary-orb parser");

expect(entity, /playStepSound\(BlockPos pos, BlockState state\)[\s\S]*?Kind\.ARACHNIDA[\s\S]*?ModSounds\.HEAVY_MULTIPLE_STEP\.get\(\)/,
  "Adapted Arachnida heavy multi-leg step sound is missing");
expect(profiles, /register\("aranrac"[\s\S]*?"ada_arachnida"/,
  "Adapted Arachnida ambient, hurt, and death profile is not aranrac");
expect(entity, /public void die\(DamageSource source\)[\s\S]*?Kind\.ARACHNIDA[\s\S]*?colonies\(\)\.isEmpty\(\)[\s\S]*?ModEntities\.PRI_ARACHNIDA/,
  "Adapted Arachnida colony death reversion is missing");
expect(entity, /xpReward = 55/,
  "Adapted Arachnida XP reward is not 55");

expect(model, /case 5 -> ARACHNIDA_VIRULENT_TEXTURE[\s\S]*?case 6 -> ARACHNIDA_BLEEDING_TEXTURE[\s\S]*?case 7 -> ARACHNIDA_HEAVY_TEXTURE/,
  "Adapted Arachnida variant textures are not selected by skin");
expect(renderer, /textures\/entity\/guardian_beam\.png[\s\S]*?getArachnidaStatus\(\) != 3[\s\S]*?getArachnidaTetherTarget\(\)[\s\S]*?renderArachnidaBeam/,
  "Adapted Arachnida Guardian-beam tether rendering is missing");
expect(client, /ADA_ARACHNIDA[\s\S]*?"ada_arachnida", 1\.0F/,
  "Adapted Arachnida shadow radius is not the original 1.0");
expect(items, /"ada_arachnida_spawn_egg"[\s\S]*?0x7F3F00, 0xB6FF00/,
  "Adapted Arachnida spawn egg colors differ from the original");

const expectedTextures = {
  "src/main/resources/assets/csrp/textures/entity/ada_arachnida_virulent.png":
    "d7e03c9006fcd6ebb0fc131a4c277820f69b754477e17cfde062f5c3da14b95b",
  "src/main/resources/assets/csrp/textures/entity/ada_arachnida_bleeding.png":
    "6ee423dfdf3ba35470370a5e3297025ef2b3e3bdb5ca9e5a9c4fa3f18c4ac132",
  "src/main/resources/assets/csrp/textures/entity/ada_arachnida_heavy.png":
    "b6f9aa45ae52e9521977715fa20d48b8b6c283a5e8ea3cd268fc701ea07b4977"
};
for (const [relative, expectedHash] of Object.entries(expectedTextures)) {
  if (!fs.existsSync(path.join(root, relative))) {
    failures.push(`missing original Adapted Arachnida texture: ${relative}`);
  } else if (sha256(relative) !== expectedHash) {
    failures.push(`Adapted Arachnida texture differs from SRP 1.10.7: ${relative}`);
  }
}

for (const relative of [
  "src/main/resources/data/csrp/loot_tables/entities/ada_arachnida.json",
  "src/main/resources/assets/csrp/compendium/drops/ada_arachnida.json"
]) {
  const table = parseJson(relative);
  const expectedPools = new Map([
    ["csrp:lurecomponent4", { chance: 0.6, min: 1, max: 3 }],
    ["csrp:ada_arachnida_drop", { chance: 0.8, min: 1, max: 3 }],
    ["csrp:bone", { chance: 0.2, min: 1, max: 2 }]
  ]);
  if (!Array.isArray(table.pools) || table.pools.length !== expectedPools.size) {
    failures.push(`${relative}: expected three independent original drop pools`);
    continue;
  }
  for (const pool of table.pools) {
    const entry = pool.entries?.[0];
    const expected = expectedPools.get(entry?.name);
    const count = entry?.functions?.find((fn) => fn.function === "minecraft:set_count")?.count;
    const chance = pool.conditions?.find((condition) =>
      condition.condition === "minecraft:random_chance")?.chance;
    if (!expected || chance !== expected.chance || count?.min !== expected.min || count?.max !== expected.max) {
      failures.push(`${relative}: invalid original pool for ${entry?.name ?? "unknown item"}`);
    }
    if (pool.conditions?.some((condition) => condition.condition === "minecraft:killed_by_player")) {
      failures.push(`${relative}: original independent drop is incorrectly gated behind a player kill`);
    }
  }
}

const deathMethod = entity.match(/public void die\(DamageSource source\) \{[\s\S]*?\n    \}/)?.[0] ?? "";
if (/setBlock|INFESTED_REMAINS|PARASITE_REMAINS/.test(deathMethod)) {
  failures.push("Adapted Arachnida death path restores a forbidden remains block");
}

if (failures.length) {
  console.error(`Adapted Arachnida verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Adapted Arachnida original behavior, resources, rendering, and drops are verified.");
