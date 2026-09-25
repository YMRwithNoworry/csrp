#!/usr/bin/env node
/*
 * Independent cross-check of the per-creature audits against the original sources.
 *
 * It does NOT trust the agents' prose: for every `ai` clause it extracts the referenced
 * AI class names and verifies that the class is actually registered somewhere along that
 * creature's decompiled inheritance chain (`new <AiClass>(`).
 *
 * Rationale (measured, see docs/entity-parity/original-ai-baseline.json): 110 of the 127
 * creatures override `func_184651_r` (initEntityAI) WITHOUT calling `super.func_184651_r()`,
 * so the vanilla EntityMob baseline goals (melee / wander / watch closest / look idle /
 * hurt by / nearest player) are NOT registered for them. Audits that list such tasks as
 * "inherited from EntityMob" are therefore false positives.
 *
 * Usage:
 *   node scripts/entity-parity/cross-check-audit-claims.cjs            # report only
 *   node scripts/entity-parity/cross-check-audit-claims.cjs --apply    # rewrite raw/*.json
 *
 * Report: docs/entity-parity/AUDIT_CROSSCHECK.md
 * --apply marks unverifiable `ai` premises as `na` with a note + evidence, and stamps a
 *         `groundTruth` field ({ verified, file, line }) on verified clauses.
 */
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const PARITY_DIR = path.join(REPO, 'docs', 'entity-parity');
const RAW_DIR = path.join(PARITY_DIR, 'raw');
const OUT_FILE = path.join(PARITY_DIR, 'AUDIT_CROSSCHECK.md');
const apply = process.argv.includes('--apply');

const input = JSON.parse(fs.readFileSync(path.join(PARITY_DIR, 'audit-input.json'), 'utf8'));
const chainById = new Map(input.creatures.map((c) => [c.id, c.originalChain || []]));

// Vanilla EntityMob#initEntityAI registers these; they are only present when the creature's
// chain does not replace initEntityAI without calling super (see original-ai-baseline.json).
const VANILLA_ENTITY_MOB_TASKS = new Set([
    'EntityAIAttackMelee',
    'EntityAIWanderAvoidWater',
    'EntityAIWatchClosest',
    'EntityAILookIdle',
    'EntityAIHurtByTarget',
    'EntityAINearestAttackableTarget',
]);
const baselineFile = path.join(PARITY_DIR, 'original-ai-baseline.json');
const vanillaBaselineApplies = new Map();
if (fs.existsSync(baselineFile)) {
    const baseline = JSON.parse(fs.readFileSync(baselineFile, 'utf8'));
    for (const creature of baseline.creatures || []) vanillaBaselineApplies.set(creature.id, !!creature.baselineApplies);
}

// Ground truth: the tasks the original really registers along each creature's chain
// (docs/entity-parity/original-ai-tasks.json, from extract-original-ai-tasks.cjs).
// Includes base-class constructor registrations, which count for every creature of that tier.
const truthFile = path.join(PARITY_DIR, 'original-ai-tasks.json');
const originalTasksById = new Map();
if (fs.existsSync(truthFile)) {
    const truth = JSON.parse(fs.readFileSync(truthFile, 'utf8'));
    for (const creature of truth.creatures || []) {
        const map = new Map();
        for (const task of creature.tasks || []) if (!map.has(task.task)) map.set(task.task, task);
        originalTasksById.set(creature.id, map);
    }
}

