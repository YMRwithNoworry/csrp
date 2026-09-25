#!/usr/bin/env node
/**
 * Verifies that the parasite "Generations" (迭代) system matches the original
 * SRParasites 1.10.9 implementation.
 *
 * Ground truth: D:\code\MC模组\_srp-orig\decomp-1.10.9\...\world\SRPSaveData.java
 *               D:\code\MC模组\_srp-orig\decomp-1.10.9\...\util\config\SRPConfigSystems.java
 * Every numeric default asserted below is copied verbatim from that source.
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
const expect = (text, pattern, message) => {
    if (!pattern.test(text)) failures.push(message);
};

const config = read('src/main/java/alku/csrp/Config.java');
const system = read('src/main/java/alku/csrp/world/EvolutionSystem.java');
const data = read('src/main/java/alku/csrp/world/SrpWorldData.java');
const events = read('src/main/java/alku/csrp/world/EvolutionEvents.java');
const commands = read('src/main/java/alku/csrp/command/SrpCommands.java');
const relay = read('src/main/java/alku/csrp/relay/RelayScanReportFactory.java');

/* ------------------------------------------------------------------ *
 * 1. Original "parasite_generation" category.
 * ------------------------------------------------------------------ */
expect(config, /define\("generationEnabled", true\)/,
    'original "Generation Enabled" option (generationEnabled) is missing');
expect(config, /boolean generationEnabled\(\).*GENERATION_ENABLED\.get\(\)/,
    'generationEnabled accessor is missing');
expect(config, /defineInRange\("generationDefaultValue", 0, 0, 5\)/,
    'original "Generation Value" option (generationDefaultValue, default 0, range 0..5) is missing');
