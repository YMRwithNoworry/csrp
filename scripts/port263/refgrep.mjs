#!/usr/bin/env node
// Search the real 26.3 + NeoForge 26.3 source tree (.ref263/mc-src) for API names.
//
// PLAN.md forbids writing API signatures from memory during the port, and the
// workspace grep tool skips .ref263, so this is the lookup tool for every wave.
//
// Usage:
//   node scripts/port263/refgrep.mjs <regex> [-n max] [-p subpath] [--files] [--ctx 2]
//   node scripts/port263/refgrep.mjs "class EntityType" -p net/minecraft/world/entity
//   node scripts/port263/refgrep.mjs "\bcreate\(" -p net/minecraft/world/entity/EntityType.java
//
// Exit code 1 when there are no matches.

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const SRC = path.join(ROOT, ".ref263", "mc-src");

const argv = process.argv.slice(2);
function opt(name, fallback = null) {
  const i = argv.indexOf(`-${name}`);
  if (i < 0) return fallback;
  const v = argv[i + 1];
  argv.splice(i, 2);
  return v;
}

const max = Number(opt("n", "60"));
const sub = opt("p", "");
const ctx = Number(opt("ctx", "0"));
const filesOnly = argv.includes("--files");
const pattern = argv.filter((a) => !a.startsWith("-")).join(" ");
if (!pattern) {
  console.error("usage: node scripts/port263/refgrep.mjs <regex> [-n max] [-p subpath] [--files] [--ctx N]");
  process.exit(2);
}
const re = new RegExp(pattern);

const start = sub ? path.join(SRC, sub) : SRC;
function walk(target, out = []) {
  const st = fs.statSync(target, { throwIfNoEntry: false });
  if (!st) return out;
  if (st.isFile()) {
    out.push(target);
    return out;
  }
  for (const entry of fs.readdirSync(target, { withFileTypes: true })) {
    const full = path.join(target, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (entry.name.endsWith(".java")) out.push(full);
  }
  return out;
}

let hits = 0;
const matchedFiles = new Set();
outer:
for (const file of walk(start)) {
  const lines = fs.readFileSync(file, "utf8").split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    if (!re.test(lines[i])) continue;
    hits++;
    matchedFiles.add(file);
    if (filesOnly) {
      if (matchedFiles.size >= max) break outer;
      continue;
    }
    const rel = path.relative(SRC, file).split(path.sep).join("/");
    const from = Math.max(0, i - ctx);
    const to = Math.min(lines.length - 1, i + ctx);
    for (let j = from; j <= to; j++) {
      console.log(`${rel}:${j + 1}: ${lines[j].trim()}`);
    }
    if (ctx > 0) console.log("--");
    if (hits >= max) break outer;
  }
}

if (filesOnly) {
  for (const f of matchedFiles) console.log(path.relative(SRC, f).split(path.sep).join("/"));
}
console.log(`# ${hits} match(es)${filesOnly ? ` in ${matchedFiles.size} file(s)` : ""}`);
process.exit(hits === 0 ? 1 : 0);
