const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (file) => {
  const full = path.join(root, file);
  if (!fs.existsSync(full)) {
    failures.push(`missing ${file}`);
    return "";
  }
  return fs.readFileSync(full, "utf8");
};
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};
const reject = (text, pattern, message) => {
  if (pattern.test(text)) failures.push(message);
};

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const tabs = read("src/main/java/alku/csrp/Csrp.java");
const shared = read("src/main/java/alku/csrp/entity/CarrierEntity.java");

expect(shared, /LOW_HEALTH_FUSE_THRESHOLD = 0\.05F/, "carrier low-health fuse threshold is missing");
expect(shared, /level\(\)\.explode\(this, getX\(\), getY\(\), getZ\(\), 4\.0F/, "carrier explosion radius is missing");
reject(shared, /public boolean hurt\(DamageSource source, float amount\)/,
  "carrier must not turn lethal damage into a surviving fuse state");
reject(shared, /survivableDamage|setHealth\(Math\.max\(1\.0F/,
  "carrier lethal-damage health clamp is still present");
expect(shared, /super\.die\(damageSources\(\)\.mobAttack\(this\)\);\s*discard\(\);/,
  "carrier detonation must die and remove itself immediately");
expect(shared, /AreaEffectCloud/, "carrier lingering cloud is missing");
expect(shared, /ModMobEffects\.COTH/, "carrier COTH cloud effect is missing");
expect(shared, /ModMobEffects\.VIRAL/, "carrier viral cloud effect is missing");
expect(shared, /ModMobEffects\.VOMIT, vomitDuration, 0/, "carrier explosion vomit effect is missing");
expect(shared, /this::isValidParasiteTarget/, "carrier effects must exclude parasite targets");
reject(shared, /INFESTED_REMAINS|spreadResidue|placeResidueAtFloor/,
  "carrier detonation must not place blocks after death");

const carriers = {
  carrier_heavy: ["CarrierHeavyEntity.java", /super\(type, level, 70, 7\.0, 1, 1200, 600\)/],
  carrier_light: ["CarrierLightEntity.java", /super\(type, level, 70, 7\.0, 1, 300, 500\)/],
  carrier_flying: ["CarrierFlyingEntity.java", /super\(type, level, 30, 4\.0, 0, 300, 400\)/]
};

for (const [id, [javaFile, behavior]] of Object.entries(carriers)) {
  const java = read(`src/main/java/alku/csrp/entity/${javaFile}`);
  expect(entities, new RegExp(`monster\\("${id}"`), `${id} entity type is missing`);
  expect(items, new RegExp(`${id.toUpperCase()}_SPAWN_EGG`), `${id} spawn egg is missing`);
  expect(attributes, new RegExp(`ModEntities\\.${id.toUpperCase()}`), `${id} attributes are missing`);
  expect(client, new RegExp(`"${id}"`), `${id} renderer is missing`);
  expect(tabs, new RegExp(`${id.toUpperCase()}_SPAWN_EGG`), `${id} is missing from creative tabs`);
  expect(java, behavior, `${id} legacy fuse behavior is missing`);

  for (const resource of [
    `geo/${id}.geo.json`,
    `animations/${id}.animation.json`,
    `textures/entity/${id}.png`,
    `textures/item/${id}_spawn_egg.png`,
    `models/item/${id}_spawn_egg.json`
  ]) read(`src/main/resources/assets/csrp/${resource}`);

  const geometry = JSON.parse(read(`src/main/resources/assets/csrp/geo/${id}.geo.json`));
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${id}.animation.json`));
  const bones = new Set(geometry["minecraft:geometry"][0].bones.map((bone) => bone.name));
  for (const [animationName, animation] of Object.entries(animations.animations)) {
    for (const bone of Object.keys(animation.bones ?? {})) {
      if (!bones.has(bone)) failures.push(`${id}/${animationName} references missing bone ${bone}`);
    }
  }

  const loot = read(`src/main/resources/data/csrp/loot_table/entities/${id}.json`);
  expect(loot, /csrp:lurecomponent3/, `${id} common primitive drop is missing`);
}

const flying = read("src/main/java/alku/csrp/entity/CarrierFlyingEntity.java");
expect(flying, /FlyingMoveControl/, "flying carrier movement control is missing");
expect(flying, /causeFallDamage/, "flying carrier fall-damage immunity is missing");
expect(flying, /random\.nextInt\(7\)/, "flying carrier random charge is missing");

const worm = read("src/main/java/alku/csrp/entity/CarrierWormEntity.java");
expect(entities, /monster\("carrier_worm"/, "carrier_worm entity type is missing");
expect(items, /CARRIER_WORM_SPAWN_EGG/, "carrier_worm spawn egg is missing");
expect(attributes, /ModEntities\.CARRIER_WORM/, "carrier_worm attributes are missing");
expect(client, /ModEntities\.CARRIER_WORM\.get\(\), NoopRenderer::new/,
  "carrier_worm original rendererless registration is missing");
expect(tabs, /CARRIER_WORM_SPAWN_EGG/, "carrier_worm is missing from creative tabs");
expect(worm, /BODY_SEGMENTS = 4/, "carrier_worm four-part body chain is missing");
expect(worm, /return 1\.9D;/, "carrier_worm body spacing is missing");
expect(worm, /MAX_HEALTH, 77\.0D/, "carrier_worm reset health is missing");
expect(worm, /ARMOR, 20\.0D/, "carrier_worm reset armor is missing");
expect(worm, /ATTACK_DAMAGE, 22\.0D/, "carrier_worm reset attack damage is missing");
for (const resource of [
  "textures/item/carrier_worm_spawn_egg.png",
  "models/item/carrier_worm_spawn_egg.json"
]) read(`src/main/resources/assets/csrp/${resource}`);
expect(read("src/main/resources/data/csrp/loot_table/entities/carrier_worm.json"),
  /"pools"\s*:\s*\[\s*\]/, "carrier_worm original empty loot behavior is missing");

for (const id of ["pri_longarms", "pri_summoner", "pri_vermin", "pri_viscera"]) {
  const loot = read(`src/main/resources/data/csrp/loot_table/entities/${id}.json`);
  expect(loot, /csrp:lurecomponent3/, `${id} still has an adapted-only drop`);
}

if (failures.length) {
  console.error("Carrier entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log("Carrier entity port verification passed (4 entities).");
