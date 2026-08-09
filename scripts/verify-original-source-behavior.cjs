const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const source = read("src/main/java/alku/csrp/entity/SourceEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const legacyPath = path.join(root, "src/main/java/alku/csrp/entity/LegacyAuxiliaryEntity.java");
const legacy = fs.existsSync(legacyPath) ? fs.readFileSync(legacyPath, "utf8") : "";
const allEntities = fs.readdirSync(path.join(root, "src/main/java/alku/csrp/entity"))
  .filter((file) => file.endsWith(".java") && file !== "SourceEntity.java")
  .map((file) => fs.readFileSync(path.join(root, "src/main/java/alku/csrp/entity", file), "utf8"))
  .join("\n");

expect(registry, /EntityType<SourceEntity>> SOURCE[\s\S]*?sized\(0\.5F, 0\.5F\)/,
  "source is not registered as its original dedicated entity type");
expect(client, /ModEntities\.SOURCE\.get\(\), NoopRenderer::new/,
  "source does not preserve its original empty renderer");
expect(source, /new ServerBossEvent\(getName\(\),[\s\S]*?BossBarColor\.RED[\s\S]*?BossBarOverlay\.PROGRESS/,
  "source red progress BossBar is missing");
expect(source, /Component\.literal\("The Source"\)/, "source BossBar name is not original");
expect(source, /float total = 100\.0F/, "source total charge is not 100");
expect(source, /tickCount % 20 == 0[\s\S]*?charging\+\+/, "source does not charge once per second");
expect(source, /charging > total[\s\S]*?attack\(\)/, "source does not enter its completed attack state");
expect(source, /charging > 200\.0F[\s\S]*?discard\(\)/, "source does not expire after 200 charge");
expect(source, /startSeenByPlayer[\s\S]*?bossEvent\.addPlayer\(player\)/,
  "source BossBar is not shown to tracking players");
expect(source, /stopSeenByPlayer[\s\S]*?bossEvent\.removePlayer\(player\)/,
  "source BossBar is not removed from players");
expect(source, /setCustomName[\s\S]*?bossEvent\.setName\(getName\(\)\)/,
  "source custom name does not update its BossBar");
if (/new SourceEntity|ModEntities\.SOURCE\.get\(\)\.create/.test(allEntities)) {
  failures.push("source was attached to gameplay even though SRP 1.10.7 never creates it");
}
if (/\bSOURCE\b/.test(legacy)) failures.push("LegacyAuxiliaryEntity still carries the source id");

if (failures.length) {
  console.error("Original source behavior verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Original Source BossBar lifecycle is restored without inventing a caller.");
