const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const primitive = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java"), "utf8");
const adapted = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/entity/AdaptedVariantEntity.java"), "utf8");
const events = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/config/OriginalConfigEvents.java"), "utf8");
const failures = [];

function methodBody(source, signature) {
  const start = source.indexOf(signature);
  if (start < 0) {
    failures.push(`Missing method: ${signature}`);
    return "";
  }
  const open = source.indexOf("{", start);
  let depth = 0;
  for (let index = open; index < source.length; index++) {
    if (source[index] === "{") depth++;
    if (source[index] === "}") depth--;
    if (depth === 0) return source.slice(open + 1, index);
  }
  failures.push(`Unclosed method: ${signature}`);
  return "";
}

function expect(source, pattern, message) {
  if (!pattern.test(source)) failures.push(message);
}

for (const [name, source] of [["primitive", primitive], ["adapted", adapted]]) {
  const attributes = methodBody(source,
    "public static AttributeSupplier.Builder createAttributes(Kind kind)");
  if (attributes.includes("MobsConfig.")) {
    failures.push(`${name} attributes read config before NeoForge loads it`);
  }
  const configured = methodBody(source, "public void applyConfiguredAttributes()");
  if (!configured.includes("MobsConfig.")) {
    failures.push(`${name} entities do not apply config after joining the level`);
  }
}

const joinHandler = methodBody(events,
  "public static void applyOriginalMobProperties(EntityJoinLevelEvent event)");
expect(joinHandler,
  /applyConfiguredMobAttributes\(entity\);[\s\S]*applyMultiplier\(entity\.getAttribute\(Attributes\.MAX_HEALTH\)/,
  "Per-mob config must be applied before global attribute multipliers");
expect(events, /primitive\.applyConfiguredAttributes\(\)/,
  "Primitive configured attributes are not applied on entity join");
expect(events, /adapted\.applyConfiguredAttributes\(\)/,
  "Adapted configured attributes are not applied on entity join");

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}

console.log("Config values are deferred until after NeoForge loads the config.");
