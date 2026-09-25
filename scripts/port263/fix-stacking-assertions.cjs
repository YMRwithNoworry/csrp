// Removes the top-level expect(...) calls whose message says "is not stacked": those asserted the
// entity-side stacking that turned out to double-apply the multiplier. Splices whole call blocks
// only (a top-level `expect(` up to the line ending with `);`) and validates the result parses.
const fs = require("node:fs");
const path = require("node:path");

const file = path.join(__dirname, "..", "verify-parasite-combat-rules.cjs");
const original = fs.readFileSync(file, "utf8");
const lines = original.split("\n");

const output = [];
let removed = 0;
for (let i = 0; i < lines.length; i++) {
  if (/^expect\(/.test(lines[i])) {
    let end = i;
    while (end < lines.length - 1 && !lines[end].trimEnd().endsWith(");")) end++;
    const block = lines.slice(i, end + 1);
    if (block.join("\n").includes("is not stacked")) {
      removed++;
      i = end;
      continue;
    }
    output.push(...block);
    i = end;
    continue;
  }
  output.push(lines[i]);
}

fs.writeFileSync(file, output.join("\n"));
try {
  new Function(fs.readFileSync(file, "utf8"));
  console.log(`removed ${removed} stacked-form assertion(s); file parses`);
} catch (error) {
  fs.writeFileSync(file, original);
  console.log(`reverted: output did not parse (${error.message})`);
  process.exit(1);
}
