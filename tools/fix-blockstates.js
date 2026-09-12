// Rebuilds the remaining 1.12 "forge_marker" blockstate files as modern ones.
//
// The port kept the original Forge metadata blockstates, which modern
// Minecraft rejects with "Unknown blockstate property" (the block ids no longer
// carry the old metadata) or "Missing model".  This tool understands the small
// set of legacy shapes used by the mod and rewrites them against the modern
// properties registered in ModBlocks:
//
//   * slabs           -> type=bottom|top|double (plus the legacy variant name)
//   * variant blocks  -> variant=..., end/node booleans, stage/active integers
//   * plain blocks    -> a single "" variant
//
// usage: node tools/fix-blockstates.js [--dry-run]
const fs = require('fs');
const path = require('path');

const DIR = 'src/main/resources/assets/csrp/blockstates';
const MODEL_DIR = 'src/main/resources/assets/csrp/models/block';
const MODEL_DIR_ITEM = 'src/main/resources/assets/csrp/models/item';
const DRY_RUN = process.argv.includes('--dry-run');

const blockModels = new Set(fs.readdirSync(MODEL_DIR).filter((f) => f.endsWith('.json'))
        .map((f) => f.slice(0, -5)));

/** variant name -> model base for the multi-variant legacy slabs. */
const RUBBLE_SLABS = {
    flesh: 'slabflesh', bone: 'slabbone', stone: 'slabstone', stonedebris: 'slabstonedebris',
    wood: 'slabwood', bricks: 'slabbricks', metal: 'slabmetal', obsidian: 'slabobsidian',
    fungus: 'slabfungus'
};
const STAIN_SLABS = {
    dirt: 'slabdirt', mud: 'slabmud', sflesh: 'slabsflesh', feeler: 'slabfeeler',
    spore: 'slabspore', red: 'slabred', sackflesh: 'slabsackflesh'
};
/** The legacy "one id per rubble type" slab blocks. */
const RUBBLE_SLAB_IDS = {
    'parasiterubbleslabhalf_bone': 'slabbone', 'parasiterubbleslabhalf_bricks': 'slabbricks',
    'parasiterubbleslabhalf_flesh': 'slabflesh', 'parasiterubbleslabhalf_fungus': 'slabfungus',
    'parasiterubbleslabhalf_metal': 'slabmetal', 'parasiterubbleslabhalf_obsidian': 'slabobsidian',
    'parasiterubbleslabhalf_stone': 'slabstone',
    'parasiterubbleslabhalf_stonedebris': 'slabstonedebris',
    'parasiterubbleslabhalf_wood': 'slabwood'
};

function model(id) {
    return 'csrp:block/' + id;
}

/** Returns {bottom, top, double} model ids, or null when a model is missing. */
function slabModels(base, fallbackDouble) {
    const bottom = blockModels.has(base + '_bottom') ? base + '_bottom' : base;
    const top = blockModels.has(base + '_top') ? base + '_top' : null;
    const double = blockModels.has(base + '_double') ? base + '_double' : fallbackDouble;
    if (!top || !double) return null;
    return { bottom, top, double };
}

function slabVariants(base, fallbackDouble) {
    const models = slabModels(base, fallbackDouble);
    if (!models) return null;
    return {
        'type=bottom': { model: model(models.bottom) },
        'type=double': { model: model(models.double) },
        'type=top': { model: model(models.top) }
    };
}

/** <name>_slab / <name>_slab_double: models are <name>_slab{,_top,_double}. */
function plainSlabVariants(id, fallbackDouble) {
    const base = id.endsWith('_double') ? id.slice(0, -'_double'.length) : id;
    return slabVariants(base, fallbackDouble);
}

function write(file, json, note) {
    const text = JSON.stringify(json, null, 2) + '\n';
    if (!DRY_RUN) fs.writeFileSync(path.join(DIR, file), text);
    return note;
}

const changed = [];

// --- the nine legacy "one id per rubble type" slab blocks -------------------
for (const [id, base] of Object.entries(RUBBLE_SLAB_IDS)) {
    const variants = slabVariants(base, null);
    if (!variants) {
        changed.push('!! ' + id + ': models for ' + base + ' missing');
        continue;
    }
    changed.push(write(id + '.json', { variants }, 'created ' + id));
}

