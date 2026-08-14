const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];

function collectJava(directory, files = []) {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
        const fullPath = path.join(directory, entry.name);
        if (entry.isDirectory()) collectJava(fullPath, files);
        else if (entry.isFile() && entry.name.endsWith(".java")) files.push(fullPath);
    }
    return files;
}

const sourceRoot = path.join(root, "src", "main", "java");
const hiddenCoth = /new\s+MobEffectInstance\(\s*[A-Za-z0-9_.]*COTH\s*,[^;]*?false\s*,\s*false\s*\)/g;
const files = collectJava(sourceRoot);
for (const file of files) {
    const source = fs.readFileSync(file, "utf8");
    if (hiddenCoth.test(source)) {
        failures.push(path.relative(root, file));
    }
    hiddenCoth.lastIndex = 0;
}

const infection = fs.readFileSync(
    path.join(sourceRoot, "alku", "csrp", "infection", "InfectionMechanics.java"), "utf8");
if (!/MobEffectInstance\(ModMobEffects\.COTH,[\s\S]*?(?:visible|mergedVisible), true\)/.test(infection)) {
    failures.push("InfectionMechanics.java: shared COTH helper does not force the status icon visible");
}

if (failures.length) {
    console.error("COTH visibility verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("COTH visibility verification passed.");
