#!/usr/bin/env node
/*
 * Merges the per-creature audit files (docs/entity-parity/raw/<id>.json) into
 *   docs/entity-parity/parity-matrix.json  (机器可读矩阵)
 *   docs/entity-parity/PARITY_MATRIX.md    (人读矩阵 + 缺口清单)
 *   docs/entity-parity/BASELINE.md         (完成度基线)
 * and validates the audit files against docs/entity-parity/AUDIT_PROTOCOL.md.
 *
 * Usage:
 *   node scripts/entity-parity/verify-entity-parity.cjs [--allow-missing] [--quiet]
 *
 * Exit code: 0 when every creature in audit-input.json has a valid audit file
 * (or --allow-missing was passed), non-zero otherwise.
 */
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const PARITY_DIR = path.join(REPO, 'docs', 'entity-parity');
const RAW_DIR = path.join(PARITY_DIR, 'raw');
const INPUT_FILE = path.join(PARITY_DIR, 'audit-input.json');
const MATRIX_JSON = path.join(PARITY_DIR, 'parity-matrix.json');
const MATRIX_MD = path.join(PARITY_DIR, 'PARITY_MATRIX.md');
const BASELINE_MD = path.join(PARITY_DIR, 'BASELINE.md');

const allowMissing = process.argv.includes('--allow-missing');
const quiet = process.argv.includes('--quiet');

// The 11 audit facets, in report order. Aliases tolerate spelling drift in agent output.
const FACETS = [
    { key: 'registration', label: '注册', aliases: ['registration', 'registration_identity', 'identity'] },
    { key: 'attributes', label: '属性', aliases: ['attributes', 'attribute', 'stats'] },
    { key: 'ai', label: 'AI', aliases: ['ai', 'ai_goals', 'goals'] },
    { key: 'behaviors', label: '行为', aliases: ['behaviors', 'behaviour', 'behavior', 'skills'] },
    { key: 'damage_and_effects', label: '伤害/效果', aliases: ['damage_and_effects', 'damage_effects', 'damage', 'effects'] },
    { key: 'sync_data', label: '同步数据', aliases: ['sync_data', 'data_sync', 'synched_data', 'data'] },
    { key: 'animation', label: '动画', aliases: ['animation', 'animations'] },
    { key: 'model_texture', label: '模型/贴图', aliases: ['model_texture', 'model_and_texture', 'model', 'visuals', 'renderer'] },
    { key: 'sounds', label: '音效', aliases: ['sounds', 'sound', 'audio'] },
    { key: 'spawning', label: '生成', aliases: ['spawning', 'spawn', 'spawn_rules'] },
    { key: 'loot', label: '掉落', aliases: ['loot', 'drops', 'loot_table'] },
];

const VERDICTS = ['satisfied', 'partial', 'missing', 'na'];
const SCORE = { satisfied: 1, partial: 0.5, missing: 0 };

function normalizeFacetName(name) {
    const clean = String(name || '').trim().toLowerCase().replace(/[\s-]+/g, '_');
    for (const facet of FACETS) {
        if (facet.key === clean || facet.aliases.includes(clean)) return facet.key;
    }
    return null;
}

function pct(part, total) {
    if (!total) return 0;
    return Math.round((part / total) * 1000) / 10;
}

const input = JSON.parse(fs.readFileSync(INPUT_FILE, 'utf8'));
const byId = new Map(input.creatures.map((c) => [c.id, c]));

// Optional group manifest (batch view) — reuse scripts/entity-port-manifest.cjs when present.
const groups = new Map();
const manifestFile = path.join(REPO, 'scripts', 'entity-port-manifest.cjs');
if (fs.existsSync(manifestFile)) {
    try {
        const manifest = require(manifestFile);
        const groupSource = typeof manifest.groups === 'object' ? manifest.groups : manifest;
        for (const [group, ids] of Object.entries(groupSource)) {
            if (!Array.isArray(ids)) continue;
            for (const id of ids) groups.set(id, group);
        }
    } catch (error) {
        if (!quiet) console.warn(`warn: cannot load entity-port-manifest.cjs (${error.message})`);
    }
}

const problems = [];
const warnings = [];
const creatures = [];

