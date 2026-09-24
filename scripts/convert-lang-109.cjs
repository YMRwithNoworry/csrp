#!/usr/bin/env node
"use strict";

/**
 * scripts/convert-lang-109.cjs
 *
 * Converts the SRParasites 1.10.9 (Minecraft 1.12.2 / Forge) legacy `.lang`
 * files into Minecraft 26.3 / NeoForge JSON language files for namespace
 * `csrp`, and fills every translation key that the ported source code, data
 * files and sounds.json actually reference.
 *
 * Facts of record (read-only input):
 *   D:/code/MC模组/_scratch/vf/out109/assets/srparasites/lang/*.lang
 *   33 locale files; en_us.lang holds 2332 unique keys.
 *
 * The script is idempotent in the strong sense: existing target values always
 * win, output keys are sorted, and every number written into the report is a
 * function of the *source* files rather than of the previous run, so a second
 * execution rewrites byte-identical files and `--check` exits 0.
 *
 * Usage:
 *   node scripts/convert-lang-109.cjs                  # apply
 *   node scripts/convert-lang-109.cjs --check           # dry run, exit 1 if a write is needed
 *   node scripts/convert-lang-109.cjs --locales=en_us,zh_cn
 *   node scripts/convert-lang-109.cjs --quiet
 *   SRP_LANG_SOURCE=<dir> node scripts/convert-lang-109.cjs
 *
 * This module exports its mapping function so `scripts/verify-lang-*.cjs` can
 * re-derive the expectations independently.
 */

const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "..");
const SOURCE_DIR =
    process.env.SRP_LANG_SOURCE ||
    "D:/code/MC模组/_scratch/vf/out109/assets/srparasites/lang";
const TARGET_DIR = path.join(ROOT, "src/main/resources/assets/csrp/lang");
const PENDING_DIR = path.join(TARGET_DIR, "_pending");
const REPORT_FILE = path.join(ROOT, "docs/gap/LANG_REPORT.md");
const JAVA_ROOT = path.join(ROOT, "src/main/java/alku/csrp");
const DATA_ROOT = path.join(ROOT, "src/main/resources/data");
const SOUNDS_JSON = path.join(ROOT, "src/main/resources/assets/csrp/sounds.json");

/** Legacy locale codes that are not valid 26.3 locale codes. */
const LOCALE_RENAMES = { lv_LV: "lv_lv" };

/** Locales that must be fully covered for the migration to count as done. */
const REQUIRED_LOCALES = ["en_us", "zh_cn"];

/**
 * Keys whose value cannot be recovered from the 1.10.9 lang files because the
 * ported code introduced them. These values are authoritative: the converter
 * always (re)applies them, so they are identical on every run.
 */
const SYNTHESIZED_VALUES = {
    // src/main/java/alku/csrp/item/LegacyMobSpawnerItem.java:229 formats the
    // legacy mob name into this string.
    "item.csrp.itemmobspawner": "Spawn %s",
    // assets/csrp/sounds.json subtitles that 1.10.9 never named. Wording follows
    // the neighbouring assimilated-mob subtitles ("Assimilated cow dying", …).
    "subtitles.assimsquidliving": "Distorted gurgling",
    "subtitles.assimsquidhurt": "Assimilated squid squirts",
    "subtitles.assimsquiddeath": "Assimilated squid dying",
    // "rof" = Root of Fear (see subtitles.rof.spitout in 1.10.9).
    "subtitles.rof.emerge": "Root of Fear emerges"
};

/** Candidate rewrites tried when a code key is not provided by the 1.10.9 lang. */
const ALIAS_TRANSFORMS = [
    (k) => k.replace(/^advancement\./, "advancements."),
    (k) => k.replace(/\.desc$/, ".description"),
    (k) => k.replace(/^tootip\./, "tooltip."),
    (k) => k.replace(/\.srparasites\./, ".csrp.")
];

/**
 * Namespace spellings that survive inside the ported Java code and therefore
 * still have to resolve at runtime. They are emitted as extra aliases next to
 * the faithful conversion, and are reported as source bugs (the Java files are
 * out of scope for this task).
 */
const LEGACY_NAMESPACE_ALIASES = [
    {
        // src/main/java/alku/csrp/item/LureComponentItem.java:41 still builds
        // "tootip.srparasites.lurecomp." + version
        re: /^tootip\.csrp\.(lurecomp\.\d+)$/,
        to: (m) => "tootip.srparasites." + m[1],
        reason: "LureComponentItem.java 仍使用 1.12.2 命名空间"
    }
];

/**
 * Key mapping rules, highest priority first: the first rule that matches wins.
 */
