#!/usr/bin/env node
/*
 * Deterministic extraction of the AI tasks the ORIGINAL SRParasites 1.10.9 creatures register,
 * along their full inheritance chains. This is ground truth for the `ai` facet: it replaces
 * "agent prose" with parsed registrations and enables both directions of the audit
 * (clauses that do not exist -> false positives; project goals that have no original
 * counterpart -> extras).
 *
 * 1.12.2 obfuscated names used by the decompiled mod:
 *   EntityLiving#tasks       -> field_70714_bg
 *   EntityLiving#targetTasks -> field_70715_bh
 *   EntityAITasks#addTask    -> func_75776_a(priority, task)
 *
 * Writes:
 *   docs/entity-parity/original-ai-tasks.json   (machine readable)
 *   docs/entity-parity/ORIGINAL_AI_TASKS.md     (human readable table)
 *
 * Usage: node scripts/entity-parity/extract-original-ai-tasks.cjs
 */
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const PARITY_DIR = path.join(REPO, 'docs', 'entity-parity');
const INPUT_FILE = path.join(PARITY_DIR, 'audit-input.json');
const OUT_JSON = path.join(PARITY_DIR, 'original-ai-tasks.json');
const OUT_MD = path.join(PARITY_DIR, 'ORIGINAL_AI_TASKS.md');