for (const entry of input.creatures) {
    const rawFile = path.join(RAW_DIR, `${entry.id}.json`);
    if (!fs.existsSync(rawFile)) {
        problems.push(`missing audit: ${entry.id}`);
        creatures.push({ ...entry, audit: null, missing: true });
        continue;
    }
    let raw;
    try {
        raw = JSON.parse(fs.readFileSync(rawFile, 'utf8'));
    } catch (error) {
        problems.push(`invalid JSON: ${entry.id} (${error.message})`);
        creatures.push({ ...entry, audit: null, missing: true });
        continue;
    }

    const facetStats = new Map();
    for (const facet of FACETS) facetStats.set(facet.key, { satisfied: 0, partial: 0, missing: 0, na: 0, clauses: 0 });
    const seen = new Set();
    for (const facet of raw.facets || []) {
        const key = normalizeFacetName(facet.name);
        if (!key) {
            warnings.push(`${entry.id}: unknown facet "${facet.name}"`);
            continue;
        }
        if (seen.has(key)) warnings.push(`${entry.id}: duplicate facet "${key}"`);
        seen.add(key);
        const stats = facetStats.get(key);
        for (const clause of facet.clauses || []) {
            const verdict = String(clause.verdict || '').trim().toLowerCase();
            if (!VERDICTS.includes(verdict)) {
                problems.push(`${entry.id}/${key}: invalid verdict "${clause.verdict}"`);
                continue;
            }
            const evidence = clause.evidence || {};
            if (!evidence.original) problems.push(`${entry.id}/${key}: clause without original evidence ("${clause.clause}")`);
            // Protocol §4: a null `project` is only acceptable when the clause is not satisfied.
            if (verdict === 'satisfied' && !evidence.project) {
                problems.push(`${entry.id}/${key}: satisfied without project evidence ("${clause.clause}")`);
            }
            stats[verdict] += 1;
            stats.clauses += 1;
        }
    }
    for (const facet of FACETS) {
        if (!seen.has(facet.key)) warnings.push(`${entry.id}: facet "${facet.key}" not audited`);
    }

    const totals = { satisfied: 0, partial: 0, missing: 0, na: 0 };
    for (const stats of facetStats.values()) {
        totals.satisfied += stats.satisfied;
        totals.partial += stats.partial;
        totals.missing += stats.missing;
        totals.na += stats.na;
    }
    const denominator = totals.satisfied + totals.partial + totals.missing;
    const completion = denominator ? pct(totals.satisfied + 0.5 * totals.partial, denominator) : 0;

    creatures.push({
        ...entry,
        group: groups.get(entry.id) || null,
        missing: false,
        totals,
        denominator,
        completion,
        facets: Object.fromEntries([...facetStats.entries()]),
        missingSummary: raw.missingSummary || [],
        confidence: raw.confidence || null,
    });
}

// ---- aggregation ---------------------------------------------------------

const audited = creatures.filter((c) => !c.missing);
const overall = { satisfied: 0, partial: 0, missing: 0, na: 0 };
for (const c of audited) {
    overall.satisfied += c.totals.satisfied;
    overall.partial += c.totals.partial;
    overall.missing += c.totals.missing;
    overall.na += c.totals.na;
}
const overallDenominator = overall.satisfied + overall.partial + overall.missing;
const overallCompletion = overallDenominator ? pct(overall.satisfied + 0.5 * overall.partial, overallDenominator) : 0;

const facetAggregate = FACETS.map((facet) => {
    const totals = { satisfied: 0, partial: 0, missing: 0, na: 0 };
    for (const c of audited) {
        const stats = c.facets[facet.key];
        if (!stats) continue;
        totals.satisfied += stats.satisfied;
        totals.partial += stats.partial;
        totals.missing += stats.missing;
        totals.na += stats.na;
    }
    const denominator = totals.satisfied + totals.partial + totals.missing;
    return {
        facet: facet.key,
        label: facet.label,
        ...totals,
        denominator,
        completion: denominator ? pct(totals.satisfied + 0.5 * totals.partial, denominator) : 0,
    };
});

const groupAggregate = [...new Set([...groups.values()])].map((group) => {
    const members = audited.filter((c) => c.group === group);
    const totals = { satisfied: 0, partial: 0, missing: 0, na: 0 };
    for (const c of members) {
        totals.satisfied += c.totals.satisfied;
        totals.partial += c.totals.partial;
        totals.missing += c.totals.missing;
        totals.na += c.totals.na;
    }
    const denominator = totals.satisfied + totals.partial + totals.missing;
    return {
        group,
        audited: members.length,
        expected: [...groups.values()].filter((g) => g === group).length,
        ...totals,
        denominator,
        completion: denominator ? pct(totals.satisfied + 0.5 * totals.partial, denominator) : 0,
    };
}).sort((a, b) => a.completion - b.completion);

