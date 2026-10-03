#!/usr/bin/env node
/**
 * Guards the "generated from the wrong creature" class of bug.
 *
 * The Citadel Tabula ports under {@code client/model/tabula} are generated from the decompiled
 * SRParasites models. A wrong source model silently ships a creature that looks like a different
 * mob: the assimilated adventurer was generated from {@code ModelMudo} (the Rupter) and the thrall
 * from {@code ModelFerEnderman} instead of {@code ModelMes}. Both were only caught by eye in game.
 *
 * The checks below need no external decompile: a model that was generated from the wrong source
 * duplicates another model's part set, and the entity-specific part names of the intended source
 * are either present or absent.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const failures = [];
const modelRoot = path.join(root, 'src', 'main', 'java', 'alku', 'csrp', 'client', 'model', 'tabula');

function read(relative) {
    const file = path.join(root, relative);
    if (!fs.existsSync(file)) {
        failures.push('missing ' + relative);
        return '';
    }
    return fs.readFileSync(file, 'utf8');
}

function collect(dir, list) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) collect(full, list);
        else if (entry.name.endsWith('.java') && entry.name.startsWith('Model')) list.push(full);
    }
    return list;
}

/** Every declared model part of a Tabula model class. */
function parts(text) {
    const found = new Set();
    for (const match of text.matchAll(/(?:public|protected)\s+(?:AdvancedModelBox|ModelRenderer)\s+([A-Za-z0-9_]+)/g)) {
        found.add(match[1]);
    }
    return found;
}

const files = collect(modelRoot, []);
const bySignature = new Map();
for (const file of files) {
    const text = fs.readFileSync(file, 'utf8');
    const set = parts(text);
    if (set.size < 5) continue;
    const signature = [...set].sort().join(',');
    if (!bySignature.has(signature)) bySignature.set(signature, []);
    bySignature.get(signature).push(path.relative(root, file).replace(/\\/g, '/'));
}

// The dragon head has two registry ids (the original "sim_dragonehead" plus the "sim_dragonhead"
// compatibility id) that intentionally share one original model.
const ALLOWED_DUPLICATES = new Set([
    'ModelTabula_sim_dragonehead.java|ModelTabula_sim_dragonhead.java'
]);
for (const [, group] of bySignature) {
    if (group.length < 2) continue;
    const names = group.map((p) => path.basename(p)).sort();
    if (ALLOWED_DUPLICATES.has(names.join('|'))) continue;
    failures.push('these models share one part set, so at least one was generated from the wrong '
        + 'creature: ' + group.join(' == '));
}

/** Assert that a model owns the parts of its intended source and none of the wrong one. */
function expectOwnParts(file, required, forbidden, description) {
    const text = read(file);
    if (!text) return;
    const set = parts(text);
    const missing = required.filter((part) => !set.has(part));
    const wrong = forbidden.filter((part) => set.has(part));
    if (missing.length) failures.push(description + ': missing ' + missing.join(', '));
    if (wrong.length) failures.push(description + ': contains parts of the wrong model: ' + wrong.join(', '));
}

// The assimilated adventurer (original entity "sim_adventurer" = EntityInfPlayer = ModelInfPlayer).
// ModelMudo (the Rupter) was shipped here by mistake.
expectOwnParts('src/main/java/alku/csrp/client/model/tabula/generated/ModelTabula_sim_adventurer.java',
    ['jointH', 'mainbody', 'jointRL', 'jointLB0', 'hair_jointM0', 'arm', 'arm_1'],
    ['joingRB', 'jointFLLX', 'jointFRLX', 'jointBLLX', 'jointBRLX', 'JD'],
    'the assimilated adventurer must use the original ModelInfPlayer geometry');
const adventurer = read('src/main/java/alku/csrp/client/model/tabula/generated/ModelTabula_sim_adventurer.java');
if (!/texWidth = 64;[\s\S]{0,60}?texHeight = 55;/.test(adventurer)) {
    failures.push('the assimilated adventurer texture size must match sim_adventurer.png (64x55)');
}

// The thrall (original entity "thrall" = EntityMes = ModelMes). ModelFerEnderman was shipped here.
expectOwnParts('src/main/java/alku/csrp/client/model/tabula/generated/ModelTabula_thrall.java',
    ['bm', 'bh', 'bodyl', 'jointDLA', 'jointDRA', 'jd'],
    ['bodym', 'jointLL0', 'jointLA0', 'neck'],
    'the thrall must use the original ModelMes geometry');

// The Rupter keeps its own hand-written ModelMudo.
const rupter = read('src/main/java/alku/csrp/client/renderer/RupterRenderer.java');
if (!/new ModelMudo\(\)/.test(rupter)) {
    failures.push('the Rupter must keep the hand-written ModelMudo renderer');
}

// Renderer wiring: each entity id must resolve to the model id of the same original creature.
const client = read('src/main/java/alku/csrp/client/ClientModEvents.java');
const wiring = [
    ['SIM_ADVENTURER', 'sim_adventurer'],
    ['THRALL', 'thrall'],
    ['SIM_DRAGON_HEAD', 'sim_dragonehead'],
    ['SIM_HUMAN', 'sim_human']
];
for (const [entity, modelId] of wiring) {
    const pattern = new RegExp('ModEntities\\.' + entity + '\\.get\\(\\)[\\s\\S]{0,120}?"' + modelId + '"');
    if (!pattern.test(client)) {
        failures.push('ModEntities.' + entity + ' is not rendered with the "' + modelId + '" model');
    }
}

if (failures.length) {
    console.error('Tabula model identity verification failed:');
    failures.forEach((failure) => console.error('- ' + failure));
    process.exit(1);
}
console.log('Tabula model identity verification passed.');
console.log('  model classes checked: ' + files.length);
