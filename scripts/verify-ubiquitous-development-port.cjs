#!/usr/bin/env node
/**
 * Verifies that the parasite Ubiquitous Development (泛演化发展) system matches the original
 * SRParasites 1.10.9 implementation and the wiki page https://www.mcmod.cn/item/923332.html.
 *
 * Ground truth: D:\\code\\MC模组\\_srp-orig\\decomp-1.10.9\\...\\util\\config\\SRPConfigSystems.java
 *               (parasite_ubiquitous_development_1..4 + deve* knobs)
 *               ...\\world\\SRPSaveData.java (getDeveDimsPhases / getDeveLevel)
 *               ...\\init\\SRPSpawning.java (deveMobChance table swap)
 *               ...\\entity\\ai\\EntityAINexusGrow.java (node/colony/hive gates)
 * Every default asserted below is copied verbatim from that source.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const failures = [];
const read = (file) => {
    const full = path.join(root, file);
    if (!fs.existsSync(full)) {
        failures.push('missing ' + file);
        return '';
    }
    return fs.readFileSync(full, 'utf8');
};
const expectContains = (text, needle, message) => {
    if (!text.includes(needle)) failures.push(message);
};

const config = read('src/main/java/alku/csrp/Config.java');
const system = read('src/main/java/alku/csrp/world/EvolutionSystem.java');
const tables = read('src/main/java/alku/csrp/world/NaturalSpawnTables.java');
const nexus = read('src/main/java/alku/csrp/entity/NexusParasiteEntity.java');
const melt = read('src/main/java/alku/csrp/entity/AssimilatedMeltSystem.java');
const parasite = read('src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java');
const commands = read('src/main/java/alku/csrp/command/SrpCommands.java');

/* 1. Original deve* defaults. */
const knobs = [
    ['ubiquitousMobChance', '0.5D'],
    ['ubiquitousPoints1', '4,'], ['ubiquitousDimensions1', '1,'],
    ['ubiquitousPoints2', '7,'], ['ubiquitousDimensions2', '2,'],
    ['ubiquitousPoints3', '10,'], ['ubiquitousDimensions3', '2,'],
    ['ubiquitousPoints4', '14,'], ['ubiquitousDimensions4', '2,'],
    ['ubiquitousMergeLevel', '1,'],
    ['ubiquitousOneMindLevel', '2,'],
    ['ubiquitousVectorlessLevel', '2,'],
    ['ubiquitousNestsLevel', '3,'],
    ['ubiquitousVariantsLevel', '3,'],
    ['ubiquitousColoniesLevel', '4,'],
    ['ubiquitousNodesLevel', '4,'],
    ['ubiquitousHivesLevel', '4,']
];
for (const [key, value] of knobs) {
    expectContains(config, '"' + key + '", ' + value,
        'original development knob ' + key + ' default ' + value + ' is missing');
}
for (const [key, value] of [['evolutionNestsUnlock', '11,'], ['evolutionNodesUnlock', '11,'],
    ['evolutionColoniesUnlock', '11,'], ['evolutionHivesUnlock', '11,'],
    ['evolutionOneMindUnlock', '11,']]) {
    expectContains(config, '"' + key + '", ' + value,
        'original phase unlock ' + key + ' default 11 is missing');
}
// dislodgment (deveDisloUse) and scent (deveScentUse) keep the level defaults they already had.
expectContains(config, '"dislodgmentUnlockDevelopment", 1,', 'original deveDisloUse 1 is missing');
expectContains(config, '"scentDevelopmentLevel", 2,', 'original deveScentUse 2 is missing');

/* 2. Level computation reads the configurable table and can drop again. */
expectContains(system, 'Config.ubiquitousPoints(level)',
    'the development level table is not read from the config');
expectContains(system, 'Config.ubiquitousDimensions(level)',
    'the development dimension requirement is not read from the config');
expectContains(system, 'public static int ubiquitousDevelopment(ServerLevel level)',
    'the per-dimension development accessor is missing');
const levelLoop = system.indexOf('for (int level = 4; level >= 1; level--)');
if (levelLoop < 0) failures.push('development level must be recomputed from the highest level down');

/* 3. Unlock helpers: original deveXUse / evolutionX pairs. */
const helpers = ['mergeUnlocked', 'collectiveConsciousnessUnlocked', 'vectorlessSpawningUnlocked',
    'nestsUnlocked', 'nodesUnlocked', 'coloniesUnlocked', 'hivesUnlocked', 'variantsUnlocked'];
