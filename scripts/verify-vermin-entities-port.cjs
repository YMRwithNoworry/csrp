const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};
const expectPattern = (source, pattern, message) => expect(pattern.test(source), message);
const hash = (relative) => crypto.createHash("sha256")
  .update(fs.readFileSync(path.join(root, relative))).digest("hex");

const primitive = read("src/main/java/alku/csrp/entity/VerminEntity.java");
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const shared = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const renderers = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const particles = read("src/main/java/alku/csrp/registry/ModParticles.java");
const payloadParticles = read("src/main/java/alku/csrp/entity/VerminParticles.java");
const splash = read("src/main/java/alku/csrp/client/particle/AssimilationSplashParticle.java");
const cloud = read("src/main/java/alku/csrp/client/particle/GoreCloudParticle.java");
const cloudDescription = JSON.parse(
  read("src/main/resources/assets/csrp/particles/gore_cloud.json"));

expectPattern(shared, /protected boolean usesDefaultTargetGoals\(\)[\s\S]*?return true;/,
  "shared target-goal opt-out hook is missing");
expectPattern(primitive, /usesDefaultTargetGoals\(\)[\s\S]*?return false;/,
  "Primitive Vermin still installs the generic all-living target goal");
expectPattern(adapted, /usesDefaultTargetGoals\(\)[\s\S]*?activeKind\(\) != Kind\.VERMIN/,
  "Adapted Vermin still installs the generic all-living target goal");

expectPattern(primitive,
  /MAX_HEALTH, 45\.0[\s\S]*?ARMOR, 15\.0[\s\S]*?ATTACK_DAMAGE, 30\.0[\s\S]*?KNOCKBACK_RESISTANCE, 0\.65[\s\S]*?FOLLOW_RANGE, 32\.0/,
  "Primitive Vermin does not use EntityIki's effective original attributes");
