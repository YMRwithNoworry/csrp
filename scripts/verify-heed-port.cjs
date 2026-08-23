const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const heed = read("src/main/java/alku/csrp/entity/HeedEntity.java");
const config = read("src/main/java/alku/csrp/Config.java");
const follow = read("src/main/java/alku/csrp/entity/ParasiteFollowGoal.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");

expect(entities, /monster\("heed", HeedEntity::new, 0\.9F, 1\.9F\)/,
  "EntityHeed body is not the original 0.9 x 1.9");
expect(client, /ModEntities\.HEED[\s\S]*?"heed", 0\.8F/,
  "EntityHeed shadow radius is not the original 0.8");
expect(heed,
  /MAX_HEALTH, 50\.0D[\s\S]*?ARMOR, 9\.0D[\s\S]*?ATTACK_DAMAGE, 15\.0D[\s\S]*?MOVEMENT_SPEED, 0\.32D[\s\S]*?KNOCKBACK_RESISTANCE, 0\.7D[\s\S]*?FOLLOW_RANGE, 32\.0D[\s\S]*?(?:STEP_HEIGHT|STEP_HEIGHT_ADDITION).*?1\.0D/,
  "EntityHeed original attributes are missing");
expect(heed,
  /addGoal\(0, new SwimmingDivingGoal\(\)\)[\s\S]*?addGoal\(2, new WaterLeapGoal\(\)\)[\s\S]*?addGoal\(2, new RageSkillGoal\(\)\)[\s\S]*?addGoal\(3, new HeedMeleeGoal\(\)\)[\s\S]*?addGoal\(6, new RecruitFollowersGoal\(\)\)/,
  "EntityHeed original goal priorities are missing");
expect(heed,
  /targetSelector\.addGoal\(1, new HurtByTargetGoal\(this\)\.setAlertOthers\(\)\)[\s\S]*?Player\.class, 0[\s\S]*?Mob\.class, 0/,
  "EntityHeed target priorities or zero search interval are missing");
expect(heed, /WaterAnimal[\s\S]*?Animal[\s\S]*?Villager/,
  "EntityHeed mob target exclusions are incomplete");
expect(heed, /MELEE_ATTACK_INTERVAL_TICKS\s*=\s*10[\s\S]*?MELEE_SPRINT_DISTANCE_SQR\s*=\s*8\.0D \* 8\.0D/,
  "EntityHeed melee cadence or sprint distance is wrong");
expect(heed,
  /WATER_LEAP_CHARGE_TICKS\s*=\s*20[\s\S]*?-0\.095D[\s\S]*?1\.5D \* 0\.9D[\s\S]*?0\.7D \+ targetY/,
  "EntityHeed swimming or water leap behavior is incomplete");
expect(heed,
  /RAGE_SKILL_COOLDOWN_TICKS\s*=\s*200[\s\S]*?RAGE_DURATION_TICKS\s*=\s*1_200[\s\S]*?RAGE_TARGET_RANGE_SQR\s*=\s*20\.0D \* 20\.0D/,
  "EntityHeed Rage timing or target range is wrong");
expect(heed,
  /generationProfile\(serverLevel\)\.specialMoves\(\)[\s\S]*?getBoundingBox\(\)\.inflate\(RAGE_EFFECT_RANGE\)[\s\S]*?ModMobEffects\.RAGE/,
  "EntityHeed Rage skill is not generation-gated or does not affect nearby parasites");
expect(heed,
  /SCENT_COOLDOWN_TICKS\s*=\s*1_000[\s\S]*?ubiquitousDevelopment[\s\S]*?tickCount % 21 != 10[\s\S]*?setTargetToKill\(target, false\)[\s\S]*?setDieAfterKilling\(true\)[\s\S]*?setCanFollow\(true\)/,
  "EntityHeed Scent creation does not match the original conditions");
expect(heed,
  /RECRUIT_RANGE\s*=\s*16\.0D[\s\S]*?commandRank\(candidate\) < 41[\s\S]*?commandRank\(leader\) <= 30/,
  "EntityHeed follower recruitment does not match version two");
expect(follow, /parasite instanceof HeedEntity[\s\S]*?return 31;/,
  "EntityHeed does not retain its original command rank 31");
expect(heed,
  /amount \* 3\.0F[\s\S]*?PartEntity<HeedEntity>[\s\S]*?scalable\(1\.8F, 1\.8F\)[\s\S]*?getEyeHeight\([\s\S]*?0\.2F/,
  "EntityHeed vulnerable head dimensions or damage multiplier are wrong");
expect(heed, /random\.nextBoolean\(\)[\s\S]*?EffectStacking\.apply\(this, ModMobEffects\.BLEED(?:\.get\(\))?, 80, 0\)/,
  "EntityHeed head hits do not preserve the original Bleed chance");
expect(heed, /(?:withEyeHeight\(1\.5F\)|getEyeHeight\([\s\S]*?1\.5F)/,
  "EntityHeed eye height is not the original 1.5");
expect(config,
  /define\("rageEnabled", true\)[\s\S]*?define\("mobAttackingEnabled", true\)[\s\S]*?define\("collectiveConsciousnessEnabled", true\)/,
  "EntityHeed dependencies are not exposed through configuration");

if (failures.length) {
  console.error("Heed port verification failed:");
  failures.forEach((failure) => console.error("- " + failure));
  process.exit(1);
}
console.log("Heed port verification passed.");
