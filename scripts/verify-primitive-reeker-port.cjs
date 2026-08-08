const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const entity = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const orbEffects = read("src/main/java/alku/csrp/entity/ConfiguredOrbEffects.java");
const model = read("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const drops = read("src/main/java/alku/csrp/event/BookOfVengeanceEvents.java");
const failures = [];

function expect(source, pattern, message) {
  if (!pattern.test(source)) failures.push(message);
}

expect(registry, /"pri_reeker", \(type, level\) -> new PrimitiveVariantEntity\(type, level,[\s\S]*?Kind\.REEKER\)/,
  "registered Primitive Reeker does not use the audited implementation");
expect(entity, /case REEKER -> \{[\s\S]*?health = 40\.0D;[\s\S]*?armor = 12\.0D;[\s\S]*?damage = 12\.0D;[\s\S]*?speed = 0\.31234D;[\s\S]*?knockbackResistance = 0\.60D;/,
  "original Reeker attribute declaration is missing");
expect(entity, /double speed = ricardo \? 0\.45D : 0\.3D;/,
  "spawn-time Reeker speed correction is missing");
expect(entity, /REEKER_ATTACK_INTERVAL = 10/,
  "original ten-tick Reeker melee interval is missing");
expect(entity, /MobEffects\.POISON, 100, 0/,
  "Primitive Reeker attacks do not apply the original poison effect");
expect(entity, /REEKER_SKIN_VIRULENT[\s\S]*?EffectStacking\.apply\(target, ModMobEffects\.VIRAL, 40, 0\)/,
  "virulent Reeker effect is missing");
expect(entity, /REEKER_SKIN_BERSERKER[\s\S]*?EffectStacking\.apply\(target, ModMobEffects\.BLEED, 40, 0\)/,
  "berserker Reeker effect is missing");
expect(entity, /REEKER_SKIN_FRAGILE[\s\S]*?health \*= 0\.5D;[\s\S]*?damage \*= 1\.5D;/,
  "fragile high-damage Reeker variant is missing");
expect(entity, /class ReekerWaterLeapGoal extends Goal/,
  "Primitive Reeker water leap is missing");
expect(entity, /REEKER_EVADE_COOLDOWN = 55[\s\S]*?REEKER_EVADE_DURATION = 10/,
  "Primitive Reeker evade timing is missing");
expect(entity, /class ReekerEvadeGoal extends Goal/,
  "Primitive Reeker evade behavior is missing");
expect(entity, /class ReekerRecruitFollowersGoal extends Goal[\s\S]*?inflate\(16\.0D, 2\.0D, 16\.0D\)/,
  "Primitive Reeker follower recruitment is missing");
expect(entity, /REEKER_SKILL_PREP_TICKS = 40[\s\S]*?REEKER_WINDUP_TICKS = 20[\s\S]*?REEKER_CHARGE_TICKS = 40/,
  "original Reeker charge timing is missing");
expect(entity, /getNavigation\(\)\.moveTo\(targetX, targetY, targetZ, 2\.5D\)/,
  "original Reeker charge path speed is missing");
expect(entity, /getBoundingBox\(\)\.inflate\(2\.0D, 0\.0D, 2\.0D\)[\s\S]*?doHurtTarget\(victim\)/,
  "Reeker charge does not deal contact damage throughout the charge");
expect(entity, /REEKER_DIVE_COOLDOWN_TICKS = 1200[\s\S]*?REEKER_DIVE_EXPLOSION = 3\.0F/,
  "Ricardo dive-bomb constants are missing");
expect(entity, /class RicardoDiveBombGoal extends Goal/,
  "Ricardo dive bomb is missing");
expect(entity, /RICARDO_MAX_HEALTH = 3763\.0D/,
  "Ricardo maximum health is missing");
expect(entity, /stack\.hurtAndBreak\(1, player,[\s\S]*?EquipmentSlot\.MAINHAND/,
  "Ricardo shearing does not damage the shears");
expect(entity, /tag\.putBoolean\("RicardoBald"/,
  "Ricardo bald state is not persisted");
expect(entity, /applyReekerOrbEffects\(target, nearbyEntities\)/,
  "Primitive Reeker scary-orb effect application is missing");
expect(orbEffects, /BuiltInRegistries\.MOB_EFFECT\.getOptional\(effectId\)[\s\S]*?EffectStacking\.apply/,
  "Primitive Reeker configurable scary-orb effect parser is missing");

expect(config, /"enableRicardoVariant", false/,
  "Ricardo config switch or its original default is missing");
expect(config, /"reekerOrbEffects"[\s\S]*?minecraft:hunger[\s\S]*?minecraft:nausea/,
  "Primitive Reeker original scary-orb effect defaults are missing");
for (const key of ["primitiveReekerHealthMultiplier", "primitiveReekerDamageMultiplier",
  "primitiveReekerArmorMultiplier", "primitiveReekerKnockbackResistanceMultiplier"]) {
  if (!config.includes(`"${key}"`)) failures.push(`missing Reeker config key: ${key}`);
}

for (const texture of ["noglasp1.png", "noglav.png", "noglab.png", "noglah.png",
  "ricardo.png", "ricardo_bald.png"]) {
  if (!model.includes(texture)) failures.push(`missing Reeker texture selection: ${texture}`);
  if (!fs.existsSync(path.join(root, "src/main/resources/assets/csrp/textures/entity", texture))) {
    failures.push(`missing Reeker texture asset: ${texture}`);
  }
}
expect(drops, /PrimitiveVariantEntity reeker[\s\S]*?!reeker\.isRicardoVariant\(\)/,
  "Book of Vengeance drop is not restricted to an enabled Ricardo variant");

if (failures.length) {
  console.error(`Primitive Reeker verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Primitive Reeker behavior, variants, Ricardo form, and visuals verified.");