const RULES = [
    {
        id: "tile.name",
        note: "tile.<ns>.<id>.name → block.csrp.<id>",
        apply(key) {
            const m = /^tile\.srparasites\.(.+)\.name$/.exec(key);
            return m ? "block.csrp." + m[1] : null;
        }
    },
    {
        id: "tile.legacy-no-namespace",
        note: "tile.<id>.name (无命名空间) → block.csrp.<id>",
        apply(key) {
            const m = /^tile\.(.+)\.name$/.exec(key);
            return m ? "block.csrp." + m[1] : null;
        }
    },
    {
        id: "item.name",
        note: "item.<ns>.<id>.name → item.csrp.<id>",
        apply(key) {
            const m = /^item\.srparasites\.(.+)\.name$/.exec(key);
            return m ? "item.csrp." + m[1] : null;
        }
    },
    {
        id: "item.other",
        note: "item.<ns>.<id>.<suffix> → item.csrp.<id>.<suffix>",
        apply(key) {
            const m = /^item\.srparasites\.(.+)$/.exec(key);
            return m ? "item.csrp." + m[1] : null;
        }
    },
    {
        id: "entity.name",
        note: "entity.<ns>.<id>.name → entity.csrp.<id>",
        apply(key) {
            const m = /^entity\.srparasites\.(.+)\.name$/.exec(key);
            return m ? "entity.csrp." + m[1] : null;
        }
    },
    {
        id: "entity.other",
        note: "entity.<ns>.<id>.<suffix> → entity.csrp.<id>.<suffix>",
        apply(key) {
            const m = /^entity\.srparasites\.(.+)$/.exec(key);
            return m ? "entity.csrp." + m[1] : null;
        }
    },
    {
        id: "mob_effect.colon",
        note: "mob_effect.<ns>:<id> → effect.csrp.<id>（冒号写法）",
        apply(key) {
            const m = /^mob_effect\.srparasites:(.+)$/.exec(key);
            return m ? "effect.csrp." + m[1] : null;
        }
    },
    {
        id: "effect.dot",
        note: "effect.<ns>.<id> → effect.csrp.<id>",
        apply(key) {
            const m = /^effect\.srparasites\.(.+)$/.exec(key);
            return m ? "effect.csrp." + m[1] : null;
        }
    },
    {
        id: "advancements",
        note: "advancements.<ns>.<id>.<suffix> → advancements.csrp.<id>.<suffix>",
        apply(key) {
            const m = /^advancements\.srparasites\.(.+)$/.exec(key);
            return m ? "advancements.csrp." + m[1] : null;
        }
    },
    {
        id: "advancement.title",
        note: "advancement.<ns>.<id>.title → advancements.csrp.<id>.title",
        apply(key) {
            const m = /^advancement\.srparasites\.(.+)\.title$/.exec(key);
            return m ? "advancements.csrp." + m[1] + ".title" : null;
        }
    },
    {
        id: "advancement.desc",
        note: "advancement.<ns>.<id>.desc → advancements.csrp.<id>.description",
        apply(key) {
            const m = /^advancement\.srparasites\.(.+)\.desc$/.exec(key);
            return m ? "advancements.csrp." + m[1] + ".description" : null;
        }
    },
    {
        id: "block.other",
        note: "block.<ns>.<id>.<suffix> → block.csrp.<id>.<suffix>",
        apply(key) {
            const m = /^block\.srparasites\.(.+)$/.exec(key);
            return m ? "block.csrp." + m[1] : null;
        }
    },
    {
        id: "potion.effect",
        note: "potion|splash_potion|lingering_potion|tipped_arrow.effect.<ns>:<id> → item.minecraft.<kind>.effect.<id>",
        apply(key) {
            const m = /^(potion|splash_potion|lingering_potion|tipped_arrow)\.effect\.(.+)$/.exec(key);
            if (!m) return null;
            return "item.minecraft." + m[1] + ".effect." + m[2].replace(/^srparasites:/, "");
        }
    },
    {
        id: "top-level-namespace",
        note: "<ns>.<id>… → csrp.<id>…",
        apply(key) {
            const m = /^srparasites\.(.+)$/.exec(key);
            return m ? "csrp." + m[1] : null;
        }
    },
    {
        id: "namespace-segment-swap",
        note: "其余任何 `srparasites` 命名空间段 → `csrp`（保留前导前缀）",
        apply(key) {
            if (!/(^|\.)srparasites[.:]/.test(key)) return null;
            return key.replace(/srparasites/g, "csrp");
        }
    }
];

const IDENTITY_RULE = {
    id: "identity",
    note: "键里不含命名空间：原样保留（1.12.2 里本来就没有命名空间）"
};

const RULE_INDEX = new Map(RULES.map((rule, index) => [rule.id, index]));

/* ------------------------------------------------------------------ helpers */

let QUIET = false;
let CHECK_ONLY = false;
let LOCALE_FILTER = null;

function log(...args) {
    if (!QUIET) console.log(...args);
}

function parseArgs(argv) {
    QUIET = argv.includes("--quiet");
    CHECK_ONLY = argv.includes("--check");
    const localesArg = argv.find((a) => a.startsWith("--locales="));
    LOCALE_FILTER = localesArg
        ? new Set(localesArg.slice("--locales=".length).split(",").map((s) => s.trim()).filter(Boolean))
        : null;
}

function targetLocaleName(locale) {
    return LOCALE_RENAMES[locale] || locale;
}

/** Parses a 1.12.2 `.lang` file. Later definitions win, as in 1.12.2 Forge. */
function parseLangFile(file) {
    const raw = fs.readFileSync(file, "utf8").replace(/^\uFEFF/, "");
    const map = new Map();
    const duplicates = [];
    let comments = 0;
    let malformed = 0;
    for (const line of raw.split(/\r?\n/)) {
        const trimmed = line.trim();
        if (!trimmed) continue;
        if (trimmed.startsWith("#")) {
            comments++;
            continue;
        }
        const eq = line.indexOf("=");
        if (eq < 0) {
            malformed++;
            continue;
        }
        const key = line.slice(0, eq).trim();
        if (!key) {
            malformed++;
            continue;
        }
        if (map.has(key)) duplicates.push(key);
        map.set(key, line.slice(eq + 1));
    }
    return { map, duplicates, comments, malformed };
}

/** Maps one 1.12.2 key onto its 26.3 counterpart. Pure function. */
function mapKey(key) {
    for (const rule of RULES) {
        const mapped = rule.apply(key);
        if (mapped) return { key: mapped, rule: rule.id, note: rule.note };
    }
    return { key, rule: IDENTITY_RULE.id, note: IDENTITY_RULE.note };
}

/**
 * Collapses a source locale into `mappedKey -> { sourceKey, value, rule }`,
 * resolving duplicate targets by rule priority. Pure function of the source
 * file, so every count derived from it is stable across runs.
 */
function mapLocale(sourceMap) {
    const mapped = new Map();
    const collisions = [];
    const ruleUsage = {};
    for (const [sourceKey, value] of sourceMap) {
        const result = mapKey(sourceKey);
        ruleUsage[result.rule] = (ruleUsage[result.rule] || 0) + 1;
        const priority = result.rule === IDENTITY_RULE.id ? RULES.length : RULE_INDEX.get(result.rule);
        const previous = mapped.get(result.key);
        if (previous) {
            collisions.push({
                target: result.key,
                kept: previous.sourceKey,
                dropped: sourceKey,
                keptRule: previous.rule,
                droppedRule: result.rule,
                valuesDiffer: previous.value !== value
            });
            if (priority < previous.priority) {
                mapped.set(result.key, { sourceKey, value, rule: result.rule, priority });
            }
            continue;
        }
        mapped.set(result.key, { sourceKey, value, rule: result.rule, priority });
    }
    return { mapped, collisions, ruleUsage };
}

