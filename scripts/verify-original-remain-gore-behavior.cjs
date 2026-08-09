const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const remain = read("src/main/java/alku/csrp/entity/RemainEntity.java");
const gore = read("src/main/java/alku/csrp/entity/GoreEntity.java");
const goreRenderer = read("src/main/java/alku/csrp/client/renderer/GoreRenderer.java");
const inventory = read("src/main/java/alku/csrp/entity/ParasiteBlockInventory.java");

expect(registry, /EntityType<RemainEntity>> REMAIN/, "remain still uses an auxiliary placeholder");
expect(registry, /EntityType<GoreEntity>> GORE/, "gore still uses an auxiliary placeholder");
if (fs.existsSync(path.join(root, "src/main/java/alku/csrp/entity/LegacyAuxiliaryEntity.java"))) {
  failures.push("LegacyAuxiliaryEntity still exists");
}
expect(client, /ModEntities\.GORE\.get\(\), GoreRenderer::new/,
  "gore does not use its original visible model renderer");
expect(remain, /count \+= plus/, "remain rebuild progress is missing");
expect(remain, /rebuilt\.setHealth\(rebuilt\.getMaxHealth\(\) \* health\)/,
  "remain health multiplier is missing");
expect(remain, /ModMobEffects\.DEBAR, 400/, "remain resurrection Debar is missing");
expect(remain, /summoner\.resurrect/, "remain resurrection sound is missing");
expect(gore, /tickCount >= LIFETIME_TICKS/, "gore 200-tick lifetime is missing");
expect(gore, /movement\.add\(0\.0D, -0\.04D, 0\.0D\)/, "gore gravity is missing");
expect(gore, /goreType == 10[\s\S]*?GLUTTONOUS_CYST/, "gore cyst payload is missing");
expect(gore, /goreType == 11[\s\S]*?DispatcherNidusBlock\.tryPlace/,
  "gore Dispatcher Nidus payload is missing");
expect(gore, /getSkin\(\) < 1 \|\| getSkin\(\) > 4/,
  "non-particle Gore skins still emit an invented trail");
expect(goreRenderer, /case 1, 10 -> assimilated;[\s\S]*?default -> null;/,
  "Gore skin-to-model visibility differs from the original");
expect(inventory, /list\.size\(\) >= GoreEntity\.cystThreshold\(\)/,
  "full parasite block inventories do not launch cyst payloads");
expect(inventory, /gore\.setType\(\(byte\) 10\)/, "inventory cyst does not use original gore type 10");

for (const source of [remain, gore, inventory]) {
  if (/void\s+(?:die|tickDeath)\s*\([^)]*\)[\s\S]*?(?:setBlock|tryPlace)/.test(source)) {
    failures.push("a death method creates a block");
  }
}

if (failures.length) {
  console.error("Original Remain/Gore verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Original Remain rebuilds, Gore physics/rendering, and non-death cyst payloads restored.");
