#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Wave 1 of the MC 26.3 port: purely mechanical renames verified against the real
MC 26.3 + NeoForge 26.3 source tree (extracted to .ref263/mc-src).

Why MC 26.3 renamed things: since 26.1 Minecraft ships UNOBFUSCATED, so Mojang's
own source names are used instead of the old mojmap-derived names
(ResourceLocation -> Identifier). On top of that, 26.3 reorganised a lot of
packages (RenderType -> ...renderer.rendertype, GameRules -> ...level.gamerules).

Every mapping below was verified by locating the declaration in .ref263/mc-src
(see .ref263/typeindex.json), not from memory.

Usage:  python scripts/port263/wave1_renames.py [--dry-run]
"""
import io
import os
import re
import sys

SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'java')

# --- same simple name, new package: rewrite the import line + inline FQNs only
PACKAGE_MOVES = {
    'net.minecraft.world.level.GameRules': 'net.minecraft.world.level.gamerules.GameRules',
    'net.minecraft.client.renderer.RenderType': 'net.minecraft.client.renderer.rendertype.RenderType',
    'net.minecraft.world.entity.animal.WaterAnimal': 'net.minecraft.world.entity.animal.fish.WaterAnimal',
    'net.minecraft.world.entity.npc.Villager': 'net.minecraft.world.entity.npc.villager.Villager',
    'net.minecraft.world.item.ArmorMaterial': 'net.minecraft.world.item.equipment.ArmorMaterial',
    'net.minecraft.world.entity.projectile.Snowball':
        'net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball',
    'net.minecraft.world.entity.monster.AbstractSkeleton':
        'net.minecraft.world.entity.monster.skeleton.AbstractSkeleton',
    'net.minecraft.world.entity.monster.Slime': 'net.minecraft.world.entity.monster.cubemob.Slime',
    'net.minecraft.world.entity.projectile.AbstractArrow': 'net.minecraft.world.entity.projectile.arrow.AbstractArrow',
    'net.minecraft.world.entity.projectile.Arrow': 'net.minecraft.world.entity.projectile.arrow.Arrow',
    'net.minecraft.advancements.CriteriaTriggers': 'net.minecraft.advancements.triggers.CriteriaTriggers',
    'net.minecraft.client.gui.GuiMessage': 'net.minecraft.client.multiplayer.chat.GuiMessage',
    'net.minecraft.client.gui.GuiMessageTag': 'net.minecraft.client.multiplayer.chat.GuiMessageTag',
}

# --- the class itself was renamed: rewrite every occurrence of the simple name
TYPE_RENAMES = {
    'ResourceLocation': 'Identifier',
    'MobSpawnType': 'EntitySpawnReason',
    'ItemInteractionResult': 'InteractionResult',
}

# --- InteractionResultHolder was removed; InteractionResult absorbed it
IRH_IMPORT_FROM = 'import net.minecraft.world.InteractionResultHolder;'
IRH_IMPORT_TO = 'import net.minecraft.world.InteractionResult;'
IRH_CALLS = {
    'sidedSuccess': 'InteractionResult.SUCCESS',
    'success': 'InteractionResult.SUCCESS',
    'consume': 'InteractionResult.CONSUME',
    'pass': 'InteractionResult.PASS',
    'fail': 'InteractionResult.FAIL',
}


def replace_call(text, name, target):
    """Replace InteractionResultHolder.<name>( ... ) with <target>, honouring nesting."""
    needle = 'InteractionResultHolder.' + name + '('
    out = []
    i = 0
    count = 0
    while True:
        j = text.find(needle, i)
        if j < 0:
            out.append(text[i:])
            break
        out.append(text[i:j])
        k = j + len(needle)
        depth = 1
        while k < len(text) and depth:
            if text[k] == '(':
                depth += 1
            elif text[k] == ')':
                depth -= 1
            k += 1
        out.append(target)
        i = k
        count += 1
    return ''.join(out), count


def main():
    dry = '--dry-run' in sys.argv
    stats = {k: 0 for k in list(PACKAGE_MOVES) + list(TYPE_RENAMES) + list(IRH_CALLS)}
    stats['<generic>'] = 0
    stats['<bare>'] = 0
    stats['files'] = 0
    touched = []

    for dirpath, _, files in os.walk(SRC):
        for fn in files:
            if not fn.endswith('.java'):
                continue
            path = os.path.join(dirpath, fn)
            orig = io.open(path, encoding='utf-8').read()
            text = orig
            hits = 0

            for old, new in PACKAGE_MOVES.items():
                n = text.count(old)
                if n:
                    text = text.replace(old, new)
                    stats[old] += n
                    hits += n

            for old, new in TYPE_RENAMES.items():
                new_text, n = re.subn(r'\b%s\b' % re.escape(old), new, text)
                if n:
                    stats[old] += n
                    hits += n
                text = new_text

            if 'InteractionResultHolder' in text:
                if IRH_IMPORT_FROM in text:
                    text = text.replace(IRH_IMPORT_FROM, IRH_IMPORT_TO)
                    hits += 1
                for name, target in IRH_CALLS.items():
                    text, n = replace_call(text, name, target)
                    stats[name] += n
                    hits += n
                # return type / generics: InteractionResultHolder<ItemStack> -> InteractionResult
                new_text, n = re.subn(r'\bInteractionResultHolder\s*<[^<>]*>', 'InteractionResult', text)
                stats['<generic>'] += n
                hits += n
                new_text2, n = re.subn(r'\bInteractionResultHolder\b', 'InteractionResult', new_text)
                stats['<bare>'] += n
                hits += n
                text = new_text2

            if text != orig:
                stats['files'] += 1
                touched.append(path)
                if not dry:
                    io.open(path, 'w', encoding='utf-8', newline='\n').write(text)

    print('DRY RUN' if dry else 'APPLIED')
    for k, v in stats.items():
        if v:
            print('  %-70s %d' % (k, v))
    print('files touched: %d' % stats['files'])
    if dry:
        for t in touched[:40]:
            print('   ', t)


if __name__ == '__main__':
    main()
