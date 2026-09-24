#!/usr/bin/env node
"use strict";

/**
 * scripts/verify-lang-parity.cjs
 *
 * Asserts that the 26.3 language files actually cover the 1.10.9 source and the
 * keys the ported code asks for:
 *   1. every locale's `.json` contains every key its own `.lang` maps onto;
 *   2. `en_us` covers all 2332 `en_us.lang` keys, `zh_cn` covers all 2140
 *      `zh_cn.lang` keys *and* all `en_us.lang` keys (100% both ways);
 *   3. a deterministic pseudo-random sample of source keys resolves;
 *   4. an embedded mapping table (one case per rule) always holds, so the check
 *      still means something when the read-only 1.10.9 extraction is absent;
 *   5. every translation key referenced by Java / data / sounds.json exists;
 *   6. no vanilla key is redefined;
 *   7. teammate `_pending/*.json` keys made it into `en_us.json`.
 */

const fs = require("fs");
const path = require("path");

const converter = require("./convert-lang-109.cjs");

const root = path.resolve(__dirname, "..");
const langDir = path.join(root, "src/main/resources/assets/csrp/lang");
const pendingDir = path.join(langDir, "_pending");

const failures = [];
const fail = (message) => failures.push(message);

/** 1.10.9 key counts, recorded from `out109/assets/srparasites/lang/*.lang`. */
const SOURCE_KEY_COUNTS = { en_us: 2332, zh_cn: 2140 };

/** Minimum final key counts; a regression below these means keys were lost. */
const MIN_KEYS = { en_us: 2900, zh_cn: 2900 };

/**
 * One case per mapping rule. These hold whether or not the read-only 1.10.9
 * extraction is available, and they are what the 26.3 keys are validated
 * against on a machine that only has this repository.
 */
const EMBEDDED_MAPPING_CASES = [
    ["tile.srparasites.esca_bulb.name", "block.csrp.esca_bulb"],
    ["tile.node_lamp_off.name", "block.csrp.node_lamp_off"],
    ["item.srparasites.shrimp.name", "item.csrp.shrimp"],
    ["item.srparasites.book_of_vengeance.vengeance0", "item.csrp.book_of_vengeance.vengeance0"],
    ["item.srparasites.lurecomponent1.name", "item.csrp.lurecomponent1"],
    ["entity.srparasites.buglin.name", "entity.csrp.buglin"],
    ["entity.srparasites.nametag.ricardo", "entity.csrp.nametag.ricardo"],
    ["mob_effect.srparasites:coth", "effect.csrp.coth"],
    ["effect.srparasites.the_sign", "effect.csrp.the_sign"],
    ["advancements.srparasites.root.title", "advancements.csrp.root.title"],
    ["advancements.srparasites.root.description", "advancements.csrp.root.description"],
    ["advancement.srparasites.sepeku.title", "advancements.csrp.sepeku.title"],
    ["advancement.srparasites.sepeku.desc", "advancements.csrp.sepeku.description"],
    ["block.srparasites.relay_controller.no_space", "block.csrp.relay_controller.no_space"],
    ["potion.effect.srparasites:coth", "item.minecraft.potion.effect.coth"],
    ["splash_potion.effect.distorted_enlightenment", "item.minecraft.splash_potion.effect.distorted_enlightenment"],
    ["lingering_potion.effect.srparasites:bleed", "item.minecraft.lingering_potion.effect.bleed"],
    ["tipped_arrow.effect.srparasites:rage", "item.minecraft.tipped_arrow.effect.rage"],
    ["srparasites.dislodgement.header", "csrp.dislodgement.header"],
    ["gui.srparasites.escape_button", "gui.csrp.escape_button"],
    ["tooltip.srparasites.shrimp.desc", "tooltip.csrp.shrimp.desc"],
    ["tootip.srparasites.lurecomp.1", "tootip.csrp.lurecomp.1"],
    ["lore.srparasites.buglin", "lore.csrp.buglin"],
    ["message.srparasites.diffuser.started", "message.csrp.diffuser.started"],
    ["commands.srparasites.bestiary_stats.usage", "commands.csrp.bestiary_stats.usage"],
    ["death.attack.srparasites.ricardo", "death.attack.csrp.ricardo"],
    ["chat.srparasites.relay.not_formed", "chat.csrp.relay.not_formed"],
    ["profile.srparasites.inborn", "profile.csrp.inborn"],
    ["tier.srparasites.inborn", "tier.csrp.inborn"],
    ["srphelp.srpevolution.getphase.usage", "srphelp.srpevolution.getphase.usage"],
    ["command.srpevolution.prefix", "command.srpevolution.prefix"],
    ["bestiary.progress.title", "bestiary.progress.title"],
    ["subtitles.kirin.blackhole", "subtitles.kirin.blackhole"],
    ["item.record.discone.desc", "item.record.discone.desc"],
    ["block.srparasites.relay_controller.no_space", "block.csrp.relay_controller.no_space"]
];

