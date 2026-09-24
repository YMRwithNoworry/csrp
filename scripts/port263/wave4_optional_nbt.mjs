#!/usr/bin/env node
// wave 4 — MC 26.3 turned the CompoundTag getters into Optional-returning methods.
//
// Verified against .ref263/mc-src/net/minecraft/nbt/CompoundTag.java:
//   public Optional<Integer> getInt(String)      + public int     getIntOr(String, int)
//   public Optional<Boolean> getBoolean(String)  + public boolean getBooleanOr(String, boolean)
//   public Optional<Float>   getFloat(String)    + public float   getFloatOr(String, float)
//   public Optional<Double>  getDouble(String)   + public double  getDoubleOr(String, double)
//   public Optional<Long>    getLong(String)     + public long    getLongOr(String, long)
//   public Optional<String>  getString(String)   + public String  getStringOr(String, String)
//   public Optional<Byte>    getByte(String)     + public byte    getByteOr(String, byte)
//   public Optional<Short>   getShort(String)    + public short   getShortOr(String, short)
//
// The default literal equals the value the 1.21.1 getter returned for a missing or
// mistyped key, so `x.getInt("k")` -> `x.getIntOr("k", 0)` is behaviour preserving.
// The argument list is scanned with a bracket/string aware walker, because keys are
// frequently built from nested calls (`tag.getInt(KEY + suffix())`).
//
// Usage: node scripts/port263/wave4_optional_nbt.mjs [--dry]
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const SRC = path.join(ROOT, "src", "main", "java");
const dry = process.argv.includes("--dry");

const GETTERS = [
  ["getInt", "0"],
  ["getBoolean", "false"],
  ["getFloat", "0.0F"],
  ["getDouble", "0.0D"],
  ["getLong", "0L"],
  ["getString", "\"\""],
  ["getByte", "(byte) 0"],
  ["getShort", "(short) 0"]
];

function rewriteCalls(text, method, fallback) {
  const needle = `.${method}(`;
  let out = "";
  let i = 0;
  let count = 0;
  for (;;) {
    const idx = text.indexOf(needle, i);
    if (idx < 0) {
      out += text.slice(i);
      break;
    }
    const open = idx + needle.length - 1;
    let depth = 0;
    let j = open;
    let quote = null;
    for (; j < text.length; j++) {
      const c = text[j];
      if (quote) {
        if (c === "\\") {
          j++;
        } else if (c === quote) {
          quote = null;
        }
        continue;
      }
      if (c === '"' || c === "'") {
        quote = c;
      } else if (c === "(") {
        depth++;
      } else if (c === ")") {
        depth--;
        if (depth === 0) break;
      }
    }
    const args = text.slice(open + 1, j);
    if (args.trim() === "") {
      // No argument at all: this is a different, unrelated method with the same name
      // (Component.getString()), which 26.3 still provides. Leave it alone.
      out += text.slice(i, j + 1);
      i = j + 1;
      continue;
    }
    out += text.slice(i, idx) + `.${method}Or(` + args + `, ${fallback})`;
    i = j + 1;
    count++;
  }
  return { text: out, count };
}

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
  for (const [method, fallback] of GETTERS) {
    const result = rewriteCalls(text, method, fallback);
    text = result.text;
    if (result.count) {
      touched.push(`${method}->${method}Or x${result.count}`);
      totals.set(method, (totals.get(method) ?? 0) + result.count);
    }
  }
  if (text !== original) {
    if (!dry) fs.writeFileSync(file, text, "utf8");
    console.log(`${dry ? "[dry] " : ""}${path.relative(ROOT, file).split(path.sep).join("/")}: ${
      touched.join(", ")}`);
  }
}

console.log("\n=== wave 4 totals ===");
let sum = 0;
for (const [name, count] of [...totals.entries()].sort((a, b) => b[1] - a[1])) {
  console.log(`${String(count).padStart(6)}  ${name} -> ${name}Or`);
  sum += count;
}
console.log(`${String(sum).padStart(6)}  total`);
