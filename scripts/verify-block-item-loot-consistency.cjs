#!/usr/bin/env node
/**
 * Guards the two block/item data bugs that showed up in the 2026-09-25 crash report:
 *
 *  1. A block listed in ModBlocks#declaredBlockNames() (the "already explicitly registered"
 *     de-duplication set) that nothing actually registers. The legacy placeholder pass then skips
 *     it, so the block never exists - while its loot table still asks for "csrp:<id>" as an item and
 *     the whole loot table fails to parse:
 *       Couldn't parse element loot_tables:csrp:blocks/parasitestain
 *       Expected name to be an item, was unknown string 'csrp:parasitestain'
 *
 *  2. A block that is registered but never gets a BlockItem, so its self-drop loot table fails the
 *     same way (csrp:consumed_workbench).
 *
 * The item side is resolved statically, so the two generated families (the legacy placeholders and
 * the ESCA bulbs) are expanded from their generators instead of being listed by hand.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const failures = [];
const read = (file) => {
    const full = path.join(root, file);
    if (!fs.existsSync(full)) {
        failures.push(`missing ${file}`);
        return '';
    }
    return fs.readFileSync(full, 'utf8');
};
const body = (source, pattern) => {
    const match = source.match(pattern);
    return match ? match[1] : '';
};

const blocks = read('src/main/java/alku/csrp/registry/ModBlocks.java');
const items = read('src/main/java/alku/csrp/registry/ModItems.java');

/* ---- names the legacy placeholder pass must skip because they are registered explicitly ---- */
const declaredBody = body(blocks, /declaredBlockNames\(\)\s*\{[\s\S]*?Set\.of\(([\s\S]*?)\);/);
if (!declaredBody) {
    failures.push('ModBlocks#declaredBlockNames() was not found');
}
const declared = new Set([...declaredBody.matchAll(/"([a-z_0-9]+)"/g)].map((match) => match[1]));
const legacyBody = body(blocks, /registerLegacyBlocks\(\)\s*\{([\s\S]*?)\n    \}/);

/* ---- 1. every declared block name must really be registered somewhere ---- */
if (declaredBody) {
    // Names inside declaredBlockNames() and inside the legacy id array are the two places that do
    // NOT register anything: the first only suppresses the placeholder, the second is the
    // placeholder list itself (and it skips every declared name). So a declared block has to appear
    // once more, outside both.
    const rest = blocks.replace(declaredBody, '').replace(legacyBody, '');
    for (const id of declared) {
        if ((rest.match(new RegExp(`"${id}"`, 'g')) || []).length === 0) {
            failures.push(`block "${id}" is listed in declaredBlockNames() but never registered,`
                + ' so the placeholder pass skips it and any loot table referencing it fails to parse');
        }
    }
}

/* ---- 2. every loot table entry must resolve to a registered item ---- */
const legacyIds = new Set([...legacyBody.matchAll(/"([a-z_0-9]+)"/g)]
    .map((match) => match[1])
    .filter((id) => !declared.has(id)));

const bulbBody = body(blocks, /registerEscaBulbs\(\)\s*\{([\s\S]*?)\n    \}/);
const bulbColors = (bulbBody.match(/String\[\] colors = \{([\s\S]*?)\};/) || ['', ''])[1];
const bulbIds = new Set([...bulbColors.matchAll(/"([a-z_0-9]*)"/g)]
    .map((match) => (match[1].isEmpty ? 'esca_bulb' : `esca_bulb_${match[1]}`)));

const itemIds = new Set([...items.matchAll(/"([a-z_0-9]+)"/g)].map((match) => match[1]));

const collectNames = (node, output) => {
    if (Array.isArray(node)) {
        node.forEach((child) => collectNames(child, output));
        return;
    }
    if (node && typeof node === 'object') {
        if (typeof node.name === 'string' && node.name.startsWith('csrp:')) {
            output.add(node.name.slice('csrp:'.length));
        }
        Object.values(node).forEach((child) => collectNames(child, output));
    }
};

const lootDirectory = path.join(root, 'src', 'main', 'resources', 'data', 'csrp', 'loot_tables', 'blocks');
if (!fs.existsSync(lootDirectory)) {
    failures.push('missing data/csrp/loot_tables/blocks');
} else {
    for (const name of fs.readdirSync(lootDirectory).sort()) {
        if (!name.endsWith('.json')) {
            continue;
        }
        const file = path.join(lootDirectory, name);
        let table;
        try {
            table = JSON.parse(fs.readFileSync(file, 'utf8'));
        } catch (error) {
            failures.push(`loot table csrp:blocks/${name} is not valid JSON: ${error.message}`);
            continue;
        }
        const dropped = new Set();
        collectNames(table, dropped);
        for (const id of dropped) {
            if (itemIds.has(id) || legacyIds.has(id) || bulbIds.has(id)) {
                continue;
            }
            failures.push(`loot table csrp:blocks/${name} drops "csrp:${id}", but no item with that id`
                + ' is registered (add a BlockItem in ModItems or register the block explicitly)');
        }
    }
}

if (failures.length) {
    console.error('Block/item loot consistency verification failed:');
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}
console.log('Block/item loot consistency verification passed.');
