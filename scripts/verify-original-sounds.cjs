const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const projectRoot = path.resolve(__dirname, "..");
const sourceAssetsRoot = path.resolve(process.argv[2] ??
  path.join(projectRoot, ".firecrawl/srp-jar/assets/srparasites"));
const targetAssetsRoot = path.join(projectRoot, "src/main/resources/assets/csrp");
const sourceSoundsRoot = path.join(sourceAssetsRoot, "sounds");
const targetSoundsRoot = path.join(targetAssetsRoot, "sounds");
const reportPath = path.join(projectRoot, "docs/original-sounds-import.json");
const catalogPath = path.join(projectRoot,
  "src/main/java/alku/csrp/registry/SoundEventCatalog.java");
const profilesPath = path.join(projectRoot,
  "src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java");
const failures = [];

const readJson = (file) => JSON.parse(fs.readFileSync(file, "utf8"));
const hash = (file) => crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
const walkFiles = (root) => fs.readdirSync(root, { withFileTypes: true }).flatMap((entry) => {
  const file = path.join(root, entry.name);
  return entry.isDirectory() ? walkFiles(file) : [file];
});
const rewriteNamespace = (value) => {
  if (typeof value === "string") return value.replace(/^srparasites:/, "csrp:");
  if (Array.isArray(value)) return value.map(rewriteNamespace);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, child]) => [key, rewriteNamespace(child)]));
  }
  return value;
};
const stable = (value) => JSON.stringify(value);
const namespacePattern = /^[a-z0-9._-]+$/;
const pathPattern = /^[a-z0-9/._-]+$/;
const normalizeSoundReferences = (definitions) => Object.fromEntries(
  Object.entries(definitions).map(([event, definition]) => [event, {
    ...definition,
    sounds: definition.sounds?.map((entry) => {
      if (typeof entry === "string") return entry.toLowerCase();
      return entry && typeof entry === "object" && typeof entry.name === "string"
        ? { ...entry, name: entry.name.toLowerCase() }
        : entry;
    }),
  }]),
);

let sourceDefinitions;
let targetDefinitions;
let report;
try {
  sourceDefinitions = normalizeSoundReferences(
    rewriteNamespace(readJson(path.join(sourceAssetsRoot, "sounds.json"))),
  );
  targetDefinitions = readJson(path.join(targetAssetsRoot, "sounds.json"));
  report = readJson(reportPath);
} catch (error) {
  console.error(`Sound resource verification could not read required JSON: ${error.message}`);
  process.exit(1);
}

const sourceOggFiles = walkFiles(sourceSoundsRoot)
  .filter((file) => path.extname(file).toLowerCase() === ".ogg");
for (const target of walkFiles(targetSoundsRoot)
  .filter((file) => path.extname(file).toLowerCase() === ".ogg")) {
  const relative = path.relative(targetSoundsRoot, target).replaceAll(path.sep, "/");
  if (!pathPattern.test(relative)) failures.push(`invalid OGG resource path: ${relative}`);
}
for (const source of sourceOggFiles) {
  const relative = path.relative(sourceSoundsRoot, source);
  const target = path.join(targetSoundsRoot, relative);
  if (!fs.existsSync(target)) failures.push(`missing original OGG: ${relative}`);
  else if (hash(source) !== hash(target)) failures.push(`original OGG differs: ${relative}`);
}
for (const alias of report.compatibility_aliases ?? []) {
  const source = path.join(sourceSoundsRoot, `${alias.source}.ogg`);
  const target = path.join(targetSoundsRoot, `${alias.target}.ogg`);
  if (!fs.existsSync(target)) failures.push(`missing compatibility OGG: ${alias.target}`);
  else if (hash(source) !== hash(target)) failures.push(`compatibility OGG differs: ${alias.target}`);
}

