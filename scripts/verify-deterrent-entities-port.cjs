const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const ids = ["dispatcherten", "kyphosis", "seizer", "sentry", "worm"];
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
const deterrent = read("src/main/java/alku/csrp/entity/DeterrentParasiteEntity.java");

expect(deterrent, /MAX_ADAPTATION_HITS\s*=\s*6/, "Deterrent damage-adaptation cap is missing");
expect(deterrent, /ADAPTATION_PER_HIT\s*=\s*0\.16F/, "Deterrent adaptation reduction is missing");
expect(deterrent, /DamageTypeTags\.IS_FIRE.*amount \* 4\.0F/, "Deterrent fire weakness is missing");
expect(deterrent, /KyphosisWaveGoal/, "Kyphosis ground-wave behavior is missing");
expect(deterrent, /SeizerHoldGoal/, "Seizer hold behavior is missing");
expect(deterrent, /SentrySpineGoal/, "Sentry spine behavior is missing");
expect(deterrent, /WormEruptionGoal/, "Worm eruption behavior is missing");
expect(deterrent, /tickDispatcherTentacle/, "Dispatcher Tentacle behavior is missing");
expect(deterrent, /source\.getDirectEntity\(\) instanceof ParasiteProjectileEntity/,
  "Seizer projectile redirection is missing");

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
  console.error("Deterrent entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Deterrent entity port verification passed (${ids.length} entities).`);
