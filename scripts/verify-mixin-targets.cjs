#!/usr/bin/env node
/**
 * Guard for the client Mixin target selectors, added after the 2026-09-25 pack report
 * (a Forge 1.20.1 client died during early display with):
 *
 *   Mixin apply failed champions-common.mixins.json:MinecraftMixin -> net.minecraft.client.Minecraft:
 *   InvalidInjectionException: @ModifyVariable annotation on modifyItemStack could not find any
 *   targets matching 'pickBlock' in net.minecraft.client.Minecraft. No refMap loaded.
 *
 * Forge 1.20.1 production runs Minecraft with SRG member names (Gui#render is m_280421_), while a
 * development runtime built from official Mojang mappings keeps the official names (render). Mods
 * normally bridge the two with a Mixin refmap, but this project deliberately does not run the Mixin
 * annotation processor (see build.gradle), so no csrp.refmap.json is ever emitted. Every selector
 * therefore has to name the member twice - official first, SRG second - and every injector has to
 * carry "require = 0" so a drifted selector degrades to a skipped injection instead of taking the
 * whole client down the way champions did.
 *
 * This guard fails when
 *   - csrp.mixins.json declares a refmap that is not shipped,
 *   - a configured mixin class has no Java source,
 *   - an @Inject/@ModifyVariable selector list lacks either spelling or "require = 0",
 *   - a spelled name/descriptor does not exist on the @Mixin target class in the 1.20.1 mappings,
 *   - an @Shadow member has neither the SRG name nor an official alias.
 */
'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');

const root = path.resolve(__dirname, '..');
const configFile = 'src/main/resources/csrp.mixins.json';
const failures = [];
const notes = [];
const fail = (message) => failures.push(message);

const SRG_MEMBER = /^[mf]_\d+_$/;
const MAPPING_SOURCES = [];
const METHOD_MEMBERS = new Map(); // owner -> Set("name descriptor")
const METHOD_NAMES = new Map(); // owner -> Set(name)
const FIELD_MEMBERS = new Map(); // owner -> Set(name)

const addMethod = (owner, name, descriptor) => {
    if (!METHOD_MEMBERS.has(owner)) METHOD_MEMBERS.set(owner, new Set());
    METHOD_MEMBERS.get(owner).add(name + ' ' + descriptor);
    if (!METHOD_NAMES.has(owner)) METHOD_NAMES.set(owner, new Set());
    METHOD_NAMES.get(owner).add(name);
};

const addField = (owner, name) => {
    if (!FIELD_MEMBERS.has(owner)) FIELD_MEMBERS.set(owner, new Set());
    FIELD_MEMBERS.get(owner).add(name);
};

const nameOf = (qualified) => qualified.slice(qualified.lastIndexOf('/') + 1);
const ownerOf = (qualified) => qualified.slice(0, qualified.lastIndexOf('/'));

// build/official_to_searge.srg, written by build.gradle's generateSrgForMixinAp task.
const readOfficialToSrg = (file) => {
    for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
        if (line.startsWith('MD: ')) {
            const parts = line.split(/\s+/);
            addMethod(ownerOf(parts[1]), nameOf(parts[1]), parts[3]);
            addMethod(ownerOf(parts[1]), nameOf(parts[2]), parts[3]);
        } else if (line.startsWith('FD: ')) {
            const parts = line.split(/\s+/);
            addField(ownerOf(parts[1]), nameOf(parts[1]));
            addField(ownerOf(parts[1]), nameOf(parts[2]));
        }
    }
};

// ForgeGradle's cached srg_to_official_1.20.1.tsrg (tsrg2), used when the build output is absent.
const readSrgToOfficialTsrg = (file) => {
    let owner = null;
    for (const raw of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
        const line = raw.endsWith('\r') ? raw.slice(0, -1) : raw;
        if (!line.trim() || line.trim() === 'static') continue;
        if (!line.startsWith('\t')) {
            owner = line.trim().split(/\s+/)[1];
            continue;
        }
        if (owner === null) continue;
        const parts = line.trim().split(/\s+/);
        if (parts.length >= 3 && parts[1].startsWith('(')) {
            addMethod(owner, parts[0], parts[1]);
            addMethod(owner, parts[2], parts[1]);
        } else if (parts.length >= 2) {
            addField(owner, parts[0]);
            addField(owner, parts[1]);
        }
    }
};

