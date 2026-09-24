#!/usr/bin/env node
// Census helper for the MC 26.3 port (Node port of scripts/port263/census.py:
// this machine has no python interpreter).
//
// Runs javac over the whole source set with the *real* NeoForge/MC compile
// classpath (materialised by `gradlew dumpCompileClasspath` into
// build/compile-cp.txt) and prints an aggregated error breakdown.
//
// javac stops at 100 errors by default, which is useless for a port of this size,
// so -Xmaxerrs is raised. Gradle's own output is truncated by the harness, which
// is the other reason this runs javac directly: we need the complete list to
// drive codemods instead of fixing errors 100 at a time.
//
// Usage:  node scripts/port263/census.mjs [--out .ref263/censusN.txt]

import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const JAVA_HOME = process.env.CSRP_JAVA_HOME ?? "D:\\MC\\jdk\\graalvm-25.2.4+7.1";
const CENSUS_DIR = path.join(ROOT, ".ref263");
const HEADER = /^(.*?\.java):(\d+): error: (.*)$/;

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (entry.name.endsWith(".java")) out.push(full);
  }
  return out;
}

function compileAll(outPath) {
  const cpFile = path.join(ROOT, "build", "compile-cp.txt");
  if (!fs.existsSync(cpFile)) {
    console.error("missing build/compile-cp.txt - run: gradlew dumpCompileClasspath");
    process.exit(2);
  }
  const cp = fs.readFileSync(cpFile, "utf8").trim();
  const sources = walk(path.join(ROOT, "src", "main", "java"));
  const argFile = path.join(CENSUS_DIR, "sources.txt");
  fs.writeFileSync(argFile, sources.join("\n"), "utf8");
  fs.mkdirSync(path.join(CENSUS_DIR, "classes"), { recursive: true });

  const javac = path.join(JAVA_HOME, "bin", "javac.exe");
  const args = [
    "-J-Duser.language=en", "-J-Duser.country=US",
    // javac writes diagnostics in the *console* charset unless told otherwise, which turns the
    // workspace path (D:\code\MC模组\...) into mojibake and breaks every later source lookup.
    "-J-Dfile.encoding=UTF-8", "-J-Dstdout.encoding=UTF-8", "-J-Dstderr.encoding=UTF-8",
    "-Xmaxerrs", "30000", "-nowarn", "-proc:none", "-encoding", "UTF-8",
    "-d", path.join(CENSUS_DIR, "classes"), "-cp", cp, `@${argFile}`
  ];
  const r = spawnSync(javac, args, { encoding: "buffer", maxBuffer: 1 << 30 });
  if (r.error) {
    console.error(`failed to run javac: ${r.error.message}`);
    process.exit(2);
  }
  const bytes = Buffer.concat([r.stdout ?? Buffer.alloc(0), r.stderr ?? Buffer.alloc(0)]);
  let text = bytes.toString("utf8");
  if (text.includes("\uFFFD")) {
    // Fall back to the legacy console charset if javac ignored the encoding properties.
    try {
      text = new TextDecoder("gbk").decode(bytes);
    } catch {
      // keep the UTF-8 best effort
    }
  }
  fs.writeFileSync(outPath, text, "utf8");
  return sources.length;
}

function parse(text) {
  const errors = [];
  let cur = null;
  for (const line of text.split(/\r?\n/)) {
    const m = HEADER.exec(line);
    if (m) {
      cur = { file: m[1], line: Number(m[2]), msg: m[3], symbol: null, loc: null };
      errors.push(cur);
      continue;
    }
    if (!cur) continue;
    const s = line.trim();
    if (s.startsWith("symbol:")) cur.symbol = s.slice(7).trim();
    else if (s.startsWith("location:")) cur.loc = s.slice(9).trim();
  }
  return errors;
}

function count(values) {
  const map = new Map();
  for (const v of values) map.set(v, (map.get(v) ?? 0) + 1);
  return [...map.entries()].sort((a, b) => b[1] - a[1]);
}

function main() {
  const outIndex = process.argv.indexOf("--out");
  const out = outIndex >= 0 ? process.argv[outIndex + 1] : path.join(CENSUS_DIR, "census.txt");
  const sourceCount = compileAll(out);
  const text = fs.readFileSync(out, "utf8");
  const errors = parse(text);

  console.log(`sources: ${sourceCount}   ERRORS: ${errors.length}   files affected: ${
    new Set(errors.map((e) => e.file)).size}`);
  console.log("\n=== TOP MESSAGES ===");
  for (const [msg, c] of count(errors.map((e) => e.msg)).slice(0, 20)) {
    console.log(`${String(c).padStart(6)}  ${msg}`);
  }
  console.log("\n=== TOP MISSING SYMBOLS ===");
  const symbols = errors
    .filter((e) => e.msg.includes("cannot find symbol") && e.symbol)
    .map((e) => e.symbol.replace(/^(class|variable|method|interface|enum)\s+/, ""));
  for (const [sym, c] of count(symbols).slice(0, 40)) {
    console.log(`${String(c).padStart(6)}  ${sym}`);
  }
  console.log("\n=== WORST FILES ===");
  for (const [file, c] of count(errors.map((e) => e.file)).slice(0, 20)) {
    console.log(`${String(c).padStart(6)}  ${path.relative(ROOT, file)}`);
  }
  fs.writeFileSync(out.replace(/\.txt$/, ".json"), JSON.stringify(errors), "utf8");
}

main();
