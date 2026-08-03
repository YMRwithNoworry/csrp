const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const projectRoot = path.resolve(__dirname, "..");
const sourceRoot = path.resolve(process.argv[2] ??
  "D:/code/MC模组/srp生物模型和动画提取/提取结果");
const assetsRoot = path.join(projectRoot, "src/main/resources/assets/csrp");
const textureRoot = path.join(assetsRoot, "textures/entity");
const reportPath = path.join(projectRoot, "docs/extracted-entity-resources-import.json");

const hash = (file) => crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
const readJson = (file) => JSON.parse(fs.readFileSync(file, "utf8"));
const copy = (source, target) => {
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(source, target);
};

const manifestPath = path.join(sourceRoot, "manifest.json");
if (!fs.existsSync(manifestPath)) throw new Error(`Missing extractor manifest: ${manifestPath}`);
const manifest = readJson(manifestPath);
const exported = manifest.entities.filter((entity) => entity.status === "approximate");
const skipped = manifest.entities.filter((entity) => entity.status !== "approximate");
if (exported.length !== manifest.exported_mobs) {
  throw new Error(`Manifest exported_mobs=${manifest.exported_mobs}, but found ${exported.length} exportable entries`);
}

// Preserve flat compatibility names by remembering which extracted texture each
// existing file came from before replacing any resources.
const extractedTextures = [];
for (const entity of exported) {
  for (const relative of entity.textures ?? []) {
    const source = path.join(sourceRoot, entity.registry_id, relative);
    if (!fs.existsSync(source)) throw new Error(`Missing extracted texture: ${source}`);
    extractedTextures.push({ entity: entity.registry_id, relative, source, sha256: hash(source) });
  }
}
const texturesByHash = new Map();
for (const texture of extractedTextures) {
  if (!texturesByHash.has(texture.sha256)) texturesByHash.set(texture.sha256, texture);
}

const compatibilityAliases = [];
if (fs.existsSync(textureRoot)) {
  for (const entry of fs.readdirSync(textureRoot, { withFileTypes: true })) {
    if (!entry.isFile() || path.extname(entry.name).toLowerCase() !== ".png") continue;
    const target = path.join(textureRoot, entry.name);
    const source = texturesByHash.get(hash(target));
    if (source) compatibilityAliases.push({ name: entry.name, source });
  }
}

const basenameHashes = new Map();
for (const texture of extractedTextures) {
  const basename = path.basename(texture.relative);
  const previous = basenameHashes.get(basename);
  if (previous && previous.sha256 !== texture.sha256) {
    throw new Error(`Conflicting extracted texture basename ${basename}: ${previous.relative} vs ${texture.relative}`);
  }
  basenameHashes.set(basename, texture);
}

const primaryTextures = [];
for (const entity of exported) {
  const id = entity.registry_id;
  const entityRoot = path.join(sourceRoot, id);
  const geoSource = path.join(entityRoot, `${id}.geo.json`);
  const animationSource = path.join(entityRoot, `${id}.animation.json`);
  const textures = entity.textures ?? [];
  if (!fs.existsSync(geoSource) || !fs.existsSync(animationSource) || textures.length === 0) {
    throw new Error(`${id}: incomplete extracted model, animation, or texture set`);
  }
  readJson(geoSource);
  readJson(animationSource);
  copy(geoSource, path.join(assetsRoot, "geo", `${id}.geo.json`));
  copy(animationSource, path.join(assetsRoot, "animations", `${id}.animation.json`));

  for (const relative of textures) {
    const source = path.join(entityRoot, relative);
    copy(source, path.join(assetsRoot, relative));
    copy(source, path.join(textureRoot, path.basename(relative)));
  }

  const primary = path.join(entityRoot, textures[0]);
  copy(primary, path.join(textureRoot, `${id}.png`));
  primaryTextures.push({ entity: id, source: textures[0] });
}

for (const alias of compatibilityAliases) {
  copy(alias.source.source, path.join(textureRoot, alias.name));
}

const explicitAliases = [
  ["sim_sheep_grey.png", "sim_sheep", "textures/entity/monster/sheep_grey.png"],
  ["sim_sheep_black.png", "sim_sheep", "textures/entity/monster/sheep_black.png"],
  ["sim_wolf_tamed.png", "sim_wolf", "textures/entity/monster/wolftamed.png"]
];
for (const [name, entity, relative] of explicitAliases) {
  const source = path.join(sourceRoot, entity, relative);
  if (!fs.existsSync(source)) throw new Error(`Missing compatibility texture source: ${source}`);
  copy(source, path.join(textureRoot, name));
}

const historicalId = "sim_dragonehead";
const compatibilityId = "sim_dragonhead";
copy(path.join(assetsRoot, "geo", `${historicalId}.geo.json`),
  path.join(assetsRoot, "geo", `${compatibilityId}.geo.json`));
copy(path.join(assetsRoot, "animations", `${historicalId}.animation.json`),
  path.join(assetsRoot, "animations", `${compatibilityId}.animation.json`));
copy(path.join(textureRoot, `${historicalId}.png`), path.join(textureRoot, `${compatibilityId}.png`));

const report = {
  source_jar: manifest.source_jar,
  source_sha256: manifest.source_sha256,
  animation_mode: manifest.animation_mode,
  exported_entities: exported.length,
  copied_extracted_textures: extractedTextures.length,
  skipped_entities: skipped.map((entity) => ({
    registry_id: entity.registry_id,
    diagnostics: entity.diagnostics
  })),
  primary_textures: primaryTextures,
  preserved_flat_aliases: compatibilityAliases.map((alias) => ({
    target: alias.name,
    entity: alias.source.entity,
    source: alias.source.relative
  })),
  explicit_aliases: explicitAliases.map(([target, entity, source]) => ({ target, entity, source })),
  resource_id_aliases: [{ target: compatibilityId, source: historicalId }]
};
fs.mkdirSync(path.dirname(reportPath), { recursive: true });
fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);

console.log(`Imported ${exported.length} entity resource sets and ${extractedTextures.length} extracted textures.`);
console.log(`Preserved ${compatibilityAliases.length} existing flat texture aliases.`);
console.log(`Report: ${path.relative(projectRoot, reportPath)}`);
