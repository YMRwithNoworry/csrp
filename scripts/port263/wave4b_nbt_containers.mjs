#!/usr/bin/env node
// wave 4b — NBT container getters that return Optional in 26.3, plus the two artefacts the
// wave 4 getter rewrite produced on same-named non-NBT methods.
//
// Verified against .ref263/mc-src/net/minecraft/nbt/{CompoundTag,ListTag}.java:
//   Optional<CompoundTag> getCompound(String)   / getCompound(int)  + getCompoundOrEmpty(String|int)
//   Optional<ListTag>     getList(String, int)  is gone            + getListOrEmpty(String|int)
//   Optional<int[]>       getIntArray(String)   / getIntArray(int)
//   Optional<long[]>      getLongArray(String)  / getLongArray(int)
// and java.lang.Boolean.getBoolean(String) must stay single-argument (wave 4 matched it because
// it shares the name with CompoundTag.getBoolean).
//
// Usage: node scripts/port263/wave4b_nbt_containers.mjs [--dry]
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const SRC = path.join(ROOT, "src", "main", "java");
const dry = process.argv.includes("--dry");

const RULES = [
  {
    name: "Boolean.getBooleanOr(x, default) -> Boolean.getBoolean(x)",
    re: /Boolean\.getBooleanOr\(([^,()]+),\s*(?:true|false)\)/g,
    to: "Boolean.getBoolean($1)"
  },
  {
    name: "getList(key, Tag.TAG_X) -> getListOrEmpty(key)",
    re: /\.getList\(([^,()]+),\s*Tag\.TAG_[A-Z_]+\)/g,
    to: ".getListOrEmpty($1)"
  },
  { name: "getCompound -> getCompoundOrEmpty", re: /\.getCompound\(/g, to: ".getCompoundOrEmpty(" },
  {
    name: "getIntArray(key) -> getIntArray(key).orElse(new int[0])",
    re: /\.getIntArray\(("[^"]*")\)/g,
    to: ".getIntArray($1).orElse(new int[0])"
  },
  {
    name: "getLongArray(key) -> getLongArray(key).orElse(new long[0])",
    re: /\.getLongArray\(("[^"]*")\)/g,
    to: ".getLongArray($1).orElse(new long[0])"
  }
];

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (entry.name.endsWith(".java")) out.push(full);
  }
  return out;
}

const totals = new Map();
for (const file of walk(SRC)) {
  const original = fs.readFileSync(file, "utf8");
  let text = original;
  const touched = [];
  for (const rule of RULES) {
    const matches = text.match(rule.re);
    rule.re.lastIndex = 0;
    if (!matches) continue;
    text = text.replace(rule.re, rule.to);
    rule.re.lastIndex = 0;
    touched.push(`${rule.name} x${matches.length}`);
    totals.set(rule.name, (totals.get(rule.name) ?? 0) + matches.length);
  }
  if (text !== original) {
    if (!dry) fs.writeFileSync(file, text, "utf8");
    console.log(`${dry ? "[dry] " : ""}${path.relative(ROOT, file).split(path.sep).join("/")}: ${
      touched.join(", ")}`);
  }
}

console.log("\n=== wave 4b totals ===");
let sum = 0;
for (const [name, count] of [...totals.entries()].sort((a, b) => b[1] - a[1])) {
  console.log(`${String(count).padStart(6)}  ${name}`);
  sum += count;
}
console.log(`${String(sum).padStart(6)}  total`);