expectPattern(adapted,
  /case VERMIN -> \{[\s\S]*?health = 70\.0D[\s\S]*?armor = 15\.0D[\s\S]*?damage = 30\.0D[\s\S]*?knockbackResistance = 0\.65D[\s\S]*?followRange = 32\.0D/,
  "Adapted Vermin original attributes are missing");
for (const id of ["PRI_VERMIN", "ADA_VERMIN"]) {
  expectPattern(entities, new RegExp(id + "[\\s\\S]*?1\\.1F, 1\\.4F, 0\\.7F"),
    id + " does not use the original 1.1 x 1.4 body and 0.7 eye height");
  expectPattern(renderers, new RegExp(id + "[\\s\\S]*?\\\"" +
    (id === "PRI_VERMIN" ? "pri_vermin" : "ada_vermin") + "\\\", 0\\.2F"),
    id + " does not use the original 0.2 shadow radius");
}

expectPattern(primitive,
  /addGoal\(3, new FlightAttackGoal\(\)\)[\s\S]*?addGoal\(4, new ChargeAttackGoal\(\)\)[\s\S]*?addGoal\(5, new DropPayloadGoal\(\)\)[\s\S]*?addGoal\(7, new RandomFlightGoal\(\)\)/,
  "Primitive Vermin original goal priorities are missing");
const adaptedVerminGoals = adapted.match(
  /case VERMIN -> \{\s*registerVerminTargetGoals\(\)[\s\S]*?case VISCERA ->/)?.[0] ?? "";
expectPattern(adaptedVerminGoals,
  /addGoal\(3, new VerminFlightAttackGoal\(\)\)[\s\S]*?addGoal\(5, new VerminPayloadGoal\(\)\)[\s\S]*?addGoal\(7, new VerminRandomFlightGoal\(\)\)/,
  "Adapted Vermin original goal priorities are missing");
expect(!adaptedVerminGoals.includes("ChargeAttack"),
  "Adapted Vermin incorrectly registers EntityIkiAdapted's unused charge class");

for (const source of [primitive, adapted]) {
  expectPattern(source, /cycleTick > 0 && cycleTick <= 10/,
    "Vermin flight attack does not use the original 1-10/21 tick window");
  expectPattern(source, /lostTargetTicks >= 6/,
    "Vermin flight attack does not clear an unreachable target after six checks");
  expectPattern(source,
    /instanceof Animal[\s\S]*?instanceof Creeper[\s\S]*?instanceof WaterAnimal/,
    "Vermin flight scan does not exclude animals, Creepers and water mobs");
  expectPattern(source, /instanceof WaterAnimal[\s\S]*?instanceof Animal[\s\S]*?instanceof Villager/,
    "Vermin base target goal does not exclude water mobs, animals and Villagers");
}
expectPattern(primitive,
  /distance > 100\.0D[\s\S]*?nextInt\(6\) - 2[\s\S]*?nextInt\(7\) - 2[\s\S]*?nextInt\(4\) \+ 3[\s\S]*?nextInt\(5\) \+ 4/,
  "Primitive Vermin original random-flight distance modes are missing");
expectPattern(adapted,
  /distance > 225\.0D \|\| distance < 36\.0D[\s\S]*?nextInt\(15\) - 7[\s\S]*?nextInt\(11\) - 5/,
  "Adapted Vermin original random-flight range is missing");
for (const source of [primitive, adapted]) {
  expectPattern(source, /arrivalRadius = \(getBbWidth\(\) \* 2\.0D \+ getBbHeight\(\)\) \/ 3\.0D/,
    "Vermin move control does not stop at the original bounding-box average");
  expectPattern(source,
    /configure\([\s\S]*?60, 0\.0F,[\s\S]*?Attributes\.ATTACK_DAMAGE[\s\S]*?2, 1, false\)/,
    "Vermin bomb fuse, strength, damage, range or skin differs from the original");
  expectPattern(source, /getXRot\(\) \+ 20\.0F/,
    "Vermin bomb does not use the original +20 degree pitch");
}
expectPattern(primitive, /if \(!target\.onGround\(\)\)[\s\S]*?checkTicks = 10/,
  "Primitive Vermin payload goal does not defer airborne targets by ten checks");
expectPattern(primitive, /ModEntities\.GNAT\.get\(\)\.create\(serverLevel\)/,
  "Primitive Vermin does not directly construct its original EntityAta payload");
expect(!primitive.includes("MobSpawnType.MOB_SUMMONED"),
  "Primitive Vermin payload incorrectly runs modern spawn finalization");
expect(!adaptedVerminGoals.includes("onGround"),
  "Adapted Vermin incorrectly requires a grounded payload target");
expectPattern(adapted, /case VERMIN -> ModEntities\.MOVINGFLESH/,
  "Adapted Vermin colony death protection does not create EntityLesh/Moving Flesh");

expectPattern(particles, /GORE_CLOUD[\s\S]*?register\("gore_cloud"/,
  "original GCLOUD particle is not registered");
expectPattern(renderers, /ModParticles\.GORE_CLOUD[\s\S]*?GoreCloudParticle\.Provider/,
  "GCLOUD particle provider is not registered");
expectPattern(payloadParticles,
  /PAYLOAD_SPLASH_COUNT = 3[\s\S]*?PAYLOAD_SPRAY_COUNT = 5[\s\S]*?PAYLOAD_CLOUD_COUNT = 5/,
  "payload packet type 10 does not retain its original 3 + 5 + 5 particle composition");
expectPattern(payloadParticles,
  /sendParticles\(ModParticles\.GORE_CLOUD[\s\S]*?PAYLOAD_CLOUD_COUNT/,
  "payload burst does not emit the five red GCLOUD particles");
expectPattern(payloadParticles, /AMBIENT_SPLASH_COUNT = 5/,
  "Vermin ambient mouth drip count is not the original five");
for (const source of [primitive, adapted]) {
  expectPattern(source, /VerminParticles\.spawnMouthDrips/,
    "Vermin does not use the original mouth-drip burst");
  expectPattern(source, /VerminParticles\.sendPayloadBurst/,
    "Vermin does not use the original payload particle burst");
}
expectPattern(splash, /int previousAge = age\+\+[\s\S]*?lifetime-- <= 0/,
  "GSPLASH does not preserve the original independent age and lifetime countdown");
expectPattern(cloud,
  /setColor\(127\.0F \/ 255\.0F, 0\.0F, 0\.0F\)[\s\S]*?quadSize \*= 0\.75F[\s\S]*?quadSize \*= 2\.5F/,
  "payload GCLOUD does not use the original red color and scale");
expectPattern(cloud, /getNearestPlayer[\s\S]*?getBoundingBox\(\)\.minY/,
  "payload GCLOUD does not preserve the original player-relative drift");
expect(cloudDescription.textures.join(",") ===
  Array.from({length: 8}, (_, index) => "minecraft:generic_" + (7 - index)).join(","),
  "GCLOUD sprite order does not match the original smoke animation");

const expectedSplashHashes = [
  "4b04b1a7577600e6761e29b31c73d8ef859b150e9b58c8668efa22b8a4a9e584",
  "d9da47a15c13680cb87336c65cf31b74f386697e3cbafd9eff3c5263d2783136",
  "d3dbe0a335eefd66e6967f352620bbd524d1e0092c6cde56c8243dea7ca5536a"
];
for (let index = 0; index < expectedSplashHashes.length; index++) {
  const relative = "src/main/resources/assets/csrp/textures/particle/assimilation_splash_" +
    (index + 1) + ".png";
  expect(hash(relative) === expectedSplashHashes[index],
    relative + " differs from the original flesh_assimilated texture");
}

if (failures.length) {
  console.error("Vermin entity port verification failed:");
  failures.forEach((failure) => console.error("- " + failure));
  process.exit(1);
}
console.log("Vermin entity port verification passed.");