const payload = {
    generatedAt: new Date().toISOString(),
    protocol: 'docs/entity-parity/AUDIT_PROTOCOL.md',
    input: 'docs/entity-parity/audit-input.json',
    counts: {
        expected: input.creatures.length,
        audited: audited.length,
        missing: input.creatures.length - audited.length,
    },
    overall: { ...overall, denominator: overallDenominator, completion: overallCompletion },
    facets: facetAggregate,
    groups: groupAggregate,
    creatures,
};

fs.writeFileSync(MATRIX_JSON, JSON.stringify(payload, null, 2));

// ---- PARITY_MATRIX.md ---------------------------------------------------

function cell(stats) {
    if (!stats || !stats.clauses) return '·';
    if (stats.missing > 0) return '❌';
    if (stats.partial > 0) return '🟠';
    return '✅';
}

const md = [];
md.push('# 生物还原矩阵（SRParasites 1.10.9 → csrp）');
md.push('');
md.push(`> 生成时间：${payload.generatedAt}；方法见 \`docs/entity-parity/AUDIT_PROTOCOL.md\`，逐生物明细见 \`docs/entity-parity/raw/<id>.json\`。`);
md.push('> 判定：✅ 全部条款满足；🟠 有部分实现但无缺失；❌ 存在缺失；· 未审计。');
md.push('');
md.push(`- 注册生物总数：**${payload.counts.expected}**；已审计：**${payload.counts.audited}**；未审计：**${payload.counts.missing}**`);
md.push(`- 条款总计：满足 ${overall.satisfied} / 部分 ${overall.partial} / 缺失 ${overall.missing}（不计入 ${overall.na} 条不适用）`);
md.push(`- **加权完成度：${overallCompletion}%**（partial 计 0.5）`);
md.push('');
md.push('## 分面完成度');
md.push('');
md.push('| 面 | 满足 | 部分 | 缺失 | 完成度 |');
md.push('| --- | ---: | ---: | ---: | ---: |');
for (const facet of facetAggregate) {
    md.push(`| ${facet.label} \`${facet.facet}\` | ${facet.satisfied} | ${facet.partial} | ${facet.missing} | ${facet.completion}% |`);
}
md.push('');
md.push('## 分组完成度');
md.push('');
md.push('| 分组 | 已审计/应有 | 满足 | 部分 | 缺失 | 完成度 |');
md.push('| --- | ---: | ---: | ---: | ---: | ---: |');
for (const group of groupAggregate) {
    md.push(`| ${group.group} | ${group.audited}/${group.expected} | ${group.satisfied} | ${group.partial} | ${group.missing} | ${group.completion}% |`);
}
md.push('');
md.push('## 逐生物矩阵');
md.push('');
md.push('| id | 原版类 | 工程类 | ' + FACETS.map((f) => f.label).join(' | ') + ' | 完成度 |');
md.push('| --- | --- | --- | ' + FACETS.map(() => '---').join(' | ') + ' | ---: |');
for (const c of [...creatures].sort((a, b) => (a.completion ?? -1) - (b.completion ?? -1))) {
    if (c.missing) {
        md.push(`| \`${c.id}\` | ${c.originalClass} | ${c.projectClass || '?'} | ` + FACETS.map(() => '·').join(' | ') + ' | 未审计 |');
        continue;
    }
    md.push(
        `| \`${c.id}\` | ${c.originalClass} | ${c.projectClass || '?'} | ` +
        FACETS.map((f) => cell(c.facets[f.key])).join(' | ') +
        ` | ${c.completion}% |`,
    );
}
md.push('');
md.push('## 缺口清单（按完成度升序）');
md.push('');
for (const c of [...creatures].sort((a, b) => (a.completion ?? -1) - (b.completion ?? -1))) {
    if (c.missing) continue;
    const gaps = c.missingSummary.slice(0, 6);
    md.push(`### \`${c.id}\`（${c.originalClass} → ${c.projectClass}，${c.completion}%）`);
    if (!gaps.length) md.push('- （无缺口摘要，见 raw JSON）');
    for (const gap of gaps) md.push(`- ${gap}`);
    md.push('');
}
fs.writeFileSync(MATRIX_MD, md.join('\n'));

// ---- BASELINE.md --------------------------------------------------------

