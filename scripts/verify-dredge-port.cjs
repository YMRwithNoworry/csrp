const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const dredge = read("src/main/java/alku/csrp/entity/DredgeEntity.java");
const follow = read("src/main/java/alku/csrp/entity/ParasiteFollowGoal.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");

expect(entities, /monster\("dredge", DredgeEntity::new, 0\.8F, 3\.4F, 1\.73F\)/,
  "Dredge dimensions or eye height do not match EntityDone");
expect(dredge,
  /MAX_HEALTH, 40\.0D[\s\S]*?ARMOR, 9\.0D[\s\S]*?ATTACK_DAMAGE, 15\.0D[\s\S]*?MOVEMENT_SPEED, 0\.4D[\s\S]*?KNOCKBACK_RESISTANCE, 0\.7D[\s\S]*?FOLLOW_RANGE, 32\.0D/,
  "Dredge attributes are incomplete");
expect(dredge, /xpReward\s*=\s*30/, "Dredge XP reward is not the primitive value");
expect(dredge, /incomingDamageCapDivisor\(\)[\s\S]*?return 6;/,
  "Dredge primitive damage cap divisor is not six");
expect(dredge,
  /addGoal\(0, new DredgeSwimmingGoal\(\)\)[\s\S]*?addGoal\(3, new DredgeMeleeGoal\(\)\)[\s\S]*?addGoal\(6, new RecruitFollowersGoal\(\)/,
  "Dredge movement and melee goal priorities are incomplete");
expect(dredge,
  /targetSelector\.addGoal\(1, new HurtByTargetGoal\(this\)[\s\S]*?Player\.class, 0[\s\S]*?Mob\.class, 0/,
  "Dredge target priorities or zero search interval are incomplete");
expect(dredge, /WaterAnimal[\s\S]*?Animal[\s\S]*?Villager[\s\S]*?mobAttackingBlacklist/,
  "Dredge mob target exclusions are incomplete");
expect(dredge, /TARGET_ENTITY[\s\S]*?EntityDataSerializers\.INT[\s\S]*?targetedEntity/,
  "Dredge pull target is not synchronized by entity ID");
expect(dredge, /canPull\s*=\s*true[\s\S]*?pulling\+\+[\s\S]*?MAX_PULL_TICKS[\s\S]*?canPull\s*=\s*false/,
  "Dredge pull state cooldown is incomplete");
expect(dredge, /applyPrimitiveMinimumDamage\(target, 0\.02F\)/,
  "Dredge pull state does not apply the original minimum damage");
expect(dredge, /MobEffects\.WEAKNESS, 60, 3[\s\S]*?MOVEMENT_SLOWDOWN, 20, 1[\s\S]*?DIG_SLOWDOWN, 20, 1/,
  "Dredge pull effects are incomplete");
expect(dredge,
  /MAX_LIQUID_LEAPS\s*=\s*8[\s\S]*?LIQUID_LEAP_INTERVAL_TICKS\s*=\s*21[\s\S]*?LIQUID_LEAP_HORIZONTAL_SPEED\s*=\s*1\.2D[\s\S]*?LIQUID_LEAP_VERTICAL_SPEED\s*=\s*0\.3D/,
  "Dredge liquid leap constants are incomplete");
expect(dredge, /MELEE_ATTACK_INTERVAL_TICKS\s*=\s*10/,
  "Dredge melee cadence is not the legacy 10 ticks");
expect(dredge, /REGENERATION_AMOUNT\s*=\s*4\.0F[\s\S]*?consumeParasiteKill\(\)/,
  "Dredge primitive regeneration is incomplete");
expect(dredge, /getParasiteStatus\(\) == STATUS_IDLE[\s\S]*?getAdaptationHitStatus\(\) > 0/,
  "Dredge ambient or adaptation-silence sound behavior is incomplete");
expect(follow, /parasite instanceof DredgeEntity[\s\S]*?return 31;/,
  "Dredge command rank 31 is missing");

for (const forbidden of ["UUID", "putUUID", "pull_target", "spawnGore", "EntityRemain", "BlockGore"]) {
  if (dredge.includes(forbidden)) failures.push(`Dredge retains forbidden transient/death behavior: ${forbidden}`);
}

if (failures.length) {
  console.error("Dredge port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log("Dredge port verification passed.");
