const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};
const javaFiles = (directory) => fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
  const absolute = path.join(directory, entry.name);
  return entry.isDirectory() ? javaFiles(absolute) : entry.name.endsWith(".java") ? [absolute] : [];
});
const methodBodies = (source, signature) => {
  const bodies = [];
  for (const match of source.matchAll(signature)) {
    const openingBrace = source.indexOf("{", match.index);
    if (openingBrace < 0) continue;
    let depth = 0;
    for (let index = openingBrace; index < source.length; index++) {
      if (source[index] === "{") depth++;
      if (source[index] === "}" && --depth === 0) {
        bodies.push(source.slice(match.index, index + 1));
        break;
      }
    }
  }
  return bodies;
};

const cystEvent = path.join(root, "src/main/java/alku/csrp/event/ParasiticCystEvents.java");
if (fs.existsSync(cystEvent)) {
  failures.push("parasite deaths still register the Gluttonous Cyst placement event");
}

const remainsEvent = path.join(root, "src/main/java/alku/csrp/event/ParasiteDeathRemainsEvents.java");
if (fs.existsSync(remainsEvent)) {
  failures.push("parasite deaths still register the remains-spawning event");
}

const deathFx = read("src/main/java/alku/csrp/event/ParasiteDeathFxEvents.java");
expect(deathFx, /(?:PacketDistributor\.sendToPlayer\(player, payload\)|CsrpNetwork\.sendToPlayer\(player, payload\))/,
  "parasite death particles were removed unexpectedly");
for (const forbidden of ["ParasiteRemainsEntity", "PARASITE_REMAINS", "addFreshEntity"]) {
  if (deathFx.includes(forbidden)) failures.push(`parasite death FX still spawns remains: ${forbidden}`);
}

const dispatcher = read("src/main/java/alku/csrp/event/DispatcherNidusEvents.java");
for (const forbidden of ["DispatcherNidusBlock.tryPlace", "NIDUS_KILLS", "PLACE_KILLS", "PLACE_CHANCE"]) {
  if (dispatcher.includes(forbidden)) failures.push(`death-triggered Dispatcher Nidus placement remains: ${forbidden}`);
}
expect(dispatcher, /findNearbyNidus[\s\S]*?nidus\.addKill\(\)/,
  "existing Dispatcher Nidi no longer receive nearby parasite kills");

const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const deathBurst = adapted.match(/private void createBolsterDeathBurst\(\) \{[\s\S]*?\n    \}/)?.[0] ?? "";
if (!deathBurst) failures.push("could not audit the Adapted Bolster death burst");
if (/spreadBolsterResidue|setBlock|INFESTED_REMAINS/.test(deathBurst)) {
  failures.push("Adapted Bolster death burst still places infected-residue blocks");
}
expect(adapted, /tickBolster[\s\S]*?spreadBolsterResidue\(\)/,
  "non-death Adapted Bolster residue skill was removed unexpectedly");

const removedRemainsFiles = [
  "src/main/java/alku/csrp/entity/ParasiteRemainsEntity.java",
  "src/main/java/alku/csrp/client/renderer/ParasiteRemainsRenderer.java"
];
for (const relative of removedRemainsFiles) {
  if (fs.existsSync(path.join(root, relative))) {
    failures.push(`obsolete death-fragment implementation remains: ${relative}`);
  }
}

const entityRegistry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const clientEvents = read("src/main/java/alku/csrp/client/ClientModEvents.java");
for (const forbidden of ["ParasiteRemainsEntity", "PARASITE_REMAINS", "parasite_remains"]) {
  if (entityRegistry.includes(forbidden) || clientEvents.includes(forbidden)) {
    failures.push(`death-fragment entity is still registered: ${forbidden}`);
  }
}

const javaRoot = path.join(root, "src/main/java/alku/csrp");
const forbiddenPlacement = /setBlock(?:AndUpdate)?\s*\(|\.tryPlace\s*\(|INFESTED_REMAINS|PARASITE_REMAINS/;
for (const absolute of javaFiles(javaRoot)) {
  const source = fs.readFileSync(absolute, "utf8");
  const relative = path.relative(root, absolute);
  const deathHandlers = methodBodies(source,
    /(?:public|protected|private)\s+static\s+void\s+\w+\s*\([^)]*LivingDeathEvent[^)]*\)\s*\{/g);
  const entityDeathMethods = methodBodies(source,
    /(?:public|protected)\s+void\s+(?:die|tickDeath)\s*\([^)]*\)\s*\{/g);
  for (const body of [...deathHandlers, ...entityDeathMethods]) {
    if (forbiddenPlacement.test(body)) {
      failures.push(`${relative} still places a block from a monster death path`);
      break;
    }
  }
}

if (failures.length) {
  console.error(`Death block-generation verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Monster deaths no longer place, create, or convert world blocks.");
