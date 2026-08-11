const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};
const expectPattern = (source, pattern, message) => expect(pattern.test(source), message);
const section = (source, start, end) => {
  const startIndex = source.indexOf(start);
  if (startIndex < 0) return "";
  const endIndex = source.indexOf(end, startIndex + start.length);
  return source.slice(startIndex, endIndex < 0 ? source.length : endIndex);
};
const expectOrder = (source, tokens, message) => {
  let cursor = -1;
  for (const token of tokens) {
    cursor = source.indexOf(token, cursor + 1);
    if (cursor < 0) {
      failures.push(message);
      return;
    }
  }
};

const gnat = read("src/main/java/alku/csrp/entity/GnatEntity.java");
const lice = read("src/main/java/alku/csrp/entity/LiceEntity.java");
const particles = read("src/main/java/alku/csrp/entity/VerminParticles.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const common = read("src/main/java/alku/csrp/registry/CommonModEvents.java");

expectPattern(entities, /monster\("gnat", GnatEntity::new, 0\.85F, 1\.0F\)/,
  "EntityAta body is not the original 0.85 x 1.0");
expectPattern(entities, /monster\("lice", LiceEntity::new, 0\.85F, 1\.0F\)/,
  "EntityViin body is not the original 0.85 x 1.0");
expectPattern(client, /ModEntities\.GNAT[\s\S]*?"gnat", 0\.5F/,
  "EntityAta shadow radius is not the original 0.5");
expectPattern(client, /ModEntities\.LICE[\s\S]*?"lice", 0\.5F/,
  "EntityViin shadow radius is not the original 0.5");
for (const [name, source] of [["EntityAta", gnat], ["EntityViin", lice]]) {
  expectPattern(source, /withEyeHeight\(0\.8F\)/,
    name + " eye height is not the original 0.8");
  expectPattern(source,
    /getPassengerAttachmentPoint\(Entity passenger, EntityDimensions dimensions, float partialTick\)[\s\S]*?dimensions\.height\(\) \* 0\.5D/,
    name + " mounted offset is not half its height");
  expect(!source.includes("super.registerGoals()"),
    name + " still installs generic parasite goals");
  expectPattern(source, /MAX_LIFE(?:TIME|SPAN)_TICKS\s*=\s*1_200/,
    name + " lifespan is not the original 1200 ticks");
  expectPattern(source,
    /ModSounds\.get\("mob\.silence"\)[\s\S]*?ModSounds\.get\("mob\.silence"\)[\s\S]*?ModSounds\.get\("mob\.silence"\)/,
    name + " does not retain the original silent ambient, hurt and death sounds");
}

expectPattern(gnat,
  /MAX_HEALTH, 5\.0[\s\S]*?ARMOR, 2\.0[\s\S]*?ATTACK_DAMAGE, 5\.0[\s\S]*?MOVEMENT_SPEED, 0\.34559[\s\S]*?KNOCKBACK_RESISTANCE, 0\.6[\s\S]*?FOLLOW_RANGE, 32\.0/,
  "EntityAta original attributes are missing");
expectPattern(gnat,
  /addGoal\(0, new SkillLeapGoal\(\)\)[\s\S]*?addGoal\(0, new SwimmingDivingGoal\(\)\)[\s\S]*?addGoal\(3, new FastMeleeAttackGoal\(\)\)[\s\S]*?addGoal\(3, new LeapAtTargetGoal\(this, 0\.4F\)\)[\s\S]*?addGoal\(8, new RandomLookAroundGoal\(this\)\)/,
  "EntityAta original goal priorities are missing");
expectPattern(gnat,
  /targetSelector\.addGoal\(1, new HurtByTargetGoal\(this\)\)[\s\S]*?targetSelector\.addGoal\(4, new NearestAttackableTargetGoal<>\(this, Player\.class, 0[\s\S]*?targetSelector\.addGoal\(4, new NearestAttackableTargetGoal<>\(this, Mob\.class, 0/,
  "EntityAta original target priorities or zero search interval are missing");
expectPattern(gnat, /super\(GnatEntity\.this, 1\.3D, false\)[\s\S]*?return 6;/,
  "EntityAta melee speed or six-tick cadence is wrong");
expectPattern(gnat,
  /CHARGE_TICKS = 20[\s\S]*?MIN_DISTANCE_SQR = 25\.0D[\s\S]*?MAX_DISTANCE_SQR = 10_000\.0D/,
  "EntityAta skill leap charge or distance window is wrong");
expectPattern(gnat,
  /movement\.x \* 1\.3D[\s\S]*?dx \/ horizontalDistance \* 0\.9D[\s\S]*?setDeltaMovement\(launchX, 0\.4D, launchZ\)/,
  "EntityAta skill leap velocity does not match EntityAISkill type 14");
expectPattern(gnat,
  /if \(!onGround\(\)\)[\s\S]*?sawAirborne = true[\s\S]*?else if \(sawAirborne\)[\s\S]*?SKILL_LEAPING, false/,
  "EntityAta skill animation does not remain active until landing");
expectPattern(gnat,
  /setFlags\(EnumSet\.of\(Flag\.JUMP\)\)[\s\S]*?isInWaterOrBubble\(\)[\s\S]*?isInLava\(\)[\s\S]*?-0\.12D[\s\S]*?random\.nextFloat\(\) < 0\.8F/,
  "EntityAta original diving goal is incomplete");
expectPattern(gnat, /ModSounds\.get\("small\.step"\)/,
  "EntityAta original small.step sound is missing");

expectPattern(lice,
  /MAX_HEALTH, 12\.0[\s\S]*?ARMOR, 5\.0[\s\S]*?ATTACK_DAMAGE, 11\.0[\s\S]*?MOVEMENT_SPEED, 0\.34559[\s\S]*?FLYING_SPEED, 0\.34559[\s\S]*?KNOCKBACK_RESISTANCE, 0\.4[\s\S]*?FOLLOW_RANGE, 32\.0/,
  "EntityViin original attributes are missing");
expectPattern(lice,
  /addGoal\(3, new FlightAttackGoal\(\)\)[\s\S]*?addGoal\(4, new ChargeAttackGoal\(\)\)[\s\S]*?addGoal\(6, new RandomFlyGoal\(\)\)[\s\S]*?addGoal\(8, new RandomLookAroundGoal\(this\)\)/,
  "EntityViin original goal priorities are missing");
expectPattern(lice,
  /targetSelector\.addGoal\(1, new HurtByTargetGoal\(this\)\)[\s\S]*?targetSelector\.addGoal\(4, new NearestAttackableTargetGoal<>\(this, Player\.class, 0[\s\S]*?targetSelector\.addGoal\(4, new NearestAttackableTargetGoal<>\(this, Mob\.class, 0/,
  "EntityViin original target priorities or zero search interval are missing");
expectPattern(lice, /FLIGHT_FLAGS[\s\S]*?builder\.define\(FLIGHT_FLAGS, \(byte\) 0\)/,
  "EntityViin synchronized charging flag is missing");
expectPattern(lice, /cycleTick = tickCount % 21[\s\S]*?cycleTick > 0 && cycleTick <= 10/,
  "EntityViin flight attack does not use the original 1-10/21 tick window");
expectPattern(lice, /lostTargetTicks >= 6/,
  "EntityViin flight attack does not clear an unreachable target after six checks");
expect(!lice.includes("FlyingMoveControl"),
  "EntityViin still uses the non-original FlyingMoveControl");
expectPattern(lice,
  /arrivalRadius = \(getBbWidth\(\) \* 2\.0D \+ getBbHeight\(\)\) \/ 3\.0D[\s\S]*?scale\(0\.5D\)[\s\S]*?0\.05D \* speedModifier/,
  "EntityViin move acceleration or arrival behavior is wrong");

const randomFly = section(lice, "private final class RandomFlyGoal", "private final class LiceMoveControl");
const randomFlyCanUse = randomFly.match(/public boolean canUse\(\) \{([\s\S]*?)\n        \}/)?.[1] ?? "";
expect(!randomFlyCanUse.includes("getTarget()"),
  "EntityViin random flight incorrectly requires no target");
const charge = section(lice, "private final class ChargeAttackGoal", "private final class RandomFlyGoal");
expectPattern(charge, /getBoundingBox\(\)\.intersects\(target\.getBoundingBox\(\)\)/,
  "EntityViin charge does not use a real bounding-box collision");
expect(!charge.includes("inflate("),
  "EntityViin charge still attacks through an expanded collision box");
expect(!charge.includes("performContactAttack("),
  "EntityViin charge bypasses the original push-only contact conversion");

expectPattern(common,
  /ModEntities\.LICE\.get\(\)[\s\S]*?SpawnPlacementTypes\.NO_RESTRICTIONS/,
  "EntityViin is not registered with the original unrestricted air spawn placement");

expectPattern(particles,
  /PAYLOAD_SPLASH_COUNT = 3[\s\S]*?PAYLOAD_SPRAY_COUNT = 5[\s\S]*?PAYLOAD_CLOUD_COUNT = 5[\s\S]*?LARGE_SPRAY_COUNT = 33[\s\S]*?LARGE_CLOUD_COUNT = 13/,
  "EntityAta/EntityViin packet type 10 or 11 particle counts are wrong");
expectPattern(particles,
  /if \(converted\) \{[\s\S]*?repeat\(level, source, 2, true\)[\s\S]*?repeat\(level, source, 3, false\)[\s\S]*?else \{[\s\S]*?repeat\(level, source, 4, false\)/,
  "EntityAta/EntityViin contact particle repetitions are wrong");
for (const [name, source] of [["EntityAta", gnat], ["EntityViin", lice]]) {
  expectPattern(source, /tickDeath\(\)[\s\S]*?sendType10Burst/,
    name + " death does not emit original packet type 10");
  expectPattern(source, /expireAndDiscard\(\)[\s\S]*?sendType11Burst/,
    name + " expiry does not emit original packet type 11");
}

const gnatContact = section(gnat, "private void contactAndDiscard", "@Override\n    public boolean onClimbable");
expectOrder(gnatContact,
  ["sendContactBursts", "playSound", "EffectStacking.apply", "discard()"],
  "EntityAta contact particles, sound, Viral and removal are out of original order");
const liceContact = section(lice, "private void contactAndDiscard", "private boolean isCharging");
expectOrder(liceContact,
  ["sendContactBursts", "EffectStacking.apply", "playSound", "discard()"],
  "EntityViin contact particles, Viral, sound and removal are out of original order");

if (failures.length) {
  console.error("Gnat/Lice port verification failed:");
  failures.forEach((failure) => console.error("- " + failure));
  process.exit(1);
}
console.log("Gnat/Lice port verification passed.");