// The decompiled source may break the line before the dot:
//   this.field_70714_bg
//      .func_75776_a(3, new EntityAIAvoidEntity(...
const TASK_LIST = /this\.(field_70714_bg|field_70715_bh)\s*\.\s*(func_75776_a|func_75777_a)\s*\(\s*([0-9]+)\s*,/g;
const LIST_NAME = { field_70714_bg: 'tasks', field_70715_bh: 'targetTasks' };

const fileCache = new Map();
function readSource(file) {
    if (!fileCache.has(file)) fileCache.set(file, fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
    return fileCache.get(file);
}

/** Enclosing method/constructor name for a character offset. */
function enclosingScope(src, offset) {
    const before = src.slice(0, offset);
    const matches = [...before.matchAll(/(?:^|\n)\s*(?:@Override\s*\n\s*)?(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?([A-Za-z0-9_<>\[\], .]+?)\s*([A-Za-z0-9_]+)\s*\([^;{]*\)\s*\{/g)];
    if (!matches.length) return null;
    const last = matches[matches.length - 1];
    return { name: last[2], line: before.slice(0, last.index).split('\n').length };
}

/** Heuristic: is the call inside a conditional/loop block? */
function isConditional(src, offset) {
    const start = src.lastIndexOf('\n', offset);
    const window = src.slice(Math.max(0, offset - 1200), offset);
    const lastOpen = window.lastIndexOf('{');
    if (lastOpen < 0) return false;
    const head = window.slice(Math.max(0, lastOpen - 160), lastOpen);
    return /\b(if|else if|for|while)\s*\(/.test(head);
}

/** Balanced-paren argument text of a call starting at `offset` (the '(' index). */
function callArguments(src, openIndex) {
    let depth = 0;
    for (let i = openIndex; i < src.length; i += 1) {
        const ch = src[i];
        if (ch === '(') depth += 1;
        else if (ch === ')') {
            depth -= 1;
            if (depth === 0) return src.slice(openIndex + 1, i);
        }
    }
    return '';
}

function extractFromFile(file) {
    const src = readSource(file);
    const clean = src.replace(/\/\*[\s\S]*?\*\//g, (m) => m.replace(/[^\n]/g, ' ')); // keep offsets
    const found = [];
    for (const match of clean.matchAll(TASK_LIST)) {
        const list = LIST_NAME[match[1]];
        const priority = Number(match[3]);
        const openIndex = clean.indexOf('(', match.index); // the func_75776_a '(' itself
        const args = callArguments(clean, openIndex);
        const constructor = args.match(/new\s+([A-Za-z0-9_$.]+)\s*\(/);
        if (!constructor) continue;
        const classToken = constructor[1].split('.').pop();
        const scope = enclosingScope(clean, match.index);
        found.push({
            list,
            priority,
            task: classToken,
            qualified: constructor[1],
            file,
            line: clean.slice(0, match.index).split('\n').length,
            scope: scope ? scope.name : null,
            conditional: isConditional(clean, match.index),
            args: args.trim().slice(0, 200),
        });
    }
    return found;
}

const input = JSON.parse(fs.readFileSync(INPUT_FILE, 'utf8'));
const creatures = [];

for (const creature of input.creatures) {
    const tasks = [];
    for (const link of creature.originalChain) {
        if (!link.file) continue;
        for (const task of extractFromFile(link.file)) tasks.push({ inheritedFrom: link.class, ...task });
    }
    // The base-class setup lives in the chain (EntityParasiteBase), so nothing extra is needed.
    creatures.push({
        id: creature.id,
        originalClass: creature.originalClass,
        chain: creature.originalChain.map((l) => l.class),
        tasks,
        counts: {
            tasks: tasks.filter((t) => t.list === 'tasks').length,
            targetTasks: tasks.filter((t) => t.list === 'targetTasks').length,
        },
    });
}

const payload = {
    generatedAt: new Date().toISOString(),
    method:
        'Parsed 1.12.2 registrations along each creature inheritance chain: field_70714_bg (tasks) / field_70715_bh (targetTasks) + func_75776_a(priority, task); ' +
        'base classes (EntityParasiteBase etc.) contribute constructor registrations, per-mob initEntityAI contributes the rest. ' +
        'NOTE: vanilla EntityMob baseline goals only exist when the chain does not replace initEntityAI without super (see original-ai-baseline.json).',
    creatures,
};
fs.writeFileSync(OUT_JSON, JSON.stringify(payload, null, 2));

// ---- markdown ----
const md = [];
md.push('# 原版生物 AI 任务地面真值（1.10.9）');
md.push('');
md.push(`> 生成时间：${payload.generatedAt}；由 \`scripts/entity-parity/extract-original-ai-tasks.cjs\` 从反编译源码解析。`);
md.push('> 口径：沿每只生物的原版继承链解析 `field_70714_bg`(tasks) / `field_70715_bh`(targetTasks) 的 `func_75776_a(优先级, 任务)` 注册，');
md.push('> 含基类构造函数注册；`cond` 表示该注册位于条件块内（阶段/配置门控）。');
md.push('> 原版未覆盖 `initEntityAI` 或覆盖时调用 `super` 的生物（17 只）另有 vanilla `EntityMob` 基线任务，见 `original-ai-baseline.json`。');
md.push('');
const withCounts = creatures.map((c) => ({ ...c, total: c.tasks.length }));
md.push(`- 生物数：${withCounts.length}；任务注册总数：${withCounts.reduce((sum, c) => sum + c.total, 0)}`);
md.push(`- 零注册生物（AI 完全来自 vanilla EntityMob 或能力接口驱动）：${withCounts.filter((c) => c.total === 0).map((c) => '`' + c.id + '`').join('、') || '无'}`);
md.push('');
md.push('## 逐生物任务表');
md.push('');
for (const creature of withCounts.sort((a, b) => a.id.localeCompare(b.id))) {
    md.push(`### \`${creature.id}\`（${creature.originalClass}）`);
    md.push('');
    if (!creature.total) {
        md.push('- （无显式注册）');
        md.push('');
        continue;
    }
    md.push('| 列表 | 优先级 | 任务 | 来源类 | 条件 | 位置 |');
    md.push('| --- | ---: | --- | --- | --- | --- |');
    for (const task of creature.tasks.sort((a, b) => a.list.localeCompare(b.list) || a.priority - b.priority)) {
        const file = path.basename(task.file);
        md.push(`| ${task.list} | ${task.priority} | \`${task.task}\` | ${task.inheritedFrom} | ${task.conditional ? 'cond' : ''} | \`${file}:${task.line}\` |`);
    }
    md.push('');
}
fs.writeFileSync(OUT_MD, md.join('\n'));

const zero = withCounts.filter((c) => c.total === 0).length;
console.log(`creatures: ${withCounts.length}, task registrations: ${withCounts.reduce((s, c) => s + c.total, 0)}, zero-registration creatures: ${zero}`);
console.log(`wrote ${path.relative(REPO, OUT_JSON)} and ${path.relative(REPO, OUT_MD)}`);