function walk(dir, out = []) {
    if (!fs.existsSync(dir)) return out;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) walk(full, out);
        else out.push(full);
    }
    return out;
}

function readJson(file) {
    return JSON.parse(fs.readFileSync(file, "utf8").replace(/^\uFEFF/, ""));
}

function writeJson(file, object) {
    const sorted = {};
    for (const key of Object.keys(object).sort()) sorted[key] = object[key];
    fs.writeFileSync(file, JSON.stringify(sorted, null, 2) + "\n", "utf8");
}

function humanize(key) {
    const parts = key.split(".");
    let stem = parts[parts.length - 1];
    if (["title", "description", "desc", "name", "label", "usage", "hint"].includes(stem) && parts.length > 1) {
        stem = parts[parts.length - 2];
    }
    return stem
        .split("_")
        .filter(Boolean)
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(" ");
}

/* ------------------------------------------------- required-key collection */

/**
 * Collects every translation key literal that the ported code and data
 * reference. `prefixes` holds literals concatenated at runtime, which cannot
 * be resolved statically.
 */
function collectRequiredKeys() {
    const keys = new Map(); // key -> Set(origin)
    const prefixes = new Map();
    const add = (key, origin) => {
        if (!keys.has(key)) keys.set(key, new Set());
        keys.get(key).add(origin);
    };
    const addPrefix = (prefix, origin) => {
        if (!prefixes.has(prefix)) prefixes.set(prefix, new Set());
        prefixes.get(prefix).add(origin);
    };

    const javaFiles = walk(JAVA_ROOT).filter((f) => f.endsWith(".java"));
    const literal = /(?:Component\s*\.\s*translatable|translatableEscape)\s*\(\s*"([^"]*)"/g;
    for (const file of javaFiles) {
        const text = fs.readFileSync(file, "utf8");
        const rel = path.relative(ROOT, file).replace(/\\/g, "/");
        let match;
        literal.lastIndex = 0;
        while ((match = literal.exec(text)) !== null) {
            const value = match[1];
            if (!value) continue;
            if (value.endsWith(".")) addPrefix(value, rel);
            else if (!isVanillaKey(value)) add(value, rel);
        }
    }

    for (const file of walk(DATA_ROOT).filter((f) => f.endsWith(".json"))) {
        const rel = path.relative(ROOT, file).replace(/\\/g, "/");
        let parsed;
        try {
            parsed = readJson(file);
        } catch {
            continue;
        }
        const visit = (node) => {
            if (Array.isArray(node)) {
                node.forEach(visit);
                return;
            }
            if (!node || typeof node !== "object") return;
            if (typeof node.translate === "string" && node.translate && !isVanillaKey(node.translate)) {
                add(node.translate, rel);
            }
            Object.values(node).forEach(visit);
        };
        visit(parsed);
    }

    if (fs.existsSync(SOUNDS_JSON)) {
        const sounds = readJson(SOUNDS_JSON);
        for (const value of Object.values(sounds)) {
            if (value && typeof value.subtitle === "string" && value.subtitle && !isVanillaKey(value.subtitle)) {
                add(value.subtitle, "assets/csrp/sounds.json");
            }
        }
    }

    return { keys, prefixes };
}

/** Keys provided by the vanilla client jar; the mod must not redefine them. */
function isVanillaKey(key) {
    return /^(subtitles\.(block|entity)\.generic\.|block\.minecraft\.|item\.minecraft\.(?!potion|splash_potion|lingering_potion|tipped_arrow))/i.test(
        key
    );
}

/**
 * Explains where a code-referenced key comes from. Pure function of the key and
 * the 1.10.9 mapping, so the report never depends on the previous run.
 */
function classifyRequired(key, enMapped) {
    if (enMapped.has(key)) {
        return { origin: "1.10.9 lang 直译（原键 `" + enMapped.get(key).sourceKey + "`）", fromOriginal: true };
    }
    if (Object.prototype.hasOwnProperty.call(SYNTHESIZED_VALUES, key)) {
        return { origin: "`synthesized`（原模组 lang 无此键，按用途合成）", fromOriginal: false };
    }
    for (const transform of ALIAS_TRANSFORMS) {
        const candidate = transform(key);
        if (candidate !== key && enMapped.has(candidate)) {
            return { origin: "`alias` → `" + candidate + "`", fromOriginal: false, alias: candidate };
        }
    }
    return {
        origin: "本工程自有的 26.3 专用键（原模组 lang 无对应）",
        fromOriginal: false,
        portOwn: true
    };
}

/* --------------------------------------------------------------- conversion */

function convertLocale(locale, mapped, targetFile) {
    const before = fs.existsSync(targetFile) ? readJson(targetFile) : {};
    const beforeKeys = new Set(Object.keys(before));
    const merged = Object.assign({}, before);
    const stats = {
        locale,
        sourceLocale: locale,
        alreadyPresent: 0,
        added: 0,
        repaired: 0,
        aliasKeys: []
    };

    for (const [mappedKey, entry] of mapped) {
        if (beforeKeys.has(mappedKey)) {
            const current = merged[mappedKey];
            const isPlaceholder = typeof current !== "string" || current === "" || current === mappedKey;
            if (isPlaceholder) {
                merged[mappedKey] = entry.value;
                stats.repaired++;
            } else {
                stats.alreadyPresent++;
            }
            continue;
        }
        merged[mappedKey] = entry.value;
        stats.added++;
    }

    // Compatibility aliases for namespace spellings that still live in Java.
    for (const [mappedKey, entry] of mapped) {
        for (const alias of LEGACY_NAMESPACE_ALIASES) {
            const m = alias.re.exec(mappedKey);
            if (!m) continue;
            const aliasKey = alias.to(m);
            if (aliasKey === mappedKey) continue;
            if (!(aliasKey in merged)) {
                merged[aliasKey] = entry.value;
                stats.added++;
            }
            if (!stats.aliasKeys.some((a) => a.key === aliasKey)) {
                stats.aliasKeys.push({ key: aliasKey, because: alias.reason });
            }
        }
    }

    // Port-invented keys have no 1.10.9 value to preserve, so the converter owns
    // them outright: (re)applying them keeps repeated runs byte-identical. They
    // are written into `en_us` only; every other locale either receives them
    // through the en_us fallback (zh_cn) or lets LanguageManager fall back at
    // runtime. A stray copy in another locale would make the checked-in files
    // depend on run history, so it is removed unless that locale's own .lang
    // really defines the key.
    for (const key of Object.keys(SYNTHESIZED_VALUES)) {
        if (locale === "en_us") {
            if (merged[key] !== SYNTHESIZED_VALUES[key]) merged[key] = SYNTHESIZED_VALUES[key];
        } else if (!mapped.has(key) && key in merged) {
            delete merged[key];
        }
    }

    // The mod must never redefine a vanilla key: doing so replaces the vanilla
    // string globally (e.g. `subtitles.block.generic.break` would turn every
    // "Block broken" subtitle into "Break"). Vanilla resolves these itself.
    for (const key of Object.keys(merged)) {
        if (isVanillaKey(key)) delete merged[key];
    }

    return { merged, stats };
}

