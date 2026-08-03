const fs = require("fs");
const path = require("path");

const configPath = path.resolve(__dirname, "../src/main/java/alku/csrp/Config.java");
const source = fs.readFileSync(configPath, "utf8");
const failures = [];

if (!source.includes('defineInList("overlastHudPosition", "top left", Arrays.asList(')) {
  failures.push("overlastHudPosition must use a null-tolerant allowed-values list");
}

const unsafeDefineInList = /defineInList\([\s\S]{0,160}?List\.of\(/g;
if (unsafeDefineInList.test(source)) {
  failures.push("List.of must not be passed to defineInList because NeoForge probes missing values with null");
}

if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}

console.log("Config migration verification passed.");
