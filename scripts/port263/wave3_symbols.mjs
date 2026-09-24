#!/usr/bin/env node
// wave 3 — simple missing symbols of the MC 26.3 port.
//
// Every replacement below was verified against the real 26.3 sources in .ref263/mc-src
// (PLAN.md §7: never write an API name from memory):
//
//   isInWaterOrBubble()      -> isInWater()              Entity.isInWaterOrBubble is gone; 26.3
//                                                        keeps isInWater()/isInLiquid() and a
//                                                        fluid-interaction API for the rest.
//   hurtMarked = true        -> syncVelocity = true      Entity.hurtMarked was unified into the
//   hasImpulse = true        -> syncVelocity = true      public Entity.syncVelocity field
//                                                        (Entity.java:284; vanilla Ravager does
//                                                        `defender.syncVelocity = true`).
//   .noCollission()          -> .noCollision()           BlockBehaviour.Properties.noCollision()
//   .getMinBuildHeight()     -> .getMinY()               level height accessors
//   .getDayTime()            -> .getOverworldClockTime() Level.getDayTime() is gone; the debug
//                                                        string uses ("game time", "day time") =
//                                                        (getGameTime(), getOverworldClockTime()).
//   level().getGameRules().getBoolean(GameRules.RULE)
//                            -> SrpGameRules.mobGriefing(level())
//                                                        Level lost getGameRules(); only
//                                                        ServerLevel has it and reads rules with
//                                                        getGameRules().get(GameRule<T>).
//
// Usage: node scripts/port263/wave3_symbols.mjs [--dry]
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const SRC = path.join(ROOT, "src", "main", "java");
const dry = process.argv.includes("--dry");

const RULES = [
  { name: "isInWaterOrBubble() -> isInWater()", re: /\bisInWaterOrBubble\(\)/g, to: "isInWater()" },
  { name: "hurtMarked -> syncVelocity", re: /\bhurtMarked\b/g, to: "syncVelocity" },
  { name: "hasImpulse -> syncVelocity", re: /\bhasImpulse\b/g, to: "syncVelocity" },
  { name: "noCollission() -> noCollision()", re: /\.noCollission\(\)/g, to: ".noCollision()" },
  { name: "getMinBuildHeight() -> getMinY()", re: /\.getMinBuildHeight\(\)/g, to: ".getMinY()" },
  {
    name: "getDayTime() -> getOverworldClockTime()",
    re: /\.getDayTime\(\)/g,
    to: ".getOverworldClockTime()"
  },
  {
    name: "Level.getGameRules().getBoolean(rule) -> SrpGameRules.flag(level, rule)",
    re: /([A-Za-z_$][\w$]*(?:\(\))?)\.getGameRules\(\)\.getBoolean\(GameRules\.([A-Z_0-9]+)\)/g,
    to: (_m, levelExpr, rule) => rule === "MOB_GRIEFING"
      ? `SrpGameRules.mobGriefing(${levelExpr})`
      : `SrpGameRules.flag(${levelExpr}, GameRules.${rule})`,
    needsImport: "alku.csrp.world.SrpGameRules"
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

function addImport(text, fqcn) {
  const simple = fqcn.split(".").pop();
  if (new RegExp(`^import\\s+${fqcn.replace(/\./g, "\\.")}\\s*;`, "m").test(text)) return text;
  if (new RegExp(`\\b${simple}\\.`).test(text.replace(/^import .*$/gm, "")) === false) return text;
  const lines = text.split("\n");
  let last = -1;
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith("import ")) last = i;
  }
  if (last < 0) return text;
  lines.splice(last + 1, 0, `import ${fqcn};`);
  return lines.join("\n");
}

const totals = new Map();
for (const file of walk(SRC)) {
  const original = fs.readFileSync(file, "utf8");
  let text = original;
  const touched = [];
  for (const rule of RULES) {
    const before = text;
    text = rule.re.test(text) ? text.replace(rule.re, rule.to) : text;
    rule.re.lastIndex = 0;
    if (text !== before) {
      const count = (before.match(rule.re) ?? []).length;
      rule.re.lastIndex = 0;
      touched.push(`${rule.name} x${count}`);
      totals.set(rule.name, (totals.get(rule.name) ?? 0) + count);
      if (rule.needsImport) text = addImport(text, rule.needsImport);
    }
  }
  if (text !== original && !dry) fs.writeFileSync(file, text, "utf8");
  if (text !== original) {
    console.log(`${dry ? "[dry] " : ""}${path.relative(ROOT, file).split(path.sep).join("/")}: ${
      touched.join(", ")}`);
  }
}

console.log("\n=== wave 3 totals ===");
for (const [name, count] of [...totals.entries()].sort((a, b) => b[1] - a[1])) {
  console.log(`${String(count).padStart(6)}  ${name}`);
}
