#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把 1.10.9 的 deadhead_tree_large_*.nbt 从 1.12.2 结构格式转成 1.20.1 可加载的格式。

为什么必须在离线做
------------------
`StructureTemplate.load` → `SimplePalette.load` 对每个 palette 条目调用
`BlockStateParser.parseForBlock(...)`；遇到该方块不存在的属性时 StateDefinition 会抛
IllegalArgumentException，整个结构放置**静默失败**（只有一条日志）。1.12.2 的
`check_decay` / `decayable` / `snow_layer` / `part` / `variant` 在 1.20.1 都已不存在，
所以必须在写盘前清掉，不能指望运行时兜底。

转换规则（每条都对应 1.20.1 的目标状态定义，已用 javap 核实）
------------------------------------------------------------
* `srparasites:`                     -> `csrp:`（命名空间迁移）
* `minecraft:snow_layer`             -> `minecraft:snow`（1.13 起重命名；属性 `layers` 保留）
* `*:deadhead_leaves`                -> 只保留 `snowy`（丢弃 check_decay / decayable）
* `*:parasitetrunk`                  -> 只保留 `axis`（丢弃 1.12.2 的 `variant`：
   本工程 parasitetrunk 是普通 RotatedPillarBlock，无 variant 属性）
* `*:deadhead_grass_tall`            -> `part=top|bottom` 改为 `half=upper|lower`（T23）
* `*:deadhead_grass_short`           -> 只保留 `texture`
* `*:snow_short_grass` /
  `*:snow_tall_grass` /
  `*:snow_covered_grass`             -> 清空属性（这三个方块在 1.20.1 无 state 属性）
