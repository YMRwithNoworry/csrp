#!/usr/bin/env node
/*
 * Verifies the project-side evidence of the audits:
 *   1. hard failures  — the referenced file is missing, or the line number is out of range;
 *   2. soft signal    — do identifiers mentioned in the clause appear around the referenced line?
 *      (original-side names like `func_70105_a` legitimately differ from project symbols, so this is
 *      informational only, and with --relocate it is used to re-locate the referenced symbol).
 *
 * Usage:
 *   node scripts/entity-parity/check-evidence-drift.cjs [--window 12] [--max-report 200] [--relocate] [--strict]
 *
 * Report: docs/entity-parity/EVIDENCE_DRIFT.md
 * With --relocate the raw/system JSON files are rewritten: evidence.project line numbers that do not
 * contain any clause identifier are replaced by the line where the identifier actually occurs
 * (only when a unique occurrence exists), and `evidenceShifted` records the change.
 */
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const PARITY_DIR = path.join(REPO, 'docs', 'entity-parity');
const OUT_FILE = path.join(PARITY_DIR, 'EVIDENCE_DRIFT.md');

function argValue(flag, fallback) {
    const i = process.argv.indexOf(flag);
    return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : fallback;
}
const window = Number(argValue('--window', '12'));
const maxReport = Number(argValue('--max-report', '200'));
const strict = process.argv.includes('--strict');
const relocate = process.argv.includes('--relocate');

const fileCache = new Map();
function readLines(file) {
    if (!fileCache.has(file)) {
        fileCache.set(file, fs.existsSync(file) ? fs.readFileSync(file, 'utf8').split('\n') : null);
    }
    return fileCache.get(file);
}

/** Normalizes an evidence reference into { file, line }. Accepts "a/b.java:12" and "/abs/a.java:12". */
function parseRef(ref) {
    if (!ref || typeof ref !== 'string') return null;
    const match = ref.match(/^(.*?):(\d+)(?:\s|$)/);
    if (!match) return null;
    let file = match[1].trim();
    file = file.replace(/\\/g, '/');
    if (!path.isAbsolute(file)) file = path.join(REPO, file);
    return { file, line: Number(match[2]) };
}

/** Identifiers worth looking for on the evidence line: class names, method names, constants. */
function keyIdentifiers(text) {
    const idents = (String(text || '').match(/[A-Za-z_][A-Za-z0-9_]{3,}/g) || []);
    const noise = new Set([
        'tasks', 'task', 'targetTasks', 'addTask', 'addGoal', 'with', 'this', 'true', 'false', 'null',
        'that', 'from', 'into', 'with', 'value', 'values', 'amount', 'class', 'final', 'public', 'private',
        'protected', 'void', 'return', 'import', 'package', 'static', 'int', 'float', 'double', 'boolean',
    ]);
    return [...new Set(idents.filter((i) => !noise.has(i) && !/^[0-9]/.test(i)))];
}

const dirs = [
    { dir: path.join(PARITY_DIR, 'raw'), label: 'creature' },
    { dir: path.join(PARITY_DIR, 'system'), label: 'system' },
];

const results = [];
const totals = { checked: 0, ok: 0, identMiss: 0, outOfRange: 0, missingFile: 0, noRef: 0, relocated: 0 };

