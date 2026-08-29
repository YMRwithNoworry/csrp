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
const primitiveVariants = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
const mobsConfig = read("src/main/java/alku/csrp/config/MobsConfig.java");
const summoner = read("src/main/java/alku/csrp/entity/SummonerEntity.java");
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const biomass = read("src/main/java/alku/csrp/entity/BiomassEntity.java");
const capacity = read("src/main/java/alku/csrp/entity/SummonCapacityTracker.java");
const biomassModel = read("src/main/java/alku/csrp/client/model/BiomassModel.java");
const biomassRenderer = read("src/main/java/alku/csrp/client/renderer/BiomassRenderer.java");
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

expect(primitiveVariants, /case MANDUCATER[\s\S]*speed = 0\.35D;[\s\S]*knockbackResistance = 0\.50D;/,
  "Primitive Manducater original movement speed or knockback resistance is missing");
expect(primitiveVariants, /new ManducaterWaterLeapGoal\(\)/,
  "Primitive Manducater original water leap is missing");
expect(primitiveVariants, /super\(PrimitiveVariantEntity\.this, 1\.30D, false\)/,
  "Primitive Manducater melee movement speed is wrong");
expect(primitiveVariants, /protected int getAttackInterval\(\)[\s\S]*return 6;/,
  "Primitive Manducater six-tick attack cadence is missing");
expect(primitiveVariants, /MANDUCATER_CAMOUFLAGED[\s\S]*MobsConfig\.manducaterNeededHealth\(\)/,
  "Primitive Manducater camouflage health gate is missing");
expect(primitiveVariants, /MobsConfig\.manducaterNeededTime\(\)/,
  "Primitive Manducater camouflage timer is missing");
expect(primitiveVariants, /(?:MobsConfig\.manducaterStealthDamageMultiplier\(\)[\s\S]*(?:EnchantmentHelper\.modifyDamage|applyManducaterStealthDamage)|applyManducaterStealthDamage[\s\S]*MobsConfig\.manducaterStealthDamageMultiplier\(\))/,
  "Primitive Manducater camouflage bonus damage is missing");
expect(primitiveVariants, /MANDUCATER_PULL_MAX_TICKS\s*=\s*200/,
  "Primitive Manducater pull duration is wrong");
expect(primitiveVariants, /MANDUCATER_PULL_MAX_DISTANCE_SQR\s*=\s*9\.0D/,
  "Primitive Manducater pull distance is wrong");
expect(primitiveVariants, /MANDUCATER_PULL_STRENGTH\s*=\s*0\.13D/,
  "Primitive Manducater pull strength is wrong");
expect(primitiveVariants, /MobEffects\.WEAKNESS, 60, 3/,
  "Primitive Manducater initial pull Weakness is missing");
expect(primitiveVariants, /MobEffects\.MOVEMENT_SLOWDOWN, 20, 1[\s\S]*MobEffects\.DIG_SLOWDOWN, 20, 1/,
  "Primitive Manducater sustained pull debuffs are missing");
expect(primitiveVariants, /new ManducaterEvadeGoal\(\)/,
  "Primitive Manducater original evade behavior is missing");
expect(mobsConfig, /manducaterNeededHealth", 0\.70D, 0\.0D, 1\.0D/,
  "Primitive Manducater camouflage health config is missing");
expect(mobsConfig, /manducaterNeededTime", 15\.0D, 1\.0D, 100\.0D/,
  "Primitive Manducater camouflage time config is missing");
expect(mobsConfig, /manducaterStealthDamageMultiplier", 2\.0D, 0\.01D, 100\.0D/,
  "Primitive Manducater stealth damage config is missing");

const checks = {
  pri_longarms: ["LongarmsEntity.java", /spawnShockwave\(target\)/, /SHOCKWAVE_COOLDOWN_TICKS\s*=\s*100/],
  pri_summoner: ["SummonerEntity.java", /BiomassEntity\.spawnFromVomit/, /SUMMON_LIMIT\s*=\s*2/],
  pri_vermin: ["VerminEntity.java", /FlyingPathNavigation/, /dropGnatBomb/],
  pri_viscera: ["VisceraEntity.java", /setClimbing\(horizontalCollision\)/, /ModMobEffects\.VIRAL[\s\S]*ModMobEffects\.BLEED/],
  gnat: ["GnatEntity.java", /new SkillLeapGoal\(\)/, /new FastMeleeAttackGoal\(\)/]
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
  read(`src/main/resources/data/csrp/loot_tables/entities/${id}.json`);
}
expect(entities, /SCARY_ORB/, "Scary Orb support entity is missing");
expect(client, /ScaryOrbRenderer/, "Scary Orb renderer is missing");

