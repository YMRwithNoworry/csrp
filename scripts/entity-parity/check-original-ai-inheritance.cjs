#!/usr/bin/env node
/*
 * Ground truth for the "vanilla EntityMob baseline goals" question.
 *
 * In 1.12.2 the vanilla mob baseline goals (EntityAIAttackMelee, EntityAIWanderAvoidWater,
 * EntityAIWatchClosest, EntityAILookIdle, EntityAIHurtByTarget, EntityAINearestAttackableTarget)
 * are registered by EntityMob#initEntityAI (obfuscated `func_184651_r`) and dispatched virtually:
 * a creature that overrides `func_184651_r` WITHOUT calling `super.func_184651_r()` never gets them.
 * The decompiled mobs live in package `ai/misc` (EntityParasiteBase and friends), so the question is
 * answerable statically from the audit input's inheritance chains.
 *
 * Writes docs/entity-parity/original-ai-baseline.json:
 *   { creatures: [{ id, originalClass, definesInitEntityAI, callsSuper, decidedBy, baselineApplies }] }
 *
 * Usage: node scripts/entity-parity/check-original-ai-inheritance.cjs
 */
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const PARITY_DIR = path.join(REPO, 'docs', 'entity-parity');
const INPUT_FILE = path.join(PARITY_DIR, 'audit-input.json');
const OUT_FILE = path.join(PARITY_DIR, 'original-ai-baseline.json');

const INIT_METHOD = 'func_184651_r';

function stripComments(src) {
    return src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '');
}

/** Returns { defines, callsSuper, line } for the first definition of func_184651_r in the file. */
function inspectInitEntityAI(file) {
    const src = stripComments(fs.readFileSync(file, 'utf8'));
    const lines = src.split('\n');
    for (let i = 0; i < lines.length; i += 1) {
        if (!new RegExp(`(void|protected\\s+void)\\s+${INIT_METHOD}\\s*\\(`).test(lines[i])) continue;
        // Collect the method body until brace depth returns to zero.
        let depth = 0;
        let started = false;
        let body = '';
        for (let j = i; j < lines.length; j += 1) {
            const line = lines[j];
            body += line + '\n';
            for (const ch of line) {
                if (ch === '{') {
                    depth += 1;
                    started = true;
                } else if (ch === '}') {
                    depth -= 1;
                    if (started && depth <= 0) {
                        return {
                            defines: true,
                            callsSuper: new RegExp(`super\\s*\\.\\s*${INIT_METHOD}\\s*\\(`).test(body),
                            line: i + 1,
                            bodyLines: j - i + 1,
                        };
                    }
                }
            }
        }
        return { defines: true, callsSuper: new RegExp(`super\\s*\\.\\s*${INIT_METHOD}\\s*\\(`).test(body), line: i + 1 };
    }
    return { defines: false, callsSuper: false, line: null };
}

const input = JSON.parse(fs.readFileSync(INPUT_FILE, 'utf8'));

const creatures = [];
const summary = { baselineApplies: 0, baselineNotApplicable: 0, undecided: 0 };

for (const creature of input.creatures) {
    // Walk from the leaf upward: the first class that defines initEntityAI decides.
    let decision = null;
    for (const link of creature.originalChain) {
        if (!link.file) continue;
        const info = inspectInitEntityAI(link.file);
        if (!info.defines) continue;
        decision = { class: link.class, file: link.file, ...info };
        break;
    }
    // Nothing in the (decompiled) chain defines it -> the vanilla EntityMob implementation runs.
    const baselineApplies = decision ? decision.callsSuper : true;
    if (baselineApplies) summary.baselineApplies += 1;
    else summary.baselineNotApplicable += 1;

    creatures.push({
        id: creature.id,
        originalClass: creature.originalClass,
        baselineApplies,
        decidedBy: decision
            ? {
                class: decision.class,
                file: decision.file,
                line: decision.line,
                callsSuper: decision.callsSuper,
            }
            : { class: 'net.minecraft.entity.monster.EntityMob (vanilla, not in decompile)', line: null, callsSuper: true },
    });
}

const payload = {
    generatedAt: new Date().toISOString(),
    question: 'Does the creature inherit the vanilla EntityMob baseline goals (melee attack, wander, watch closest, look idle, hurt-by, nearest player target)?',
    method: `${INIT_METHOD} (initEntityAI) definition scan along each creature's decompiled inheritance chain; EntityParasiteBase does not define it, so the first definition decides. Override without super.func_184651_r() => vanilla baseline goals are NOT registered.`,
    summary,
    creatures,
};

fs.writeFileSync(OUT_FILE, JSON.stringify(payload, null, 2));

console.log(`vanilla baseline applies: ${summary.baselineApplies}`);
console.log(`vanilla baseline NOT applicable (override without super): ${summary.baselineNotApplicable}`);
console.log('');
console.log('overriding classes (override without super call):');
const byClass = new Map();
for (const c of creatures) {
    if (c.baselineApplies) continue;
    const key = `${c.decidedBy.class}:${c.decidedBy.line}`;
    byClass.set(key, (byClass.get(key) || 0) + 1);
}
for (const [key, count] of [...byClass.entries()].sort((a, b) => b[1] - a[1])) {
    console.log(`  ${String(count).padStart(3)} creatures <- ${key}`);
}
console.log('');
console.log(`wrote ${path.relative(REPO, OUT_FILE)}`);