expect(config, /defineList\("generationDimensionStartingList"/,
    'original "Generation Dimension Starting List" option is missing');
expect(config, /generationDimensionStartingList\(\)/,
    'generationDimensionStartingList accessor is missing');

const generationTimes = [25000, 45000, 72000, 72000, 72000];
generationTimes.forEach((ticks, index) => {
    const n = index + 1;
    expect(config, new RegExp(`defineInRange\\("generationTime${n}", ${ticks}, 0, 2147483640\\)`),
        `original "Generation ${n} Time Needed" default ${ticks} is missing`);
});
expect(config, /case 0 -> GENERATION_TIME_1\.get\(\)[\s\S]{0,400}case 4 -> GENERATION_TIME_5\.get\(\)/,
    'generationTime(generation) does not map generations 0..4 onto generationTime1..5');

const generationPhases = [
    '1, 2, 3, 4, 5, 6, 7, 8, 9, 10',
    '3, 4, 5, 6, 7, 8, 9, 10',
    '5, 6, 7, 8, 9, 10',
    '7, 8, 9, 10',
    '9, 10'
];
generationPhases.forEach((list, index) => {
    const n = index + 1;
    expect(config, new RegExp(`defineList\\("generationPhases${n}", List\\.of\\(${list.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\)`),
        `original "Generation ${n} Phases" default {${list}} is missing`);
});
expect(config, /case 0 -> GENERATION_PHASES_1\.get\(\)[\s\S]{0,400}case 4 -> GENERATION_PHASES_5\.get\(\)/,
    'generationPhases(generation) does not map generations 0..4 onto generationPhases1..5');
expect(config, /defineInRange\("generationPhasePenalty", 1\.5D, 0\.0D, 10\.0D\)/,
    'original "Generation 0th" penalty default 1.5 is missing');

/* ------------------------------------------------------------------ *
 * 2. Original "parasite_generation_0N" categories: 14 knobs per generation.
 * ------------------------------------------------------------------ */
// [key suffix, kind, six original defaults]
const PER_GENERATION = [
    ['coth', 'double', [0.2, 0.3, 0.65, 1.0, 1.0, 1.0]],
    ['specialMoves', 'bool', [false, false, false, false, true, true]],
    ['sprinting', 'bool', [false, false, true, true, true, true]],
    ['lookWalls', 'bool', [false, false, false, false, true, true]],
    ['adaptation', 'bool', [false, false, false, true, true, true]],
    ['damageCap', 'bool', [false, false, false, true, true, true]],
    ['minimumDamage', 'bool', [false, false, true, true, true, true]],
    ['waterLeap', 'bool', [false, false, false, true, true, true]],
    ['blockSearch', 'bool', [false, false, false, false, false, true]],
    ['residue', 'bool', [false, false, false, false, true, true]],
    ['orb', 'bool', [false, false, false, false, false, true]],
    ['poisonHeal', 'double', [0.0, 0.3, 1.0, 1.5, 2.0, 2.5]],
    ['mobHealing', 'double', [0.0, 0.0, 0.5, 1.0, 2.0, 3.0]],
    ['attackSpeed', 'double', [1.0, 1.0, 1.0, 0.9, 0.7, 0.5]]
];
for (const [key, kind, defaults] of PER_GENERATION) {
    const cfgKey = `generation${key.charAt(0).toUpperCase()}${key.slice(1)}`;
    defaults.forEach((value, generation) => {
        const full = `${cfgKey.replace('generation', `generation${generation}`)}`;
        if (kind === 'bool') {
            expect(config, new RegExp(`define\\("${full}", ${value}\\)`),
                `original "Generation ${generation} ${key}" default ${value} is missing (${full})`);
        } else {
            expect(config, new RegExp(`defineInRange\\("${full}", ${value}D,`),
                `original "Generation ${generation} ${key}" default ${value} is missing (${full})`);
        }
    });
}

/* ------------------------------------------------------------------ *
 * 3. Needed-time formula (SRPSaveData#getGenerationNeededTime).
 * ------------------------------------------------------------------ */
expect(system, /case EASY -> 0\.5D;\s*case HARD -> 3\.0D;\s*case IMPOSSIBLE -> 10\.0D;/,
    'generation time must be divided by the original difficulty bonus (Easy 0.5, Hard 3, Impossible 10)');
expect(system, /Math\.round\(Config\.generationTime\(generation\) \/ bonus\)/,
    'needed time must be the difficulty-scaled generationTime(generation)');
expect(system, /needed = \(int\) \(needed \* Config\.generationPhasePenalty\(\)\)/,
    'needed time must be multiplied by the configurable phase penalty when the phase is out of range');
expect(system, /generationPhases\(generation\)/,
    'generationPhaseAllowed must read the configurable phase list');
expect(system, /public static int generationNeededTicks\(int generation, int phase, SrpDifficulty difficulty\)/,
    'difficulty-aware generationNeededTicks overload is missing');

/* ------------------------------------------------------------------ *
 * 4. Gene profile: original getGeneModi/getGeneModi2 order and contents.
 * ------------------------------------------------------------------ */
const profileOrder = ['minimumDamage', 'damageCap', 'lookWalls', 'sprinting', 'waterLeap', 'specialMoves',
    'adaptation', 'blockSearch', 'residue', 'ordinaryOrb', 'cothChance', 'poisonHealing', 'mobHealing',
    'attackSpeedMultiplier'];
const recordBody = (system.match(/public record GenerationProfile\(([\s\S]*?)\)\s*\{\s*\}/) || ['', ''])[1];
if (!recordBody) {
    failures.push('GenerationProfile record is missing');
} else {
    let cursor = -1;
    for (const component of profileOrder) {
        const at = recordBody.indexOf(component);
        if (at < 0) {
            failures.push(`GenerationProfile is missing the original gene "${component}"`);
        } else if (at < cursor) {
            failures.push(`GenerationProfile component "${component}" is out of the original order`);
        }
        cursor = at;
    }
}
expect(system, /Config\.generationEnabled\(\)\s*\?\s*SrpWorldData\.get\(level\)\.generation\(\)\s*:\s*5/,
    'disabling generations must select the full generation-5 profile');
for (let generation = 0; generation <= 5; generation += 1) {
    expect(system, new RegExp(`Config\\.generation${generation}LookWalls\\(\\)`),
        `generation profile never reads the generation ${generation} X-ray flag`);
    expect(system, new RegExp(`Config\\.generation${generation}Residue\\(\\)`),
        `generation profile never reads the generation ${generation} residue flag`);
    expect(system, new RegExp(`Config\\.generation${generation}WaterLeap\\(\\)`),
        `generation profile never reads the generation ${generation} water-leap flag`);
    expect(system, new RegExp(`Config\\.generation${generation}AttackSpeed\\(\\)`),
        `generation profile never reads the generation ${generation} attack-speed multiplier`);
    expect(system, new RegExp(`Config\\.generation${generation}Coth\\(\\)`),
        `generation profile never reads the generation ${generation} COTH health multiplier`);
}

/* ------------------------------------------------------------------ *
 * 5. State machine: absolute world-time model, not a per-second accumulator.
 * ------------------------------------------------------------------ */
expect(data, /generationStartedAt|generationStartTime|generationTimeStamp/,
    'SrpWorldData must store the world time at which the current generation started');
expect(data, /getGameTime\(\)/,
    'elapsed generation time must be derived from the world game time');
expect(data, /generationStartedAt|generationStartTime|generationTimeStamp|generationTime\b/,
    'SrpWorldData must expose the stored generation start time');
expect(data, /tag\.getInt\("[a-z_]*(generation|Generation)[a-z_]*"\)|getInt\("generation/,
    'the generation start time is not persisted');
if (/generationTicks = generationTicks \+ ticks/.test(data)) {
    failures.push('SrpWorldData still advances generations with a per-second tick accumulator');
}
if (/generationTicks = Math\.max\(0, generationTicks \+ ticks\)/.test(data)) {
    failures.push('SrpWorldData still advances generations with a per-second tick accumulator');
}
expect(data, /Config\.generationDefaultValue\(\)/,
    'a new dimension must start at the configured "Generation Value"');
expect(data, /Config\.generationDimensionStartingList\(\)/,
    'the "Generation Dimension Starting List" override is not applied');
expect(data, /checkGeneration/,
    'SrpWorldData has no checkGeneration entry point matching the original');

/* ------------------------------------------------------------------ *
 * 6. The advance check runs from the evolution point path, as in the original.
 * ------------------------------------------------------------------ */
const addEvolution = (data.match(/public boolean addEvolutionPoints\(ServerLevel level, int points, boolean bypassCooldown\)[\s\S]*?\n    \}/) || [''])[0];
expect(addEvolution, /checkGeneration/, 'addEvolutionPoints must call checkGeneration like the original setTotalKills');
if (/data\.tickGeneration\(|generationTicks = generationTicks \+ ticks/.test(events)) {
    failures.push('EvolutionEvents must not advance generations on a per-second timer (the original checks on point gain)');
}

/* ------------------------------------------------------------------ *
 * 7. Per-generation gene consumers.
 * ------------------------------------------------------------------ */
const entityFiles = [];
const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) walk(full);
        else if (entry.name.endsWith('.java')) entityFiles.push(full);
    }
};
const entityDir = path.join(root, 'src', 'main', 'java', 'alku', 'csrp', 'entity');
if (fs.existsSync(entityDir)) walk(entityDir);
// The COTH health multiplier lives in the conversion pipeline, not in entity/.
const infectionDir = path.join(root, 'src', 'main', 'java', 'alku', 'csrp', 'infection');
if (fs.existsSync(infectionDir)) walk(infectionDir);
const entitySources = entityFiles.map((file) => ({ file, text: fs.readFileSync(file, 'utf8') }));
const findEntities = (pattern) => entitySources.filter((entry) => pattern.test(entry.text))
    .map((entry) => path.relative(root, entry.file).replace(/\\/g, '/'));

