#!/usr/bin/env node
// List the clause-level parity gaps recorded in docs/entity-parity/raw/<mob>.json.
//
// Usage:
//   node scripts/entity-parity/list-gaps.cjs                        # summary per mob
//   node scripts/entity-parity/list-gaps.cjs --mob sim_cow          # all non-satisfied clauses
//   node scripts/entity-parity/list-gaps.cjs --mob sim_cow --verdict missing --facet behaviors
//   node scripts/entity-parity/list-gaps.cjs --facet damage_and_effects --mobs
//
// The raw audits are produced by the audit protocol in docs/entity-parity/AUDIT_PROTOCOL.md;
// this tool only reads them so implementation batches can be driven by verified gaps.
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..", "..");
const rawDir = path.join(root, "docs", "entity-parity", "raw");

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}
const flag = (name) => process.argv.includes(`--${name}`);

const mobFilter = arg("mob");
const verdictFilter = arg("verdict");
const facetFilter = arg("facet");
const files = fs.readdirSync(rawDir).filter((f) => f.endsWith(".json"))
  .filter((f) => !mobFilter || f === `${mobFilter}.json`).sort();

let missing = 0;
let partial = 0;
let satisfied = 0;
const facetCounts = new Map();

for (const file of files) {
  const data = JSON.parse(fs.readFileSync(path.join(rawDir, file), "utf8"));
  const rows = [];
  for (const facet of data.facets ?? []) {
    if (facetFilter && facet.name !== facetFilter) continue;
    for (const clause of facet.clauses ?? []) {
      const verdict = clause.verdict ?? "?";
      if (verdict === "satisfied") satisfied++;
      else if (verdict === "partial") partial++;
      else if (verdict === "missing") missing++;
      if (verdictFilter && verdict !== verdictFilter) continue;
      if (verdict === "satisfied" || verdict === "na") continue;
      facetCounts.set(facet.name, (facetCounts.get(facet.name) ?? 0) + 1);
      rows.push({ facet: facet.name, verdict, clause: clause.clause, evidence: clause.evidence, note: clause.note });
    }
  }
  if (!rows.length) continue;
  if (flag("mobs")) {
    console.log(`${data.id}: ${rows.length}`);
    continue;
  }
  console.log(`\n### ${data.id} (${data.originalClass} → ${data.projectClass})`);
  for (const r of rows) {
    const orig = (r.evidence?.original ?? "").replace(/^.*decomp-1\.10\.9[\\/]/, "").replace(/\\/g, "/");
    console.log(`- [${r.verdict}/${r.facet}] ${r.clause}`);
    if (orig) console.log(`    orig: ${orig}`);
    if (r.note) console.log(`    note: ${r.note}`);
  }
}

if (flag("mobs")) {
  console.log(`\nfacets: ${[...facetCounts.entries()].sort((a, b) => b[1] - a[1]).map(([k, v]) => `${k}=${v}`).join(" ")}`);
} else {
  console.log(`\n--- totals: satisfied=${satisfied} partial=${partial} missing=${missing} (mobs: ${files.length}) ---`);
}