const worst = [...audited].sort((a, b) => a.completion - b.completion).slice(0, 20);
const baseline = [];
baseline.push('# 生物部分还原度基线');
baseline.push('');
baseline.push(`> 生成时间：${payload.generatedAt}`);
baseline.push(`> 事实来源：\`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites\`（SRParasites 1.10.9，生物部分与 1.10.8 一致）`);
baseline.push(`> 本工程：\`D:/code/MC模组/csrp\`（MC 26.3 / NeoForge 26.3，分支 \`port-26.3\`）`);
baseline.push('');
baseline.push('## 方法与口径');
baseline.push('');
baseline.push('- 审计单元 = `SRPEntities.CreateEntityMob` 的 127 个注册项，逐生物走完双方继承链（父类共用行为计入该生物）。');
baseline.push('- 11 个审计面：' + FACETS.map((f) => f.label).join('、') + '。');
baseline.push('- 条款判定 `satisfied` / `partial` / `missing`，`na` 不计入分母；每条判定必须带原版与（满足时的）本工程证据行号。');
baseline.push('- 完成度 = (satisfied + 0.5 × partial) / (satisfied + partial + missing)。');
baseline.push('- 复跑：`node scripts/entity-parity/build-entity-parity-input.cjs && node scripts/entity-parity/verify-entity-parity.cjs`。');
baseline.push('');
baseline.push('## 总体基线');
baseline.push('');
baseline.push(`- 覆盖：**${payload.counts.audited}/${payload.counts.expected}** 只生物已出条款级审计`);
baseline.push(`- 条款：满足 **${overall.satisfied}**、部分 **${overall.partial}**、缺失 **${overall.missing}**（另有 ${overall.na} 条判定为不适用）`);
baseline.push(`- **加权完成度：${overallCompletion}%**`);
baseline.push('');
baseline.push('## 分面基线');
baseline.push('');
baseline.push('| 面 | 满足 | 部分 | 缺失 | 完成度 |');
baseline.push('| --- | ---: | ---: | ---: | ---: |');
for (const facet of facetAggregate) {
    baseline.push(`| ${facet.label} | ${facet.satisfied} | ${facet.partial} | ${facet.missing} | ${facet.completion}% |`);
}
baseline.push('');
baseline.push('## 完成度最低的 20 只（补齐队列起点）');
baseline.push('');
baseline.push('| id | 原版类 | 工程类 | 完成度 | 满足/部分/缺失 |');
baseline.push('| --- | --- | --- | ---: | --- |');
for (const c of worst) {
    baseline.push(`| \`${c.id}\` | ${c.originalClass} | ${c.projectClass} | ${c.completion}% | ${c.totals.satisfied}/${c.totals.partial}/${c.totals.missing} |`);
}
baseline.push('');
baseline.push('## 已知前提与风险');
baseline.push('');
baseline.push('- 本基线的审计对象是 `csrp` 当前工作树（`port-26.3`）的源码与资源；');
baseline.push('- 该分支的 26.3 迁移尚未完成，`gradlew build` 当前失败，因此本轮基线只覆盖「源码与资源层还原度」，不含运行时验证；');
baseline.push('- 审计由代理逐条比对得出，`confidence` 字段标注了每只生物的把握程度；低把握条目应在补齐时复核。');
baseline.push('');
fs.writeFileSync(BASELINE_MD, baseline.join('\n'));

if (!quiet) {
    console.log(`audited ${payload.counts.audited}/${payload.counts.expected} creatures, overall completion ${overallCompletion}%`);
    console.log(`wrote ${path.relative(REPO, MATRIX_JSON)}, ${path.relative(REPO, MATRIX_MD)}, ${path.relative(REPO, BASELINE_MD)}`);
    if (warnings.length) console.log(`warnings: ${warnings.length}`);
    for (const warning of warnings.slice(0, 20)) console.log(`  w: ${warning}`);
    if (warnings.length > 20) console.log(`  ... ${warnings.length - 20} more`);
}

if (problems.length) {
    console.error(`problems: ${problems.length}`);
    for (const problem of problems.slice(0, 40)) console.error(`  - ${problem}`);
    if (problems.length > 40) console.error(`  ... ${problems.length - 40} more`);
    if (!allowMissing) process.exitCode = 1;
} else if (payload.counts.missing > 0 && !allowMissing) {
    console.error(`${payload.counts.missing} creature(s) still unaudited`);
    process.exitCode = 1;
}
