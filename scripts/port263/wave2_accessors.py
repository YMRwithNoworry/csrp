#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Wave 2 of the MC 26.3 port: accessor renames and constant renames.

Source of truth for every rule: the real MC 26.3 + NeoForge 26.3 sources in
.ref263/mc-src (never from memory). What changed and why:

  * Level.isClientSide          field became private -> isClientSide() method
  * Level.random                field became protected -> getRandom()
  * Entity.moveTo(x,y,z,yRot,xRot)  renamed to snapTo(...)
        !! PathNavigation.moveTo(...) STILL EXISTS and is a different method, so
           getNavigation().moveTo(...) must be left alone (28 call sites).
  * MobEffects MOVEMENT_SLOWDOWN->SLOWNESS, DIG_SLOWDOWN->MINING_FATIGUE,
        MOVEMENT_SPEED->SPEED, CONFUSION->NAUSEA, DAMAGE_BOOST->STRENGTH,
        DAMAGE_RESISTANCE->RESISTANCE   (MobEffects now holds Holder<MobEffect>)
  * GameRules.RULE_MOBGRIEFING  -> GameRules.MOB_GRIEFING
  * InteractionResult.sidedSuccess(boolean) was removed; SUCCESS is the
    client-predicted swing (SwingSource.PREDICTED), which is what sidedSuccess
    did on the client.

Usage:  python scripts/port263/wave2_accessors.py [--dry-run]
"""
import io
import os
import re
import sys

SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'java')

PLAIN_RULES = [
    # isClientSide field access -> method. Do not touch calls that already have ().
    (re.compile(r'\bisClientSide\b(?!\s*\()'), 'isClientSide()'),
    # Level.random is now protected; getRandom() is the public accessor.
    (re.compile(r'\blevel\.random\b'), 'level.getRandom()'),
    (re.compile(r'\blevel\(\)\.random\b'), 'level().getRandom()'),
    # Entity.moveTo -> snapTo, but NOT PathNavigation.moveTo.
    (re.compile(r'(?<!getNavigation\(\))\.moveTo\('), '.snapTo('),
    (re.compile(r'\bMobEffects\.MOVEMENT_SLOWDOWN\b'), 'MobEffects.SLOWNESS'),
    (re.compile(r'\bMobEffects\.DIG_SLOWDOWN\b'), 'MobEffects.MINING_FATIGUE'),
    (re.compile(r'\bMobEffects\.MOVEMENT_SPEED\b'), 'MobEffects.SPEED'),
    (re.compile(r'\bMobEffects\.CONFUSION\b'), 'MobEffects.NAUSEA'),
    (re.compile(r'\bMobEffects\.DAMAGE_BOOST\b'), 'MobEffects.STRENGTH'),
    (re.compile(r'\bMobEffects\.DAMAGE_RESISTANCE\b'), 'MobEffects.RESISTANCE'),
    (re.compile(r'\bGameRules\.RULE_MOBGRIEFING\b'), 'GameRules.MOB_GRIEFING'),
]

SIDED_SUCCESS = re.compile(r'\bInteractionResult\.sidedSuccess\(')


def replace_sided_success(text):
    """InteractionResult.sidedSuccess(<one boolean arg>) -> InteractionResult.SUCCESS"""
    out = []
    i = 0
    n = 0
    while True:
        m = SIDED_SUCCESS.search(text, i)
        if not m:
            out.append(text[i:])
            break
        out.append(text[i:m.start()])
        k = m.end()
        depth = 1
        while k < len(text) and depth:
            if text[k] == '(':
                depth += 1
            elif text[k] == ')':
                depth -= 1
            k += 1
        out.append('InteractionResult.SUCCESS')
        i = k
        n += 1
    return ''.join(out), n


def main():
    dry = '--dry-run' in sys.argv
    stats = {r.pattern: 0 for r, _ in PLAIN_RULES}
    stats['InteractionResult.sidedSuccess'] = 0
    files = 0

    for dirpath, _, fns in os.walk(SRC):
        for fn in fns:
            if not fn.endswith('.java'):
                continue
            path = os.path.join(dirpath, fn)
            orig = io.open(path, encoding='utf-8').read()
            text = orig
            for rx, repl in PLAIN_RULES:
                text, n = rx.subn(repl, text)
                stats[rx.pattern] += n
            text, n = replace_sided_success(text)
            stats['InteractionResult.sidedSuccess'] += n
            if text != orig:
                files += 1
                if not dry:
                    io.open(path, 'w', encoding='utf-8', newline='\n').write(text)

    print('DRY RUN' if dry else 'APPLIED')
    for k, v in stats.items():
        if v:
            print('  %-55s %d' % (k, v))
    print('files touched: %d' % files)


if __name__ == '__main__':
    main()
