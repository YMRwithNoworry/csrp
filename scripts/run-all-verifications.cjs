const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const scriptsDirectory = __dirname;
const scripts = fs.readdirSync(scriptsDirectory)
  .filter((name) => name.startsWith("verify-") && name.endsWith(".cjs"))
  .sort();
const failures = [];

for (const script of scripts) {
  const result = spawnSync(process.execPath, [path.join(scriptsDirectory, script)], {
    encoding: "utf8"
  });
  if (result.status !== 0) {
    failures.push({
      script,
      stdout: result.stdout.trim(),
      stderr: result.stderr.trim()
    });
  }
}

console.log(JSON.stringify({
  total: scripts.length,
  passed: scripts.length - failures.length,
  failed: failures.length
}));

if (failures.length) {
  for (const failure of failures) {
    console.error(`\n${failure.script}`);
    if (failure.stdout) console.error(failure.stdout);
    if (failure.stderr) console.error(failure.stderr);
  }
  process.exit(1);
}
