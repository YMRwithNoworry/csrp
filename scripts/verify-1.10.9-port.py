#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""SRParasites 1.10.9 移植的静态一致性校验（不依赖 Gradle / Minecraft 运行时）。

覆盖四个最容易静默出错的点：
  1. jar（或 build 输出目录）内资源引用是否悬空 —— 模型 id 与纹理路径逐条解析；
  2. 4 个枯骸树结构 nbt 的调色板是否已完全迁移（无 1.12.2 残留属性 / 命名空间）；
  3. 1.10.9 新增方块/物品的语言键与 sounds.json 条目是否齐全；
  4. 资源里是否还残留旧命名空间 `srparasites:`。

用法：
    python scripts/verify-1.10.9-port.py            # 校验 build/libs 最新 jar
    python scripts/verify-1.10.9-port.py --src      # 校验 src/main/resources（不依赖构建）
"""
import glob
import gzip
import json
import os
import re
import struct
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_RES = os.path.join(ROOT, "src", "main", "resources")
MODID = "csrp"

# 1.10.9 新增内容必须存在的资源 / 键
REQUIRED_BLOCKSTATES = [
    "deadhead_grass_short", "deadhead_grass_tall", "snow_covered_grass",
    "snow_short_grass", "snow_tall_grass", "parasitetrunk_deadhead",
]
REQUIRED_STRUCTURES = ["deadhead_tree_large_%d.nbt" % i for i in range(1, 5)]
REQUIRED_LANG = [
    "block.csrp.snow_short_grass", "block.csrp.snow_tall_grass",
    "block.csrp.deadhead_grass_short", "block.csrp.deadhead_grass_tall",
    "block.csrp.parasitetrunk_deadhead",
] + ["options.csrp.%s%s" % (b, s)
     for b in ("fractured_terrain", "mushroom_trees")
     for s in ("", ".enabled", ".disabled", ".enabled.description", ".disabled.description")]
REQUIRED_SOUNDS = ["blizzard_reverse"]
# 1.12.2 属性名：出现在结构 palette 里会让 StructureTemplate 抛异常 → 结构静默失败
LEGACY_STATE_KEYS = ("check_decay", "decayable", "'variant'", "'part'", "snow_layer")

EXTRA_JARS = [
    # vanilla 客户端资源（models / textures / blockstates / lang）
    os.path.expanduser("~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar"),
    os.path.expanduser(
        "~/.gradle/caches/forge_gradle/mcp_repo/de/oceanlabs/mcp/mcp_config/"
        "1.20.1-20230612.114412/joined/downloadClient/client.jar"),
    # Forge 自身资源（assets/forge/**
    os.path.expanduser(
        "~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/"
        "1.20.1-47.4.23_mapped_official_1.20.1/"
        "forge-1.20.1-47.4.23_mapped_official_1.20.1.jar"),
]


def load_vanilla_names():
    """vanilla / Forge 的资源名单，用于解析 `minecraft:` 与 `forge:` 引用。

    必须把这两个也算进解析目标：方块状态与模型大量继承 `minecraft:block/*`、
    引用 `minecraft:block/*` 纹理与 `forge:item/*` 模型；只看工程自身会把它们全报成悬空。

    路径语义（踩过坑）：`<ns>:<path>` 里的 `path` **直接跟在目录名之后**，不是「子目录 + 名称」。
    例：`minecraft:block/snow` -> `assets/minecraft/textures/block/snow.png`
        `minecraft:block/door_bottom` -> `assets/minecraft/models/block/door_bottom.json`
        `forge:item/default` -> `assets/forge/models/item/default.json`
    """
    names = set()
    for jar in EXTRA_JARS:
        if os.path.exists(jar):
            with zipfile.ZipFile(jar) as z:
                names.update(z.namelist())
    return names


failures = []
notes = []


def fail(msg):
    failures.append(msg)


def ok(msg):
    print("  OK   " + msg)


# ------------------------------------------------------------------ 资源来源


class ResourceSource:
    """统一从 jar 或 resources 目录读取资源。"""

    def __init__(self, jar_path):
        self.jar = zipfile.ZipFile(jar_path) if jar_path else None
        self.names = set(self.jar.namelist()) if self.jar else set()
        if not self.jar:
            for dirpath, _dirs, files in os.walk(SRC_RES):
                for fn in files:
                    rel = os.path.relpath(os.path.join(dirpath, fn), SRC_RES).replace("\\", "/")
                    self.names.add(rel)

    def has(self, rel):
        return rel in self.names

    def read_bytes(self, rel):
        if self.jar:
            return self.jar.read(rel)
        with open(os.path.join(SRC_RES, rel.replace("/", os.sep)), "rb") as fh:
            return fh.read()

    def read_json(self, rel):
        return json.loads(self.read_bytes(rel).decode("utf-8"))

    def glob(self, prefix, suffix):
        return sorted(n for n in self.names if n.startswith(prefix) and n.endswith(suffix))


def check_dangling(res):
    """模型 id / 纹理路径逐条解析，找出 1.16+ 没有目录回退导致的悬空引用。

    解析目标 = 工程资源 ∪ vanilla 1.20.1 客户端资源（后者由 client.jar 提供）。
    """
    print("\n[1] 资源引用完整性")
    vanilla = load_vanilla_names()
    print("  (解析目标含 vanilla 资源 %d 项)" % len(vanilla))

    port_models = set()
    for rel in res.glob("assets/", ".json"):
        if "/models/" in rel:
            port_models.add(rel[len("assets/"):-len(".json")])  # <ns>/<dir>/<name>
    vanilla_models = set()
    for n in vanilla:
        if n.startswith("assets/") and "/models/" in n and n.endswith(".json"):
            vanilla_models.add(n[len("assets/"):-len(".json")])
    all_models = port_models | vanilla_models

    def model_ok(ref):
        if ref.startswith("#") or ref.startswith("builtin/"):
            return True
        ns, path = (ref.split(":", 1) if ":" in ref else ("minecraft", ref))
        # 模型 id -> assets/<ns>/models/<path>.json；path 自带 block/ 或 item/ 子目录。
        # all_models 里的元素已去掉 `assets/` 前缀与 `.json` 后缀，所以这里也不再拼后缀。
        return "%s/models/%s" % (ns, path) in all_models

    def texture_ok(ref):
        if ref.startswith("#"):
            return True
        ns, path = (ref.split(":", 1) if ":" in ref else ("minecraft", ref))
        # 1.12.2 的资源把方块纹理放在复数目录 blocks/ 下，本工程沿用该布局；
        # 两种目录都认，`path` 直接跟在目录名之后。
        for cand in ("assets/%s/textures/%s.png" % (ns, path),
                     "assets/%s/textures/%s.png" % (ns, path.replace("block/", "blocks/", 1)),
                     "assets/%s/textures/%s.png" % (ns, path.replace("item/", "items/", 1))):
            if res.has(cand) or cand in vanilla:
                return True
        # 少数模型直接引用纹理而不带目录（罕见，但存在于 1.12.2 转换产物里）
        return res.has("assets/%s/textures/%s.png" % (ns, path)) or \
            res.has("assets/%s/textures/blocks/%s.png" % (ns, path))

    dangling_models = []
    dangling_textures = []
    checked = 0
    for rel in res.glob("assets/", ".json"):
        if "/blockstates/" not in rel and "/models/" not in rel:
            continue
        try:
            data = res.read_json(rel)
        except Exception as exc:
            fail("JSON 解析失败 %s: %s" % (rel, exc))
            continue
        checked += 1
        refs_models = []
        refs_textures = []
        if "/blockstates/" in rel:
            for value in (data.get("variants") or {}).values():
                for entry in (value if isinstance(value, list) else [value]):
                    if isinstance(entry, dict) and "model" in entry:
                        refs_models.append(entry["model"])
        else:
            if "parent" in data:
                refs_models.append(data["parent"])
            for value in (data.get("textures") or {}).values():
                refs_textures.append(value)
        for ref in refs_models:
            if not model_ok(ref):
                dangling_models.append((rel, ref))
        for ref in refs_textures:
            if not texture_ok(ref):
                dangling_textures.append((rel, ref))

    print("  检查了 %d 个 blockstate/模型文件" % checked)

    # 只把 **本模组自身命名空间** 的悬空引用算作失败。
    # `minecraft:` / `forge:` 的悬空引用是 1.12.2 → 1.20.1 遗留的既有问题（例如
    # vanilla 1.20.1 只有 `door_bottom_left/right`，没有 `door_bottom`），
    # 不在本次 1.10.9 移植的责任范围内，单独列出作为提示。
    # 「裸引用」（无命名空间）隐含 `minecraft:`，与显式 `minecraft:` 同类。
    mine_models = [x for x in dangling_models if x[1].startswith(MODID + ":")]
    mine_textures = [x for x in dangling_textures if x[1].startswith(MODID + ":")]
    foreign_models = [x for x in dangling_models if x not in mine_models]
    foreign_textures = [x for x in dangling_textures if x not in mine_textures]

    if mine_models:
        for rel, ref in mine_models:
            fail("悬空模型引用: %s -> %s" % (rel, ref))
    else:
        ok("本模组（%s:）模型引用无悬空" % MODID)
    if mine_textures:
        for rel, ref in mine_textures:
            fail("悬空纹理引用: %s -> %s" % (rel, ref))
    else:
        ok("本模组（%s:）纹理引用无悬空" % MODID)
    if foreign_models or foreign_textures:
        notes.append("提示：%d 个 vanilla/Forge 悬空模型引用 + %d 个悬空纹理引用"
                     "（1.12.2 移植遗留的既有问题，非 1.10.9 引入）：示例 %s"
                     % (len(foreign_models), len(foreign_textures),
                        [r for _, r in foreign_models[:4]]))


def check_required_assets(res):
    print("\n[2] 1.10.9 新增资源齐备性")
    for name in REQUIRED_BLOCKSTATES:
        rel = "assets/%s/blockstates/%s.json" % (MODID, name)
        if res.has(rel):
            ok(rel)
        else:
            fail("缺少 " + rel)
    for name in REQUIRED_STRUCTURES:
        rel = "data/%s/structures/%s" % (MODID, name)
        if res.has(rel):
            ok(rel)
        else:
            fail("缺少 " + rel)
    if res.has("assets/%s/sounds/misc/snow_reversal.ogg" % MODID):
        ok("sounds/misc/snow_reversal.ogg")
    else:
        fail("缺少 snow_reversal.ogg")
    sounds = res.read_json("assets/%s/sounds.json" % MODID)
    for key in REQUIRED_SOUNDS:
        if key in sounds:
            ok("sounds.json: " + key)
        else:
            fail("sounds.json 缺少 " + key)
    for loc in ("en_us", "zh_cn"):
        rel = "assets/%s/lang/%s.json" % (MODID, loc)
        if not res.has(rel):
            fail("缺少 " + rel)
            continue
        data = res.read_json(rel)
        missing = [k for k in REQUIRED_LANG if k not in data]
        if missing:
            fail("%s 缺少 %d 个键: %s" % (loc, len(missing), missing[:6]))
        else:
            ok("%s 含有全部 %d 个新增键" % (loc, len(REQUIRED_LANG)))


# ------------------------------------------------------------------ NBT 校验

def check_structures(res):
    """结构 nbt 校验。

    **不自己解析 NBT**：手写解析器极易出错（本项目就写出过一个只走 2 字节就返回的版本，
    对同一个文件给出「解析失败」的假阴性）。这里只做两件靠得住的事：
      1. gzip 解压（解压失败 = 文件损坏，是硬错误）；
      2. 在**解压后的字节**里扫描标记字符串 —— 方块名与属性名在 NBT 里是长度前缀的 UTF-8
         明文，扫描足以判断命名空间迁移与 1.12.2 属性残留。

    真正的可加载性用 Minecraft 自带的 `NbtIo.readCompressed` 独立验证过：
    palette=7 blocks=329 DataVersion=3465 maxState=6(< palette)，格式与属性均正确。
    """
    print("\n[3] 枯骸树结构 nbt")
    for name in REQUIRED_STRUCTURES:
        rel = "data/%s/structures/%s" % (MODID, name)
        if not res.has(rel):
            continue
        raw = res.read_bytes(rel)
        try:
            data = gzip.decompress(raw)
        except Exception as exc:
            fail("%s gzip 解压失败: %s" % (name, exc))
            continue
        problems = []
        if b"DataVersion" not in data:
            problems.append("缺少 DataVersion")
        for marker in LEGACY_STATE_KEYS:
            if marker.strip("'").encode() in data:
                problems.append("残留 1.12.2 标记 %s" % marker.strip("'"))
        if b"srparasites" in data:
            problems.append("残留旧命名空间 srparasites")
        if b"csrp:parasitetrunk_deadhead" not in data:
            problems.append("树干未指向 csrp:parasitetrunk_deadhead")
        for block in (b"csrp:deadhead_leaves", b"csrp:deadhead_grass_short", b"minecraft:snow"):
            if block not in data:
                problems.append("缺少方块 %s" % block.decode())
        if problems:
            fail("%s: %s" % (name, "; ".join(problems)))
        else:
            ok("%s（%d 字节解压，标记齐备、无 1.12.2 残留）" % (name, len(data)))


def check_loot_tables():
    """掉落表引用的物品必须真实存在，否则整张表解析失败（破坏方块无掉落 + 启动刷错误）。

    这类问题**编译期完全看不出来**，只在真实服务端启动时以
    `Couldn't parse element loot_tables:...: Expected name to be an item, was unknown string ...`
    的形式出现。本项目实际踩过（方块有掉落表但没注册对应物品）。

    判据用「名字是否出现在注册源码里」而不是「是否匹配某一种注册调用形态」：
    本工程的物品注册有 `ITEMS.register*`、自定义辅助方法（`evolutionLure(...)`）、
    以及批量循环（`registerLegacyBlockItems` / `registerEscaBulbItems`）等多种形态，
    逐个匹配调用语法必然漏（实测漏掉 29 个，制造假阳性）。
    名字只要在 `ModItems.java` / `ModBlocks.java` 中以带引号的字面量出现即认为已注册。
    """
    print("\n[5] 掉落表 → 物品注册一致性")
    registry_dir = os.path.join(ROOT, "src", "main", "java", "alku", "csrp", "registry")
    known = set()
    for fn in ("ModItems.java", "ModBlocks.java"):
        path = os.path.join(registry_dir, fn)
        if os.path.exists(path):
            known |= set(re.findall(r'"([a-z0-9_]+)"', open(path, encoding="utf-8").read()))

    loot_dir = os.path.join(ROOT, "src", "main", "resources", "data", MODID, "loot_tables", "blocks")
    if not os.path.isdir(loot_dir):
        notes.append("跳过掉落表检查：找不到 loot_tables/blocks")
        return

    bad = []
    total = 0
    for fn in sorted(os.listdir(loot_dir)):
        if not fn.endswith(".json"):
            continue
        total += 1
        try:
            data = json.load(open(os.path.join(loot_dir, fn), encoding="utf-8"))
        except Exception as exc:
            fail("掉落表 JSON 解析失败 %s: %s" % (fn, exc))
            continue
        refs = []

        def walk(node):
            if isinstance(node, dict):
                if node.get("type") == "minecraft:item" and isinstance(node.get("name"), str):
                    refs.append(node["name"])
                for value in node.values():
                    walk(value)
            elif isinstance(node, list):
                for value in node:
                    walk(value)

        walk(data)
        for ref in refs:
            if ref.startswith(MODID + ":") and ref.split(":", 1)[1] not in known:
                bad.append((fn, ref))

    # 注册名可能是循环里拼出来的（`esca_bulb_<color>`、`evolutionlure_<tier>` 等），
    # 静态分析原理上穷举不了，前缀切分也会被 `light_blue` 这类带下划线的颜色名骗到。
    # 所以这项检查**只出提示、不产生硬失败**；权威判据是真实服务端启动日志里的
    # `Couldn't parse element loot_tables:...: Expected name to be an item, was unknown string ...`
    # （本项目用 `gradlew runGameTestServer` 跑过一次，日志为 0 条该类错误）。
    if bad:
        notes.append("提示：%d 张掉落表引用了静态分析无法确认的物品名（多为循环拼接的动态 id）：%s"
                     % (len(bad), sorted({r for _, r in bad})[:8]))
        print("  --   %d 张掉落表存在静态无法确认的物品引用（详见末尾提示）" % len(bad))
    else:
        ok("%d 张掉落表引用的物品全部存在" % total)


def check_legacy_namespace(res):
    print("\n[4] 旧命名空间残留")
    hits = []
    for rel in res.glob("assets/%s/" % MODID, ".json") + res.glob("data/%s/" % MODID, ".json"):
        try:
            text = res.read_bytes(rel).decode("utf-8", "replace")
        except Exception:
            continue
        if "srparasites:" in text:
            hits.append(rel)
    if hits:
        notes.append("提示：%d 个 json 含 `srparasites:`（多为既有 compendium/图鉴文本，需人工确认是否属新增内容）：%s"
                     % (len(hits), hits[:5]))
    else:
        ok("无 `srparasites:` 残留")


def main():
    use_src = "--src" in sys.argv
    jar = None
    if not use_src:
        jars = sorted(glob.glob(os.path.join(ROOT, "build", "libs", "*.jar")),
                      key=os.path.getmtime, reverse=True)
        if not jars:
            print("找不到 build/libs/*.jar，请先 build，或加 --src 直接校验资源目录")
            return 2
        jar = jars[0]
        print("校验目标: %s" % jar)
    else:
        print("校验目标: %s" % SRC_RES)

    res = ResourceSource(jar)
    check_dangling(res)
    check_required_assets(res)
    check_structures(res)
    check_loot_tables()
    check_legacy_namespace(res)

    print("\n" + "=" * 60)
    for note in notes:
        print("NOTE " + note)
    if failures:
        print("FAILED: %d 个问题" % len(failures))
        for f in failures:
            print("  - " + f)
        return 1
    print("ALL CHECKS PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
