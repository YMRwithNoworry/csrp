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

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const events = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const mod = read("src/main/java/alku/csrp/Csrp.java");

const checks = {
  gnat: ["GnatEntity.java", /MAX_LIFETIME_TICKS\s*=\s*1_200/,
    /return false;[\s\S]*void push\(Entity entity\)[\s\S]*entity == getTarget\(\)/,
    /convertFeralEndermanHost\(target\)[\s\S]*convertGnatHost\(target\)/,
    /EffectStacking\.apply\(target, ModMobEffects\.VIRAL, VIRAL_DURATION_TICKS, VIRAL_AMPLIFIER\)/,
    /distance >= 60\.0F[\s\S]*super\.causeFallDamage/,
    /FastMeleeAttackGoal[\s\S]*getTicksUntilNextAttack\(\)[\s\S]*return 6;/,
    /SwimmingDivingGoal[\s\S]*-0\.12D[\s\S]*random\.nextFloat\(\) < 0\.8F/,
    /ModSounds\.get\("buthol\.boom"\)/],
  lice: ["LiceEntity.java", /MAX_LIFESPAN_TICKS\s*=\s*1200/, /VIRAL_DURATION_TICKS\s*=\s*120/,
    /VIRAL_AMPLIFIER\s*=\s*2/, /FlyingPathNavigation/, /ChargeAttackGoal/,
    /void push\(Entity entity\)[\s\S]*entity == getTarget\(\)/,
    /convertFeralEndermanHost\(target\)[\s\S]*convertGnatHost\(target\)/,
    /EffectStacking\.apply\(target, ModMobEffects\.VIRAL, VIRAL_DURATION_TICKS, VIRAL_AMPLIFIER\)/,
    /getMoveControl\(\)\.hasWanted\(\)[\s\S]*random\.nextInt\(7\)/,
    /setWantedPosition\([\s\S]*0\.25D\)/,
    /ModSounds\.get\("buthol\.boom"\), 0\.4F/],
  mangler: ["ManglerEntity.java", /MAX_HEALTH,\s*17\.0/, /ARMOR,\s*10\.0/,
    /ATTACK_DAMAGE,\s*9\.0/, /MOVEMENT_SPEED,\s*0\.37/, /onClimbable\(\)/,
    /createAnimatedLeapGoal\(0\.8F,\s*20\)[\s\S]*new LeapAtTargetGoal\(this,\s*0\.4F\)/,
    /DASH_COOLDOWN_TICKS\s*=\s*10[\s\S]*MAX_DASH_DISTANCE_SQR\s*=\s*225\.0D/,
    /WallClimberNavigation[\s\S]*setClimbing\(horizontalCollision && canClimbForTarget\(\)\)/,
    /FastMeleeAttackGoal[\s\S]*getTicksUntilNextAttack\(\)[\s\S]*return 6;/,
    /SwimmingDivingGoal[\s\S]*-0\.12D[\s\S]*random\.nextFloat\(\) < 0\.8F/,
    /movement\.x \* 0\.2D \+ direction\.x \* 0\.8D[\s\S]*movement\.z \* 0\.2D \+ direction\.z \* 0\.8D/,
    /ModSounds\.get\("small\.step"\)[\s\S]*ModSounds\.get\("nuuh\.growl"\)[\s\S]*ModSounds\.get\("nuuh\.hurt"\)[\s\S]*ModSounds\.get\("nuuh\.death"\)/],
  host: ["HostEntity.java", /createHostAttributes\(50\.0,\s*7\.0,\s*10\.0/,
    /BURROW_DURATION_TICKS/, /performShockwave/, /summonRupters/,
    /ModEntities\.HOSTII/],
  hostii: ["HostIIEntity.java", /createHostAttributes\(140\.0,\s*12\.0,\s*18\.0/,
    /performBombAttack/, /performSpineBallAttack/, /summonManglers/],
  incompleteform_small: ["IncompleteFormSmallEntity.java", /MAX_HEALTH,\s*9\.0/,
    /ATTACK_DAMAGE,\s*8\.0/, /MOVEMENT_SPEED,\s*0\.12/],
  incompleteform_medium: ["IncompleteFormMediumEntity.java", /MAX_HEALTH,\s*14\.0/,
    /ATTACK_DAMAGE,\s*11\.0/, /MOVEMENT_SPEED,\s*0\.15/],
  draconite: ["DraconiteEntity.java", /MAX_HEALTH,\s*525\.0/, /ATTACK_DAMAGE,\s*210\.0/,
    /MOVEMENT_SPEED,\s*0\.27/, /switchFlightMode/, /spawnToxicCloud/,
    /beginMeteorRain[\s\S]*spawnMeteor/, /performLightBarrage/],
  kirin: ["KirinEntity.java", /MAX_HEALTH,\s*410\.0/, /ATTACK_DAMAGE,\s*155\.0/,
    /MOVEMENT_SPEED,\s*0\.24/, /BLINK_CHARGE_TICKS\s*=\s*60/,
    /BLINK_COOLDOWN_TICKS\s*=\s*200/, /BLINK_LIFE_STEAL_RADIUS\s*=\s*5\.0/,
    /BLINK_HEALTH_DRAIN_FRACTION\s*=\s*0\.5/, /summonVoidOrb/]
};

expect(entities, /monster\("gnat", GnatEntity::new, 0\.85F, 1\.0F\)/,
  "Gnat dimensions do not match EntityAta");

for (const [id, [javaFile, ...patterns]] of Object.entries(checks)) {
  const constant = id.toUpperCase();
  const java = read(`src/main/java/alku/csrp/entity/${javaFile}`);
  expect(entities, new RegExp(`(?:monster\\("${id}"|ENTITIES\\.register\\("${id}")`),
    `${id} entity type is missing`);
  expect(items, new RegExp(`${constant}_SPAWN_EGG`), `${id} spawn egg is missing`);
  expect(events, new RegExp(`ModEntities\\.${constant}`), `${id} attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id} renderer is missing`);
  expect(mod, new RegExp(`${constant}_SPAWN_EGG`), `${id} spawn egg is absent from creative tabs`);
  patterns.forEach((pattern, index) => expect(java, pattern, `${id} legacy behavior ${index + 1} is missing`));

  for (const resource of [
    `geo/${id}.geo.json`, `animations/${id}.animation.json`, `textures/entity/${id}.png`,
    `models/item/${id}_spawn_egg.json`
  ]) read(`src/main/resources/assets/csrp/${resource}`);
  read(`src/main/resources/data/csrp/loot_table/entities/${id}.json`);

  const geometryText = read(`src/main/resources/assets/csrp/geo/${id}.geo.json`);
  const animationText = read(`src/main/resources/assets/csrp/animations/${id}.animation.json`);
  if (geometryText && animationText) {
    const geometry = JSON.parse(geometryText);
    const animations = JSON.parse(animationText);
    const bones = new Set(geometry["minecraft:geometry"][0].bones.map((bone) => bone.name));
    for (const [animationName, animation] of Object.entries(animations.animations)) {
      for (const bone of Object.keys(animation.bones ?? {})) {
        if (!bones.has(bone)) failures.push(`${id}/${animationName} references missing bone ${bone}`);
      }
    }
  }
}

const evolution = read("src/main/java/alku/csrp/entity/ManglerEvolutionTarget.java");
const airscrew = read("src/main/java/alku/csrp/entity/AirscrewEntity.java");
expect(mod, /ManglerEvolutionTarget\.registerMangler\(ModEntities\.MANGLER\)/,
  "Rupter evolution target is not wired to Mangler");
expect(evolution, /registerMangler/, "Mangler evolution bridge is missing");
expect(airscrew, /INCOMPLETEFORM_SMALL|INCOMPLETE_FORM_SMALL/,
  "Airscrew death does not create incomplete small forms");
expect(airscrew, /INCOMPLETEFORM_MEDIUM|INCOMPLETE_FORM_MEDIUM/,
  "Airscrew death does not create incomplete medium forms");

if (failures.length) {
  console.error("Early lifecycle entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Early lifecycle entity port verification passed.");