const consumers = [
    ['lookWalls', /\.lookWalls\(\)/, 'no entity consults the per-generation X-ray flag'],
    ['waterLeap', /\.waterLeap\(\)/, 'no entity consults the per-generation water-leap flag'],
    ['blockSearch', /\.blockSearch\(\)/, 'no entity consults the per-generation block-search flag'],
    ['residue', /\.residue\(\)/, 'no entity consults the per-generation residue flag'],
    ['attackSpeedMultiplier', /\.attackSpeedMultiplier\(\)/, 'no entity consults the per-generation attack-speed multiplier'],
    ['cothChance', /\.cothChance\(\)/, 'no entity consults the per-generation COTH health multiplier'],
    ['ordinaryOrb', /\.ordinaryOrb\(\)/, 'no entity consults the per-generation orb flag'],
    ['specialMoves', /\.specialMoves\(\)/, 'no entity consults the per-generation special-move flag'],
    ['minimumDamage', /\.minimumDamage\(\)/, 'no entity consults the per-generation minimum-damage flag'],
    ['damageCap', /\.damageCap\(\)/, 'no entity consults the per-generation damage-cap flag'],
    ['sprinting', /\.sprinting\(\)/, 'no entity consults the per-generation sprinting flag'],
    ['adaptation', /\.adaptation\(\)/, 'no entity consults the per-generation adaptation flag']
];
const consumerCounts = {};
for (const [name, pattern, message] of consumers) {
    const hits = findEntities(pattern);
    consumerCounts[name] = hits.length;
    if (hits.length === 0) failures.push(message);
}

/* ------------------------------------------------------------------ *
 * 8. Command / report surface.
 * ------------------------------------------------------------------ */
expect(commands, /admin\("srpgeneration"\)/,
    'the original /srpgeneration command root is missing');
expect(commands, /literal\("setgeneration"\)/,
    '/srpgeneration setgeneration is missing');
expect(commands, /literal\("getgeneration"\)/,
    '/srpgeneration getgeneration is missing');
expect(commands, /literal\("addticks"\)/,
    '/srpgeneration addticks is missing');
expect(commands, /locked \(unlocks at generation 3\)/,
    'the command output must still explain the generation-3 adaptation unlock');
expect(commands, /effective profile: full \(generation 5\), adaptation: active/,
    'the command output must still report the disabled-generation full profile');
expect(commands, /literal\("setphase"\)[\s\S]{0,700}IntegerArgumentType\.integer\(0, 5\)[\s\S]{0,200}setPhase/,
    '/srpevolution setphase must accept the optional generation argument');
expect(relay, /data\.generationTicks\(\)/,
    'the relay phase report must read the remaining generation ticks');

if (failures.length) {
    console.error('Generation (迭代) port verification failed:');
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}
console.log('Generation (迭代) port verification passed.');
console.log('  gene consumers: ' + Object.entries(consumerCounts)
    .map(([name, count]) => `${name}=${count}`).join(', '));
