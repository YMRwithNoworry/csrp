#!/usr/bin/env node
/**
 * Guard for the configurable per-dimension starting evolution phase.
 *
 * 1.10.9 shipped "Evolution Phases Dimension Starting Phase List" ({@code SRPConfigSystems.evolutionDimStart},
 * entries {@code "<dimension>;<phase>;<points>"}) and applied it in {@code SRPSaveData#createData}:
 * phase -2 locks the dimension, phase -1 starts it at {@code -points}, 0-10 start it at {@code points}.
 * The port now exposes the same option as {@code evolutionDimensionStartingList} (csrp-systems.toml),
 * so a pack author can give the Nether, the End or any modded dimension its own phase.
 *
 * This guard fails when
 *   - the option or its accessor is missing,
 *   - the option is not a list of strings with an empty (behaviour preserving) default,
 *   - the original 1.10.9 values are no longer documented in the option comment,
 *   - a dimension's data is created without applying the configured start,
 *   - the phase/points/lock semantics of the original are not honoured,
 *   - dimensions are matched only by their namespaced id (the legacy numeric ids and the bare path
 *     must keep working, exactly like the generation list),
 *   - or the built-in per-dimension defaults were dropped instead of being overridden.
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
const expect = (text, pattern, message) => {
    if (!pattern.test(text)) failures.push(message);
};

const config = read('src/main/java/alku/csrp/Config.java');
const world = read('src/main/java/alku/csrp/world/SrpWorldData.java');
const system = read('src/main/java/alku/csrp/world/EvolutionSystem.java');

// 1. The option itself: a string list that defaults to empty so existing worlds keep their behaviour.
const declaration = config.match(/defineList\("evolutionDimensionStartingList",\s*List\.of\(\)\s*,\s*value -> value instanceof String\)/);
if (!declaration) {
    failures.push('Config must declare "evolutionDimensionStartingList" as an empty string list');
}
expect(config, /public static List<\? extends String> evolutionDimensionStartingList\(\)/,
    'Config#evolutionDimensionStartingList accessor is missing');
expect(config, /<dimension_id>;<phase>;<points>/,
    'the option comment must document the "<dimension_id>;<phase>;<points>" entry format');
expect(config, /minecraft:the_nether;-1;50/,
    'the option comment must list the original 1.10.9 values as an example');
expect(config, /original SRP[\s\S]{0,80}Evolution Phases Dimension Starting Phase List/,
    'the option comment must name the original option it ports');

// 2. Applied while a dimension's data is created, not on every load (progress must never be reset).
expect(world, /applyConfiguredGeneration\(level\);\s*\n\s*applyConfiguredEvolutionStart\(level\);/,
    'SrpWorldData#initialize must apply the configured starting phase when the data is created');
expect(world, /private void applyConfiguredEvolutionStart\(ServerLevel level\)/,
    'SrpWorldData#applyConfiguredEvolutionStart is missing');
expect(world, /entry\.split\(";",\s*3\)/,
    'the entry parser must split "<dimension>;<phase>;<points>" into three fields');
expect(world, /Math\.max\(-2,\s*Math\.min\(10,\s*Integer\.parseInt\(fields\[1\]\.trim\(\)\)\)\)/,
    'the configured phase must be clamped to -2..10');
expect(world, /catch \(NumberFormatException ignored\)[\s\S]{0,120}keep the default/,
    'malformed entries must be skipped silently, like the original');

// 3. The original semantics: -2 locks gain and loss, -1 starts at -points, otherwise points.
const applier = (world.match(/private void applyConfiguredEvolutionStart\(ServerLevel level\)[\s\S]*?\n    \}/) || [''])[0];
if (!applier) {
    failures.push('SrpWorldData#applyConfiguredEvolutionStart body is missing');
} else {
    expect(applier, /forceEvolutionPhase\(level, phase\)/,
        'the configured phase must be applied through forceEvolutionPhase');
    expect(applier, /phase == -2[\s\S]{0,160}canGain = false;[\s\S]{0,80}canLose = false;/,
        'phase -2 must lock the dimension (no point gain and no point loss)');
    expect(applier, /evolutionPoints = phase == -1 \? -points : points;/,
        'phase -1 must start the dimension at -points and 0-10 at the configured points, like the original');
    expect(applier, /fields\.length > 2/,
        'the three-field original form "<dimension>;<phase>;<points>" must be supported');
    expect(applier, /evolutionPoints = EvolutionSystem\.thresholdForPhase\(phase\);/,
        'the two-field form must start the dimension exactly at the configured phase');
}

// 4. Dimension matching: full id, path and the legacy numeric ids.
expect(world, /private static boolean matchesDimension\(ServerLevel level, String configured\)/,
    'the shared dimension matcher (matchesDimension) is missing');
expect(world, /configured\.equals\(location\) \|\| configured\.equals\(path\)/,
    'a configured dimension must also match the bare dimension path');
expect(world, /legacyId != null && configured\.equals\(legacyId\)/,
    'the legacy numeric dimension ids (0, -1, 1) must keep working');
expect(world, /for \(String entry : Config\.generationDimensionStartingList\(\)\) \{\s*\n\s*int separator[\s\S]{0,400}?matchesDimension\(level,/,
    'the generation list must reuse the shared dimension matcher');

// 5. The built-in defaults stay in place and are only overridden by the config.
expect(system, /new InitialProgress\(0, 0\)/, 'the overworld default (phase 0, 0 points) is missing');
expect(system, /new InitialProgress\(-1, -50\)/, 'the nether default (phase -1, -50 points) is missing');
expect(system, /new InitialProgress\(-1, -100\)/, 'the end default (phase -1, -100 points) is missing');
expect(system, /new InitialProgress\(-1, -300\)/, 'the fallback dimension default (phase -1, -300 points) is missing');

if (failures.length) {
    console.error('Dimension starting phase verification failed:');
    failures.forEach((failure) => console.error('- ' + failure));
    process.exit(1);
}
console.log('Dimension starting phase verification passed.');
console.log('  configurable per dimension: "<dimension_id>;<phase>;<points>" (original Evolution Phases Dimension Starting Phase List)');
