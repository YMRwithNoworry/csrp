const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const { isDeepStrictEqual } = require("util");

const projectRoot = path.resolve(__dirname, "..");
const sourceRoot = path.resolve(process.argv[2] ??
  "D:/code/MC模组/srp生物模型和动画提取/提取结果");
const assetsRoot = path.join(projectRoot, "src/main/resources/assets/csrp");
const manifest = JSON.parse(fs.readFileSync(path.join(sourceRoot, "manifest.json"), "utf8"));
const failures = [];
const hash = (file) => crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
const sameFile = (source, target, label) => {
  if (!fs.existsSync(target)) return failures.push(`${label}: target is missing`);
  if (hash(source) !== hash(target)) failures.push(`${label}: SHA-256 differs from extractor output`);
};
const parse = (file, label) => {
  try {
    return JSON.parse(fs.readFileSync(file, "utf8"));
  } catch (error) {
    failures.push(`${label}: invalid JSON (${error.message})`);
    return null;
  }
};
const normalizeJson = (value, key = "") => {
  if (Array.isArray(value)) {
    const normalized = value.map((entry) => normalizeJson(entry));
    return key === "bones" && normalized.every((entry) => entry && typeof entry.name === "string")
      ? normalized.sort((left, right) => left.name.localeCompare(right.name))
      : normalized;
  }
  if (!value || typeof value !== "object") return value;
  return Object.fromEntries(Object.entries(value)
    .map(([childKey, child]) => [childKey, normalizeJson(child, childKey)]));
};
const sameJson = (source, target, label) => {
  if (!fs.existsSync(target)) return failures.push(`${label}: target is missing`);
  const sourceJson = parse(source, `${label} extractor source`);
  const targetJson = parse(target, label);
  if (sourceJson && targetJson
      && !isDeepStrictEqual(normalizeJson(sourceJson), normalizeJson(targetJson))) {
    failures.push(`${label}: structure differs from extractor output`);
  }
};
// The extractor exports the historical GeckoLib identifiers geometry.srparasites.<id>. This project
// registers everything under the csrp namespace, so the shipped resources read geometry.csrp.<id>.
// Only that single identifier string is allowed to differ; every bone, cube and pivot must still be
// identical to the extractor export, which is why the mismatch is asserted explicitly here and the
// rest of the document is compared unchanged.
const identifierNamespace = (identifier) => {
  const separator = identifier.indexOf(".");
  return separator < 0 ? identifier : identifier.slice(0, separator);
};
const withProjectGeometryNamespace = (entityId, geometry, label) => {
  const document = structuredClone(geometry);
  const entries = document?.["minecraft:geometry"];
  if (!Array.isArray(entries) || !entries.length) {
    failures.push(`${label}: extractor geometry has no minecraft:geometry entries`);
    return document;
  }
  for (const entry of entries) {
    const identifier = entry?.description?.identifier;
    if (typeof identifier !== "string"
        || identifierNamespace(identifier) !== "geometry"
        || !identifier.endsWith(`.${entityId}`)) {
      failures.push(`${label}: extractor geometry identifier is not geometry.<namespace>.${entityId}`);
      continue;
    }
    entry.description.identifier = `geometry.csrp.${entityId}`;
  }
  return document;
};
const sameGeoJson = (source, target, entityId, label) => {
  if (!fs.existsSync(target)) return failures.push(`${label}: target is missing`);
  const sourceJson = parse(source, `${label} extractor source`);
  const targetJson = parse(target, label);
  if (!sourceJson || !targetJson) return;
  const expected = withProjectGeometryNamespace(entityId, sourceJson, label);
  if (!isDeepStrictEqual(normalizeJson(expected), normalizeJson(targetJson))) {
    failures.push(`${label}: structure differs from extractor output`);
  }
};