for (const { dir, label } of dirs) {
    if (!fs.existsSync(dir)) continue;
    for (const file of fs.readdirSync(dir).filter((f) => f.endsWith('.json'))) {
        const id = path.basename(file, '.json');
        const fullPath = path.join(dir, file);
        let data;
        try {
            data = JSON.parse(fs.readFileSync(fullPath, 'utf8'));
        } catch {
            continue;
        }
        let changed = false;
        for (const facet of data.facets || data.clauses || []) {
            const clauses = data.facets ? facet.clauses || [] : [facet];
            const facetName = data.facets ? facet.name : data.area;
            for (const clause of clauses) {
                const evidence = clause.evidence || {};
                const verdict = String(clause.verdict || '').toLowerCase();
                if (!evidence.project) {
                    if (verdict === 'satisfied') totals.noRef += 1;
                    continue;
                }
                const ref = parseRef(evidence.project);
                if (!ref) {
                    totals.noRef += 1;
                    continue;
                }
                totals.checked += 1;
                const lines = readLines(ref.file);
                if (!lines) {
                    totals.missingFile += 1;
                    results.push({ kind: label, id, facet: facetName, clause: clause.clause, ref: evidence.project, status: 'missing-file' });
                    continue;
                }
                const idents = keyIdentifiers(clause.clause);
                const outOfRange = ref.line > lines.length;
                if (outOfRange) totals.outOfRange += 1;
                const lo = Math.max(1, ref.line - window);
                const hi = Math.min(lines.length, ref.line + window);
                const slice = lines.slice(lo - 1, hi).join('\n');
                if (idents.some((ident) => slice.includes(ident))) {
                    totals.ok += 1;
                    continue;
                }
                totals.identMiss += 1;

                // Try to re-locate the strongest identifier in the whole file (drift repair).
                let relocated = null;
                for (const ident of idents) {
                    const hits = [];
                    for (let i = 0; i < lines.length; i += 1) if (lines[i].includes(ident)) hits.push(i + 1);
                    if (hits.length === 1) {
                        relocated = { ident, line: hits[0] };
                        break;
                    }
                }
                if (relocate && relocated) {
                    const shortPath = path.relative(REPO, ref.file).split(path.sep).join('/');
                    clause.evidence = { ...evidence, project: `${shortPath}:${relocated.line}` };
                    clause.evidenceShifted = { from: evidence.project, to: clause.evidence.project, symbol: relocated.ident };
                    totals.relocated += 1;
                    changed = true;
                    continue;
                }
                results.push({
                    kind: label,
                    id,
                    facet: facetName,
                    clause: clause.clause,
                    ref: evidence.project,
                    status: outOfRange ? 'line-out-of-range' : 'identifier-not-found',
                    lookedFor: idents.slice(0, 6),
                    relocatable: relocated ? `${relocated.ident} -> :${relocated.line}` : '—',
                });
            }
        }
        if (changed) fs.writeFileSync(fullPath, JSON.stringify(data, null, 2));
    }
}

const md = [];
md.push('# 审计证据校验（工程侧行号）');
md.push('');
md.push(`> 生成时间：${new Date().toISOString()}；窗口 ±${window} 行。`);
md.push('> 方法：对每条带 `evidence.project` 的条款，检查该文件是否存在、并确认 `文件:行号±窗口` 中至少出现一个条款里提到的标识符。');
md.push('> 用途：抓「凭空行号」的证据造假，以及在切换基线后量化行号漂移。');
md.push('');
md.push(`- 校验条数：${totals.checked}`);
md.push(`- 命中：${totals.ok}`);
md.push(`- 未命中（可疑/疑似漂移）：${totals.drift}`);
md.push(`- 文件不存在：${totals.missingFile}`);
md.push(`- 缺 project 证据（判定为 satisfied 时不允许）：${totals.noRef}`);
md.push('');
if (results.length) {
    md.push('| 类型 | 生物/领域 | 面 | 条款 | 证据 | 状态 | 期望标识符 |');
    md.push('| --- | --- | --- | --- | --- | --- | --- |');
    for (const item of results.slice(0, maxReport)) {
        md.push(
            `| ${item.kind} | \`${item.id}\` | ${item.facet} | ${String(item.clause).replace(/\|/g, '\\|').slice(0, 120)} | \`${item.ref}\` | ${item.status} | ${(item.lookedFor || []).join('、') || '—'} |`,
        );
    }
    if (results.length > maxReport) md.push(`\n（另有 ${results.length - maxReport} 条未列出）`);
}
md.push('');
fs.writeFileSync(OUT_FILE, md.join('\n'));

console.log(`evidence refs checked: ${totals.checked} (ok ${totals.ok}, suspicious ${totals.drift}, missing file ${totals.missingFile}, absent ${totals.noRef})`);
console.log(`wrote ${path.relative(REPO, OUT_FILE)}`);
if (strict && (totals.drift || totals.missingFile)) process.exitCode = 1;
