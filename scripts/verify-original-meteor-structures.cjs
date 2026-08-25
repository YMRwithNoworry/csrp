const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const zlib = require("node:zlib");

const projectRoot = path.resolve(__dirname, "..");
const defaultSourceRoot = path.join(projectRoot, ".firecrawl/srp-jar/assets/srparasites/structures");
const extractedSourceRoot = path.resolve(
    "D:/code/模组反编译器/decompiled/[逃逸：寄生体] SRParasites-1.10.8/assets/srparasites/structures");
const sourceRoot = fs.existsSync(defaultSourceRoot) ? defaultSourceRoot
    : fs.existsSync(extractedSourceRoot) ? extractedSourceRoot : null;
const targetRoot = path.join(projectRoot, "src/main/resources/data/csrp/structures");
const expectedHashes = {
    meteor: "c51a5fa726644949663ed4cc64025420ddb52bf767b8553e7d7d529467b7eca9",
    meteor_fragment_large1: "3b544e4331772cf3f7ad07f9138db491e7371f3042fc13850327d260ebc2fd53",
    meteor_fragment_large2: "9bab951b6171346fd8780db903dfa1975a4534f41328e8f42b326d66629e0aa1",
    meteor_fragment_large3: "6158953cd7774f4ec62619732218f9f55d3d72d91c427345bcaaa3d35c12d425",
    meteor_fragment_small1: "9def583c759808265d4cf5fbc415a97e73d8723c0ff5a437731581f108684071",
    meteor_fragment_small2: "1dae9f18d4622aeae76dcfb6611fe565f11f829b2cdb401bd2a82d22a85b5a6a",
    meteor_fragment_small3: "3d97677d4fcb30e8bbb3e6ea3b143087ff117f7cd99b2ca76ebea3797a6b239f",
    meteor_fragment_small4: "aa2798a23ad38d2c1fec60bf97bb35fcb8ba81d7ce441f71ab11376aff12058b",
    meteor_fragment_small5: "b4c11d95a83c1e22bfa774336dde6dd280fff5651ae5315ec791147b3871472f",
    meteor_fragment_small6: "abd829e1aecbd81bec738b0b625cf2f233376333679a9ba1b78cfd711410de39",
};
const expectedAssets = {
    "textures/entity/projectile/meteor.png": "18eb2b3760c5605d4122536ce1593ec9aa5819039fa6107ca8b5dd99f44d1dff",
    "textures/gui/worldsettings/meteor_orbit.png": "174465ed8312324fb69076d5cc6399052d01dad312c4049545f8c207a0508c6f",
    "sounds/misc/meteor.ogg": "62ff0ac189e1bf9594951ced048ab388ce71dc88d0dd3aaa271cb0fd5616fa2b",
    "textures/celestial/meteors/meteor_group1.png": "9ad0f41d31eee346ca529908af3500f78987538b24e3b38c1eaed8348917d0c7",
    "textures/celestial/meteors/meteor_group2.png": "174465ed8312324fb69076d5cc6399052d01dad312c4049545f8c207a0508c6f",
    "textures/celestial/meteors/meteor_group3.png": "43787c7715b07bed41aaf687675c54b21fa87344ce73366b63ba6f58293af108",
    "textures/celestial/meteors/meteor_large1.png": "24cb5f50759d9eff9705d1dbd1b45d7e3bb83bda8d067a2a03dee371433fda28",
    "textures/celestial/meteors/meteor_small1.png": "20b3de5424ec8f5ada9208e851c82d0bb03bc410f4c6f569c46dcd2e4cc89b06",
    "textures/celestial/meteors/meteor_small2.png": "58838cdb68c0f67906473b35d286a2bab49812d3a75acdf0e70ac8f4b49c49d8",
};
const names = Object.keys(expectedHashes);
const failures = [];

function sha256(file) {
    return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

function paletteNames(file) {
    const uncompressed = zlib.gunzipSync(fs.readFileSync(file)).toString("latin1");
    return [...new Set(uncompressed.match(/(?:minecraft|srparasites):[a-z0-9_]+/g) ?? [])].sort();
}

for (const name of names) {
    const target = path.join(targetRoot, `${name}.nbt`);
    if (!fs.existsSync(target)) {
        failures.push(`missing imported structure: ${name}.nbt`);
        continue;
    }
    if (sha256(target) !== expectedHashes[name]) {
        failures.push(`imported structure differs from original hash: ${name}.nbt`);
    }
    if (sourceRoot) {
        const source = path.join(sourceRoot, `${name}.nbt`);
        if (!fs.existsSync(source) || sha256(source) !== expectedHashes[name]) {
            failures.push(`local original structure differs from recorded hash: ${name}.nbt`);
        }
    }
    if (process.argv.includes("--inspect")) {
        console.log(`${name}: ${paletteNames(target).join(", ")}`);
    }
}

for (const [relativePath, expectedHash] of Object.entries(expectedAssets)) {
    const target = path.join(projectRoot, "src/main/resources/assets/csrp", relativePath);
    if (!fs.existsSync(target)) {
        failures.push(`missing imported meteor asset: ${relativePath}`);
        continue;
    }
    if (sha256(target) !== expectedHash) {
        failures.push(`imported meteor asset differs from original hash: ${relativePath}`);
    }
    if (sourceRoot) {
        const source = path.join(path.dirname(sourceRoot), relativePath);
        if (!fs.existsSync(source) || sha256(source) !== expectedHash) {
            failures.push(`local original meteor asset differs from recorded hash: ${relativePath}`);
        }
    }
}

if (failures.length) {
    console.error("Original meteor structure verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log(`Verified ${names.length} structures and ${Object.keys(expectedAssets).length} meteor assets by SHA-256.`);
