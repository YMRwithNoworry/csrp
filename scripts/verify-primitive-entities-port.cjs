const fs = require("fs");
const path = require("path");
const root = path.resolve(__dirname, "..");
const failures = [];
const read = (file) => {
  const full = path.join(root, file);
  if (!fs.existsSync(full)) { failures.push(`missing ${file}`); return ""; }
  return fs.readFileSync(full, "utf8");
};
const expect = (text, pattern, message) => { if (!pattern.test(text)) failures.push(message); };

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const shared = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const combat = read("src/main/java/alku/csrp/entity/ParasiteCombatEffects.java");
const summoner = read("src/main/java/alku/csrp/entity/SummonerEntity.java");
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const projectile = read("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");
expect(shared, /parasitekills/, "legacy parasite kill state is missing");
expect(shared, /damageAdaptations/, "primitive damage adaptation is missing");
expect(combat, /ModMobEffects\.VOMIT/, "shared original Vomit cloud effect is missing");
expect(summoner, /VOMIT_COOLDOWN_TICKS\s*=\s*180/, "primitive Summoner ranged cadence is missing");
expect(summoner, /spawnVomitCloud\(SummonerEntity\.this,\s*5\.5D, 4\.0F, 100, 300, 25\)/s,
  "primitive Summoner original Vomit cloud is missing");
expect(adapted, /spawnVomitCloud\(AdaptedVariantEntity\.this,\s*6\.5D, 5\.0F, 100, 300, 40\)/s,
  "adapted Summoner original Vomit cloud is missing");
expect(projectile, /case VOMIT[\s\S]*ModMobEffects\.VOMIT/, "Vomit projectiles do not apply Vomit");

const checks = {
  pri_longarms: ["LongarmsEntity.java", /spawnShockwave\(target\)/, /SHOCKWAVE_COOLDOWN_TICKS\s*=\s*100/],
  pri_summoner: ["SummonerEntity.java", /for \(int i = 0; i < 3; i\+\+\)/, /ScaryOrbEntity/],
  pri_vermin: ["VerminEntity.java", /FlyingPathNavigation/, /dropGnatBomb/],
  pri_viscera: ["VisceraEntity.java", /setClimbing\(horizontalCollision\)/, /ModMobEffects\.VIRAL[\s\S]*ModMobEffects\.BLEED/],
  gnat: ["GnatEntity.java", /createAnimatedLeapGoal\(0\.4F, 20\)/, /MeleeAttackGoal/]
};
for (const [id, [javaFile, first, second]] of Object.entries(checks)) {
  const java = read(`src/main/java/alku/csrp/entity/${javaFile}`);
  expect(entities, new RegExp(`monster\\("${id}"`), `${id} entity type is missing`);
  expect(items, new RegExp(`${id.toUpperCase()}_SPAWN_EGG`), `${id} spawn egg is missing`);
  expect(client, new RegExp(`"${id}"`), `${id} renderer is missing`);
  expect(java, first, `${id} first legacy behavior is missing`);
  expect(java, second, `${id} second legacy behavior is missing`);
  for (const resource of [`geo/${id}.geo.json`, `animations/${id}.animation.json`,
      `textures/entity/${id}.png`, `textures/item/${id}_spawn_egg.png`, `models/item/${id}_spawn_egg.json`]) {
    read(`src/main/resources/assets/csrp/${resource}`);
  }
  const geometry = JSON.parse(read(`src/main/resources/assets/csrp/geo/${id}.geo.json`));
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${id}.animation.json`));
  const bones = new Set(geometry["minecraft:geometry"][0].bones.map((bone) => bone.name));
  for (const [animationName, animation] of Object.entries(animations.animations)) {
    for (const bone of Object.keys(animation.bones ?? {})) {
      if (!bones.has(bone)) failures.push(`${id}/${animationName} references missing bone ${bone}`);
    }
  }
  read(`src/main/resources/data/csrp/loot_table/entities/${id}.json`);
}
expect(entities, /SCARY_ORB/, "Scary Orb support entity is missing");
expect(client, /ScaryOrbRenderer/, "Scary Orb renderer is missing");

if (failures.length) {
  console.error("Primitive entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log("Primitive entity port verification passed.");
