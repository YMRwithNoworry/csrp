#!/usr/bin/env node
/**
 * Verifies that every mod dependency of this project is bundled inside the mod jar
 * (Forge Jar-in-Jar), so a player no longer has to install GeckoLib or Citadel.
 *
 * Checked:
 *  - build.gradle declares both libraries as jarJar dependencies with matching ranges,
 *    enables Jar-in-Jar and ships the self-contained build under the plain artifact name;
 *  - src/main/templates/META-INF/mods.toml still declares both dependencies, so FML accepts
 *    the embedded copies as the provider;
 *  - when build/libs/csrp-<version>.jar exists, it really contains META-INF/jarjar plus the
 *    jarjar metadata for both libraries.
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

const build = read('build.gradle');
const properties = read('gradle.properties');
const modsToml = read('src/main/templates/META-INF/mods.toml');

const property = (name) => (properties.match(new RegExp('^' + name + '=(.*)$', 'm')) || [])[1];
const geckolibVersion = (property('geckolib_version') || '').trim();
const citadelVersion = (property('citadel_version') || '').trim();
const modVersion = (property('mod_version') || '').trim();

if (!geckolibVersion || !citadelVersion) {
    failures.push('gradle.properties must define geckolib_version and citadel_version');
}

// Jar-in-Jar declarations.
if (!/jarJar\("software\.bernie\.geckolib:geckolib-forge-\$\{minecraft_version\}:\$\{geckolib_version\}"\)/.test(build)) {
    failures.push('GeckoLib is not declared as a jarJar dependency in build.gradle');
}
if (!/jarJar\("maven\.modrinth:citadel:\$\{citadel_version\}"\)/.test(build)) {
    failures.push('Citadel is not declared as a jarJar dependency in build.gradle');
}
if (!/jarJar\.ranged\(it, "\[\$\{geckolib_version\},\)"\)/.test(build)) {
    failures.push('the embedded GeckoLib version range is missing');
}
if (!/jarJar\.ranged\(it, "\[\$\{citadel_version\},\)"\)/.test(build)) {
    failures.push('the embedded Citadel version range is missing');
}
if (!/jarJar\.enable\(\)/.test(build)) {
    failures.push('Jar-in-Jar is not enabled (jarJar.enable())');
}
// The self-contained build must be the plain artifact, not a separate "-all" jar.
if (!/tasks\.named\('jarJar', Jar\) \{\s*\n\s*archiveClassifier = ''/.test(build)) {
    failures.push("the jarJar build must keep the plain artifact name (archiveClassifier = '')");
}
if (!/tasks\.named\('jar', Jar\) \{\s*\n\s*archiveClassifier = 'thin'/.test(build)) {
    failures.push("the library-less build must move to the '-thin' classifier");
}

// The mod still declares both libraries, so the embedded copies satisfy the dependencies.
for (const [modId, propertyName] of [['geckolib', 'geckolib_version'], ['citadel', 'citadel_version']]) {
    const block = new RegExp('modId = "' + modId + '"[\\s\\S]{0,200}?versionRange = "\\[\\$\\{' + propertyName + '\\},\\)"');
    if (!block.test(modsToml)) {
        failures.push('mods.toml must keep the mandatory "' + modId + '" dependency at [ + propertyName + ,)');
    }
}

// When the mod jar has been built, its Jar-in-Jar payload must be complete.
const jarPath = path.join(root, 'build', 'libs', 'csrp-' + modVersion + '.jar');
if (fs.existsSync(jarPath)) {
    const { execFileSync } = require('child_process');
    let entries = '';
    try {
        entries = execFileSync('tar', ['-tf', jarPath], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
    } catch (error) {
        failures.push('could not list ' + path.relative(root, jarPath) + ': ' + error.message);
    }
    for (const expected of [
        'META-INF/jarjar/metadata.json',
        'META-INF/jarjar/geckolib-forge-1.20.1-' + geckolibVersion + '.jar',
        'META-INF/jarjar/citadel-' + citadelVersion + '.jar'
    ]) {
        if (!entries.split(/\r?\n/).includes(expected)) {
            failures.push('the built mod jar is missing ' + expected);
        }
    }
}

if (failures.length) {
    console.error('Jar-in-Jar bundling verification failed:');
    failures.forEach((failure) => console.error('- ' + failure));
    process.exit(1);
}
console.log('Jar-in-Jar bundling verification passed.');
console.log('  bundled: geckolib ' + geckolibVersion + ', citadel ' + citadelVersion
    + (fs.existsSync(jarPath) ? ' (verified inside ' + path.relative(root, jarPath) + ')' : ' (jar not built yet)'));