const loadMappings = () => {
    const generated = path.join(root, 'build', 'official_to_searge.srg');
    if (fs.existsSync(generated)) {
        readOfficialToSrg(generated);
        MAPPING_SOURCES.push('build/official_to_searge.srg');
        return;
    }
    const gradleHome = process.env.GRADLE_USER_HOME || path.join(os.homedir(), '.gradle');
    const mcpRoot = path.join(gradleHome, 'caches', 'forge_gradle', 'minecraft_user_repo',
        'de', 'oceanlabs', 'mcp', 'mcp_config');
    if (!fs.existsSync(mcpRoot)) return;
    for (const dir of fs.readdirSync(mcpRoot)) {
        if (!dir.startsWith('1.20.1')) continue;
        const candidate = path.join(mcpRoot, dir, 'srg_to_official_1.20.1.tsrg');
        if (!fs.existsSync(candidate)) continue;
        readSrgToOfficialTsrg(candidate);
        MAPPING_SOURCES.push(path.relative(root, candidate).split(path.sep).join('/'));
        return;
    }
};

const readSource = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');

const resolveTarget = (java, file) => {
    const match = java.match(/@Mixin\(\s*([A-Za-z0-9_$.]+)\s*\.class/);
    if (!match) {
        fail(file + ': no @Mixin(<class>.class) target found');
        return null;
    }
    if (match[1].includes('.')) return match[1].replace(/\./g, '/');
    const imports = [...java.matchAll(/^import\s+([\w.$]+);/gm)].map((entry) => entry[1]);
    const imported = imports.find((name) => name.endsWith('.' + match[1]));
    if (!imported) {
        fail(file + ': cannot resolve the @Mixin target ' + match[1] + ' from the imports');
        return null;
    }
    return imported.replace(/\./g, '/');
};

// Every "@Inject(" / "@ModifyVariable(" body, with parentheses balanced and strings skipped.
const annotationBodies = (java, annotation) => {
    const bodies = [];
    const token = '@' + annotation;
    let index = java.indexOf(token);
    while (index !== -1) {
        const open = java.indexOf('(', index);
        if (open === -1) break;
        let depth = 0;
        let cursor = open;
        let inString = false;
        for (; cursor < java.length; cursor++) {
            const character = java[cursor];
            if (inString) {
                if (character === '\\') cursor++;
                else if (character === '"') inString = false;
                continue;
            }
            if (character === '"') inString = true;
            else if (character === '(') depth++;
            else if (character === ')' && --depth === 0) break;
        }
        bodies.push(java.slice(open + 1, cursor));
        index = java.indexOf(token, cursor + 1);
    }
    return bodies;
};

// The string list of "method = ..." inside one annotation body, concatenations joined.
const selectorList = (body, key) => {
    const match = body.match(new RegExp(key + '\\s*=\\s*'));
    if (!match) return null;
    const start = match.index + match[0].length;
    let end;
    if (body[start] === '{') {
        let depth = 0;
        let cursor = start;
        for (; cursor < body.length; cursor++) {
            if (body[cursor] === '{') depth++;
            else if (body[cursor] === '}' && --depth === 0) {
                cursor++;
                break;
            }
        }
        end = cursor;
    } else {
        const rest = body.slice(start);
        const next = rest.search(/,\s*[A-Za-z_$][\w$]*\s*=/);
        end = next === -1 ? body.length : start + next;
    }
    return body.slice(start, end)
        // JVM descriptors never contain a comma, so a comma outside the literals separates elements.
        .split(/,(?=(?:[^"]*"[^"]*")*[^"]*$)/)
        .map((part) => (part.match(/"[^"]*"/g) || []).map((literal) => literal.slice(1, -1)).join(''))
        .map((selector) => selector.trim())
        .filter((selector) => selector.length > 0);
};

const splitSelector = (selector) => {
    const open = selector.indexOf('(');
    return open === -1 ? [selector, null] : [selector.slice(0, open), selector.slice(open)];
};

const checkMethod = (target, name, descriptor, file) => {
    if (!MAPPING_SOURCES.length) return;
    const members = METHOD_MEMBERS.get(target);
    if (descriptor !== null && members && members.has(name + ' ' + descriptor)) return;
    if (descriptor === null && METHOD_NAMES.get(target) && METHOD_NAMES.get(target).has(name)) return;
    const declaredElsewhere = descriptor !== null
        ? [...METHOD_MEMBERS.entries()].filter(([, values]) => values.has(name + ' ' + descriptor))
        : [...METHOD_NAMES.entries()].filter(([, values]) => values.has(name));
    if (declaredElsewhere.length) {
        notes.push(file + ': ' + name + ' is not declared on ' + target + ' but on '
            + declaredElsewhere.map(([owner]) => owner).join(', ') + ' (inherited target?)');
        return;
    }
    fail(file + ': ' + name + (descriptor === null ? '' : descriptor)
        + ' does not exist on ' + target + ' in the 1.20.1 mappings');
};

const checkField = (target, name, file) => {
    if (!MAPPING_SOURCES.length) return;
    const members = FIELD_MEMBERS.get(target);
    if (members && members.has(name)) return;
    fail(file + ': field ' + name + ' does not exist on ' + target + ' in the 1.20.1 mappings');
};

const checkShadowFields = (java, target, file) => {
    let index = java.indexOf('@Shadow');
    while (index !== -1) {
        const end = java.indexOf(';', index);
        if (end === -1) break;
        const declaration = java.slice(index, end);
        const identifier = declaration.match(/([A-Za-z_$][\w$]*)\s*$/);
        const aliases = [...declaration.matchAll(/"([^"]+)"/g)].map((entry) => entry[1]);
        if (identifier) {
            const name = identifier[1];
            if (!SRG_MEMBER.test(name)) {
                fail(file + ': @Shadow ' + name + ' must use the SRG field name (Forge production)');
            }
            if (!aliases.length) {
                fail(file + ': @Shadow ' + name + ' needs an official alias for the development runtime');
            }
            checkField(target, name, file);
            aliases.forEach((alias) => checkField(target, alias, file));
        }
        index = java.indexOf('@Shadow', end + 1);
    }
};

const checkMixins = (java, target, file) => {
    for (const annotation of ['Inject', 'ModifyVariable']) {
        for (const body of annotationBodies(java, annotation)) {
            const selectors = selectorList(body, 'method');
            if (!selectors || !selectors.length) {
                fail(file + ': @' + annotation + ' declares no method selectors');
                continue;
            }
            const label = '@' + annotation + ' ' + selectors.join(' | ');
            if (!selectors.some((selector) => SRG_MEMBER.test(splitSelector(selector)[0]))) {
                fail(file + ': ' + label + ' has no SRG selector, so Forge production cannot match it');
            }
            if (!selectors.some((selector) => !SRG_MEMBER.test(splitSelector(selector)[0]))) {
                fail(file + ': ' + label + ' has no official selector for the development runtime');
            }
            if (!/require\s*=\s*0/.test(body)) {
                fail(file + ': ' + label + ' must set require = 0 so a drifted selector cannot crash the client');
            }
            selectors.forEach((selector) => {
                const [name, descriptor] = splitSelector(selector);
                checkMethod(target, name, descriptor, file);
            });
        }
    }
    checkShadowFields(java, target, file);
};

loadMappings();

let config;
try {
    config = JSON.parse(readSource(configFile));
} catch (error) {
    fail(configFile + ' is not valid JSON (Mixin parses it with Gson, no comments allowed): ' + error.message);
}

if (config) {
    if (config.refmap) {
        const shipped = path.join(root, 'src/main/resources', config.refmap);
        if (!fs.existsSync(shipped)) {
            fail(configFile + ': declares refmap "' + config.refmap + '" but that file is not shipped; '
                + 'the Mixin annotation processor is disabled, so the reference must be removed');
        }
    }
    const entries = []
        .concat(config.mixins || [], config.client || [], config.server || [])
        .filter((entry) => typeof entry === 'string');
    for (const entry of entries) {
        const file = 'src/main/java/' + String(config.package).replace(/\./g, '/') + '/'
            + entry.replace(/\./g, '/') + '.java';
        if (!fs.existsSync(path.join(root, file))) {
            fail(configFile + ': mixin ' + entry + ' has no source at ' + file);
            continue;
        }
        const java = readSource(file);
        const target = resolveTarget(java, file);
        if (target) checkMixins(java, target, file);
    }
}

if (failures.length) {
    console.error('Mixin target verification failed:');
    failures.forEach((failure) => console.error('- ' + failure));
    process.exit(1);
}

const mapping = MAPPING_SOURCES.length
    ? 'checked against ' + MAPPING_SOURCES.join(', ')
    : 'mapping file unavailable, member existence not checked';
notes.forEach((note) => console.warn('note: ' + note));
console.log('Mixin target verification passed (' + mapping + ').');