for (const [event, definition] of Object.entries(sourceDefinitions)) {
  if (!Object.hasOwn(targetDefinitions, event)) failures.push(`missing original event: ${event}`);
  else if (stable(targetDefinitions[event]) !== stable(definition)) {
    failures.push(`original event definition differs: ${event}`);
  }
}
for (const event of report.preserved_modern_events ?? []) {
  if (!Object.hasOwn(targetDefinitions, event)) failures.push(`lost modern event: ${event}`);
}

const resolveSound = (name) => {
  const separator = name.indexOf(":");
  const namespace = separator < 0 ? "csrp" : name.slice(0, separator);
  const soundPath = separator < 0 ? name : name.slice(separator + 1);
  return { namespace, soundPath };
};
for (const [event, definition] of Object.entries(targetDefinitions)) {
  if (!pathPattern.test(event)) failures.push(`invalid sound event path: ${event}`);
  if (!Array.isArray(definition.sounds)) {
    failures.push(`${event}: sounds is not an array`);
    continue;
  }
  for (const entry of definition.sounds) {
    const name = typeof entry === "string" ? entry : entry?.name;
    const type = typeof entry === "object" && entry ? entry.type ?? "file" : "file";
    if (typeof name !== "string") {
      failures.push(`${event}: invalid sound entry`);
      continue;
    }
    const { namespace, soundPath } = resolveSound(name);
    if (!namespacePattern.test(namespace) || !pathPattern.test(soundPath)) {
      failures.push(`${event}: invalid sound resource location ${name}`);
      continue;
    }
    if (type === "event") {
      if (namespace === "csrp" && !Object.hasOwn(targetDefinitions, soundPath)) {
        failures.push(`${event}: missing referenced event ${name}`);
      }
      continue;
    }
    if (namespace !== "csrp") continue;
    const file = path.join(targetSoundsRoot, `${soundPath}.ogg`);
    if (!fs.existsSync(file)) failures.push(`${event}: missing referenced OGG ${name}`);
  }
}

const catalogSource = fs.readFileSync(catalogPath, "utf8");
for (const event of Object.keys(targetDefinitions)) {
  if (!catalogSource.includes(`            ${JSON.stringify(event)}`)) {
    failures.push(`sound event is absent from Java catalog: ${event}`);
  }
}
const catalogEntries = [...catalogSource.matchAll(/^\s{12}"([^"]+)"[,]?$/gm)].map((match) => match[1]);
if (catalogEntries.length !== Object.keys(targetDefinitions).length) {
  failures.push(`Java catalog has ${catalogEntries.length} entries, expected ${Object.keys(targetDefinitions).length}`);
}

const profileSource = fs.readFileSync(profilesPath, "utf8");
const profileEntityIds = new Set();
const profileByEntity = new Map();
const profileRegistrations = [...profileSource.matchAll(/register\("([^"]+)",\s*([\s\S]*?)\);/g)];
for (const [, prefix, rawEntityIds] of profileRegistrations) {
  for (const suffix of ["growl", "hurt", "death"]) {
    const event = `${prefix}.${suffix}`;
    if (!Object.hasOwn(targetDefinitions, event)) {
      failures.push(`entity sound profile references missing event: ${event}`);
    }
  }
  for (const [, entityId] of rawEntityIds.matchAll(/"([^"]+)"/g)) {
    if (profileEntityIds.has(entityId)) failures.push(`duplicate entity sound profile: ${entityId}`);
    profileEntityIds.add(entityId);
    profileByEntity.set(entityId, prefix);
  }
}

