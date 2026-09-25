#!/usr/bin/env node
/*
 * Merges the system-layer audits (docs/entity-parity/system/<area>.json) into
 *   docs/entity-parity/SYSTEM_GAPS.md  — 系统层缺口清单与补齐批次建议
 * and validates their structure (verdicts, evidence, impact).
 *
 * Usage:
 *   node scripts/entity-parity/verify-system-parity.cjs [--allow-missing]
 */
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const SYSTEM_DIR = path.join(REPO, 'docs', 'entity-parity', 'system');
const OUT_FILE = path.join(REPO, 'docs', 'entity-parity', 'SYSTEM_GAPS.md');

const EXPECTED_AREAS = [
    'base_classes',
    'damage_effects',
    'ai_inventory',
    'attributes_genes',
    'sync_data',
    'spawning',
    'loot_drops',
    'animation_model',
];

const VERDICTS = ['satisfied', 'partial', 'missing', 'na'];
const IMPACTS = ['high', 'medium', 'low'];
const allowMissing = process.argv.includes('--allow-missing');

const problems = [];
const audits = [];

for (const area of EXPECTED_AREAS) {
    const file = path.join(SYSTEM_DIR, `${area}.json`);
    if (!fs.existsSync(file)) {
        problems.push(`missing system audit: ${area}`);
        continue;
    }
    let data;
    try {
        data = JSON.parse(fs.readFileSync(file, 'utf8'));
    } catch (error) {
        problems.push(`invalid JSON: ${area} (${error.message})`);
        continue;
    }
    for (const clause of data.clauses || []) {
        const verdict = String(clause.verdict || '').toLowerCase();
        if (!VERDICTS.includes(verdict)) problems.push(`${area}: invalid verdict "${clause.verdict}"`);
        if (clause.impact && !IMPACTS.includes(String(clause.impact).toLowerCase())) {
            problems.push(`${area}: invalid impact "${clause.impact}"`);
        }
        const evidence = clause.evidence || {};
        if (!evidence.original) problems.push(`${area}: clause without original evidence ("${clause.clause}")`);
        if (verdict === 'satisfied' && !evidence.project) {
            problems.push(`${area}: satisfied without project evidence ("${clause.clause}")`);
        }
    }
    audits.push(data);
}

const rank = { high: 0, medium: 1, low: 2 };
const md = [];
md.push('# 系统层还原缺口（补齐批次依据）');
md.push('');
md.push(`> 生成时间：${new Date().toISOString()}；来源：\`docs/entity-parity/system/*.json\`。`);
md.push('> 逐生物缺口见 `PARITY_MATRIX.md`，本文件给出共用系统的缺口与影响面。');
md.push('');
md.push('## 总览');
md.push('');
md.push('| area | 满足 | 部分 | 缺失 | 不适用 | 完成度 | 高影响缺口 |');
md.push('| --- | ---: | ---: | ---: | ---: | ---: | ---: |');
for (const audit of audits) {
    const totals = { satisfied: 0, partial: 0, missing: 0, na: 0 };
    let high = 0;
    for (const clause of audit.clauses || []) {
        const verdict = String(clause.verdict || '').toLowerCase();
        if (totals[verdict] !== undefined) totals[verdict] += 1;
        if ((verdict === 'missing' || verdict === 'partial') && String(clause.impact || '').toLowerCase() === 'high') high += 1;
    }
    const denominator = totals.satisfied + totals.partial + totals.missing;
    const completion = denominator ? Math.round(((totals.satisfied + 0.5 * totals.partial) / denominator) * 1000) / 10 : 0;
    md.push(`| \`${audit.area}\` | ${totals.satisfied} | ${totals.partial} | ${totals.missing} | ${totals.na} | ${completion}% | ${high} |`);
}
md.push('');

for (const audit of audits) {
    md.push(`## ${audit.title || audit.area}`);
    md.push('');
    if (audit.summary) {
        md.push(`**结论**：${audit.summary}`);
        md.push('');
    }
    if (audit.originalRefs || audit.projectRefs) {
        md.push(`- 原版：${(audit.originalRefs || []).map((r) => '`' + r + '`').join('、') || '—'}`);
        md.push(`- 工程：${(audit.projectRefs || []).map((r) => '`' + r + '`').join('、') || '—'}`);
        md.push('');
    }
    const open = (audit.clauses || [])
        .filter((c) => ['missing', 'partial'].includes(String(c.verdict || '').toLowerCase()))
        .sort((a, b) => rank[String(a.impact || 'low')] - rank[String(b.impact || 'low')]);
    if (!open.length) {
        md.push('（无缺口）');
        md.push('');
        continue;
    }
    md.push('| 影响 | 条款 | 判定 | 影响生物组 | 原版证据 | 工程证据 | 备注 |');
    md.push('| --- | --- | --- | --- | --- | --- | --- |');
    for (const clause of open) {
        const impact = String(clause.impact || 'low');
        const badge = impact === 'high' ? '🔴 high' : impact === 'medium' ? '🟠 medium' : '🟡 low';
        const groups = (clause.affectedGroups || []).join('、') || '—';
        const original = clause.evidence && clause.evidence.original ? clause.evidence.original : '—';
        const project = clause.evidence && clause.evidence.project ? clause.evidence.project : '—';
        md.push(`| ${badge} | ${String(clause.clause || '').replace(/\|/g, '\\|')} | ${clause.verdict} | ${groups} | \`${original}\` | \`${project}\` | ${String(clause.note || '').replace(/\|/g, '\\|')} |`);
    }
    md.push('');
}

fs.writeFileSync(OUT_FILE, md.join('\n'));

const totalsAll = { satisfied: 0, partial: 0, missing: 0, high: 0 };
for (const audit of audits) {
    for (const clause of audit.clauses || []) {
        const verdict = String(clause.verdict || '').toLowerCase();
        if (totalsAll[verdict] !== undefined) totalsAll[verdict] += 1;
        if ((verdict === 'missing' || verdict === 'partial') && String(clause.impact || '').toLowerCase() === 'high') totalsAll.high += 1;
    }
}
console.log(`system audits: ${audits.length}/${EXPECTED_AREAS.length}`);
console.log(`clauses: satisfied ${totalsAll.satisfied}, partial ${totalsAll.partial}, missing ${totalsAll.missing} (high-impact open: ${totalsAll.high})`);
console.log(`wrote ${path.relative(REPO, OUT_FILE)}`);
if (problems.length) {
    console.error(`problems: ${problems.length}`);
    for (const problem of problems.slice(0, 30)) console.error(`  - ${problem}`);
    if (!allowMissing) process.exitCode = 1;
}