const fileCache = new Map();
function readSource(file) {
    if (!fileCache.has(file)) {
        fileCache.set(file, fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
    }
    return fileCache.get(file);
}

/** Finds `new <optional outer prefix>.<token>(` in the chain and returns { file, line } of the first hit. */
function findRegistration(chain, token) {
    const pattern = new RegExp(`new\\s+(?:[A-Za-z0-9_$]+\\.)*${token}\\s*\\(`);
    for (const link of chain) {
        if (!link.file) continue;
        const src = readSource(link.file);
        const match = pattern.exec(src);
        if (match) {
            const line = src.slice(0, match.index).split('\n').length;
            return { file: link.file, line, inClass: link.class };
        }
    }
    return null;
}

const AI_TOKEN = /EntityAI[A-Za-z0-9_]*/g;

const report = { checked: 0, verified: 0, refuted: 0, uncovered: 0, creatures: [], uncoveredCreatures: [], applied: 0 };
const files = fs.existsSync(RAW_DIR) ? fs.readdirSync(RAW_DIR).filter((f) => f.endsWith('.json')) : [];

for (const file of files) {
    const id = path.basename(file, '.json');
    const fullPath = path.join(RAW_DIR, file);
    let data;
    try {
        data = JSON.parse(fs.readFileSync(fullPath, 'utf8'));
    } catch {
        continue;
    }
    const chain = chainById.get(id) || [];
    let changed = false;
    const creatureReport = { id, refuted: [], verified: 0 };

    for (const facet of data.facets || []) {
        if (String(facet.name || '').toLowerCase() !== 'ai') continue;
        for (const clause of facet.clauses || []) {
            const text = String(clause.clause || '');
            const rawTokens = [...new Set(text.match(AI_TOKEN) || [])];
            // Auditors often write the short form ("NearestAttackableTargetStatus", "CircleGroup");
            // accept a ground-truth task when either its full or short name appears in the clause.
            const truthForClause = originalTasksById.get(id) || new Map();
            for (const token of truthForClause.keys()) {
                const short = token.replace(/^EntityAI/, '');
                if (short.length > 4 && text.includes(short)) rawTokens.push(token);
            }
            const tokens = [...new Set(rawTokens)];
            if (!tokens.length) continue;
            report.checked += 1;
            const truth = originalTasksById.get(id) || new Map();
            const hits = tokens.map((token) => {
                if (truth.has(token)) {
                    const task = truth.get(token);
                    return { token, hit: { file: task.file, line: task.line, inClass: task.inheritedFrom } };
                }
                const direct = findRegistration(chain, token);
                if (direct) return { token, hit: direct };
                if (VANILLA_ENTITY_MOB_TASKS.has(token) && vanillaBaselineApplies.get(id)) {
                    return { token, hit: { file: 'net.minecraft.entity.monster.EntityMob (vanilla)', line: null, inClass: 'EntityMob' } };
                }
                return { token, hit: null };
            });
            const missing = hits.filter((h) => !h.hit);
            if (!missing.length) {
                report.verified += 1;
                creatureReport.verified += 1;
                if (apply && !clause.groundTruth) {
                    clause.groundTruth = { verified: true, file: hits[0].hit.file, line: hits[0].hit.line };
                    changed = true;
                }
                continue;
            }
            // Premise cannot be backed by the original sources: the task is not registered on this
            // creature's chain at all.
            report.refuted += 1;
            creatureReport.refuted.push({
                clause: clause.clause,
                verdict: clause.verdict,
                tokens: missing.map((m) => m.token),
            });
            if (apply && clause.verdict !== 'na') {
                clause.reviewNote = clause.reviewNote
                    ? `${clause.reviewNote} ` : '';
                clause.reviewNote +=
                    `cross-check: 原版继承链上未注册 ${missing.map((m) => m.token).join('、')}（initEntityAI 覆盖且未调用 super，见 original-ai-baseline.json），该条款不成立。`;
                clause.verdict = 'na';
                clause.evidence = { ...(clause.evidence || {}), project: null };
                clause.groundTruth = { verified: false, file: hits.find((h) => h.hit)?.hit?.file || null, line: null };
                report.applied += 1;
                changed = true;
            }
        }
    }

    if (creatureReport.refuted.length || creatureReport.verified) report.creatures.push(creatureReport);

    // Coverage: original tasks the audit never mentions at all in its `ai` facet.
    const truth = originalTasksById.get(id);
    if (truth && truth.size) {
        const mentioned = new Set();
        for (const facet of data.facets || []) {
            if (String(facet.name || '').toLowerCase() !== 'ai') continue;
            for (const clause of facet.clauses || []) {
                const text = String(clause.clause || '');
                for (const token of text.match(AI_TOKEN) || []) mentioned.add(token);
                for (const token of truth.keys()) {
                    const short = token.replace(/^EntityAI/, '');
                    if (short.length > 4 && text.includes(short)) mentioned.add(token);
                }
            }
        }
        const uncovered = [...truth.keys()].filter((token) => {
            if (mentioned.has(token)) return false;
            const short = token.replace(/^EntityAI/, '');
            return !(short.length > 4 && mentioned.has(short));
        });
        if (uncovered.length) {
            report.uncoveredCreatures.push({ id, uncovered: uncovered.map((token) => ({ token, ...truth.get(token) })) });
            report.uncovered += uncovered.length;
        }
    }

    if (apply && changed) fs.writeFileSync(fullPath, JSON.stringify(data, null, 2));
}

const md = [];
md.push('# 审计交叉校验（AI 条款 premise 校验）');
md.push('');
md.push(`> 生成时间：${new Date().toISOString()}；模式：${apply ? '**已回写 raw/**' : '仅报告'}。`);
md.push('> 方法：对每条 `ai` 条款抽取被引用的 AI 类名，与该生物的原版任务地面真值（`original-ai-tasks.json`）比对，');
md.push('> 并回落到继承链上的 `new <类名>(` 注册点；两者都找不到即为 premise 错误（原版根本没有该任务），');
md.push('> `--apply` 会将其判为 `na` 并记录 `reviewNote`；同时报告「原版有、审计未提」的遗漏任务。');
md.push('> 背景：`docs/entity-parity/original-ai-baseline.json`（110/127 只生物覆盖了 `initEntityAI` 且未调用 `super`）。');
md.push('');
md.push(`- 校验条款：${report.checked}`);
md.push(`- premise 成立：${report.verified}`);
md.push(`- premise 被推翻：${report.refuted}（已修正 ${report.applied} 条）`);
md.push(`- 未被任何条款覆盖的原版任务：${report.uncovered}`);
md.push('');
md.push('| 生物 | 被推翻条款 | 判定 | 未注册的 AI 类 |');
md.push('| --- | --- | --- | --- |');
for (const creature of report.creatures) {
    for (const refuted of creature.refuted) {
        md.push(`| \`${creature.id}\` | ${String(refuted.clause).replace(/\|/g, '\\|')} | ${refuted.verdict} | ${refuted.tokens.join('、')} |`);
    }
}
md.push('');
md.push('## 未被条款覆盖的原版任务（审计遗漏，补齐时必须补条款）');
md.push('');
if (!report.uncoveredCreatures.length) {
    md.push('（无）');
} else {
    md.push('| 生物 | 原版任务 | 优先级 | 列表 | 来源类 | 位置 |');
    md.push('| --- | --- | ---: | --- | --- | --- |');
    for (const creature of report.uncoveredCreatures) {
        for (const task of creature.uncovered) {
            const file = String(task.file).split('/').pop();
            md.push(`| \`${creature.id}\` | \`${task.token}\` | ${task.priority} | ${task.list} | ${task.inheritedFrom} | \`${file}:${task.line}\` |`);
        }
    }
}
md.push('');
fs.writeFileSync(OUT_FILE, md.join('\n'));

console.log(`ai clauses cross-checked: ${report.checked}`);
console.log(`premise verified: ${report.verified}, refuted: ${report.refuted}, applied fixes: ${report.applied}`);
console.log(`original tasks not covered by any clause: ${report.uncovered}`);
console.log(`wrote ${path.relative(REPO, OUT_FILE)}`);
