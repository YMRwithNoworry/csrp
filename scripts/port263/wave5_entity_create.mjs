#!/usr/bin/env node
// wave 5a — MC 26.3 entity creation and registration keys.
//
// Verified against .ref263/mc-src:
//   net/minecraft/world/entity/EntityType.java
//     public T create(Level level, EntitySpawnReason reason)      // the 1-arg create is gone
//     public EntityType<T> build(ResourceKey<EntityType<?>> name) // was build(String)
//   net/minecraft/world/entity/EntitySpawnReason.java             // MOB_SUMMONED/CONVERSION/...
//
// The create() rewrite is driven by the census: only call sites javac actually rejected are
// touched, and only when the argument list has no top-level comma (i.e. still the 1-arg form).
// Every mod-side spawn is a programmatic summon, so it passes MOB_SUMMONED — the same reason the
// port already uses in AssimilatedMeltSystem/MovingFleshEntity.
//
// Usage: node scripts/port263/wave5_entity_create.mjs [--file .ref263/census8.json] [--dry]
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const dry = process.argv.includes("--dry");
const fileArg = process.argv.indexOf("--file");
const censusFile = fileArg >= 0
  ? process.argv[fileArg + 1]
  : latestCensus();

function latestCensus() {
  const dir = path.join(ROOT, ".ref263");
  return path.join(".ref263", fs.readdirSync(dir)
    .map((name) => /^census(\d*)\.json$/.exec(name))
    .filter(Boolean)
    .map((m) => ({ name: m[0], n: m[1] === "" ? 1 : Number(m[1]) }))
    .sort((a, b) => b.n - a.n)[0].name);
}

const errors = JSON.parse(fs.readFileSync(path.join(ROOT, censusFile), "utf8"));
const createErrors = errors.filter((e) => e.msg.includes("no suitable method found for create("));

/** Finds the matching close paren of the paren at `open`, honouring strings and char literals. */
function matchParen(text, open) {
  let depth = 0;
  let quote = null;
  for (let i = open; i < text.length; i++) {
    const c = text[i];
    if (quote) {
      if (c === "\\") i++;
      else if (c === quote) quote = null;
      continue;
    }
    if (c === '"' || c === "'") quote = c;
    else if (c === "(") depth++;
    else if (c === ")") {
      depth--;
      if (depth === 0) return i;
    }
  }
  return -1;
}

const byFile = new Map();
for (const e of createErrors) {
  if (!byFile.has(e.file)) byFile.set(e.file, []);
  byFile.get(e.file).push(e);
}

let totalEdits = 0;
const touchedFiles = [];
for (const [file, fileErrors] of byFile) {
  const text = fs.readFileSync(file, "utf8");
  const lineStarts = [0];
  for (let i = 0; i < text.length; i++) {
    if (text[i] === "\n") lineStarts.push(i + 1);
  }
  const edits = [];
  for (const error of fileErrors) {
    const from = lineStarts[error.line - 1];
    if (from === undefined) continue;
    const idx = text.indexOf(".create(", from);
    if (idx < 0 || idx - from > 300) continue;
    const open = idx + ".create(".length - 1;
    const close = matchParen(text, open);
    if (close < 0) continue;
    const args = text.slice(open + 1, close);
    // Skip calls that already pass a reason (top-level comma present).
    let depth = 0;
    let quote = null;
    let hasComma = false;
    for (const c of args) {
      if (quote) {
        if (c === quote) quote = null;
      } else if (c === '"' || c === "'") quote = c;
      else if (c === "(") depth++;
      else if (c === ")") depth--;
      else if (c === "," && depth === 0) hasComma = true;
    }
    if (hasComma) continue;
    edits.push(close);
  }
  if (!edits.length) continue;
  const unique = [...new Set(edits)].sort((a, b) => b - a);
  let updated = text;
  for (const at of unique) {
    updated = `${updated.slice(0, at)}, EntitySpawnReason.MOB_SUMMONED${updated.slice(at)}`;
  }
  if (!/^import net\.minecraft\.world\.entity\.EntitySpawnReason;$/m.test(updated)) {
    const lines = updated.split("\n");
    let last = -1;
    for (let i = 0; i < lines.length; i++) if (lines[i].startsWith("import ")) last = i;
    lines.splice(last + 1, 0, "import net.minecraft.world.entity.EntitySpawnReason;");
    updated = lines.join("\n");
  }
  totalEdits += unique.length;
  touchedFiles.push({ file, count: unique.length });
  if (!dry) fs.writeFileSync(file, updated, "utf8");
}

for (const { file, count } of touchedFiles.sort((a, b) => b.count - a.count)) {
  console.log(`${dry ? "[dry] " : ""}${path.relative(ROOT, file).split(path.sep).join("/")}: x${count}`);
}
console.log(`\nwave 5a: ${totalEdits} create() call sites given EntitySpawnReason.MOB_SUMMONED`);

// --- 5b: EntityType.Builder.build(ResourceKey<EntityType<?>>) -----------------------------
const modEntities = path.join(ROOT, "src", "main", "java", "alku", "csrp", "registry", "ModEntities.java");
let entities = fs.readFileSync(modEntities, "utf8");
const before = entities;
entities = entities.replace(
  /\.build\(Identifier\.fromNamespaceAndPath\(Csrp\.MODID, ("[^"]*"|id)\)\.toString\(\)\)/g,
  ".build(entityKey($1))");
entities = entities.replace(
  /\.build\(Identifier\.fromNamespaceAndPath\(Csrp\.MODID, ("[^"]*"|id)\)\.toString\(\)\)/g,
  ".build(entityKey($1))");
const buildCount = (before.match(/\.build\(Identifier\.fromNamespaceAndPath/g) ?? []).length;
if (entities !== before) {
  if (!/^import net\.minecraft\.resources\.ResourceKey;$/m.test(entities)) {
    entities = entities.replace(/^import net\.minecraft\.resources\.Identifier;$/m,
      "import net.minecraft.resources.Identifier;\nimport net.minecraft.resources.ResourceKey;");
  }
  if (!entities.includes("private static ResourceKey<EntityType<?>> entityKey(")) {
    entities = entities.replace(
      /(    private static <T extends net\.minecraft\.world\.entity\.Mob> DeferredHolder<EntityType<\?>, EntityType<T>> monster\()/,
      "    /** MC 26.3 builds an entity type from a registry key instead of a raw id string. */\n"
      + "    private static ResourceKey<EntityType<?>> entityKey(String id) {\n"
      + "        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Csrp.MODID, id));\n"
      + "    }\n\n$1");
  }
  if (!dry) fs.writeFileSync(modEntities, entities, "utf8");
  console.log(`wave 5b: ${buildCount} EntityType.Builder.build(String) call sites -> build(entityKey(...))`);
} else {
  console.log("wave 5b: no build(String) call sites left");
}
