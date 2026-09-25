#!/usr/bin/env node
/*
 * Clusters the missing/partial clauses from docs/entity-parity/raw/<id>.json into
 * systemic gaps (shared by many creatures) vs per-creature gaps, so the "补齐" phase can
 * be batched by system instead of by creature.
 *
 * Usage:
 *   node scripts/entity-parity/summarize-parity-gaps.cjs [--min-shared 3] [--out docs/entity-parity/GAP_CLUSTERS.md]
 */
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const RAW_DIR = path.join(REPO, 'docs', 'entity-parity', 'raw');

function argValue(flag, fallback) {
    const i = process.argv.indexOf(flag);
    return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : fallback;
}

const minShared = Number(argValue('--min-shared', '3'));
const outFile = path.resolve(REPO, argValue('--out', 'docs/entity-parity/GAP_CLUSTERS.md'));

// Normalizes a clause into a comparison key: strip numbers, punctuation and FQ names.
function clauseKey(text) {
    return String(text || '')
        .replace(/`/g, '')
        .replace(/\(.*?\)/g, '')
        .replace(/[0-9]+(\.[0-9]+)?/g, '#')
        .replace(/\s+/g, ' ')
        .trim()
        .toLowerCase()
        .slice(0, 120);
}

const files = fs.existsSync(RAW_DIR) ? fs.readdirSync(RAW_DIR).filter((f) => f.endsWith('.json')) : [];
const clusters = new Map(); // facet -> key -> {clause, creatures:Set, verdicts:{}}
const perCreature = [];

for (const file of files) {
    const id = path.basename(file, '.json');
    let data;
    try {
        data = JSON.parse(fs.readFileSync(path.join(RAW_DIR, file), 'utf8'));
    } catch {
        continue;
    }
    for (const facet of data.facets || []) {
        const facetName = String(facet.name || 'unknown');
        for (const clause of facet.clauses || []) {
            const verdict = String(clause.verdict || '').toLowerCase();
            if (verdict !== 'missing' && verdict !== 'partial') continue;
            const key = facetName + '||' + clauseKey(clause.clause);
            if (!clusters.has(key)) {
                clusters.set(key, { facet: facetName, clause: clause.clause, creatures: new Set(), verdicts: {} });
            }
            const bucket = clusters.get(key);
            bucket.creatures.add(id);
            bucket.verdicts[verdict] = (bucket.verdicts[verdict] || 0) + 1;
        }
    }
    perCreature.push({ id, facetCounts: (data.facets || []).length });
}

const all = [...clusters.values()].map((c) => ({ ...c, size: c.creatures.size }));
const systemic = all.filter((c) => c.size >= minShared).sort((a, b) => b.size - a.size || a.facet.localeCompare(b.facet));
const isolated = all.filter((c) => c.size < minShared);

const byFacet = new Map();
for (const cluster of systemic) {
    if (!byFacet.has(cluster.facet)) byFacet.set(cluster.facet, []);
    byFacet.get(cluster.facet).push(cluster);
}

const md = [];
md.push('# 生物还原缺口聚类（补齐批次依据）');
md.push('');
md.push(`> 生成时间：${new Date().toISOString()}；来源：\`docs/entity-parity/raw/*.json\`（${files.length} 只生物）。`);
md.push(`> 「系统性缺口」= 被 ≥${minShared} 只生物共享的 missing/partial 条款，这些应作为共用系统一次性补齐；其余作为逐生物条目。`);
md.push('');
md.push('## 系统性缺口');
md.push('');
for (const [facet, list] of [...byFacet.entries()].sort((a, b) => b[1][0].size - a[1][0].size)) {
    md.push(`### ${facet}`);
    md.push('');
    md.push('| 条款 | 涉及生物数 | 缺失/部分 | 示例生物 |');
    md.push('| --- | ---: | --- | --- |');
    for (const cluster of list) {
        const examples = [...cluster.creatures].slice(0, 5).map((c) => `\`${c}\``).join(' ');
        md.push(`| ${cluster.clause.replace(/\|/g, '\\|')} | ${cluster.size} | ${cluster.verdicts.missing || 0}/${cluster.verdicts.partial || 0} | ${examples} |`);
    }
    md.push('');
}
md.push('## 逐生物缺口（未被共享的条款）');
md.push('');
md.push(`共 ${isolated.length} 条，明细见各生物 raw JSON 与 \`PARITY_MATRIX.md\`。`);
md.push('');

fs.writeFileSync(outFile, md.join('\n'));
console.log(`raw files: ${files.length}`);
console.log(`systemic clusters (>=${minShared} creatures): ${systemic.length}`);
console.log(`isolated clauses: ${isolated.length}`);
console.log(`wrote ${path.relative(REPO, outFile)}`);
for (const cluster of systemic.slice(0, 15)) {
    console.log(`  ${String(cluster.size).padStart(3)}× [${cluster.facet}] ${cluster.clause.slice(0, 90)}`);
}
