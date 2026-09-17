/**
 * Copies assets that exist in the donor branch (`csrp-26.3`) but are still missing in this
 * 1.20.1 Forge port. Only additive: an existing target file is never overwritten, and every
 * copied file is reported.
 *
 * Version-safe categories only. Recipe / advancement / loot-table JSON is deliberately excluded
 * because those schemas changed between 1.20.1 and 26.3 and must be converted, not copied.
 *
 *   node scripts/sync-donor-assets.cjs [--dry]
 */
const fs = require("fs");
const path = require("path");

const DRY = process.argv.includes("--dry");
const ROOT = path.resolve(__dirname, "..");
const DONOR = path.resolve(ROOT, "..", "csrp-26.3");
const TARGET = ROOT;

if (!fs.existsSync(DONOR)) {
  console.error(`donor branch not found: ${DONOR}`);
  process.exit(1);
}

const trees = [
  "src/main/resources/assets/csrp/models",
  "src/main/resources/assets/csrp/blockstates",
  "src/main/resources/assets/csrp/geo",
  "src/main/resources/assets/csrp/animations",
  "src/main/resources/assets/csrp/textures",
  "src/main/resources/assets/csrp/sounds",
  "src/main/resources/assets/csrp/lang",
  "src/main/resources/assets/csrp/shaders",
  "src/main/resources/assets/csrp/particles",
  "src/main/resources/assets/csrp/compendium",
  "src/main/resources/data/csrp/structures"
];

// Paths that belong to another Minecraft version's resource layout.
const skip = [
  /^src\/main\/resources\/assets\/csrp\/items\//, // 1.21.4+ item model definitions
  /\/obj\// // Bedrock-style geometry folder; this port uses models/block/*.obj instead
];

let copied = 0;
let skippedExisting = 0;
const report = [];

for (const tree of trees) {
  const from = path.join(DONOR, tree);
  if (!fs.existsSync(from)) {
    continue;
  }
  let treeCopied = 0;
  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(full);
        continue;
      }
      const relative = path.relative(DONOR, full).split(path.sep).join("/");
      if (skip.some((pattern) => pattern.test(relative))) {
        continue;
      }
      const destination = path.join(TARGET, ...relative.split("/"));
      if (fs.existsSync(destination)) {
        skippedExisting += 1;
        continue;
      }
      if (!DRY) {
        fs.mkdirSync(path.dirname(destination), { recursive: true });
        fs.copyFileSync(full, destination);
      }
      treeCopied += 1;
      copied += 1;
    }
  };
  walk(from);
  if (treeCopied > 0) {
    report.push(`${tree.replace("src/main/resources/", "")}: +${treeCopied}`);
  }
}

console.log(report.join("\n"));
console.log(`\n${DRY ? "[dry] would copy" : "copied"} ${copied} files; ${skippedExisting} already present`);
