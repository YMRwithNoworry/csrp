#!/usr/bin/env node
/*
 * Builds the audit input for the "寄生虫生物部分 100% 还原" work:
 * maps every original creature registration (SRParasites 1.10.9 SRPEntities.java)
 * to its decompiled reference source, its current csrp implementation, and both
 * inheritance chains (behaviour is inherited, so the audit must walk the chain).
 *
 * Usage:
 *   node scripts/entity-parity/build-entity-parity-input.cjs [--original <root>] [--out <file>]
 *
 * Defaults:
 *   --original D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites
 *   --out      docs/entity-parity/audit-input.json
 */
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const DEFAULT_ORIGINAL = 'D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites';

function argValue(flag, fallback) {
    const i = process.argv.indexOf(flag);
    return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : fallback;
}

const originalRoot = path.resolve(argValue('--original', DEFAULT_ORIGINAL));
const outFile = path.resolve(REPO, argValue('--out', 'docs/entity-parity/audit-input.json'));

function walk(dir, out = []) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) walk(full, out);
        else if (entry.name.endsWith('.java')) out.push(full);
    }
    return out;
}

function toPosix(p) {
    return p.split(path.sep).join('/');
}

// ---- original side -------------------------------------------------------

const originalFiles = walk(originalRoot);
const originalByClass = new Map();
const originalByRel = new Map();
for (const file of originalFiles) {
    const rel = toPosix(path.relative(originalRoot, file));
    const cls = path.basename(file, '.java');
    if (!originalByClass.has(cls)) originalByClass.set(cls, file);
    originalByRel.set(rel, file);
}

const entitiesSource = fs.readFileSync(path.join(originalRoot, 'init', 'SRPEntities.java'), 'utf8');

