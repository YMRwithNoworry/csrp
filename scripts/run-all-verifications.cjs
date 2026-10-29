const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const scriptsDirectory = __dirname;
const scripts = fs.readdirSync(scriptsDirectory)
  .filter((name) => name.startsWith("verify-") && name.endsWith(".cjs"))
  .sort();
const failures = [];
const skipped = [];

for (const script of scripts) {
  const result = spawnSync(process.execPath, [path.join(scriptsDirectory, script)], {
    encoding: "utf8"
  });
  const entry = {
    script,
    stdout: result.stdout.trim(),
    stderr: result.stderr.trim()
  };
  if (result.status === 77) {
    skipped.push(entry);
  } else if (result.status !== 0) {
    failures.push(entry);
  }
}

console.log(JSON.stringify({
  total: scripts.length,
  passed: scripts.length - failures.length - skipped.length,
  skipped: skipped.length,
  failed: failures.length
}));

for (const entry of skipped) {
  console.log(`\n${entry.script}`);
  if (entry.stdout) console.log(entry.stdout);
  if (entry.stderr) console.log(entry.stderr);
}

if (failures.length) {
  for (const failure of failures) {
    console.error(`\n${failure.script}`);
    if (failure.stdout) console.error(failure.stdout);
    if (failure.stderr) console.error(failure.stderr);
  }
  process.exit(1);
}
