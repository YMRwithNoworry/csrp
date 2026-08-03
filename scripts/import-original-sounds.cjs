const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const projectRoot = path.resolve(__dirname, "..");
const sourceAssetsRoot = path.resolve(process.argv[2] ??
  path.join(projectRoot, ".firecrawl/srp-jar/assets/srparasites"));
const targetAssetsRoot = path.join(projectRoot, "src/main/resources/assets/csrp");
const sourceSoundsRoot = path.join(sourceAssetsRoot, "sounds");
const targetSoundsRoot = path.join(targetAssetsRoot, "sounds");
const targetSoundsJson = path.join(targetAssetsRoot, "sounds.json");
const catalogPath = path.join(projectRoot,
  "src/main/java/alku/csrp/registry/SoundEventCatalog.java");
const reportPath = path.join(projectRoot, "docs/original-sounds-import.json");

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
const parseLang = (file) => {
  if (!fs.existsSync(file)) return {};
  const entries = {};
  for (const rawLine of fs.readFileSync(file, "utf8").replace(/^\uFEFF/, "").split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const separator = line.indexOf("=");
    if (separator < 1) continue;
    entries[line.slice(0, separator).trim()] = line.slice(separator + 1).trim();
  }
  return entries;
};

const compatibilityAliasSources = {
  "adapted/tozoon/special": "mob/adapted/tozoon/special",
  "prim_burrower_death": "prim_burrower_digging_3",
  "prim_burrower_hurt_1": "prim_burrower_digging_1",
  "prim_burrower_hurt_2": "prim_burrower_digging_2",
  "prim_burrower_hurt_3": "prim_burrower_digging_3",
  "prim_burrower_living_1": "prim_burrower_digging_1",
  "prim_burrower_living_2": "prim_burrower_digging_2",
  "prim_burrower_living_3": "prim_burrower_digging_3",
  "prim_tozoon_death": "mob/adapted/tozoon/death",
  "primitive/tozoon/hurt1": "mob/adapted/tozoon/hurt1",
  "primitive/tozoon/hurt2": "mob/adapted/tozoon/hurt2",
  "primitive/tozoon/hurt3": "mob/adapted/tozoon/hurt3",
  "primitive/tozoon/idle1": "mob/adapted/tozoon/idle1",
  "primitive/tozoon/idle2": "mob/adapted/tozoon/idle2",
  "primitive/tozoon/idle3": "mob/adapted/tozoon/idle3",
  "mob/crude/flesh/infected_melting": "mob/crude/moving_flesh/infected_melting",
  "ada_burrower_death": "prim_burrower_digging_3",
  "ada_burrower_living_1": "prim_burrower_digging_1",
  "ada_burrower_living_2": "prim_burrower_digging_2",
  "ada_burrower_living_3": "prim_burrower_digging_3",
  "ada_burrower_living_4": "prim_burrower_digging_1",
  "ada_burrower_hurt_1": "prim_burrower_digging_1",
  "ada_burrower_hurt_2": "prim_burrower_digging_2",
  "ada_burrower_hurt_3": "prim_burrower_digging_3",
  "worm_carrier_death": "carrierdeath",
  "worm_carrier_hurt_1": "carrierhurt1",
  "worm_carrier_hurt_2": "carrierhurt2",
  "worm_carrier_hurt_3": "carrierhurt2",
  "worm_carrier_living_1": "carrierliving1",
  "worm_carrier_living_2": "carrierliving2",
  "worm_carrier_living_3": "carrierliving2",
  "mob/derived/draconite/death": "mob/derived/draconite/idle5",
  "ancient": "misc/ancient",
  "misc/biome_heart": "biome_heart",
  "music/tying_veins": "music/tyingveins",
  "music/well_meet_again": "music/parasite",
  "misc/vector_removed": "misc/vector_outbreak"
};

const sourceSoundsJson = path.join(sourceAssetsRoot, "sounds.json");
for (const required of [sourceSoundsRoot, sourceSoundsJson, targetSoundsJson]) {
  if (!fs.existsSync(required)) throw new Error(`Missing required sound resource: ${required}`);
}

const sourceDefinitions = rewriteNamespace(readJson(sourceSoundsJson));
const currentDefinitions = readJson(targetSoundsJson);
const mergedDefinitions = { ...sourceDefinitions };
const preservedEvents = [];
for (const [event, definition] of Object.entries(currentDefinitions)) {
  if (Object.hasOwn(sourceDefinitions, event)) continue;
  mergedDefinitions[event] = definition;
  preservedEvents.push(event);
}

const copiedFiles = [];
for (const source of walkFiles(sourceSoundsRoot).filter((file) => path.extname(file).toLowerCase() === ".ogg")) {
  const relative = path.relative(sourceSoundsRoot, source);
  const target = path.join(targetSoundsRoot, relative);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(source, target);
  copiedFiles.push({ relative: relative.replaceAll("\\", "/"), sha256: hash(source) });
}

const compatibilityAliases = [];
for (const [targetName, sourceName] of Object.entries(compatibilityAliasSources)) {
  const source = path.join(sourceSoundsRoot, `${sourceName}.ogg`);
  const target = path.join(targetSoundsRoot, `${targetName}.ogg`);
  if (!fs.existsSync(source)) throw new Error(`Missing compatibility sound source: ${source}`);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(source, target);
  compatibilityAliases.push({ target: targetName, source: sourceName, sha256: hash(source) });
}

fs.writeFileSync(targetSoundsJson, `${JSON.stringify(mergedDefinitions, null, 2)}\n`);

const importedSubtitleCounts = {};
for (const locale of ["en_us", "zh_cn"]) {
  const sourceLang = parseLang(path.join(sourceAssetsRoot, "lang", `${locale}.lang`));
  const targetLangPath = path.join(targetAssetsRoot, "lang", `${locale}.json`);
  const targetLang = readJson(targetLangPath);
  let imported = 0;
  for (const [key, value] of Object.entries(sourceLang)) {
    if (!key.startsWith("subtitles.") || Object.hasOwn(targetLang, key)) continue;
    targetLang[key] = value;
    imported++;
  }
  fs.writeFileSync(targetLangPath, `${JSON.stringify(targetLang, null, 2)}\n`);
  importedSubtitleCounts[locale] = imported;
}

const eventNames = Object.keys(mergedDefinitions).sort();
const catalog = `package alku.csrp.registry;

import java.util.List;

/** Generated by scripts/import-original-sounds.cjs. */
final class SoundEventCatalog {
    static final List<String> EVENTS = List.of(
${eventNames.map((event, index) => `            ${JSON.stringify(event)}${index + 1 === eventNames.length ? "" : ","}`).join("\n")}
    );

    private SoundEventCatalog() {
    }
}
`;
fs.mkdirSync(path.dirname(catalogPath), { recursive: true });
fs.writeFileSync(catalogPath, catalog);

const report = {
  source_assets: sourceAssetsRoot.replaceAll("\\", "/"),
  source_events: Object.keys(sourceDefinitions).length,
  preserved_modern_events: preservedEvents.sort(),
  merged_events: eventNames.length,
  copied_ogg_files: copiedFiles,
  compatibility_aliases: compatibilityAliases,
  imported_subtitle_keys: importedSubtitleCounts
};
fs.mkdirSync(path.dirname(reportPath), { recursive: true });
fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);

console.log(`Imported ${copiedFiles.length} original OGG files.`);
console.log(`Merged ${Object.keys(sourceDefinitions).length} original and ${preservedEvents.length} modern sound events.`);
console.log(`Generated ${path.relative(projectRoot, catalogPath)} with ${eventNames.length} registered event names.`);
