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
const sounds = read("src/main/java/alku/csrp/registry/ModSounds.java");

const checks = {
  airscrew: ["AirscrewEntity.java", /MAX_PULL_TARGETS\s*=\s*3/, /PULL_DURATION_TICKS\s*=\s*600/,
    /PullingBallEntity/, /FlyingPathNavigation/, /MOVEMENT_SLOWDOWN[\s\S]*DIG_SLOWDOWN/],
  heed: ["HeedEntity.java", /SCENT_COOLDOWN_TICKS\s*=\s*1_000/, /RAGE_DURATION_TICKS\s*=\s*1_200/,
    /RageSkillGoal/, /WaterLeapGoal/, /HeedHeadPart/],
  dredge: ["DredgeEntity.java", /MAX_PULL_TICKS\s*=\s*200/, /PULL_STRENGTH\s*=\s*0\.13/,
    /MOVEMENT_SLOWDOWN[\s\S]*DIG_SLOWDOWN/, /MobEffects\.WEAKNESS/, /pullTarget/],
  thrall: ["ThrallEntity.java", /MAX_HEALTH,\s*40\.0/, /ATTACK_DAMAGE,\s*13\.0/,
    /entity instanceof Player/, /hasCustomName\(\)/, /0\.5F/]
};

for (const [id, [javaFile, ...patterns]] of Object.entries(checks)) {
  const constant = id.toUpperCase();
  const java = read(`src/main/java/alku/csrp/entity/${javaFile}`);
  expect(entities, new RegExp(`monster\\("${id}"`), `${id} entity type is missing`);
  expect(items, new RegExp(`${constant}_SPAWN_EGG`), `${id} spawn egg is missing`);
  expect(events, new RegExp(`ModEntities\\.${constant}`), `${id} attributes are missing`);
  expect(client, new RegExp(`ModEntities\\.${constant}`), `${id} renderer is missing`);
  patterns.forEach((pattern, index) => expect(java, pattern, `${id} legacy behavior ${index + 1} is missing`));

  for (const resource of [
    `geo/${id}.geo.json`, `animations/${id}.animation.json`, `textures/entity/${id}.png`,
    `models/item/${id}_spawn_egg.json`
  ]) read(`src/main/resources/assets/csrp/${resource}`);
  read(`src/main/resources/data/csrp/loot_tables/entities/${id}.json`);

  const geometry = JSON.parse(read(`src/main/resources/assets/csrp/geo/${id}.geo.json`));
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${id}.animation.json`));
  const bones = new Set(geometry["minecraft:geometry"][0].bones.map((bone) => bone.name));
  for (const [animationName, animation] of Object.entries(animations.animations)) {
    for (const bone of Object.keys(animation.bones ?? {})) {
      if (!bones.has(bone)) failures.push(`${id}/${animationName} references missing bone ${bone}`);
    }
  }
}

const pullBall = read("src/main/java/alku/csrp/entity/PullingBallEntity.java");
expect(entities, /PULLING_BALL/, "Airscrew pulling-ball entity is missing");
expect(client, /PullingBallRenderer/, "Airscrew pulling-ball renderer is missing");
expect(pullBall, /ModBlocks\.SRP_WEB[\s\S]*SrpWebBlock\.Kind\.THIN/,
  "Pulling ball does not create the original thin SRP web hazards on block impact");
expect(pullBall, /random\.nextInt\(3\)\s*\+\s*1/,
  "Pulling ball does not preserve the original one-to-three web count");
expect(pullBall, /captureTarget/, "Pulling ball does not hand victims to its Airscrew owner");
expect(sounds, /DREDGE_(LIVING|HURT|DEATH)/, "Dredge legacy sounds are not registered");
expect(sounds, /THRALL_(LIVING|HURT|DEATH)/, "Thrall legacy sounds are not registered");

if (failures.length) {
  console.error("Crude entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Crude entity port verification passed.");