const expectedTierProfiles = {
  pri_longarms: "shyco", pri_summoner: "canra", pri_vermin: "iki", pri_viscera: "gim",
  pri_bolster: "zetmo", pri_devourer: "lum", pri_manducater: "hull", pri_reeker: "nogla",
  pri_yelloweye: "emana",
  ada_arachnida: "aranrac", ada_bolster: "azetmo", ada_devourer: "lum",
  ada_longarms: "ashyco", ada_manducater: "ahull", ada_reeker: "anogla",
  ada_summoner: "acanra", ada_tozoon: "awymo", ada_vermin: "aiki",
  ada_viscera: "agim", ada_yelloweye: "aemana",
};
for (const [entityId, expectedPrefix] of Object.entries(expectedTierProfiles)) {
  if (profileByEntity.get(entityId) !== expectedPrefix) {
    failures.push(`${entityId}: expected ${expectedPrefix} ambient/hurt/death profile`);
  }
}
const intentionallyNoVoiceProfile = ["pri_arachnida", "pri_burrower", "pri_tozoon", "ada_burrower"];
for (const entityId of intentionallyNoVoiceProfile) {
  if (profileByEntity.has(entityId)) {
    failures.push(`${entityId}: original SRP has no ambient/hurt/death profile`);
  }
}
if (Object.keys(expectedTierProfiles).length + intentionallyNoVoiceProfile.length !== 24) {
  failures.push("primitive/adapted sound audit must cover all 24 tier entities");
}

const burrowingSource = fs.readFileSync(path.join(projectRoot,
  "src/main/java/alku/csrp/entity/BurrowingVariantEntity.java"), "utf8");
const primitiveVariantSource = fs.readFileSync(path.join(projectRoot,
  "src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java"), "utf8");
const adaptedVariantSource = fs.readFileSync(path.join(projectRoot,
  "src/main/java/alku/csrp/entity/AdaptedVariantEntity.java"), "utf8");
if (!/playSound\(burrowSound\(\), 2\.0F, getVoicePitch\(\)\)/.test(burrowingSource)) {
  failures.push("burrowing variants do not play their original digging event");
}
for (const [source, constants] of [
  [primitiveVariantSource, ["PRIMITIVE_BURROWER_DIG", "PRIMITIVE_TOZOON_DIG"]],
  [adaptedVariantSource, ["ADAPTED_BURROWER_DIG", "ADAPTED_TOZOON_DIG"]],
]) {
  for (const constant of constants) {
    if (!source.includes(`ModSounds.${constant}.get()`)) {
      failures.push(`missing digging sound binding: ${constant}`);
    }
  }
}
for (const sourceFile of [
  "PrimitiveParasiteEntity.java",
  "AssimilatedParasiteEntity.java",
  "AssimilatedVariantEntity.java",
  "AssimilatedEndermanEntity.java",
  "AssimilatedHeadEntity.java",
  "FeralParasiteEntity.java",
  "SimHumanEntity.java",
]) {
  const source = fs.readFileSync(path.join(projectRoot, "src/main/java/alku/csrp/entity", sourceFile), "utf8");
  for (const method of ["ambient", "hurt", "death"]) {
    if (!source.includes(`ParasiteSoundProfiles.${method}(this)`)) {
      failures.push(`${sourceFile}: missing ${method} sound-profile binding`);
    }
  }
}
const simHumanSource = fs.readFileSync(path.join(projectRoot,
  "src/main/java/alku/csrp/entity/SimHumanEntity.java"), "utf8");
if (!simHumanSource.includes('ModSounds.get("mob.silence")')) {
  failures.push("SimHumanEntity: non-normal ambient state does not use the original silence event");
}

if (failures.length) {
  console.error(`Original sound verification failed (${failures.length}):`);
  failures.slice(0, 100).forEach((failure) => console.error(`- ${failure}`));
  if (failures.length > 100) console.error(`- ... ${failures.length - 100} additional failures`);
  process.exit(1);
}

console.log(`Verified ${sourceOggFiles.length} original OGG files by SHA-256.`);
console.log(`Verified ${Object.keys(sourceDefinitions).length} original events and ${Object.keys(targetDefinitions).length} total registered events.`);
console.log(`Verified ${profileEntityIds.size} entity sound profiles and shared entity bindings.`);
console.log("All csrp sound file and event references resolve.");
