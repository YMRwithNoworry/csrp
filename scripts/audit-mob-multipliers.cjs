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
const accessor = /public static double\s+(\w+)\(\)\s*\{\s*return\s+([A-Z0-9_]+)\.get\(\)/g;
for (const match of source.matchAll(accessor)) {
  const list = accessors.get(match[2]) ?? [];
  list.push(match[1]);
  accessors.set(match[2], list);
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

if (dangling.length) {
  console.error("\nDangling accessors would fail compilation; fix them first.");
  process.exit(1);
}
