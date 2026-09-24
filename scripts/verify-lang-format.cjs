#!/usr/bin/env node
"use strict";

/**
 * scripts/verify-lang-format.cjs
 *
 * Structural contract for `src/main/resources/assets/csrp/lang`:
 *   1. exactly the 33 converted locales exist, and no stale `.lang` survives;
 *   2. every file is UTF-8 without BOM and uses one consistent line ending;
 *   3. every file parses as a JSON object of strings, keys sorted;
 *   4. no mojibake `Â§` and no lone surrogates (§ colour codes stay intact);
 *   5. no vanilla key is redefined (that would overwrite vanilla strings);
 *   6. values that are empty in the JSON are empty in the 1.10.9 source too;
 *   7. `_pending/*.json` (teammate append-only files) are well formed.
 */

const fs = require("fs");
const path = require("path");

const converter = require("./convert-lang-109.cjs");

const root = path.resolve(__dirname, "..");
const langDir = path.join(root, "src/main/resources/assets/csrp/lang");
const pendingDir = path.join(langDir, "_pending");

const failures = [];
const fail = (message) => failures.push(message);

/** The 33 locales of the 1.10.9 lang set, after `lv_LV` → `lv_lv`. */
const EXPECTED_LOCALES = [
    "de_at", "de_ch", "de_de", "en_pt", "en_us", "en_ws",
    "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve",
    "fr_ca", "fr_fr", "hr_hr", "it_it", "ja_jp", "ja_jp21", "ko_kr",
    "lol_us", "lv_lv", "nl_nl", "pl_pl", "pt_br", "ro_ro", "ru_ru",
    "sv_se", "tr_tr", "uk_ua", "zh_cn", "zh_tw", "zh_tw21"
];

const LONE_SURROGATE = /(?:[\uD800-\uDBFF](?![\uDC00-\uDFFF]))|(?:(?<![\uD800-\uDBFF])[\uDC00-\uDFFF])/;

/** Keys that belong to the vanilla client jar; the mod must not redefine them. */
const VANILLA_KEY = /^(subtitles\.(block|entity)\.generic\.|block\.minecraft\.|item\.minecraft\.(?!potion|splash_potion|lingering_potion|tipped_arrow))/i;

if (!fs.existsSync(langDir)) {
    console.error(`missing lang directory: ${path.relative(root, langDir)}`);
    process.exit(1);
}

/* 1. file set ------------------------------------------------------------- */

const entries = fs.readdirSync(langDir, { withFileTypes: true });
const fileNames = entries.filter((e) => e.isFile()).map((e) => e.name).sort();

for (const name of fileNames.filter((n) => n.endsWith(".lang"))) {
    fail(`stale 1.12.2 lang file must not exist in 26.3: ${name}`);
}

const jsonNames = fileNames.filter((n) => n.endsWith(".json"));
const locales = jsonNames.map((n) => n.slice(0, -".json".length));
for (const locale of EXPECTED_LOCALES) {
    if (!locales.includes(locale)) fail(`missing language file: ${locale}.json`);
}
for (const locale of locales) {
    if (!EXPECTED_LOCALES.includes(locale)) fail(`unexpected language file: ${locale}.json`);
}
if (locales.includes("lv_LV")) fail("lv_LV.json must be renamed to lv_lv.json (26.3 locale codes are lowercase)");

/* 2./3./4. per-file structural checks ------------------------------------ */

const parsed = new Map();
for (const name of jsonNames) {
    const file = path.join(langDir, name);
    const raw = fs.readFileSync(file, "utf8");

    if (raw.charCodeAt(0) === 0xfeff) fail(`${name}: must not start with a UTF-8 BOM`);
    // The converter writes LF. The repository uses core.autocrlf=true, so a
    // fresh checkout may legitimately present CRLF; what must never happen is a
    // mix, or a missing/duplicated trailing newline.
    const usesCrLf = raw.includes("\r\n");
    if (raw.includes("\r") && !usesCrLf) fail(`${name}: contains a bare CR`);
    if (usesCrLf && /(?<!\r)\n/.test(raw)) fail(`${name}: mixes CRLF and LF line endings`);
    const newline = usesCrLf ? "\r\n" : "\n";
    if (!raw.endsWith(newline)) fail(`${name}: must end with a single trailing newline`);
    if (raw.endsWith(newline + newline)) fail(`${name}: must not end with a blank line`);

    let object;
    try {
        object = JSON.parse(raw);
    } catch (error) {
        fail(`${name}: invalid JSON (${error.message})`);
        continue;
    }
    if (object === null || typeof object !== "object" || Array.isArray(object)) {
        fail(`${name}: top level must be a JSON object`);
        continue;
    }

    const keys = Object.keys(object);
    if (keys.length === 0) fail(`${name}: contains no keys`);

    const sorted = [...keys].sort();
    for (let index = 0; index < keys.length; index++) {
        if (keys[index] !== sorted[index]) {
            fail(`${name}: keys are not sorted (${keys[index]} should be ${sorted[index]})`);
            break;
        }
    }

    for (const key of keys) {
        const value = object[key];
        if (typeof value !== "string") {
            fail(`${name}: value for "${key}" is not a string`);
            continue;
        }
        if (value.includes("\u00c2\u00a7")) fail(`${name}: mojibake "Â§" in "${key}"`);
        if (LONE_SURROGATE.test(value)) fail(`${name}: lone surrogate in value of "${key}"`);
        if (LONE_SURROGATE.test(key)) fail(`${name}: lone surrogate in key "${key}"`);
        if (VANILLA_KEY.test(key)) fail(`${name}: redefines the vanilla key "${key}"`);
    }

    parsed.set(name.slice(0, -".json".length), object);
}

