// Clusters the remaining unaudited-free gaps: groups every "missing"/"partial" clause in
// docs/entity-parity/raw/*.json by its leading text so the biggest shared gaps are visible.
// Usage: node scripts/entity-parity/cluster-gaps.cjs
const fs = require("node:fs");
const path = require("node:path");

const rawDir = path.join(__dirname, "..", "..", "docs", "entity-parity", "raw");
const counts = new Map();
const partialCounts = new Map();

for (const file of fs.readdirSync(rawDir).filter((name) => name.endsWith(".json"))) {
  const data = JSON.parse(fs.readFileSync(path.join(rawDir, file), "utf8"));
  for (const facet of data.facets ?? []) {
    for (const clause of facet.clauses ?? []) {
      if (clause.verdict === "satisfied" || clause.verdict === "na") continue;
      const key = String(clause.clause ?? "").replace(/[（(].*?[)）]/g, "").slice(0, 44);
      const bucket = clause.verdict === "missing" ? counts : partialCounts;
      bucket.set(key, (bucket.get(key) ?? 0) + 1);
    }
  }
}

function top(map, label, limit) {
  console.log(`\n=== ${label} (top ${limit}) ===`);
  for (const [key, value] of [...map.entries()].sort((a, b) => b[1] - a[1]).slice(0, limit)) {
    console.log(`${String(value).padStart(3)}  ${key}`);
  }
}

top(counts, "missing clauses", 18);
top(partialCounts, "partial clauses", 12);
console.log(`\ntotals: missing ${[...counts.values()].reduce((a, b) => a + b, 0)}, `
  + `partial ${[...partialCounts.values()].reduce((a, b) => a + b, 0)}`);