* 任何其它属性一律丢弃（安全兜底，宁少不错）
"""
import gzip
import os
import shutil
import struct
import sys

SRC_DIR = r"D:/code/MC模组/_srp-orig/jar/assets/srparasites/structures"
DST_DIR = r"src/main/resources/data/csrp/structures"
FILES = ["deadhead_tree_large_%d.nbt" % i for i in range(1, 5)]
TARGET_DATA_VERSION = 3465  # 1.20.1

# 方块名 -> 允许保留的属性（None 表示丢弃全部属性）
ALLOWED = {
    "csrp:deadhead_leaves": {"snowy"},
    "csrp:parasitetrunk": {"axis"},
    "csrp:deadhead_grass_short": {"texture"},
    "csrp:deadhead_grass_tall": {"half"},
    "csrp:snow_short_grass": set(),
    "csrp:snow_tall_grass": set(),
    "csrp:snow_covered_grass": set(),
    "minecraft:snow": {"layers"},
}
VALUE_MAP = {("csrp:deadhead_grass_tall", "part", "top"): ("half", "upper"),
             ("csrp:deadhead_grass_tall", "part", "bottom"): ("half", "lower"),
             ("csrp:deadhead_grass_tall", "half", "top"): ("half", "upper"),
             ("csrp:deadhead_grass_tall", "half", "bottom"): ("half", "lower")}
NAME_MAP = {"minecraft:snow_layer": "minecraft:snow"}

# ---------------------------------------------------------------- NBT 读写


def read_nbt(data):
    pos = 0

    def u1():
        nonlocal pos
        v = data[pos]
        pos += 1
        return v

    def i2():
        nonlocal pos
        v = struct.unpack_from(">h", data, pos)[0]
        pos += 2
        return v

    def i4():
        nonlocal pos
        v = struct.unpack_from(">i", data, pos)[0]
        pos += 4
        return v

    def i8():
        nonlocal pos
        v = struct.unpack_from(">q", data, pos)[0]
        pos += 8
        return v

    def f4():
        nonlocal pos
        v = struct.unpack_from(">f", data, pos)[0]
        pos += 4
        return v

    def f8():
        nonlocal pos
        v = struct.unpack_from(">d", data, pos)[0]
        pos += 8
        return v

    def s():
        nonlocal pos
        n = struct.unpack_from(">H", data, pos)[0]
        pos += 2
        v = data[pos:pos + n].decode("utf-8")
        pos += n
        return v

    def pay(t):
        nonlocal pos
        if t == 1:
            v = struct.unpack_from(">b", data, pos)[0]
            pos += 1
            return v
        if t == 2:
            return i2()
        if t == 3:
            return i4()
        if t == 4:
            return i8()
        if t == 5:
            return f4()
        if t == 6:
            return f8()
        if t == 7:
            n = i4()
            v = data[pos:pos + n]
            pos += n
            return v
        if t == 8:
            return s()
        if t == 9:
            it = u1()
            n = i4()
            return [pay(it) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = u1()
                if tt == 0:
                    return d
                nm = s()
                d[nm] = (tt, pay(tt))
            return d
        if t == 11:
            n = i4()
            return [i4() for _ in range(n)]
        if t == 12:
            n = i4()
            return [i8() for _ in range(n)]
        raise ValueError("tag %d" % t)

    root_type = u1()
    assert root_type == 10, root_type
    s()  # root name
    return pay(10)


def write_nbt(tree):
    """tree 必须是已经过 bare() 的纯 Python 树（无类型前缀）。"""
    out = bytearray()

    def w1(v):
        out.append(v & 0xFF)

    def w2(v):
        out.extend(struct.pack(">h", v))

    def w4(v):
        out.extend(struct.pack(">i", v))

    def w8(v):
        out.extend(struct.pack(">q", v))

    def wstr(v):
        b = v.encode("utf-8")
        w2(len(b))
        out.extend(b)

    def _scalar_type(x):
        if isinstance(x, str):
            return 8
        if isinstance(x, bool):
            return 1
        if isinstance(x, dict):
            return 10
        if isinstance(x, list):
            return 9
        if isinstance(x, int):
            return 3
        if isinstance(x, float):
            return 5
        if isinstance(x, (bytes, bytearray)):
            return 7
        raise ValueError(type(x))

    def wtag(t, v):
        if t is None:
            t = _scalar_type(v)
        if t == 1:
            out.extend(struct.pack(">b", v))
        elif t == 2:
            w2(v)
        elif t == 3:
            w4(v)
        elif t == 4:
            w8(v)
        elif t == 5:
            out.extend(struct.pack(">f", v))
        elif t == 6:
            out.extend(struct.pack(">d", v))
        elif t == 7:
            w4(len(v))
            out.extend(v)
        elif t == 8:
            wstr(v)
        elif t == 9:
            items = v
            if items:
                # compound 列表统一用 tag 10；标量列表取首元素类型
                it = 10 if isinstance(items[0], dict) else _scalar_type(items[0])
            else:
                it = 8
            w1(it)
            w4(len(items))
            for x in items:
                wtag(it, x)
        elif t == 10:
            for k, cv in v.items():
                ct = _scalar_type(cv)
                w1(ct)
                wstr(k)
                wtag(ct, cv)
            w1(0)
        elif t == 11:
            w4(len(v))
            for x in v:
                w4(x)
        elif t == 12:
            w4(len(v))
            for x in v:
                w8(x)
        else:
            raise ValueError("write tag %d" % t)

    w1(10)
    wstr("")
    # 类型由值本身推断（见 _scalar_type）：字符串/整数/列表/compound 均无损，
    # 唯一需要显式保留的是 compound 列表（用 tag 10 而非标量列表）。
    wtag(None, tree)
    return bytes(out)


# ---------------------------------------------------------------- 转换


def bare(node):
    """把读取器输出的 (tagType, value) 树，转成纯 Python 树。

    读取器的表示法：
      compound -> {key: (tagType, value)}
      list     -> [(elemTagType, elemValue), ...]   <- 元素本身也带类型前缀
      标量     -> python 值

    这里把 compound/list 里的类型前缀全部剥掉，只留下值。之所以需要它，是因为
    palette 条目本身就是 compound，落在 LIST<COMPOUND> 里，会被包成
    `(10, {..: (8, '...')})`，直接当 dict 用会炸。

    类型在写回时由值本身推断（`_scalar_type`），对本工程用到的结构数据无损：
    string / int / compound / LIST<COMPOUND> 都能准确还原。
    """

    def conv(t, v):
        if t == 10:
            return {k: conv(ct, cv) for k, (ct, cv) in v.items()}
        if t == 9:
            # 元素可能带类型前缀 (tag, value)，也可能是裸值；compound 元素还是裸 dict
            # 但字段值仍带前缀，所以裸值一律过一遍 unwrap_leftovers。
            out = []
            for item in v:
                if isinstance(item, tuple) and len(item) == 2 and isinstance(item[0], int):
                    out.append(conv(item[0], item[1]))
                else:
                    out.append(unwrap_leftovers(item))
            return out
        if t in (11, 12):
            return list(v)
        if t == 7:
            return v                      # byte[] 保持 bytes
        if t == 1:
            return bool(v)                # byte 语义上是布尔/小整数
        return v

    def unwrap_leftovers(x):
        """兜底：把仍然以 (tag, value) 形式存在的值递归剥掉类型前缀。"""
        if isinstance(x, tuple) and len(x) == 2 and isinstance(x[0], int):
            return conv(x[0], x[1])
        if isinstance(x, dict):
            return {k: unwrap_leftovers(val) for k, val in x.items()}
        if isinstance(x, list):
            return [unwrap_leftovers(i) for i in x]
        return x

    t, v = node if isinstance(node, tuple) else (10, node)
    return unwrap_leftovers(conv(t, v))


def convert_palette(entries):
    """entries: [{'Name': str, 'Properties': {k: v}}] -> 转换后的新列表"""
    result = []
    for raw in entries:
        entry = raw if isinstance(raw, dict) else bare(raw)
        name = entry.get("Name")
        name = NAME_MAP.get(name, name.replace("srparasites:", "csrp:"))
        props = entry.get("Properties") or {}
        allowed = ALLOWED.get(name)
        if allowed is None:
            print("      ! unknown block in palette, properties kept as-is:", name)
            new_props = dict(props)
        else:
            new_props = {}
            for k, v in props.items():
                mapped = VALUE_MAP.get((name, k, v))
                if mapped is not None:
                    new_props[mapped[0]] = mapped[1]
                elif k in allowed:
                    new_props[k] = v
        e = {"Name": name}
        if new_props:
            e["Properties"] = new_props
        result.append(e)
    return result


def main():
    os.makedirs(DST_DIR, exist_ok=True)
    # 先把四个结构全部在内存里转换成功，再一次性落盘 —— 避免中途异常把已写好的
    # 结构文件截断成空文件（本脚本调试期发生过一次，靠重新运行导入脚本才恢复）。
    results = []
    for fn in FILES:
        src = os.path.join(SRC_DIR, fn)
        if not os.path.exists(src):
            sys.exit("missing source: " + src)
        with gzip.open(src, "rb") as fh:
            root = read_nbt(fh.read())

        # 读取器把 compound 存成 {key: (tagType, value)}；整体转成裸树，
        # 再只替换 palette 与 DataVersion 两个子项，其余子树原样保留。
        tree = bare(root)
        palette = tree["palette"]
        before = [(e.get("Name"), tuple(sorted((e.get("Properties") or {}).items()))) for e in palette]
        new_palette = convert_palette(palette)

        tree["palette"] = [{"Name": e["Name"], **({"Properties": e["Properties"]} if e.get("Properties") else {})}
                           for e in new_palette]
        # 结构根里补 DataVersion（1.20.1 = 3465）；缺失时 StructureTemplate 也能加载，但显式写上更稳
        tree["DataVersion"] = TARGET_DATA_VERSION
        # blocks[].state 是调色板下标；转换不改条目数与顺序，因此下标依然有效（下方校验）
        assert len(tree["palette"]) == len(palette), fn
        max_state = max(b["state"] for b in tree["blocks"])
        assert 0 <= max_state < len(tree["palette"]), (fn, max_state)

        results.append((fn, before, new_palette, write_nbt(tree)))

    for fn, before, new_palette, blob in results:
        dst = os.path.join(DST_DIR, fn)
        with gzip.open(dst, "wb") as fh:
            fh.write(blob)
        print("== %s" % fn)
        for (n0, p0), e in zip(before, new_palette):
            p1 = tuple(sorted((e.get("Properties") or {}).items()))
            flag = "  " if (n0, p0) == (e["Name"], p1) else "->"
            print("   %s %-46s %s" % (flag, "%s %s" % (n0, dict(p0)), "%s %s" % (e["Name"], dict(p1))))
        print("   written %s (%d bytes)" % (dst, os.path.getsize(dst)))


if __name__ == "__main__":
    main()
