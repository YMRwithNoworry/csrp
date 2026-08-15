const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const architect = read("src/main/java/alku/csrp/entity/ArchitectEntity.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const originalEvents = read("src/main/java/alku/csrp/config/OriginalConfigEvents.java");
const spawning = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const common = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const failures = [];
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};
const expectPattern = (source, pattern, message) => expect(pattern.test(source), message);

expectPattern(architect, /moveControl = new ArchitectMoveControl\(\)[\s\S]*?noPhysics = true/,
  "Architect does not use the legacy no-clip flying move controller");
expectPattern(architect,
  /distance < getBoundingBox\(\)\.getSize\(\)[\s\S]*?0\.05D \* speedModifier[\s\S]*?Mth\.atan2/,
  "Architect move control is missing the original acceleration and facing behavior");
expectPattern(architect, /distance > 400\.0D[\s\S]*?mode = 2[\s\S]*?nextInt\(6\) - 2/,
  "Architect long-distance random-flight branch is incomplete");
expectPattern(architect, /distance < 100\.0D[\s\S]*?mode = 3[\s\S]*?nextInt\(4\) \+ 3/,
  "Architect close-distance random-flight branch is incomplete");
expectPattern(architect,
  /random\.nextFloat\(\) \* 2\.0F - 1\.0F[\s\S]*?\* 16\.0F[\s\S]*?setWantedPosition\(x, y, z, 0\.5D\)/,
  "Architect idle flight does not preserve the original +/-16 random range");
expectPattern(architect,
  /applyFlightLimit\(getTarget\(\)\)[\s\S]*?overseerMaxY\(\)[\s\S]*?hasGroundWithin/,
  "Architect maximum flight-height configuration is not applied");
expectPattern(architect,
  /tickCount % COLONY_WORKER_CYCLE_TICKS == COLONY_WORKER_CYCLE_OFFSET[\s\S]*?random\.nextInt\(10\) == 0[\s\S]*?spawnColonyWorker/,
  "Architect worker deployment does not preserve the original 1/10 cycle");
expectPattern(architect,
  /MobsConfig\.overseerTotalActiveMobs\(\)[\s\S]*?broadcastEntityEvent\(succor, \(byte\) 8\)/,
  "Architect Succor support does not preserve the configured six-capacity summon behavior or particles");
expectPattern(architect, /withEyeHeight\(1\.6F\)[\s\S]*?getSoundVolume\(\)[\s\S]*?2\.0F/,
  "Architect eye height or sound volume differs from EntityTenn");
expectPattern(architect, /boolean onlySpawnInside\(\)[\s\S]*?return true/,
  "Architect colony-only spawn marker is missing");
expectPattern(config, /entity instanceof ArchitectEntity\) return PURE_FOLLOW\.get\(\)/,
  "Architect does not use the original pure follow-range configuration");
expectPattern(originalEvents, /entity instanceof ArchitectEntity architect[\s\S]*?architect\.applyConfiguredAttributes\(\)/,
  "Architect configured attributes are not applied on join");
expectPattern(spawning,
  /event\.getEntity\(\) instanceof ArchitectEntity architect[\s\S]*?totalColonyPoints\(\) > 0[\s\S]*?nearestColonyInConstructionRange[\s\S]*?Result\.FAIL/,
  "Architect colony-only natural spawning gate is missing");
expect(common.includes('"architect"'), "Architect is missing from the air spawn placement set");
expect(client.includes('"architect", 1.3F'), "Architect renderer shadow radius is not the original 1.3F");

if (failures.length) {
  console.error(`Architect behavior verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Architect (EntityTenn) behavior port verification passed.");
