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

const config = read("src/main/java/alku/csrp/Config.java");
const system = read("src/main/java/alku/csrp/world/EvolutionSystem.java");
const events = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");
const primitive = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/ParasiteGeoRenderer.java");

expect(config, /define\("generationEnabled", true\)/,
  "original-default generationEnabled config is missing");
expect(config, /boolean generationEnabled\(\).*GENERATION_ENABLED\.get\(\)/,
  "generationEnabled accessor is missing");
expect(system, /GENERATION_ADAPTATION\s*=\s*\{false, false, false, true, true, true\}/,
  "adaptation must unlock at generation 3");
expect(system, /Config\.generationEnabled\(\)\s*\?\s*SrpWorldData\.get\(level\)\.generation\(\)\s*:\s*5/,
  "disabling generations must select the full generation-5 profile");
expect(events, /if \(Config\.generationEnabled\(\)\)\s*\{\s*data\.tickGeneration\(level, 20\)/,
  "generation ticks must pause while the generation system is disabled");
expect(commands, /literal\("status"\)[\s\S]{0,120}showGenerationStatus/,
  "srpgeneration status command is missing");
expect(commands, /locked \(unlocks at generation 3\)/,
  "commands do not explain the generation-3 adaptation unlock");
expect(commands, /effective profile: full \(generation 5\), adaptation: active/,
  "commands do not report the disabled-generation full profile");

expect(primitive, /source\.is\(DamageTypes\.IN_WALL\)[\s\S]{0,100}source\.is\(DamageTypes\.FELL_OUT_OF_WORLD\)/,
  "suffocation and void adaptation exclusions are missing");
expect(primitive, /livingSource instanceof Player[\s\S]{0,220}BuiltInRegistries\.ITEM/,
  "player-held-item damage classification is missing");
expect(primitive, /BuiltInRegistries\.ENTITY_TYPE\.getKey\(livingSource\.getType\(\)\)/,
  "living-entity damage classification is missing");
expect(primitive, /source\.getMsgId\(\)/,
  "non-living DamageType classification is missing");
expect(primitive, /fireAdaptationSuppressionChance\(\)[\s\S]{0,120}fireAdaptationBlockTicks\s*=\s*FIRE_ADAPTATION_BLOCK_TICKS/,
  "fire adaptation suppression window is missing");
expect(primitive, /tag\.put\(ADAPTATIONS_TAG, adaptations\)/,
  "adaptation NBT persistence is missing");
expect(primitive, /tag\.getList\(ADAPTATIONS_TAG, Tag\.TAG_COMPOUND\)/,
  "adaptation NBT loading is missing");
expect(primitive, /EvolutionSystem\.generationProfile\(serverLevel\)\.adaptation\(\)/,
  "entity adaptation is not connected to the generation profile");
expect(renderer, /Color\.ofRGBA\(64, 255, 64, 255\)/,
  "green partial-adaptation feedback is missing");
expect(renderer, /Color\.ofRGBA\(255, 64, 255, 255\)/,
  "purple full-adaptation feedback is missing");

const tierChecks = [
  ["AdaptedVariantEntity.java", /damageAdaptationLearningChance\(\)[\s\S]{0,80}0\.80F/,
    /damageAdaptationPerHit\(\)[\s\S]{0,80}0\.10F/, /maxLearnableDamageSources\(\)[\s\S]{0,80}8/],
  ["DeterrentParasiteEntity.java", /ADAPTATION_LEARN_CHANCE\s*=\s*0\.85F/,
    /ADAPTATION_PER_HIT\s*=\s*0\.16F/, /MAX_LEARNABLE_DAMAGE_SOURCES\s*=\s*10/],
  ["PureParasiteEntity.java", /ADAPTATION_LEARN_CHANCE\s*=\s*0\.95F/,
    /ADAPTATION_PER_HIT\s*=\s*0\.125F/, /MAX_LEARNABLE_DAMAGE_SOURCES\s*=\s*12/],
  ["PreeminentParasiteEntity.java", /ADAPTATION_LEARN_CHANCE\s*=\s*1\.0F/,
    /ADAPTATION_PER_HIT\s*=\s*0\.20F/, /MAX_LEARNABLE_DAMAGE_SOURCES\s*=\s*20/],
  ["AncientParasiteEntity.java", /ADAPTATION_LEARN_CHANCE\s*=\s*0\.90F/,
    /ADAPTATION_PER_HIT\s*=\s*0\.10F/, /MAX_LEARNABLE_DAMAGE_SOURCES\s*=\s*5/],
  ["DerivedParasiteEntity.java", /damageAdaptationLearningChance\(\)[\s\S]{0,80}1\.0F/,
    /damageAdaptationPerHit\(\)[\s\S]{0,80}0\.20F/, /maxLearnableDamageSources\(\)[\s\S]{0,80}30/]
];
for (const [file, chance, reduction, sources] of tierChecks) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  expect(source, chance, `${file} adaptation learning chance differs from SRP`);
  expect(source, reduction, `${file} adaptation reduction differs from SRP`);
  expect(source, sources, `${file} adaptation source cap differs from SRP`);
}

for (const file of ["CrudeParasiteEntity.java", "HijackedParasiteEntity.java",
  "CarrierEntity.java", "GnatEntity.java", "LiceEntity.java"]) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  expect(source, /supportsDamageAdaptation\(\)[\s\S]{0,100}return false/,
    `${file} must not adapt according to the Wiki tier exclusions`);
}
for (const file of ["AssimilatedParasiteEntity.java", "AssimilatedVariantEntity.java",
  "FeralParasiteEntity.java"]) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  expect(source, /extends Monster implements GeoEntity, Parasite/,
    `${file} must remain outside the malleable adaptation base class`);
}

for (const file of ["ManglerEntity.java", "HostEntity.java", "HostIIEntity.java", "ThrallEntity.java"]) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  expect(source, /supportsDamageAdaptation\(\)[\s\S]{0,100}return true|maxDamageAdaptationHits\(\)/,
    `${file} Wiki adaptation exception is missing`);
}

const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
expect(nexus, /case 1 -> 10;[\s\S]{0,80}case 2 -> 8;[\s\S]{0,80}case 3 -> 6;[\s\S]{0,80}case 4 -> 4;/,
  "Nexus stage adaptation point caps differ from SRP");
expect(nexus, /case 1 -> 0\.07F;[\s\S]{0,80}case 2 -> 0\.125F;[\s\S]{0,80}case 3 -> 0\.17F;[\s\S]{0,80}case 4 -> 0\.25F;/,
  "Nexus stage adaptation reductions differ from SRP");
expect(nexus, /case 1 -> 5;[\s\S]{0,80}case 2 -> 10;[\s\S]{0,80}case 3 -> 15;[\s\S]{0,80}case 4 -> 23;/,
  "Nexus stage adaptation source caps differ from SRP");

if (failures.length) {
  console.error("Damage adaptation port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log("Damage adaptation port verification passed.");