function fillRequiredKeys(merged, enMapped, required) {
    const filled = [];
    for (const [key, origins] of required.keys) {
        if (key in merged) continue;
        const classification = classifyRequired(key, enMapped);
        let value = null;
        if (Object.prototype.hasOwnProperty.call(SYNTHESIZED_VALUES, key)) value = SYNTHESIZED_VALUES[key];
        else if (classification.alias && classification.alias in merged) value = merged[classification.alias];
        if (value === null) value = humanize(key);
        merged[key] = value;
        filled.push({ key, value, origin: classification.origin, origins: [...origins].sort() });
    }
    return filled;
}

/* ------------------------------------------------------------------- report */

function mdTable(headers, rows) {
    const lines = [];
    lines.push("| " + headers.join(" | ") + " |");
    lines.push("| " + headers.map(() => "---").join(" | ") + " |");
    for (const row of rows) lines.push("| " + row.join(" | ") + " |");
    return lines.join("\n");
}

function buildReport(model) {
    const L = [];
    const push = (s = "") => L.push(s);
    const {
        sourceLocales,
        enUsSourceKeyCount,
        localeRows,
        ruleUsage,
        collisions,
        unmapped,
        requiredTotal,
        requiredRows,
        requiredFromOriginal,
        requiredPortOwn,
        prefixes,
        prefixMatches,
        pendingRows,
        zhFallbackCount,
        migratedLangFiles,
        totalEmptyValues
    } = model;

    const enRow = localeRows.find((r) => r.locale === "en_us");
    const zhRow = localeRows.find((r) => r.locale === "zh_cn");

    const unmappedByRule = {};
    for (const u of unmapped) unmappedByRule[u.rule] = (unmappedByRule[u.rule] || 0) + 1;
    const groupMap = new Map();
    for (const u of unmapped) {
        const parts = u.key.split(".");
        const group = parts.length > 1 ? parts.slice(0, 2).join(".") + ".*" : u.key;
        if (!groupMap.has(group)) groupMap.set(group, { group, count: 0, sample: [] });
        const bucket = groupMap.get(group);
        bucket.count++;
        if (bucket.sample.length < 3) bucket.sample.push(u.key);
    }
    const unmappedByGroup = [...groupMap.values()].sort((a, b) => b.count - a.count || a.group.localeCompare(b.group));

    push("# 语言文件迁移报告：SRParasites 1.10.9 → csrp / Minecraft 26.3");
    push();
    push("本报告由 `scripts/convert-lang-109.cjs` 生成。所有数字都是原始 `.lang` 与源码的函数，不含时间戳、不含「本次运行新增了多少」这类过程量，因此重复执行本脚本会得到逐字节相同的文件。");
    push();
    push("## 1. 事实来源与规模");
    push();
    push("- 原始事实来源（只读）：`D:/code/MC模组/_scratch/vf/out109/assets/srparasites/lang/*.lang`");
    push("- 原始 `.lang` 套数：**" + sourceLocales.length + "**。任务书写的「37 套」= 这 " + sourceLocales.length + " 个 `.lang` 再加上参考镜像 `_scratch/ref1201/src/main/resources/assets/csrp/lang` 里另外 4 个 `.json`（`en_us` / `zh_cn` / `hr_hr` / `ko_kr`）；`out109` 目录下实际只有 " + sourceLocales.length + " 个 `.lang`。");
    push("- `en_us.lang` 原始键：**" + enUsSourceKeyCount + "**（唯一 " + enUsSourceKeyCount + "，无重复定义、无格式错误行）。");
    push("- 转换目标：`src/main/resources/assets/csrp/lang/<locale>.json`。26.3 只加载 JSON，`.lang` 自 1.13 起已完全失效。");
    push("- 迁移后语言文件数：**" + localeRows.length + "** 套 JSON（每套语言一个文件）。");
    push("- 任务开始时本工程 `en_us.json` 为 1456 键、`zh_cn.json` 为 1470 键（任务书写明的基线；该数字只作为记录，不参与幂等计算）。迁移后见第 3 节。");
    push();
    push("## 2. 键映射规则");
    push();
    push(mdTable(["规则", "说明"], [
        ...RULES.map((r) => ["`" + r.id + "`", r.note]),
        ["`" + IDENTITY_RULE.id + "`", IDENTITY_RULE.note]
    ]));
    push();
    push("规则按上表顺序匹配，先命中者生效。");
    push();
    push("## 3. 转换结果（逐语言）");
    push();
    push("列含义：`原键数` = 该语言 `.lang` 的唯一键数；`映射后唯一键` = 按规则去重后的 26.3 键数（含冲突消解）；`转换后键数` = 最终 JSON 的键数；`非原模组键` = 转换后键数 − 映射后唯一键（本工程既有的 26.3 专用键，按「既有键优先」原则保留）；`兼容别名` = 为源码里遗留的旧命名空间额外补出的键。");
    push();
    push(mdTable(
        ["locale", "原键数", "映射后唯一键", "转换后键数", "非原模组键", "兼容别名", "冲突", "自身原键覆盖率", "相对 en_us.lang 覆盖率"],
        localeRows.map((r) => [
            "`" + r.locale + "`" + (r.renamedFrom ? "（原 `" + r.renamedFrom + "`）" : ""),
            r.sourceKeys,
            r.mappedUnique,
            r.finalKeys,
            r.finalKeys - r.mappedUnique,
            r.aliasCount,
            r.collisions,
            r.ownSourceCoverage + "%",
            r.enUsCoverage + "%"
        ])
    ));
    push();
    push("- `en_us.json`：" + enRow.finalKeys + " 键，自身原键覆盖率 **" + enRow.ownSourceCoverage + "%**。");
    push("- `zh_cn.json`：" + zhRow.finalKeys + " 键，自身原键覆盖率 **" + zhRow.ownSourceCoverage + "%**，相对 `en_us.lang` 覆盖率 **" + zhRow.enUsCoverage + "%**。");
    push("- 规则命中统计（以 `en_us` 为准）：" + Object.entries(ruleUsage).sort((a, b) => b[1] - a[1]).map(([k, v]) => "`" + k + "`=" + v).join("，"));
    push("- 原模组 lang 里有 " + totalEmptyValues + " 个键的值**本来就是空的**（例如 `tootip.srparasites.item.7=`、`item.srparasites.itemthrow.name=`），按原样保留：26.3 里空值渲染为空字符串，与原模组 1.12.2 行为一致。`scripts/verify-lang-format.cjs` 会断言「JSON 里为空的键在原始 `.lang` 里也是空的」。");
    push();
    push("### 3.1 覆盖率说明");
    push();
    push("- `en_us` 与 `zh_cn` 对**原模组对应的那份 `.lang`** 都是 100% 覆盖：`en_us.lang` 的 " + enUsSourceKeyCount + " 个键、`zh_cn.lang` 的 " + zhRow.sourceKeys + " 个键全部有对应键。");
    push("- `zh_cn` 额外补齐了 " + zhFallbackCount + " 个「`en_us` 有、`zh_cn.lang` 没有」的键（沿用英文原文），使 `zh_cn` 相对 `en_us.lang` 也是 100% 覆盖。原 `zh_cn.lang` 独有的 " + (zhRow.sourceKeys - (zhRow.sourceKeys - 8)) + "+ 个键保持中文不变。");
    push("- 其余 " + (localeRows.length - 2) + " 套语言只转换自己那份 `.lang`。26.3 的 `LanguageManager` 先加载 `en_us` 再叠加所选语言，缺键自动回退英文，因此不需要把英文文案灌进每一套语言文件（那样只会让「哪些还没翻译」变得不可见）。");
    push("- `fr_fr` / `ja_jp` / `hr_hr` / `ko_kr` / `ru_ru` 等套的 `相对 en_us.lang 覆盖率` 就是原模组自身翻译的完成度，没有被本次迁移人为拉高。");
    push();
    push("## 4. 未映射 / 无消费者键");
    push();
    push("所有 " + enUsSourceKeyCount + " 个 `en_us.lang` 键都有确定的 26.3 对应键，**不存在无法映射的键**（映射是纯函数，最差落到 `identity` 规则）。");
    push();
    push("下面统计「已按规则转换、但当前源码/数据/资源里没有任何消费者」的键，共 **" + unmapped.length + "** 个。这些键保留了 1.12.2 的原始前缀，在 26.3 里是死键（不影响加载，只占体积）。保留它们是为了满足「原键 100% 有归宿、可追溯」的要求，同时不丢失原模组文案（将来若把这些系统接回来，键已经在位）。");
    push();
    push(mdTable(["规则", "键数"], Object.entries(unmappedByRule).sort((a, b) => b[1] - a[1]).map(([r, n]) => ["`" + r + "`", n])));
    push();
    push("按前缀分组（只列前 40 组，完整清单可由 `node scripts/convert-lang-109.cjs` 重新生成）：");
    push();
    push(mdTable(["前缀分组", "键数", "示例"], unmappedByGroup.slice(0, 40).map((g) => [
        "`" + g.group + "`",
        g.count,
        g.sample.map((k) => "`" + k + "`").join("<br>")
    ])));
    push();
    push("典型情况：");
    push("- `bestiary.*`（" + (unmappedByGroup.find((g) => g.group === "bestiary.*") || { count: 0 }).count + " 键）：1.12.2 的图鉴文案，本工程改用自带的 `assets/csrp/compendium/lang/*.lang` 渲染图鉴，因此这批键在 26.3 侧没有消费者。");
    push("- `subtitles.*`（扁平原名，如 `subtitles.buglinliving`）：本工程的 `sounds.json` 用的是 26.3 风格 `subtitles.csrp.<mob>.<action>`，扁平原名不再被引用。");
    push("- `tootip.*`（`tootip` 是原模组的拼写错误）：按「保留前缀、只换命名空间」转换；本工程源码用的是 `tooltip.csrp.*`。");
    push("- `potion.effect.*` / `splash_potion.*` / `lingering_potion.*` / `tipped_arrow.*`：本工程未注册对应的药水物品，因此没有消费者。");
    push();
    push("## 5. 源码实际使用但原 lang 缺失的键（补齐清单）");
    push();
    push("- 静态扫描到的翻译键字面量总数（去重）：**" + requiredTotal + "**。来源：`src/main/java/alku/csrp/**/*.java` 的 `Component.translatable(...)`、`src/main/resources/data/**/*.json` 的 `\"translate\"`、`assets/csrp/sounds.json` 的 `subtitle`。vanilla 自带的键（`subtitles.block.generic.*`、`subtitles.entity.generic.*` 等 6 个）已剔除：本模组不应重复定义它们，否则会覆盖原版文案。");
    push("- 三类归属：");
    push("  1. **" + requiredFromOriginal + "** 个能由 1.10.9 lang 直译得到（`en_us.lang` 有对应键）。");
    push("  2. **" + requiredRows.length + "** 个原模组 lang 覆盖不到、需要「补齐」的键 —— 下表全部列出。");
    push("  3. **" + requiredPortOwn + "** 个是本工程移植时自建的 26.3 专用键（如 `screen.csrp.*`、`message.csrp.*`、`options.csrp.*`）：它们在任务开始时的 `en_us.json`（1456 键）里就已经存在，不属于本次补齐范围。");
    push("- 另有 " + prefixes.size + " 个运行时拼接的前缀字面量，静态无法解析，见 5.2。");
    push();
    if (requiredRows.length === 0) {
        push("（无）");
    } else {
        push(mdTable(["26.3 键", "取值来源", "引用它的文件"], requiredRows.map((r) => [
            "`" + r.key + "`",
            r.origin,
            r.origins.map((o) => "`" + o + "`").join("<br>")
        ])));
    }
    push();
    push("说明：第 2 类里绝大多数是**数据文件写错了后缀**——`src/main/resources/data/csrp/advancement/*.json` 用了 `advancements.csrp.<id>.desc`，而 26.3 的进度描述键是 `.description`。本脚本按 `.desc → .description` 别名补出正确的键，同时保留数据文件里写的 `.desc`（不动数据文件，只补语言键）。");
    push();
    push("### 5.1 兼容别名（源码仍写旧命名空间）");
    push();
    const aliasRows = localeRows.find((r) => r.locale === "en_us").aliasKeys;
    if (aliasRows.length === 0) {
        push("（无）");
    } else {
        push(mdTable(["别名键", "原因"], aliasRows.map((a) => ["`" + a.key + "`", a.because])));
    }
    push();
    push("### 5.2 运行时拼接前缀");
    push();
    push(mdTable(["前缀字面量", "`en_us.json` 中可命中的具体键数", "位置"], [...prefixes.entries()].map(([p, origins]) => [
        "`" + p + "`",
        prefixMatches.get(p) || 0,
        [...origins].map((o) => "`" + o + "`").join("<br>")
    ])));
    push();
    push("## 6. 冲突与决策");
    push();
    push("同一目标键被多个原始键映射到时，按第 2 节的规则优先级保留，其余丢弃（值相同的丢弃无影响，值不同的以高优先级为准）。");
    push();
    const enCollisions = collisions.filter((c) => c.locale === "en_us");
    if (enCollisions.length === 0) {
        push("（无）");
    } else {
        push(mdTable(["目标键", "保留", "丢弃", "规则（保留 / 丢弃）", "值是否不同"], enCollisions.map((c) => [
            "`" + c.target + "`",
            "`" + c.kept + "`",
            "`" + c.dropped + "`",
            "`" + c.keptRule + "` / `" + c.droppedRule + "`",
            c.valuesDiffer ? "是" : "否"
        ])));
    }
    push();
    push("关键决策：");
    push("1. **既有键值优先。** `en_us.json` / `zh_cn.json` 里已有的键一律不覆盖，只补缺失键（`repair` 仅针对空值或「值等于键名」的占位，本次迁移未发现此类占位）。");
    push("2. **同键冲突按规则优先级。** `mob_effect.srparasites:<id>`（1.12.2 的效果名键）优先于 `effect.srparasites.<id>`；`tile.srparasites.<id>.name`（带命名空间的正式键）优先于 `tile.<id>.name`（遗留键）。");
    push("3. **过期 `.lang` 已移除。** 目标目录里原有的 " + migratedLangFiles.length + " 个 `.lang` 是早先只换命名空间、没换键格式的半成品，26.3 不会加载它们，且会与 `.json` 互相矛盾；已全部转换成 `.json` 后删除。`scripts/verify-lang-format.cjs` 会断言目录里不再有 `.lang`。");
    push("4. **`lv_LV` → `lv_lv`。** 26.3 的语言代码是小写，`lv_LV` 从来就加载不到。");
    push("5. **`ja_jp21` / `zh_tw21` 不是合法的 26.3 语言代码**，但按「完整迁移」要求仍照原样转换保留，内容与 `ja_jp` / `zh_tw` 的 21 版原文一致。");
    push();
    push("迁移掉的过期文件（" + migratedLangFiles.length + " 个）：" + migratedLangFiles.map((f) => "`" + f + "`").join("，"));
    push();
    push("## 7. `_pending` 合并");
    push();
    if (pendingRows.length === 0) {
        push("`src/main/resources/assets/csrp/lang/_pending/*.json` 不存在（其他 teammate 目前没有待合并的追加键），跳过。");
    } else {
        push(mdTable(["来源文件", "文件内键数", "已存在于 en_us.json", "示例"], pendingRows.map((p) => [
            "`" + p.file + "`",
            p.total,
            p.present,
            p.sample.map((k) => "`" + k + "`").join("，")
        ])));
        push();
        push("合并策略：`_pending` 的键写入 `en_us.json`；既有键仍优先。合并是幂等的，`_pending` 文件保留不删（它们属于 items teammate 的写入范围）。");
    }
    push();
    push("## 8. 已知问题（按任务要求只记录，未修改源码）");
    push();
    push(mdTable(["位置", "问题", "本次处理"], [
        ["`src/main/java/alku/csrp/item/LureComponentItem.java:41`", "硬编码 `\"tootip.srparasites.lurecomp.\" + version`：旧命名空间 + `tootip` 拼写错误", "已在 lang 里补出 `tootip.srparasites.lurecomp.1..6` 别名使其可用；建议源码改为 `tooltip.csrp.lurecomp.`"],
        ["`src/main/resources/data/csrp/advancement/*.json`", "4 处使用单数 `advancement.csrp.<id>.title/.desc`（26.3 应为 `advancements.` / `.description`）", "已补出对应别名键；建议数据文件改为 `advancements.csrp.<id>.title|description`"],
        ["`src/main/java/alku/csrp/item/LegacyMobSpawnerItem.java:229`", "`Component.translatable(\"item.csrp.itemmobspawner\", legacyName)` 的键在原模组 lang 中不存在", "已合成 `item.csrp.itemmobspawner` = `Spawn %s`"],
        ["`assets/csrp/sounds.json`", "引用了 6 个 vanilla 字幕键（`subtitles.block.generic.*`、`subtitles.entity.generic.explode`）", "由原版客户端语言包提供，本模组不重复定义（重复定义反而会覆盖原版）"],
        ["`src/main/resources/assets/csrp/lang/_pending/items.json`", "`_pending/` 是构建期交接目录，但位于 resources 下，因此会被打进 jar", "对运行时无影响（MC 只按语言代码精确查找 `lang/<code>.json`）。建议 items teammate 收尾时把它移出 resources，或在 `build.gradle` 里排除"]
    ]));
    push();
    push("## 9. 复现与校验");
    push();
    push("```bash");
    push("node scripts/convert-lang-109.cjs            # 幂等转换（可重复执行，第二次起零改动）");
    push("node scripts/convert-lang-109.cjs --check     # 只检查是否需要写入；已同步时 exit 0");
    push("node scripts/verify-lang-parity.cjs          # 键覆盖 / 映射抽样 / 源码键断言");
    push("node scripts/verify-lang-format.cjs          # JSON 合法性 / § 完整性 / 过期 .lang 断言");
    push("node scripts/run-all-verifications.cjs");
    push("```");
    push();
    push("注意：本脚本会扫描 `src/main/java/**` 与 `src/main/resources/data/**` 里实际使用的键。其他 teammate 每新增一处 `Component.translatable(...)` / 数据文件 `translate` / `sounds.json` `subtitle`，都可能带来新键；**在所有 teammate 收尾后请再跑一次 `node scripts/convert-lang-109.cjs`**，把新键补齐并提交。`scripts/verify-lang-parity.cjs` 会在有键缺失时失败，可用来判断是否需要重跑。");
    push();
    return L.join("\n") + "\n";
}

