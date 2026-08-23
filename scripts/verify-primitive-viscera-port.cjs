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
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const entity = read("src/main/java/alku/csrp/entity/VisceraEntity.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const configEvents = read("src/main/java/alku/csrp/config/OriginalConfigEvents.java");
const orb = read("src/main/java/alku/csrp/entity/ConfiguredOrbEffects.java");

expect(entities, /monster\("pri_viscera",[\s\S]*?1\.211F, 2\.351F, 1\.5F\)/,
  "Primitive Viscera dimensions or eye height differ from EntityGim");
expect(entity, /Attributes\.MAX_HEALTH, 45\.0D[\s\S]*?Attributes\.ARMOR, 9\.0D[\s\S]*?Attributes\.ATTACK_DAMAGE, 15\.0D[\s\S]*?Attributes\.MOVEMENT_SPEED, 0\.33D[\s\S]*?Attributes\.KNOCKBACK_RESISTANCE, 0\.7D/,
  "Primitive Viscera default attributes differ from EntityGim");
expect(entity, /(?:Attributes\.STEP_HEIGHT|ForgeMod\.STEP_HEIGHT_ADDITION\.get\(\)), 1\.0D/, "Primitive Viscera one-block step height is missing");
expect(entity, /new WallClimberNavigation\(this, level\)/, "Primitive Viscera wall-climber navigation is missing");
expect(entity, /usesDefaultFloatGoal\(\)[\s\S]*?return false/, "Primitive Viscera still uses the default FloatGoal");

for (const [priority, goal] of [
  [0, "SwimmingDivingGoal"],
  [2, "WaterLeapGoal"],
  [2, "EvadeGoal"],
  [3, "VisceraMeleeGoal"],
  [6, "RecruitFollowersGoal"]
]) {
  expect(entity, new RegExp(`addGoal\\(${priority}, new ${goal}\\(\\)\\)`),
    `Primitive Viscera ${goal} priority is missing`);
}
expect(entity, /setDeltaMovement\([\s\S]*?-0\.095D/, "Primitive Viscera diving motion is missing");
expect(entity, /attackTimer >= 20[\s\S]*?attacking == 2 && onGround\(\)[\s\S]*?0\.7D \+ targetY/,
  "Primitive Viscera water leap charge or launch is incomplete");
expect(entity, /MELEE_ATTACK_INTERVAL = 20/, "Primitive Viscera attack interval is not the original 20 ticks");
expect(entity, /distanceToSqr\(target\) > 16\.0D[\s\S]*?distanceToSqr\(target\) < 225\.0D[\s\S]*?cooldown < 55/,
  "Primitive Viscera evade distance or cooldown is wrong");
expect(entity, /private int cooldown = 56/, "Primitive Viscera evade initial cooldown is wrong");
expect(entity, /inflate\(16\.0D, 2\.0D, 16\.0D\)[\s\S]*?commandRank\(candidate\) < 41[\s\S]*?commandRank\(leader\) <= 30/,
  "Primitive Viscera version-2 follower recruitment is missing");

expect(entity, /EntityDataAccessor<Integer> PARASITE_STATUS[\s\S]*?(?:builder\.define|entityData\.define)\(PARASITE_STATUS, STATUS_IDLE\)/,
  "Primitive Viscera combat state is not a synchronized integer");
expect(entity, /EntityDataAccessor<Integer> SKIN[\s\S]*?setSkin\(tag\.getInt\("viscera_skin"\)\)/,
  "Primitive Viscera skin is not synchronized and persisted");
const attack = entity.match(/public boolean doHurtTarget\(Entity entity\) \{[\s\S]*?\n    \}/)?.[0] ?? "";
expect(attack, /getSkin\(\) == SKIN_VIRULENT[\s\S]*?ModMobEffects\.VIRAL/, "Viscera skin 5 Viral attack effect is missing");
expect(attack, /else if \(getSkin\(\) == SKIN_BLEEDING\)[\s\S]*?ModMobEffects\.BLEED/, "Viscera skin 6 Bleed attack effect is missing");
if (!/else if \(getSkin\(\) == SKIN_BLEEDING\)/.test(attack)) {
  failures.push("Viscera attack applies Viral and Bleed unconditionally");
}
expect(entity, /public void push\(Entity entity\)[\s\S]*?SKIN_VIRULENT[\s\S]*?ModMobEffects\.VIRAL/,
  "Viscera skin 5 contact infection is missing");
expect(entity, /getAdaptationHitStatus\(\) > 0 && random\.nextBoolean\(\) \? null/,
  "Viscera adapted-hit hurt-sound silence is missing");

for (const [method, base, field] of [
  ["visceraHealth", "45.0D", "VISCERA_HEALTH_MULTIPLIER"],
  ["visceraDamage", "15.0D", "VISCERA_DAMAGE_MULTIPLIER"],
  ["visceraArmor", "9.0D", "VISCERA_ARMOR_MULTIPLIER"],
  ["visceraKnockbackResistance", "0.7D", "VISCERA_KNOCKBACK_MULTIPLIER"]
]) {
  expect(config, new RegExp(`${method}\\(\\)[\\s\\S]*?${base.replaceAll(".", "\\.")}[\\s\\S]*?${field}`),
    `Primitive Viscera config method ${method} is missing`);
}
expect(config, /primitiveVisceraOrbEffects"[\s\S]*?0;15;1;minecraft:hunger;0;0[\s\S]*?0;15;1;minecraft:slowness;0;0/,
  "Primitive Viscera scary-orb config is missing");
expect(entity, /ConfiguredOrbEffects\.apply\(this, target, nearbyEntities, MobsConfig\.visceraOrbEffects\(\)\)/,
  "Primitive Viscera scary-orb effects are not applied");
expect(orb, /parts\.length != 6[\s\S]*?wrapAsHolder[\s\S]*?EffectStacking\.apply/,
  "Shared six-field scary-orb parser is incomplete");
expect(configEvents, /VisceraEntity viscera[\s\S]*?viscera\.applyConfiguredAttributes\(\)/,
  "Primitive Viscera configured attributes are not applied on join");
expect(client, /PRI_VISCERA[\s\S]*?"pri_viscera", 1\.0F/, "Primitive Viscera render shadow is not 1.0");
if (/public void die\([\s\S]*?setBlock/.test(entity)) {
  failures.push("Primitive Viscera death path generates a forbidden remains block");
}

if (failures.length) {
  console.error(`Primitive Viscera verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log("Primitive Viscera original behavior is verified.");