/* 6. empty values must be empty in the 1.10.9 source too ------------------ */

// The original mod ships a handful of intentionally blank entries
// (`tootip.srparasites.item.7=`), which are carried over verbatim. Anything
// else being empty would mean a lost translation.
const sourceEmptyKeys = new Map();
if (fs.existsSync(converter.SOURCE_DIR)) {
    for (const locale of EXPECTED_LOCALES) {
        const sourceFile = path.join(converter.SOURCE_DIR, locale + ".lang");
        if (!fs.existsSync(sourceFile)) continue;
        const { map } = converter.parseLangFile(sourceFile);
        const empties = new Set();
        for (const [key, value] of map) {
            if (value.length === 0) empties.add(converter.mapKey(key).key);
        }
        sourceEmptyKeys.set(locale, empties);
    }
}

for (const [locale, object] of parsed) {
    const emptyKeys = Object.keys(object).filter((k) => object[k].length === 0);
    if (emptyKeys.length === 0) continue;
    const expected = sourceEmptyKeys.get(locale);
    if (expected) {
        for (const key of emptyKeys) {
            if (!expected.has(key)) {
                fail(`${locale}.json: "${key}" is empty but the 1.10.9 source is not`);
            }
        }
    } else if (emptyKeys.length > 12) {
        fail(`${locale}.json: ${emptyKeys.length} empty values (expected at most 12)`);
    }
}

/* 5. § colour codes must survive the .lang → JSON round trip -------------- */

for (const locale of ["en_us", "zh_cn", "ru_ru", "ja_jp"]) {
    const object = parsed.get(locale);
    if (!object) continue;
    const withSection = Object.values(object).filter((v) => v.includes("\u00a7")).length;
    if (withSection < 100) {
        fail(`${locale}.json: only ${withSection} values keep a § colour code (expected >= 100)`);
    }
}

const english = parsed.get("en_us");
if (english) {
    // A key that only exists in the 1.10.9 lang, so its § codes come straight
    // from the migration rather than from the pre-existing curated entries.
    const migrated = english["tootip.csrp.lurecomp.1"];
    if (typeof migrated !== "string" || !migrated.includes("\u00a7")) {
        fail(`en_us.json: "tootip.csrp.lurecomp.1" lost its § colour codes (${JSON.stringify(migrated)})`);
    }
    const legacyAlias = english["tootip.srparasites.lurecomp.1"];
    if (legacyAlias !== migrated) {
        fail("en_us.json: legacy alias \"tootip.srparasites.lurecomp.1\" must equal \"tootip.csrp.lurecomp.1\"");
    }
}

/* 6. teammate append-only pending files ---------------------------------- */

if (fs.existsSync(pendingDir)) {
    for (const name of fs.readdirSync(pendingDir).filter((n) => n.endsWith(".json")).sort()) {
        const raw = fs.readFileSync(path.join(pendingDir, name), "utf8");
        let object;
        try {
            object = JSON.parse(raw);
        } catch (error) {
            fail(`_pending/${name}: invalid JSON (${error.message})`);
            continue;
        }
        if (object === null || typeof object !== "object" || Array.isArray(object)) {
            fail(`_pending/${name}: top level must be a JSON object`);
            continue;
        }
        for (const [key, value] of Object.entries(object)) {
            if (typeof value !== "string" || value.length === 0) {
                fail(`_pending/${name}: value for "${key}" must be a non-empty string`);
            }
        }
    }
}

/* report ----------------------------------------------------------------- */

if (failures.length) {
    console.error(`verify-lang-format: ${failures.length} failure(s)`);
    for (const failure of failures) console.error(`  - ${failure}`);
    process.exit(1);
}

console.log(`verify-lang-format: ok (${jsonNames.length} locales, ${[...parsed.values()].reduce((n, o) => n + Object.keys(o).length, 0)} keys)`);
