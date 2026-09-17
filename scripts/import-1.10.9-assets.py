#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""SRParasites 1.10.9 -> csrp 1.20.1 资源导入（确定性、可重复执行）。

原始 jar 解包根：<SRC>/assets/srparasites  +  <SRC>/data/srparasites
目标工程资源：   <DST>/assets/csrp         +  <DST>/data/csrp

改写规则
--------
1. 命名空间          srparasites:            -> csrp:
2. 方块模型引用       csrp:<name>             -> csrp:block/<name>      （仅 blockstates 内）
3. 纹理目录           csrp:blocks/            -> csrp:blocks/           （保持不变：
   本工程沿用 1.12.2 的复数目录 textures/blocks/，已在 453 个既有模型中确认）
4. 父模型            block/<x>               -> minecraft:block/<x>
5. 原版纹理           minecraft:blocks/<x>    -> minecraft:block/<x>    （1.20.1 目录为单数）
6. 双高草属性          part=top|bottom         -> half=upper|lower       （T23：DOUBLE_BLOCK_HALF
   序列化名为 upper/lower，1.10.9 的自定义 "part" 属性在 1.20.1 不存在）
7. 无属性多模型变体    "normal": [...]        -> "": [...]              （1.20.1 不接受带属性名的
   无属性变体键；"normal" 不是合法的状态属性名）
8. snow_covered_grass 的 minecraft:grass_snowed -> minecraft:grass_block
   （grass_snowed 模型自 1.13 起移除）