const exported = manifest.entities.filter((entity) => entity.status === "approximate");
const skipped = manifest.entities.filter((entity) => entity.status !== "approximate");
if (exported.length !== 124) failures.push(`expected 124 exported entities, found ${exported.length}`);
if (skipped.map((entity) => entity.registry_id).sort().join(",") !== "abo_head,carrier_worm,seeker") {
  failures.push(`unexpected skipped entities: ${skipped.map((entity) => entity.registry_id).join(", ")}`);
}

let textureCount = 0;
for (const entity of exported) {
  const id = entity.registry_id;
  const sourceEntityRoot = path.join(sourceRoot, id);
  const sourceGeo = path.join(sourceEntityRoot, `${id}.geo.json`);
  const sourceAnimation = path.join(sourceEntityRoot, `${id}.animation.json`);
  const targetGeo = path.join(assetsRoot, "geo", `${id}.geo.json`);
  const targetAnimation = path.join(assetsRoot, "animations", `${id}.animation.json`);
  sameGeoJson(sourceGeo, targetGeo, id, `${id} geometry`);
  sameJson(sourceAnimation, targetAnimation, `${id} animation`);

  const geometry = parse(targetGeo, `${id} geometry`);
  const animations = parse(targetAnimation, `${id} animation`);
  const geometryEntries = geometry?.["minecraft:geometry"];
  const bones = new Set(geometryEntries?.flatMap((entry) => entry.bones ?? []).map((bone) => bone.name) ?? []);
  if (!geometryEntries?.length) failures.push(`${id}: geometry has no minecraft:geometry entries`);
  const expectedAnimations = [...(entity.animations ?? [])].sort();
  const actualAnimations = Object.keys(animations?.animations ?? {}).sort();
  if (expectedAnimations.length !== actualAnimations.length
      || expectedAnimations.some((name, index) => name !== actualAnimations[index])) {
    failures.push(`${id}: animation keys differ from extractor manifest`);
  }
  for (const [animationName, animation] of Object.entries(animations?.animations ?? {})) {
    for (const bone of Object.keys(animation.bones ?? {})) {
      if (!bones.has(bone)) failures.push(`${id}/${animationName}: missing geometry bone ${bone}`);
    }
  }

  for (const relative of entity.textures ?? []) {
    textureCount++;
    const source = path.join(sourceEntityRoot, relative);
    sameFile(source, path.join(assetsRoot, relative), `${id} texture ${relative}`);
    sameFile(source, path.join(assetsRoot, "textures/entity", path.basename(relative)),
      `${id} flat texture ${path.basename(relative)}`);
  }
  const primary = entity.textures?.[0];
  if (!primary) failures.push(`${id}: no primary texture in extractor manifest`);
  else sameFile(path.join(sourceEntityRoot, primary),
    path.join(assetsRoot, "textures/entity", `${id}.png`), `${id} primary texture`);
}

for (const extension of ["geo.json", "animation.json"]) {
  sameJson(
    path.join(assetsRoot, extension === "geo.json" ? "geo" : "animations", `sim_dragonehead.${extension}`),
    path.join(assetsRoot, extension === "geo.json" ? "geo" : "animations", `sim_dragonhead.${extension}`),
    `sim_dragonhead compatibility ${extension}`
  );
}
sameFile(
  path.join(assetsRoot, "textures/entity/sim_dragonehead.png"),
  path.join(assetsRoot, "textures/entity/sim_dragonhead.png"),
  "sim_dragonhead compatibility texture"
);

for (const resource of [
  "geo/abo_head.geo.json",
  "animations/abo_head.animation.json",
  "textures/entity/abo_head.png",
  "geo/marauder_tendril.geo.json",
  "animations/marauder_tendril.animation.json",
  "textures/entity/marauder_tendril.png"
]) {
  if (!fs.existsSync(path.join(assetsRoot, resource))) failures.push(`preserved resource is missing: ${resource}`);
}

if (failures.length) {
  console.error(`Extracted entity resource verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log(`Verified ${exported.length} extracted entity resource sets and ${textureCount} textures.`);
console.log("All geometry/animation structures, extracted function keys, bone references, and compatibility resources passed.");