expect(entities, /register\("biomass"/, "Biomass entity type is missing");
expect(client, /ModEntities\.BIOMASS/, "Biomass renderer registration is missing");
expect(biomass, /HATCH_FUSE_TICKS\s*=\s*80/, "Biomass 80 tick hatch fuse is missing");
expect(biomass, /tickCount\s*>=\s*200/, "Biomass airborne 200 tick fuse fallback is missing");
expect(biomass, /(?:ModMobEffects\.COTH,\s*200,\s*1|applyCothEffect\(living,\s*this,\s*200,\s*1(?:,\s*false,\s*false)?\))/,
  "Biomass COTH aura duration or amplifier is wrong");
expect(biomass, /ModMobEffects\.RAGE(?:\.get\(\))?,\s*1200,\s*1/, "Biomass hatch Rage duration or amplifier is wrong");
expect(biomass, /ModMobEffects\.DEBAR(?:\.get\(\))?,\s*120000,\s*1/, "Biomass hatch Debar duration or amplifier is wrong");
expect(biomass, /(?:igniteForSeconds\(8\.0F\)|setSecondsOnFire\(8\)|setRemainingFireTicks\(8\s*\*\s*20\))/, "Biomass fire propagation must use the original eight seconds");
expect(biomass, /attacker instanceof Parasite[\s\S]*direct instanceof Parasite/, "Biomass parasite damage immunity is missing");
expect(capacity, /putUUID\("entity"/, "Summon capacity UUID persistence is missing");
expect(capacity, /replace\(UUID previousId, UUID replacementId/, "Summon capacity replacement tracking is missing");
expect(biomassModel, /applyGrowthScale[\s\S]*setScaleX[\s\S]*setScaleY[\s\S]*setScaleZ/,
  "Biomass growth is not applied to the selected original root bone");
if (biomassRenderer.includes("poseStack.scale")) {
  failures.push("Biomass renderer still scales the full pose stack instead of its selected root bone");
}
expect(summoner, /TOTAL_SUMMON_CAPACITY\s*=\s*4/, "Primitive Summoner capacity is wrong");
expect(summoner, /SUMMON_COOLDOWN_TICKS\s*=\s*200/, "Primitive Summoner cooldown is wrong");
expect(summoner, /new BiomassEntity\.SummonOption\(ModEntities\.RUPTER\.get\(\),\s*1\.0D,\s*1\)/,
  "Primitive Summoner biomass option is wrong");
expect(adapted, /SUMMONER_TOTAL_CAPACITY\s*=\s*6/, "Adapted Summoner capacity is wrong");
expect(adapted, /SUMMONER_COOLDOWN_TICKS\s*=\s*160/, "Adapted Summoner cooldown is wrong");
for (const option of [
  /ModEntities\.RUPTER\.get\(\),\s*0\.1D,\s*1/,
  /ModEntities\.SIM_HUMAN\.get\(\),\s*0\.3D,\s*2/,
  /ModEntities\.SIM_COW\.get\(\),\s*0\.3D,\s*2/,
  /ModEntities\.SIM_WOLF\.get\(\),\s*0\.3D,\s*2/
]) expect(adapted, option, "Adapted Summoner biomass option is wrong");
if (summoner.includes("ModEntities.GNAT") || summoner.includes("ScaryOrbEntity")) {
  failures.push("Primitive Summoner still uses the removed direct Gnat/Scary Orb summon path");
}
const adaptedSummon = adapted.match(/private boolean summonBiomass\(\)[\s\S]*?\n    }/);
if (!adaptedSummon || adaptedSummon[0].includes("ScaryOrbEntity")
    || adaptedSummon[0].includes("PRI_")) {
  failures.push("Adapted Summoner still directly spawns primitive bodies instead of biomass");
}
for (const resource of [
  "geo/biomass_pod.geo.json", "geo/biomass_venkrol.geo.json",
  "animations/biomass.animation.json", "particles/biomass.json",
  "textures/entity/biomass_pod.png", "textures/entity/biomass_venkrol.png",
  "textures/particle/biomass_1.png", "textures/particle/biomass_2.png",
  "textures/particle/biomass_3.png", "textures/particle/biomass_4.png"
]) read(`src/main/resources/assets/csrp/${resource}`);
const biomassAnimations = JSON.parse(read("src/main/resources/assets/csrp/animations/biomass.animation.json")).animations;
if (!biomassAnimations["animation.biomass.idle"]) failures.push("Biomass idle animation is missing");
for (const geometryFile of ["biomass_pod", "biomass_venkrol"]) {
  const geometry = JSON.parse(read(`src/main/resources/assets/csrp/geo/${geometryFile}.geo.json`));
  const bones = new Set(geometry["minecraft:geometry"].flatMap((entry) => entry.bones.map((bone) => bone.name)));
  if (!bones.has("srp_coordinate_root")) failures.push(`${geometryFile}: coordinate root is missing`);
}

if (failures.length) {
  console.error("Primitive entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log("Primitive entity port verification passed.");
