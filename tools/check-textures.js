// Offline validator for model -> texture references.
//
// Walks the models the game actually loads (every blockstate entry plus every
// item model of the mod), follows parent chains and #variable indirection, and
// reports models or textures that do not exist on disk.  Running this avoids
// launching the client just to read "Missing textures in model" warnings.
//
// usage: node tools/check-textures.js
const fs = require('fs');
const path = require('path');

const ASSET_ROOTS = [
    'src/main/resources/assets',
    '.vanilla-assets/assets'
];
const MOD = 'csrp';

const modelCache = new Map();
const textureCache = new Map();

function findFile(rel) {
    for (const root of ASSET_ROOTS) {
        const full = path.join(root, rel);
        if (fs.existsSync(full)) return full;
    }
    return null;
}

function splitId(id) {
    const separator = id.indexOf(':');
    return separator < 0
            ? { namespace: 'minecraft', name: id }
            : { namespace: id.slice(0, separator), name: id.slice(separator + 1) };
}

function loadModel(id) {
    if (modelCache.has(id)) return modelCache.get(id);
    const { namespace, name } = splitId(id);
    const found = findFile(namespace + '/models/' + name + '.json');
    let json = null;
    if (found) {
        try {
            json = JSON.parse(fs.readFileSync(found, 'utf8'));
        } catch (error) {
            json = null;
        }
    }
    modelCache.set(id, json);
    return json;
}

function textureExists(ref) {
    if (textureCache.has(ref)) return textureCache.get(ref);
    const { namespace, name } = splitId(ref);
    const ok = !!findFile(namespace + '/textures/' + name + '.png');
    textureCache.set(ref, ok);
    return ok;
}

// Collects the texture variables visible to a model plus the variables its own
// geometry uses.  Parents contribute variables; geometry is only inherited when
// the child does not declare elements of its own.
function collect(id, depth, stack) {
    if (depth > 32 || stack.has(id)) {
        return { textures: new Map(), used: new Set(), cyclic: true };
    }
    const model = loadModel(id);
    if (!model) return { missingModel: true, textures: new Map(), used: new Set() };
    stack.add(id);
    const hasElements = Array.isArray(model.elements) && model.elements.length > 0;
    let textures = new Map();
    const used = new Set();
    if (model.parent) {
        const parent = collect(model.parent, depth + 1, stack);
        textures = new Map(parent.textures);
        if (!hasElements) {
            for (const value of parent.used) used.add(value);
        }
    }
    stack.delete(id);
    for (const [key, value] of Object.entries(model.textures || {})) {
        textures.set(key.replace(/^#/, ''), value);
    }
    for (const element of model.elements || []) {
        for (const face of Object.values(element.faces || {})) {
            if (typeof face.texture === 'string' && face.texture.startsWith('#')) {
                used.add(face.texture.slice(1));
            }
        }
    }
    return { textures, used };
}

function resolveVariable(name, textures, depth = 0) {
    if (depth > 16) return null;
    const value = textures.get(name);
    if (value === undefined) return undefined;
    if (typeof value === 'string' && value.startsWith('#')) {
        return resolveVariable(value.slice(1), textures, depth + 1);
    }
    return value;
}

function walk(dir, predicate, out = []) {
    if (!fs.existsSync(dir)) return out;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) walk(full, predicate, out);
        else if (predicate(entry.name)) out.push(full);
    }
    return out;
}

function normalizeModelRef(ref) {
    if (ref.startsWith('#')) return null;
    const { namespace, name } = splitId(ref);
    return namespace + ':' + (name.includes('/') ? name : 'block/' + name);
}

// Seeds: every model referenced by a blockstate, plus every item model.
const seeds = new Set();
for (const file of walk('src/main/resources/assets/' + MOD + '/blockstates',
        (n) => n.endsWith('.json'))) {
    let json;
    try {
        json = JSON.parse(fs.readFileSync(file, 'utf8'));
    } catch (error) {
        console.log('invalid blockstate json: ' + file + ' (' + error.message + ')');
        continue;
    }
    const visit = (node) => {
        if (Array.isArray(node)) node.forEach(visit);
        else if (node && typeof node === 'object') {
            for (const [key, value] of Object.entries(node)) {
                if (key === 'model' && typeof value === 'string') {
                    const id = normalizeModelRef(value);
                    if (id) seeds.add(id);
                } else visit(value);
            }
        }
    };
    visit(json);
}
for (const file of walk('src/main/resources/assets/' + MOD + '/models/item',
        (n) => n.endsWith('.json'))) {
    seeds.add(MOD + ':item/' + path.basename(file, '.json'));
}

const report = [];
const reported = new Set();
const emit = (kind, id, detail) => {
    const key = kind + '|' + id + '|' + detail;
    if (reported.has(key)) return;
    reported.add(key);
    report.push([kind, id, detail]);
};

for (const seed of seeds) {
    const { missingModel, textures, used } = collect(seed, 0, new Set());
    if (missingModel) {
        emit('missing model', seed, seed);
        continue;
    }
    for (const name of used) {
        const ref = resolveVariable(name, textures);
        if (ref === undefined) emit('unresolved variable', seed, '#' + name);
        else if (ref === null) emit('cyclic variable', seed, '#' + name);
        else if (!textureExists(ref)) emit('missing texture', seed, ref);
    }
}

console.log('blockstate/item models checked: ' + seeds.size);
if (!report.length) {
    console.log('no missing textures or models');
} else {
    const byKind = new Map();
    for (const row of report) {
        if (!byKind.has(row[0])) byKind.set(row[0], []);
        byKind.get(row[0]).push(row);
    }
    for (const [kind, rows] of byKind) {
        console.log('');
        console.log('=== ' + kind + ' (' + rows.length + '):');
        for (const row of rows) console.log('  ' + row[1].padEnd(58) + ' -> ' + row[2]);
    }
}
