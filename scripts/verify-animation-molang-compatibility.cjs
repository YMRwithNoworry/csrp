const path = require("path");
const {spawnSync} = require("child_process");

const converter = path.join(__dirname, "rewrite-animation-comparison-ternaries.cjs");
const result = spawnSync(process.execPath, [converter, "--check"], {encoding: "utf8"});
if (result.status !== 0) {
  if (result.stdout) process.stderr.write(result.stdout);
  if (result.stderr) process.stderr.write(result.stderr);
  process.exit(result.status ?? 1);
}

console.log("Extracted animation Molang expressions are GeckoLib-compatible.");
