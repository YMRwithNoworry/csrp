/*
 * Converts the remaining 1.12-style wall resources to the modern wall shape.
 *
 * 1.12 walls stored their connections as booleans (north/south/east/west =
 * true/false); 1.13+ uses the WallSide enum (none/low/tall) and needs a separate
 * "tall" model per side. Blockstates still using "true" fail to parse
 *     java.lang.RuntimeException: Unknown value 'true' for property 'north'
 * and the wall renders as the missing model.
 *
 * Usage: node tools/fix-wall-blockstates.js
 */

const fs = require('fs');
const path = require('path');

const STATES = 'src/main/resources/assets/csrp/blockstates';
const MODELS = 'src/main/resources/assets/csrp/models/block';
const SIDES = ['north', 'east', 'south', 'west'];

/** Returns the model id of the "low" side entry for each direction. */
function sideEntries(multipart) {
    const found = new Map();
    for (const part of multipart) {
        if (!part.when) continue;
        for (const side of SIDES) {
            if (part.when[side] !== undefined) {
                found.set(side, part);
            }
        }
    }
    return found;
}

let patchedStates = 0;
const createdModels = [];

for (const file of fs.readdirSync(STATES).filter((f) => f.endsWith('.json') && f.includes('wall'))) {
    const full = path.join(STATES, file);
    const state = JSON.parse(fs.readFileSync(full, 'utf8'));
    if (!state.multipart) continue;

    const isBoolish = (value) => value === true || value === false
            || value === 'true' || value === 'false';
    const truthy = (value) => value === true || value === 'true';

    const needsFix = state.multipart.some((part) => part.when
            && SIDES.some((side) => isBoolish(part.when[side])));
    if (!needsFix) continue;

    // `true` meant "connected", which is the low connection in the modern shape.
    for (const part of state.multipart) {
        if (!part.when) continue;
        for (const side of SIDES) {
            if (!isBoolish(part.when[side])) continue;
            part.when[side] = truthy(part.when[side]) ? 'low' : 'none';
        }
    }

    // Add the missing "tall" variants, reusing the rotation of the low entry.
    const lows = sideEntries(state.multipart);
    const additions = [];
    for (const side of SIDES) {
        const low = lows.get(side);
        if (!low || !low.apply || !low.apply.model) continue;
        const tallModel = low.apply.model.replace(/_wall_side$/, '_wall_side_tall');
        if (tallModel === low.apply.model) continue;
        const tallName = tallModel.replace(/^csrp:block\//, '');
        ensureSideTallModel(tallName, path.basename(file));
        additions.push({
            when: { [side]: 'tall' },
            apply: { ...low.apply, model: tallModel }
        });
    }
    state.multipart.push(...additions);

    fs.writeFileSync(full, JSON.stringify(state, null, 2) + '\n');
    patchedStates++;
    console.log('patched blockstate ' + file + ' (+' + additions.length + ' tall variants)');
}

/** Creates a `*_wall_side_tall` model mirroring the sibling `*_wall_side` texture. */
function ensureSideTallModel(tallName, blockstateFile) {
    const tallPath = path.join(MODELS, tallName + '.json');
    if (fs.existsSync(tallPath)) return;

    const sideName = tallName.replace(/_side_tall$/, '_side');
    const sidePath = path.join(MODELS, sideName + '.json');
    if (!fs.existsSync(sidePath)) {
        console.log('  ! no source model for ' + tallName + ' (from ' + blockstateFile + ')');
        return;
    }
    const side = JSON.parse(fs.readFileSync(sidePath, 'utf8'));
    const model = {
        parent: 'minecraft:block/template_wall_side_tall',
        textures: side.textures || {}
    };
    fs.writeFileSync(tallPath, JSON.stringify(model, null, 2) + '\n');
    createdModels.push(tallName);
}

console.log('\nblockstates patched: ' + patchedStates);
console.log('side_tall models created: ' + createdModels.length);
for (const name of createdModels) console.log('  + ' + name);