// --- every remaining forge_marker blockstate --------------------------------
for (const file of fs.readdirSync(DIR).filter((f) => f.endsWith('.json')).sort()) {
    const full = path.join(DIR, file);
    let json;
    try {
        json = JSON.parse(fs.readFileSync(full, 'utf8'));
    } catch (error) {
        changed.push('!! ' + file + ': invalid json (' + error.message + ')');
        continue;
    }
    if (json.forge_marker === undefined) continue;
    const id = file.slice(0, -5);
    const legacy = json.variants || {};
    const defaultModel = json.defaults && json.defaults.model ? json.defaults.model : null;

    if (id === 'parasitetendril') {
        // 1.12 shipped the vine_1..vine_4u models in vanilla; 1.21 replaced them
        // with a single multipart model, which is what the block needs now.
        const vine = JSON.parse(fs.readFileSync(
                '.vanilla-assets/assets/minecraft/blockstates/vine.json', 'utf8'));
        changed.push(write(file, vine, 'multipart ' + id));
        continue;
    }

    if (Object.keys(legacy).some((key) => key.startsWith('half='))) {
        // legacy slab: half=bottom|top (+ optional variant=...)
        const byVariant = new Map();
        for (const [key, value] of Object.entries(legacy)) {
            if (key === 'inventory') continue;
            const parts = Object.fromEntries(key.split(',').map((p) => p.split('=')));
            const variant = parts.variant || '';
            if (!byVariant.has(variant)) byVariant.set(variant, {});
            byVariant.get(variant)[parts.half] = Array.isArray(value) ? value[0].model : value.model;
        }
        const variants = {};
        const table = id.startsWith('parasitestainslab') ? STAIN_SLABS
                : id.startsWith('parasiterubbleslab') ? RUBBLE_SLABS : null;
        for (const [variant, halves] of byVariant) {
            // Only the multi-variant legacy ids carry a variant property; the
            // plain slabs kept a redundant "variant=default" key in 1.12.
            const prefix = useVariantPrefix(table, variant) ? 'variant=' + variant + ',type=' : 'type=';
            const bottom = halves.bottom || defaultModel;
            const top = halves.top || defaultModel;
            const double = table && table[variant] && blockModels.has(table[variant] + '_double')
                    ? model(table[variant] + '_double')
                    : (blockModels.has(id + '_double') ? model(id + '_double') : top);
            variants[prefix + 'bottom'] = { model: bottom };
            variants[prefix + 'top'] = { model: top };
            variants[prefix + 'double'] = { model: double };
        }
        changed.push(write(file, { variants }, 'slab ' + id));
        continue;
    }

    const variants = {};
    const keys = Object.keys(legacy).filter((key) => key !== 'inventory');
    const typed = keys.filter((key) => key.includes('='));
    const untyped = keys.filter((key) => !key.includes('='));
    for (const key of typed) {
        const entry = legacy[key];
        variants[key] = Array.isArray(entry)
                ? entry.map((part) => withModel(part, defaultModel))
                : withModel(entry, defaultModel);
    }
    if (!typed.length || (untyped.length && !typed.length)) {
        const entry = legacy[untyped[0]] || legacy.normal || { model: defaultModel };
        variants[''] = Array.isArray(entry) ? entry.map((p) => withModel(p, defaultModel))
                : withModel(entry, defaultModel);
    }
    if (!Object.keys(variants).length) {
        changed.push('!! ' + file + ': no usable variants');
        continue;
    }
    changed.push(write(file, { variants }, 'variants ' + id + ' (' + Object.keys(variants).length + ')'));
}

function useVariantPrefix(table, variant) {
    return !!table && !!variant && variant !== 'default';
}

function withModel(entry, defaultModel) {
    const copy = { ...entry };
    if (!copy.model && defaultModel) copy.model = defaultModel;
    return copy;
}

// --- drop the redundant "variant=default" keys carried over from 1.12 -------
let cleaned = 0;
for (const file of fs.readdirSync(DIR).filter((f) => f.endsWith('.json')).sort()) {
    const full = path.join(DIR, file);
    let json;
    try {
        json = JSON.parse(fs.readFileSync(full, 'utf8'));
    } catch (error) {
        continue;
    }
    if (!json.variants) continue;
    const next = {};
    let touched = false;
    for (const [key, value] of Object.entries(json.variants)) {
        if (key.includes('variant=default')) {
            const renamed = key.split(',').filter((p) => p !== 'variant=default').join(',');
            const finalKey = renamed || '';
            if (!(finalKey in next)) next[finalKey] = value;
            touched = true;
        } else if (!(key in next)) {
            next[key] = value;
        }
    }
    if (touched) {
        json.variants = next;
        changed.push(write(file, json, 'dropped variant=default from ' + file.slice(0, -5)));
        cleaned++;
    }
}

