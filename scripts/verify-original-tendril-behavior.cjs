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
const legacy = exists("src/main/java/alku/csrp/entity/LegacyAuxiliaryEntity.java")
  ? read("src/main/java/alku/csrp/entity/LegacyAuxiliaryEntity.java") : "";
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const model = read("src/main/java/alku/csrp/client/renderer/TabulaTendrilRenderer.java");
const tabulaRegistry = read("src/main/java/alku/csrp/client/model/tabula/TabulaModelRegistry.java");
const parentModel = read("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const pure = read("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const dragon = read("src/main/java/alku/csrp/entity/AssimilatedDragonEntity.java");

// The tendril is a Citadel/Tabula-rendered entity now, so it no longer implements GeoEntity:
// the Citadel renderer owns the model and texture lookup instead of a GeckoLib model.
expect(entity, /public final class TendrilEntity extends Monster implements Parasite/,
  "Tendril is not a dedicated living parasite entity");
expect(entity, /registerGoals\(\)[\s\S]*?original removes its wander and parasite-follow goals/,
  "Tendril does not preserve the original no-wander/no-follow behavior");
expect(entity, /builder\.define\(SKIN, SHYCO\)/,
  "Tendril skin is not synchronized");
expect(entity, /parasitetype/,
  "Tendril skin does not use the original parasite NBT key");
expect(entity, /IS_FIRE[\s\S]*?amount \* 4\.0F/,
  "Tendril does not preserve parasite fire weakness");
if (legacy && !/enum Kind\s*\{\s*REMAIN,\s*GORE\s*\}/.test(legacy)) {
  failures.push("Legacy auxiliary placeholder still owns Tendril");
}
expect(registry, /EntityType<TendrilEntity>> TENDRIL[\s\S]*?register\("tendril"[\s\S]*?sized\(1\.0F, 1\.0F\)[\s\S]*?clientTrackingRange\(4\)[\s\S]*?updateInterval\(3\)/,
  "Tendril registration does not preserve its original id, size, or tracking");
expect(attributes, /ModEntities\.TENDRIL\.get\(\), TendrilEntity\.createAttributes\(\)\.build\(\)/,
  "Tendril attributes are not registered");
expect(client, /ModEntities\.TENDRIL\.get\(\),\s*TabulaTendrilRenderer::new/,
  "Tendril does not use the Citadel Tabula tendril renderer");
// The removed TendrilModel selected geo/tendril_<skin>.geo.json per skin. In the Citadel
// port the same eight skins are the renderer's model and texture lookup tables, so the
// skin-mapping contract is asserted on those tables instead.
expect(model, /private static final String\[\] MODEL_IDS = \{[\s\S]*?"tendril_shyco",[\s\S]*?"tendril_nogla",[\s\S]*?"tendril_canra",[\s\S]*?"tendril_bano",[\s\S]*?"marauder_tendril",[\s\S]*?"tendril_anged",[\s\S]*?"tendril_dragonelw",[\s\S]*?"tendril_dragonerw"[\s\S]*?\};/,
  "Tendril renderer does not map all eight original skins");
expect(model, /private static final ResourceLocation\[\] TEXTURES = \{[\s\S]*?texture\("tendrilshyco\.png"\)[\s\S]*?texture\("tendrilnogla\.png"\)[\s\S]*?texture\("tendrilcanra\.png"\)[\s\S]*?texture\("tendrilbano\.png"\)[\s\S]*?texture\("tendrilesor\.png"\)[\s\S]*?texture\("tendrilanged\.png"\)[\s\S]*?texture\("tendrildragonelw\.png"\)[\s\S]*?texture\("tendrildragonerw\.png"\)/,
  "Tendril renderer does not resolve all eight original skin textures");
expect(model, /model = models\[skin\(entity\)\];/,
  "Tendril renderer does not swap to the model selected by the synced skin");
expect(model, /getTextureLocation\(TendrilEntity entity\)[\s\S]*?return TEXTURES\[skin\(entity\)\];/,
  "Tendril renderer does not resolve the texture from the synced skin");
expect(model, /super\(context, model\(MODEL_IDS\[TendrilEntity\.SHYCO\]\), 0\.3F\)/,
  "Tendril renderer default skin model or shadow radius is wrong");
expect(model, /TabulaModelRegistry\.create\(id\)/,
  "Tendril renderer does not resolve the Citadel Tabula models through the shared registry");
expect(tabulaRegistry, /"alku\.csrp\.client\.model\.tabula\.generated\.ModelTabula_"/,
  "the Citadel model registry does not resolve generated Tabula tendril models");
for (const id of ["tendril_shyco", "tendril_nogla", "tendril_canra", "tendril_bano",
  "tendril_anged", "tendril_dragonelw", "tendril_dragonerw", "marauder_tendril"]) {
  if (!exists(`src/main/java/alku/csrp/client/model/tabula/generated/ModelTabula_${id}.java`)) {
    failures.push(`Missing generated Citadel tendril model: ${id}`);
  }
}

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
