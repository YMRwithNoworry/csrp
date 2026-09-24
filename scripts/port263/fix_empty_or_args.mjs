#!/usr/bin/env node
// Repair helper: undo `XOr(, default)` artefacts that a codemod produced when it matched a
// no-argument method sharing a name with an NBT getter (Component.getString()).
//
// Usage: node scripts/port263/fix_empty_or_args.mjs [--dry]
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const SRC = path.join(ROOT, "src", "main", "java");
const dry = process.argv.includes("--dry");

const RE = /\.(getInt|getBoolean|getFloat|getDouble|getLong|getString|getByte|getShort)Or\(\s*,\s*[^)]*\)/g;

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (entry.name.endsWith(".java")) out.push(full);
  }
  return out;
}

let total = 0;
for (const file of walk(SRC)) {
  const original = fs.readFileSync(file, "utf8");
  let count = 0;
  const text = original.replace(RE, (_m, method) => {
    count++;
    return `.${method}()`;
  });
  if (count) {
    total += count;
    console.log(`${dry ? "[dry] " : ""}${path.relative(ROOT, file).split(path.sep).join("/")}: x${count}`);
    if (!dry) fs.writeFileSync(file, text, "utf8");
  }
}
console.log(`total repaired: ${total}`);