// --- canonical slab mapping for every <name>_slab / <name>_slab_double -------
// The legacy "double" ids are full SlabBlocks too, so they need the same
// bottom/top/double triangle as their single counterpart.
let normalised = 0;
for (const id of fs.readdirSync(DIR).filter((f) => f.endsWith('.json')).map((f) => f.slice(0, -5))) {
    if (!id.endsWith('_slab') && !id.endsWith('_slab_double')) continue;
    if (id.startsWith('parasiterubbleslab') || id.startsWith('parasitestainslab')) continue;
    const base = id.endsWith('_slab_double') ? id.slice(0, -'_double'.length) : id;
    if (!blockModels.has(base) || !blockModels.has(base + '_top')
            || !blockModels.has(base + '_double')) {
        changed.push('!! ' + id + ': incomplete slab models for ' + base);
        continue;
    }
    const variants = {
        'type=bottom': { model: model(base) },
        'type=top': { model: model(base + '_top') },
        'type=double': { model: model(base + '_double') }
    };
    const file = id + '.json';
    const current = fs.existsSync(path.join(DIR, file))
            ? fs.readFileSync(path.join(DIR, file), 'utf8') : '';
    if (JSON.stringify(JSON.parse(current || '{}').variants) === JSON.stringify(variants)) continue;
    changed.push(write(file, { variants }, 'slab models ' + id));
    normalised++;
}

// --- blockstates that still carry 1.12-only keys ----------------------------
// These ids use properties the modern registration does not have (normal,
// inventory, facing_*, age, source, ...), so they collapse to the single state
// the block really has.  infested_furnace keeps facing+lit because that pair is
// registered for it.
const SINGLE_VARIANT = [
    'assimilated_blossom', 'consumed_pot', 'epitome_infestation_warp_diffuser',
    'harlequinn_grass', 'hirsute_hair', 'infested_pot', 'lipoma_mass',
    'parasite_barrier', 'potted_assimilated_blossom',
    'potted_consumed_assimilated_blossom', 'infested_workbench', 'consumed_workbench',
    'infested_leaves', 'infested_leaves_fast', 'infestedremain'
];
const FURNACE_Y = { north: 0, east: 90, south: 180, west: 270 };

function pickDefault(variants) {
    const keys = Object.keys(variants);
    const plain = keys.filter((key) => !key.includes('=') && key !== 'inventory');
    const key = plain[0] || keys.find((k) => k !== 'inventory') || keys[0];
    const entry = variants[key];
    return { model: Array.isArray(entry) ? entry[0].model : entry.model };
}

let collapsed = 0;
for (const id of [...SINGLE_VARIANT, 'infested_furnace', 'infested_furnace_lit']) {
    const file = id + '.json';
    const full = path.join(DIR, file);
    if (!fs.existsSync(full)) continue;
    const json = JSON.parse(fs.readFileSync(full, 'utf8'));
    if (!json.variants) continue;
    let variants;
    if (id.startsWith('infested_furnace')) {
        variants = {};
        for (const [facing, y] of Object.entries(FURNACE_Y)) {
            for (const lit of [false, true]) {
                const entry = { model: model(lit ? 'infested_furnace_lit' : 'infested_furnace') };
                if (y) entry.y = y;
                variants['facing=' + facing + ',lit=' + lit] = entry;
            }
        }
    } else {
        variants = { '': pickDefault(json.variants) };
    }
    if (JSON.stringify(json.variants) === JSON.stringify(variants)) continue;
    changed.push(write(file, { variants }, 'collapsed legacy keys ' + id));
    collapsed++;
}

console.log((DRY_RUN ? '[dry run] ' : '') + 'rewrote ' + changed.length + ' blockstates ('
        + cleaned + ' variant=default cleanups, ' + normalised + ' slab normalisations, '
        + collapsed + ' legacy-key collapses)');
for (const line of changed) console.log('  ' + line);