const originalCreatures = [];
const mobRe = /CreateEntityMob\(\s*"([a-z0-9_]+)"\s*,\s*([A-Za-z0-9_]+)\.class/g;
for (const m of entitiesSource.matchAll(mobRe)) {
    originalCreatures.push({ id: m[1], originalClass: m[2] });
}

const originalProjectiles = [];
const projRe = /CreateEntityProjectile\(\s*"([a-z0-9_]+)"\s*,\s*([A-Za-z0-9_]+)\.class/g;
for (const m of entitiesSource.matchAll(projRe)) {
    originalProjectiles.push({ id: m[1], originalClass: m[2] });
}

// Parse "class X extends Y implements A, B" / "abstract class X extends Y".
function parseHierarchy(file) {
    const src = fs.readFileSync(file, 'utf8');
    const cls = path.basename(file, '.java');
    const body = src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '');
    const re = new RegExp(`(?:class|interface)\\s+${cls}\\b[^{]*`);
    const decl = body.match(re);
    const info = { class: cls, file: toPosix(file), extends: null, implements: [], isAbstract: false };
    if (!decl) return info;
    const text = decl[0];
    info.isAbstract = /\babstract\b/.test(text);
    const ext = text.match(/extends\s+([A-Za-z0-9_.$]+)/);
    if (ext) info.extends = ext[1].split('.').pop();
    const impl = text.match(/implements\s+([^{]+)/);
    if (impl) {
        info.implements = impl[1]
            .split(',')
            .map((s) => s.trim().split('.').pop().split('<')[0].trim())
            .filter((s) => /^[A-Za-z0-9_$]+$/.test(s));
    }
    return info;
}

function chainFor(startClass, byClass, seen = new Set()) {
    const chain = [];
    let current = startClass;
    while (current && byClass.has(current) && !seen.has(current)) {
        seen.add(current);
        const file = byClass.get(current);
        const info = parseHierarchy(file);
        chain.push(info);
        current = info.extends;
    }
    return chain;
}

// ---- project side --------------------------------------------------------

const projectEntityDir = path.join(REPO, 'src', 'main', 'java', 'alku', 'csrp', 'entity');
const projectFiles = walk(projectEntityDir);
const projectByClass = new Map();
for (const file of projectFiles) projectByClass.set(path.basename(file, '.java'), file);

const registrationsSource = fs.readFileSync(
    path.join(REPO, 'src', 'main', 'java', 'alku', 'csrp', 'registry', 'ModEntities.java'),
    'utf8',
);

// project id -> { class, kind, via }
const projectRegistrations = new Map();

// Splits a Java argument list at top-level commas (ignores parens/brackets/strings).
function splitArguments(text) {
    const args = [];
    let depth = 0;
    let current = '';
    let inString = false;
    for (const ch of text) {
        if (inString) {
            current += ch;
            if (ch === '"') inString = false;
            continue;
        }
        if (ch === '"') {
            inString = true;
            current += ch;
            continue;
        }
        if (ch === '(' || ch === '<' || ch === '[') depth += 1;
        else if (ch === ')' || ch === '>' || ch === ']') depth -= 1;
        if (ch === ',' && depth === 0) {
            args.push(current.trim());
            current = '';
            continue;
        }
        current += ch;
    }
    if (current.trim()) args.push(current.trim());
    return args;
}

function matchEntityClass(expr) {
    const direct = expr.match(/([A-Za-z0-9_]+Entity)\s*::\s*new/);
    if (direct) return direct[1];
    const viaCtor = expr.match(/new\s+([A-Za-z0-9_]+Entity)\s*\(/);
    if (viaCtor) return viaCtor[1];
    const anyEntity = expr.match(/([A-Za-z0-9_]+Entity)\b/);
    return anyEntity ? anyEntity[1] : null;
}

// Finds every `marker("id", ...)` / `marker("id", () -> ...)` call and parses its argument list.
function collectCalls(marker) {
    const needle = `${marker}(`;
    let index = registrationsSource.indexOf(needle);
    while (index >= 0) {
        const open = index + needle.length - 1;
        let depth = 0;
        let end = -1;
        let inString = false;
        for (let i = open; i < registrationsSource.length; i += 1) {
            const ch = registrationsSource[i];
            if (inString) {
                if (ch === '"') inString = false;
                continue;
            }
            if (ch === '"') inString = true;
            else if (ch === '(') depth += 1;
            else if (ch === ')') {
                depth -= 1;
                if (depth === 0) {
                    end = i;
                    break;
                }
            }
        }
        if (end > 0) {
            const args = splitArguments(registrationsSource.slice(open + 1, end));
            const rawFirst = args[0] || '';
            const first = rawFirst.replace(/"/g, '');
            // Only accept quoted literals, so helper declarations like `monster(String id, ...)` are skipped.
            if (rawFirst.startsWith('"') && /^[a-z0-9_]+$/.test(first)) {
                const factory = args.slice(1).join(', ');
                projectRegistrations.set(first, {
                    class: matchEntityClass(factory),
                    kind: (factory.match(/Kind\.([A-Z0-9_]+)/) || [])[1] || null,
                    via: marker,
                });
            }
        }
        index = registrationsSource.indexOf(needle, index + needle.length);
    }
}

collectCalls('monster');
collectCalls('ENTITIES.register');

// ---- join ----------------------------------------------------------------

const creatures = [];
const issues = [];

for (const entry of originalCreatures) {
    const originalFile = originalByClass.get(entry.originalClass) || null;
    const current = projectRegistrations.get(entry.id) || null;
    if (!originalFile) issues.push(`original class not found: ${entry.originalClass} (${entry.id})`);
    if (!current) issues.push(`project registration not found: ${entry.id}`);
    const projectFile = current && current.class && projectByClass.get(current.class)
        ? path.join(projectEntityDir, `${current.class}.java`)
        : null;
    if (current && current.class && !projectFile) {
        issues.push(`project class file not found: ${current.class} (${entry.id})`);
    }

    creatures.push({
        id: entry.id,
        originalClass: entry.originalClass,
        originalFile: originalFile ? toPosix(originalFile) : null,
        originalChain: originalFile ? chainFor(entry.originalClass, originalByClass) : [],
        projectClass: current ? current.class : null,
        projectFile: projectFile ? toPosix(projectFile) : null,
        projectChain: projectFile ? chainFor(current.class, projectByClass) : [],
        variantKind: current ? current.kind : null,
        registration: current ? current.via : null,
    });
}

const registeredIds = new Set(projectRegistrations.keys());
const originalIds = new Set(originalCreatures.map((c) => c.id));
const projectOnlyIds = [...registeredIds].filter((id) => !originalIds.has(id));

const payload = {
    generatedAt: new Date().toISOString(),
    sources: {
        original: toPosix(originalRoot),
        project: toPosix(REPO),
        entityRegistry: 'src/main/java/alku/csrp/registry/ModEntities.java',
        originalRegistry: toPosix(path.join(originalRoot, 'init', 'SRPEntities.java')),
    },
    counts: {
        originalCreatures: originalCreatures.length,
        matched: creatures.filter((c) => c.originalFile && c.projectFile).length,
        originalProjectiles: originalProjectiles.length,
    },
    creatures,
    originalProjectiles,
    projectOnlyRegistrations: projectOnlyIds,
    issues,
};

fs.mkdirSync(path.dirname(outFile), { recursive: true });
fs.writeFileSync(outFile, JSON.stringify(payload, null, 2));

console.log(`wrote ${toPosix(outFile)}`);
console.log(`creatures: ${payload.counts.originalCreatures}, matched: ${payload.counts.matched}`);
console.log(`project-only registrations: ${projectOnlyIds.length}${projectOnlyIds.length ? ' -> ' + projectOnlyIds.join(', ') : ''}`);
if (issues.length) {
    console.log(`issues (${issues.length}):`);
    for (const issue of issues) console.log(`  - ${issue}`);
    process.exitCode = 1;
}
