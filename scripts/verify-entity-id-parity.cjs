const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];

const ORIGINAL_ENTITIES =
    "D:/code/MC模组/_scratch/vf/out109/com/dhanantry/scapeandrunparasites/init/SRPEntities.java";
const PROJECT_ENTITIES = path.join(root, "src/main/java/alku/csrp/registry/ModEntities.java");

/** Ids the original 1.10.9 jar registers, used as the fallback when the decompiled tree is absent. */
const EXPECTED_ORIGINAL_COUNT = 158;

function read(file) {
    if (!fs.existsSync(file)) return null;
    return fs.readFileSync(file, "utf8");
}

function projectIds(source) {
    const ids = new Set();
    let match;
    const patterns = [
        /(?:monster|projectile|misc|entity|register)\s*\(\s*"([a-z0-9_]+)"/g,
        /entityType\s*\(\s*"([a-z0-9_]+)"/g
    ];
    for (const pattern of patterns) {
        while ((match = pattern.exec(source))) ids.add(match[1]);
    }
    return ids;
}

function originalIds(source) {
    const ids = new Set();
    let match;
    const pattern = /CreateEntity(?:Mob|Projectile|Other)?\s*\(\s*"([a-z0-9_]+)"/g;
    while ((match = pattern.exec(source))) ids.add(match[1]);
    return ids;
}

const projectSource = read(PROJECT_ENTITIES);
if (projectSource === null) {
    failures.push("missing src/main/java/alku/csrp/registry/ModEntities.java");
}

const ported = projectSource === null ? new Set() : projectIds(projectSource);

// The port must cover every id the original registers; extra ids are allowed and are the port's own
// helper entities (projectile splits, damage carriers, ...).
const originalSource = read(ORIGINAL_ENTITIES);
if (originalSource !== null) {
    const original = originalIds(originalSource);
    if (original.size !== EXPECTED_ORIGINAL_COUNT) {
        failures.push(`the decompiled original now registers ${original.size} ids, expected ${EXPECTED_ORIGINAL_COUNT}`);
    }
    const missing = [...original].filter((id) => !ported.has(id)).sort();
    if (missing.length > 0) {
        failures.push(`entity ids missing from the port (${missing.length}): ${missing.join(", ")}`);
    }
} else {
    // Without the reference tree, still guard the total so a regression that deletes ids is caught.
    if (ported.size < EXPECTED_ORIGINAL_COUNT) {
        failures.push(`the port registers only ${ported.size} entity ids, expected at least ${EXPECTED_ORIGINAL_COUNT}`);
    }
}

// Spot-check ids whose absence would be easy to miss (one per family).
const SPOT_CHECKS = [
    "rupter", "buglin", "mangler", "draconite", "kirin", "heblu_light",
    "pri_longarms", "ada_arachnida", "sim_human", "hi_golem",
    "anc_overlord", "abo_head", "biomass", "scent", "parasite_projectile"
];
for (const id of SPOT_CHECKS) {
    if (!ported.has(id)) failures.push(`entity id "${id}" is not registered`);
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log(`entity id parity ok (${ported.size} registered ids)`);
