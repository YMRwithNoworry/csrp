#!/usr/bin/env node
// Query helper for the MC 26.3 port census (.ref263/censusN.json).
//
// Usage:
//   node scripts/port263/errors.mjs --file .ref263/census4.json --symbol isInWaterOrBubble
//   node scripts/port263/errors.mjs --file .ref263/census4.json --msg "cannot find symbol"
//   node scripts/port263/errors.mjs --file .ref263/census4.json --path client/particle
//   node scripts/port263/errors.mjs --file .ref263/census4.json --group-symbols 60
//   node scripts/port263/errors.mjs --file .ref263/census4.json --count
//
// --symbol matches the reported symbol, --msg the diagnostic text, --path the source
// file, and the default output prints file:line with the offending source line.

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

const file = arg("file", latestCensus());
const errors = JSON.parse(fs.readFileSync(path.join(ROOT, file), "utf8"));

/** Newest .ref263/censusN.json, so wave work always queries the current run. */
function latestCensus() {
  const dir = path.join(ROOT, ".ref263");
  const candidates = fs.readdirSync(dir)
    .map((name) => /^census(\d*)\.json$/.exec(name))
    .filter(Boolean)
    .map((m) => ({ name: m[0], n: m[1] === "" ? 1 : Number(m[1]) }))
    .sort((a, b) => b.n - a.n);
  if (!candidates.length) {
    console.error("no .ref263/censusN.json found - run scripts/port263/census.mjs first");
    process.exit(2);
  }
  return path.join(".ref263", candidates[0].name);
}
const symbol = arg("symbol");
const msg = arg("msg");
const pathFilter = arg("path");
const groupSymbols = Number(arg("group-symbols", "0"));
const groupMsg = Number(arg("group-msg", "0"));
const extract = arg("extract");
const limit = Number(arg("limit", "40"));
const showSource = process.argv.includes("--source");

function sourceLine(file, line) {
  try {
    return fs.readFileSync(file, "utf8").split(/\r?\n/)[line - 1]?.trim() ?? "";
  } catch {
    return "";
  }
}

if (process.argv.includes("--count")) {
  console.log(errors.length);
} else if (extract) {
  // Counts regex captures over the offending source lines: used to plan a codemod.
  const re = new RegExp(extract, "g");
  let candidates = errors;
  if (msg) candidates = candidates.filter((e) => e.msg.includes(msg));
  if (pathFilter) {
    candidates = candidates.filter((e) =>
      path.relative(ROOT, e.file).split(path.sep).join("/").includes(pathFilter));
  }
  const counts = new Map();
  for (const e of candidates) {
    for (const m of sourceLine(e.file, e.line).matchAll(re)) {
      const key = m[1] ?? m[0];
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
  }
  console.log(`${candidates.length} errors scanned`);
  for (const [k, c] of [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, limit)) {
    console.log(`${String(c).padStart(6)}  ${k}`);
  }
} else if (groupSymbols) {
  const counts = new Map();
  for (const e of errors) {
    if (!e.symbol) continue;
    const key = e.symbol.replace(/^(class|variable|method|interface|enum)\s+/, "");
    counts.set(key, (counts.get(key) ?? 0) + 1);
  }
  for (const [k, c] of [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, groupSymbols)) {
    console.log(`${String(c).padStart(6)}  ${k}`);
  }
} else if (groupMsg) {
  const counts = new Map();
  for (const e of errors) counts.set(e.msg, (counts.get(e.msg) ?? 0) + 1);
  for (const [k, c] of [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, groupMsg)) {
    console.log(`${String(c).padStart(6)}  ${k}`);
  }
} else {
  let hits = errors;
  if (symbol) hits = hits.filter((e) => e.symbol?.includes(symbol));
  if (msg) hits = hits.filter((e) => e.msg.includes(msg));
  if (pathFilter) {
    hits = hits.filter((e) => path.relative(ROOT, e.file).split(path.sep).join("/").includes(pathFilter));
  }
  console.log(`${hits.length} errors`);
  for (const e of hits.slice(0, limit)) {
    const rel = path.relative(ROOT, e.file).split(path.sep).join("/");
    console.log(`${rel}:${e.line}  [${e.msg}]${e.symbol ? ` {${e.symbol}}` : ""}${
      e.loc ? ` @${e.loc}` : ""}`);
    if (showSource) console.log(`    | ${sourceLine(e.file, e.line)}`);
  }
}
