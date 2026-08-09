const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = ["anc_dreadnaut", "anc_overlord", "anc_pod", "anc_dreadnaut_ten"];
const failures = [];
const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
};
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const creative = read("src/main/java/alku/csrp/Csrp.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");
const ancient = read("src/main/java/alku/csrp/entity/AncientParasiteEntity.java");
const pod = read("src/main/java/alku/csrp/entity/AncientPodEntity.java");
const tentacle = read("src/main/java/alku/csrp/entity/DreadnautTentacleEntity.java");
const projectile = read("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");
const mobsConfig = read("src/main/java/alku/csrp/config/MobsConfig.java");
const model = read("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");

expect(ancient, /MAX_ADAPTATION_HITS\s*=\s*10/, "Ancient adaptation hit cap is missing");
expect(ancient, /MAX_LEARNABLE_DAMAGE_SOURCES\s*=\s*5/, "Ancient learnable-source cap is missing");
expect(ancient, /DamageTypeTags\.IS_FIRE/, "Ancient fire weakness is missing");
expect(ancient, /DreadVolleyGoal/, "Dreadnaut projectile volley is missing");
expect(ancient, /DreadPodGoal/, "Dreadnaut pod summon is missing");
expect(ancient, /DreadFlightGoal/, "Dreadnaut swoop is missing");
expect(ancient, /OverlordHomingGoal/, "Overlord homing projectile is missing");
expect(ancient, /configureLegacyFireball\(this, ParasiteProjectileEntity\.Mode\.ANCIENT_BALL/,
    "Dreadnaut must fire the accelerating original Ancient Ball");
expect(ancient, /Mode\.HOMING, 0\.0D, 15\.0F, 2\.0D, 200/,
    "Overlord homing projectile contract is missing");
expect(ancient, /OVERLORD\(250\.0D, 15\.0D, 20\.0D, 0\.23D\)/,
    "Overlord legacy attributes are missing");
expect(ancient, /activeKind\(\) != Kind\.OVERLORD \|\| !effect\.is\(MobEffects\.POISON\)/,
    "Overlord poison immunity is missing");
expect(ancient, /DREAD_URTEN[\s\S]*DREAD_ULTEN[\s\S]*DREAD_RATEN[\s\S]*DREAD_LATEN/,
    "Dreadnaut detachable tendril state is missing");
expect(ancient, /isMultipartEntity\(\)[\s\S]*getParts\(\)/,
    "Ancient multipart hitboxes are missing");
expect(ancient, /ModEntities\.ANC_DREADNAUT_TEN/, "Dreadnaut detached-tendril spawn is missing");
expect(model, /bodytenbaseUR[\s\S]*bodytenbaseUL[\s\S]*bodytenbaseRA[\s\S]*bodytenbaseLA/,
    "Dreadnaut detached-tendril model visibility is missing");
expect(ancient, /maximum \* 0\.8F[\s\S]*maximum \* 0\.6F[\s\S]*maximum \* 0\.4F[\s\S]*maximum \* 0\.2F/,
    "Dreadnaut health-threshold order is missing");
expect(ancient, /MobsConfig\.ancientDreadnautPodCooldownTicks\(\)/,
    "Dreadnaut pod cooldown is not config driven");
expect(ancient, /MobsConfig\.ancientDreadnautPodNumber\(\)/,
    "Dreadnaut pod count is not config driven");
expect(ancient, /MobsConfig\.ancientDreadnautMinY\(\)/,
    "Dreadnaut minimum flight limit is not config driven");
expect(ancient, /MobsConfig\.ancientDreadnautMaxY\(\)/,
    "Dreadnaut maximum flight limit is not config driven");
if (/triggerAncientDeathBurst|deathBurstFired/.test(ancient)) {
  failures.push("Dreadnaut still contains the non-legacy random death burst");
}
expect(projectile, /HOMING/, "Original homing projectile mode is missing");
expect(projectile, /mode != Mode\.HOMING && blockHit/, "Original block-piercing homing movement is missing");
expect(projectile, /mode == Mode\.HOMING[\s\S]*0\.075D/, "Original homing acceleration is missing");
expect(projectile, /spawnLingeringAncientCloud[\s\S]*setRadius\(1\.2F\)[\s\S]*setRadiusOnUse\(-0\.5F\)/,
    "Original Ancient Ball cloud geometry is missing");
expect(projectile, /MobEffects\.WITHER, 300, 0[\s\S]*ModMobEffects\.COTH, 3600, 0/,
    "Original Ancient Ball cloud effects are missing");
expect(pod, /spawnContents/, "Ancient drop-pod payload behavior is missing");
expect(pod, /owner == 62[\s\S]*owner == 63/, "Ancient drop-pod owner payload mapping is missing");
expect(pod, /MobsConfig\.ancientDreadnautPodMaxMobs\(\)/,
    "Ancient drop-pod payload cap is not config driven");
expect(pod, /MobEffects\.POISON, 300, 0/, "Ancient drop-pod poison cloud is missing");
expect(pod, /ModMobEffects\.COTH, 3600, 0/, "Ancient drop-pod COTH cloud is missing");
expect(pod, /KNOCKBACK_RESISTANCE, 2\.0D/, "Ancient drop-pod knockback resistance is missing");
expect(mobsConfig, /ancientDreadnautPodCooldownSeconds[\s\S]*ancientDreadnautPodNumber[\s\S]*ancientDreadnautPodMaxMobs/,
    "Ancient Dreadnaut original pod config is missing");
expect(tentacle, /nearbyNonParasitesHaveAdvantage/, "Dreadnaut tendril balance check is missing");
expect(tentacle, /spawnBuglin/, "Dreadnaut tendril Buglin summon is missing");
expect(tentacle, /KNOCKBACK_RESISTANCE, 2\.0D/, "Dreadnaut tendril knockback resistance is missing");

for (const id of ids) {
  const constant = id.toUpperCase();
  expect(entities, new RegExp(`monster\\("${id}"`), `${id}: entity registration is missing`);
  expect(items, new RegExp(`"${id}_spawn_egg"`), `${id}: spawn egg is missing`);
  expect(attributes, new RegExp(`ModEntities\\.${constant}`), `${id}: attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id}: renderer is missing`);
  expect(creative, new RegExp(`ModItems\\.${constant}_SPAWN_EGG`), `${id}: creative tab entry is missing`);
  expect(english, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: English translation is missing`);
  expect(chinese, new RegExp(`"entity\\.csrp\\.${id}"`), `${id}: Chinese translation is missing`);

  for (const relative of [
    `src/main/resources/assets/csrp/geo/${id}.geo.json`,
    `src/main/resources/assets/csrp/animations/${id}.animation.json`,
    `src/main/resources/assets/csrp/textures/entity/${id}.png`,
    `src/main/resources/assets/csrp/models/item/${id}_spawn_egg.json`,
    `src/main/resources/assets/csrp/textures/item/${id}_spawn_egg.png`,
    `src/main/resources/data/csrp/loot_table/entities/${id}.json`
  ]) read(relative);
}

if (failures.length) {
  console.error("Ancient entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Ancient entity port verification passed (${ids.length} entities).`);
