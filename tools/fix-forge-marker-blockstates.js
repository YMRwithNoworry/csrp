/*
 * Rebuilds the blockstates that were still written in the 1.12 `forge_marker`
 * format. In 1.13+ `variants` keys must be real block state strings
 * ("property=value"); a stale key such as "normal", "half=top,variant=bone"
 * or a missing "inner_left" entry makes every affected block state fall back to
 * the missing model, which is why those blocks rendered without textures.
 *
 * The variant matrices are taken verbatim from the matching vanilla blockstate
 * (stairs/slab/door/trapdoor/pillar) and only the model ids are rewritten to the
 * mod's own models, so the geometry matches vanilla exactly.
 *
 * Usage: node tools/fix-forge-marker-blockstates.js
 */

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const VANILLA_JAR = 'build/moddev/artifacts/neoforge-21.1.235-client-extra-aka-minecraft-resources.jar';
const STATES = 'src/main/resources/assets/csrp/blockstates';
const MODELS = 'src/main/resources/assets/csrp/models/block';
const PREFIX = 'csrp:block/';

function vanillaBlockstate(name) {
    const out = execFileSync('unzip', ['-p', VANILLA_JAR, 'assets/minecraft/blockstates/' + name + '.json'],
            { encoding: 'utf8', maxBuffer: 1 << 20 });
    return JSON.parse(out);
}

function modelExists(name) {
    return fs.existsSync(path.join(MODELS, name + '.json'));
}

function write(name, json) {
    fs.writeFileSync(path.join(STATES, name + '.json'), JSON.stringify(json, null, 2) + '\n');
    return name;
}

/** Rewrites every model id in a vanilla variant map through `resolve`. */
function remapVariants(variants, resolve) {
    const out = {};
    for (const [key, value] of Object.entries(variants)) {
        if (Array.isArray(value)) {
            out[key] = value.map((v) => ({ ...v, model: resolve(v.model) }));
        } else {
            out[key] = { ...value, model: resolve(value.model) };
        }
    }
    return { variants: out };
}

// ---------------------------------------------------------------- stairs

const STAIRS = [
    'parasiterubble_bonestairs', 'parasiterubble_fleshstairs', 'parasiterubble_stonestairs',
    'parasiterubble_stonedebrisstairs', 'parasiterubble_woodstairs', 'parasiterubble_bricksstairs',
    'parasiterubble_metalstairs', 'parasiterubble_obsidianstairs', 'parasiterubble_fungusstairs',
    'parasiterubbledense_wallstairs', 'parasiterubbledense_biomestairs',
    'parasiterubbledense_colonystairs', 'parasitetrunk_treestairs', 'parasitetrunk_ballstairs',
    'parasitetrunk_plantstairs', 'bruisewood_plank_stairs', 'consumed_planks_stairs',
    'deadhead_plank_stairs', 'flesh_stairs', 'frost_weathered_stone_stairs', 'goth_planks_stairs',
    'harleskinn_stairs', 'infestedrubblestairs', 'infestedstainstairs', 'infestedtrunkstairs',
    'parasitestain_dirtstairs', 'parasitestain_feelerstairs', 'parasitestain_fleshstairs',
    'parasitestain_mudstairs', 'wheathered_bricks_stairs', 'wheathered_cobblestone_stairs'
];

const stairsTemplate = vanillaBlockstate('cobblestone_stairs');
const stairsDone = [];
for (const name of STAIRS) {
    if (!modelExists(name)) {
        console.log('! stairs model missing for ' + name);
        continue;
    }
    const json = remapVariants(stairsTemplate.variants, (model) => {
        if (model.endsWith('_stairs_inner')) return PREFIX + name + '_inner';
        if (model.endsWith('_stairs_outer')) return PREFIX + name + '_outer';
        return PREFIX + name;
    });
    stairsDone.push(write(name, json));
}

// ---------------------------------------------------------------- doors

const DOORS = ['goth_door', 'brusewood_door', 'consumed_door', 'infested_door', 'flesh_door',
    'cooked_flesh_door'];

const doorTemplate = vanillaBlockstate('oak_door');
const doorsDone = [];
for (const name of DOORS) {
    const json = remapVariants(doorTemplate.variants, (model) =>
            PREFIX + model.replace('minecraft:block/oak_door', name));
    doorsDone.push(write(name, json));
}

// ---------------------------------------------------------------- trapdoors

const TRAPDOORS = ['goth_trapdoor', 'brusewood_trapdoor', 'consumed_trapdoor', 'infested_trapdoor',
    'flesh_trapdoor', 'cooked_flesh_trapdoor'];

const trapdoorTemplate = vanillaBlockstate('oak_trapdoor');
const trapdoorsDone = [];
for (const name of TRAPDOORS) {
    const json = remapVariants(trapdoorTemplate.variants, (model) =>
            PREFIX + model.replace('minecraft:block/oak_trapdoor', name));
    trapdoorsDone.push(write(name, json));
}

// ---------------------------------------------------------------- pillars

// `axis` only exists when the block is a RotatedPillarBlock (see ModBlocks).
const PILLARS = ['parasitetrunk', 'parasitetrunk_ball', 'parasitetrunk_plant', 'goth_stem'];
const pillarTemplate = vanillaBlockstate('oak_log');
const pillarsDone = [];
for (const name of PILLARS) {
    const json = remapVariants(pillarTemplate.variants, (model) => {
        const horizontal = model.endsWith('_horizontal');
        const base = horizontal ? name + '_horizontal' : name;
        // Fall back to the upright model with the same rotation when no dedicated
        // horizontal model was shipped.
        return PREFIX + (modelExists(base) ? base : name);
    });
    pillarsDone.push(write(name, json));
}

console.log('stairs rebuilt: ' + stairsDone.length);
console.log('doors rebuilt: ' + doorsDone.length);
console.log('trapdoors rebuilt: ' + trapdoorsDone.length);
console.log('pillars rebuilt: ' + pillarsDone.length);