/** Keys the ported Java still spells with the 1.12.2 namespace. */
const LEGACY_ALIAS_KEYS = [
    "tootip.srparasites.lurecomp.1",
    "tootip.srparasites.lurecomp.6"
];

const readLang = (locale) => {
    const file = path.join(langDir, locale + ".json");
    if (!fs.existsSync(file)) return null;
    try {
        return JSON.parse(fs.readFileSync(file, "utf8"));
    } catch {
        return null;
    }
};

const locales = fs
    .readdirSync(langDir, { withFileTypes: true })
    .filter((e) => e.isFile() && e.name.endsWith(".json"))
    .map((e) => e.name.slice(0, -".json".length))
    .sort();

const english = readLang("en_us");
const chinese = readLang("zh_cn");
if (!english) fail("en_us.json is missing or not valid JSON");
if (!chinese) fail("zh_cn.json is missing or not valid JSON");

/* 1./2. coverage --------------------------------------------------------- */

const sourceAvailable = fs.existsSync(converter.SOURCE_DIR);
const mappedByLocale = new Map();

if (sourceAvailable) {
    const sourceLocales = fs
        .readdirSync(converter.SOURCE_DIR)
        .filter((n) => n.endsWith(".lang"))
        .map((n) => n.slice(0, -".lang".length))
        .sort();

    for (const locale of sourceLocales) {
        const outLocale = converter.targetLocaleName(locale);
        const { map } = converter.parseLangFile(path.join(converter.SOURCE_DIR, locale + ".lang"));
        const { mapped } = converter.mapLocale(map);
        mappedByLocale.set(outLocale, mapped);

        const declared = SOURCE_KEY_COUNTS[outLocale];
        if (declared !== undefined && map.size !== declared) {
            fail(`${outLocale}.lang: expected ${declared} unique keys, found ${map.size}`);
        }

        const target = readLang(outLocale);
        if (!target) {
            fail(`${outLocale}.json is missing`);
            continue;
        }
        const missing = [...mapped.keys()].filter((key) => !(key in target));
        if (missing.length) {
            fail(`${outLocale}.json: ${missing.length} mapped key(s) missing, e.g. ${missing.slice(0, 5).join(", ")}`);
        }
    }

    // zh_cn must also cover every en_us.lang key (English fallback for the 200
    // keys 1.10.9 only shipped in English).
    const enMapped = mappedByLocale.get("en_us");
    if (enMapped && chinese) {
        const missing = [...enMapped.keys()].filter((key) => !(key in chinese));
        if (missing.length) {
            fail(`zh_cn.json: ${missing.length} en_us.lang key(s) not covered, e.g. ${missing.slice(0, 5).join(", ")}`);
        }
    }

    // 3. deterministic sample: 40 source keys through a fixed LCG
    if (enMapped) {
        const keys = [...enMapped.keys()].sort();
        let state = 0x2f6e2b1;
        const next = () => (state = (state * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
        let sampled = 0;
        for (let index = 0; index < 40; index++) {
            const key = keys[Math.floor(next() * keys.length)];
            sampled++;
            if (!(key in english)) fail(`en_us.json: sampled key "${key}" is missing`);
            if (chinese && !(key in chinese)) fail(`zh_cn.json: sampled key "${key}" is missing`);
        }
        if (sampled !== 40) fail(`sampling produced ${sampled} keys instead of 40`);
    }
} else {
    console.log(`verify-lang-parity: 1.10.9 source not present (${converter.SOURCE_DIR}); using embedded expectations`);
}

/* 4. embedded mapping table (always) ------------------------------------- */

if (english) {
    for (const [sourceKey, expectedKey] of EMBEDDED_MAPPING_CASES) {
        const mapped = converter.mapKey(sourceKey);
        if (mapped.key !== expectedKey) {
            fail(`mapping rule drift: ${sourceKey} → ${mapped.key} (expected ${expectedKey})`);
        }
        if (!(expectedKey in english)) {
            fail(`en_us.json: expected key "${expectedKey}" (from ${sourceKey}) is missing`);
        }
        if (chinese && !(expectedKey in chinese)) {
            fail(`zh_cn.json: expected key "${expectedKey}" (from ${sourceKey}) is missing`);
        }
    }
    for (const key of LEGACY_ALIAS_KEYS) {
        if (!(key in english)) fail(`en_us.json: legacy alias "${key}" is missing`);
    }
}

/* 5. keys the ported code / data / resources reference ------------------- */

const required = converter.collectRequiredKeys();
if (english) {
    const missing = [];
    for (const key of required.keys.keys()) {
        if (!(key in english)) missing.push(key);
    }
    if (missing.length) {
        fail(`en_us.json: ${missing.length} key(s) referenced by code/data/sounds are missing, e.g. ${missing.slice(0, 5).join(", ")}`);
    }
    for (const prefix of required.prefixes.keys()) {
        const hits = Object.keys(english).filter((k) => k.startsWith(prefix));
        if (hits.length === 0) {
            fail(`en_us.json: runtime prefix "${prefix}" has no concrete key`);
        }
    }
}

/* 6. no vanilla key redefined -------------------------------------------- */

for (const locale of locales) {
    const object = readLang(locale);
    if (!object) continue;
    for (const key of Object.keys(object)) {
        if (converter.isVanillaKey(key)) fail(`${locale}.json: redefines the vanilla key "${key}"`);
    }
}

/* 7. teammate pending keys merged ---------------------------------------- */

if (fs.existsSync(pendingDir)) {
    for (const name of fs.readdirSync(pendingDir).filter((n) => n.endsWith(".json")).sort()) {
        let object;
        try {
            object = JSON.parse(fs.readFileSync(path.join(pendingDir, name), "utf8"));
        } catch (error) {
            fail(`_pending/${name}: invalid JSON (${error.message})`);
            continue;
        }
        for (const key of Object.keys(object)) {
            if (english && !(key in english)) fail(`_pending/${name}: "${key}" was not merged into en_us.json`);
        }
    }
}

/* 8. key-count floors ---------------------------------------------------- */

for (const [locale, minimum] of Object.entries(MIN_KEYS)) {
    const object = readLang(locale);
    if (!object) continue;
    const count = Object.keys(object).length;
    if (count < minimum) fail(`${locale}.json: ${count} keys, expected at least ${minimum}`);
}

/* report ----------------------------------------------------------------- */

if (failures.length) {
    console.error(`verify-lang-parity: ${failures.length} failure(s)`);
    for (const failure of failures) console.error(`  - ${failure}`);
    process.exit(1);
}

const detail = sourceAvailable
    ? `en_us=${Object.keys(english).length}, zh_cn=${Object.keys(chinese).length}, locales=${locales.length}, source=${converter.SOURCE_DIR}`
    : `en_us=${Object.keys(english).length}, zh_cn=${Object.keys(chinese).length}, locales=${locales.length}`;
console.log(`verify-lang-parity: ok (${detail})`);
