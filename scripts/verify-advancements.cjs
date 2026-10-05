const fs = require("node:fs");
const path = require("node:path");

// Guards two corrections made after the fact:
//
//  1. an invented feature ("parasites assimilate nearby dragon eggs when they attack", with its own
//     entity, renderer, advancement csrp:again and language keys) was removed — 1.10.9 and 1.10.8 have
//     no dragon-egg mechanic at all, so it violated the "do not add gameplay the original never had"
//     rule;
//  2. a stale duplicate advancement tree under data/csrp/advancement/csrp/ was removed — those 39 files
//     load as csrp:csrp/* and 24 of them do not exist in the original under any name.
//
// The advancement set is pinned to the original's 40 ids so neither mistake can come back.
const root = path.resolve(__dirname, "..");
const failures = [];

function read(relativePath) {
    const file = path.join(root, relativePath);
    if (!fs.existsSync(file)) {
        failures.push(`missing ${relativePath}`);
        return "";
    }
    return fs.readFileSync(file, "utf8");
}

function walk(dir, out = []) {
    if (!fs.existsSync(dir)) return out;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const p = path.join(dir, entry.name);
        if (entry.isDirectory()) walk(p, out);
        else out.push(p);
    }
    return out;
}

// --- 1. the invented dragon-egg feature is gone ------------------------------------------------------
for (const relative of ["src/main/java/alku/csrp/entity/DragonEggAssimilationEntity.java",
    "src/main/java/alku/csrp/client/renderer/DragonEggAssimilationRenderer.java",
    "src/main/resources/data/csrp/advancement/again.json"]) {
    if (fs.existsSync(path.join(root, relative))) {
        failures.push(`${relative} is back; it belongs to an invented feature the original never had`);
    }
}
for (const file of walk(path.join(root, "src/main/java"))) {
    const text = fs.readFileSync(file, "utf8");
    if (/DragonEggAssimilation|assimilateDragonEggs/.test(text)) {
        failures.push(`${path.relative(root, file).replace(/\\/g, "/")} references the removed dragon-egg feature`);
    }
}
for (const name of fs.readdirSync(path.join(root, "src/main/resources/assets/csrp/lang"))) {
    if (!name.endsWith(".json")) continue;
    const json = JSON.parse(read("src/main/resources/assets/csrp/lang/" + name));
    for (const key of ["entity.csrp.dragon_egg_assimilation", "advancements.csrp.again.title",
        "advancements.csrp.again.description"]) {
        if (json[key] !== undefined) failures.push(`${name} still has the invented lang key ${key}`);
    }
}

// --- 2. the advancement set is exactly the original's 40 --------------------------------------------
const ORIGINAL_ADVANCEMENTS = ["brew_fear", "columbus", "controversial", "cosmic_structural_failure",
    "cut_roots", "dark_days", "enemy_enemy", "fermented_arrogance", "firmamentum_root", "fog_nullifier",
    "guerilla", "hellfire_chemical_warfare", "hunt_season", "kill_adapted", "kill_assimilated",
    "kill_assimilated_head", "kill_beckon_s1", "kill_beckon_s2", "kill_beckon_s3", "kill_beckon_s4",
    "kill_crude", "kill_deterrent", "kill_dispatcher_s1", "kill_dispatcher_s2", "kill_dispatcher_s3",
    "kill_dispatcher_s4", "kill_inborn", "kill_primitive", "kill_pure", "kill_root", "monument",
    "monument_root", "peacekeeper", "qliphoth_overload", "root", "secret_root", "sepeku", "stolas",
    "thornshade_self_destruct", "tricked_me_did_you"];

const advancementDir = path.join(root, "src/main/resources/data/csrp/advancement");
const present = fs.readdirSync(advancementDir, { withFileTypes: true })
    .filter((e) => e.isFile() && e.name.endsWith(".json"))
    .map((e) => e.name.replace(/\.json$/, "")).sort();
const expected = [...ORIGINAL_ADVANCEMENTS].sort();

if (present.length !== expected.length) {
    failures.push(`the data pack has ${present.length} advancements, the original has ${expected.length}`);
}
for (const id of expected) {
    if (!present.includes(id)) failures.push(`the original advancement ${id} is missing`);
}
for (const id of present) {
    if (!expected.includes(id)) failures.push(`${id} is not an advancement of the original`);
}
// A nested namespace directory would register as csrp:csrp/<name> — duplicates with wrong ids.
if (fs.existsSync(path.join(advancementDir, "csrp"))) {
    failures.push("data/csrp/advancement/csrp/ is back; its files load as csrp:csrp/* duplicates");
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log(`advancement + invented-content contract ok (${expected.length} advancements)`);
