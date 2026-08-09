const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const exists = (file) => fs.existsSync(path.join(root, file));
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const entity = read("src/main/java/alku/csrp/entity/TendrilEntity.java");
const legacy = read("src/main/java/alku/csrp/entity/LegacyAuxiliaryEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const model = read("src/main/java/alku/csrp/client/model/TendrilModel.java");
const parentModel = read("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const pure = read("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const dragon = read("src/main/java/alku/csrp/entity/AssimilatedDragonEntity.java");

expect(entity, /class TendrilEntity extends Monster implements GeoEntity, Parasite/,
  "Tendril is not a dedicated living parasite entity");
expect(entity, /registerGoals\(\)[\s\S]*?original removes its wander and parasite-follow goals/,
  "Tendril does not preserve the original no-wander/no-follow behavior");
expect(entity, /builder\.define\(SKIN, SHYCO\)/,
  "Tendril skin is not synchronized");
expect(entity, /parasitetype/,
  "Tendril skin does not use the original parasite NBT key");
expect(entity, /IS_FIRE[\s\S]*?amount \* 4\.0F/,
  "Tendril does not preserve parasite fire weakness");
expect(legacy, /enum Kind\s*\{\s*REMAIN,\s*GORE\s*\}/,
  "Legacy auxiliary placeholder still owns Tendril");
expect(registry, /EntityType<TendrilEntity>> TENDRIL[\s\S]*?register\("tendril"[\s\S]*?sized\(1\.0F, 1\.0F\)[\s\S]*?clientTrackingRange\(4\)[\s\S]*?updateInterval\(3\)/,
  "Tendril registration does not preserve its original id, size, or tracking");
expect(attributes, /ModEntities\.TENDRIL\.get\(\), TendrilEntity\.createAttributes\(\)\.build\(\)/,
  "Tendril attributes are not registered");
expect(client, /ModEntities\.TENDRIL\.get\(\), TendrilRenderer::new/,
  "Tendril still uses the placeholder renderer");
expect(model, /tendril_shyco[\s\S]*?tendril_nogla[\s\S]*?tendril_canra[\s\S]*?tendril_bano[\s\S]*?marauder_tendril[\s\S]*?tendril_anged[\s\S]*?tendril_dragonelw[\s\S]*?tendril_dragonerw/,
  "Tendril model does not map all eight original skins");

for (const id of ["shyco", "nogla", "canra", "bano", "anged", "dragonelw", "dragonerw"]) {
  const geometryFile = `src/main/resources/assets/csrp/geo/tendril_${id}.geo.json`;
  if (!exists(geometryFile)) {
    failures.push(`Missing Tendril geometry: ${id}`);
    continue;
  }
  const geometry = JSON.parse(read(geometryFile))["minecraft:geometry"][0];
  if (geometry.description.identifier !== `geometry.tendril_${id}` || geometry.bones.length < 9) {
    failures.push(`Tendril geometry is incomplete: ${id}`);
  }
}

for (const texture of ["shyco", "nogla", "canra", "bano", "esor", "anged", "dragonelw", "dragonerw"]) {
  if (!exists(`src/main/resources/assets/csrp/textures/entity/monster/tendril${texture}.png`)) {
    failures.push(`Missing Tendril texture: ${texture}`);
  }
}

for (const animation of ["static", "shyco", "nogla", "bano", "anged"]) {
  const animationFile = `src/main/resources/assets/csrp/animations/tendril_${animation}.animation.json`;
  if (!exists(animationFile) || !JSON.parse(read(animationFile)).animations.idle) {
    failures.push(`Missing Tendril idle animation: ${animation}`);
  }
}

expect(adapted, /case LONGARMS -> TendrilEntity\.SHYCO/,
  "Adapted Longarms does not shed the Shyco tendril");
expect(adapted, /case REEKER -> TendrilEntity\.NOGLA/,
  "Adapted Reeker does not shed the Nogla tendril");
expect(adapted, /case MANDUCATER, SUMMONER -> TendrilEntity\.CANRA/,
  "Adapted Manducater/Summoner do not shed the Canra tendril");
expect(adapted, /case BOLSTER -> TendrilEntity\.BANO/,
  "Adapted Bolster does not shed the Bano tendril");
expect(pure, /setSkin\(TendrilEntity\.ANGED\)/,
  "Vigilante does not shed the Anged tendril");
expect(dragon, /spawnDetachedWing\(leftWingPart, TendrilEntity\.DRAGON_LEFT_WING\)/,
  "Assimilated Dragon does not shed its left wing");
expect(dragon, /spawnDetachedWing\(rightWingPart, TendrilEntity\.DRAGON_RIGHT_WING\)/,
  "Assimilated Dragon does not shed its right wing");
expect(parentModel, /jointLW1[\s\S]*?hasLeftWing[\s\S]*?jointRW1[\s\S]*?hasRightWing/,
  "Detached dragon wings are not hidden on the parent model");
expect(registry, /MARAUDER_TENDRIL/,
  "Marauder's distinct tendril entity was incorrectly merged");
expect(registry, /ANC_DREADNAUT_TEN/,
  "Dreadnaut's distinct tentacle entity was incorrectly merged");

if (failures.length) {
  console.error("Original Tendril behavior verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Original Tendril entity, eight skins, shedding callers, and distinct tentacle types restored.");
