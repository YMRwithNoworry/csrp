// Audits the per-mob multiplier config surface of MobsConfig.
//
// The earlier grep-based inventory produced truncated pseudo-keys (e.g. `anducaterArmorMultiplier`)
// because a single-line grep cannot cope with the multi-line `value(id, key, default, min, max,
// comment)` calls used throughout the file. This script parses those calls properly instead.
//
// Usage: node scripts/audit-mob-multipliers.cjs
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const source = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/config/MobsConfig.java"), "utf8");

// constant name -> config key, for every DoubleValue defined through the value(...) helper
const constants = new Map();
const declaration = /private static final ModConfigSpec\.DoubleValue\s+([A-Z0-9_]+)\s*=\s*value\(\s*"([^"]*)"\s*,\s*"([^"]*)"/g;
for (const match of source.matchAll(declaration)) {
  constants.set(match[1], { key: match[3], group: match[2] });
}

// constant name -> accessor name(s)
const accessors = new Map();
// the accessors read their constant either directly or through the safe(...) not-loaded guard
const accessor = /public static (?:double|float)\s+(\w+)\(\)\s*\{[^}]*?(?:([A-Z0-9_]+)\.get\(\)|safe\(([A-Z0-9_]+)\))/g;
for (const match of source.matchAll(accessor)) {
  const constant = match[2] ?? match[3];
  const list = accessors.get(constant) ?? [];
  list.push(match[1]);
  accessors.set(constant, list);
}

const multiplierKeys = [];
const unreachable = [];
const dangling = [];
for (const [name, info] of constants) {
  if (!info.key.toLowerCase().includes("multiplier")) continue;
  multiplierKeys.push(`${name} -> ${info.key}`);
  if (!accessors.has(name)) unreachable.push(`${name} (${info.key})`);
}
for (const [name, list] of accessors) {
  if (!constants.has(name)) dangling.push(`${list.join("/")} -> ${name}`);
}

console.log(`declared value(...) constants: ${constants.size}`);
console.log(`per-mob multiplier keys:      ${multiplierKeys.length}`);
console.log(`accessors:                    ${[...accessors.values()].flat().length}`);
console.log(`\nunreachable (no accessor):     ${unreachable.length}`);
for (const line of unreachable.sort()) console.log(`  ${line}`);
console.log(`\ndangling (accessor without constant): ${dangling.length}`);
for (const line of dangling.sort()) console.log(`  ${line}`);

// Classify the backlog against the original mod: a key the original never had is a port-only
// extension, so "no accessor" is not a restoration gap for it. Original field names drop the port's
// `primitive` prefix (port primitiveBolsterHealthMultiplier <- original bolsterHealthMultiplier),
// so both spellings are probed.
const originalConfig = path.join(root, "..", "_srp-orig", "decomp-1.10.9", "dhanantry",
  "scapeandrunparasites", "util", "config", "SRPConfigMobs.java");
if (fs.existsSync(originalConfig)) {
  const original = fs.readFileSync(originalConfig, "utf8");
  const originalBacked = [];
  const portOnly = [];
  for (const entry of unreachable) {
    const key = entry.slice(entry.indexOf("(") + 1, -1);
    // third spelling: the port constant name in camelCase (JINJO_HEALTH_MULTIPLIER ->
    // jinjoHealthMultiplier), which is how the original names the same knob for that mob.
    const constant = entry.slice(0, entry.indexOf(" ")).toLowerCase()
      .replace(/_([a-z0-9])/g, (_, c) => c.toUpperCase());
    const candidates = [key, key.replace(/^primitive/, ""), constant];
    (candidates.some((name) => original.includes("float " + name)) ? originalBacked : portOnly).push(key);
  }
  console.log(`\nbacklog backed by the original (must be wired): ${originalBacked.length}`);
  for (const key of originalBacked.sort()) console.log(`  ${key}`);
  console.log(`backlog port-only (no restoration gap):         ${portOnly.length}`);
  for (const key of portOnly.sort()) console.log(`  ${key}`);
} else {
  console.log("\n(original SRPConfigMobs.java not found; backlog not classified)");
}

// Informational only: some accessors read constants declared through other helpers (follow ranges,
// explosion multipliers) that this parser does not model, so they are not failures.
if (dangling.length) {
  console.log(`\n(accessors whose constant this parser does not model: ${dangling.length})`);
}

// --strict turns the inventory into a guard: any unreachable multiplier key outside the known
// backlog fails the run, so a newly added dead key cannot slip in unnoticed. The allowlist is the
// parsed backlog itself (see the group names above) - shrink it as groups get wired up.
const KNOWN_UNWIRED_GROUPS = [
  "REEKER_", "VISCERA_", "YELLOWEYE_",
  "JINJO_", "OVERSEER_", "VIGILANTE_", "WARDEN_"
];
if (process.argv.includes("--strict")) {
  const unexpected = unreachable.filter((entry) => !KNOWN_UNWIRED_GROUPS.some((g) => entry.startsWith(g)));
  if (unexpected.length) {
    console.error(`\n${unexpected.length} new unreachable multiplier key(s):`);
    for (const line of unexpected) console.error(`  ${line}`);
    process.exit(1);
  }
  console.log(`\nstrict: ${unreachable.length} known backlog key(s), 0 new ones.`);
}
