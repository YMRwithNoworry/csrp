#!/usr/bin/env node
/**
 * Guard for the configurable phase cooldown (original SRP "Phase # Delay").
 *
 * 1.10.9 locks point gain for a while after a dimension advances an evolution phase - 4,000 s when
 * entering phase 1, 4,800 s for phase 2, and so on - and the port used to ship those numbers
 * hard-coded in EvolutionSystem. The delay is now the configurable "phaseDelaySeconds" list
 * (Config, csrp-systems.toml) and every phase defaults to 0 seconds, so advancing a phase never
 * stops the dimension from gaining points unless a value is configured.
 *
 * This guard fails when
 *   - the list option is missing, has the wrong length, or does not default to all zeros,
 *   - the option accepts negative delays,
 *   - Config#phaseDelaySeconds does not answer 0 for phases without a configured entry,
 *   - EvolutionSystem hard-codes the original delay table again instead of reading the config,
 *   - a phase change applies the delay unconditionally, which would both lock point gain again and
 *     wipe an active Lure cooldown whenever the configured delay is 0.
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
const system = read('src/main/java/alku/csrp/world/EvolutionSystem.java');
const world = read('src/main/java/alku/csrp/world/SrpWorldData.java');

// The option itself: eleven entries (phase 0 .. phase 10), all zero by default.
const declaration = config.match(/defineList\("phaseDelaySeconds",\s*List\.of\(([^)]*)\)/);
if (!declaration) {
    failures.push('Config must declare the phaseDelaySeconds list option');
} else {
    const defaults = declaration[1].split(',').map((value) => value.trim()).filter((value) => value.length > 0);
    if (defaults.length !== 11) {
        failures.push('phaseDelaySeconds must list one default per phase (0..10), found ' + defaults.length);
    }
    defaults.forEach((value, phase) => {
        if (value !== '0') {
            failures.push('phaseDelaySeconds default for phase ' + phase + ' must be 0, found ' + value);
        }
    });
}
expect(config, /value -> value instanceof Integer seconds && seconds >= 0/,
    'the phaseDelaySeconds validator must reject negative delays');
expect(config, /\[0, 4000, 4800, 4700, 4500, 4200, 3800, 3700, 3700, 3800, 6000\]/,
    'the option comment must keep documenting the original 1.10.9 "Phase # Delay" values');
expect(config, /public static int phaseDelaySeconds\(int phase\)[\s\S]{0,500}?if \(phase < 0\) \{[\s\S]{0,300}?if \(phase >= delays\.size\(\)\) \{/,
    'Config#phaseDelaySeconds must answer 0 for the pre-phase-0 states and for unlisted phases');

// The evolution system must read the configuration instead of the removed hard-coded table.
expect(system, /public static int phaseDelaySeconds\(int phase\) \{\s*return Config\.phaseDelaySeconds\(phase\);/,
    'EvolutionSystem#phaseDelaySeconds must delegate to Config#phaseDelaySeconds');
if (/PHASE_DELAY_SECONDS\s*=\s*\{/.test(system)) {
    failures.push('EvolutionSystem must not hard-code the original phase delay table again');
}

// A phase change may only lock points - and only ever extend nothing else - when a delay is set.
expect(world, /int phaseDelay = EvolutionSystem\.phaseDelaySeconds\(evolutionPhase\);\s*if \(phaseDelay > 0\) \{\s*setCooldown\(level, phaseDelay\);/,
    'a phase change must apply the configured delay only when it is greater than zero');
expect(world, /!bypassCooldown && cooldown\(level\) > 0/,
    'the point gain path must still be gated by the stored cooldown, which is 0 by default');
expect(world, /announcePhaseChange\(level, previous, evolutionPhase\)/,
    'the phase change announcement must stay in the phase change path');

if (failures.length) {
    console.error('Phase cooldown configuration verification failed:');
    failures.forEach((failure) => console.error('- ' + failure));
    process.exit(1);
}
console.log('Phase cooldown configuration verification passed.');
