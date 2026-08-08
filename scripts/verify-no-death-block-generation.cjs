const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const cystEvent = path.join(root, "src/main/java/alku/csrp/event/ParasiticCystEvents.java");
if (fs.existsSync(cystEvent)) {
  failures.push("parasite deaths still register the Gluttonous Cyst placement event");
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

const remains = read("src/main/java/alku/csrp/entity/ParasiteRemainsEntity.java");
for (const forbidden of ["BlockInfestation", "contaminate(", "setBlock", "setBlockAndUpdate"]) {
  if (remains.includes(forbidden)) failures.push(`death remains can still mutate blocks: ${forbidden}`);
}
expect(remains, /tickCount >= LIFETIME_TICKS[\s\S]*?discard\(\)/,
  "death remains no longer expire cleanly");

const eventDirectory = path.join(root, "src/main/java/alku/csrp/event");
for (const entry of fs.readdirSync(eventDirectory)) {
  if (!entry.endsWith(".java")) continue;
  const source = fs.readFileSync(path.join(eventDirectory, entry), "utf8");
  if (source.includes("LivingDeathEvent")
      && /setBlock(?:AndUpdate)?\(|\.tryPlace\(/.test(source)) {
    failures.push(`${entry} still combines a death event with block placement`);
  }
}

if (failures.length) {
  console.error(`Death block-generation verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Monster deaths no longer place, create, or convert world blocks.");