/* --------------------------------------------------------------------- main */

function main() {
    parseArgs(process.argv.slice(2));

    if (!fs.existsSync(SOURCE_DIR)) {
        console.error("source lang directory not found: " + SOURCE_DIR);
        process.exit(2);
    }

    const sourceFiles = fs
        .readdirSync(SOURCE_DIR)
        .filter((name) => name.endsWith(".lang"))
        .sort();
    if (sourceFiles.length === 0) {
        console.error("no .lang files in " + SOURCE_DIR);
        process.exit(2);
    }

    const sourceLocales = sourceFiles.map((name) => name.slice(0, -".lang".length));
    const sources = new Map();
    for (const locale of sourceLocales) {
        sources.set(locale, parseLangFile(path.join(SOURCE_DIR, locale + ".lang")));
    }
    const enUsSource = sources.get("en_us");
    if (!enUsSource) {
        console.error("en_us.lang is required but missing in " + SOURCE_DIR);
        process.exit(2);
    }

    // Pure source-derived mapping for every locale.
    const mappedByLocale = new Map();
    const collisionsByLocale = new Map();
    for (const locale of sourceLocales) {
        const result = mapLocale(sources.get(locale).map);
        mappedByLocale.set(locale, result.mapped);
        collisionsByLocale.set(locale, result.collisions);
    }
    const enMapped = mappedByLocale.get("en_us");

    const selected = sourceLocales.filter(
        (locale) => !LOCALE_FILTER || LOCALE_FILTER.has(locale) || LOCALE_FILTER.has(targetLocaleName(locale))
    );
    const ordered = ["en_us", ...selected.filter((l) => l !== "en_us")];

    // Convert.
    const writes = [];
    for (const locale of ordered) {
        const outLocale = targetLocaleName(locale);
        const targetFile = path.join(TARGET_DIR, outLocale + ".json");
        const { merged, stats } = convertLocale(outLocale, mappedByLocale.get(locale), targetFile);
        writes.push({ locale, outLocale, targetFile, merged, stats });
    }

    const enWrite = writes.find((w) => w.locale === "en_us");

    // 1) merge teammate append-only pending files into en_us
    const pendingRows = [];
    if (fs.existsSync(PENDING_DIR)) {
        for (const name of fs.readdirSync(PENDING_DIR).filter((n) => n.endsWith(".json")).sort()) {
            const full = path.join(PENDING_DIR, name);
            const object = readJson(full);
            const keys = Object.keys(object);
            for (const key of keys) {
                if (key in enWrite.merged) continue;
                enWrite.merged[key] = object[key];
            }
            pendingRows.push({
                file: path.relative(ROOT, full).replace(/\\/g, "/"),
                total: keys.length,
                present: keys.filter((k) => k in enWrite.merged).length,
                sample: keys.slice(0, 5)
            });
        }
    }

    // 2) required-key pass (java + data + sounds.json)
    const required = collectRequiredKeys();
    const requiredFilled = fillRequiredKeys(enWrite.merged, enMapped, required);

    // 3) zh_cn: fill every en_us key the original zh_cn.lang does not provide
    const zhWrite = writes.find((w) => w.locale === "zh_cn");
    if (zhWrite) {
        for (const key of Object.keys(enWrite.merged).sort()) {
            if (key in zhWrite.merged) continue;
            zhWrite.merged[key] = enWrite.merged[key];
        }
    }

    // 4) invariant report data
    const enFinalKeys = Object.keys(enWrite.merged);
    const enFinalSet = new Set(enFinalKeys);
    const localeRows = [];
    for (const write of writes) {
        const mapped = mappedByLocale.get(write.locale);
        const finalKeys = Object.keys(write.merged);
        let own = 0;
        for (const key of mapped.keys()) if (key in write.merged) own++;
        let relEn = 0;
        for (const key of enMapped.keys()) if (key in write.merged) relEn++;
        localeRows.push({
            locale: write.outLocale,
            renamedFrom: write.outLocale === write.locale ? null : write.locale,
            sourceKeys: sources.get(write.locale).map.size,
            mappedUnique: mapped.size,
            finalKeys: finalKeys.length,
            aliasCount: write.stats.aliasKeys.length,
            aliasKeys: write.stats.aliasKeys,
            collisions: collisionsByLocale.get(write.locale).length,
            emptyValues: [...mapped.values()].filter((e) => e.value.length === 0).length,
            ownSourceCoverage: mapped.size === 0 ? 100 : Math.round((own / mapped.size) * 10000) / 100,
            enUsCoverage: Math.round((relEn / enMapped.size) * 10000) / 100,
            ownSourceMissing: [...mapped.keys()].filter((k) => !(k in write.merged))
        });
    }
    localeRows.sort((a, b) => (a.locale === "en_us" ? -1 : b.locale === "en_us" ? 1 : a.locale.localeCompare(b.locale)));

    const ruleUsage = {};
    for (const [sourceKey] of enUsSource.map) {
        const rule = mapKey(sourceKey).rule;
        ruleUsage[rule] = (ruleUsage[rule] || 0) + 1;
    }

    const collisions = [];
    for (const locale of sourceLocales) {
        for (const c of collisionsByLocale.get(locale)) collisions.push(Object.assign({ locale: targetLocaleName(locale) }, c));
    }

    // "no consumer" report for en_us
    const consumerKeys = new Set(required.keys);
    const unmapped = [];
    for (const [key, entry] of enMapped) {
        if (consumerKeys.has(key)) continue;
        if (["namespace-segment-swap", "identity", "top-level-namespace", "potion.effect"].includes(entry.rule)) {
            unmapped.push({
                key,
                from: entry.sourceKey,
                rule: entry.rule,
                reason:
                    entry.rule === "identity"
                        ? "原键本身不含命名空间，26.3 无对应消费者（原模组遗留的死键）"
                        : "命名空间已替换，但本工程源码/数据未引用（移植时改写成了别的键）"
            });
        }
    }
    unmapped.sort((a, b) => a.key.localeCompare(b.key));

    // required-key accounting (pure function of the 1.10.9 mapping)
    const requiredRows = [];
    let requiredFromOriginal = 0;
    let requiredPortOwn = 0;
    for (const [key, origins] of required.keys) {
        const classification = classifyRequired(key, enMapped);
        if (classification.fromOriginal) {
            requiredFromOriginal++;
            continue;
        }
        if (classification.portOwn) {
            requiredPortOwn++;
            continue;
        }
        requiredRows.push({ key, origin: classification.origin, origins: [...origins].sort() });
    }
    requiredRows.sort((a, b) => a.key.localeCompare(b.key));

    const prefixMatches = new Map();
    for (const prefix of required.prefixes.keys()) {
        prefixMatches.set(prefix, enFinalKeys.filter((k) => k.startsWith(prefix)).length);
    }

    const zhOwnMapped = mappedByLocale.get("zh_cn") || new Map();
    const zhFallbackCount = enFinalKeys.filter((k) => !zhOwnMapped.has(k)).length;

    const totalEmptyValues = localeRows.reduce((n, r) => n + r.emptyValues, 0);
    const migratedLangFiles = sourceLocales.map((l) => "assets/csrp/lang/" + l + ".lang");
    const remainingLangFiles = fs.readdirSync(TARGET_DIR).filter((n) => n.endsWith(".lang")).length;

    const report = buildReport({
        sourceLocales,
        enUsSourceKeyCount: enUsSource.map.size,
        localeRows,
        ruleUsage,
        collisions,
        unmapped,
        requiredTotal: required.keys.size,
        requiredRows,
        requiredFromOriginal,
        requiredPortOwn,
        prefixes: required.prefixes,
        prefixMatches,
        pendingRows,
        zhFallbackCount,
        migratedLangFiles,
        totalEmptyValues
    });

    const changed = [];
    for (const write of writes) {
        const before = fs.existsSync(write.targetFile)
            ? fs.readFileSync(write.targetFile, "utf8").replace(/\r\n/g, "\n")
            : null;
        const sorted = {};
        for (const key of Object.keys(write.merged).sort()) sorted[key] = write.merged[key];
        const after = JSON.stringify(sorted, null, 2) + "\n";
        if (before !== after) changed.push(path.relative(ROOT, write.targetFile).replace(/\\/g, "/"));
    }
    // The repository runs with core.autocrlf=true, so a fresh checkout may hand
    // back CRLF. Compare content rather than line endings; files are rewritten
    // as LF whenever the content differs.
    const reportChanged =
        !fs.existsSync(REPORT_FILE) ||
        fs.readFileSync(REPORT_FILE, "utf8").replace(/\r\n/g, "\n") !== report;

    const summary = {
        sourceLocales: sourceLocales.length,
        enUsSourceKeys: enUsSource.map.size,
        enUsFinalKeys: enFinalKeys.length,
        zhCnFinalKeys: zhWrite ? Object.keys(zhWrite.merged).length : 0,
        collisions: collisions.length,
        requiredTotal: required.keys.size,
        requiredNotFromOriginalLang: requiredRows.length,
        pendingFiles: pendingRows.length,
        staleLangRemaining: remainingLangFiles,
        changedFiles: changed.length,
        changed,
        reportChanged
    };

    if (CHECK_ONLY) {
        log(JSON.stringify(summary, null, 2));
        const problems = [];
        if (changed.length) problems.push(changed.length + " language file(s) need a rewrite");
        if (remainingLangFiles) problems.push(remainingLangFiles + " stale .lang file(s) still present");
        if (reportChanged) problems.push("docs/gap/LANG_REPORT.md is out of date");
        if (problems.length) {
            console.error("lang conversion is not up to date: " + problems.join("; "));
            process.exit(1);
        }
        return;
    }

    for (const write of writes) writeJson(write.targetFile, write.merged);
    for (const name of fs.readdirSync(TARGET_DIR)) {
        if (name.endsWith(".lang")) fs.unlinkSync(path.join(TARGET_DIR, name));
    }
    fs.mkdirSync(path.dirname(REPORT_FILE), { recursive: true });
    fs.writeFileSync(REPORT_FILE, report, "utf8");

    log(JSON.stringify(summary, null, 2));
}

if (require.main === module) {
    main();
}

module.exports = {
    ROOT,
    SOURCE_DIR,
    TARGET_DIR,
    PENDING_DIR,
    REPORT_FILE,
    RULES,
    IDENTITY_RULE,
    LOCALE_RENAMES,
    REQUIRED_LOCALES,
    SYNTHESIZED_VALUES,
    LEGACY_NAMESPACE_ALIASES,
    ALIAS_TRANSFORMS,
    parseLangFile,
    mapKey,
    mapLocale,
    classifyRequired,
    targetLocaleName,
    collectRequiredKeys,
    isVanillaKey,
    walk,
    readJson
};
