const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const entity = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
const shared = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const model = read("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");

expect(entity, /case BOLSTER -> \{[\s\S]*?health = 35\.0D[\s\S]*?armor = 4\.0D[\s\S]*?damage = 6\.0D[\s\S]*?speed = 0\.19D[\s\S]*?knockbackResistance = 0\.35D/,
  "Primitive Bolster original attributes are missing");
expect(entity, /case BOLSTER -> applyConfiguredAttributes\([\s\S]*?MobsConfig\.bolsterHealth\(\)[\s\S]*?MobsConfig\.bolsterArmor\(\)[\s\S]*?MobsConfig\.bolsterDamage\(\)[\s\S]*?MobsConfig\.bolsterKnockbackResistance\(\)/,
  "Primitive Bolster config attributes are not applied");
expect(entity, /new MeleeAttackGoal\(this, 1\.0D, false\)/,
  "Primitive Bolster melee speed is not the original 1.0");
expect(entity, /class BolsterSupportGoal extends Goal[\s\S]*?buffTimer\+\+[\s\S]*?buffTimer < 60[\s\S]*?bolsterBuffCooldownTicks\(\)/,
  "Primitive Bolster area-buff cadence is missing");
expect(entity, /entity != PrimitiveVariantEntity\.this && entity instanceof Parasite/,
  "Primitive Bolster area buff incorrectly includes itself");
if (!config.includes("primitiveBolsterEffects") || !config.includes("30;1;minecraft:regeneration")) {
  failures.push("Primitive Bolster original area effect config is missing");
}
expect(entity, /new ReekerRecruitFollowersGoal\(\)/,
  "Primitive Bolster follower recruitment is missing");
expect(entity, /BOLSTER_SKIN_VIRULENT[\s\S]*?EffectStacking\.apply\(target, ModMobEffects\.VIRAL, 40, 0\)/,
  "Virulent Primitive Bolster attack/collision effect is missing");
expect(entity, /BOLSTER_SKIN[\s\S]*?setBolsterSkin\(tag\.getInt\("bolster_skin"\)\)/,
  "Primitive Bolster skin synchronization or persistence is missing");
expect(model, /textures\/entity\/banov\.png[\s\S]*?textures\/entity\/banoh\.png/,
  "Primitive Bolster variant textures are not selected");
expect(client, /"pri_bolster", 0\.5F/,
  "Primitive Bolster original shadow radius is missing");

expect(entity, /case MANDUCATER -> \{[\s\S]*?health = 30\.0D[\s\S]*?armor = 4\.0D[\s\S]*?damage = 12\.0D[\s\S]*?speed = 0\.35D[\s\S]*?knockbackResistance = 0\.50D/,
  "Primitive Manducater original attributes are missing");
expect(entity, /class ManducaterSwimmingDivingGoal extends Goal[\s\S]*?DIVE_MOTION = 0\.095D[\s\S]*?getJumpControl\(\)\.jump\(\)/,
  "Primitive Manducater swimming/diving behavior is missing");
expect(entity, /new ManducaterWaterLeapGoal\(\)[\s\S]*?new ManducaterEvadeGoal\(\)[\s\S]*?new ManducaterMeleeGoal\(\)/,
  "Primitive Manducater dedicated movement goals are missing");
expect(entity, /super\(PrimitiveVariantEntity\.this, 1\.30D, false\)[\s\S]*?return 6;/,
  "Primitive Manducater attack speed or cadence is wrong");
expect(entity, /status > 0 && status < 3[\s\S]*?distanceToSqr\(target\) > 64\.0D[\s\S]*?distanceToSqr\(target\) < 225\.0D/,
  "Primitive Manducater evade status and distance gates are missing");
expect(entity, /updateManducaterStatus\(\)[\s\S]*?distanceToSqr\(target\) > 64\.0D \|\| manducaterAttackAnimationCooldown == 0/,
  "Primitive Manducater sprint/prepare animation state is missing");
expect(entity, /MANDUCATER_CAMOUFLAGE_CHECK_PERIOD = 21[\s\S]*?MANDUCATER_PULL_MAX_TICKS = 200[\s\S]*?MANDUCATER_PULL_MAX_DISTANCE_SQR = 9\.0D[\s\S]*?MANDUCATER_PULL_STRENGTH = 0\.13D/,
  "Primitive Manducater camouflage and pull constants are missing");
expect(entity, /MobEffects\.WEAKNESS, 60, 3[\s\S]*?MobEffects\.MOVEMENT_SLOWDOWN, 20, 1[\s\S]*?MobEffects\.DIG_SLOWDOWN, 20, 1/,
  "Primitive Manducater pull effects are incomplete");
expect(entity, /activeKind\(\) == Kind\.MANDUCATER && getManducaterTarget\(\) == entity/,
  "Primitive Manducater targeted collision suppression is missing");
expect(entity, /MANDUCATER_SKIN[\s\S]*?setManducaterSkin\(tag\.getInt\("manducater_skin"\)\)/,
  "Primitive Manducater skin synchronization or persistence is missing");
expect(model, /textures\/entity\/hullh\.png[\s\S]*?getManducaterSkin\(\) == 7/,
  "Primitive Manducater heavy texture is not selected");
expect(client, /"pri_manducater", 0\.8F/,
  "Primitive Manducater original shadow radius is missing");
expect(shared, /usesDefaultFloatGoal\(\)/,
  "shared movement goals do not allow the Manducater diving goal to replace FloatGoal");

for (const [key, value] of [
  ["primitiveBolsterHealthMultiplier", "1.0D"],
  ["primitiveBolsterDamageMultiplier", "1.0D"],
  ["primitiveBolsterArmorMultiplier", "1.0D"],
  ["primitiveBolsterKnockbackResistanceMultiplier", "1.0D"],
  ["primitiveManducaterHealthMultiplier", "1.0D"],
  ["primitiveManducaterDamageMultiplier", "1.0D"],
  ["primitiveManducaterArmorMultiplier", "1.0D"],
  ["primitiveManducaterKnockbackResistanceMultiplier", "1.0D"]
]) {
  if (!config.includes(`"${key}"`) || !config.includes(value)) {
    failures.push(`missing primitive config default: ${key}`);
  }
}
for (const resource of ["banov.png", "banoh.png", "hullh.png"]) {
  if (!fs.existsSync(path.join(root, "src/main/resources/assets/csrp/textures/entity", resource))) {
    failures.push(`missing variant texture asset: ${resource}`);
  }
}

if (failures.length) {
  console.error(`Primitive Bolster/Manducater verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Primitive Bolster and Manducater attributes, AI, variants, effects, and visuals verified.");
