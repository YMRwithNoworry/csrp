#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""为枯骸树补上「死头树干」独立方块的资源（模型 + blockstate）。

背景
----
1.10.9 的 `BlockParasiteTrunk` 有一个自定义属性 `variant`（值含 `DEADHEAD`），
枯骸树的树干用 `variant=deadhead`，掉皮逻辑也靠它判断（`WorldGenDeadheadTreeStructure`
第 413/419 行明确读写 `VARIANT == DEADHEAD`）。

本工程 1.20.1 的 `csrp:parasitetrunk` 是普通 `RotatedPillarBlock`（只有 `axis`），
没有 `variant`。若给加 `variant`，就会把该方块的状态空间从 3 扩到 3×5，
连带 `parasitetrunk_ball` / `_plant` / `_treestairs` 等 5 个既有 blockstate
全部要补 `variant` 组合，否则出现 missing model —— 代价与风险都不划算。

因此改用**独立方块** `csrp:parasitetrunk_deadhead` 承载死头树干：
- 树根仍用 `csrp:parasitetrunk`（可存活判定需要它）
- 树干用 `csrp:parasitetrunk_deadhead`（可由 `getStateForPlacement` 看下方方块自动选择）
- 稀有纹理变体（`parasitetrunk_deadhead_side_rare`）用 **blockstate 的 weight 机制**
  表达（19:1），无需 Java 随机，也不占存档状态空间。

本脚本产出 3 个资源文件；不接触任何 Java 代码。
"""
import json
import os

ASSETS = "src/main/resources/assets/csrp"
TEXTURE_NS = "csrp:blocks"

BLOCK_MODELS = {
    # 普通死头树干（19/20）
    "parasitetrunk_deadhead": {
        "particle": TEXTURE_NS + "/parasitetrunk_deadhead",
        "end": TEXTURE_NS + "/parasitetrunk_deadhead",
        "side": TEXTURE_NS + "/parasitetrunk_deadhead_side",
    },
    # 稀有死头树干（1/20）
    "parasitetrunk_deadhead_rare": {
        "particle": TEXTURE_NS + "/parasitetrunk_deadhead",
        "end": TEXTURE_NS + "/parasitetrunk_deadhead",
        "side": TEXTURE_NS + "/parasitetrunk_deadhead_side_rare",
    },
}
WEIGHTS = {"parasitetrunk_deadhead": 19, "parasitetrunk_deadhead_rare": 1}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    print("write", path)


def main():
    log = []

    # 1. 方块模型
    for name, textures in BLOCK_MODELS.items():
        path = os.path.join(ASSETS, "models", "block", name + ".json")
        expected = {"parent": "minecraft:block/cube_column", "textures": textures}
        if os.path.exists(path):
            current = json.load(open(path, encoding="utf-8"))
            if current == expected:
                print("keep ", path, "(already correct)")
                continue
        write_json(path, expected)
        log.append(path)

    # 2. blockstate：三个 axis × 加权模型数组（19:1）
    #    1.20.1 支持多模型加权列表，且属性解析在该分支被跳过，
    #    所以键只写 "axis=y" 这种既有属性即可。
    variants = {}
    for axis, rot in (("x", {"x": 90, "y": 90}), ("y", {}), ("z", {"x": 90})):
        variants["axis=" + axis] = [
            dict({"model": "csrp:block/" + name}, **rot, weight=WEIGHTS[name])
            for name in BLOCK_MODELS
        ]
    bs_path = os.path.join(ASSETS, "blockstates", "parasitetrunk_deadhead.json")
    write_json(bs_path, {"variants": variants})
    log.append(bs_path)

    # 3. 物品模型（工程既有 models/item/trunk_deadhead.json 已指向本模型，这里补一个
    #    与方块同名的，保持命名一致，便于后续挂 BlockItem）
    item_path = os.path.join(ASSETS, "models", "item", "parasitetrunk_deadhead.json")
    write_json(item_path, {"parent": "csrp:block/parasitetrunk_deadhead"})
    log.append(item_path)

    print("\n%d file(s) written" % len(log))


if __name__ == "__main__":
    main()
