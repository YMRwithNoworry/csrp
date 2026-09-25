#!/usr/bin/env node
/*
 * One-shot parity pipeline: rebuild the audit input, merge the per-creature and system
 * audits, and regenerate the gap reports. Used by FIX_BRIEF.md §0.4 as the reproducible
 * "where do we stand" command.
 *
 * Usage:
 *   node scripts/entity-parity/run-parity-pipeline.cjs [--allow-missing]
 *
 * Exit code: 0 when every creature/system audit is present and valid, 1 otherwise
 * (unless --allow-missing is passed, which only reports).
 */
const { spawnSync } = require('child_process');
const path = require('path');

const REPO = path.resolve(__dirname, '..', '..');
const allowMissing = process.argv.includes('--allow-missing');

const steps = [
    ['scripts/entity-parity/build-entity-parity-input.cjs', []],
    ['scripts/entity-parity/verify-entity-parity.cjs', allowMissing ? ['--allow-missing'] : []],
    ['scripts/entity-parity/summarize-parity-gaps.cjs', []],
    ['scripts/entity-parity/verify-system-parity.cjs', allowMissing ? ['--allow-missing'] : []],
];

let failed = 0;
for (const [script, extra] of steps) {
    const result = spawnSync(process.execPath, [path.join(REPO, script), ...extra], {
        cwd: REPO,
        stdio: 'inherit',
        encoding: 'utf8',
    });
    if (result.status !== 0) failed += 1;
}

console.log('');
console.log(failed === 0 ? 'parity pipeline: OK' : `parity pipeline: ${failed} step(s) reported problems`);
if (failed > 0 && !allowMissing) process.exitCode = 1;