"""
import json
import os
import shutil
import sys

SRC = r"D:/code/MC模组/_srp-orig/jar"
DST = r"src/main/resources"
ASSETS_SRC = os.path.join(SRC, "assets", "srparasites")
ASSETS_DST = os.path.join(DST, "assets", "csrp")
DATA_SRC = os.path.join(SRC, "data", "srparasites")
DATA_DST = os.path.join(DST, "data", "csrp")

NEW_BLOCKSTATES = [
    "deadhead_grass_short",
    "deadhead_grass_tall",
    "snow_covered_grass",
    "snow_short_grass",
    "snow_tall_grass",
]
NEW_BLOCK_MODELS = [
    "deadhead_grass_short1",
    "deadhead_grass_short2",
    "deadhead_grass_short3",
    "deadhead_grass_short4",
    "deadhead_grass_short5",
    "deadhead_grass_tall_bottom",
    "deadhead_grass_tall_top",
    "parasitetrunk_deadhead_rare",
    "snow_short_grass",
    "snow_short_grass_1",
    "snow_short_grass_2",
    "snow_short_grass_3",
    "snow_short_grass_4",
    "snow_tall_grass",
    "snow_tall_grass_1",
    "snow_tall_grass_2",
    "snow_tall_grass_3",
    "snow_tall_grass_4",
]
NEW_ITEM_MODELS = [
    "deadhead_grass_short",
    "deadhead_grass_tall",
    "snow_short_grass",
    "snow_tall_grass",
]
NEW_TEXTURES = [
    "deadhead_grass_short1.png",
    "deadhead_grass_short2.png",
    "deadhead_grass_short3.png",
    "deadhead_grass_short4.png",
    "deadhead_grass_short5.png",
    "deadhead_grass_tall_bottom.png",
    "deadhead_grass_tall_top.png",
    "parasitetrunk_deadhead_side_rare.png",
    "snowy_grass_short.png",
    "snowy_double_plant_grass_bottom.png",
    "snowy_double_plant_grass_top.png",
]
NEW_STRUCTURES = [
    "deadhead_tree_large_1.nbt",
    "deadhead_tree_large_2.nbt",
    "deadhead_tree_large_3.nbt",
    "deadhead_tree_large_4.nbt",
]

log = []


def read_json(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def write_json(path, obj, compact=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        if compact:
            json.dump(obj, fh, ensure_ascii=False, separators=(",", ":"))
        else:
            json.dump(obj, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    log.append("write " + os.path.relpath(path, DST).replace("\\", "/"))


def ns(value):
    """规则 1 + 5：命名空间与 vanilla 纹理目录。"""
    if not isinstance(value, str):
        return value
    v = value.replace("srparasites:", "csrp:")
    v = v.replace("minecraft:blocks/", "minecraft:block/")
    return v


def convert_blockstate(name):
    """规则 1/2/3/5/6/7/8：转换一个 blockstate json。"""
    src = os.path.join(ASSETS_SRC, "blockstates", name + ".json")
    data = read_json(src)

    if name == "snow_covered_grass":
        # 规则 8：grass_snowed 已移除，改用 1.20.1 的草方块模型。
        data = {"variants": {"snowy=false": {"model": "minecraft:block/grass_block"},
                             "snowy=true": {"model": "minecraft:block/grass_block"}}}
        return write_json(os.path.join(ASSETS_DST, "blockstates", name + ".json"), data)

    variants = {}
    for key, val in data.get("variants", {}).items():
        k = key.replace("srparasites:", "csrp:")
        if name == "deadhead_grass_tall":
            # 规则 6
            k = k.replace("part=top", "half=upper").replace("part=bottom", "half=lower")
        if k == "normal":
            # 规则 7
            k = ""
        if isinstance(val, list):
            for entry in val:
                if "model" in entry:
                    m = entry["model"].replace("srparasites:", "")
                    entry["model"] = "csrp:block/" + m
        elif isinstance(val, dict) and "model" in val:
            m = val["model"].replace("srparasites:", "")
            val["model"] = "csrp:block/" + m
        variants[k] = val
    write_json(os.path.join(ASSETS_DST, "blockstates", name + ".json"), {"variants": variants})


def convert_block_model(name):
    """规则 1/3/4/5：转换一个 models/block json。"""
    src = os.path.join(ASSETS_SRC, "models", "block", name + ".json")
    data = read_json(src)
    if "parent" in data:
        p = data["parent"]
        if p.startswith("srparasites:"):
            p = "csrp:block/" + p.split(":", 1)[1]
        elif not p.startswith("minecraft:") and not p.startswith("csrp:"):
            p = "minecraft:" + p  # 规则 4
        data["parent"] = p
    if "textures" in data:
        data["textures"] = {k: ns(v) for k, v in data["textures"].items()}
    write_json(os.path.join(ASSETS_DST, "models", "block", name + ".json"), data)


def convert_item_model(name):
    src = os.path.join(ASSETS_SRC, "models", "item", name + ".json")
    data = read_json(src)
    if "parent" in data:
        p = data["parent"]
        if p.startswith("srparasites:"):
            rest = p.split(":", 1)[1]
            p = "csrp:block/" + rest if rest.startswith("snow_") else "csrp:item/" + rest
        elif not p.startswith("minecraft:") and not p.startswith("csrp:"):
            p = "minecraft:" + p
        data["parent"] = p
    if "textures" in data:
        data["textures"] = {k: ns(v) for k, v in data["textures"].items()}
    write_json(os.path.join(ASSETS_DST, "models", "item", name + ".json"), data)


def copy_texture(name):
    src = os.path.join(ASSETS_SRC, "textures", "blocks", name)
    dst = os.path.join(ASSETS_DST, "textures", "blocks", name)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)
    log.append("copy  textures/blocks/" + name)


def copy_structure(name):
    src = os.path.join(ASSETS_SRC, "structures", name)
    dst = os.path.join(DATA_DST, "structures", name)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)
    log.append("copy  data/structures/" + name)


def add_sound():
    """规则 1：追加 blizzard_reverse 到 sounds.json。"""
    path = os.path.join(ASSETS_DST, "sounds.json")
    data = read_json(path)
    entry = {
        "category": "ambient",
        "sounds": [{"name": "csrp:misc/snow_reversal", "stream": True}],
    }
    if data.get("blizzard_reverse") != entry:
        data["blizzard_reverse"] = entry
        write_json(path, data)
        log.append("edit  sounds.json += blizzard_reverse")
    src = os.path.join(ASSETS_SRC, "sounds", "misc", "snow_reversal.ogg")
    dst = os.path.join(ASSETS_DST, "sounds", "misc", "snow_reversal.ogg")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)
    log.append("copy  sounds/misc/snow_reversal.ogg")


def add_lang_keys():
    """4 个新方块名 + 世界设置开关（1.10.9 lang/en_us.lang 原文）。"""
    blocks = {
        "block.csrp.snow_short_grass": "Snowy Tall Grass",
        "block.csrp.snow_tall_grass": "Snowy Double Tall Grass",
        "block.csrp.deadhead_grass_short": "Short Deadhead Vines",
        "block.csrp.deadhead_grass_tall": "Long Deadhead Vines",
    }
    options_en = {
        "options.csrp.fractured_terrain": "Fractured Terrain",
        "options.csrp.fractured_terrain.enabled": "Enabled",
        "options.csrp.fractured_terrain.disabled": "Disabled",
        "options.csrp.fractured_terrain.description": "Splits the cold star surface into drifting plates and ravines.",
        "options.csrp.mushroom_trees": "Enable Mushroom Trees",
        "options.csrp.mushroom_trees.enabled": "Enabled",
        "options.csrp.mushroom_trees.disabled": "Disabled",
        "options.csrp.mushroom_trees.description": "Allows the cold star to grow its mushroom-shaped deadhead trees.",
    }
    blocks_zh = {
        "block.csrp.snow_short_grass": "雪覆高草",
        "block.csrp.snow_tall_grass": "雪覆双高高草",
        "block.csrp.deadhead_grass_short": "枯骸短藤",
        "block.csrp.deadhead_grass_tall": "枯骸长藤",
    }
    options_zh = {
        "options.csrp.fractured_terrain": "碎裂地形",
        "options.csrp.fractured_terrain.enabled": "启用",
        "options.csrp.fractured_terrain.disabled": "禁用",
        "options.csrp.fractured_terrain.description": "把冷星的表面撕成漂移的板块与沟壑。",
        "options.csrp.mushroom_trees": "启用菌形树",
        "options.csrp.mushroom_trees.enabled": "启用",
        "options.csrp.mushroom_trees.disabled": "禁用",
        "options.csrp.mushroom_trees.description": "允许冷星生长菌形的枯骸树。",
    }
    for locale, extra in (("en_us", dict(blocks, **options_en)), ("zh_cn", dict(blocks_zh, **options_zh))):
        path = os.path.join(ASSETS_DST, "lang", locale + ".json")
        if not os.path.exists(path):
            continue
        data = read_json(path)
        added = 0
        for k, v in extra.items():
            if k not in data:
                data[k] = v
                added += 1
        if added:
            write_json(path, data)
            log.append("edit  lang/%s.json += %d keys" % (locale, added))


def main():
    if not os.path.isdir(ASSETS_SRC):
        sys.exit("source assets not found: " + ASSETS_SRC)

    # 1. 先修复早期版本误改的 model/parent 字段（幂等，对干净仓库为 0 改动）
    repaired = repair_bad_model_refs()
    if repaired:
        log.append("repair %d file(s): model/parent csrp:blocks/ -> csrp:block/" % repaired)

    # 2. 既有模型 textures 块内的非法路径修正（限定 textures 块内，幂等）
    fixed = fix_existing_model_texture_paths()
    if fixed:
        log.append("edit  %d existing model(s): csrp:block/ -> csrp:blocks/ (textures only)" % fixed)

    # 3. 新增资产
    for n in NEW_BLOCKSTATES:
        convert_blockstate(n)
    for n in NEW_BLOCK_MODELS:
        convert_block_model(n)
    for n in NEW_ITEM_MODELS:
        convert_item_model(n)
    for n in NEW_TEXTURES:
        copy_texture(n)
    for n in NEW_STRUCTURES:
        copy_structure(n)
    add_sound()
    add_lang_keys()

    print("\n".join(log))
    print("\n%d operations" % len(log))


def repair_bad_model_refs():
    """修复本脚本早期版本误造成的损坏（幂等）。

    早期版本的 `fix_existing_model_texture_paths` 用了无差别正则，把 `"model"` 与
    `"parent"` 字段里的 `csrp:block/<x>` 也改成了 `csrp:blocks/<x>`。这两个字段是
    **模型 id**，不是纹理路径：1.16+ 起模型引用没有目录回退，
    `csrp:blocks/<x>` 会去解析 `models/blocks/<x>.json`，必然找不到 → 方块/物品变成
    missing model。

    已核实 HEAD 中 `csrp:blocks/` 只出现在 `textures` 块内，从未出现在 `model`/`parent`
    字段，因此把这两个字段里的 `csrp:blocks/` 改回 `csrp:block/` 一定是还原。
    """
    import re

    fix = re.compile(r'("(?:model|parent)"\s*:\s*")csrp:blocks/')
    changed = 0
    for sub in ("models", "blockstates"):
        root = os.path.join(ASSETS_DST, sub)
        for dirpath, _dirs, files in os.walk(root):
            for fn in files:
                if not fn.endswith(".json"):
                    continue
                path = os.path.join(dirpath, fn)
                raw = open(path, encoding="utf-8").read()
                new = fix.sub(r"\1csrp:block/", raw)
                if new != raw:
                    with open(path, "w", encoding="utf-8", newline="\n") as fh:
                        fh.write(new)
                    changed += 1
    return changed


def fix_existing_model_texture_paths():
    """把既有模型 json 的 `textures` 块里非法的路径 csrp:block/x 改成 csrp:blocks/x。

    本工程实际使用复数目录 `textures/blocks/`。仅当 `textures/blocks/x.png` 存在、
    而 `textures/block/x.png` 不存在时才改写。

    **只在 `"textures": { ... }` 块内改写** —— `"model"` 字段也写作 `csrp:block/x`
    但语义完全不同（模型 id 而非纹理），绝不能一起改，否则 blockstate 会指向不存在的
    模型（1.16+ 起模型引用不再有目录回退，必定变成 missing model）。
    """
    import re

    tex_block = re.compile(r'("textures"\s*:\s*\{)([^}]*)(\})', re.S)
    entry = re.compile(r'"([^"]+)"\s*:\s*"csrp:block/([^"]+)"')

    changed = 0
    for dirpath, _dirs, files in os.walk(os.path.join(ASSETS_DST, "models")):
        for fn in files:
            if not fn.endswith(".json"):
                continue
            path = os.path.join(dirpath, fn)
            raw = open(path, encoding="utf-8").read()
            if '"csrp:block/' not in raw or '"textures"' not in raw:
                continue
            dirty = False

            def fix_block(match):
                nonlocal dirty

                def fix_entry(m):
                    nonlocal dirty
                    key, name = m.group(1), m.group(2)
                    plural = os.path.join(ASSETS_DST, "textures", "blocks", name + ".png")
                    singular = os.path.join(ASSETS_DST, "textures", "block", name + ".png")
                    if os.path.exists(plural) and not os.path.exists(singular):
                        dirty = True
                        return '"%s": "csrp:blocks/%s"' % (key, name)
                    return m.group(0)

                return match.group(1) + entry.sub(fix_entry, match.group(2)) + match.group(3)

            new = tex_block.sub(fix_block, raw)
            if dirty and new != raw:
                with open(path, "w", encoding="utf-8", newline="\n") as fh:
                    fh.write(new)
                changed += 1
    return changed


if __name__ == "__main__":
    main()