for (const helper of helpers) {
    expectContains(system, 'public static boolean ' + helper + '(ServerLevel level)',
        'unlock helper ' + helper + ' is missing');
}
expectContains(system, 'SrpWorldData.get(level).evolutionPhase() >= phaseUnlock',
    'unlocks must also fire from the original phase unlock');

/* 4. Consumers: each documented unlock is wired to a mechanic. */
// L1 dislodgment (added with the dislodgment restoration).
expectContains(read('src/main/java/alku/csrp/world/DislodgmentSystem.java'),
    'Config.dislodgmentUnlockDevelopment()',
    'dislodgment is not gated by development 1');
// L1 merge system.
expectContains(melt, 'EvolutionSystem.mergeUnlocked(serverLevel)',
    'the merge system is not gated by development 1');
// L2 collective consciousness.
expectContains(parasite, 'EvolutionSystem.collectiveConsciousnessUnlocked(serverLevel)',
    'collective consciousness is not gated by development 2');
for (const file of ['ArchitectEntity', 'DredgeEntity', 'HeedEntity']) {
    expectContains(read('src/main/java/alku/csrp/entity/' + file + '.java'),
        'collectiveConsciousnessActive()',
        file + ' does not route collective consciousness through the development gate');
}
// L2 vectorless natural spawning.
expectContains(tables, 'EvolutionSystem.vectorlessSpawningUnlocked(level)',
    'spawning outside a vector is not gated by development 2');
// 50% ubiquitous spawn list swap from the config.
expectContains(tables, 'Config.ubiquitousMobChance()',
    'the ubiquitous spawn list swap chance is not configurable');
expectContains(tables, 'case 2 -> UD_TWO', 'development 2 does not use the original UD table');
expectContains(tables, 'case 3 -> UD_THREE', 'development 3 does not use the original UD table');
expectContains(tables, 'case 4 -> UD_FOUR', 'development 4 does not use the original UD table');
// L3 nests, L4 nodes/colonies/hives inside the nexus growth AI.
expectContains(nexus, 'EvolutionSystem.nestsUnlocked(serverLevel)',
    'nest growth is not gated by development 3');
expectContains(nexus, 'case BECKON -> EvolutionSystem.nodesUnlocked(level)',
    'node generation is not gated by development 4');
expectContains(nexus, 'case DISPATCHER -> EvolutionSystem.coloniesUnlocked(level)',
    'colony generation is not gated by development 4');
expectContains(nexus, 'case ROOTER -> EvolutionSystem.hivesUnlocked(level)',
    'hive generation is not gated by development 4');
expectContains(nexus, 'activeKind.stage == 3 && !familyUpgradeUnlocked(serverLevel)',
    'the family stage-4 upgrade is not gated');
expectContains(nexus, '!EvolutionSystem.coloniesUnlocked(serverLevel)',
    'colony placement is not gated by development 4');

/* 5. Original UD spawn tables (weights and counts copied from deveSpawnEntryUD*). */
const tableEntries = [
    'spawn("pri_devourer", 1, 2, 5)',
    'spawn("pri_longarms", 2, 3, 15)',
    'spawn("thrall", 3, 5, 25)',
    'spawn("ada_devourer", 1, 2, 10)',
    'spawn("ada_arachnida", 2, 3, 20)',
    'spawn("grunt", 3, 6, 30)',
    'spawn("monarch", 1, 2, 10)',
    'spawn("warden", 1, 2, 10)',
    'spawn("overseer", 1, 2, 10)',
    'spawn("vigilante", 1, 2, 10)',
    'spawn("marauder", 1, 2, 10)',
    'spawn("grunt", 6, 10, 40)'
];
for (const entry of tableEntries) {
    expectContains(tables, entry, 'original ubiquitous spawn entry missing: ' + entry);
}

/* 6. Report surface (original SRPProgressSnapshot unlock table). */
for (const label of ['Dislodgment', 'Merge system', 'Collective consciousness', 'Scent system',
    'Vectorless spawning', 'Nest generation', 'Node generation', 'Colony generation',
    'Hive generation', 'Spawn list swap chance']) {
    expectContains(commands, label, 'the development report does not list: ' + label);
}

if (failures.length) {
    console.error('Ubiquitous development (泛演化发展) port verification failed:');
    failures.forEach((failure) => console.error('- ' + failure));
    process.exit(1);
}
console.log('Ubiquitous development (泛演化发展) port verification passed.');
console.log('  development knobs asserted: ' + (knobs.length + 7));