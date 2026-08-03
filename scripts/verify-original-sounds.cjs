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

let sourceDefinitions;
let targetDefinitions;
let report;
try {
  sourceDefinitions = rewriteNamespace(readJson(path.join(sourceAssetsRoot, "sounds.json")));
  targetDefinitions = readJson(path.join(targetAssetsRoot, "sounds.json"));
  report = readJson(reportPath);
} catch (error) {
  console.error(`Sound resource verification could not read required JSON: ${error.message}`);
  process.exit(1);
}

const sourceOggFiles = walkFiles(sourceSoundsRoot)
  .filter((file) => path.extname(file).toLowerCase() === ".ogg");
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
  }
}
for (const sourceFile of [
  "PrimitiveParasiteEntity.java",
  "AssimilatedParasiteEntity.java",
  "AssimilatedVariantEntity.java",
  "AssimilatedEndermanEntity.java",
  "AssimilatedHeadEntity.java",
  "FeralParasiteEntity.java",
]) {
  const source = fs.readFileSync(path.join(projectRoot, "src/main/java/alku/csrp/entity", sourceFile), "utf8");
  for (const method of ["ambient", "hurt", "death"]) {
    if (!source.includes(`ParasiteSoundProfiles.${method}(this)`)) {
      failures.push(`${sourceFile}: missing ${method} sound-profile binding`);
    }
  }
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
